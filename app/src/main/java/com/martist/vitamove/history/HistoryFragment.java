package com.martist.vitamove.history;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.OtherMonthDecorator;
import com.martist.vitamove.databinding.FragmentHistoryBinding;
import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.domain.WorkoutRepeatEvent;
import com.martist.vitamove.workout.ui.WorkoutDayDecorator;
import com.martist.vitamove.workout.ui.adapters.WorkoutDetailsAdapter;
import com.martist.vitamove.workout.ui.adapters.WorkoutHistoryAdapter;
import com.martist.vitamove.workout.ui.fragments.WorkoutFragment;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class HistoryFragment extends Fragment implements WorkoutHistoryAdapter.OnWorkoutClickListener {
    private static final String TAG = "HistoryFragment";
    private WorkoutHistoryAdapter adapter;
    private WorkoutDayDecorator workoutDecorator;
    private final Set<CalendarDay> workoutDays = new HashSet<>();
    HistoryViewModel viewModel;
    private final Executor executor = Executors.newCachedThreadPool();
    private WorkoutWithExercisesMapper workoutWithExercisesMapper = new WorkoutWithExercisesMapper();
    private FragmentHistoryBinding binding;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentHistoryBinding.bind(view);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        binding.recentWorkoutsList.setHasFixedSize(true);
        setupRecyclerView();


        setupCalendar();


        viewModel.getMonthWorkouts().observe(getViewLifecycleOwner(), workouts -> {

            executor.execute(() -> {
                List<UserWorkout> list = workoutWithExercisesMapper.map(workouts);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> updateUI(list));
                }
            });

        });

        viewModel.getWorkoutsByDay().observe(getViewLifecycleOwner(), workouts -> {
            executor.execute(() -> {
                if (!workouts.isEmpty()) {
                    var mapped = workoutWithExercisesMapper.map(workouts.get(0));
                    if (isAdded() && !mapped.getExercises().isEmpty()) {
                        requireActivity().runOnUiThread(() -> showWorkoutDetails(mapped));
                    }
                }
            });


        });


        updateCalendarMarkersForSixMonth();
    }


    @Override
    public void onResume() {
        super.onResume();


        updateStatsTitle(LocalDate.now().getMonthValue());
    }


    private void setupRecyclerView() {
        adapter = new WorkoutHistoryAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recentWorkoutsList.setLayoutManager(layoutManager);
        binding.recentWorkoutsList.setItemAnimator(null);
        binding.recentWorkoutsList.setAdapter(adapter);
    }

    private void setupCalendar() {

        if (!isAdded()) {
            return;
        }


        binding.calendarView.setTitleAnimationOrientation(MaterialCalendarView.VERTICAL);
        binding.calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
        binding.calendarView.setHeaderTextAppearance(R.style.CalendarHeader);
        binding.calendarView.setDateTextAppearance(R.style.CalendarDateText);
        binding.calendarView.setWeekDayTextAppearance(R.style.CalendarWeekText);


        String[] monthsArray = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        binding.calendarView.setTitleFormatter(new MonthArrayTitleFormatter(monthsArray));


        String[] weekDaysArray = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        binding.calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(weekDaysArray));


        binding.calendarView.state().edit()
                .setFirstDayOfWeek(DayOfWeek.of(1))
                .commit();


        binding.calendarView.setShowOtherDates(MaterialCalendarView.SHOW_OUT_OF_RANGE);


        binding.calendarView.setDynamicHeightEnabled(true);


        binding.calendarView.addDecorator(new OtherMonthDecorator());


        try {
            if (isAdded()) {
                workoutDecorator = new WorkoutDayDecorator(requireContext(), workoutDays);
                binding.calendarView.addDecorator(workoutDecorator);
            }
        } catch (IllegalStateException e) {
        }


        binding.calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return binding.calendarView.getSelectedDate() != null &&
                        binding.calendarView.getSelectedDate().equals(day);
            }

            @Override
            public void decorate(DayViewFacade view) {
                if (!isAdded()) {
                    return;
                }
                try {
                    Drawable background = ContextCompat.getDrawable(requireContext(), R.drawable.calendar_day_selected);
                    view.setBackgroundDrawable(background);
                    view.addSpan(new ForegroundColorSpan(Color.WHITE));
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error in selected day decorator: " + e.getMessage());
                }
            }
        });


        binding.calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.equals(CalendarDay.today());
            }

            @Override
            public void decorate(DayViewFacade view) {
                if (!isAdded()) {
                    return;
                }
                try {

                    Drawable background = ContextCompat.getDrawable(requireContext(), R.drawable.calendar_day_today);
                    view.setBackgroundDrawable(background);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error in today decorator: " + e.getMessage());
                }
            }
        });


        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.MONTH, -6);

        Calendar maxDate = Calendar.getInstance();

        binding.calendarView.state().edit()
                .setMinimumDate(CalendarDay.from(
                        minDate.get(Calendar.YEAR),
                        minDate.get(Calendar.MONTH) + 1,
                        minDate.get(Calendar.DAY_OF_MONTH)))
                .setMaximumDate(CalendarDay.from(
                        maxDate.get(Calendar.YEAR),
                        maxDate.get(Calendar.MONTH) + 1,
                        maxDate.get(Calendar.DAY_OF_MONTH)))
                .commit();


        binding.calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) {

                loadWorkoutsForDate(date);
            }
        });

        binding.calendarView.setOnMonthChangedListener((widget, date) -> {

            int year = date.getYear();
            int month = date.getMonth();

            viewModel.onMonthChanged(year, month);

            updateStatsTitle(month);


            if (workoutDecorator != null) {


                workoutDecorator.setDates(workoutDays);


                binding.calendarView.postDelayed(() -> {
                    if (isAdded() && workoutDecorator != null) {
                        binding.calendarView.invalidateDecorators();

                    }
                }, 100);
            }
        });
    }

    private void updateUI(List<UserWorkout> workouts) {

        executor.execute(() -> {

            final List<UserWorkout> filteredWorkouts = workouts.stream()
                    .filter(workout -> workout.getExercises() != null && !workout.getExercises().isEmpty())
                    .collect(Collectors.toList());


            final int totalWorkoutsCount = filteredWorkouts.size();
            final int totalExercisesCount = filteredWorkouts.stream()
                    .mapToInt(w -> w.getExercises().size())
                    .sum();
            final double avgDurationMinutesValue = filteredWorkouts.stream()
                    .mapToInt(UserWorkout::getDurationMinutes)
                    .average()
                    .orElse(0.0);


            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {

                    updateStats(totalWorkoutsCount, totalExercisesCount, avgDurationMinutesValue);


                    if (filteredWorkouts.isEmpty()) {
                        binding.recentWorkoutsList.setVisibility(View.GONE);
                        return;
                    }

                    binding.recentWorkoutsList.setVisibility(View.VISIBLE);


                    adapter.updateWorkouts(filteredWorkouts);

                });
            }
        });
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);


        if (isLoading && adapter.getItemCount() == 0) {
            binding.contentScroll.setVisibility(View.GONE);
        } else {
            binding.contentScroll.setVisibility(View.VISIBLE);
        }
    }

    private void updateStats(int totalWorkouts, int totalExercises, double avgDurationMinutes) {

        executor.execute(() -> {

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    binding.totalWorkouts.setText(String.format(Locale.getDefault(), "%d", totalWorkouts));
                    binding.totalExercises.setText(String.format(Locale.getDefault(), "%d", totalExercises));


                    if (totalWorkouts == 0 || avgDurationMinutes == 0.0) {
                        binding.avgDuration.setText("0 мин");
                    } else {
                        binding.avgDuration.setText(String.format(Locale.getDefault(), "%.0f мин", avgDurationMinutes));
                    }
                });
            }


            List<UserWorkout> currentWorkouts = adapter.getWorkouts();


            if (totalWorkouts == 0 || currentWorkouts == null || currentWorkouts.isEmpty()) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            binding.favoriteExercise.setText("Нет данных")
                    );
                }
                return;
            }


            String finalFavoriteExercise = currentWorkouts.stream()
                    .flatMap(w -> w.getExercises().stream())
                    .filter(e -> e.getExercise() != null)
                    .collect(Collectors.groupingBy(
                            e -> e.getExercise().getName(),
                            Collectors.counting()
                    ))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);


            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (finalFavoriteExercise != null) {
                        binding.favoriteExercise.setText(finalFavoriteExercise);
                    } else {
                        binding.favoriteExercise.setText("Нет данных");
                    }
                });
            }
        });
    }


    private void updateCalendarMarkersForSixMonth() {
        viewModel.getWorkoutDays().observe(getViewLifecycleOwner(), days -> {
            Set<CalendarDay> CalDays = new HashSet<>();


            for (Long normalizedDate : days) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(normalizedDate);
                CalDays.add(CalendarDay.from(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                ));
            }

            workoutDays.clear();
            workoutDays.addAll(CalDays);

            if (workoutDecorator != null) {
                workoutDecorator.setDates(workoutDays);
                binding.calendarView.invalidateDecorators();

            }


        });
    }

    private void showWorkoutDetails(UserWorkout workout) {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_workout_details, null);


        executor.execute(() -> {

            final String workoutName = workout.getName();
            final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, EEEE", new Locale("ru"));
            final String formattedDate = dateFormat.format(workout.getStartTime());


            String formattedDuration;
            if (workout.getEndTime() != null) {
                long duration = workout.getEndTime() - workout.getStartTime();
                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;

                if (hours > 0) {
                    formattedDuration = String.format(Locale.getDefault(), "%d ч %d мин", hours, minutes);
                } else {
                    formattedDuration = String.format(Locale.getDefault(), "%d минут", minutes);
                }
            } else {
                formattedDuration = "-";
            }


            final int exerciseCount = workout.getExercises().size();


            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    try {

                        TextView workoutNameText = dialogView.findViewById(R.id.workout_name);
                        if (workoutName != null && !workoutName.isEmpty()) {
                            workoutNameText.setText(workoutName);
                            workoutNameText.setVisibility(View.VISIBLE);
                        } else {
                            workoutNameText.setVisibility(View.GONE);
                        }


                        TextView dateText = dialogView.findViewById(R.id.workout_date);
                        dateText.setText(formattedDate);


                        TextView durationText = dialogView.findViewById(R.id.workout_duration);
                        durationText.setText(formattedDuration);


                        TextView exerciseCountText = dialogView.findViewById(R.id.exercise_count);
                        String exercisesText = getResources().getQuantityString(
                                R.plurals.exercise_count, exerciseCount, exerciseCount);
                        exerciseCountText.setText(exercisesText);


                        RecyclerView exercisesList = dialogView.findViewById(R.id.exercises_list);
                        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
                        exercisesList.setLayoutManager(layoutManager);


                        exercisesList.setHasFixedSize(false);
                        exercisesList.setNestedScrollingEnabled(true);


                        WorkoutDetailsAdapter detailsAdapter = new WorkoutDetailsAdapter();
                        exercisesList.setAdapter(detailsAdapter);
                        detailsAdapter.updateExercises(workout.getExercises());


                        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
                        dialog.setContentView(dialogView);


                        MaterialButton repeatButton = dialogView.findViewById(R.id.repeat_workout_button);
                        repeatButton.setOnClickListener(v -> {
                            dialog.dismiss();
                            repeatWorkout(workout);
                        });


                        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                        if (bottomSheet != null) {
                            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                        }

                        dialog.show();
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при отображении деталей тренировки", e);
                    }
                });
            }
        });
    }


    private void loadWorkoutsForDate(CalendarDay date) {
        viewModel.getWorkoutsByCalendarDay(date);
    }


    @Override
    public void onWorkoutClick(UserWorkout workout) {
        if (workout != null && workout.getExercises() != null && !workout.getExercises().isEmpty()) {
            showWorkoutDetails(workout);
        }
    }


    private void repeatWorkout(UserWorkout workout) {
        if (workout == null || workout.getExercises() == null || workout.getExercises().isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Нет упражнений для повторения", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }


        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof WorkoutFragment) {

            android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("VitaMovePrefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().putInt("workout_tab_index", 1).apply();


            saveExercisesForRepeat(workout.getExercises());


            WorkoutRepeatEvent event = new WorkoutRepeatEvent(workout.getExercises());
            org.greenrobot.eventbus.EventBus.getDefault().post(event);
        }
    }

    private void saveExercisesForRepeat(List<com.martist.vitamove.workout.data.model.WorkoutExercise> exercises) {
        StringBuilder exerciseIds = viewModel.saveExercisesForRepeat(exercises);
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("VitaMovePrefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putString("repeat_exercise_ids", exerciseIds.toString()).apply();

    }

    private void updateStatsTitle(int month) {
        if (isAdded()) {
            String monthName = new GetRussianMonthNameUseCase().invoke(month);
            String title = "Статистика за " + monthName;
            binding.statsMonthTitle.setText(title);
        }
    }


}
