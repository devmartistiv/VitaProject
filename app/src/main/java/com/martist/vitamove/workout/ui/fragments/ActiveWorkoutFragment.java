package com.martist.vitamove.workout.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.auth.LoginActivity;
import com.martist.vitamove.exercise.domain.AddExerciseEvent;
import com.martist.vitamove.exercise.ui.ExerciseNotesFragment;
import com.martist.vitamove.exercise.ui.ExerciseSearchActivity;
import com.martist.vitamove.exercise.ui.ExerciseSettingsActivity;
import com.martist.vitamove.exercise.ui.ReplaceExerciseDialog;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.nutrition.data.managers.CaloriesManager;
import com.martist.vitamove.set.AddSupersetDialog;
import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.data.model.WorkoutExercise;
import com.martist.vitamove.workout.domain.WorkoutCompletedEvent;
import com.martist.vitamove.workout.domain.WorkoutRepeatEvent;
import com.martist.vitamove.workout.domain.WorkoutStartedEvent;
import com.martist.vitamove.workout.ui.ActiveWorkoutViewModel;
import com.martist.vitamove.workout.ui.adapters.ActiveWorkoutAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ActiveWorkoutFragment extends Fragment
        implements ActiveWorkoutAdapter.OnExerciseClickListener, ReplaceExerciseDialog.OnExerciseSelectedListener {

    private static final String TAG = "ActiveWorkoutFragment";
    private static final int REQUEST_CODE_SELECT_EXERCISE = 1;
    private static final int REQUEST_CODE_CONFIGURE_EXERCISE = 2;

    private RecyclerView exerciseList;
    private TextView totalSetsText;
    private TextView totalExercisesText;
    private ActiveWorkoutAdapter adapter;
    private ActiveWorkoutViewModel workoutViewModel;
    private String userId;
    private TextView workoutTimerText;
    private TextView workoutCaloriesText;
    private MaterialButton startWorkoutButton;
    private MaterialButton finishWorkoutButton;
    private CountDownTimer workoutTimer;
    private long workoutStartTime;
    private boolean isWorkoutActive = false;
    private static final String KEY_WORKOUT_START_TIME = "workout_start_time";
    private static final String KEY_IS_WORKOUT_ACTIVE = "is_workout_active";
    private ItemTouchHelper itemTouchHelper;
    private CaloriesManager caloriesManager;


    private long completedWorkoutDuration = 0;
    private int completedWorkoutCalories = 0;
    private int completedWorkoutTonnage = 0;
    private String completedWorkoutName = "";


    private boolean isDataLoaded = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences prefs = requireActivity().getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        String accessToken = prefs.getString("accessToken", null);
        String refreshToken = prefs.getString("refreshToken", null);

        if (userId == null || accessToken == null || refreshToken == null) {
            Log.e(TAG, "Отсутствуют необходимые данные авторизации");
            Toast.makeText(requireContext(), "Ошибка авторизации. Пожалуйста, войдите снова", Toast.LENGTH_LONG).show();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }


        caloriesManager = CaloriesManager.getInstance(requireContext());

        Log.d(TAG, "Инициализация с userId: " + userId);


        workoutViewModel = new ViewModelProvider(this).get(ActiveWorkoutViewModel.class);
        Log.d(TAG, "onCreate: Получен экземпляр ActiveWorkoutViewModel с хеш-кодом: " + System.identityHashCode(workoutViewModel));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_workout, container, false);

        initializeViews(view);
        setupClickListeners();
        setupObservers();
        setupItemTouchHelper();
        restoreWorkoutState();


        if (!isDataLoaded && userId != null && workoutViewModel != null) {
            Log.d(TAG, "onCreateView: Запуск загрузки тренировки");
            isDataLoaded = true;
            workoutViewModel.loadOrCreateActiveWorkout(userId);
        }

        return view;
    }

    private void initializeViews(View view) {
        exerciseList = view.findViewById(R.id.exercise_list);
        totalSetsText = view.findViewById(R.id.total_sets);
        totalExercisesText = view.findViewById(R.id.total_exercises);
        startWorkoutButton = view.findViewById(R.id.start_workout_button);
        finishWorkoutButton = view.findViewById(R.id.finish_workout_button);
        workoutTimerText = view.findViewById(R.id.workout_timer);
        workoutCaloriesText = view.findViewById(R.id.workout_calories);


        exerciseList.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ActiveWorkoutAdapter(new ArrayList<>(), this);
        exerciseList.setAdapter(adapter);


    }

    private void setupClickListeners() {
        startWorkoutButton.setOnClickListener(v -> startWorkout());
        finishWorkoutButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext(), R.style.MyAlertDialogTheme)
                    .setTitle("Завершить тренировку?")
                    .setMessage("Вы уверены, что хотите завершить тренировку?")
                    .setPositiveButton("Да", (dialog, which) -> finishWorkout())
                    .setNegativeButton("Нет", null)
                    .show();
        });
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();


                return adapter.moveExercise(fromPosition, toPosition);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);


            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {

                    if (viewHolder != null) {
                        viewHolder.itemView.setAlpha(0.9f);
                    }
                }
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(exerciseList);
    }

    private void startWorkout() {

        UserWorkout workout = workoutViewModel.getActiveWorkout().getValue();
        if (workout == null || workout.getEndTime() != null) {

            if (userId != null) {

                workoutViewModel.loadOrCreateActiveWorkout(userId);
            } else {
                Log.e(TAG, "startWorkout: userId is null, не могу создать новую тренировку");
                Toast.makeText(requireContext(), "Ошибка создания тренировки", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        isWorkoutActive = true;
        workoutStartTime = System.currentTimeMillis();


        if (workout != null && workout.getId() != null) {
            try {

                new Thread(() -> {
                    try {
                        workoutViewModel.updateWorkoutStartTime(workout.getId(), workoutStartTime);
                        Log.i(TAG, "Время начала тренировки обновлено: " + workoutStartTime);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обновлении времени начала тренировки: " + e.getMessage(), e);

                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(requireContext(), "Не удалось обновить время начала тренировки", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при запуске потока обновления времени: " + e.getMessage(), e);
            }
        }


        startWorkoutButton.setVisibility(View.GONE);
        finishWorkoutButton.setVisibility(View.VISIBLE);


        startWorkoutTimer();


        saveWorkoutState();
    }

    private void finishWorkout() {
        UserWorkout workout = workoutViewModel.getActiveWorkout().getValue();

        if (workout == null || workout.getId().startsWith("temp-")) {
            Toast.makeText(requireContext(), "Тренировка еще не сохранена", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (workout.getId() != null) {
                workoutViewModel.updateWorkoutStartTime(workout.getId(), workoutStartTime);
            }
        } catch (Exception e) {

        }

        Integer currentCalories = workoutViewModel.getRealTimeCalories().getValue();
        if (currentCalories != null && currentCalories > 0) {

            caloriesManager.addCompletedWorkoutCalories(currentCalories);

            caloriesManager.resetActiveWorkoutCalories();


            long workoutDuration = workoutStartTime > 0 ? (System.currentTimeMillis() - workoutStartTime) : 0;
            WorkoutCompletedEvent event = new WorkoutCompletedEvent(currentCalories, workoutDuration, workout.getId());
            EventBus.getDefault().post(event);
            Log.d(TAG, "Отправлено событие WorkoutCompletedEvent с калориями: " + currentCalories);
        }


        completedWorkoutDuration = workoutStartTime > 0 ? (System.currentTimeMillis() - workoutStartTime) : 0;
        completedWorkoutCalories = currentCalories != null ? currentCalories : 0;
        completedWorkoutTonnage = calculateTonnage(workout);
        completedWorkoutName = workout.getName() != null ? workout.getName() : "Тренировка";

        Log.d(TAG, String.format("Сохранены данные тренировки: duration=%d, calories=%d, tonnage=%d, name=%s",
                completedWorkoutDuration, completedWorkoutCalories, completedWorkoutTonnage, completedWorkoutName));

        long endTime = System.currentTimeMillis();

        workoutViewModel.completeWorkout(endTime);


    }


    private int calculateTonnage(UserWorkout workout) {
        if (workout == null || workout.getExercises() == null) {
            Log.d(TAG, "calculateTonnage: тренировка или список упражнений null");
            return 0;
        }

        int tonnage = 0;

        for (WorkoutExercise exercise : workout.getExercises()) {
            if (exercise.getSetsCompleted() != null) {
                for (ExerciseSet set : exercise.getSetsCompleted()) {

                    if (set.isCompleted() && set.getWeight() > 0 && set.getReps() > 0) {
                        int setTonnage = (int) (set.getWeight() * set.getReps());
                        tonnage += setTonnage;

                    }
                }
            }
        }

        Log.d(TAG, "calculateTonnage: общий тоннаж = " + tonnage + " кг");
        return tonnage;
    }

    private void startWorkoutTimer() {

        workoutTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

                long elapsedTime = System.currentTimeMillis() - workoutStartTime;

                String timeText = formatElapsedTime(elapsedTime);

                workoutTimerText.setText(timeText);

                finishWorkoutButton.setIcon(null);
                finishWorkoutButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                finishWorkoutButton.setIconPadding(8);
                finishWorkoutButton.setText(timeText + " | Завершить");


                if (workoutViewModel != null) {

                    workoutViewModel.calculateRealTimeCalories(userId);
                }
            }

            @Override
            public void onFinish() {

                start();
            }
        };
        workoutTimer.start();
    }

    private void saveWorkoutState() {
        SharedPreferences.Editor editor = requireActivity()
                .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE)
                .edit();
        editor.putLong(KEY_WORKOUT_START_TIME, workoutStartTime);
        editor.putBoolean(KEY_IS_WORKOUT_ACTIVE, isWorkoutActive);
        editor.apply();
    }

    private void restoreWorkoutState() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        isWorkoutActive = prefs.getBoolean(KEY_IS_WORKOUT_ACTIVE, false);
        workoutStartTime = prefs.getLong(KEY_WORKOUT_START_TIME, 0);

        if (isWorkoutActive) {
            startWorkoutButton.setVisibility(View.GONE);
            finishWorkoutButton.setVisibility(View.VISIBLE);
            startWorkoutTimer();
        }
    }


    private void setupObservers() {
        if (workoutViewModel != null) {

            workoutViewModel.getActiveWorkout().observe(getViewLifecycleOwner(), workout -> {
                if (workout != null) {
                    Log.d(TAG, "Активная тренировка обновлена (ID: " + workout.getId() + ")");
                    adapter.updateExercises(workout.getExercises());

                    updateWorkoutStats(workout);
                } else {
                    Log.d(TAG, "Наблюдение: Активная тренировка равна null");
                    adapter.updateExercises(new ArrayList<>());

                    updateWorkoutStats(null);
                }
            });


            workoutViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {

            });


            workoutViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
                if (error != null && !error.isEmpty()) {
                    Log.e(TAG, error);
                }
            });


            workoutViewModel.getIsWorkoutCompleted().observe(getViewLifecycleOwner(), isCompleted -> {
                if (isCompleted) {
                    handleWorkoutCompletionUI();
                    Log.d(TAG, "Тренировка отмечена как завершенная");
                }
            });


            workoutViewModel.getRealTimeCalories().observe(getViewLifecycleOwner(), calories -> {

                if (workoutCaloriesText != null) {
                    workoutCaloriesText.setText(String.format(Locale.getDefault(), "%d", calories));
                }


                if (isWorkoutActive) {
                    caloriesManager.updateActiveWorkoutCalories(calories);
                }
            });
        }
    }

    private void handleWorkoutCompletionUI() {

        WorkoutSummaryDialogFragment summaryDialog = WorkoutSummaryDialogFragment.newInstance(
                completedWorkoutDuration,
                completedWorkoutCalories,
                completedWorkoutTonnage,
                completedWorkoutName
        );


        summaryDialog.show(getParentFragmentManager(), "WorkoutSummary");

        Log.d(TAG, "Показан WorkoutSummaryDialogFragment с итогами тренировки");


        if (workoutViewModel != null) {
            workoutViewModel.resetWorkoutCompletedFlag();
        }


        if (workoutTimer != null) {
            workoutTimer.cancel();
            workoutTimer = null;
        }

        isWorkoutActive = false;
        saveWorkoutState();


        startWorkoutButton.setVisibility(View.VISIBLE);
        finishWorkoutButton.setVisibility(View.GONE);


        totalExercisesText.setText("0");
        totalSetsText.setText("0");
        workoutCaloriesText.setText("0");
        workoutTimerText.setText("00:00:00");


        if (adapter != null) {
            adapter.updateExercises(new ArrayList<>());
        }


        if (caloriesManager != null) {
            caloriesManager.updateActiveWorkoutCalories(0);
        }


        completedWorkoutDuration = 0;
        completedWorkoutCalories = 0;
        completedWorkoutTonnage = 0;
        completedWorkoutName = "";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            Log.w(TAG, "onActivityResult: data is null");
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELECT_EXERCISE) {
                if (data.hasExtra("selected_exercise")) {
                    Exercise selectedExercise = data.getParcelableExtra("selected_exercise");
                    if (selectedExercise != null && userId != null) {
                        Log.d(TAG, "onActivityResult: Добавление упражнения '" + selectedExercise.getName() + "' напрямую из ExerciseSearchActivity.");

                        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
                        if (currentWorkout == null || currentWorkout.getEndTime() != null) {
                            Log.d(TAG, "onActivityResult (прямое добавление): Активная тренировка отсутствует или завершена. Создание новой.");
                            workoutViewModel.loadOrCreateActiveWorkout(userId);

                        }

                        workoutViewModel.addExercise(selectedExercise.getId(), userId, selectedExercise);
                    } else {
                        Log.w(TAG, "onActivityResult (прямое добавление): selectedExercise или userId is null.");
                    }
                } else if (data.getBooleanExtra("exercise_added_via_details", false)) {
                    String exerciseId = data.getStringExtra("exercise_id");
                    if (exerciseId != null && userId != null) {
                        Log.d(TAG, "onActivityResult: Добавление упражнения (ID: " + exerciseId + ") после просмотра деталей (через ExerciseSearchActivity).");

                        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
                        if (currentWorkout == null || currentWorkout.getEndTime() != null) {
                            Log.d(TAG, "onActivityResult (через детали): Активная тренировка отсутствует или завершена. Создание новой.");
                            workoutViewModel.loadOrCreateActiveWorkout(userId);

                        }
                        workoutViewModel.addExercise(exerciseId, userId);
                    } else {
                        Log.w(TAG, "onActivityResult (через детали): exerciseId или userId is null.");
                    }
                } else {
                    Log.w(TAG, "onActivityResult: REQUEST_CODE_SELECT_EXERCISE вернулся без 'selected_exercise' или 'exercise_added_via_details'.");
                }
            } else if (requestCode == REQUEST_CODE_CONFIGURE_EXERCISE) {
                Log.d(TAG, "onActivityResult: получен результат от ExerciseSettingsActivity, resultCode=" + resultCode);

                WorkoutExercise completedExercise = data.getParcelableExtra("completed_exercise");
                if (completedExercise != null) {
                    String exerciseName = completedExercise.getExercise() != null ?
                            completedExercise.getExercise().getName() : "Неизвестное упражнение";

                    Log.i(TAG, "onActivityResult: Получено выполненное упражнение из ExerciseSettingsActivity: " + exerciseName);

                    if (completedExercise.getSetsCompleted() == null || completedExercise.getSetsCompleted().isEmpty()) {
                        Log.w(TAG, "onActivityResult: подходы не найдены для упражнения " + exerciseName + " из ExerciseSettingsActivity");
                    } else {
                        for (ExerciseSet set : completedExercise.getSetsCompleted()) {
                            String durationInfo = "";
                            if (set.getDurationSeconds() != null && set.getDurationSeconds() > 0) {
                                int minutes = set.getDurationSeconds() / 60;
                                int seconds = set.getDurationSeconds() % 60;
                                durationInfo = ", длительность: " + minutes + " мин. " + seconds + " сек.";
                            }
                            Log.d(TAG, "onActivityResult (ExerciseSettings): подход #" + set.getSetNumber() +
                                    ", ID: " + set.getId() +
                                    ", выполнен: " + set.isCompleted() +
                                    durationInfo);
                        }
                    }

                    workoutViewModel.updateWorkoutExerciseSets(
                            completedExercise.getId(),
                            completedExercise.getSetsCompleted()
                    );
                    Log.i(TAG, "onActivityResult: Вызван метод ViewModel для обновления подходов для WorkoutExercise ID: " + completedExercise.getId() + " из ExerciseSettingsActivity");

                    if (userId != null) {
                        Log.d(TAG, "onActivityResult: Запуск принудительного расчета калорий после обновления подходов из ExerciseSettingsActivity");
                        workoutViewModel.calculateRealTimeCalories(userId);
                    }

                    workoutViewModel.saveCurrentWorkoutStateToDb();

                    UserWorkout currentWorkoutState = workoutViewModel.getActiveWorkout().getValue();
                    if (currentWorkoutState != null && currentWorkoutState.getExercises() != null) {
                        Log.d(TAG, "onActivityResult: Принудительное обновление адаптера после ExerciseSettingsActivity");
                        adapter.updateExercises(currentWorkoutState.getExercises());


                        boolean autoNextExercise = data.getBooleanExtra("auto_next_exercise", false);


                        boolean isPartOfSuperset = completedExercise.getSuperset_id() != null &&
                                !completedExercise.getSuperset_id().isEmpty();

                        if (autoNextExercise && !isPartOfSuperset) {
                            Log.d(TAG, "onActivityResult: Обнаружен флаг auto_next_exercise=true, ищем следующее упражнение");


                            int currentIndex;
                            List<WorkoutExercise> exercises = currentWorkoutState.getExercises();

                            currentIndex = IntStream.range(0, exercises.size()).filter(i -> exercises.get(i).getId().equals(completedExercise.getId())).findFirst().orElse(-1);


                            if (currentIndex >= 0 && currentIndex < exercises.size() - 1) {

                                WorkoutExercise nextExercise = exercises.get(currentIndex + 1);
                                Log.d(TAG, "onActivityResult: Выполняем автопереход к следующему упражнению: " +
                                        (nextExercise.getExercise() != null ? nextExercise.getExercise().getName() : "Неизвестное упражнение"));


                                new Handler().postDelayed(() -> {
                                    onExerciseClick(nextExercise, currentIndex + 1);
                                }, 300);
                            } else {
                                Log.d(TAG, "onActivityResult: Следующее упражнение не найдено или это последнее упражнение в тренировке");
                                Toast.makeText(requireContext(), "Вы завершили все упражнения!", Toast.LENGTH_SHORT).show();
                            }
                        } else if (isPartOfSuperset) {
                            Log.d(TAG, "onActivityResult: Упражнение является частью суперсета, автопереход отключен");
                        }
                    }


                } else {
                    Log.w(TAG, "onActivityResult: REQUEST_CODE_CONFIGURE_EXERCISE вернулся с null 'completed_exercise'");
                }
            }
        } else if (resultCode != Activity.RESULT_CANCELED) {
            Log.w(TAG, "onActivityResult: получен неожиданный resultCode=" + resultCode + " для requestCode=" + requestCode);
        }
    }

    @Override
    public void onExerciseClick(WorkoutExercise exercise, int position) {
        Log.d(TAG, "onExerciseClick: " + exercise.getExercise().getName() + ", position=" + position);


        Intent intent = new Intent(getActivity(), ExerciseSettingsActivity.class);
        intent.putExtra("exercise", exercise.getExercise());
        intent.putExtra("workout_exercise", exercise);
        intent.putExtra("workout_id", workoutViewModel.getActiveWorkoutId().getValue());


        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
        if (currentWorkout != null && currentWorkout.getExercises() != null) {
            ArrayList<WorkoutExercise> allExercises = new ArrayList<>(currentWorkout.getExercises());
            intent.putParcelableArrayListExtra("all_workout_exercises", allExercises);
            Log.d(TAG, "onExerciseClick: Передаем " + allExercises.size() + " упражнений для поддержки суперсетов");
        }

        startActivityForResult(intent, REQUEST_CODE_CONFIGURE_EXERCISE);
    }

    @Override
    public void onDeleteExercise(WorkoutExercise exercise, int position) {
        Log.d(TAG, "onDeleteExercise: " + exercise.getExercise().getName() + ", position=" + position);

        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить упражнение?")
                .setMessage("Вы уверены, что хотите удалить " + exercise.getExercise().getName() + "?")
                .setPositiveButton("Да", (dialog, which) -> {
                    workoutViewModel.removeExercise(exercise.getId());
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    public void onAddSuperset(WorkoutExercise exercise, int position) {

        openAddSupersetDialog(exercise, adapter.exercises, position);
    }

    @Override
    public void onAddExerciseNote(WorkoutExercise exercise, int position) {
        Log.d(TAG, "onAddExerciseNote: " + exercise.getExercise().getName() + ", position=" + position);
        openExerciseNotesFragment(exercise, position);
    }

    @Override
    public void onReplaceExercise(WorkoutExercise exercise, int position) {
        Log.d(TAG, "onReplaceExercise: " + exercise.getExercise().getName() + ", position=" + position);
        openReplaceExerciseDialog(exercise, position);
    }

    private void openAddSupersetDialog(WorkoutExercise exercise, List<WorkoutExercise> exercises, int position) {
        AddSupersetDialog.OnSupersetCreatedListener listener = (selectedExercises) -> {

            createSuperset(selectedExercises);
            Log.d(TAG, "Создан суперсет с " + selectedExercises.size() + " упражнениями");
        };
        AddSupersetDialog addSupersetDialog = new AddSupersetDialog(exercises, listener);
        addSupersetDialog.show(getChildFragmentManager(), "AddSuperset");
    }

    private void createSuperset(List<WorkoutExercise> selectedExercises) {
        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {

                VitaMoveApplication app = (VitaMoveApplication) requireActivity().getApplication();
                String supersetId = app.getWorkoutRepository().createSuperset(selectedExercises);


                getActivity().runOnUiThread(() -> {

                    updateExercisesModelsInMemory(selectedExercises, supersetId);


                    adapter.refreshSupersetDisplay();

                    Toast.makeText(requireContext(),
                            "Суперсет создан из " + selectedExercises.size() + " упражнений",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка создания суперсета", e);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Ошибка создания суперсета", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private void updateExercisesModelsInMemory(List<WorkoutExercise> selectedExercises, String supersetId) {
        if (selectedExercises == null || selectedExercises.isEmpty() || supersetId == null) return;


        for (int i = 0; i < selectedExercises.size(); i++) {
            WorkoutExercise selectedExercise = selectedExercises.get(i);


            for (WorkoutExercise adapterExercise : adapter.exercises) {
                if (adapterExercise.getId().equals(selectedExercise.getId())) {

                    adapterExercise.setSuperset_id(supersetId);
                    adapterExercise.setSuperset_order(i);

                    Log.d(TAG, "updateExercisesModelsInMemory: Обновлено " +
                            adapterExercise.getExercise().getName() + " - supersetId=" +
                            supersetId + ", order=" + i);
                    break;
                }
            }
        }


        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
        if (currentWorkout != null && currentWorkout.getExercises() != null) {
            for (int i = 0; i < selectedExercises.size(); i++) {
                WorkoutExercise selectedExercise = selectedExercises.get(i);

                for (WorkoutExercise workoutExercise : currentWorkout.getExercises()) {
                    if (workoutExercise.getId().equals(selectedExercise.getId())) {
                        workoutExercise.setSuperset_id(supersetId);
                        workoutExercise.setSuperset_order(i);
                        break;
                    }
                }
            }
        }
    }


    private void openExerciseNotesFragment(WorkoutExercise exercise, int position) {
        if (exercise == null || exercise.getExercise() == null) {
            Log.w(TAG, "openExerciseNotesFragment: упражнение или базовое упражнение не найдено");
            return;
        }

        ExerciseNotesFragment notesFragment = ExerciseNotesFragment.newInstance(exercise, position);


        try {
            Fragment parentFragment = getParentFragment();
            if (parentFragment != null && parentFragment.getParentFragmentManager() != null) {

                parentFragment.getParentFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, notesFragment, "ExerciseNotes")
                        .hide(this.getParentFragment())
                        .addToBackStack("ExerciseNotes")
                        .commit();

                Log.d(TAG, "openExerciseNotesFragment: Фрагмент заметок добавлен с сохранением состояния");
            } else {
                Log.e(TAG, "openExerciseNotesFragment: Не удалось получить ParentFragmentManager");
            }
        } catch (Exception e) {
            Log.e(TAG, "openExerciseNotesFragment: Ошибка при открытии фрагмента заметок", e);
        }
    }

    @Override
    public void onExerciseOrderChanged(List<WorkoutExercise> exercises) {
        if (workoutViewModel != null) {
            workoutViewModel.updateExerciseOrder(exercises);
        }
    }

    @Override
    public void onAddExerciseClick() {
        Intent intent = new Intent(getActivity(), ExerciseSearchActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_EXERCISE);
    }

    private void updateWorkoutStats(UserWorkout workout) {
        if (workout != null && workout.getExercises() != null) {
            int totalExercises = workout.getExercises().size();
            int totalSets = 0;


            for (WorkoutExercise workoutExercise : workout.getExercises()) {
                Exercise exercise = workoutExercise.getExercise();
                if (exercise == null) continue;

                if (exercise.isCardioExercise() || exercise.isStaticExercise()) {
                    totalSets += 1;
                } else {
                    int targetSets = exercise.getDefaultSets();
                    if (workoutExercise.getSetsCompleted() != null && !workoutExercise.getSetsCompleted().isEmpty()) {
                        totalSets += Math.max(targetSets, workoutExercise.getSetsCompleted().size());
                    } else {
                        totalSets += (targetSets > 0 ? targetSets : 3);
                    }
                }
            }


            totalExercisesText.setText(String.valueOf(totalExercises));
            totalSetsText.setText(String.valueOf(totalSets));


            if (isWorkoutActive && workout.getTotalCalories() > 0) {
                workoutCaloriesText.setText(String.valueOf(workout.getTotalCalories()));
            }
        } else {

            totalExercisesText.setText("0");
            totalSetsText.setText("0");
            workoutCaloriesText.setText("0");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        saveWorkoutState();


        if (isWorkoutActive && userId != null) {
            workoutViewModel.calculateRealTimeCalories(userId);


            Integer currentCalories = workoutViewModel.getRealTimeCalories().getValue();
            if (currentCalories != null && currentCalories > 0) {
                caloriesManager.updateActiveWorkoutCalories(currentCalories);
                Log.d(TAG, "onPause: Обновлены калории активной тренировки: " + currentCalories);
            }
        }

        Log.d(TAG, "onPause");
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
            Log.d(TAG, "Регистрация в EventBus в onStart - УСПЕШНО");
        } else {
            Log.d(TAG, "ActiveWorkoutFragment уже зарегистрирован в EventBus в onStart");
        }


        Log.d(TAG, "onStart: ActiveWorkoutFragment готов к получению EventBus событий");
    }

    @Override
    public void onStop() {
        super.onStop();


        if (isWorkoutActive && userId != null) {
            workoutViewModel.calculateRealTimeCalories(userId);


            Integer currentCalories = workoutViewModel.getRealTimeCalories().getValue();
            if (currentCalories != null && currentCalories > 0) {
                caloriesManager.updateActiveWorkoutCalories(currentCalories);
                Log.d(TAG, "onStop: Обновлены калории активной тренировки: " + currentCalories);
            }
        }


        if (workoutViewModel != null) {
            workoutViewModel.saveCurrentWorkoutStateToDb();
        }


        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
            Log.d(TAG, "Отписка от EventBus в onStop");
        }

        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (workoutTimer != null) {
            workoutTimer.cancel();
            workoutTimer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");


        checkWorkoutStateOnResume();


        if (isWorkoutActive && userId != null) {
            workoutViewModel.calculateRealTimeCalories(userId);
        }
    }


    private void checkWorkoutStateOnResume() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            boolean isWorkoutActiveInPrefs = prefs.getBoolean("is_workout_active", false);
            long workoutStartTimeInPrefs = prefs.getLong("workout_start_time", 0);

            Log.d(TAG, "checkWorkoutStateOnResume: Проверка состояния тренировки");
            Log.d(TAG, "isWorkoutActive (локально): " + isWorkoutActive);
            Log.d(TAG, "is_workout_active (SharedPrefs): " + isWorkoutActiveInPrefs);
            Log.d(TAG, "workout_start_time (SharedPrefs): " + workoutStartTimeInPrefs);


            if (isWorkoutActiveInPrefs && !isWorkoutActive && workoutStartTimeInPrefs > 0) {
                Log.i(TAG, "🔄 Обнаружена тренировка, запущенная извне! Синхронизируем состояние...");


                isWorkoutActive = true;
                workoutStartTime = workoutStartTimeInPrefs;


                UserWorkout workout = workoutViewModel != null ? workoutViewModel.getActiveWorkout().getValue() : null;
                if (workout == null || workout.getEndTime() != null) {

                    if (userId != null && workoutViewModel != null) {
                        Log.d(TAG, "checkWorkoutStateOnResume: Создаем новую тренировку для userId: " + userId);
                        workoutViewModel.loadOrCreateActiveWorkout(userId);
                    }
                }


                if (startWorkoutButton != null) {
                    startWorkoutButton.setVisibility(View.GONE);
                }
                if (finishWorkoutButton != null) {
                    finishWorkoutButton.setVisibility(View.VISIBLE);
                }


                startWorkoutTimer();
            }
        } catch (Exception e) {
            Log.e(TAG, "checkWorkoutStateOnResume: Ошибка при проверке состояния тренировки: " + e.getMessage(), e);
        }
    }


    private String formatElapsedTime(long elapsedTime) {
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddExerciseEvent(AddExerciseEvent event) {
        if (event == null || event.exerciseId == null) {
            Log.e(TAG, "Получено некорректное AddExerciseEvent или отсутствует exerciseId");
            return;
        }

        Log.d(TAG, "Получено AddExerciseEvent для exerciseId: " + event.exerciseId +
                (event.exercise != null ? " (с объектом)" : " (без объекта)"));

        if (userId == null) {
            Log.e(TAG, "userId is null, не могу добавить упражнение. Пользователь может быть не авторизован.");
            Toast.makeText(getContext(), "Ошибка: пользователь не авторизован.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (workoutViewModel == null) {
            Log.e(TAG, "workoutViewModel is null, не могу добавить упражнение.");

            return;
        }


        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
        if (currentWorkout == null || currentWorkout.getEndTime() != null) {

            Log.d(TAG, "onAddExerciseEvent: Активная тренировка отсутствует или завершена. Создание новой.");
            workoutViewModel.loadOrCreateActiveWorkout(userId);

        }

        Log.i(TAG, "Вызов workoutViewModel.addExercise с exerciseId: " + event.exerciseId + " и userId: " + userId);

        workoutViewModel.addExercise(event.exerciseId, userId, event.exercise);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWorkoutRepeatEvent(WorkoutRepeatEvent event) {
        if (event == null || event.getExercises() == null || event.getExercises().isEmpty()) {
            Log.e(TAG, "Получено некорректное WorkoutRepeatEvent или отсутствуют упражнения");
            return;
        }

        Log.d(TAG, "Получено WorkoutRepeatEvent с " + event.getExercises().size() + " упражнениями");

        if (userId == null) {
            Log.e(TAG, "userId is null, не могу повторить тренировку. Пользователь может быть не авторизован.");
            Toast.makeText(getContext(), "Ошибка: пользователь не авторизован.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (workoutViewModel == null) {
            Log.e(TAG, "workoutViewModel is null, не могу повторить тренировку.");
            return;
        }


        UserWorkout currentWorkout = workoutViewModel.getActiveWorkout().getValue();
        if (currentWorkout == null || currentWorkout.getEndTime() != null) {

            Log.d(TAG, "onWorkoutRepeatEvent: Активная тренировка отсутствует или завершена. Создание новой.");
            workoutViewModel.loadOrCreateActiveWorkout(userId);
        }


        for (WorkoutExercise workoutExercise : event.getExercises()) {
            if (workoutExercise.getExercise() != null && workoutExercise.getExercise().getId() != null) {
                String exerciseId = workoutExercise.getExercise().getId();
                Log.d(TAG, "Добавление упражнения из повторяемой тренировки: " + workoutExercise.getExercise().getName());
                workoutViewModel.addExercise(exerciseId, userId);
            }
        }

        Toast.makeText(getContext(), "Упражнения добавлены в тренировку", Toast.LENGTH_SHORT).show();
    }


    private void openReplaceExerciseDialog(WorkoutExercise exercise, int position) {
        if (exercise == null || exercise.getExercise() == null) {
            Log.w(TAG, "openReplaceExerciseDialog: упражнение или базовое упражнение не найдено");
            return;
        }

        Log.d(TAG, "Открытие диалога замены для упражнения: " + exercise.getExercise().getName());

        ReplaceExerciseDialog dialog = ReplaceExerciseDialog.newInstance(exercise);
        dialog.setOnExerciseSelectedListener(this);


        dialog.show(getChildFragmentManager(), "ReplaceExerciseDialog");

        Log.d(TAG, "openReplaceExerciseDialog: диалог показан, listener установлен");
    }


    @Override
    public void onExerciseSelected(Exercise selectedExercise, WorkoutExercise originalExercise) {
        Log.d(TAG, "onExerciseSelected: Заменяем " + originalExercise.getExercise().getName() +
                " на " + selectedExercise.getName());

        Log.d(TAG, "onExerciseSelected: workoutViewModel = " + (workoutViewModel != null ? "установлен" : "null"));
        Log.d(TAG, "onExerciseSelected: originalExercise.getId() = " + originalExercise.getId());
        Log.d(TAG, "onExerciseSelected: selectedExercise.getId() = " + selectedExercise.getId());

        if (workoutViewModel != null) {
            try {
                workoutViewModel.replaceExercise(originalExercise.getId(), selectedExercise);
                Toast.makeText(getContext(), "Упражнение заменено на " + selectedExercise.getName(),
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onExerciseSelected: замена выполнена успешно");
            } catch (Exception e) {
                Log.e(TAG, "onExerciseSelected: ошибка при замене упражнения", e);
                Toast.makeText(getContext(), "Ошибка при замене упражнения: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e(TAG, "workoutViewModel is null, не могу заменить упражнение");
            Toast.makeText(getContext(), "Ошибка при замене упражнения", Toast.LENGTH_SHORT).show();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWorkoutStartedEvent(WorkoutStartedEvent event) {
        Log.d(TAG, "========= ПОЛУЧЕНО СОБЫТИЕ WorkoutStartedEvent =========");

        if (event == null) {
            Log.e(TAG, "Получено некорректное WorkoutStartedEvent");
            return;
        }


        if (isWorkoutActive) {
            Log.d(TAG, "onWorkoutStartedEvent: Тренировка уже активна, игнорируем событие");
            return;
        }


        workoutStartTime = event.getStartTime();
        isWorkoutActive = true;


        UserWorkout workout = workoutViewModel != null ? workoutViewModel.getActiveWorkout().getValue() : null;
        if (workout == null || workout.getEndTime() != null) {

            if (userId != null && workoutViewModel != null) {
                Log.d(TAG, "onWorkoutStartedEvent: Создаем новую тренировку для userId: " + userId);
                workoutViewModel.loadOrCreateActiveWorkout(userId);
            } else {
                Log.e(TAG, "onWorkoutStartedEvent: userId или workoutViewModel is null");
                return;
            }
        }


        if (workout != null && workout.getId() != null && workoutViewModel != null) {
            try {

                new Thread(() -> {
                    try {
                        workoutViewModel.updateWorkoutStartTime(workout.getId(), workoutStartTime);
                        Log.i(TAG, "onWorkoutStartedEvent: Время начала тренировки обновлено: " + workoutStartTime);
                    } catch (Exception e) {
                        Log.e(TAG, "onWorkoutStartedEvent: Ошибка при обновлении времени начала тренировки: " + e.getMessage(), e);
                    }
                }).start();
            } catch (Exception e) {
                Log.e(TAG, "onWorkoutStartedEvent: Ошибка при запуске потока обновления времени: " + e.getMessage(), e);
            }
        }


        if (startWorkoutButton != null) {
            startWorkoutButton.setVisibility(View.GONE);
        }
        if (finishWorkoutButton != null) {
            finishWorkoutButton.setVisibility(View.VISIBLE);
        }


        startWorkoutTimer();


        saveWorkoutState();

        Log.i(TAG, "onWorkoutStartedEvent: Тренировка автоматически запущена из ExerciseSettings");
    }


} 