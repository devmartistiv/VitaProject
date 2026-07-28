package com.martist.vitamove.programs.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.exercise.ui.ExercisesViewModel;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.programs.data.ProgramTemplateManager;
import com.martist.vitamove.programs.model.CreateProgramDay;
import com.martist.vitamove.programs.ui.adapter.CreateWeekDayAdapter;
import com.martist.vitamove.programs.ui.model.ProgramTemplate;
import com.martist.vitamove.programs.ui.model.ProgramTemplateDay;
import com.martist.vitamove.programs.ui.model.ProgramTemplateExercise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

@AndroidEntryPoint
public class CreateProgramWeekActivity extends BaseActivity implements
        CreateWeekDayAdapter.OnDayClickListener,
        CreateWeekDayAdapter.ExerciseActionCallback,
        CreateWeekDayAdapter.OnDayTitleEditListener {


    public static final String EXTRA_TOTAL_WEEKS = "com.vitamove.app.EXTRA_TOTAL_WEEKS";
    public static final String EXTRA_NAVIGATE_TO_PROGRAMS = "com.vitamove.app.NAVIGATE_TO_PROGRAMS";

    private TextView tvWeekTitle;
    private RecyclerView rvDays;
    private Button btnCopyWeek;
    private Button btnNextWeek;
    private Button btnCreateProgram;
    private CreateWeekDayAdapter dayAdapter;

    private ProgressBar progressBarSaving;
    private View dimOverlay;
    private ProgramTemplateManager templateManager;

    private int currentWeek = 1;
    private int totalWeeks = 1;
    private int numberOfDaysPerWeek = 0;


    private final Map<Integer, List<String>> selectedExerciseIdsPerDay = new HashMap<>();

    private final List<CreateProgramDay> currentWeekDays = new ArrayList<>();

    private ActivityResultLauncher<Intent> exerciseSelectionLauncher;

    private int replacingDayNumber = -1;
    private int replacingExercisePosition = -1;


    private String programName;
    private String programLevel;
    ExercisesViewModel exercisesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_program_week);

        exercisesViewModel = new ViewModelProvider(this).get(ExercisesViewModel.class);
        templateManager = new ProgramTemplateManager(this);


        numberOfDaysPerWeek = getIntent().getIntExtra("NUMBER_OF_DAYS", 0);
        totalWeeks = getIntent().getIntExtra(EXTRA_TOTAL_WEEKS, 1);
        programName = getIntent().getStringExtra("PROGRAM_NAME");
        programLevel = getIntent().getStringExtra("PROGRAM_LEVEL");


        if (programName == null || programName.isEmpty()) {
            programName = getString(R.string.default_program_name);
        }
        if (programLevel == null || programLevel.isEmpty()) {
            programLevel = "MEDIUM";
        }

        findViews();
        setupRecyclerView();
        updateWeekTitle();
        setupButtonClickListeners();
        registerActivityLauncher();
        loadWeekData(currentWeek);
        updateButtonStates();
    }

    private void findViews() {
        tvWeekTitle = findViewById(R.id.tv_week_title);
        rvDays = findViewById(R.id.rv_days);
        btnCopyWeek = findViewById(R.id.btn_copy_week);
        btnNextWeek = findViewById(R.id.btn_next_week);
        btnCreateProgram = findViewById(R.id.btn_create_program);
        progressBarSaving = findViewById(R.id.progress_bar_saving);


        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        dayAdapter = new CreateWeekDayAdapter(this, this, this);
        rvDays.setLayoutManager(new LinearLayoutManager(this));
        rvDays.setAdapter(dayAdapter);
    }

    private void updateWeekTitle() {
        tvWeekTitle.setText(String.format("Неделя %d", currentWeek));
    }

    private void setupButtonClickListeners() {
        btnNextWeek.setOnClickListener(v -> {
            if (!validateCurrentWeekDaysFilled()) {
                return;
            }
            Log.d("CreateProgramWeek", "Переход к следующей неделе (без копирования).");
            currentWeek++;
            updateWeekTitle();
            loadWeekData(currentWeek);
            updateButtonStates();
        });

        btnCopyWeek.setOnClickListener(v -> {
            if (!validateCurrentWeekDaysFilled()) {
                return;
            }
            Log.d("CreateProgramWeek", "Копирование недели " + currentWeek + " -> " + (currentWeek + 1));

            for (int dayNum = 1; dayNum <= numberOfDaysPerWeek; dayNum++) {
                int currentDayKey = generateDayKey(currentWeek, dayNum);
                int nextWeekDayKey = generateDayKey(currentWeek + 1, dayNum);

                List<String> idsToCopy = selectedExerciseIdsPerDay.get(currentDayKey);

                if (idsToCopy != null) {
                    selectedExerciseIdsPerDay.put(nextWeekDayKey, new ArrayList<>(idsToCopy));
                } else {
                    selectedExerciseIdsPerDay.remove(nextWeekDayKey);
                }
            }

            currentWeek++;
            updateWeekTitle();
            loadWeekData(currentWeek);
            updateButtonStates();
            Toast.makeText(this, "Неделя скопирована", Toast.LENGTH_SHORT).show();
        });


        btnCreateProgram.setOnClickListener(v -> {

            if (!validateCurrentWeekDaysFilled()) {
                Toast.makeText(this, "Заполните упражнения для всех дней последней недели", Toast.LENGTH_LONG).show();
                return;
            }


            showLoading(true);


            String userId = ((VitaMoveApplication) getApplication()).getCurrentUserId();
            if (userId == null || userId.isEmpty()) {
                showLoading(false);
                Toast.makeText(this, "Не удалось определить ID пользователя", Toast.LENGTH_LONG).show();
                return;
            }


            templateManager.createTemplateAsync(
                    userId,
                    "",
                    programName,
                    getString(R.string.program_type_user_created_description),
                    "CUSTOM",
                    totalWeeks,
                    numberOfDaysPerWeek,
                    programLevel,
                    false,
                    new ProgramTemplateManager.AsyncCallback<ProgramTemplate>() {
                        @Override
                        public void onSuccess(ProgramTemplate template) {

                            saveAllWeeksAndExercises(template.getId(), new ProgramTemplateManager.AsyncCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    runOnUiThread(() -> {
                                        showLoading(false);
                                        Toast.makeText(CreateProgramWeekActivity.this,
                                                "Программа успешно сохранена!", Toast.LENGTH_SHORT).show();


                                        Intent intent = new Intent(CreateProgramWeekActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtra(EXTRA_NAVIGATE_TO_PROGRAMS, true);

                                        intent.putExtra("workout_tab_index", 2);
                                        startActivity(intent);
                                        finish();
                                    });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    runOnUiThread(() -> {
                                        showLoading(false);
                                        Toast.makeText(CreateProgramWeekActivity.this,
                                                "Ошибка при сохранении программы: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                        Log.e("CreateProgramWeek", "Ошибка сохранения программы", e);
                                    });
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(CreateProgramWeekActivity.this,
                                        "Ошибка при создании программы: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                Log.e("CreateProgramWeek", "Ошибка создания программы", e);
                            });
                        }
                    }
            );
        });
    }


    private void updateButtonStates() {
        if (currentWeek >= totalWeeks) {

            btnNextWeek.setVisibility(View.GONE);
            btnCopyWeek.setVisibility(View.GONE);
            btnCreateProgram.setVisibility(View.VISIBLE);
            Log.d("CreateProgramWeek", "Обновление кнопок: Последняя неделя (Показана 'Создать')");
        } else {

            btnNextWeek.setVisibility(View.VISIBLE);
            btnCopyWeek.setVisibility(View.VISIBLE);
            btnCreateProgram.setVisibility(View.GONE);
            Log.d("CreateProgramWeek", "Обновление кнопок: Не последняя неделя (Показаны 'След.' и 'Копировать')");
        }
    }


    private boolean validateCurrentWeekDaysFilled() {
        boolean allDaysConfigured = true;
        if (currentWeekDays == null || currentWeekDays.isEmpty()) {
            Toast.makeText(this, "Нет дней для проверки.", Toast.LENGTH_SHORT).show();
            return false;
        }
        for (CreateProgramDay day : currentWeekDays) {
            if (day.getSelectedExercises() == null || day.getSelectedExercises().isEmpty()) {
                allDaysConfigured = false;
                String message = String.format("Пожалуйста, добавьте упражнения для Дня %d", day.getDayNumber());
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                break;
            }
        }
        return allDaysConfigured;
    }

    private void loadWeekData(int weekNumber) {
        currentWeekDays.clear();
        if (numberOfDaysPerWeek > 0) {
            for (int i = 1; i <= numberOfDaysPerWeek; i++) {
                CreateProgramDay day = new CreateProgramDay(i);
                int dayKey = generateDayKey(weekNumber, i);


                List<String> savedExerciseIds = selectedExerciseIdsPerDay.get(dayKey);

                if (savedExerciseIds != null && !savedExerciseIds.isEmpty()) {
                    exercisesViewModel.getExercisesByIds(savedExerciseIds);
                    exercisesViewModel.getExercisesByIdsLiveData().observe(this, exercises1 -> {
                        day.setSelectedExercises(exercises1);

                    });

                }
                currentWeekDays.add(day);

            }

        }
        dayAdapter.setDays(currentWeekDays);

    }


    @Override
    public void onDayClick(int dayNumber) {

        replacingDayNumber = -1;
        replacingExercisePosition = -1;
        Log.d("CreateProgramWeek", "Режим: Добавление упражнений для дня " + dayNumber);
        launchExerciseSelection(dayNumber);
    }

    @Override
    public void onReplaceExerciseRequest(int dayNumber, int exercisePosition, String exerciseId) {
        this.replacingDayNumber = dayNumber;
        this.replacingExercisePosition = exercisePosition;
        Log.d("CreateProgramWeek", "Режим: Замена упражнения для Дня " + dayNumber + ", Позиция " + exercisePosition + ", Текущий ID: " + exerciseId);

        launchExerciseSelection(dayNumber, true);
    }

    private void launchExerciseSelection(int dayNumber, boolean isReplacement) {
        if (dayNumber == -1 && replacingDayNumber == -1) {
            Log.e("CreateProgramWeek", "Попытка запустить выбор упражнений без указания дня!");
            return;
        }

        Intent intent = new Intent(this, ProgramDayExerciseSelectionActivity.class);
        int targetDay = (replacingDayNumber != -1) ? replacingDayNumber : dayNumber;


        String dayTitle = "День " + targetDay;
        for (CreateProgramDay day : currentWeekDays) {
            if (day.getDayNumber() == targetDay) {
                dayTitle = day.getTitle();
                break;
            }
        }

        intent.putExtra(ProgramDayExerciseSelectionActivity.EXTRA_WEEK_NUMBER, currentWeek);
        intent.putExtra(ProgramDayExerciseSelectionActivity.EXTRA_DAY_NUMBER, targetDay);
        intent.putExtra(ProgramDayExerciseSelectionActivity.EXTRA_DAY_TITLE, dayTitle);

        intent.putExtra(ProgramDayExerciseSelectionActivity.EXTRA_IS_REPLACEMENT_MODE, isReplacement);


        int dayKey = generateDayKey(currentWeek, targetDay);
        ArrayList<String> currentSelectionIds = (ArrayList<String>) selectedExerciseIdsPerDay.get(dayKey);
        if (currentSelectionIds != null && !currentSelectionIds.isEmpty()) {

            Log.d("CreateProgramWeek", "Передача выбранных ID: " + currentSelectionIds.size());
            intent.putStringArrayListExtra(ProgramDayExerciseSelectionActivity.EXTRA_PREVIOUS_SELECTION, new ArrayList<>(currentSelectionIds));
        }

        exerciseSelectionLauncher.launch(intent);
    }


    private void launchExerciseSelection(int dayNumber) {
        launchExerciseSelection(dayNumber, false);
    }

    private void registerActivityLauncher() {
        exerciseSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int week = data.getIntExtra(ProgramDayExerciseSelectionActivity.EXTRA_WEEK_NUMBER, -1);
                        int day = data.getIntExtra(ProgramDayExerciseSelectionActivity.EXTRA_DAY_NUMBER, -1);
                        ArrayList<String> selectedIds = data.getStringArrayListExtra(ProgramDayExerciseSelectionActivity.EXTRA_SELECTED_EXERCISES);


                        if (week == currentWeek && day != -1) {
                            int dayPosition = findDayPosition(day);
                            if (dayPosition == -1) {
                                Log.e("CreateProgramWeek", "Не удалось найти позицию для дня " + day);

                                replacingDayNumber = -1;
                                replacingExercisePosition = -1;
                                return;
                            }


                            if (replacingExercisePosition != -1 && replacingDayNumber == day) {
                                Log.d("CreateProgramWeek", "Обработка результата: ЗАМЕНА для дня " + day + ", позиция " + replacingExercisePosition);
                                if (selectedIds != null && !selectedIds.isEmpty()) {
                                    String newExerciseId = selectedIds.get(0);
                                    Log.d("CreateProgramWeek", "Выбрано упражнение для замены: ID " + newExerciseId);
                                    exercisesViewModel.getExerciseById(newExerciseId);
                                    exercisesViewModel.getExerciseLiveData().observe(this, newExercise -> {

                                        try {

                                            List<Exercise> existingExercises = currentWeekDays.get(dayPosition).getSelectedExercises();

                                            List<String> existingIds = selectedExerciseIdsPerDay.getOrDefault(generateDayKey(week, day), new ArrayList<>());


                                            if (replacingExercisePosition >= 0 && replacingExercisePosition < existingExercises.size()) {

                                                existingExercises.set(replacingExercisePosition, newExercise);

                                                if (replacingExercisePosition < existingIds.size()) {
                                                    existingIds.set(replacingExercisePosition, newExerciseId);
                                                } else {

                                                    Log.w("CreateProgramWeek", "Позиция замены " + replacingExercisePosition + " вне диапазона списка ID, добавляем ID");
                                                    existingIds.add(newExerciseId);
                                                }
                                                selectedExerciseIdsPerDay.put(generateDayKey(week, day), existingIds);


                                                dayAdapter.notifyItemChanged(dayPosition);
                                                Toast.makeText(this, "Упражнение заменено", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Log.e("CreateProgramWeek", "Ошибка: Неверная позиция для замены (" + replacingExercisePosition + ")");
                                                Toast.makeText(this, "Ошибка индекса при замене", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (Exception e) {
                                            Log.e("CreateProgramWeek", "Ошибка при замене упражнения", e);
                                            Toast.makeText(this, "Ошибка замены упражнения", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {

                                    Toast.makeText(this, "Замена отменена: упражнение не выбрано", Toast.LENGTH_SHORT).show();
                                }

                                replacingDayNumber = -1;
                                replacingExercisePosition = -1;
                            } else if (replacingExercisePosition == -1) {
                                Log.d("CreateProgramWeek", "Обработка результата: ДОБАВЛЕНИЕ/ОБНОВЛЕНИЕ для дня " + day);
                                if (selectedIds != null) {
                                    int dayKey = generateDayKey(week, day);


                                    selectedExerciseIdsPerDay.put(dayKey, new ArrayList<>(selectedIds));
                                    exercisesViewModel.getExercisesByIds2(
                                            selectedIds,
                                            selectedExercises -> {

                                                currentWeekDays.get(dayPosition)
                                                        .setSelectedExercises(new ArrayList<>(selectedExercises));

                                                dayAdapter.notifyItemChanged(dayPosition);
                                                return Unit.INSTANCE;
                                            }
                                    );


                                    String message = String.format("День %d обновлен: %d упр.", day, selectedIds.size());
                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                    Log.d("CreateProgramWeek", "Упражнения обновлены, всего: " + selectedIds.size());
                                } else {
                                    Log.w("CreateProgramWeek", "Получен null список ID упражнений для дня " + day + ", очистка.");
                                    int dayKey = generateDayKey(week, day);
                                    selectedExerciseIdsPerDay.put(dayKey, new ArrayList<>());
                                    currentWeekDays.get(dayPosition).setSelectedExercises(new ArrayList<>());
                                    dayAdapter.notifyItemChanged(dayPosition);
                                    Toast.makeText(this, "Упражнения для дня " + day + " очищены", Toast.LENGTH_SHORT).show();
                                }
                            } else {

                                Log.w("CreateProgramWeek", "Результат получен для дня " + day + ", но ожидалась замена для дня " + replacingDayNumber + ". Замена отменена.");
                                replacingDayNumber = -1;
                                replacingExercisePosition = -1;
                            }
                        } else {
                            Log.w("CreateProgramWeek", "Ошибка результата: неделя=" + week + " (ожидалось " + currentWeek + "), день=" + day);
                            replacingDayNumber = -1;
                            replacingExercisePosition = -1;
                            Toast.makeText(this, "Ошибка получения данных или неверная неделя", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("CreateProgramWeek", "Выбор упражнений отменен пользователем.");
                        replacingDayNumber = -1;
                        replacingExercisePosition = -1;
                        Toast.makeText(this, "Выбор упражнений отменен", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private int findDayPosition(int dayNumber) {
        for (int i = 0; i < currentWeekDays.size(); i++) {
            if (currentWeekDays.get(i).getDayNumber() == dayNumber) {
                return i;
            }
        }
        return -1;
    }

    private int generateDayKey(int week, int day) {
        return week * 100 + day;
    }


    private void saveAllWeeksAndExercises(String templateId, ProgramTemplateManager.AsyncCallback<Boolean> callback) {

        int totalDays = totalWeeks * numberOfDaysPerWeek;
        final AtomicInteger savedDays = new AtomicInteger(0);
        final AtomicInteger failedDays = new AtomicInteger(0);


        for (int week = 1; week <= totalWeeks; week++) {
            final int weekNum = week;


            List<CreateProgramDay> weekDays = new ArrayList<>();
            if (week == currentWeek) {

                weekDays = currentWeekDays;
            } else {

                for (int d = 1; d <= numberOfDaysPerWeek; d++) {
                    CreateProgramDay day = new CreateProgramDay(d);
                    int dayKey = generateDayKey(weekNum, d);
                    List<String> savedIds = selectedExerciseIdsPerDay.get(dayKey);
                    if (savedIds != null && !savedIds.isEmpty()) {
                        exercisesViewModel.getExercisesByIds(savedIds);


                        exercisesViewModel.getExercisesByIdsLiveData().observe(this, exercises -> {

                            day.setSelectedExercises(exercises);

                        });


                    }
                    weekDays.add(day);
                }
            }


            for (CreateProgramDay programDay : weekDays) {
                final int dayNumInWeek = programDay.getDayNumber();
                final int overallDayNum = ((weekNum - 1) * numberOfDaysPerWeek) + dayNumInWeek;


                final String dayTitle = programDay.getTitle();


                int dayKey = generateDayKey(weekNum, dayNumInWeek);
                List<String> exerciseIds = selectedExerciseIdsPerDay.get(dayKey);


                if (exerciseIds == null || exerciseIds.isEmpty()) {
                    savedDays.incrementAndGet();
                    checkCompletion(savedDays, failedDays, totalDays, callback);
                    continue;
                }


                templateManager.addTemplateDayAsync(
                        templateId,
                        dayTitle,
                        "",
                        overallDayNum,
                        weekNum,
                        "",
                        "",
                        60,
                        new ProgramTemplateManager.AsyncCallback<ProgramTemplateDay>() {
                            @Override
                            public void onSuccess(ProgramTemplateDay day) {
                                exercisesViewModel.getExercisesByIds2(exerciseIds, exercises -> {

                                    if (exercises.isEmpty()) {

                                        savedDays.incrementAndGet();
                                        checkCompletion(savedDays, failedDays, totalDays, callback);
                                        return Unit.INSTANCE;
                                    }


                                    final AtomicInteger savedExercises = new AtomicInteger(0);
                                    final int totalExercises = exercises.size();


                                    for (int i = 0; i < exercises.size(); i++) {
                                        Exercise exercise = exercises.get(i);
                                        final int orderIndex = i + 1;


                                        templateManager.addTemplateExerciseAsync(
                                                day.getId(),
                                                exercise.getId(),
                                                exercise.getName(),
                                                orderIndex,
                                                4,
                                                "8-12",
                                                "",
                                                "90",
                                                "",
                                                new ProgramTemplateManager.AsyncCallback<ProgramTemplateExercise>() {
                                                    @Override
                                                    public void onSuccess(ProgramTemplateExercise templateExercise) {

                                                        if (savedExercises.incrementAndGet() == totalExercises) {

                                                            savedDays.incrementAndGet();
                                                            checkCompletion(savedDays, failedDays, totalDays, callback);
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        Log.e("CreateProgramWeek", "Ошибка сохранения упражнения: " + e.getMessage(), e);

                                                        if (savedExercises.incrementAndGet() == totalExercises) {
                                                            savedDays.incrementAndGet();
                                                            checkCompletion(savedDays, failedDays, totalDays, callback);
                                                        }
                                                    }
                                                }
                                        );
                                    }
                                    return Unit.INSTANCE;
                                });


                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e("CreateProgramWeek", "Ошибка сохранения дня: " + e.getMessage(), e);
                                failedDays.incrementAndGet();
                                checkCompletion(savedDays, failedDays, totalDays, callback);
                            }
                        }
                );
            }
        }
    }


    private void checkCompletion(AtomicInteger savedDays, AtomicInteger failedDays,
                                 int totalDays, ProgramTemplateManager.AsyncCallback<Boolean> callback) {
        if (savedDays.get() + failedDays.get() >= totalDays) {
            if (failedDays.get() == 0) {
                callback.onSuccess(true);
            } else {
                callback.onFailure(new Exception("Не удалось сохранить " + failedDays.get() + " дней программы"));
            }
        }
    }


    private void showLoading(boolean show) {
        if (progressBarSaving != null) {
            progressBarSaving.setVisibility(show ? View.VISIBLE : View.GONE);
        }


        if (dimOverlay != null) {
            dimOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }


        if (btnCreateProgram != null) {
            btnCreateProgram.setEnabled(!show);
        }
        if (btnNextWeek != null) {
            btnNextWeek.setEnabled(!show);
        }
        if (btnCopyWeek != null) {
            btnCopyWeek.setEnabled(!show);
        }
    }


    private void showDayTitleEditDialog(int dayNumber, String currentTitle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Название для дня " + dayNumber);


        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentTitle);
        input.selectAll();
        builder.setView(input);


        builder.setPositiveButton("OK", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty()) {

                for (CreateProgramDay day : currentWeekDays) {
                    if (day.getDayNumber() == dayNumber) {
                        day.setTitle(newTitle);
                        break;
                    }
                }

                dayAdapter.setDays(currentWeekDays);
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public void onEditDayTitle(int dayNumber, String currentTitle) {
        showDayTitleEditDialog(dayNumber, currentTitle);
    }

    @Override
    public void onBackPressed() {

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_NAVIGATE_TO_PROGRAMS, true);
        intent.putExtra("workout_tab_index", 2);
        startActivity(intent);
        finish();
    }


} 