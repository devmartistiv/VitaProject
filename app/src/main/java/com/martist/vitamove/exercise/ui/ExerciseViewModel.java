package com.martist.vitamove.exercise.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.workout.data.model.WorkoutExercise;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ExerciseViewModel extends AndroidViewModel {
    private static final String TAG = "ExerciseViewModel";
    private static final String PREFS_NAME = "ExerciseCache";
    private static final String KEY_EXERCISE_SETS = "exercise_sets_";
    private static final String KEY_WORKOUT_EXERCISE = "workout_exercise_";

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new GsonBuilder().create();
    private final SupabaseWorkoutRepository workoutRepository;


    private final MutableLiveData<List<ExerciseSet>> exerciseSets = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<WorkoutExercise> workoutExercise = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isResting = new MutableLiveData<>(false);
    private final MutableLiveData<Long> restTimeRemaining = new MutableLiveData<>(0L);


    private Exercise exercise;
    private String workoutId;
    private String exerciseId;
    private CountDownTimer restTimer;


    private final SharedPreferences sharedPreferences;

    public ExerciseViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );
        workoutRepository = new SupabaseWorkoutRepository(supabaseClient);
    }


    public void initialize(Exercise exercise, WorkoutExercise workoutExercise, String workoutId) {
        this.exercise = exercise;
        this.workoutId = workoutId;
        this.exerciseId = workoutExercise.getId();
        this.workoutExercise.setValue(workoutExercise);


        loadExerciseSets();
    }


    private <T> void safeSetValue(MutableLiveData<T> liveData, T value) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }


    private void loadExerciseSets() {
        if (workoutId == null || exercise == null) {
            Log.e(TAG, "loadExerciseSets: workoutId или exercise равны null");
            return;
        }

        Log.d(TAG, "loadExerciseSets: загрузка подходов для упражнения: " + exercise.getName());


        safeSetValue(isLoading, true);


        List<ExerciseSet> cachedSets = loadSetsFromCache();
        WorkoutExercise currentExercise = workoutExercise.getValue();

        if (cachedSets != null && !cachedSets.isEmpty()) {

            safeSetValue(exerciseSets, new ArrayList<>(cachedSets));


            if (currentExercise != null && (currentExercise.getSetsCompleted() == null || currentExercise.getSetsCompleted().isEmpty())) {
                Log.d(TAG, "loadExerciseSets: обновление связи workoutExercise с подходами из кэша");
                currentExercise.getSetsCompleted().clear();
                currentExercise.getSetsCompleted().addAll(cachedSets);
                safeSetValue(workoutExercise, currentExercise);
            }

            safeSetValue(isLoading, false);
            return;
        }


        String workoutExerciseId = currentExercise != null ? currentExercise.getId() : null;

        if (workoutExerciseId == null || workoutExerciseId.isEmpty()) {
            Log.e(TAG, "loadExerciseSets: workoutExerciseId равен null или пуст");
            safeSetValue(isLoading, false);
            return;
        }

        try {
            executor.execute(() -> {
                try {
                    Log.d(TAG, "loadExerciseSets: загрузка подходов из БД для workoutExerciseId: " + workoutExerciseId);
                    List<ExerciseSet> sets = workoutRepository.getExerciseSets(workoutExerciseId);

                    if (sets != null && !sets.isEmpty()) {
                        Log.d(TAG, "loadExerciseSets: подходы загружены из БД, количество: " + sets.size());


                        if (exercise != null && exercise.isFunctionalExercise()) {
                            List<ExerciseSet> uniqueSets = removeDuplicateSets(sets);
                            if (uniqueSets.size() < sets.size()) {
                                Log.d(TAG, "loadExerciseSets: удалено " + (sets.size() - uniqueSets.size()) + " дубликатов подходов для функционального упражнения");
                                sets = uniqueSets;
                            }
                        }


                        safeSetValue(exerciseSets, new ArrayList<>(sets));
                        saveToCache(sets);


                        if (currentExercise != null) {
                            currentExercise.getSetsCompleted().clear();
                            currentExercise.getSetsCompleted().addAll(sets);
                            safeSetValue(workoutExercise, currentExercise);
                        }

                    } else {
                        Log.d(TAG, "loadExerciseSets: в БД не найдены подходы, создаем дефолтные");

                        String exerciseType = exercise != null ? exercise.getExerciseType() : null;
                        int defaultSetsCount = exercise != null ? exercise.getDefaultSets() : 3;
                        String baseExerciseId = exercise != null ? exercise.getId() : null;

                        List<ExerciseSet> defaultSets = createDefaultSets(workoutExerciseId, baseExerciseId, exerciseType, defaultSetsCount);


                        saveDefaultSetsToDB(defaultSets, workoutExerciseId);


                        safeSetValue(exerciseSets, new ArrayList<>(defaultSets));
                        saveToCache(defaultSets);


                        if (currentExercise != null) {
                            currentExercise.getSetsCompleted().clear();
                            currentExercise.getSetsCompleted().addAll(defaultSets);
                            safeSetValue(workoutExercise, currentExercise);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "loadExerciseSets: ошибка при загрузке подходов: " + e.getMessage(), e);
                    safeSetValue(errorMessage, "Ошибка при загрузке подходов: " + e.getMessage());
                } finally {
                    safeSetValue(isLoading, false);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadExerciseSets: ошибка при запуске задачи загрузки: " + e.getMessage(), e);
            safeSetValue(isLoading, false);
            safeSetValue(errorMessage, "Ошибка при загрузке подходов: " + e.getMessage());
        }
    }


    private List<ExerciseSet> createDefaultSets(String workoutExerciseId, String baseExerciseId, String type, int numSets) {
        List<ExerciseSet> newSets = new ArrayList<>();


        boolean isSpecialExerciseType = (type != null &&
                (type.equals("восстановительное") ||
                        type.equals("реабилитационное")));


        boolean isFunctionalExercise = (exercise != null && exercise.isFunctionalExercise());


        boolean isBodyweightExercise = (exercise != null && exercise.isBodyweightExercise());


        int setsToCreate = isCardioExercise() ? 1 : numSets;


        if ((isFunctionalExercise || isBodyweightExercise) && (setsToCreate <= 0 || numSets <= 0)) {
            setsToCreate = 3;
        }


        for (int i = 0; i < setsToCreate; i++) {
            ExerciseSet set = new ExerciseSet();
            set.setWorkoutExerciseId(workoutExerciseId);
            set.setExerciseId(baseExerciseId);
            set.setSetNumber(i + 1);


            if (exercise != null) {
                String defaultReps = exercise.getDefaultReps();

                if (isCardioExercise()) {
                    set.setReps(null);
                } else {
                    try {

                        Integer lastReps = getLastRepsForExercise(baseExerciseId);
                        if (lastReps != null && lastReps > 0) {

                            set.setReps(lastReps);
                            Log.d(TAG, "createDefaultSets: Установлено последнее использованное количество повторений " + lastReps + " для упражнения " + baseExerciseId);
                        } else if (defaultReps != null && !defaultReps.isEmpty()) {

                            try {
                                int reps = Integer.parseInt(defaultReps.split("-")[0].trim());
                                set.setReps(reps);
                                Log.d(TAG, "createDefaultSets: Установлено количество повторений по умолчанию " + reps + " для упражнения " + baseExerciseId);
                            } catch (NumberFormatException e) {

                                set.setReps(12);
                                Log.d(TAG, "createDefaultSets: Установлено стандартное количество повторений 12 для упражнения " + baseExerciseId + " (ошибка парсинга defaultReps)");
                            }
                        } else {

                            set.setReps(12);
                            Log.d(TAG, "createDefaultSets: Установлено стандартное количество повторений 12 для упражнения " + baseExerciseId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "createDefaultSets: Ошибка при установке повторений: " + e.getMessage(), e);
                        set.setReps(12);
                    }
                }


                if (isCardioExercise()) {
                    set.setWeight(null);
                } else if (isSpecialExerciseType || isFunctionalExercise || isBodyweightExercise) {
                    set.setWeight(null);
                } else {

                    Float lastWeight = getLastWeightForExercise(baseExerciseId);
                    if (lastWeight != null && lastWeight > 0) {

                        set.setWeight(lastWeight);
                        Log.d(TAG, "createDefaultSets: Установлен последний использованный вес " + lastWeight + " для упражнения " + baseExerciseId);
                    } else {

                        set.setWeight(0.0f);
                    }
                }
            }

            set.setCompleted(false);
            newSets.add(set);
        }


        boolean allHaveExerciseId = true;
        for (int i = 0; i < newSets.size(); i++) {
            ExerciseSet set = newSets.get(i);
            if (set.getExerciseId() == null || set.getExerciseId().isEmpty()) {
                allHaveExerciseId = false;
                Log.e(TAG, "createDefaultSets: Подход #" + (i + 1) + " НЕ имеет exercise_id!");
            } else {
                Log.d(TAG, "createDefaultSets: Подход #" + (i + 1) + " имеет exercise_id = " + set.getExerciseId());
            }
        }

        if (!allHaveExerciseId) {
            Log.e(TAG, "createDefaultSets: ВНИМАНИЕ! Не все подходы имеют exercise_id!");
        }

        return newSets;
    }


    private List<ExerciseSet> loadSetsFromCache() {
        try {
            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Чтение подходов из КЭША для упражнения: " + exerciseId);
            String key = KEY_EXERCISE_SETS + exerciseId;
            String setsJson = sharedPreferences.getString(key, null);

            if (setsJson != null && !setsJson.isEmpty()) {
                Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Найдены данные в КЭШЕ, размер JSON: " + setsJson.length() + " байт");
                Type listType = new TypeToken<List<ExerciseSet>>() {
                }.getType();
                List<ExerciseSet> cachedSets = gson.fromJson(setsJson, listType);

                if (cachedSets != null && !cachedSets.isEmpty()) {
                    Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Успешно десериализованы подходы из КЭША, количество: " + cachedSets.size());
                    return cachedSets;
                } else {
                    Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Десериализация из КЭША вернула пустой список");
                }
            } else {
                Log.d(TAG, "ИСТОЧНИК ДАННЫХ: В КЭШЕ отсутствуют данные о подходах для упражнения: " + exerciseId);
            }
        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке подходов из КЭША: " + e.getMessage(), e);
        }
        return null;
    }


    private void saveToCache(List<ExerciseSet> sets) {
        if (sets == null) return;

        try {
            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Сохранение " + sets.size() + " подходов в КЭШ для упражнения: " + exerciseId);
            long startTime = System.currentTimeMillis();

            String key = KEY_EXERCISE_SETS + exerciseId;
            String setsJson = gson.toJson(sets);

            sharedPreferences.edit()
                    .putString(key, setsJson)
                    .apply();

            long endTime = System.currentTimeMillis();
            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Подходы сохранены в КЭШ за " + (endTime - startTime) +
                    " мс, размер JSON: " + setsJson.length() + " байт");
        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при сохранении подходов в КЭШ: " + e.getMessage(), e);
        }
    }


    public void updateSet(ExerciseSet updatedSet) {
        if (updatedSet == null) {
            Log.e(TAG, "updateSet: updatedSet равен null");
            return;
        }


        String setId = updatedSet.getId();
        if (setId == null || setId.isEmpty()) {
            Log.e(TAG, "updateSet: ID подхода равен null или пустой");
            return;
        }


        boolean isTempId = setId.startsWith("temp_");
        if (isTempId) {
            Log.d(TAG, "updateSet: Подход имеет временный ID, пропускаем обновление в БД: " + setId);


            List<ExerciseSet> currentSets = exerciseSets.getValue();
            if (currentSets != null) {
                List<ExerciseSet> updatedSets = new ArrayList<>(currentSets);
                for (int i = 0; i < updatedSets.size(); i++) {
                    if (setId.equals(updatedSets.get(i).getId())) {
                        updatedSets.set(i, updatedSet);
                        break;
                    }
                }


                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    exerciseSets.setValue(updatedSets);
                } else {
                    exerciseSets.postValue(updatedSets);
                }


                updateWorkoutExercise(updatedSets);


                saveToCache(updatedSets);
            }

            return;
        }


        executor.execute(() -> {
            try {
                workoutRepository.updateSet(setId, updatedSet);


                List<ExerciseSet> currentSets = exerciseSets.getValue();
                if (currentSets != null) {
                    List<ExerciseSet> updatedSets = new ArrayList<>(currentSets);
                    boolean found = false;

                    for (int i = 0; i < updatedSets.size(); i++) {
                        if (setId.equals(updatedSets.get(i).getId())) {
                            updatedSets.set(i, updatedSet);
                            found = true;
                            break;
                        }
                    }


                    if (!found) {
                        updatedSets.add(updatedSet);
                    }


                    exerciseSets.postValue(updatedSets);


                    updateWorkoutExercise(updatedSets);


                    saveToCache(updatedSets);
                }

                Log.d(TAG, "updateSet: Подход успешно обновлен, ID: " + setId);
            } catch (Exception e) {
                Log.e(TAG, "updateSet: Ошибка при обновлении подхода: " + e.getMessage(), e);


                if (e.getMessage() != null && (
                        e.getMessage().contains("row-level security policy") ||
                                e.getMessage().contains("42501") ||
                                e.getMessage().contains("403"))) {
                    errorMessage.postValue("Ошибка доступа: нет прав на обновление подходов. Пожалуйста, войдите снова в аккаунт.");
                } else {
                    errorMessage.postValue("Ошибка при обновлении подхода: " + e.getMessage());
                }
            }
        });
    }


    private void updateWorkoutExercise(List<ExerciseSet> updatedSets) {
        WorkoutExercise currentExercise = workoutExercise.getValue();
        if (currentExercise != null) {
            try {

                WorkoutExercise updatedExercise = new WorkoutExercise(
                        currentExercise.getId(),
                        currentExercise.getExercise(),
                        currentExercise.getOrderNumber(),
                        new ArrayList<>(updatedSets),
                        currentExercise.getNotes()
                );


                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {

                    workoutExercise.setValue(updatedExercise);
                } else {

                    workoutExercise.postValue(updatedExercise);
                }

                Log.d(TAG, "updateWorkoutExercise: Обновлен WorkoutExercise с " + updatedSets.size() + " подходами");
            } catch (Exception e) {
                Log.e(TAG, "updateWorkoutExercise: Ошибка при обновлении WorkoutExercise: " + e.getMessage(), e);
            }
        }
    }


    public void deleteSet(String setId) {
        List<ExerciseSet> currentSets = exerciseSets.getValue();
        if (currentSets == null) return;


        int position = -1;
        for (int i = 0; i < currentSets.size(); i++) {
            if (currentSets.get(i).getId() != null && currentSets.get(i).getId().equals(setId)) {
                position = i;
                break;
            }
        }

        if (position >= 0) {
            currentSets.remove(position);


            for (int i = position; i < currentSets.size(); i++) {
                currentSets.get(i).setSetNumber(i + 1);
            }


            safeSetValue(exerciseSets, currentSets);


            WorkoutExercise currentExercise = workoutExercise.getValue();
            if (currentExercise != null) {
                currentExercise.getSetsCompleted().clear();
                currentExercise.getSetsCompleted().addAll(currentSets);
                safeSetValue(workoutExercise, currentExercise);
            }


            saveToCache(currentSets);


            if (setId != null && setId.startsWith("temp_")) {
                Log.d(TAG, "deleteSet: Пропускаем удаление из БД для временного ID: " + setId);
                return;
            }


            final int finalPosition = position;
            executor.execute(() -> {
                try {
                    workoutRepository.deleteSet(setId);
                    Log.d(TAG, "Подход удален из БД: " + setId);


                    for (int i = finalPosition; i < currentSets.size(); i++) {

                        String currentId = currentSets.get(i).getId();
                        if (currentId != null && !currentId.startsWith("temp_")) {
                            workoutRepository.updateSet(currentId, currentSets.get(i));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при удалении подхода из БД: " + e.getMessage(), e);
                    errorMessage.postValue("Ошибка при удалении подхода: " + e.getMessage());
                }
            });
        }
    }


    public void startRest(long restTime) {
        safeSetValue(isResting, true);
        safeSetValue(restTimeRemaining, restTime);

        if (restTimer != null) {
            restTimer.cancel();
        }

        restTimer = new CountDownTimer(restTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                restTimeRemaining.postValue(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                endRest();
            }
        };

        restTimer.start();
    }


    public void endRest() {
        if (restTimer != null) {
            restTimer.cancel();
        }

        safeSetValue(isResting, false);
        safeSetValue(restTimeRemaining, 0L);
    }


    private void saveDefaultSetsToDB(List<ExerciseSet> defaultSets, String workoutExerciseId) {
        if (defaultSets == null || defaultSets.isEmpty() || workoutExerciseId == null || workoutExerciseId.isEmpty()) {
            Log.e(TAG, "saveDefaultSetsToDB: Некорректные аргументы (пустые сеты или workoutExerciseId)");
            return;
        }

        Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Сохранение " + defaultSets.size() + " стандартных подходов в БАЗУ ДАННЫХ для WorkoutExercise ID: " + workoutExerciseId);


        for (int i = 0; i < defaultSets.size(); i++) {
            ExerciseSet set = defaultSets.get(i);
            Log.d(TAG, "saveDefaultSetsToDB: Подход #" + (i + 1) +
                    " (setNumber=" + set.getSetNumber() + "): " +
                    "exerciseId=" + set.getExerciseId());
        }


        List<ExerciseSet> savedSetsWithId = new ArrayList<>();
        boolean allSavedSuccessfully = true;

        try {

            for (ExerciseSet set : defaultSets) {
                try {
                    Log.d(TAG, "saveDefaultSetsToDB: Сохранение подхода #" + set.getSetNumber() +
                            ", exerciseId=" + set.getExerciseId() +
                            ", weight=" + set.getWeight() +
                            ", reps=" + set.getReps());


                    String setId = workoutRepository.addSet(workoutExerciseId, set);
                    if (setId != null && !setId.isEmpty()) {

                        set.setId(setId);
                        savedSetsWithId.add(set);
                        Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Подход #" + set.getSetNumber() +
                                " сохранен в БАЗЕ ДАННЫХ, присвоен ID: " + setId +
                                ", exerciseId=" + set.getExerciseId());
                    } else {

                        allSavedSuccessfully = false;
                        Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Не удалось сохранить подход в БАЗЕ ДАННЫХ (setNumber: " + set.getSetNumber() + "), получен null или пустой ID");


                        errorMessage.postValue("Ошибка доступа к базе данных при сохранении подхода. Проверьте права доступа.");
                    }
                } catch (Exception e) {

                    allSavedSuccessfully = false;
                    Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при сохранении подхода (setNumber: " + set.getSetNumber() + "): " + e.getMessage(), e);


                    if (e.getMessage() != null && (
                            e.getMessage().contains("row-level security policy") ||
                                    e.getMessage().contains("42501") ||
                                    e.getMessage().contains("403"))) {
                        errorMessage.postValue("Ошибка доступа: нет прав на добавление подходов. Пожалуйста, войдите снова в аккаунт.");
                    } else {
                        errorMessage.postValue("Ошибка сохранения подхода: " + e.getMessage());
                    }
                }
            }

            if (allSavedSuccessfully) {
                Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Все стандартные подходы успешно сохранены в БАЗЕ ДАННЫХ");
            } else {
                Log.w(TAG, "ИСТОЧНИК ДАННЫХ: Не все стандартные подходы были успешно сохранены в БАЗЕ ДАННЫХ.");


                if (savedSetsWithId.isEmpty()) {
                    for (int i = 0; i < defaultSets.size(); i++) {
                        ExerciseSet set = defaultSets.get(i);

                        set.setId("temp_" + UUID.randomUUID().toString());
                        savedSetsWithId.add(set);
                        Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Создан временный ID '" + set.getId() +
                                "' для подхода #" + set.getSetNumber() +
                                ", exerciseId=" + set.getExerciseId());
                    }
                    Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Созданы временные локальные ID для подходов, чтобы избежать ошибок в UI");
                }
            }


            exerciseSets.postValue(savedSetsWithId);


            WorkoutExercise currentExercise = workoutExercise.getValue();
            if (currentExercise != null) {
                currentExercise.getSetsCompleted().clear();
                currentExercise.getSetsCompleted().addAll(savedSetsWithId);
                workoutExercise.postValue(currentExercise);
            }


            saveToCache(savedSetsWithId);

        } catch (Exception e) {


            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Непредвиденная ошибка при обработке сохранения подходов: " + e.getMessage(), e);
            errorMessage.postValue("Ошибка сохранения подходов: " + e.getMessage());
        }
    }


    public void addNewSet(ExerciseSet newSet) {
        if (newSet == null) {
            Log.e(TAG, "addNewSet: newSet равен null");
            return;
        }


        List<ExerciseSet> currentSets = exerciseSets.getValue();
        List<ExerciseSet> updatedSetsList = (currentSets == null) ? new ArrayList<>() : new ArrayList<>(currentSets);


        boolean hasTemporarySet = false;
        for (ExerciseSet set : updatedSetsList) {
            if (set.getId() != null && set.getId().startsWith("temp_") && !set.isCompleted()) {
                hasTemporarySet = true;
                Log.d(TAG, "addNewSet: Уже есть незавершенный подход с временным ID: " + set.getId());
                break;
            }
        }

        if (hasTemporarySet) {
            Log.d(TAG, "addNewSet: Пропускаем добавление нового подхода, так как есть незавершенный временный подход");

            safeSetValue(exerciseSets, updatedSetsList);
            return;
        }


        String tempId = "temp_" + UUID.randomUUID().toString();
        newSet.setId(tempId);
        updatedSetsList.add(newSet);


        safeSetValue(exerciseSets, updatedSetsList);


        WorkoutExercise currentExercise = workoutExercise.getValue();
        if (currentExercise != null) {
            currentExercise.getSetsCompleted().clear();
            currentExercise.getSetsCompleted().addAll(updatedSetsList);
            safeSetValue(workoutExercise, currentExercise);


            saveToCache(updatedSetsList);
        }


        executor.execute(() -> {
            try {
                String setId = workoutRepository.addSet(exerciseId, newSet);


                if (setId != null && !setId.isEmpty()) {

                    newSet.setId(setId);


                    List<ExerciseSet> currentSetsAfterResponse = exerciseSets.getValue();
                    if (currentSetsAfterResponse != null) {

                        List<ExerciseSet> newUpdatedSetsList = new ArrayList<>();
                        boolean foundAndReplaced = false;


                        for (ExerciseSet set : currentSetsAfterResponse) {
                            if (tempId.equals(set.getId())) {


                                ExerciseSet updatedSet = new ExerciseSet(set);
                                updatedSet.setId(setId);
                                newUpdatedSetsList.add(updatedSet);
                                foundAndReplaced = true;
                                Log.d(TAG, "addNewSet: Заменен подход с временным ID на постоянный: " + tempId + " -> " + setId);
                            } else {
                                newUpdatedSetsList.add(set);
                            }
                        }


                        if (!foundAndReplaced) {

                            boolean alreadyHasSetWithSameNumber = false;
                            for (ExerciseSet existingSet : newUpdatedSetsList) {
                                if (existingSet.getSetNumber() == newSet.getSetNumber()) {
                                    alreadyHasSetWithSameNumber = true;
                                    Log.d(TAG, "addNewSet: Подход с таким же номером уже существует: " + newSet.getSetNumber());
                                    break;
                                }
                            }


                            if (!alreadyHasSetWithSameNumber) {
                                newUpdatedSetsList.add(newSet);
                                Log.d(TAG, "addNewSet: Не найден подход с временным ID, добавляем новый с постоянным ID");
                            } else {
                                Log.d(TAG, "addNewSet: Пропускаем добавление нового подхода с постоянным ID, т.к. уже есть подход с таким номером");
                            }
                        }


                        newUpdatedSetsList.sort((a, b) -> Integer.compare(a.getSetNumber(), b.getSetNumber()));


                        exerciseSets.postValue(newUpdatedSetsList);


                        WorkoutExercise exercise = workoutExercise.getValue();
                        if (exercise != null) {
                            exercise.getSetsCompleted().clear();
                            exercise.getSetsCompleted().addAll(newUpdatedSetsList);
                            workoutExercise.postValue(exercise);
                        }


                        saveToCache(newUpdatedSetsList);
                    }

                    Log.d(TAG, "addNewSet: Добавлен новый подход с ID: " + setId);
                } else {
                    Log.e(TAG, "addNewSet: Не удалось сохранить подход в БД, получен null или пустой ID");
                    errorMessage.postValue("Ошибка: не удалось сохранить новый подход");

                }
            } catch (Exception e) {
                Log.e(TAG, "addNewSet: Ошибка при добавлении подхода: " + e.getMessage(), e);


                if (e.getMessage() != null && (
                        e.getMessage().contains("row-level security policy") ||
                                e.getMessage().contains("42501") ||
                                e.getMessage().contains("403"))) {
                    errorMessage.postValue("Ошибка доступа: нет прав на добавление подходов. Пожалуйста, войдите снова в аккаунт.");
                } else {
                    errorMessage.postValue("Ошибка при добавлении подхода: " + e.getMessage());
                }


            }
        });
    }


    public boolean isExerciseRated(String workoutExerciseId) {
        if (workoutExerciseId == null || workoutExerciseId.isEmpty()) {
            return false;
        }

        String ratedExerciseKey = "rated_exercise_" + workoutExerciseId;
        boolean isRated = sharedPreferences.getBoolean(ratedExerciseKey, false);

        Log.d(TAG, "isExerciseRated: Проверка статуса оценки из SharedPreferences: " +
                ratedExerciseKey + " = " + isRated);

        return isRated;
    }


    public LiveData<List<ExerciseSet>> getExerciseSets() {
        return exerciseSets;
    }

    public LiveData<WorkoutExercise> getWorkoutExercise() {
        return workoutExercise;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsResting() {
        return isResting;
    }

    public LiveData<Long> getRestTimeRemaining() {
        return restTimeRemaining;
    }


    public void setSets(List<ExerciseSet> sets) {
        if (sets == null) {
            Log.e(TAG, "setSets: передан null список подходов");
            return;
        }

        Log.d(TAG, "setSets: установка " + sets.size() + " подходов");
        safeSetValue(exerciseSets, new ArrayList<>(sets));


        WorkoutExercise currentExercise = workoutExercise.getValue();
        if (currentExercise != null) {
            currentExercise.getSetsCompleted().clear();
            currentExercise.getSetsCompleted().addAll(sets);
            safeSetValue(workoutExercise, currentExercise);


            saveToCache(sets);
        } else {
            Log.e(TAG, "setSets: не удалось обновить workoutExercise, так как текущее значение null");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (restTimer != null) {
            restTimer.cancel();
        }
    }


    public Float getLastWeightForExercise(String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) {
            Log.e(TAG, "getLastWeightForExercise: exerciseId равен null или пуст");
            return null;
        }

        try {

            List<ExerciseSet> currentSets = exerciseSets.getValue();
            if (currentSets != null && !currentSets.isEmpty()) {

                List<ExerciseSet> sortedSets = new ArrayList<>(currentSets);
                sortedSets.sort((a, b) -> {
                    Long timeA = a.getCreatedAt() != null ? a.getCreatedAt() : 0L;
                    Long timeB = b.getCreatedAt() != null ? b.getCreatedAt() : 0L;
                    return timeB.compareTo(timeA);
                });


                for (ExerciseSet set : sortedSets) {
                    if (exerciseId.equals(set.getExerciseId()) && set.getWeight() != null && set.getWeight() > 0) {
                        Log.d(TAG, "getLastWeightForExercise: Найден вес в текущих подходах: " + set.getWeight() + " для упражнения " + exerciseId);
                        return set.getWeight();
                    }
                }
            }


            Float lastWeight = workoutRepository.getLastWeightForExercise(exerciseId);
            if (lastWeight != null) {
                Log.d(TAG, "getLastWeightForExercise: Найден вес в БД: " + lastWeight + " для упражнения " + exerciseId);
                return lastWeight;
            } else {
                Log.d(TAG, "getLastWeightForExercise: В БД не найден вес для упражнения " + exerciseId);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastWeightForExercise: Ошибка при получении последнего веса: " + e.getMessage(), e);
            return null;
        }
    }


    public Integer getLastRepsForExercise(String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) {
            Log.e(TAG, "getLastRepsForExercise: exerciseId равен null или пуст");
            return null;
        }

        try {

            List<ExerciseSet> currentSets = exerciseSets.getValue();
            if (currentSets != null && !currentSets.isEmpty()) {

                List<ExerciseSet> sortedSets = new ArrayList<>(currentSets);
                sortedSets.sort((a, b) -> {
                    Long timeA = a.getCreatedAt() != null ? a.getCreatedAt() : 0L;
                    Long timeB = b.getCreatedAt() != null ? b.getCreatedAt() : 0L;
                    return timeB.compareTo(timeA);
                });


                for (ExerciseSet set : sortedSets) {
                    if (exerciseId.equals(set.getExerciseId()) && set.getReps() != null && set.getReps() > 0) {
                        Log.d(TAG, "getLastRepsForExercise: Найдены повторения в текущих подходах: " + set.getReps() + " для упражнения " + exerciseId);
                        return set.getReps();
                    }
                }
            }


            Integer lastReps = workoutRepository.getLastRepsForExercise(exerciseId);
            if (lastReps != null) {
                Log.d(TAG, "getLastRepsForExercise: Найдены повторения в БД: " + lastReps + " для упражнения " + exerciseId);
                return lastReps;
            } else {
                Log.d(TAG, "getLastRepsForExercise: В БД не найдены повторения для упражнения " + exerciseId);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastRepsForExercise: Ошибка при получении последних повторений: " + e.getMessage(), e);
            return null;
        }
    }


    public boolean isCardioExercise() {
        if (exercise == null) {
            return false;
        }


        return exercise.isCardioExercise() || exercise.isStaticExercise();
    }


    private List<ExerciseSet> removeDuplicateSets(List<ExerciseSet> sets) {
        if (sets == null || sets.isEmpty()) {
            return new ArrayList<>();
        }


        Map<Integer, ExerciseSet> uniqueSets = new HashMap<>();


        for (ExerciseSet set : sets) {
            if (set == null) continue;

            int setNumber = set.getSetNumber();
            uniqueSets.put(setNumber, set);
        }


        return new ArrayList<>(uniqueSets.values());
    }
} 