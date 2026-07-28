package com.martist.vitamove.analytics;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.analytics.MuscleGroupLegendAdapter.LegendItem;
import com.martist.vitamove.exercise.ui.EditExerciseValueDialog;
import com.martist.vitamove.exercise.ui.ExerciseSearchActivity;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.nutrition.data.managers.CaloriesManager;
import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.data.model.WorkoutExercise;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class AnalyticsFragment extends Fragment implements
        EditExerciseValueDialog.OnValueSavedListener {
    private static final String TAG = "AnalyticsFragment";
    private static final String PREF_TRACKED_EXERCISE_ID = "tracked_exercise_id";
    private static final String PREF_SELECTED_PERIOD = "selected_analytics_period";
    private static final int REQUEST_CODE_SELECT_EXERCISE = 1001;


    private static final String PREF_PREFIX_CUSTOM_INITIAL_WEIGHT = "custom_initial_weight_";
    private static final String PREF_PREFIX_CUSTOM_INITIAL_REPS = "custom_initial_reps_";
    private static final String PREF_PREFIX_CUSTOM_CURRENT_WEIGHT = "custom_current_weight_";
    private static final String PREF_PREFIX_CUSTOM_CURRENT_REPS = "custom_current_reps_";
    private static final String PREF_PREFIX_CUSTOM_TARGET_WEIGHT = "custom_target_weight_";
    private static final String PREF_PREFIX_CUSTOM_TARGET_REPS = "custom_target_reps_";


    private static final String PREF_PREFIX_CUSTOM_LAST_EDIT_TIME = "custom_last_edit_time_";


    private TextView dayToggle;
    private TextView weekToggle;
    private TextView monthToggle;
    private TextView periodDateText;
    private ImageButton previousPeriodButton;
    private ImageButton nextPeriodButton;
    private View loadingContainer;


    private TextView totalVolumeText;
    private TextView totalExercisesText;
    private TextView totalSetsText;
    private TextView totalWorkoutTimeText;
    private TextView totalCaloriesText;


    private PieChart musclePieChart;
    private RecyclerView muscleLegendRecycler;
    private MuscleGroupLegendAdapter legendAdapter;

    private WorkoutRepository repository;
    private String userId;
    private final Executor executor = Executors.newCachedThreadPool();


    private AnalyticsPeriod currentPeriod = AnalyticsPeriod.DAY;
    private Calendar selectedDate = Calendar.getInstance();


    private View exerciseSelector;
    private TextView selectedExerciseName;
    private LinearLayout progressContainer;
    private TextView initialValueText;
    private TextView currentValueText;
    private TextView targetValueText;
    private ProgressBar progressIndicator;
    private TextView progressPercentageText;
    private com.github.mikephil.charting.charts.LineChart progressChart;
    private MaterialButtonToggleGroup metricToggle;
    private boolean showWeightMetric = true;


    private ImageButton editInitialValueButton;
    private ImageButton editCurrentValueButton;
    private ImageButton editTargetValueButton;

    private String trackedExerciseId;
    private Exercise trackedExercise;


    private Float customInitialWeight;
    private Integer customInitialReps;
    private Float customCurrentWeight;
    private Integer customCurrentReps;
    private Float customTargetWeight;
    private Integer customTargetReps;


    private Long lastEditTime;


    private ExerciseSet initialExerciseSet;
    private ExerciseSet latestExerciseSet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow()
                    .setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.card_background));


        }

        repository = ((VitaMoveApplication) requireActivity().getApplication()).getWorkoutRepository();


        userId = ((VitaMoveApplication) requireActivity().getApplication()).getCurrentUserId();


        initViews(view);


        loadSavedPeriod();


        updatePeriodDisplay();


        View backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());


        loadPeriodData();


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        trackedExerciseId = prefs.getString(PREF_TRACKED_EXERCISE_ID, null);


        if (trackedExerciseId != null) {
            loadTrackedExerciseData();
        }
    }

    private void initViews(View view) {

        dayToggle = view.findViewById(R.id.day_toggle);
        weekToggle = view.findViewById(R.id.week_toggle);
        monthToggle = view.findViewById(R.id.month_toggle);
        periodDateText = view.findViewById(R.id.period_date_text);
        previousPeriodButton = view.findViewById(R.id.previous_period_button);
        nextPeriodButton = view.findViewById(R.id.next_period_button);
        loadingContainer = view.findViewById(R.id.loading_container);


        totalVolumeText = view.findViewById(R.id.total_volume_text);
        totalExercisesText = view.findViewById(R.id.total_exercises_text);
        totalSetsText = view.findViewById(R.id.total_sets_text);
        totalWorkoutTimeText = view.findViewById(R.id.total_workout_time_text);
        totalCaloriesText = view.findViewById(R.id.total_calories_text);


        setupPeriodToggle();


        setupPeriodNavigation();


        musclePieChart = view.findViewById(R.id.muscle_pie_chart);
        setupPieChart();

        muscleLegendRecycler = view.findViewById(R.id.muscle_legend_recycler);
        muscleLegendRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        legendAdapter = new MuscleGroupLegendAdapter(new ArrayList<>());
        muscleLegendRecycler.setAdapter(legendAdapter);


        View progressCard = view.findViewById(R.id.exercise_progress_card);
        if (progressCard != null) {
            exerciseSelector = progressCard.findViewById(R.id.exercise_selector);
            selectedExerciseName = progressCard.findViewById(R.id.selected_exercise_name);
            progressContainer = progressCard.findViewById(R.id.progress_container);
            initialValueText = progressCard.findViewById(R.id.initial_value_text);
            currentValueText = progressCard.findViewById(R.id.current_value_text);
            targetValueText = progressCard.findViewById(R.id.target_value_text);
            progressIndicator = progressCard.findViewById(R.id.goal_progress_bar);
            progressPercentageText = progressCard.findViewById(R.id.progress_percentage_text);
            progressChart = progressCard.findViewById(R.id.progress_chart);


            setupProgressChart();


            exerciseSelector.setOnClickListener(v -> showExerciseSelector());


            metricToggle = progressCard.findViewById(R.id.metric_toggle);
            metricToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {

                    boolean isRepsOnlyExercise = trackedExercise != null &&
                            (trackedExercise.isFunctionalExercise() ||
                                    trackedExercise.isBodyweightExercise());


                    if (isRepsOnlyExercise && checkedId == R.id.weight_toggle) {

                        metricToggle.check(R.id.reps_toggle);
                        return;
                    }

                    showWeightMetric = checkedId == R.id.weight_toggle;
                    if (trackedExerciseId != null) {
                        loadExerciseProgressData();
                    }
                }
            });


            editInitialValueButton = progressCard.findViewById(R.id.edit_initial_value_button);
            editCurrentValueButton = progressCard.findViewById(R.id.edit_current_value_button);
            editTargetValueButton = progressCard.findViewById(R.id.edit_target_value_button);


            setupEditButtons();
        }
    }


    private void setupEditButtons() {
        editInitialValueButton.setOnClickListener(v -> {
            if (trackedExerciseId == null) return;


            float weight = 0;
            int reps = 0;

            if (customInitialWeight != null) {
                weight = customInitialWeight;
            } else if (initialExerciseSet != null && initialExerciseSet.getWeight() != null) {
                weight = initialExerciseSet.getWeight();
            }

            if (customInitialReps != null) {
                reps = customInitialReps;
            } else if (initialExerciseSet != null && initialExerciseSet.getReps() != null) {
                reps = initialExerciseSet.getReps();
            }


            boolean isRepsOnlyExercise = trackedExercise != null &&
                    (trackedExercise.isFunctionalExercise() ||
                            trackedExercise.isBodyweightExercise());


            EditExerciseValueDialog dialog = EditExerciseValueDialog.newInstance(
                    EditExerciseValueDialog.TYPE_INITIAL, weight, reps, isRepsOnlyExercise);
            dialog.show(getChildFragmentManager(), "edit_initial_value");
        });

        editCurrentValueButton.setOnClickListener(v -> {
            if (trackedExerciseId == null) return;


            float weight = 0;
            int reps = 0;

            if (customCurrentWeight != null) {
                weight = customCurrentWeight;
            } else if (latestExerciseSet != null && latestExerciseSet.getWeight() != null) {
                weight = latestExerciseSet.getWeight();
            }

            if (customCurrentReps != null) {
                reps = customCurrentReps;
            } else if (latestExerciseSet != null && latestExerciseSet.getReps() != null) {
                reps = latestExerciseSet.getReps();
            }


            boolean isRepsOnlyExercise = trackedExercise != null &&
                    (trackedExercise.isFunctionalExercise() ||
                            trackedExercise.isBodyweightExercise());


            EditExerciseValueDialog dialog = EditExerciseValueDialog.newInstance(
                    EditExerciseValueDialog.TYPE_CURRENT, weight, reps, isRepsOnlyExercise);
            dialog.show(getChildFragmentManager(), "edit_current_value");
        });

        editTargetValueButton.setOnClickListener(v -> {
            if (trackedExerciseId == null) return;


            float weight = 0;
            int reps = 0;

            if (customTargetWeight != null) {
                weight = customTargetWeight;
            } else if (latestExerciseSet != null && latestExerciseSet.getWeight() != null) {

                weight = latestExerciseSet.getWeight() * 1.1f;
            }

            if (customTargetReps != null) {
                reps = customTargetReps;
            } else if (latestExerciseSet != null && latestExerciseSet.getReps() != null) {

                reps = (int) (latestExerciseSet.getReps() * 1.1f);
            }


            boolean isRepsOnlyExercise = trackedExercise != null &&
                    (trackedExercise.isFunctionalExercise() ||
                            trackedExercise.isBodyweightExercise());


            EditExerciseValueDialog dialog = EditExerciseValueDialog.newInstance(
                    EditExerciseValueDialog.TYPE_TARGET, weight, reps, isRepsOnlyExercise);
            dialog.show(getChildFragmentManager(), "edit_target_value");
        });
    }


    private void loadExerciseProgressData() {
        if (trackedExerciseId == null) return;

        Log.d(TAG, "Начало загрузки данных о прогрессе упражнения: " + trackedExerciseId);


        loadCustomValues();

        executor.execute(() -> {
            try {

                List<ExerciseSet> sets = repository.getExerciseSetsHistoryById(trackedExerciseId);

                Log.d(TAG, "Загружено подходов: " + (sets != null ? sets.size() : 0));


                if (sets == null || sets.isEmpty()) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {

                            if (((customCurrentWeight != null && customCurrentWeight > 0) || (customCurrentReps != null && customCurrentReps > 0))) {
                                updateProgressDisplay();
                                progressContainer.setVisibility(View.VISIBLE);
                            } else {
                                progressContainer.setVisibility(View.GONE);
                            }
                        });
                    }
                    return;
                }


                List<ExerciseSet> filteredSets = new ArrayList<>();
                Map<String, ExerciseSet> firstSetsByWorkoutExercise = new HashMap<>();
                for (ExerciseSet set : sets) {
                    if (set.isCompleted() && set.getCreatedAt() != null &&
                            ((set.getWeight() != null && set.getWeight() > 0) ||
                                    (set.getReps() != null && set.getReps() > 0))) {

                        String workoutExerciseId = set.getWorkoutExerciseId();
                        if (workoutExerciseId != null && !firstSetsByWorkoutExercise.containsKey(workoutExerciseId)) {
                            firstSetsByWorkoutExercise.put(workoutExerciseId, set);
                        }
                    }
                }
                filteredSets = new ArrayList<>(firstSetsByWorkoutExercise.values());


                List<ExerciseSet> allSetsForGraph = new ArrayList<>(filteredSets);


                if (((customCurrentWeight != null && customCurrentWeight > 0) || (customCurrentReps != null && customCurrentReps > 0)) && lastEditTime != null && lastEditTime > 0) {
                    ExerciseSet customCurrentSet = new ExerciseSet();
                    customCurrentSet.setWeight(customCurrentWeight);
                    customCurrentSet.setReps(customCurrentReps);
                    customCurrentSet.setCreatedAt(lastEditTime);
                    customCurrentSet.setCompleted(true);

                    allSetsForGraph.add(customCurrentSet);
                }


                allSetsForGraph.sort(java.util.Comparator.comparing(ExerciseSet::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

                if (allSetsForGraph.isEmpty()) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> progressContainer.setVisibility(View.GONE));
                    }
                    return;
                }


                if (customInitialWeight == null && customInitialReps == null && !filteredSets.isEmpty()) {


                    filteredSets.sort(java.util.Comparator.comparing(ExerciseSet::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
                    initialExerciseSet = filteredSets.get(0);


                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    SharedPreferences.Editor editor = prefs.edit();

                    if (initialExerciseSet.getWeight() != null && initialExerciseSet.getWeight() > 0) {
                        customInitialWeight = initialExerciseSet.getWeight();
                        editor.putFloat(PREF_PREFIX_CUSTOM_INITIAL_WEIGHT + trackedExerciseId, customInitialWeight);
                    }

                    if (initialExerciseSet.getReps() != null && initialExerciseSet.getReps() > 0) {
                        customInitialReps = initialExerciseSet.getReps();
                        editor.putInt(PREF_PREFIX_CUSTOM_INITIAL_REPS + trackedExerciseId, customInitialReps);
                    }

                    editor.apply();
                    Log.d(TAG, "Сохранили первый подход как начальное значение: вес=" + customInitialWeight + ", повторения=" + customInitialReps);
                } else {

                    initialExerciseSet = new ExerciseSet();
                    initialExerciseSet.setWeight(customInitialWeight);
                    initialExerciseSet.setReps(customInitialReps);
                    Log.d(TAG, "Используем сохраненные начальные значения: вес=" + customInitialWeight + ", повторения=" + customInitialReps);
                }


                latestExerciseSet = allSetsForGraph.get(allSetsForGraph.size() - 1);


                List<com.github.mikephil.charting.data.Entry> weightEntries = new ArrayList<>();
                List<com.github.mikephil.charting.data.Entry> repsEntries = new ArrayList<>();
                List<String> dateLabels = new ArrayList<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());

                for (int i = 0; i < allSetsForGraph.size(); i++) {
                    ExerciseSet set = allSetsForGraph.get(i);
                    if (set.getCreatedAt() == null) continue;

                    String dateLabel = dateFormat.format(new Date(set.getCreatedAt()));
                    dateLabels.add(dateLabel);

                    int index = dateLabels.size() - 1;

                    if (set.getWeight() != null && set.getWeight() > 0) {
                        weightEntries.add(new com.github.mikephil.charting.data.Entry(index, set.getWeight()));
                    }

                    if (set.getReps() != null && set.getReps() > 0) {
                        repsEntries.add(new com.github.mikephil.charting.data.Entry(index, set.getReps()));
                    }
                }

                final List<String> finalDateLabels = dateLabels;

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        updateProgressDisplay();
                        updateProgressChart(weightEntries, repsEntries, finalDateLabels);
                        progressContainer.setVisibility(View.VISIBLE);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке данных о прогрессе: " + e.getMessage());
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progressContainer.setVisibility(View.GONE);
                    });
                }
            }
        });
    }


    private void loadTrackedExerciseData() {
        if (trackedExerciseId == null) return;


        loadCustomValues();

        executor.execute(() -> {
            try {
                Exercise exercise = repository.getExerciseById(trackedExerciseId);

                if (exercise != null && isAdded()) {
                    trackedExercise = exercise;
                    requireActivity().runOnUiThread(() -> {
                        selectedExerciseName.setText(exercise.getName());


                        boolean isRepsOnlyExercise = exercise.isFunctionalExercise() || exercise.isBodyweightExercise();


                        if (isRepsOnlyExercise) {

                            if (metricToggle != null) {
                                metricToggle.setVisibility(View.GONE);
                            }


                            showWeightMetric = false;
                        } else {

                            if (metricToggle != null) {
                                metricToggle.setVisibility(View.VISIBLE);
                            }
                        }

                        loadExerciseProgressData();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке данных об упражнении: " + e.getMessage());
                Log.e(TAG, "Ошибка при загрузке отслеживаемого упражнения: " + e.getMessage());
            }
        });
    }


    private void setupProgressChart() {
        if (progressChart == null) return;


        progressChart.getDescription().setEnabled(false);
        progressChart.setTouchEnabled(true);
        progressChart.setDragEnabled(true);
        progressChart.setScaleEnabled(true);
        progressChart.setPinchZoom(true);
        progressChart.setDrawGridBackground(false);
        progressChart.setHighlightPerDragEnabled(true);


        progressChart.getAxisRight().setEnabled(false);


        com.github.mikephil.charting.components.YAxis leftAxis = progressChart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary));
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);


        com.github.mikephil.charting.components.XAxis xAxis = progressChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5, true);


        com.github.mikephil.charting.components.Legend legend = progressChart.getLegend();
        legend.setEnabled(false);


        progressChart.setNoDataText("Нет данных для отображения");
        progressChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary));
    }


    private void updateProgressChart(List<com.github.mikephil.charting.data.Entry> weightEntries, List<com.github.mikephil.charting.data.Entry> repsEntries, List<String> dateLabels) {
        if (progressChart == null) return;


        boolean hasData = showWeightMetric ? !weightEntries.isEmpty() : !repsEntries.isEmpty();

        if (!hasData) {
            progressChart.clear();
            progressChart.setNoDataText("Нет данных для отображения");
            progressChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary));
            return;
        }


        com.github.mikephil.charting.components.XAxis xAxis = progressChart.getXAxis();
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dateLabels.size()) {
                    return dateLabels.get(index);
                }
                return "";
            }
        });


        com.github.mikephil.charting.data.LineData data = new com.github.mikephil.charting.data.LineData();


        if (showWeightMetric) {

            if (!weightEntries.isEmpty()) {
                com.github.mikephil.charting.data.LineDataSet weightDataSet = new com.github.mikephil.charting.data.LineDataSet(weightEntries, "");
                weightDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
                weightDataSet.setLineWidth(2f);
                weightDataSet.setDrawCircles(true);
                weightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
                weightDataSet.setCircleRadius(4f);
                weightDataSet.setDrawValues(true);
                weightDataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.textColorPrimary));
                weightDataSet.setValueTextSize(9f);
                weightDataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.LINEAR);


                weightDataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.format(Locale.getDefault(), "%.1f", value);
                    }
                });

                data.addDataSet(weightDataSet);
            }
        } else {

            if (!repsEntries.isEmpty()) {
                com.github.mikephil.charting.data.LineDataSet repsDataSet = new com.github.mikephil.charting.data.LineDataSet(repsEntries, "");
                repsDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
                repsDataSet.setLineWidth(2f);
                repsDataSet.setDrawCircles(true);
                repsDataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
                repsDataSet.setCircleRadius(4f);
                repsDataSet.setDrawValues(true);
                repsDataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.textColorPrimary));
                repsDataSet.setValueTextSize(9f);
                repsDataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.LINEAR);


                repsDataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) value);
                    }
                });

                data.addDataSet(repsDataSet);
            }
        }


        progressChart.setData(data);


        progressChart.setVisibleXRangeMaximum(7);
        if (dateLabels.size() > 7) {
            progressChart.moveViewToX(dateLabels.size() - 7);
        }


        progressChart.invalidate();


        progressChart.animateY(1000);
    }


    private String formatSetValueForWeight(ExerciseSet set) {

        if (customCurrentWeight != null && customCurrentWeight > 0) {
            return String.format(Locale.getDefault(), "%.1f кг", customCurrentWeight);
        }


        if (set != null && set.getWeight() != null && set.getWeight() > 0) {
            return String.format(Locale.getDefault(), "%.1f кг", set.getWeight());
        }


        return "Не установлено";
    }


    private String formatSetValueForReps(ExerciseSet set) {

        if (customCurrentReps != null && customCurrentReps > 0) {
            return customCurrentReps + " повт.";
        }


        if (set != null && set.getReps() != null && set.getReps() > 0) {
            return set.getReps() + " повт.";
        }


        return "Не установлено";
    }


    @Override
    public void onValueSaved(int type, float weight, int reps) {
        if (trackedExerciseId == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = prefs.edit();

        switch (type) {
            case EditExerciseValueDialog.TYPE_INITIAL:
                customInitialWeight = weight > 0 ? weight : null;
                customInitialReps = reps > 0 ? reps : null;

                if (customInitialWeight != null) {
                    editor.putFloat(PREF_PREFIX_CUSTOM_INITIAL_WEIGHT + trackedExerciseId, customInitialWeight);
                } else {
                    editor.remove(PREF_PREFIX_CUSTOM_INITIAL_WEIGHT + trackedExerciseId);
                }

                if (customInitialReps != null) {
                    editor.putInt(PREF_PREFIX_CUSTOM_INITIAL_REPS + trackedExerciseId, customInitialReps);
                } else {
                    editor.remove(PREF_PREFIX_CUSTOM_INITIAL_REPS + trackedExerciseId);
                }
                break;

            case EditExerciseValueDialog.TYPE_CURRENT:
                long currentTime = System.currentTimeMillis();
                lastEditTime = currentTime;
                editor.putLong(PREF_PREFIX_CUSTOM_LAST_EDIT_TIME + trackedExerciseId, currentTime);

                customCurrentWeight = weight > 0 ? weight : null;
                customCurrentReps = reps > 0 ? reps : null;

                if (customCurrentWeight != null) {
                    editor.putFloat(PREF_PREFIX_CUSTOM_CURRENT_WEIGHT + trackedExerciseId, customCurrentWeight);
                } else {
                    editor.remove(PREF_PREFIX_CUSTOM_CURRENT_WEIGHT + trackedExerciseId);
                }

                if (customCurrentReps != null) {
                    editor.putInt(PREF_PREFIX_CUSTOM_CURRENT_REPS + trackedExerciseId, customCurrentReps);
                } else {
                    editor.remove(PREF_PREFIX_CUSTOM_CURRENT_REPS + trackedExerciseId);
                }
                break;

            case EditExerciseValueDialog.TYPE_TARGET:
                customTargetWeight = weight > 0 ? weight : null;
                customTargetReps = reps > 0 ? reps : null;

                if (customTargetWeight != null) {
                    editor.putFloat(PREF_PREFIX_CUSTOM_TARGET_WEIGHT + trackedExerciseId, customTargetWeight);
                } else {
                    editor.remove(PREF_PREFIX_CUSTOM_TARGET_WEIGHT + trackedExerciseId);
                }

                if (customTargetReps != null) {
                    editor.putInt(PREF_PREFIX_CUSTOM_TARGET_REPS + trackedExerciseId, customTargetReps);
                } else {
                    editor.remove(PREF_PREFIX_CUSTOM_TARGET_REPS + trackedExerciseId);
                }
                break;
        }

        editor.apply();


        loadExerciseProgressData();
    }


    private void updateProgressDisplay() {
        if (trackedExerciseId == null) return;


        boolean isRepsOnlyExercise = trackedExercise != null &&
                (trackedExercise.isFunctionalExercise() ||
                        trackedExercise.isBodyweightExercise());


        if (isRepsOnlyExercise) {
            showWeightMetric = false;
        }


        final String initialValue, currentValue, targetValue;
        final int progress;
        final float currentValue_float, targetValue_float;


        boolean exerciseNeverPerformed = (latestExerciseSet == null ||
                (latestExerciseSet.getWeight() == null && latestExerciseSet.getReps() == null)) &&
                customCurrentWeight == null && customCurrentReps == null;

        if (exerciseNeverPerformed) {

            initialValue = "Не установлено";
            currentValue = "Вы еще ни разу не выполняли это упражнение";
            targetValue = "Не задана";
            progress = 0;
            currentValue_float = 0;
            targetValue_float = 0;
        } else if (showWeightMetric) {


            if (customInitialWeight != null && customInitialWeight > 0) {
                initialValue = String.format(Locale.getDefault(), "%.1f кг", customInitialWeight);
            } else {
                initialValue = "Не установлено";
            }

            currentValue = formatSetValueForWeight(latestExerciseSet);


            currentValue_float = latestExerciseSet != null && latestExerciseSet.getWeight() != null
                    ? latestExerciseSet.getWeight() : 0f;

            if (customTargetWeight != null) {
                targetValue = String.format(Locale.getDefault(), "%.1f кг", customTargetWeight);
                targetValue_float = customTargetWeight;
            } else {

                targetValue = "Не задана";


                float targetWeight = 0;
                if (latestExerciseSet != null && latestExerciseSet.getWeight() != null && latestExerciseSet.getWeight() > 0) {
                    targetWeight = latestExerciseSet.getWeight() * 1.1f;
                }
                targetValue_float = targetWeight;
            }


            progress = calculateCustomProgressForWeight();

        } else {


            if (customInitialReps != null && customInitialReps > 0) {
                initialValue = customInitialReps + " повт.";
            } else {
                initialValue = "Не установлено";
            }

            currentValue = formatSetValueForReps(latestExerciseSet);


            currentValue_float = latestExerciseSet != null && latestExerciseSet.getReps() != null
                    ? latestExerciseSet.getReps() : 0f;

            if (customTargetReps != null) {
                targetValue = customTargetReps + " повт.";
                targetValue_float = customTargetReps;
            } else {

                targetValue = "Не задана";


                int targetReps = 0;
                if (latestExerciseSet != null && latestExerciseSet.getReps() != null && latestExerciseSet.getReps() > 0) {
                    targetReps = (int) (latestExerciseSet.getReps() * 1.1f);
                }
                targetValue_float = targetReps;
            }


            progress = calculateCustomProgressForReps();
        }


        initialValueText.setText(initialValue);
        currentValueText.setText(currentValue);
        targetValueText.setText(targetValue);


        int goalProgress = 0;


        if (!exerciseNeverPerformed && targetValue_float > 0 && currentValue_float > 0) {
            goalProgress = (int) ((currentValue_float / targetValue_float) * 100);
        }


        progressIndicator.setProgress(Math.min(100, goalProgress));


        if (targetValue.equals("Не задана")) {
            progressPercentageText.setText("Цель не задана");
        } else if (exerciseNeverPerformed) {
            progressPercentageText.setText("0% от цели");
        } else {
            progressPercentageText.setText(String.format(Locale.getDefault(), "%d%% от цели", goalProgress));
        }


        if (goalProgress >= 100) {
            progressPercentageText.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
            progressPercentageText.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            progressPercentageText.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            progressPercentageText.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }


    private int calculateCustomProgressForWeight() {

        float initialWeight = (customInitialWeight != null && customInitialWeight > 0) ? customInitialWeight : 0;


        float currentWeight = 0;
        if (customCurrentWeight != null && customCurrentWeight > 0) {
            currentWeight = customCurrentWeight;
        } else if (latestExerciseSet != null && latestExerciseSet.getWeight() != null && latestExerciseSet.getWeight() > 0) {
            currentWeight = latestExerciseSet.getWeight();
        }


        if (initialWeight <= 0 || currentWeight <= 0) return 0;

        return Math.min(100, (int) ((currentWeight - initialWeight) * 100.0f / initialWeight));
    }


    private int calculateCustomProgressForReps() {

        int initialReps = (customInitialReps != null && customInitialReps > 0) ? customInitialReps : 0;


        int currentReps = 0;
        if (customCurrentReps != null && customCurrentReps > 0) {
            currentReps = customCurrentReps;
        } else if (latestExerciseSet != null && latestExerciseSet.getReps() != null && latestExerciseSet.getReps() > 0) {
            currentReps = latestExerciseSet.getReps();
        }


        if (initialReps <= 0 || currentReps <= 0) return 0;

        return Math.min(100, (int) ((currentReps - initialReps) * 100.0f / initialReps));
    }


    private void setupPeriodToggle() {
        View.OnClickListener periodClickListener = v -> {
            AnalyticsPeriod newPeriod;
            if (v.getId() == R.id.day_toggle) {
                newPeriod = AnalyticsPeriod.DAY;
            } else if (v.getId() == R.id.week_toggle) {
                newPeriod = AnalyticsPeriod.WEEK;
            } else if (v.getId() == R.id.month_toggle) {
                newPeriod = AnalyticsPeriod.MONTH;
            } else {
                return;
            }

            if (newPeriod != currentPeriod) {
                currentPeriod = newPeriod;
                updateTabSelection();
                savePeriodPreference();
                updatePeriodDisplay();
                loadPeriodData();
            }
        };

        dayToggle.setOnClickListener(periodClickListener);
        weekToggle.setOnClickListener(periodClickListener);
        monthToggle.setOnClickListener(periodClickListener);
    }


    private void setupPeriodNavigation() {
        previousPeriodButton.setOnClickListener(v -> {
            navigatePeriod(-1);
        });

        nextPeriodButton.setOnClickListener(v -> {
            navigatePeriod(1);
        });
    }


    private void loadSavedPeriod() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String savedPeriod = prefs.getString(PREF_SELECTED_PERIOD, AnalyticsPeriod.DAY.name());

        try {
            currentPeriod = AnalyticsPeriod.valueOf(savedPeriod);
        } catch (IllegalArgumentException e) {
            currentPeriod = AnalyticsPeriod.DAY;
        }


        updateTabSelection();
    }


    private void savePeriodPreference() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().putString(PREF_SELECTED_PERIOD, currentPeriod.name()).apply();
    }


    private void updateTabSelection() {

        dayToggle.setSelected(false);
        weekToggle.setSelected(false);
        monthToggle.setSelected(false);


        switch (currentPeriod) {
            case DAY:
                dayToggle.setSelected(true);
                break;
            case WEEK:
                weekToggle.setSelected(true);
                break;
            case MONTH:
                monthToggle.setSelected(true);
                break;
        }
    }


    private void updatePeriodDisplay() {
        SimpleDateFormat dateFormat;
        String dateText;

        switch (currentPeriod) {
            case DAY:
                dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
                dateText = dateFormat.format(selectedDate.getTime());
                break;
            case WEEK:
                Calendar weekStart = (Calendar) selectedDate.clone();
                weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                Calendar weekEnd = (Calendar) weekStart.clone();
                weekEnd.add(Calendar.DAY_OF_WEEK, 6);

                SimpleDateFormat weekFormat = new SimpleDateFormat("dd MMM", new Locale("ru"));
                if (weekStart.get(Calendar.MONTH) == weekEnd.get(Calendar.MONTH)) {
                    dateText = weekFormat.format(weekStart.getTime()) + " - " +
                            weekFormat.format(weekEnd.getTime()) + " " +
                            new SimpleDateFormat("yyyy", new Locale("ru")).format(weekEnd.getTime());
                } else {
                    SimpleDateFormat fullWeekFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("ru"));
                    dateText = fullWeekFormat.format(weekStart.getTime()) + " - " +
                            fullWeekFormat.format(weekEnd.getTime());
                }
                break;
            case MONTH:
                dateFormat = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));
                dateText = dateFormat.format(selectedDate.getTime());

                dateText = dateText.substring(0, 1).toUpperCase() + dateText.substring(1);
                break;
            default:
                dateText = "";
                break;
        }

        periodDateText.setText(dateText);
    }


    private void navigatePeriod(int direction) {
        switch (currentPeriod) {
            case DAY:
                selectedDate.add(Calendar.DAY_OF_MONTH, direction);
                break;
            case WEEK:
                selectedDate.add(Calendar.WEEK_OF_YEAR, direction);
                break;
            case MONTH:
                selectedDate.add(Calendar.MONTH, direction);
                break;
        }

        updatePeriodDisplay();
        loadPeriodData();
    }


    private long[] getPeriodTimeRange() {
        Calendar startCalendar = (Calendar) selectedDate.clone();
        Calendar endCalendar = (Calendar) selectedDate.clone();

        switch (currentPeriod) {
            case DAY:

                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                startCalendar.set(Calendar.MINUTE, 0);
                startCalendar.set(Calendar.SECOND, 0);
                startCalendar.set(Calendar.MILLISECOND, 0);


                endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endCalendar.set(Calendar.MINUTE, 59);
                endCalendar.set(Calendar.SECOND, 59);
                endCalendar.set(Calendar.MILLISECOND, 999);
                break;

            case WEEK:

                startCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                startCalendar.set(Calendar.MINUTE, 0);
                startCalendar.set(Calendar.SECOND, 0);
                startCalendar.set(Calendar.MILLISECOND, 0);


                endCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endCalendar.set(Calendar.MINUTE, 59);
                endCalendar.set(Calendar.SECOND, 59);
                endCalendar.set(Calendar.MILLISECOND, 999);
                break;

            case MONTH:

                startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                startCalendar.set(Calendar.MINUTE, 0);
                startCalendar.set(Calendar.SECOND, 0);
                startCalendar.set(Calendar.MILLISECOND, 0);


                endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endCalendar.set(Calendar.MINUTE, 59);
                endCalendar.set(Calendar.SECOND, 59);
                endCalendar.set(Calendar.MILLISECOND, 999);
                break;
        }

        return new long[]{startCalendar.getTimeInMillis(), endCalendar.getTimeInMillis()};
    }


    private void loadPeriodData() {

        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                if (loadingContainer != null) {
                    loadingContainer.setVisibility(View.VISIBLE);
                }
            });
        }

        executor.execute(() -> {
            try {

                long[] timeRange = getPeriodTimeRange();
                long startTime = timeRange[0];
                long endTime = timeRange[1];


                List<UserWorkout> periodWorkouts = repository.getWorkoutHistory(userId, startTime, endTime, 0, 100);


                int totalExercises = 0;
                int totalSets = 0;
                int totalDurationMinutes = 0;
                int totalCalories = 0;
                float totalVolume = 0;


                Map<String, Integer> categoryCountMap = new HashMap<>();

                for (UserWorkout workout : periodWorkouts) {

                    if (workout.getEndTime() == null) continue;


                    List<WorkoutExercise> exercises = workout.getExercises();
                    if (exercises != null) {
                        totalExercises += exercises.size();


                        for (WorkoutExercise exercise : exercises) {

                            if (exercise.getExercise() != null) {
                                List<String> categories = exercise.getExercise().getCategories();

                                if (categories == null || categories.isEmpty()) {
                                    categoryCountMap.put("Другое", categoryCountMap.getOrDefault("Другое", 0) + 1);
                                } else {

                                    for (String category : categories) {
                                        if (category == null || category.trim().isEmpty()) {
                                            continue;
                                        }
                                        categoryCountMap.put(category, categoryCountMap.getOrDefault(category, 0) + 1);
                                    }
                                }
                            }

                            if (exercise.getSetsCompleted() != null) {
                                List<ExerciseSet> sets = exercise.getSetsCompleted();
                                totalSets += sets.size();


                                for (ExerciseSet set : sets) {
                                    if (set.isCompleted() && set.getWeight() > 0 && set.getReps() > 0) {
                                        totalVolume += set.getWeight() * set.getReps();
                                    }
                                }
                            }
                        }
                    }


                    long duration = workout.getEndTime() - workout.getStartTime();
                    totalDurationMinutes += (int) (duration / (1000 * 60));


                    totalCalories += workout.getTotalCalories();
                }


                CaloriesManager caloriesManager = CaloriesManager.getInstance(requireContext());
                int burnedCalories = caloriesManager.getTotalBurnedCalories();


                totalCalories = Math.max(totalCalories, burnedCalories);


                final List<PieEntry> entries = new ArrayList<>();
                final List<LegendItem> legendItems = new ArrayList<>();
                final int[] MATERIAL_COLORS = getMaterialColors();


                int totalCategories = 0;
                for (int count : categoryCountMap.values()) {
                    totalCategories += count;
                }


                if (categoryCountMap.isEmpty()) {
                    entries.add(new PieEntry(100, "Нет данных"));
                    legendItems.add(new LegendItem("Нет данных", Color.GRAY, 100, 0));
                } else {

                    int colorIndex = 0;
                    for (Map.Entry<String, Integer> entry : categoryCountMap.entrySet()) {
                        String category = entry.getKey();
                        int count = entry.getValue();
                        float percent = (count * 100f) / totalCategories;

                        entries.add(new PieEntry(count, category));

                        int color = MATERIAL_COLORS[colorIndex % MATERIAL_COLORS.length];
                        legendItems.add(new LegendItem(category, color, percent, count));

                        colorIndex++;
                    }
                }


                final int finalTotalExercises = totalExercises;
                final int finalTotalSets = totalSets;
                final int finalTotalDurationMinutes = totalDurationMinutes;
                final int finalTotalCalories = totalCalories;
                final float finalTotalVolume = totalVolume;

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {

                        if (loadingContainer != null) {
                            loadingContainer.setVisibility(View.GONE);
                        }

                        totalExercisesText.setText(String.valueOf(finalTotalExercises));
                        totalSetsText.setText(String.valueOf(finalTotalSets));
                        totalWorkoutTimeText.setText(formatDuration(finalTotalDurationMinutes));
                        totalCaloriesText.setText(String.valueOf(finalTotalCalories));
                        totalVolumeText.setText(String.format(Locale.getDefault(), "%.1f кг", finalTotalVolume));


                        updateMuscleChart(entries, legendItems);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке данных тренировок", e);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {

                        if (loadingContainer != null) {
                            loadingContainer.setVisibility(View.GONE);
                        }

                        totalExercisesText.setText("0");
                        totalSetsText.setText("0");
                        totalWorkoutTimeText.setText("0 мин");
                        totalCaloriesText.setText("0");
                        totalVolumeText.setText("0 кг");


                        List<PieEntry> entries = new ArrayList<>();
                        entries.add(new PieEntry(100, "Нет данных"));

                        List<LegendItem> legendItems = new ArrayList<>();
                        legendItems.add(new LegendItem("Нет данных", Color.GRAY, 100, 0));

                        updateMuscleChart(entries, legendItems);
                    });
                }
            }
        });
    }


    private void updateMuscleChart(List<PieEntry> entries, List<LegendItem> legendItems) {

        PieDataSet dataSet = new PieDataSet(entries, "Категории мышц");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);


        int[] colors = new int[legendItems.size()];
        for (int i = 0; i < legendItems.size(); i++) {
            colors[i] = legendItems.get(i).color;
        }
        dataSet.setColors(colors);


        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(musclePieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);


        musclePieChart.setData(data);
        musclePieChart.invalidate();


        legendItems.sort((item1, item2) -> Float.compare(item2.percent, item1.percent));


        legendAdapter.setItems(legendItems);
    }


    private int[] getMaterialColors() {
        return new int[]{
                Color.rgb(244, 67, 54),
                Color.rgb(33, 150, 243),
                Color.rgb(76, 175, 80),
                Color.rgb(255, 152, 0),
                Color.rgb(156, 39, 176),
                Color.rgb(0, 188, 212),
                Color.rgb(255, 87, 34),
                Color.rgb(121, 85, 72),
                Color.rgb(63, 81, 181),
                Color.rgb(139, 195, 74),
                Color.rgb(233, 30, 99),
                Color.rgb(0, 150, 136),
                Color.rgb(255, 193, 7),
                Color.rgb(96, 125, 139)
        };
    }


    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + " мин";
        } else {
            int hours = minutes / 60;
            int mins = minutes % 60;
            return hours + " ч " + mins + " мин";
        }
    }


    private void showExerciseSelector() {

        Intent intent = new Intent(getActivity(), ExerciseSearchActivity.class);

        intent.putExtra("from_analytics", true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_EXERCISE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_EXERCISE && resultCode == Activity.RESULT_OK && data != null) {

            if (data.hasExtra("selected_exercise")) {
                Exercise selectedExercise = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    selectedExercise = data.getParcelableExtra("selected_exercise", Exercise.class);
                } else {
                    selectedExercise = data.getParcelableExtra("selected_exercise");
                }

                if (selectedExercise != null) {

                    onExerciseSelected(selectedExercise);
                }
            } else if (data.getBooleanExtra("exercise_added_via_details", false)) {
                String exerciseId = data.getStringExtra("exercise_id");
                if (exerciseId != null) {
                    trackedExerciseId = exerciseId;


                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    prefs.edit().putString(PREF_TRACKED_EXERCISE_ID, trackedExerciseId).apply();


                    loadTrackedExerciseData();
                }
            }
        }
    }


    public void onExerciseSelected(Exercise exercise) {

        trackedExercise = exercise;
        trackedExerciseId = exercise.getId();


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().putString(PREF_TRACKED_EXERCISE_ID, trackedExerciseId).apply();


        customInitialWeight = null;
        customInitialReps = null;
        customCurrentWeight = null;
        customCurrentReps = null;
        customTargetWeight = null;
        customTargetReps = null;
        lastEditTime = null;


        selectedExerciseName.setText(exercise.getName());


        boolean isRepsOnlyExercise = exercise.isFunctionalExercise() || exercise.isBodyweightExercise();


        if (isRepsOnlyExercise) {

            if (metricToggle != null) {
                metricToggle.setVisibility(View.GONE);
            }


            showWeightMetric = false;
        } else {

            if (metricToggle != null) {
                metricToggle.setVisibility(View.VISIBLE);
            }
        }


        loadExerciseProgressData();
    }


    private void loadCustomValues() {
        if (trackedExerciseId == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());


        if (prefs.contains(PREF_PREFIX_CUSTOM_INITIAL_WEIGHT + trackedExerciseId)) {
            customInitialWeight = prefs.getFloat(PREF_PREFIX_CUSTOM_INITIAL_WEIGHT + trackedExerciseId, 0);
        } else {
            customInitialWeight = null;
        }

        if (prefs.contains(PREF_PREFIX_CUSTOM_INITIAL_REPS + trackedExerciseId)) {
            customInitialReps = prefs.getInt(PREF_PREFIX_CUSTOM_INITIAL_REPS + trackedExerciseId, 0);
        } else {
            customInitialReps = null;
        }

        if (prefs.contains(PREF_PREFIX_CUSTOM_CURRENT_WEIGHT + trackedExerciseId)) {
            customCurrentWeight = prefs.getFloat(PREF_PREFIX_CUSTOM_CURRENT_WEIGHT + trackedExerciseId, 0);
        } else {
            customCurrentWeight = null;
        }

        if (prefs.contains(PREF_PREFIX_CUSTOM_CURRENT_REPS + trackedExerciseId)) {
            customCurrentReps = prefs.getInt(PREF_PREFIX_CUSTOM_CURRENT_REPS + trackedExerciseId, 0);
        } else {
            customCurrentReps = null;
        }

        if (prefs.contains(PREF_PREFIX_CUSTOM_TARGET_WEIGHT + trackedExerciseId)) {
            customTargetWeight = prefs.getFloat(PREF_PREFIX_CUSTOM_TARGET_WEIGHT + trackedExerciseId, 0);
        } else {
            customTargetWeight = null;
        }

        if (prefs.contains(PREF_PREFIX_CUSTOM_TARGET_REPS + trackedExerciseId)) {
            customTargetReps = prefs.getInt(PREF_PREFIX_CUSTOM_TARGET_REPS + trackedExerciseId, 0);
        } else {
            customTargetReps = null;
        }


        if (prefs.contains(PREF_PREFIX_CUSTOM_LAST_EDIT_TIME + trackedExerciseId)) {
            lastEditTime = prefs.getLong(PREF_PREFIX_CUSTOM_LAST_EDIT_TIME + trackedExerciseId, 0);
        } else {
            lastEditTime = null;
        }
    }


    private void setupPieChart() {
        musclePieChart.setUsePercentValues(true);
        musclePieChart.getDescription().setEnabled(false);
        musclePieChart.setExtraOffsets(5, 10, 5, 5);
        musclePieChart.setDragDecelerationFrictionCoef(0.95f);
        musclePieChart.setDrawHoleEnabled(true);
        musclePieChart.setHoleColor(Color.TRANSPARENT);
        musclePieChart.setTransparentCircleRadius(61f);
        musclePieChart.setDrawEntryLabels(false);
        musclePieChart.setEntryLabelTextSize(12f);
        musclePieChart.setEntryLabelColor(Color.WHITE);
        musclePieChart.setRotationEnabled(true);
        musclePieChart.setHighlightPerTapEnabled(true);
        musclePieChart.setCenterText("Группы\nмышц");
        musclePieChart.setCenterTextSize(18f);
        int textColorPrimary = ContextCompat.getColor(requireContext(), R.color.textColorPrimary);
        musclePieChart.setCenterTextColor(textColorPrimary);
        musclePieChart.getLegend().setEnabled(false);
        musclePieChart.animateXY(1400, 1400);
    }
} 