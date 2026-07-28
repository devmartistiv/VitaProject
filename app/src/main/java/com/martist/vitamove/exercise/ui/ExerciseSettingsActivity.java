package com.martist.vitamove.exercise.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.exercise.data.managers.ExerciseAdapterFactory;
import com.martist.vitamove.exercise.data.managers.ExerciseManager;
import com.martist.vitamove.exercise.data.managers.ExercisePreferencesManager;
import com.martist.vitamove.exercise.data.managers.ExerciseTimerManager;
import com.martist.vitamove.exercise.data.managers.ExerciseUIStateManager;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.set.CardioSetAdapter;
import com.martist.vitamove.set.ExerciseSetAdapter;
import com.martist.vitamove.set.RepsOnlySetAdapter;
import com.martist.vitamove.workout.data.managers.WorkoutSettingsManager;
import com.martist.vitamove.workout.data.model.WorkoutExercise;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.domain.WorkoutStartedEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExerciseSettingsActivity extends BaseActivity
        implements ExerciseTimerManager.TimerUpdateListener,
        ExerciseAdapterFactory.AdapterEventListener {
    private static final String TAG = "ExerciseSettingsActivity";
    private static final int DEFAULT_REPS = 12;

    private TextView exerciseNameText;
    private TextView restTimerText;
    private RecyclerView setsList;
    private MaterialButton completeSetButton;
    private View restTimerContainer;
    private View exerciseDescriptionContainer;
    private MaterialButton skipRestButton;
    private ProgressBar progressBar;
    private ExerciseSetAdapter adapter;
    private CardioSetAdapter cardioAdapter;
    private RepsOnlySetAdapter repsOnlyAdapter;
    private boolean isCardioExercise = false;
    private boolean isWarmupStretching = false;
    private View cardioSummaryView;
    private Exercise exercise;
    private WorkoutExercise workoutExercise;
    private String workoutId;
    private boolean isResting = false;
    private SupabaseWorkoutRepository workoutRepository;
    private View muscleGroupsContainer;
    private com.google.android.material.chip.ChipGroup muscleGroupsChipGroup;

    private ExerciseViewModel viewModel;
    private ExerciseManager exerciseManager;
    private Executor executor;
    private Handler mainHandler;

    private GestureDetectorCompat gestureDetector;
    private ConstraintLayout rootLayout;
    private TextView activeSetTimerText, warmupStretchingMessageText, cardioTotalMinutesText, exerciseDescriptionText;
    private View activeSetTimerContainer;
    private MaterialButton startSetButton;
    private boolean isSetActive = false;
    private ExerciseSet activeSet;


    private ExerciseTimerManager timerManager;
    private ExercisePreferencesManager preferencesManager;
    private ExerciseUIStateManager uiStateManager;


    private List<WorkoutExercise> allWorkoutExercises;
    private boolean isPartOfSuperset = false;
    private String currentSupersetId = null;
    private int currentSupersetOrder = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_settings);

        if (getIntent() != null) {
            exercise = getIntent().getParcelableExtra("exercise");
            workoutExercise = getIntent().getParcelableExtra("workout_exercise");
            workoutId = getIntent().getStringExtra("workout_id");


            allWorkoutExercises = getIntent().getParcelableArrayListExtra("all_workout_exercises");

            Log.d(TAG, String.format("onCreate: получены данные - workoutId=%s", workoutId));
            if (exercise != null) {
                Log.d(TAG, String.format("onCreate: exercise.id=%s, name=%s",
                        exercise.getId(), exercise.getName()));


                isCardioExercise = exercise.isCardioExercise() || exercise.isStaticExercise();


                isWarmupStretching = exercise.isWarmupOrStretching();

                Log.d(TAG, "onCreate: тип упражнения = " + exercise.getExerciseType() +
                        ", isCardioExercise = " + isCardioExercise +
                        ", isWarmupStretching = " + isWarmupStretching +
                        ", использует таймер = " + exercise.usesTimer());
            }
            if (workoutExercise != null) {
                Log.d(TAG, String.format("onCreate: workoutExercise.id=%s",
                        workoutExercise.getId()));


                currentSupersetId = workoutExercise.getSuperset_id();
                currentSupersetOrder = workoutExercise.getSuperset_order();
                isPartOfSuperset = currentSupersetId != null && !currentSupersetId.isEmpty();

                Log.d(TAG, "SUPERSET DEBUG: isPartOfSuperset = " + isPartOfSuperset +
                        " (superset_id: '" + currentSupersetId + "', superset_order: " + currentSupersetOrder + ")");

                if (allWorkoutExercises != null) {
                    Log.d(TAG, "SUPERSET DEBUG: allWorkoutExercises size: " + allWorkoutExercises.size());
                } else {
                    Log.d(TAG, "SUPERSET DEBUG: allWorkoutExercises is null");
                }
            }

            if (exercise == null || workoutExercise == null || workoutId == null) {
                Log.e(TAG, "onCreate: exercise, workoutExercise или workoutId равны null");
                Toast.makeText(this, "Ошибка: данные упражнения не найдены", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, String.format("onCreate: получено упражнение %s, ID тренировки %s",
                    exercise.getName(), workoutId));
        } else {
            Log.e(TAG, "onCreate: intent is null");
            finish();
            return;
        }

        try {

            exerciseManager = new ExerciseManager(this);
            executor = Executors.newSingleThreadExecutor();
            mainHandler = new Handler(Looper.getMainLooper());


            VitaMoveApplication app = (VitaMoveApplication) getApplication();
            workoutRepository = (SupabaseWorkoutRepository) app.getWorkoutRepository();
            Log.d(TAG, "onCreate: workoutRepository инициализирован для проверки подходов в суперсетах");


            initManagers();

            setupToolbar();
            initViews();


            removeAllCardioCirclesFromLayout();


            setupGestureDetector();


            if (progressBar == null) {
                Log.e(TAG, "onCreate: progressBar все еще null после initViews(), пытаемся инициализировать повторно");
                progressBar = findViewById(R.id.progress_bar);
            }

            setupViewModel();
            setupRecyclerView();
            setupClickListeners();
            updateUI();


            setupSupersetIndicator();


            if (exercise != null && exercise.getId() != null) {
                loadFullExerciseDetails(exercise.getId());
            } else {
                Log.e(TAG, "onCreate: Не удалось получить ID упражнения для загрузки деталей");
                Toast.makeText(this, "Ошибка: ID упражнения не найден", Toast.LENGTH_SHORT).show();
                finish();
            }


        } catch (Exception e) {
            Log.e(TAG, "onCreate: ошибка при инициализации: " + e.getMessage(), e);
            Toast.makeText(this, "Произошла ошибка при загрузке упражнения", Toast.LENGTH_SHORT).show();
            finish();
        }

    }


    private void initManagers() {
        Log.d(TAG, "initManagers: Инициализация менеджеров");


        timerManager = new ExerciseTimerManager(this);


        preferencesManager = new ExercisePreferencesManager(this);


        Log.d(TAG, "initManagers: Менеджеры успешно инициализированы");
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });


        ImageView infoButton = findViewById(R.id.info_button);
        infoButton.setOnClickListener(v -> {

            Intent intent = ExerciseDetailsActivity.newIntent(this, exercise.getId());

            intent.putExtra("hide_add_button", true);
            startActivity(intent);

            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });


        ImageView rutubeButton = findViewById(R.id.rutube_button);
        rutubeButton.setOnClickListener(v -> openRutubeSearch());
    }

    private void initViews() {
        exerciseNameText = findViewById(R.id.exercise_name);
        setsList = findViewById(R.id.sets_recycler);
        restTimerText = findViewById(R.id.rest_timer_text);
        completeSetButton = findViewById(R.id.complete_set_button);
        restTimerContainer = findViewById(R.id.rest_timer_container);
        skipRestButton = findViewById(R.id.skip_rest_button);
        progressBar = findViewById(R.id.progress_bar);
        exerciseDescriptionText = findViewById(R.id.exercise_description);
        exerciseDescriptionContainer = findViewById(R.id.exercise_description_container);
        warmupStretchingMessageText = findViewById(R.id.warmup_stretching_message);
        activeSetTimerText = findViewById(R.id.active_set_timer_text);
        activeSetTimerContainer = findViewById(R.id.active_set_timer_container);
        startSetButton = findViewById(R.id.start_set_button);
        muscleGroupsContainer = findViewById(R.id.muscle_groups_container);
        muscleGroupsChipGroup = findViewById(R.id.muscle_groups_chip_group);
        rootLayout = findViewById(R.id.root_layout);

        View cardioInfoCard = findViewById(R.id.cardio_info_card);
        if (cardioInfoCard != null) {
            cardioInfoCard.setVisibility(isCardioExercise ? View.VISIBLE : View.GONE);
        }

        View exerciseDescriptionHeader = findViewById(R.id.exercise_description_header);
        exerciseDescriptionHeader.setOnClickListener(v -> {
            Intent intent = ExerciseDetailsActivity.newIntent(this, exercise.getId());
            intent.putExtra("hide_add_button", true);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });


        uiStateManager = new ExerciseUIStateManager(
                restTimerContainer,
                activeSetTimerContainer,
                completeSetButton,
                startSetButton,
                setsList
        );
    }

    private void setupViewModel() {
        try {
            viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);


            viewModel.initialize(exercise, workoutExercise, workoutId);


            viewModel.getExerciseSets().observe(this, sets -> {
                if (isCardioExercise) {
                    if (cardioAdapter != null) {


                        if (sets != null && !sets.isEmpty()) {

                            List<ExerciseSet> uncompletedSets = new ArrayList<>();
                            for (ExerciseSet set : sets) {
                                if (!set.isCompleted()) {
                                    uncompletedSets.add(set);
                                }
                            }


                            if (uncompletedSets.size() > 1) {
                                Log.d(TAG, "setupViewModel: Обнаружено несколько незавершенных подходов для кардио (" +
                                        uncompletedSets.size() + "), оставляем только первый");


                                List<ExerciseSet> updatedSets = new ArrayList<>();
                                boolean foundFirstUncompleted = false;
                                List<String> idsToDelete = new ArrayList<>();


                                for (ExerciseSet set : sets) {
                                    if (set.isCompleted()) {
                                        updatedSets.add(set);
                                    } else if (!foundFirstUncompleted) {

                                        updatedSets.add(set);
                                        foundFirstUncompleted = true;
                                    } else {

                                        if (set.getId() != null) {
                                            idsToDelete.add(set.getId());
                                        }
                                    }
                                }


                                for (String id : idsToDelete) {
                                    viewModel.deleteSet(id);
                                }


                                cardioAdapter.updateSets(updatedSets);


                                updateCardioTotalTime();
                                return;
                            }
                        }


                        cardioAdapter.updateSets(sets);
                        updateCardioTotalTime();
                    } else {
                        Log.e(TAG, "setupViewModel: cardioAdapter is null when updating sets");
                    }
                } else if (repsOnlyAdapter != null) {

                    repsOnlyAdapter.updateSets(sets);
                    Log.d(TAG, "setupViewModel: repsOnlyAdapter обновлен с " + (sets != null ? sets.size() : 0) + " подходами");
                } else if (adapter != null) {

                    adapter.updateSets(sets);
                } else {
                    Log.e(TAG, "setupViewModel: ни один адаптер не инициализирован при обновлении подходов");
                }


                WorkoutExercise currentExercise = viewModel.getWorkoutExercise().getValue();
                if (currentExercise != null) {
                    workoutExercise = currentExercise;
                }


                updateUI();
            });


            viewModel.getIsLoading().observe(this, isLoading -> {
                if (progressBar != null) {
                    progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                } else {
                    Log.e(TAG, "progressBar is null when setting visibility");
                }
            });


            viewModel.getErrorMessage().observe(this, error -> {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
            });


            viewModel.getIsResting().observe(this, resting -> {
                if (this.isResting != resting) {
                    this.isResting = resting;
                    updateRestTimerUI();


                    if (completeSetButton != null) {
                        completeSetButton.setEnabled(!resting);
                    } else {
                        Log.e(TAG, "completeSetButton is null when updating rest state");
                    }


                    updateUI();
                }
            });


            viewModel.getRestTimeRemaining().observe(this, timeRemaining -> {
                if (timeRemaining > 0) {
                    updateRestTimer(timeRemaining);
                }
            });

            Log.d(TAG, "setupViewModel: наблюдатели установлены успешно");
        } catch (Exception e) {
            Log.e(TAG, "setupViewModel: ошибка при инициализации ViewModel: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка инициализации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        setsList.setLayoutManager(new LinearLayoutManager(this));

        Log.d(TAG, "setupRecyclerView: exerciseType = " + (exercise != null ? exercise.getExerciseType() : "null"));


        if (isWarmupStretching) {
            setsList.setVisibility(View.GONE);
            return;
        }


        if (isCardioExercise) {


            addCardioSummaryView();


            cardioAdapter = ExerciseAdapterFactory.createCardioAdapter(new ExerciseAdapterFactory.AdapterEventListener() {
                @Override
                public void onSetClick(ExerciseSet set, int position, boolean isCompleted) {
                    ExerciseSettingsActivity.this.onSetClick(set, position, isCompleted);
                }

                @Override
                public void onDeleteClick(ExerciseSet set, int position) {
                    ExerciseSettingsActivity.this.onDeleteClick(set, position);
                }

                @Override
                public void onDataChange(ExerciseSet set, int position) {

                    ExerciseSettingsActivity.this.onDataChange(set, position);
                    updateCardioTotalTime();
                }
            });

            setsList.setAdapter(cardioAdapter);
            ensureCardioSetsValidity();
            updateCardioTotalTime();
        } else if (exercise != null && exercise.usesRepsOnly()) {
            repsOnlyAdapter = ExerciseAdapterFactory.createRepsOnlyAdapter(this);
            setsList.setAdapter(repsOnlyAdapter);
        } else {
            adapter = ExerciseAdapterFactory.createRegularAdapter(this);
            setsList.setAdapter(adapter);
        }
    }


    private void ensureCardioSetsValidity() {
        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
        if (sets == null || sets.isEmpty()) {
            ExerciseSet newSet = new ExerciseSet(null, null, 0, false);
            newSet.setDurationSeconds(0);
            addNewSet(newSet);
            Log.d(TAG, "ensureCardioSetsValidity: Добавлен подход для кардио");
            return;
        }

        if (sets.size() <= 1) return;

        int uncompletedCount = countUncompletedSets(sets);
        if (uncompletedCount <= 1) return;

        Log.d(TAG, "ensureCardioSetsValidity: Найдено " + uncompletedCount + " незавершенных подходов, оставляем один");

        List<ExerciseSet> updatedSets = new ArrayList<>();
        boolean foundFirstUncompleted = false;

        for (ExerciseSet set : sets) {
            if (set.isCompleted() || !foundFirstUncompleted) {
                updatedSets.add(set);
                if (!set.isCompleted()) foundFirstUncompleted = true;
            } else if (set.getId() != null) {
                viewModel.deleteSet(set.getId());
            }
        }

        viewModel.setSets(updatedSets);
    }


    private void addCardioSummaryView() {

        if (cardioSummaryView != null) {

            removeCardioSummaryView();
        }


        ViewGroup cardioSummaryContainer = findViewById(R.id.cardio_summary_container);
        if (cardioSummaryContainer == null) {
            Log.e(TAG, "addCardioSummaryView: контейнер cardio_summary_container не найден");
            return;
        }


        cardioSummaryContainer.setVisibility(View.VISIBLE);


        cardioSummaryView = getLayoutInflater().inflate(R.layout.cardio_summary_view, cardioSummaryContainer, false);


        cardioTotalMinutesText = cardioSummaryView.findViewById(R.id.cardio_total_minutes);


        cardioSummaryContainer.removeAllViews();


        cardioSummaryContainer.addView(cardioSummaryView);
        Log.d(TAG, "addCardioSummaryView: Добавлено представление суммарного времени кардио в новый контейнер");


        updateCardioTotalTime();
    }


    private void removeCardioSummaryView() {

        if (cardioSummaryView != null) {
            ViewGroup parent = (ViewGroup) cardioSummaryView.getParent();
            if (parent != null) {
                parent.removeView(cardioSummaryView);
                Log.d(TAG, "removeCardioSummaryView: Удалено представление суммарного времени кардио");
            }
            cardioSummaryView = null;
            cardioTotalMinutesText = null;
        }


        ViewGroup cardioSummaryContainer = findViewById(R.id.cardio_summary_container);
        if (cardioSummaryContainer != null) {
            cardioSummaryContainer.setVisibility(View.GONE);
        }
    }


    private void updateCardioTotalTime() {
        if (cardioTotalMinutesText == null || !isCardioExercise) {
            return;
        }

        int totalSeconds = 0;
        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();

        if (sets != null && !sets.isEmpty()) {
            for (ExerciseSet set : sets) {
                if (set.getDurationSeconds() != null) {

                    totalSeconds += set.getDurationSeconds();
                }
            }
        }


        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;


        cardioTotalMinutesText.setText(String.valueOf(minutes));


        TextView cardioSecondsLabel = null;
        if (cardioSummaryView != null) {
            cardioSecondsLabel = cardioSummaryView.findViewById(R.id.cardio_seconds_value);

            if (cardioSecondsLabel != null) {

                cardioSecondsLabel.setText(String.format(Locale.getDefault(), "%02d", seconds));
            }
        }

        Log.d(TAG, "updateCardioTotalTime: Обновлено общее время кардио: " + minutes + " мин. " + seconds + " сек.");
    }


    private void addNewSet(ExerciseSet set) {
        Log.d(TAG, "addNewSet: Добавление нового подхода" + (isCardioExercise ? " кардио" : " силового") +
                ", тип упражнения: " + (exercise != null ? exercise.getExerciseType() : "null"));

        if (exercise != null && exercise.getId() != null) {

            List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
            Log.d(TAG, "addNewSet: Текущее количество подходов: " + (currentSets != null ? currentSets.size() : 0));


            if (isCardioExercise) {
                if (currentSets != null && !currentSets.isEmpty()) {

                    boolean hasUncompletedSets = false;
                    for (ExerciseSet existingSet : currentSets) {
                        if (!existingSet.isCompleted()) {
                            hasUncompletedSets = true;
                            break;
                        }
                    }


                    if (hasUncompletedSets) {
                        Log.d(TAG, "addNewSet: Пропускаем добавление нового подхода кардио, так как есть незавершенные");
                        Toast.makeText(this, "Завершите текущий подход перед добавлением нового", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }


                set.setDurationSeconds(0);
                set.setReps(null);
                set.setWeight(null);
                Log.d(TAG, "addNewSet: Настроены параметры кардио для нового подхода");


                int nextSetNumber = (currentSets != null && !currentSets.isEmpty()) ?
                        currentSets.size() + 1 : 1;
                set.setSetNumber(nextSetNumber);


                viewModel.addNewSet(set);


                if (cardioAdapter != null && currentSets != null) {
                    List<ExerciseSet> updatedSets = new ArrayList<>(currentSets);
                    updatedSets.add(set);
                    cardioAdapter.updateSets(updatedSets);
                }

                Log.d(TAG, "addNewSet: Добавлен новый подход #" + nextSetNumber +
                        " для кардио-упражнения");


                updateCardioTotalTime();


                Log.d(TAG, "addNewSet: UI обновится через observer после добавления кардио-подхода");
            } else {

                Log.d(TAG, "addNewSet: Добавление нового подхода для силового упражнения");


                boolean hasUncompletedSets = false;
                if (currentSets != null && !currentSets.isEmpty()) {
                    for (ExerciseSet existingSet : currentSets) {
                        if (!existingSet.isCompleted()) {
                            hasUncompletedSets = true;
                            Log.d(TAG, "addNewSet: Найден незавершенный подход: " + existingSet.getId());
                            break;
                        }
                    }

                    if (hasUncompletedSets) {
                        Log.d(TAG, "addNewSet: Пропускаем добавление нового подхода, так как есть незавершенный подход");
                        Toast.makeText(this, "Завершите текущий подход перед добавлением нового", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }


                int nextSetNumber = (currentSets != null && !currentSets.isEmpty()) ?
                        currentSets.size() + 1 : 1;
                set.setSetNumber(nextSetNumber);


                Log.d(TAG, "addNewSet: Добавляю новый подход #" + nextSetNumber +
                        " для силового упражнения через ViewModel");
                viewModel.addNewSet(set);


                Log.d(TAG, "addNewSet: UI обновится через observer после добавления силового подхода");
            }
        } else {
            Log.e(TAG, "addNewSet: exercise или exercise.getId() равен null");
            Toast.makeText(this, "Ошибка: не удалось добавить подход, информация об упражнении отсутствует", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSet(ExerciseSet set, int position) {

        if (set == null || set.getId() == null) {
            Log.e(TAG, "deleteSet: set или set.getId() равны null, удаление невозможно");
            Toast.makeText(this, "Ошибка: не удалось удалить подход (ID не найден)", Toast.LENGTH_SHORT).show();
            return;
        }


        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        if (currentSets != null && currentSets.size() <= 1) {
            Log.d(TAG, "deleteSet: Попытка удалить последний подход, операция запрещена");
            Toast.makeText(this, "Нельзя удалить последний подход", Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            String setIdToDelete = set.getId();


            Log.d(TAG, "deleteSet: Удаление подхода с ID " + setIdToDelete);
            viewModel.deleteSet(setIdToDelete);


            if (set.equals(activeSet)) {
                stopActiveSetTimer();
                activeSet = null;
            }


            if (isCardioExercise && cardioAdapter != null) {

                List<ExerciseSet> updatedSets = viewModel.getExerciseSets().getValue();
                if (updatedSets != null) {
                    cardioAdapter.updateSets(new ArrayList<>(updatedSets));
                    updateCardioTotalTime();
                }
            } else if (repsOnlyAdapter != null) {
                List<ExerciseSet> updatedSets = viewModel.getExerciseSets().getValue();
                if (updatedSets != null) {
                    repsOnlyAdapter.updateSets(new ArrayList<>(updatedSets));
                }
            } else if (adapter != null) {
                List<ExerciseSet> updatedSets = viewModel.getExerciseSets().getValue();
                if (updatedSets != null) {
                    adapter.updateSets(new ArrayList<>(updatedSets));
                }
            }


            updateUI();


            Toast.makeText(this, "Подход удален", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "deleteSet: Ошибка при удалении подхода: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка при удалении подхода: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        Log.d(TAG, "setupClickListeners: Настройка обработчиков кликов");


        startSetButton.setOnClickListener(v -> startActiveSet());


        completeSetButton.setOnClickListener(v -> {
            if (isWarmupStretching) {

                Log.d(TAG, "Нажата кнопка 'Завершить упражнение' для разминки/растяжки");
                finishExercise();
            } else {

                completeActiveSet();
            }
        });
        skipRestButton.setOnClickListener(v -> skipRest());
    }


    private void startActiveSet() {

        checkAndStartWorkoutIfNeeded();

        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        boolean allCompleted = false;
        if (isCardioExercise && currentSets != null && !currentSets.isEmpty()) {
            allCompleted = true;
            for (ExerciseSet set : currentSets) {
                if (!set.isCompleted()) {
                    allCompleted = false;
                    break;
                }
            }
        }

        ExerciseSet setToActivate = null;


        if (isCardioExercise && allCompleted) {
            Log.d(TAG, "startActiveSet: Все подходы завершены, добавляем и активируем новый подход кардио/статический");

            ExerciseSet newSet = new ExerciseSet(null, null, 0, false);
            newSet.setDurationSeconds(0);
            newSet.setWorkoutExerciseId(workoutExercise.getId());

            int nextSetNumber = (currentSets != null && !currentSets.isEmpty()) ? currentSets.size() + 1 : 1;
            newSet.setSetNumber(nextSetNumber);


            viewModel.addNewSet(newSet);


            setToActivate = newSet;
            Log.d(TAG, "startActiveSet: Новый подход создан и помечен для активации. Ожидаемый номер: " + nextSetNumber);

        } else {

            if (currentSets != null) {
                for (ExerciseSet set : currentSets) {
                    if (!set.isCompleted()) {
                        setToActivate = set;
                        Log.d(TAG, "startActiveSet: Найден существующий незавершенный подход для активации. Номер: " + setToActivate.getSetNumber());
                        break;
                    }
                }
            }
        }


        if (setToActivate == null) {
            Log.e(TAG, "startActiveSet: Не удалось определить подход для активации.");
            Toast.makeText(this, "Не удалось начать подход.", Toast.LENGTH_SHORT).show();
            return;
        }


        activeSet = setToActivate;
        isSetActive = true;


        if (isCardioExercise) {

            int initialSeconds = 0;
            if (setToActivate.getDurationSeconds() != null) {
                initialSeconds = setToActivate.getDurationSeconds();
                Log.d(TAG, "startActiveSet: Найдено уже введенное время: " + initialSeconds + " сек.");
            }


            timerManager.setActiveSetStartTime(System.currentTimeMillis() - (initialSeconds * 1000L));


            startSetButton.setVisibility(View.GONE);


            activeSetTimerContainer.setVisibility(View.GONE);
            completeSetButton.setVisibility(View.VISIBLE);


            if (cardioSummaryView != null) {
                cardioSummaryView.setVisibility(View.VISIBLE);
                updateCardioTotalTime();
            }

            if (exercise.isStaticExercise()) {
                Log.d(TAG, "startActiveSet: Запущен подход для статического упражнения с начальным временем " + initialSeconds + " сек.");
            } else {
                Log.d(TAG, "startActiveSet: Запущен подход для кардио-упражнения с начальным временем " + initialSeconds + " сек.");
            }
        } else {

            timerManager.setActiveSetStartTime(System.currentTimeMillis());
            startSetButton.setVisibility(View.GONE);
            activeSetTimerContainer.setVisibility(View.VISIBLE);
            completeSetButton.setVisibility(View.VISIBLE);
        }


        startActiveSetTimer();
    }


    private void startActiveSetTimer() {
        timerManager.startActiveSetTimer();
        Log.d(TAG, "startActiveSetTimer: Таймер активного подхода запущен через менеджер");
    }


    private void updateCardioActiveTimer(long elapsedTimeMillis) {

        int totalSeconds = (int) (elapsedTimeMillis / 1000);


        if (activeSet != null) {

            Integer previousValue = activeSet.getDurationSeconds();


            activeSet.setDurationSeconds(totalSeconds);


            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;


            updateCardioTotalTime();


            if (activeSetTimerText != null && activeSetTimerText.getVisibility() == View.VISIBLE) {
                String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                activeSetTimerText.setText(timeFormatted);
            }

            Log.d(TAG, "updateCardioActiveTimer: Обновлено время кардио: " + minutes + " мин. " + seconds + " сек.");
        }
    }


    private void updateActiveSetTimer(long elapsedTimeMillis) {

        timerManager.setActiveSetDuration(elapsedTimeMillis);


        if (activeSet != null && !activeSet.isCompleted()) {
            int seconds = (int) (elapsedTimeMillis / 1000);
            activeSet.setDurationSeconds(seconds);
            viewModel.updateSet(activeSet);
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis) -
                TimeUnit.MINUTES.toSeconds(minutes);


        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        activeSetTimerText.setText(timeFormatted);
    }


    private void completeActiveSet() {
        if (activeSet == null) {

            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
            if (sets != null) {
                for (ExerciseSet set : sets) {
                    if (!set.isCompleted()) {
                        activeSet = set;
                        break;
                    }
                }
            } else {
                Log.e(TAG, "completeActiveSet: sets is null");
                return;
            }


            if (activeSet == null) {
                Log.e(TAG, "completeActiveSet: activeSet is null");
                Toast.makeText(this, "Ошибка: не найден активный подход", Toast.LENGTH_SHORT).show();
                return;
            }
        }


        timerManager.cancelActiveSetTimer();


        long elapsedTimeMillis = timerManager.getActiveSetDuration();


        if (isCardioExercise) {

            int totalSeconds = (int) (elapsedTimeMillis / 1000);


            activeSet.setDurationSeconds(totalSeconds);
            activeSet.setCompleted(true);


            viewModel.updateSet(activeSet);


            updateCardioTotalTime();

            Log.d(TAG, "completeActiveSet: Завершен кардио-подход, длительность: " +
                    totalSeconds + " сек.");


            resetActiveSet();


            boolean isStaticExercise = exercise != null && exercise.isStaticExercise();


            if (isStaticExercise) {
                Toast.makeText(this, "Удержание завершено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Подход кардио завершен", Toast.LENGTH_SHORT).show();
            }
        } else {

            timerManager.setActiveSetDuration(elapsedTimeMillis);
            Log.d(TAG, "completeActiveSet: Установлено время для силового подхода: " +
                    (int) (timerManager.getActiveSetDuration() / 1000) + " сек.");
            completeSet();
        }


        if (isCardioExercise) {

            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();


            if (areAllSetsCompleted(sets)) {
                Log.d(TAG, "completeActiveSet: Все подходы кардио завершены, возвращаемся к тренировке");

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    finishExercise();
                }, 1000);
            }
        }
    }

    private void completeSet() {
        try {

            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();

            if (sets == null || sets.isEmpty()) {
                Log.d(TAG, "completeSet: список подходов пуст или null");
                return;
            }

            int position = -1;
            for (int i = 0; i < sets.size(); i++) {
                if (!sets.get(i).isCompleted()) {
                    position = i;
                    break;
                }
            }

            if (position != -1) {

                ExerciseSet originalSet = sets.get(position);


                if (originalSet.getId() == null || originalSet.getId().isEmpty()) {
                    Log.e(TAG, "completeSet: Невозможно завершить подход - ID не существует. Позиция: " + position);
                    Toast.makeText(this, "Ошибка: ID подхода не найден", Toast.LENGTH_SHORT).show();
                    return;
                }


                Integer duration = originalSet.getDurationSeconds();
                if (timerManager.getActiveSetDuration() > 0) {
                    duration = (int) (timerManager.getActiveSetDuration() / 1000);
                    Log.d(TAG, "completeSet: устанавливаем длительность подхода: " + duration + " сек.");
                } else if (duration == null || duration <= 0) {
                    duration = 0;

                }


                if (isCardioExercise) {
                    if (duration == null || duration <= 0) {


                        if (originalSet.getDurationSeconds() != null && originalSet.getDurationSeconds() > 0) {
                            duration = originalSet.getDurationSeconds();
                            Log.d(TAG, "completeSet: для кардио используем длительность из оригинального подхода: " + duration + " сек.");
                        } else {

                            int totalMinutes = 0;
                            try {
                                if (cardioTotalMinutesText != null) {
                                    totalMinutes = Integer.parseInt(cardioTotalMinutesText.getText().toString());
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "completeSet: ошибка парсинга времени кардио: " + e.getMessage());
                            }

                            TextView cardioSecondsLabel = null;
                            int totalSeconds = 0;
                            if (cardioSummaryView != null) {
                                cardioSecondsLabel = cardioSummaryView.findViewById(R.id.cardio_seconds_value);
                                if (cardioSecondsLabel != null) {
                                    try {
                                        totalSeconds = Integer.parseInt(cardioSecondsLabel.getText().toString());
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "completeSet: ошибка парсинга секунд кардио: " + e.getMessage());
                                    }
                                }
                            }

                            duration = totalMinutes * 60 + totalSeconds;
                            Log.d(TAG, "completeSet: для кардио используем время из UI: " + totalMinutes + " мин " + totalSeconds + " сек = " + duration + " сек.");
                        }
                    }
                }

                ExerciseSet updatedSet = new ExerciseSet(
                        originalSet.getId(),
                        originalSet.getWeight(),
                        originalSet.getReps(),
                        true,
                        originalSet.getWorkoutExerciseId(),
                        originalSet.getSetNumber()
                );


                updatedSet.setDurationSeconds(duration);

                Log.d(TAG, "completeSet: завершаем подход " + position + ", ID: " + updatedSet.getId() + ", длительность: " + duration + " сек.");


                if (updatedSet != null && updatedSet.getId() != null && !updatedSet.getId().isEmpty()) {

                    boolean isFirstCompletedSet = true;
                    for (ExerciseSet set : sets) {
                        if (set.isCompleted() && !set.getId().equals(updatedSet.getId())) {
                            isFirstCompletedSet = false;
                            break;
                        }
                    }


                    if (isFirstCompletedSet) {
                        checkAndStartWorkoutIfNeeded();
                    }


                    viewModel.updateSet(updatedSet);


                    if (isCardioExercise) {
                        updateCardioTotalTime();
                    }
                } else {
                    Log.e(TAG, "completeSet: Невозможно обновить подход - null или отсутствует ID. Позиция: " + position);
                    Toast.makeText(this, "Ошибка: не удалось обновить данные подхода", Toast.LENGTH_SHORT).show();
                    return;
                }


                resetActiveSet();


                boolean hasMoreUncompleted = false;
                for (int i = position + 1; i < sets.size(); i++) {
                    if (!sets.get(i).isCompleted()) {
                        hasMoreUncompleted = true;
                        break;
                    }
                }


                if (isCardioExercise) {

                    boolean wasRatedBefore = viewModel.isExerciseRated(workoutExercise.getId());

                    if (!wasRatedBefore) {
                        Log.d(TAG, "completeSet: Кардио-подход выполнен, переход к оценке самочувствия");


                        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
                        if (currentSets != null && !currentSets.isEmpty()) {
                            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));


                            for (int i = 0; i < currentSets.size(); i++) {
                                ExerciseSet s = currentSets.get(i);
                                Log.d(TAG, "completeSet: Синхронизация кардио-подход #" + (i + 1) +
                                        ": ID=" + s.getId() +
                                        ", completed=" + s.isCompleted() +
                                        ", duration=" + s.getDurationSeconds() + " сек.");
                            }
                        }

                        finishExercise();
                    } else {
                        Log.d(TAG, "completeSet: Кардио-подход выполнен, возврат к списку упражнений");


                        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
                        if (currentSets != null && !currentSets.isEmpty()) {
                            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));


                            for (int i = 0; i < currentSets.size(); i++) {
                                ExerciseSet s = currentSets.get(i);
                                Log.d(TAG, "completeSet: Синхронизация кардио-подход #" + (i + 1) +
                                        ": ID=" + s.getId() +
                                        ", completed=" + s.isCompleted() +
                                        ", duration=" + s.getDurationSeconds() + " сек.");
                            }
                        }

                        Toast.makeText(this, "Кардио упражнение выполнено!", Toast.LENGTH_SHORT).show();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("completed_exercise", workoutExercise);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                } else {


                    if (isPartOfSuperset && allWorkoutExercises != null && !allWorkoutExercises.isEmpty()) {
                        Log.d(TAG, "SUPERSET DEBUG: Упражнение является частью суперсета, проверяем переход");


                        checkSupersetTransitionAsync(hasMoreUncompleted);
                        return;
                    }


                    if (hasMoreUncompleted) {

                        boolean restTimerEnabled = WorkoutSettingsManager.getInstance(this).isRestTimerEnabled();

                        if (restTimerEnabled) {
                            startRestTimer();
                        } else {
                            Log.d(TAG, "completeSet: Таймер отдыха отключен в настройках, пропускаем");

                            isResting = false;
                            updateRestTimerUI();
                            updateUI();
                        }
                    } else {

                        boolean wasRatedBefore = viewModel.isExerciseRated(workoutExercise.getId());

                        if (!wasRatedBefore) {

                            Log.d(TAG, "completeSet: Все подходы выполнены, переход к оценке самочувствия");
                            finishExercise();
                        } else {

                            Log.d(TAG, "completeSet: Все подходы выполнены, но упражнение уже оценено ранее");


                            List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
                            if (currentSets != null && !currentSets.isEmpty()) {

                                Log.d(TAG, "completeSet: Синхронизация данных подходов с workoutExercise");
                                workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));


                                for (int i = 0; i < currentSets.size(); i++) {
                                    ExerciseSet set = currentSets.get(i);
                                    Log.d(TAG, "completeSet: Подход #" + (i + 1) + ": completed=" + set.isCompleted());
                                }
                            }

                            Toast.makeText(this, "Все подходы выполнены!", Toast.LENGTH_SHORT).show();

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("completed_exercise", workoutExercise);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    }
                }
            } else {
                Log.d(TAG, "completeSet: нет невыполненных подходов");
            }
        } catch (Exception e) {
            Log.e(TAG, "completeSet: ошибка при завершении подхода: " + e.getMessage(), e);
            Toast.makeText(this, "Произошла ошибка при завершении подхода", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRestTimer() {
        try {

            int restTimerSeconds = WorkoutSettingsManager.getInstance(this).getRestTimerSeconds();
            long restTimeMillis = restTimerSeconds * 1000L;


            viewModel.startRest(restTimeMillis);

            Log.d(TAG, "startRestTimer: Запущен таймер отдыха на " + restTimerSeconds + " сек");
        } catch (Exception e) {
            Log.e(TAG, "startRestTimer: ошибка при запуске таймера: " + e.getMessage(), e);

            isResting = false;
            updateRestTimerUI();
            updateUI();
        }
    }


    private void updateRestTimer(long millisUntilFinished) {
        try {
            if (restTimerText == null) {
                Log.e(TAG, "updateRestTimer: restTimerText is null");
                return;
            }


            int minutes = (int) (millisUntilFinished / 1000) / 60;
            int seconds = (int) (millisUntilFinished / 1000) % 60;


            String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            restTimerText.setText(timeFormatted);

            Log.d(TAG, "updateRestTimer: обновлено время отдыха: " + timeFormatted);
        } catch (Exception e) {
            Log.e(TAG, "updateRestTimer: ошибка при обновлении времени: " + e.getMessage(), e);
        }
    }

    private void skipRest() {
        try {

            timerManager.cancelRestTimer();


            viewModel.endRest();


            if (isResting) {
                isResting = false;
                updateRestTimerUI();
                updateUI();
            }
        } catch (Exception e) {
            Log.e(TAG, "skipRest: ошибка при завершении отдыха: " + e.getMessage(), e);
            isResting = false;
            updateRestTimerUI();
            updateUI();
        }
    }


    private void updateRestTimerUI() {
        try {
            if (restTimerContainer == null || skipRestButton == null) {
                Log.e(TAG, "updateRestTimerUI: restTimerContainer или skipRestButton равны null");
                return;
            }


            restTimerContainer.setVisibility(isResting ? View.VISIBLE : View.GONE);
            skipRestButton.setVisibility(isResting ? View.VISIBLE : View.GONE);


            if (startSetButton != null) {
                startSetButton.setVisibility(isResting ? View.GONE : View.VISIBLE);
            }


            if (activeSetTimerContainer != null) {
                activeSetTimerContainer.setVisibility(isResting ? View.GONE :
                        (isSetActive ? View.VISIBLE : View.GONE));
            }


            if (completeSetButton != null) {
                completeSetButton.setVisibility(isResting ? View.GONE :
                        (isSetActive ? View.VISIBLE : View.GONE));
            }

            Log.d(TAG, "updateRestTimerUI: UI таймера отдыха обновлен, isResting=" + isResting);
        } catch (Exception e) {
            Log.e(TAG, "updateRestTimerUI: ошибка при обновлении UI таймера: " + e.getMessage(), e);
        }
    }

    private void updateUI() {
        if (exercise == null) return;
        Log.d(TAG, "updateUI: Обновление UI для " + exercise.getName());

        updateExerciseBasicInfo();


        if (isWarmupStretching) {
            updateUIForWarmupStretching();
            return;
        }


        if (warmupStretchingMessageText != null) {
            warmupStretchingMessageText.setVisibility(View.GONE);
        }

        updateUIForRegularExercise();
    }

    private void finishExercise() {

        if (isWarmupStretching) {
            Log.d(TAG, "finishExercise: Завершение разминки/растяжки.");


            List<ExerciseSet> existingSets = viewModel.getExerciseSets().getValue();
            boolean hasCompletedSets = false;

            if (existingSets != null && !existingSets.isEmpty()) {

                for (ExerciseSet set : existingSets) {
                    if (set.isCompleted()) {
                        hasCompletedSets = true;
                        break;
                    }
                }
            }


            if (!hasCompletedSets) {
                ExerciseSet dummySet = new ExerciseSet();
                dummySet.setWorkoutExerciseId(workoutExercise.getId());
                dummySet.setCompleted(true);
                dummySet.setReps(1);
                dummySet.setSetNumber(1);


                viewModel.addNewSet(dummySet);


                if (workoutExercise.getSetsCompleted() == null) {
                    workoutExercise.setSetsCompleted(new ArrayList<>());
                }
                workoutExercise.getSetsCompleted().add(dummySet);

                Log.d(TAG, "finishExercise: Создан фиктивный подход для разминки/растяжки с ID упражнения: " + workoutExercise.getId());
            } else {
                Log.d(TAG, "finishExercise: Разминка/растяжка уже имеет завершенные подходы, пропускаем создание фиктивного");
            }


            workoutExercise.setRated(true);
            workoutExercise.setCompleted(true);


            completeSetButton.setClickable(false);
            completeSetButton.setText("ВЫПОЛНЕНО");
            completeSetButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green_500)));


            completeSetButton.setIconResource(R.drawable.ic_check);
            completeSetButton.setIconTint(ColorStateList.valueOf(Color.WHITE));
            completeSetButton.setIconGravity(MaterialButton.ICON_GRAVITY_START);
            completeSetButton.setIconPadding(16);


            Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            pulse.setDuration(500);
            completeSetButton.startAnimation(pulse);


            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                Log.d(TAG, "finishExercise: Возвращаем результат для разминки/растяжки, workoutExercise ID: " + workoutExercise.getId() +
                        ", completed: " + workoutExercise.isCompleted() +
                        ", rated: " + workoutExercise.isRated());

                Intent resultIntent = new Intent();
                resultIntent.putExtra("completed_exercise", workoutExercise);
                setResult(RESULT_OK, resultIntent);
                finish();
            }, 1000);

            return;
        }


        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        if (currentSets != null && !currentSets.isEmpty()) {

            for (ExerciseSet set : currentSets) {
                if (!set.isCompleted()) {
                    Log.w(TAG, "finishExercise: обнаружен невыполненный подход #" + set.getSetNumber() +
                            ", ID: " + set.getId() + " - принудительно устанавливаем его статус выполнения");

                    set.setCompleted(true);


                    if (isCardioExercise && (set.getDurationSeconds() == null || set.getDurationSeconds() <= 0)) {

                        int totalMinutes = 0;
                        int totalSeconds = 0;
                        try {
                            if (cardioTotalMinutesText != null) {
                                totalMinutes = Integer.parseInt(cardioTotalMinutesText.getText().toString());
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "finishExercise: ошибка парсинга времени кардио: " + e.getMessage());
                        }

                        TextView cardioSecondsLabel = null;
                        if (cardioSummaryView != null) {
                            cardioSecondsLabel = cardioSummaryView.findViewById(R.id.cardio_seconds_value);
                            if (cardioSecondsLabel != null) {
                                try {
                                    totalSeconds = Integer.parseInt(cardioSecondsLabel.getText().toString());
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "finishExercise: ошибка парсинга секунд кардио: " + e.getMessage());
                                }
                            }
                        }

                        int duration = totalMinutes * 60 + totalSeconds;
                        if (duration > 0) {
                            set.setDurationSeconds(duration);
                            Log.d(TAG, "finishExercise: для невыполненного кардио устанавливаем время из UI: " +
                                    totalMinutes + " мин " + totalSeconds + " сек = " + duration + " сек.");
                        } else {

                            set.setDurationSeconds(60);
                            Log.d(TAG, "finishExercise: для невыполненного кардио устанавливаем минимальное время: 60 сек.");
                        }
                    }


                    viewModel.updateSet(set);
                }
            }


            currentSets = viewModel.getExerciseSets().getValue();


            Log.d(TAG, "finishExercise: Синхронизация данных подходов с workoutExercise. Количество: " + currentSets.size());
            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));


            for (int i = 0; i < currentSets.size(); i++) {
                ExerciseSet set = currentSets.get(i);
                Log.d(TAG, "finishExercise: Подход #" + (i + 1) + " (ID: " + set.getId() + ") - completed: " +
                        set.isCompleted() + ", duration: " + set.getDurationSeconds() + " сек.");
            }
        }


        boolean enableAutoNextExercise = WorkoutSettingsManager.getInstance(this).isAutoNextExerciseEnabled();
        Log.d(TAG, "finishExercise: Проверка настроек автоперехода. Автопереход включен: " + enableAutoNextExercise);


        Intent resultIntent = new Intent();


        Log.d(TAG, "finishExercise: Подготовка к возврату результата. Количество подходов в workoutExercise: " +
                (workoutExercise.getSetsCompleted() != null ? workoutExercise.getSetsCompleted().size() : "null"));
        if (workoutExercise.getSetsCompleted() != null) {
            for (int i = 0; i < workoutExercise.getSetsCompleted().size(); i++) {
                ExerciseSet set = workoutExercise.getSetsCompleted().get(i);
                Log.d(TAG, "finishExercise: Подход #" + (i + 1) + " (ID: " + set.getId() + ") - completed: " +
                        set.isCompleted() + ", duration: " + set.getDurationSeconds() + " сек.");
            }
        }

        resultIntent.putExtra("completed_exercise", workoutExercise);


        if (enableAutoNextExercise) {

            resultIntent.putExtra("auto_next_exercise", true);
            Log.d(TAG, "finishExercise: Добавлен флаг auto_next_exercise=true для автоперехода");
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {

        if (isWarmupStretching) {
            boolean hasCompletedSet = false;
            List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();

            if (sets != null && !sets.isEmpty()) {
                for (ExerciseSet set : sets) {
                    if (set.isCompleted()) {
                        hasCompletedSet = true;
                        break;
                    }
                }
            }


            if (hasCompletedSet) {
                Log.d(TAG, "onBackPressed: Разминка/растяжка имеет завершенные подходы, завершаем как выполненное");
                workoutExercise.setRated(true);
                workoutExercise.setCompleted(true);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("completed_exercise", workoutExercise);
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }

        }


        if (isCardioExercise) {
            boolean autoCompleted = autoCompleteCardioSets();
            if (autoCompleted) {
                Log.d(TAG, "onBackPressed: Были автоматически завершены подходы кардио с введенными данными");

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Log.e(TAG, "onBackPressed: ошибка задержки: " + e.getMessage());
                }
            }
        }


        boolean hasChanges = workoutExercise.getSetsCompleted().stream()
                .anyMatch(ExerciseSet::isCompleted);


        boolean allCompleted = workoutExercise.getSetsCompleted().stream()
                .allMatch(ExerciseSet::isCompleted) && !workoutExercise.getSetsCompleted().isEmpty();


        boolean wasRatedBefore = viewModel.isExerciseRated(workoutExercise.getId());


        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        if (currentSets != null && !currentSets.isEmpty()) {

            Log.d(TAG, "onBackPressed: Синхронизация данных подходов с workoutExercise");
            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));


            for (int i = 0; i < currentSets.size(); i++) {
                ExerciseSet set = currentSets.get(i);
                Log.d(TAG, "onBackPressed: Подход #" + (i + 1) + ": completed=" + set.isCompleted());
            }
        }

        if (hasChanges) {


            if (wasRatedBefore) {
                Log.d(TAG, "onBackPressed: Упражнение уже было оценено ранее, просто сохраняем прогресс");
            } else {
                Log.d(TAG, "onBackPressed: Автоматическое сохранение выполненных подходов без перехода к оценке");
            }

            Intent resultIntent = new Intent();


            Log.d(TAG, "onBackPressed: Подготовка к возврату результата. Количество подходов в workoutExercise: " +
                    (workoutExercise.getSetsCompleted() != null ? workoutExercise.getSetsCompleted().size() : "null"));
            if (workoutExercise.getSetsCompleted() != null) {
                for (int i = 0; i < workoutExercise.getSetsCompleted().size(); i++) {
                    ExerciseSet set = workoutExercise.getSetsCompleted().get(i);
                    Log.d(TAG, "onBackPressed: Подход #" + (i + 1) + " (ID: " + set.getId() + ") - completed: " + set.isCompleted());
                }
            }

            resultIntent.putExtra("completed_exercise", workoutExercise);
            setResult(RESULT_OK, resultIntent);
            finish();

        } else {

            Log.d(TAG, "onBackPressed: Выход без сохранения (нет выполненных подходов)");
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        if (timerManager != null) {
            timerManager.cleanup();
        }

        Log.d(TAG, "onDestroy: Ресурсы освобождены");
    }


    @Override
    public void onRestTimerUpdate(long remainingMillis) {
        if (restTimerText != null) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis);
            long minutes = seconds / 60;
            seconds = seconds % 60;
            restTimerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        }
    }


    @Override
    public void onRestTimerFinish() {
        isResting = false;

        if (restTimerText != null) {
            restTimerText.setText("00:00");
        }

        if (restTimerContainer != null) {
            restTimerContainer.setVisibility(View.GONE);
        }


        updateUI();

        Log.d(TAG, "onRestTimerFinish: Отдых завершен");
    }


    @Override
    public void onActiveSetTimerUpdate(long elapsedMillis) {

        if (isCardioExercise) {
            updateCardioActiveTimer(elapsedMillis);
        } else {
            updateActiveSetTimer(elapsedMillis);
        }
    }


    @Override
    public void onSetClick(ExerciseSet set, int position, boolean isCompleted) {
        Log.d(TAG, "onSetClick: Подход #" + (position + 1) + " обновлен на " + isCompleted);
        set.setCompleted(isCompleted);
        viewModel.updateSet(set);
    }


    @Override
    public void onDeleteClick(ExerciseSet set, int position) {
        Log.d(TAG, "onDeleteClick: Запрос на удаление подхода #" + (position + 1));
        deleteSet(set, position);
    }


    @Override
    public void onDataChange(ExerciseSet set, int position) {
        Log.d(TAG, "onDataChange: Сохранение изменений подхода #" + (position + 1));
        viewModel.updateSet(set);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity paused");


        if (adapter != null) {
            adapter.savePendingChangesIfAny();
            Log.d(TAG, "onPause: Called savePendingChangesIfAny on adapter");
        }

        if (cardioAdapter != null) {


        }
        if (repsOnlyAdapter != null) {


        }


        try {
            String exerciseId = workoutExercise.getId();


            timerManager.cancelAllTimers();


            if (isSetActive) {
                long elapsedTime = timerManager.getActiveSetDuration();
                long startTime = timerManager.getActiveSetStartTime();
                preferencesManager.saveActiveSetState(exerciseId, true, startTime, elapsedTime);
                Log.d(TAG, "onPause: Сохранен активный подход, продолжительность: " + elapsedTime + " мс");
            } else {
                preferencesManager.saveActiveSetState(exerciseId, false, 0, 0);
            }


            boolean isRestingNow = viewModel.getIsResting().getValue() != null && viewModel.getIsResting().getValue();
            if (isRestingNow) {
                Long remainingTime = viewModel.getRestTimeRemaining().getValue();
                if (remainingTime != null && remainingTime > 0) {
                    preferencesManager.saveRestingState(exerciseId, true, remainingTime);
                    Log.d(TAG, "onPause: Сохранен статус отдыха, оставшееся время: " + remainingTime + " мс");
                } else {
                    preferencesManager.saveRestingState(exerciseId, false, 0);
                }
            } else {
                preferencesManager.saveRestingState(exerciseId, false, 0);
            }

            Log.d(TAG, "onPause: Состояние сохранено через PreferencesManager");
        } catch (Exception e) {
            Log.e(TAG, "onPause: ошибка при сохранении состояния: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {

            if (isWarmupStretching) {
                handleWarmupStretchingResume();
                return;
            }


            updateUI();


            if (isCardioExercise) {
                handleCardioResume();
            }


            restoreStateFromPreferences();


            updateUI();

            Log.d(TAG, "onResume: Состояние восстановлено");
        } catch (Exception e) {
            Log.e(TAG, "onResume: ошибка при восстановлении состояния: " + e.getMessage(), e);
            resetState();
        }
    }


    private void resetState() {
        try {
            isResting = false;
            isSetActive = false;


            timerManager.cancelAllTimers();


            updateRestTimerUI();
            updateUI();

            Log.d(TAG, "resetState: Состояние сброшено после ошибки");
        } catch (Exception e) {
            Log.e(TAG, "resetState: ошибка при сбросе состояния: " + e.getMessage(), e);
        }
    }


    private void setupMuscleGroupChips() {
        if (exercise == null) {
            Log.e(TAG, "setupMuscleGroupChips: exercise is null");
            return;
        }


        com.google.android.material.chip.ChipGroup chipGroup = muscleGroupsChipGroup;
        if (chipGroup == null) {
            chipGroup = findViewById(R.id.muscle_groups_chip_group);

            muscleGroupsChipGroup = chipGroup;
        }

        if (chipGroup == null) {
            Log.e(TAG, "setupMuscleGroupChips: chip group is null");
            return;
        }


        List<String> muscleGroupRussianNames = exercise.getMuscleGroupRussianNames();
        if (muscleGroupRussianNames == null || muscleGroupRussianNames.isEmpty()) {
            Log.w(TAG, "setupMuscleGroupChips: список групп мышц пуст");
            chipGroup.removeAllViews();


            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText("Группы мышц не указаны");
            chip.setTextColor(getResources().getColor(R.color.gray_500));
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeWidth(1);
            chip.setChipStrokeColorResource(R.color.gray_500);
            chipGroup.addView(chip);
            return;
        }

        Log.d(TAG, "setupMuscleGroupChips: найдено " + muscleGroupRussianNames.size() + " групп мышц");


        chipGroup.removeAllViews();


        for (String muscleGroupRussianName : muscleGroupRussianNames) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(muscleGroupRussianName);


            chip.setTextColor(getResources().getColor(R.color.orange_500));
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeWidth(1);
            chip.setChipStrokeColorResource(R.color.orange_500);
            chip.setTextSize(14);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipMinHeight(36);


            chipGroup.addView(chip);
        }


        chipGroup.requestLayout();
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        gestureDetector.onTouchEvent(event);


        return super.dispatchTouchEvent(event);
    }


    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener());


        if (rootLayout != null) {
            rootLayout.setOnTouchListener((v, event) -> {

                gestureDetector.onTouchEvent(event);
                return false;
            });
        } else {
            Log.e(TAG, "setupGestureDetector: rootLayout is null");
        }


        if (setsList != null) {
            setsList.setOnTouchListener((v, event) -> {

                boolean handled = gestureDetector.onTouchEvent(event);

                return handled;
            });
        }
    }


    private void openExerciseDetails() {

        if (exercise != null) {

            Intent intent = ExerciseDetailsActivity.newIntent(this, exercise.getId());

            intent.putExtra("hide_add_button", true);


            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            Log.e(TAG, "openExerciseDetails: exercise is null");
        }
    }


    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();


                if (Math.abs(diffX) > Math.abs(diffY)) {

                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD && diffX < 0) {

                        openExerciseDetails();
                        return true;
                    }
                }
            } catch (Exception exception) {
                Log.e(TAG, "onFling: Error processing gesture: " + exception.getMessage());
            }
            return false;
        }
    }


    private void loadFullExerciseDetails(String exerciseId) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            Log.e(TAG, "loadFullExerciseDetails: progressBar is null перед показом");
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "loadFullExerciseDetails: Загрузка данных для упражнения ID: " + exerciseId);

                Exercise fullExercise = exerciseManager.getExerciseById(exerciseId);

                mainHandler.post(() -> {
                    if (fullExercise != null) {
                        Log.d(TAG, "loadFullExerciseDetails: Данные успешно загружены для " + fullExercise.getName());

                        this.exercise = fullExercise;

                        updateUI();
                    } else {
                        Log.e(TAG, "loadFullExerciseDetails: Упражнение с ID " + exerciseId + " не найдено ExerciseManager-ом");
                        Toast.makeText(this, R.string.error_exercise_not_found, Toast.LENGTH_SHORT).show();


                    }

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        Log.e(TAG, "loadFullExerciseDetails: progressBar is null перед скрытием");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "loadFullExerciseDetails: Ошибка при загрузке упражнения ID " + exerciseId + ": " + e.getMessage(), e);
                mainHandler.post(() -> {
                    Toast.makeText(this, getString(R.string.error_loading_exercise) + "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }


                });
            }
        });
    }


    private void handleWarmupStretchingResume() {
        Log.d(TAG, "handleWarmupStretchingResume: Специальная обработка для разминки/растяжки");


        boolean wasCompleted = workoutExercise.isCompleted() || workoutExercise.isRated();
        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();

        if (sets != null && !sets.isEmpty()) {
            for (ExerciseSet set : sets) {
                if (set.isCompleted()) {
                    wasCompleted = true;
                    break;
                }
            }
        }


        if (wasCompleted) {
            Log.d(TAG, "handleWarmupStretchingResume: Разминка/растяжка уже была выполнена");
            workoutExercise.setCompleted(true);
            workoutExercise.setRated(true);
        }

        updateUI();
    }


    private void handleCardioResume() {
        Log.d(TAG, "handleCardioResume: Обновление данных для кардио-упражнения");


        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();


        if (hasUncompletedSets(sets) && !isSetActive && !isResting && startSetButton != null) {
            Log.d(TAG, "handleCardioResume: Обнаружены незавершенные подходы");
            startSetButton.setVisibility(View.VISIBLE);
            startSetButton.invalidate();
        }


        syncCardioSetsWithWorkoutExercise();
    }


    private void syncCardioSetsWithWorkoutExercise() {
        List<ExerciseSet> currentSets = workoutExercise.getSetsCompleted();
        if (currentSets == null || currentSets.isEmpty()) {
            Log.d(TAG, "syncCardioSetsWithWorkoutExercise: Сохраненные подходы не найдены");
            return;
        }

        Log.d(TAG, "syncCardioSetsWithWorkoutExercise: Найдено " + currentSets.size() + " сохраненных подходов");

        List<ExerciseSet> viewModelSets = viewModel.getExerciseSets().getValue();
        boolean needsUpdate = viewModelSets == null || viewModelSets.isEmpty();

        if (!needsUpdate && viewModelSets.size() > 0 && currentSets.size() > 0) {

            Integer vmDuration = viewModelSets.get(0).getDurationSeconds();
            Integer savedDuration = currentSets.get(0).getDurationSeconds();

            if ((vmDuration == null && savedDuration != null) ||
                    (vmDuration != null && savedDuration == null) ||
                    (vmDuration != null && savedDuration != null && !vmDuration.equals(savedDuration))) {
                needsUpdate = true;
                Log.d(TAG, "syncCardioSetsWithWorkoutExercise: Несоответствие времени - viewModel=" + vmDuration + ", saved=" + savedDuration);
            }
        }

        if (needsUpdate) {
            Log.d(TAG, "syncCardioSetsWithWorkoutExercise: Обновляем подходы из сохраненных данных");
            viewModel.setSets(new ArrayList<>(currentSets));

            if (cardioAdapter != null) {
                cardioAdapter.updateSets(currentSets);
            }
            updateCardioTotalTime();
        }
    }


    private void restoreStateFromPreferences() {
        String exerciseId = workoutExercise.getId();
        ExercisePreferencesManager.SavedState savedState = preferencesManager.loadState(exerciseId);

        if (savedState.isResting) {
            restoreRestingState(savedState);
        } else {
            restoreActiveSetState(savedState);
        }
    }


    private void restoreRestingState(ExercisePreferencesManager.SavedState savedState) {
        long remainingTime = savedState.restTime;

        if (remainingTime > 1000) {
            viewModel.startRest(remainingTime);
            Log.d(TAG, "restoreRestingState: Восстановлен таймер отдыха, осталось: " + remainingTime + " мс");
        } else {
            viewModel.endRest();
            Log.d(TAG, "restoreRestingState: Завершен отдых, осталось мало времени");
        }
    }


    private void restoreActiveSetState(ExercisePreferencesManager.SavedState savedState) {
        if (!savedState.isSetActive) return;

        long savedDuration = savedState.activeSetDuration;

        if (savedDuration > 0) {
            isSetActive = true;
            timerManager.resumeActiveSetTimer(savedDuration);
            Log.d(TAG, "restoreActiveSetState: Восстановлен активный подход, прошло: " + savedDuration + " мс");
        }
    }


    private void updateExerciseBasicInfo() {

        exerciseNameText.setText(exercise.getName());


        setupMuscleGroupChips();


        String instructions = exercise.getInstructions();
        boolean hasInstructions = instructions != null && !instructions.isEmpty();

        if (hasInstructions) {
            exerciseDescriptionText.setText(instructions);
            exerciseDescriptionContainer.setVisibility(View.VISIBLE);
            exerciseDescriptionText.setVisibility(View.GONE);
            findViewById(R.id.exercise_description_header).setClickable(true);
            findViewById(R.id.toggle_description_button).setVisibility(View.VISIBLE);
        } else {
            exerciseDescriptionContainer.setVisibility(View.GONE);
            findViewById(R.id.exercise_description_header).setClickable(false);
            findViewById(R.id.toggle_description_button).setVisibility(View.GONE);
        }
    }


    private void updateUIForWarmupStretching() {
        Log.d(TAG, "updateUIForWarmupStretching: Применяем UI для разминки/растяжки");

        setsList.setVisibility(View.GONE);

        if (warmupStretchingMessageText != null) {
            warmupStretchingMessageText.setVisibility(View.VISIBLE);
            warmupStretchingMessageText.setText("Сделайте столько, сколько считаете нужным");
        } else {
            Log.e(TAG, "updateUIForWarmupStretching: warmupStretchingMessageText is null!");
        }

        if (cardioSummaryView != null) {
            cardioSummaryView.setVisibility(View.GONE);
        }


        restTimerContainer.setVisibility(View.GONE);
        activeSetTimerContainer.setVisibility(View.GONE);
        startSetButton.setVisibility(View.GONE);


        boolean isExerciseCompleted = isWarmupStretchingCompleted();

        if (isExerciseCompleted) {
            uiStateManager.applyState(ExerciseUIStateManager.UIState.WARMUP_COMPLETED);
            Log.d(TAG, "updateUIForWarmupStretching: Разминка/растяжка выполнена");
        } else {
            uiStateManager.applyState(ExerciseUIStateManager.UIState.WARMUP_ACTIVE);
            Log.d(TAG, "updateUIForWarmupStretching: Разминка/растяжка активна");
        }

        completeSetButton.setVisibility(View.VISIBLE);
        Log.d(TAG, "updateUIForWarmupStretching: UI настроен для разминки/растяжки");
    }


    private boolean isWarmupStretchingCompleted() {

        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
        if (sets != null && !sets.isEmpty()) {
            for (ExerciseSet set : sets) {
                if (set.isCompleted()) {
                    return true;
                }
            }
        }


        return workoutExercise != null && workoutExercise.isRated();
    }


    private void updateUIForRegularExercise() {

        setsList.setVisibility(View.VISIBLE);


        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();
        updateAdaptersWithSets(sets);


        updateButtonsAndTimersVisibility(sets);
    }


    private void updateAdaptersWithSets(List<ExerciseSet> sets) {
        if (sets == null) return;

        Log.d(TAG, "updateAdaptersWithSets: Обновление адаптера с " + sets.size() + " подходами");

        if (isCardioExercise && cardioAdapter != null) {
            cardioAdapter.updateSets(sets);
            updateCardioTotalTime();
        } else if (repsOnlyAdapter != null) {
            repsOnlyAdapter.updateSets(sets);
        } else if (adapter != null) {
            adapter.updateSets(sets);
        }
    }


    private void updateButtonsAndTimersVisibility(List<ExerciseSet> sets) {
        if (isResting) {
            uiStateManager.applyState(ExerciseUIStateManager.UIState.RESTING);
        } else if (isSetActive) {
            uiStateManager.applyState(ExerciseUIStateManager.UIState.ACTIVE_SET);
        } else {

            uiStateManager.applyState(ExerciseUIStateManager.UIState.IDLE);


            if (!isCardioExercise && areAllSetsCompleted(sets)) {
                uiStateManager.setStartButtonVisible(false);
                Log.d(TAG, "updateButtonsAndTimersVisibility: Скрываем кнопку, все подходы завершены");
            } else {
                uiStateManager.setStartButtonVisible(true);
                uiStateManager.setStartButtonText(exercise, isCardioExercise);
                Log.d(TAG, "updateButtonsAndTimersVisibility: Показываем кнопку 'Начать подход'");
            }
        }
    }


    private void toggleDescriptionVisibility() {
        if (exerciseDescriptionText != null) {
            if (exerciseDescriptionText.getVisibility() == View.VISIBLE) {
                exerciseDescriptionText.setVisibility(View.GONE);
            } else {
                exerciseDescriptionText.setVisibility(View.VISIBLE);
            }
        }
    }


    private boolean areAllSetsCompleted(List<ExerciseSet> sets) {
        if (sets == null || sets.isEmpty()) {
            return true;
        }

        for (ExerciseSet set : sets) {
            if (!set.isCompleted()) {
                return false;
            }
        }
        return true;
    }


    private boolean hasUncompletedSets(List<ExerciseSet> sets) {
        return !areAllSetsCompleted(sets);
    }


    private int countUncompletedSets(List<ExerciseSet> sets) {
        if (sets == null || sets.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ExerciseSet set : sets) {
            if (!set.isCompleted()) {
                count++;
            }
        }
        return count;
    }


    private void openRutubeSearch() {
        if (exercise == null || exercise.getName() == null || exercise.getName().isEmpty()) {
            Log.e(TAG, "openRutubeSearch: Название упражнения не найдено");
            Toast.makeText(this, "Не удалось получить название упражнения для поиска", Toast.LENGTH_SHORT).show();
            return;
        }

        String exerciseName = exercise.getName();

        String searchQuery = "техника выполнения упражнения " + exerciseName;
        String query = Uri.encode(searchQuery);
        String rutubeAppPackage = "ru.rutube.app";
        String rutubeSearchUrl = "https://rutube.ru/search/?query=" + query;

        try {

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("rutube://search?query=" + query));
            intent.setPackage(rutubeAppPackage);
            startActivity(intent);
            Log.d(TAG, "openRutubeSearch: Попытка открыть поиск '" + searchQuery + "' в приложении Rutube");
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "openRutubeSearch: Приложение Rutube не найдено, открываем в браузере");

            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(rutubeSearchUrl));

                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(browserIntent);
            } catch (ActivityNotFoundException browserException) {
                Log.e(TAG, "openRutubeSearch: Не найден браузер для открытия URL");
                Toast.makeText(this, "Не найден браузер для открытия ссылки", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void resetActiveSet() {
        isSetActive = false;
        timerManager.setActiveSetStartTime(0);
        timerManager.setActiveSetDuration(0);
        activeSet = null;

        timerManager.cancelActiveSetTimer();


        startSetButton.setVisibility(View.VISIBLE);
        activeSetTimerContainer.setVisibility(View.GONE);
        completeSetButton.setVisibility(View.GONE);


        activeSetTimerText.setText("00:00");


        updateUI();
    }


    private void checkAndStartWorkoutIfNeeded() {
        try {


            SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
            boolean isWorkoutActive = prefs.getBoolean("is_workout_active", false);

            if (!isWorkoutActive) {
                Log.d(TAG, "checkAndStartWorkoutIfNeeded: Тренировка не активна, запускаем автоматически");


                long workoutStartTime = System.currentTimeMillis();


                if (EventBus.getDefault().hasSubscriberForEvent(WorkoutStartedEvent.class)) {
                    Log.d(TAG, "checkAndStartWorkoutIfNeeded: Найдены подписчики на WorkoutStartedEvent, отправляем событие");
                } else {
                    Log.w(TAG, "checkAndStartWorkoutIfNeeded: НЕТ подписчиков на WorkoutStartedEvent! Событие может быть потеряно");
                }

                WorkoutStartedEvent event = new WorkoutStartedEvent(workoutStartTime);
                EventBus.getDefault().post(event);
                Log.d(TAG, "checkAndStartWorkoutIfNeeded: WorkoutStartedEvent отправлен с временем: " + workoutStartTime);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_workout_active", true);
                editor.putLong("workout_start_time", workoutStartTime);
                editor.apply();


                Toast.makeText(this, "Тренировка автоматически началась!", Toast.LENGTH_SHORT).show();


            }
        } catch (Exception e) {
            Log.e(TAG, "checkAndStartWorkoutIfNeeded: Ошибка при проверке/запуске тренировки: " + e.getMessage(), e);
        }
    }


    private void stopActiveSetTimer() {
        timerManager.cancelActiveSetTimer();


        isSetActive = false;


        if (activeSetTimerContainer != null) {
            activeSetTimerContainer.setVisibility(View.GONE);
        }

        if (completeSetButton != null) {
            completeSetButton.setVisibility(View.GONE);
        }


        if (startSetButton != null) {
            startSetButton.setVisibility(View.VISIBLE);
        }
    }


    private void removeAllCardioCirclesFromLayout() {

        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView instanceof ViewGroup) {

            findAndRemoveCircleViews((ViewGroup) rootView);
            Log.d(TAG, "removeAllCardioCirclesFromLayout: Выполнен поиск и удаление элементов cardio_time_circle");
        }
    }


    private void findAndRemoveCircleViews(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);


            if (child instanceof ViewGroup) {
                findAndRemoveCircleViews((ViewGroup) child);
            }
        }
    }


    private boolean autoCompleteCardioSets() {
        if (!isCardioExercise) {
            return false;
        }

        boolean wasAutoCompleted = false;
        List<ExerciseSet> sets = viewModel.getExerciseSets().getValue();

        if (sets != null && !sets.isEmpty()) {
            for (ExerciseSet set : sets) {

                if (!set.isCompleted() && set.getDurationSeconds() != null && set.getDurationSeconds() > 0) {

                    Log.d(TAG, "autoCompleteCardioSets: Автоматическое завершение подхода кардио с ID: " +
                            set.getId() + ", time: " + set.getDurationSeconds() + " сек.");


                    set.setCompleted(true);


                    viewModel.updateSet(set);
                    wasAutoCompleted = true;
                }
            }
        }

        return wasAutoCompleted;
    }


    private void setupSupersetIndicator() {
        if (isPartOfSuperset && exerciseNameText != null) {

            String supersetLetter = getSupersetLetter(currentSupersetOrder);
            String currentName = exerciseNameText.getText().toString();


            if (!currentName.startsWith(supersetLetter + " ")) {
                exerciseNameText.setText(supersetLetter + " " + currentName);
                Log.d(TAG, "setupSupersetIndicator: Добавлен индикатор суперсета: " + supersetLetter);
            }


            if (getIntent().getBooleanExtra("from_superset", false)) {
                Toast.makeText(this, "Суперсет: переход к следующему упражнению",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }


    private String getSupersetLetter(int order) {
        if (order >= 0 && order < 26) {
            return String.valueOf((char) ('A' + order));
        }
        return String.valueOf(order + 1);
    }


    private void checkSupersetTransitionAsync(boolean hasMoreUncompletedInCurrent) {
        executor.execute(() -> {
            try {

                WorkoutExercise nextExerciseInSuperset = findNextExerciseInSuperset();


                mainHandler.post(() -> {
                    if (nextExerciseInSuperset != null) {
                        Log.d(TAG, "SUPERSET DEBUG: ✅ Есть работа в суперсете, выполняем переход");

                        transitionToNextSupersetExercise(nextExerciseInSuperset);
                    } else {
                        Log.d(TAG, "SUPERSET DEBUG: ❌ Все упражнения в суперсете завершены или нет незавершенных подходов");


                        if (hasMoreUncompletedInCurrent) {

                            boolean restTimerEnabled = WorkoutSettingsManager.getInstance(ExerciseSettingsActivity.this).isRestTimerEnabled();
                            if (restTimerEnabled) {
                                startRestTimer();
                            } else {
                                Log.d(TAG, "checkSupersetTransitionAsync: Таймер отдыха отключен в настройках");
                                isResting = false;
                                updateRestTimerUI();
                                updateUI();
                            }
                        } else {

                            finishExercise();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "SUPERSET DEBUG: Ошибка при проверке суперсета", e);
                mainHandler.post(() -> {

                    if (hasMoreUncompletedInCurrent) {
                        startRestTimer();
                    } else {
                        finishExercise();
                    }
                });
            }
        });
    }


    private WorkoutExercise findNextExerciseInSuperset() {
        if (allWorkoutExercises == null || currentSupersetId == null) {
            Log.d(TAG, "SUPERSET DEBUG: findNextExerciseInSuperset - allWorkoutExercises или currentSupersetId is null");
            return null;
        }


        List<WorkoutExercise> supersetExercises = new ArrayList<>();
        for (WorkoutExercise ex : allWorkoutExercises) {
            if (currentSupersetId.equals(ex.getSuperset_id())) {
                supersetExercises.add(ex);
            }
        }


        supersetExercises.sort((a, b) -> Integer.compare(a.getSuperset_order(), b.getSuperset_order()));

        Log.d(TAG, "SUPERSET DEBUG: Все упражнения в суперсете " + currentSupersetId + ":");
        for (WorkoutExercise ex : supersetExercises) {
            boolean hasUncompleted = hasUncompletedSets(ex);
            Log.d(TAG, "SUPERSET DEBUG:   - " + ex.getExercise().getName() +
                    " (superset_order: " + ex.getSuperset_order() +
                    ", есть незавершенные подходы: " + hasUncompleted + ")");
        }


        int currentIndex = -1;
        for (int i = 0; i < supersetExercises.size(); i++) {
            if (supersetExercises.get(i).getId().equals(workoutExercise.getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            Log.d(TAG, "SUPERSET DEBUG: Текущее упражнение не найдено в списке суперсета");
            return null;
        }


        int searchStartIndex = currentIndex + 1;

        for (int i = 0; i < supersetExercises.size(); i++) {
            int checkIndex = (searchStartIndex + i) % supersetExercises.size();
            WorkoutExercise candidate = supersetExercises.get(checkIndex);


            if (candidate.getId().equals(workoutExercise.getId())) {
                Log.d(TAG, "SUPERSET DEBUG: Пропускаем текущее упражнение: " +
                        candidate.getExercise().getName());
                continue;
            }


            if (hasUncompletedSets(candidate)) {
                Log.d(TAG, "SUPERSET DEBUG: ✅ Найдено следующее упражнение с незавершенными подходами: " +
                        candidate.getExercise().getName() +
                        " (superset_order: " + candidate.getSuperset_order() + ")");
                return candidate;
            } else {
                Log.d(TAG, "SUPERSET DEBUG: ⏭️ Пропускаем упражнение (все подходы завершены): " +
                        candidate.getExercise().getName());
            }
        }


        Log.d(TAG, "SUPERSET DEBUG: ❌ Все упражнения в суперсете завершены");
        return null;
    }


    private boolean hasUncompletedSets(WorkoutExercise exercise) {
        if (exercise == null) {
            Log.d(TAG, "SUPERSET DEBUG: hasUncompletedSets - упражнение is null, возвращаем true");
            return true;
        }

        try {
            Log.d(TAG, "SUPERSET DEBUG: hasUncompletedSets - загружаем свежие данные для упражнения: " +
                    exercise.getExercise().getName() + " (ID: " + exercise.getId() + ")");


            List<ExerciseSet> freshSets = workoutRepository.getExerciseSets(exercise.getId());

            if (freshSets == null || freshSets.isEmpty()) {
                Log.d(TAG, "SUPERSET DEBUG: Нет подходов в БД, упражнение не начато - возвращаем true");
                return true;
            }

            Log.d(TAG, "SUPERSET DEBUG: Загружено " + freshSets.size() + " подходов из БД");


            int completedCount = 0;
            for (ExerciseSet set : freshSets) {
                if (set.isCompleted()) {
                    completedCount++;
                }
            }

            boolean hasUncompleted = completedCount < freshSets.size();
            Log.d(TAG, "SUPERSET DEBUG: Завершено подходов: " + completedCount + "/" + freshSets.size() +
                    " - есть незавершенные: " + hasUncompleted);

            return hasUncompleted;

        } catch (Exception e) {
            Log.e(TAG, "SUPERSET DEBUG: Ошибка при проверке подходов: " + e.getMessage(), e);
            return true;
        }
    }


    private void transitionToNextSupersetExercise(WorkoutExercise nextExercise) {
        Log.d(TAG, "SUPERSET DEBUG: Переход к упражнению: " + nextExercise.getExercise().getName());


        List<ExerciseSet> currentSets = viewModel.getExerciseSets().getValue();
        if (currentSets != null && !currentSets.isEmpty()) {
            workoutExercise.setSetsCompleted(new ArrayList<>(currentSets));
            Log.d(TAG, "SUPERSET DEBUG: Синхронизированы подходы текущего упражнения: " + currentSets.size());
        }


        Intent resultIntent = new Intent();
        resultIntent.putExtra("completed_exercise", workoutExercise);
        setResult(RESULT_OK, resultIntent);


        Intent nextIntent = new Intent(this, ExerciseSettingsActivity.class);
        nextIntent.putExtra("exercise", nextExercise.getExercise());
        nextIntent.putExtra("workout_exercise", nextExercise);
        nextIntent.putExtra("workout_id", workoutId);


        if (allWorkoutExercises != null) {
            nextIntent.putParcelableArrayListExtra("all_workout_exercises",
                    new ArrayList<>(allWorkoutExercises));
        }


        nextIntent.putExtra("from_superset", true);

        Log.d(TAG, "SUPERSET DEBUG: Запускаем следующее упражнение в суперсете");
        startActivity(nextIntent);


        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

}