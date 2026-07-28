package com.martist.vitamove.workout.ui;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.exercise.data.repo.ExerciseRepository;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.user.UserProfile;
import com.martist.vitamove.user.UserRepository;
import com.martist.vitamove.workout.data.dao.WorkoutDao;
import com.martist.vitamove.workout.data.entities.UserWorkoutEntity;
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity;
import com.martist.vitamove.workout.data.managers.WorkoutScheduleManager;
import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.data.model.WorkoutExercise;
import com.martist.vitamove.workout.data.model.WorkoutPlan;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ActiveWorkoutViewModel extends ViewModel implements WorkoutDao.WorkoutRepositoryHelper {
    private static final String TAG = "ActiveWorkoutViewModel";
    private static final String WORKOUT_ID_TRACE_TAG = "WorkoutIdTrace";

    private final WorkoutDao workoutDao;
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<UserWorkout> activeWorkout;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<Boolean> isWorkoutCompleted;
    private final MutableLiveData<Integer> realTimeCalories = new MutableLiveData<>(0);


    ExerciseRepository exerciseRepository;

    private final Executor executor;


    private final MutableLiveData<String> activeWorkoutId = new MutableLiveData<>();


    private final WorkoutScheduleManager scheduleManager;

    @Inject
    public ActiveWorkoutViewModel(ExerciseRepository repository) {

        AppDatabase database = AppDatabase.getInstance(VitaMoveApplication.getContext());
        workoutDao = database.workoutDao();
        workoutRepository = ((VitaMoveApplication) VitaMoveApplication.getContext()).getWorkoutRepository();
        userRepository = new UserRepository(VitaMoveApplication.getContext());

        activeWorkout = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>("");
        isWorkoutCompleted = new MutableLiveData<>(false);
        this.exerciseRepository = repository;

        scheduleManager = WorkoutScheduleManager.getInstance(VitaMoveApplication.getContext());


        executor = Executors.newSingleThreadExecutor();


    }


    public void loadOrCreateActiveWorkout(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID не может быть пустым для загрузки тренировки");
            errorMessage.postValue("Ошибка: Не удалось определить пользователя.");
            return;
        }


        UserWorkout existingWorkout = activeWorkout.getValue();
        if (existingWorkout != null && userId.equals(existingWorkout.getUserId())) {
            Log.d(TAG, "loadOrCreateActiveWorkout: Тренировка для пользователя " + userId + " уже загружена (ID: " + existingWorkout.getId() + "). Пропуск перезагрузки из БД.");

            if (Boolean.TRUE.equals(isLoading.getValue())) {
                isLoading.postValue(false);
            }
            return;
        }


        Log.d(TAG, "Загрузка или создание активной тренировки для пользователя: " + userId);
        isLoading.setValue(true);

        executor.execute(() -> {
            UserWorkout finalWorkout = null;

            try {

                cleanupOldUnfinishedWorkouts(userId);


                UserWorkout existingActiveWorkout = workoutDao.getFullActiveWorkout(userId, this);
                Log.d(TAG, "Найдена активная тренировка в Room?" + (existingActiveWorkout != null ? " Да, ID: " + existingActiveWorkout.getId() : " Нет"));


                boolean existingWorkoutIsEmpty = existingActiveWorkout != null &&
                        (existingActiveWorkout.getExercises() == null || existingActiveWorkout.getExercises().isEmpty());


                WorkoutPlan todayPlan = null;
                try {
                    todayPlan = scheduleManager.getTodayWorkoutPlan(userId);
                    Log.d(TAG, "План на сегодня:" + (todayPlan != null ? " Да, ID: " + todayPlan.getId() : " Нет"));


                    if (todayPlan != null && existingWorkoutIsEmpty) {
                        Log.i(TAG, "Найдена пустая активная тренировка (ID: " + existingActiveWorkout.getId() + ") и план на сегодня. Удаляем пустую тренировку...");
                        try {

                            String emptyWorkoutId = existingActiveWorkout.getId();


                            workoutDao.deleteFullWorkout(emptyWorkoutId);
                            Log.i(TAG, "Пустая активная тренировка (ID: " + emptyWorkoutId + ") успешно удалена из Room.");


                            try {
                                workoutRepository.deleteWorkout(emptyWorkoutId);
                                Log.i(TAG, "Пустая активная тренировка (ID: " + emptyWorkoutId + ") успешно удалена из Supabase.");
                            } catch (Exception supabaseEx) {
                                Log.e(TAG, "Ошибка при удалении пустой тренировки из Supabase: " + supabaseEx.getMessage(), supabaseEx);

                            }


                            existingActiveWorkout = null;
                        } catch (Exception deletionEx) {
                            Log.e(TAG, "Ошибка при удалении пустой тренировки: " + deletionEx.getMessage(), deletionEx);

                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при проверке плана на сегодня: " + e.getMessage(), e);
                    errorMessage.postValue("Ошибка при проверке плана на сегодня.");

                }


                if (todayPlan != null) {

                    if (existingActiveWorkout != null) {

                        Log.d(TAG, "Обнаружен и план на сегодня, и существующая активная тренировка (ID: " + existingActiveWorkout.getId() + ")");

                        if (existingActiveWorkout.getExercises() == null || existingActiveWorkout.getExercises().isEmpty()) {


                            Log.i(TAG, "Активная тренировка (ID: " + existingActiveWorkout.getId() + ") пуста. Удаляем её...");
                            try {

                                String emptyWorkoutId = existingActiveWorkout.getId();


                                workoutDao.deleteFullWorkout(emptyWorkoutId);
                                Log.i(TAG, "Пустая активная тренировка (ID: " + emptyWorkoutId + ") успешно удалена из Room.");


                                try {
                                    workoutRepository.deleteWorkout(emptyWorkoutId);
                                    Log.i(TAG, "Пустая активная тренировка (ID: " + emptyWorkoutId + ") успешно удалена из Supabase.");
                                } catch (Exception supabaseEx) {
                                    Log.e(TAG, "Ошибка при удалении пустой тренировки из Supabase: " + supabaseEx.getMessage(), supabaseEx);

                                }


                                Log.i(TAG, "Создаем новую активную тренировку из плана ID: " + todayPlan.getId());
                                String createdWorkoutId = workoutRepository.createWorkoutFromPlan(todayPlan);
                                if (createdWorkoutId != null) {
                                    finalWorkout = workoutDao.getFullActiveWorkout(userId, this);
                                    if (finalWorkout != null) {
                                        Log.i(TAG, "Активная тренировка успешно создана из плана и загружена из Room. ID: " + finalWorkout.getId());
                                        Log.d(WORKOUT_ID_TRACE_TAG, "loadOrCreate - Created from plan, finalWorkout ID: " + finalWorkout.getId());
                                    } else {
                                        Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: Не удалось загрузить тренировку из Room после создания из плана!");
                                        errorMessage.postValue("Критическая ошибка БД после создания из плана.");
                                    }
                                } else {
                                    Log.e(TAG, "Ошибка при создании активной тренировки из плана (метод вернул null ID).");
                                    errorMessage.postValue("Не удалось создать тренировку по плану.");

                                    finalWorkout = createAndSaveNewEmptyWorkout(userId);
                                    if (finalWorkout != null) {
                                        Log.d(WORKOUT_ID_TRACE_TAG, "loadOrCreate - Created empty after plan failure, finalWorkout ID: " + finalWorkout.getId());
                                    }
                                }
                            } catch (Exception deletionOrCreationEx) {
                                Log.e(TAG, "Ошибка при удалении пустой или создании из плана: " + deletionOrCreationEx.getMessage(), deletionOrCreationEx);
                                errorMessage.postValue("Ошибка: " + deletionOrCreationEx.getMessage());

                                finalWorkout = existingActiveWorkout;
                                Log.w(TAG, "Возвращаем старую активную тренировку из-за ошибки при замене.");
                            }
                        } else {

                            Log.i(TAG, "Активная тренировка (ID: " + existingActiveWorkout.getId() + ") не пуста. Используем её, сегодняшний план пока игнорируется.");
                            finalWorkout = existingActiveWorkout;
                            Log.d(WORKOUT_ID_TRACE_TAG, "loadOrCreate - Using existing non-empty workout, finalWorkout ID: " + finalWorkout.getId());
                        }
                    } else {


                        Log.i(TAG, "Активной тренировки нет. Создаем новую из плана ID: " + todayPlan.getId());
                        try {
                            String createdWorkoutId = workoutRepository.createWorkoutFromPlan(todayPlan);
                            if (createdWorkoutId != null) {
                                finalWorkout = workoutDao.getFullActiveWorkout(userId, this);
                                if (finalWorkout != null) {
                                    Log.i(TAG, "Активная тренировка успешно создана из плана и загружена из Room. ID: " + finalWorkout.getId());
                                    Log.d(WORKOUT_ID_TRACE_TAG, "loadOrCreate - Created from plan (no existing active), finalWorkout ID: " + finalWorkout.getId());
                                } else {
                                    Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: Не удалось загрузить тренировку из Room после создания из плана!");
                                    errorMessage.postValue("Критическая ошибка БД после создания из плана.");
                                }
                            } else {
                                Log.e(TAG, "Ошибка при создании активной тренировки из плана (метод вернул null ID).");
                                errorMessage.postValue("Не удалось создать тренировку по плану.");

                                finalWorkout = createAndSaveNewEmptyWorkout(userId);
                                if (finalWorkout != null) {
                                    Log.d(WORKOUT_ID_TRACE_TAG, "loadOrCreate - Created empty after plan creation failure, finalWorkout ID: " + finalWorkout.getId());
                                }
                            }
                        } catch (Exception creationEx) {
                            Log.e(TAG, "Ошибка при создании активной тренировки из плана: " + creationEx.getMessage(), creationEx);
                            errorMessage.postValue("Ошибка при создании тренировки по плану: " + creationEx.getMessage());

                            finalWorkout = createAndSaveNewEmptyWorkout(userId);
                        }
                    }
                } else {

                    if (existingActiveWorkout != null) {


                        Log.d(TAG, "Плана на сегодня нет. Используем существующую активную тренировку ID: " + existingActiveWorkout.getId());
                        finalWorkout = existingActiveWorkout;
                        Log.d(WORKOUT_ID_TRACE_TAG, "loadOrCreate - Using existing active (no plan), finalWorkout ID: " + finalWorkout.getId());
                    } else {


                        Log.d(TAG, "Активной тренировки нет и плана на сегодня нет. Создаем новую пустую тренировку...");
                        finalWorkout = createAndSaveNewEmptyWorkout(userId);
                        if (finalWorkout != null) {
                            Log.d(WORKOUT_ID_TRACE_TAG, "loadOrCreate - Created new empty (no plan, no active), finalWorkout ID: " + finalWorkout.getId());
                        }
                    }
                }


                if (finalWorkout != null) {
                    activeWorkout.postValue(finalWorkout);
                    activeWorkoutId.postValue(finalWorkout.getId());
                    Log.d(TAG, "loadOrCreateActiveWorkout: Установлен activeWorkoutId: " + finalWorkout.getId());
                } else {

                    activeWorkout.postValue(null);
                    activeWorkoutId.postValue(null);
                    Log.d(TAG, "loadOrCreateActiveWorkout: Установлен activeWorkoutId в null из-за ошибки или отсутствия тренировки.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Непредвиденная ошибка при загрузке/создании активной тренировки: " + e.getMessage(), e);
                errorMessage.postValue("Критическая ошибка: " + e.getMessage());
                activeWorkout.postValue(null);
                activeWorkoutId.postValue(null);
                Log.d(TAG, "loadOrCreateActiveWorkout: Установлен activeWorkoutId в null из-за общего catch.");
            } finally {
                isLoading.postValue(false);
            }
        });
    }


    private UserWorkout createAndSaveNewEmptyWorkout(String userId) {
        String supabaseWorkoutId = null;
        try {

            supabaseWorkoutId = workoutRepository.createWorkout(userId);
            if (supabaseWorkoutId == null) {
                throw new Exception("Supabase createWorkout вернул null ID");
            }
            Log.i(TAG, "Новая пустая тренировка успешно создана в Supabase с ID: " + supabaseWorkoutId);


            UserWorkout newWorkout = createNewLocalWorkout(userId);
            newWorkout.setId(supabaseWorkoutId);
            workoutDao.insertWorkout(UserWorkoutEntity.fromModel(newWorkout));
            Log.d(TAG, "Новая пустая тренировка создана в БД Room с ID (из Supabase): " + supabaseWorkoutId);


            UserWorkout createdWorkout = workoutDao.getFullActiveWorkout(userId, this);
            if (createdWorkout != null) {
                activeWorkout.postValue(createdWorkout);
                Log.d(TAG, "createAndSaveNewEmptyWorkout: Установка activeWorkoutId (создано пустое): " + createdWorkout.getId());
                activeWorkoutId.postValue(createdWorkout.getId());
                Log.d(TAG, "LiveData обновлены новой пустой тренировкой.");
                return createdWorkout;
            } else {
                Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: Не удалось получить только что созданную пустую тренировку из Room!");
                errorMessage.postValue("Критическая ошибка локальной базы данных при создании пустой тренировки.");
                activeWorkout.postValue(null);
                Log.d(TAG, "createAndSaveNewEmptyWorkout: Установка activeWorkoutId в null (ошибка Room)");
                activeWorkoutId.postValue(null);
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА при создании пустой тренировки: " + e.getMessage(), e);
            errorMessage.postValue("Критическая ошибка: " + e.getMessage());
            activeWorkout.postValue(null);
            Log.d(TAG, "createAndSaveNewEmptyWorkout: Установка activeWorkoutId в null (ошибка catch)");
            activeWorkoutId.postValue(null);
            return null;
        }
    }


    public void addExercise(String exerciseId, String userId) {
        addExercise(exerciseId, userId, null);
    }


    public void addExercise(String exerciseId, String userId, Exercise exerciseObject) {
        Log.d(TAG, "Добавление упражнения ID: " + exerciseId + " для пользователя " + userId +
                (exerciseObject != null ? " (объект уже предоставлен)" : " (будет загружен)"));
        isLoading.setValue(true);

        executor.execute(() -> {
            UserWorkout currentActiveWorkout = activeWorkout.getValue();

            if (currentActiveWorkout == null || currentActiveWorkout.getId() == null || currentActiveWorkout.getEndTime() != null) {
                Log.d(TAG, "addExercise: Создаем новую тренировку, так как текущая: " +
                        (currentActiveWorkout == null ? "null" :
                                (currentActiveWorkout.getEndTime() != null ? "завершена" : "имеет null ID")));


                currentActiveWorkout = createAndSaveNewEmptyWorkout(userId);
                if (currentActiveWorkout == null) {
                    Log.e(TAG, "addExercise: Не удалось создать новую тренировку");
                    errorMessage.postValue("Ошибка: Не удалось создать новую тренировку. Попробуйте перезайти на экран.");
                    isLoading.postValue(false);
                    return;
                }


                UserWorkout finalWorkout = currentActiveWorkout;
                new Handler(Looper.getMainLooper()).post(() -> {
                    activeWorkout.setValue(finalWorkout);
                    activeWorkoutId.setValue(finalWorkout.getId());
                });
            }

            String workoutIdToUse = currentActiveWorkout.getId();

            try {

                Exercise fullExercise = exerciseObject;
                if (fullExercise == null) {
                    fullExercise = getExerciseDetailsSync(exerciseId);
                    if (fullExercise == null) {
                        throw new Exception("Упражнение с ID " + exerciseId + " не найдено в кэше");
                    }
                }


                int orderNumber = currentActiveWorkout.getExercises() != null ? currentActiveWorkout.getExercises().size() : 0;


                String tempId = "temp_" + UUID.randomUUID().toString();
                WorkoutExercise newWorkoutExercise = new WorkoutExercise();
                newWorkoutExercise.setId(tempId);
                newWorkoutExercise.setExercise(fullExercise);
                newWorkoutExercise.setOrderNumber(orderNumber);


                Integer lastReps = workoutDao.getLastRepsForExercise(exerciseId);


                int defaultSetsCount = fullExercise.getDefaultSets();
                boolean isCardio = fullExercise.usesTimer();

                if (isCardio) {
                    defaultSetsCount = 1;
                } else if (defaultSetsCount <= 0) {
                    defaultSetsCount = 3;
                }

                List<ExerciseSet> sets = new ArrayList<>();
                for (int i = 1; i <= defaultSetsCount; i++) {
                    ExerciseSet set = new ExerciseSet();
                    set.setSetNumber(i);
                    set.setReps(lastReps != null && lastReps > 0 ? lastReps : 12);
                    set.setExerciseId(exerciseId);
                    set.setCompleted(false);
                    sets.add(set);
                }

                newWorkoutExercise.setSetsCompleted(sets);
                Log.d(TAG, "Добавлено " + sets.size() + " подходов с " +
                        (lastReps != null && lastReps > 0 ? lastReps : 12) +
                        " повторениями для упражнения " + exerciseId);


                List<WorkoutExercise> currentExercises = currentActiveWorkout.getExercises();
                if (currentExercises == null) {
                    currentExercises = new ArrayList<>();
                    currentActiveWorkout.setExercises(currentExercises);
                }
                currentExercises.add(newWorkoutExercise);


                activeWorkout.postValue(currentActiveWorkout);
                isLoading.postValue(false);
                Log.i(TAG, "✅ ОПТИМИСТИЧНО: Упражнение '" + fullExercise.getName() + "' добавлено в UI (временный ID: " + tempId + ")");


                final Exercise finalExercise = fullExercise;
                final String finalWorkoutId = workoutIdToUse;
                final int finalOrderNumber = orderNumber;
                final String finalUserId = (userId != null && !userId.isEmpty()) ? userId : currentActiveWorkout.getUserId();

                executor.execute(() -> {
                    try {

                        String supabaseWorkoutExerciseId = workoutRepository.addExerciseToWorkout(
                                finalWorkoutId,
                                exerciseId,
                                finalOrderNumber
                        );
                        Log.i(TAG, "✅ WorkoutExercise создан в Supabase с ID: " + supabaseWorkoutExerciseId);


                        newWorkoutExercise.setId(supabaseWorkoutExerciseId);


                        workoutDao.addExerciseToWorkout(newWorkoutExercise, finalWorkoutId);
                        Log.d(TAG, "✅ Упражнение сохранено в Room с реальным ID: " + supabaseWorkoutExerciseId);


                        if (finalUserId != null && !finalUserId.isEmpty()) {
                            UserWorkout updatedWorkout = workoutDao.getFullActiveWorkout(finalUserId, this);
                            activeWorkout.postValue(updatedWorkout);
                            Log.d(TAG, "✅ LiveData обновлен с реальными ID из Supabase");
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Ошибка синхронизации с Supabase: " + e.getMessage(), e);


                        errorMessage.postValue("Упражнение добавлено локально. Ошибка синхронизации: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при добавлении упражнения: " + e.getMessage(), e);
                errorMessage.postValue("Ошибка добавления упражнения: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }


    public void removeExercise(String exerciseIdToRemove) {
        Log.d(TAG, "Удаление упражнения ID: " + exerciseIdToRemove);
        String currentWorkoutId = activeWorkoutId.getValue();
        if (currentWorkoutId == null || exerciseIdToRemove == null) {
            Log.e(TAG, "Невозможно удалить упражнение: ID тренировки или упражнения не найдены");
            return;
        }
        isLoading.setValue(true);

        executor.execute(() -> {
            try {

                workoutDao.deleteFullWorkoutExercise(exerciseIdToRemove);
                Log.d(TAG, "Упражнение ID: " + exerciseIdToRemove + " и его подходы удалены из БД");


                UserWorkout updatedWorkout = workoutDao.getFullActiveWorkout(activeWorkout.getValue().getUserId(), this);
                activeWorkout.postValue(updatedWorkout);

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при удалении упражнения: " + e.getMessage(), e);
                errorMessage.postValue("Ошибка удаления упражнения: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }


    public void replaceExercise(String workoutExerciseId, Exercise newExercise) {
        Log.d(TAG, "Замена упражнения ID: " + workoutExerciseId + " на " + newExercise.getName());

        if (workoutExerciseId == null || newExercise == null) {
            Log.e(TAG, "replaceExercise: workoutExerciseId или newExercise не может быть null");
            errorMessage.setValue("Ошибка при замене упражнения: некорректные данные");
            return;
        }

        String currentWorkoutId = activeWorkoutId.getValue();
        if (currentWorkoutId == null) {
            Log.e(TAG, "replaceExercise: ID активной тренировки не найден");
            errorMessage.setValue("Ошибка при замене упражнения: тренировка не найдена");
            return;
        }

        isLoading.setValue(true);

        executor.execute(() -> {
            try {

                UserWorkout currentWorkout = activeWorkout.getValue();
                if (currentWorkout == null || currentWorkout.getExercises() == null) {
                    Log.e(TAG, "replaceExercise: активная тренировка не найдена");
                    errorMessage.postValue("Ошибка при замене упражнения: тренировка не найдена");
                    isLoading.postValue(false);
                    return;
                }


                WorkoutExercise exerciseToReplace = null;
                for (WorkoutExercise exercise : currentWorkout.getExercises()) {
                    if (workoutExerciseId.equals(exercise.getId())) {
                        exerciseToReplace = exercise;
                        break;
                    }
                }

                if (exerciseToReplace == null) {
                    Log.e(TAG, "replaceExercise: упражнение с ID " + workoutExerciseId + " не найдено в тренировке");
                    errorMessage.postValue("Ошибка при замене упражнения: упражнение не найдено");
                    isLoading.postValue(false);
                    return;
                }


                int originalOrder = exerciseToReplace.getOrderNumber();
                String originalNotes = exerciseToReplace.getNotes();

                Log.d(TAG, "replaceExercise: Заменяем '" + exerciseToReplace.getExercise().getName() +
                        "' на '" + newExercise.getName() + "', сохраняя порядок " + originalOrder +
                        " и заметки: '" + originalNotes + "'");


                workoutDao.deleteFullWorkoutExercise(workoutExerciseId);
                Log.d(TAG, "replaceExercise: Старое упражнение удалено из БД");


                WorkoutExercise newWorkoutExercise = new WorkoutExercise(
                        null,
                        newExercise,
                        originalOrder,
                        new ArrayList<>(),
                        originalNotes
                );


                workoutDao.addExerciseToWorkout(newWorkoutExercise, currentWorkoutId);

                Log.d(TAG, "replaceExercise: Новое упражнение добавлено в БД");


                UserWorkout updatedWorkout = workoutDao.getFullActiveWorkout(currentWorkout.getUserId(), this);
                activeWorkout.postValue(updatedWorkout);

                Log.d(TAG, "replaceExercise: Упражнение успешно заменено");

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при замене упражнения: " + e.getMessage(), e);
                errorMessage.postValue("Ошибка при замене упражнения: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }


    public void updateExerciseNote(String exerciseId, String noteText) {
        Log.d(TAG, "Обновление заметки для упражнения ID: " + exerciseId + ", текст: " +
                (noteText == null || noteText.trim().isEmpty() ? "(пусто)" : noteText));

        if (exerciseId == null) {
            Log.e(TAG, "Невозможно обновить заметку: ID упражнения не найден");
            return;
        }

        isLoading.setValue(true);

        executor.execute(() -> {
            try {

                WorkoutExerciseEntity exerciseEntity = workoutDao.getWorkoutExerciseById(exerciseId);
                if (exerciseEntity == null) {
                    Log.e(TAG, "Упражнение с ID " + exerciseId + " не найдено в базе данных");
                    errorMessage.postValue("Упражнение не найдено");
                    return;
                }


                exerciseEntity.setNotes(noteText);


                workoutDao.updateWorkoutExercise(exerciseEntity);
                Log.d(TAG, "Заметка упражнения ID: " + exerciseId + " обновлена в локальной БД");


                try {
                    workoutRepository.updateExerciseNote(exerciseId, noteText);
                    Log.d(TAG, "Заметка упражнения ID: " + exerciseId + " синхронизирована с Supabase");
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка синхронизации заметки с Supabase: " + e.getMessage(), e);


                }


                UserWorkout currentWorkout = activeWorkout.getValue();
                if (currentWorkout != null) {
                    UserWorkout updatedWorkout = workoutDao.getFullActiveWorkout(currentWorkout.getUserId(), this);
                    activeWorkout.postValue(updatedWorkout);
                    Log.d(TAG, "Активная тренировка перезагружена после обновления заметки");
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении заметки упражнения: " + e.getMessage(), e);
                errorMessage.postValue("Ошибка обновления заметки: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }


    public void completeWorkout(long endTime) {
        UserWorkout currentWorkout = activeWorkout.getValue();
        if (currentWorkout == null || currentWorkout.getUserId() == null) {
            Log.e(TAG, "Невозможно завершить тренировку: нет активной тренировки или user ID");
            return;
        }
        String userId = currentWorkout.getUserId();
        String currentWorkoutId = currentWorkout.getId();
        Log.d(WORKOUT_ID_TRACE_TAG, "completeWorkout - Start. Workout ID from LiveData: " + currentWorkoutId);

        final String planId = currentWorkout.getPlanId();

        if (currentWorkoutId == null) {
            Log.e(TAG, "Невозможно завершить тренировку: ID активной тренировки равен null");
            Log.e(WORKOUT_ID_TRACE_TAG, "completeWorkout - ERROR: currentWorkoutId is null!");
            return;
        }

        Log.d(TAG, "Завершение тренировки ID: " + currentWorkoutId + (planId != null ? " (Связана с планом ID: " + planId + ")" : ""));
        isLoading.setValue(true);

        executor.execute(() -> {
            UserWorkout completedWorkoutToSend = null;
            boolean planStatusUpdated = false;
            Log.d(WORKOUT_ID_TRACE_TAG, "completeWorkout - Inside executor. Workout ID: " + currentWorkoutId);


            double totalWorkoutCalories = 0.0;
            Float userWeight = null;

            try {

                UserProfile profile = userRepository.getCurrentUserProfile();
                if (profile != null) {


                    userWeight = profile.getCurrentWeight();
                    Log.d(TAG, "Получен вес пользователя (" + userId + "): " + userWeight);
                } else {
                    Log.e(TAG, "Не удалось получить профиль пользователя (" + userId + ") для расчета калорий.");

                }

            } catch (Exception weightEx) {
                Log.e(TAG, "Ошибка при получении профиля/веса пользователя (" + userId + ") для расчета калорий: " + weightEx.getMessage(), weightEx);

            }

            if (userWeight != null && userWeight > 0 && currentWorkout.getExercises() != null) {
                Log.d(TAG, "Начало расчета калорий для " + currentWorkout.getExercises().size() + " упражнений.");
                for (WorkoutExercise workoutExercise : currentWorkout.getExercises()) {
                    Exercise baseExercise = workoutExercise.getExercise();
                    if (baseExercise == null || baseExercise.getMet() <= 0) {
                        Log.w(TAG, "Пропуск расчета калорий для упражнения: " + (baseExercise != null ? baseExercise.getName() : "ID " + workoutExercise.getId()) + " (нет данных MET или MET <= 0)");
                        continue;
                    }
                    double metValue = baseExercise.getMet();
                    long totalExerciseDurationSeconds = 0;

                    if (workoutExercise.getSetsCompleted() != null) {
                        for (ExerciseSet set : workoutExercise.getSetsCompleted()) {

                            if (set.isCompleted() && set.getDurationSeconds() != null && set.getDurationSeconds() > 0) {
                                totalExerciseDurationSeconds += set.getDurationSeconds();
                            }
                        }
                    }

                    if (totalExerciseDurationSeconds > 0) {
                        double durationMinutes = totalExerciseDurationSeconds / 60.0;

                        double exerciseCalories = metValue * 3.5 * userWeight / 200.0 * durationMinutes;
                        totalWorkoutCalories += exerciseCalories;
                        Log.d(TAG, "Упражнение: " + baseExercise.getName() +
                                ", MET: " + metValue +
                                ", Длительность (сек): " + totalExerciseDurationSeconds +
                                ", Длительность (мин): " + String.format("%.2f", durationMinutes) +
                                ", Калории: " + String.format("%.2f", exerciseCalories));
                    } else {
                        Log.d(TAG, "Пропуск расчета калорий для упражнения: " + baseExercise.getName() + " (общая длительность подходов = 0)");
                    }
                }
                Log.i(TAG, "Итоговый расчет калорий для тренировки ID " + currentWorkoutId + ": " + String.format("%.2f", totalWorkoutCalories));
            } else {
                Log.w(TAG, "Расчет калорий не выполнен. Причина: " +
                        (userWeight == null || userWeight <= 0 ? "Вес пользователя недействителен (" + userWeight + "). " : "") +
                        (currentWorkout.getExercises() == null ? "Список упражнений null. " : ""));
            }

            int calculatedCalories = (int) Math.round(totalWorkoutCalories);


            try {

                Log.d(WORKOUT_ID_TRACE_TAG, "completeWorkout - Before getActiveWorkoutEntity. Workout ID: " + currentWorkoutId + ", User ID: " + userId);
                UserWorkoutEntity workoutEntity = workoutDao.getActiveWorkoutEntity(userId);
                if (workoutEntity != null) {
                    Log.d(WORKOUT_ID_TRACE_TAG, "completeWorkout - Found entity in Room. Entity ID: " + workoutEntity.getId());
                    workoutEntity.setEndTime(endTime);

                    workoutEntity.setTotalCalories(calculatedCalories);
                    workoutDao.updateWorkout(workoutEntity);
                    Log.d(TAG, "Тренировка ID: " + workoutEntity.getId() + " помечена как завершенная в Room с калориями: " + calculatedCalories);


                    completedWorkoutToSend = new UserWorkout(
                            workoutEntity.getId(),
                            workoutEntity.getUserId(),
                            workoutEntity.getName(),
                            workoutEntity.getStartTime(),
                            workoutEntity.getEndTime(),
                            workoutEntity.getTotalCalories(),
                            workoutEntity.getNotes(),
                            workoutEntity.getProgramId(),
                            workoutEntity.getProgramDayNumber(),
                            workoutEntity.getProgramDayId(),
                            workoutEntity.getPlanId(),
                            currentWorkout.getExercises()
                    );


                    Log.i(TAG, "completeWorkout: Перед вызовом saveCompletedWorkout. Передаваемые калории: " + completedWorkoutToSend.getTotalCalories() + " для Workout ID: " + completedWorkoutToSend.getId());
                    Log.d(WORKOUT_ID_TRACE_TAG, "completeWorkout - Before saveCompletedWorkout. Sending Workout ID: " + completedWorkoutToSend.getId());


                    Log.d(TAG, "Начало отправки завершенной тренировки ID: " + completedWorkoutToSend.getId() + " в Supabase");
                    workoutRepository.saveCompletedWorkout(completedWorkoutToSend);
                    Log.i(TAG, "Завершенная тренировка ID: " + completedWorkoutToSend.getId() + " успешно отправлена в Supabase");


                    if (planId != null && !planId.isEmpty()) {
                        try {
                            Log.d(TAG, "Попытка обновить статус плана ID: " + planId + " на 'completed'");
                            workoutRepository.updateWorkoutPlanStatus(planId, "completed");
                            planStatusUpdated = true;
                            Log.i(TAG, "Статус плана ID: " + planId + " успешно обновлен на 'completed'");
                        } catch (Exception planUpdateEx) {
                            Log.e(TAG, "Ошибка при обновлении статуса плана ID: " + planId, planUpdateEx);


                            errorMessage.postValue("Ошибка обновления статуса плана: " + planUpdateEx.getMessage());
                        }
                    } else {
                        Log.d(TAG, "Тренировка не связана с планом, статус плана не обновляется.");
                    }


                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    final boolean finalPlanStatusUpdated = planStatusUpdated;
                    mainHandler.post(() -> {
                        isWorkoutCompleted.setValue(true);
                        activeWorkout.setValue(null);
                        Log.d(TAG, "completeWorkout: Установка activeWorkoutId в null (тренировка завершена)");
                        Log.d(WORKOUT_ID_TRACE_TAG, "completeWorkout - Setting activeWorkoutId LiveData to null.");
                        activeWorkoutId.setValue(null);
                        isLoading.setValue(false);
                        Log.d(TAG, "LiveData обновлены после успешного завершения и синхронизации" + (finalPlanStatusUpdated ? " (включая статус плана)" : ""));
                    });

                } else {
                    Log.e(TAG, "Не найдена активная тренировка в Room для завершения (User ID: " + userId + ")");
                    Log.e(WORKOUT_ID_TRACE_TAG, "completeWorkout - ERROR: workoutEntity is null for User ID: " + userId);
                    throw new Exception("Активная тренировка не найдена в локальной базе.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при завершении или синхронизации тренировки (предполагаемый ID: " + currentWorkoutId + ")", e);
                Log.e(WORKOUT_ID_TRACE_TAG, "completeWorkout - Exception during completion for Workout ID: " + currentWorkoutId, e);

                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    errorMessage.setValue("Ошибка завершения тренировки: " + e.getMessage());
                    isLoading.setValue(false);
                });
            }
        });
    }


    private UserWorkout createNewLocalWorkout(String userId) {
        UserWorkout newWorkout = new UserWorkout();

        newWorkout.setId(UUID.randomUUID().toString());
        newWorkout.setUserId(userId);
        newWorkout.setName("Новая тренировка");

        newWorkout.setExercises(new ArrayList<>());
        return newWorkout;
    }


    @Override
    public Exercise getExerciseDetailsSync(String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) {
            Log.w(TAG, "getExerciseDetailsSync: Попытка получить детали для null или пустого exerciseId");
            return null;
        }


        Exercise cachedExercise = exerciseRepository.getExerciseById(exerciseId);


        return cachedExercise;


    }

    @Override
    public List<Exercise> getExercisesByIds(List<String> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) {
            Log.w(TAG, "getExercisesByIds: Пустой список ID");
            return new ArrayList<>() {
            };
        }


        return exerciseRepository.getExercisesByIds(exerciseIds);
    }


    public LiveData<UserWorkout> getActiveWorkout() {
        return activeWorkout;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsWorkoutCompleted() {
        return isWorkoutCompleted;
    }


    public void resetWorkoutCompletedFlag() {
        isWorkoutCompleted.setValue(false);
        Log.d(TAG, "resetWorkoutCompletedFlag: Флаг isWorkoutCompleted сброшен в false");
    }

    public LiveData<String> getActiveWorkoutId() {
        return activeWorkoutId;
    }


    public LiveData<Integer> getRealTimeCalories() {
        return realTimeCalories;
    }


    public void calculateRealTimeCalories(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID не может быть пустым для расчета калорий");
            return;
        }

        UserWorkout currentWorkout = activeWorkout.getValue();
        if (currentWorkout == null || currentWorkout.getExercises() == null || currentWorkout.getExercises().isEmpty()) {
            Log.d(TAG, "Нет активной тренировки или упражнений для расчета калорий");
            realTimeCalories.postValue(0);
            return;
        }

        executor.execute(() -> {
            double totalWorkoutCalories = 0.0;
            Float userWeight = null;

            try {

                UserProfile profile = userRepository.getCurrentUserProfile();
                if (profile != null) {
                    userWeight = profile.getCurrentWeight();
                    Log.d(TAG, "Расчет калорий в реальном времени - получен вес пользователя: " + userWeight);
                } else {
                    Log.e(TAG, "Не удалось получить профиль пользователя для расчета калорий в реальном времени");
                }

            } catch (Exception weightEx) {
                Log.e(TAG, "Ошибка при получении профиля/веса пользователя: " + weightEx.getMessage(), weightEx);
            }

            if (userWeight != null && userWeight > 0) {
                for (WorkoutExercise workoutExercise : currentWorkout.getExercises()) {
                    Exercise baseExercise = workoutExercise.getExercise();
                    if (baseExercise == null || baseExercise.getMet() <= 0) {
                        Log.w(TAG, "Пропуск расчета калорий для упражнения: " +
                                (baseExercise != null ? baseExercise.getName() : "ID " + workoutExercise.getId()) +
                                " (нет данных MET или MET <= 0)");
                        continue;
                    }

                    double metValue = baseExercise.getMet();
                    long totalExerciseDurationSeconds = 0;

                    if (workoutExercise.getSetsCompleted() != null) {
                        for (ExerciseSet set : workoutExercise.getSetsCompleted()) {

                            if (set.isCompleted() && set.getDurationSeconds() != null && set.getDurationSeconds() > 0) {
                                totalExerciseDurationSeconds += set.getDurationSeconds();
                            }
                        }
                    }

                    if (totalExerciseDurationSeconds > 0) {
                        double durationMinutes = totalExerciseDurationSeconds / 60.0;

                        double exerciseCalories = metValue * 3.5 * userWeight / 200.0 * durationMinutes;
                        totalWorkoutCalories += exerciseCalories;

                        Log.d(TAG, "Упражнение: " + baseExercise.getName() +
                                ", MET: " + metValue +
                                ", Длительность (сек): " + totalExerciseDurationSeconds +
                                ", Длительность (мин): " + String.format("%.2f", durationMinutes) +
                                ", Калории: " + String.format("%.2f", exerciseCalories));
                    } else {
                        Log.d(TAG, "Пропуск расчета калорий для упражнения: " + baseExercise.getName() +
                                " (общая длительность подходов = 0)");
                    }
                }


            } else {
                Log.w(TAG, "Расчет калорий не выполнен. Причина: вес пользователя недействителен (" + userWeight + ")");
            }


            int calculatedCalories = (int) Math.round(totalWorkoutCalories);
            Log.i(TAG, "calculateRealTimeCalories: рассчитано калорий: " + calculatedCalories);
            realTimeCalories.postValue(calculatedCalories);
        });
    }


    public void saveCurrentWorkoutStateToDb() {
        UserWorkout currentWorkout = activeWorkout.getValue();
        if (currentWorkout == null) {
            Log.d(TAG, "saveCurrentWorkoutStateToDb: Нет активной тренировки в LiveData для сохранения.");
            return;
        }

        Log.d(TAG, "saveCurrentWorkoutStateToDb: Инициировано сохранение текущей тренировки ID: " + currentWorkout.getId() + " в Room.");
        executor.execute(() -> {
            try {

                workoutDao.saveFullWorkout(currentWorkout);
                Log.i(TAG, "saveCurrentWorkoutStateToDb: Текущая тренировка ID: " + currentWorkout.getId() + " успешно сохранена в Room.");
            } catch (Exception e) {
                Log.e(TAG, "saveCurrentWorkoutStateToDb: Ошибка при сохранении текущей тренировки в Room: " + e.getMessage(), e);

            }
        });
    }


    public void updateExerciseOrder(List<WorkoutExercise> exercises) {
        if (exercises == null || exercises.isEmpty()) {
            Log.e(TAG, "updateExerciseOrder: Список упражнений пуст или null");
            return;
        }


        isLoading.setValue(true);


        String workoutId = activeWorkoutId.getValue();
        if (workoutId == null) {
            Log.e(TAG, "updateExerciseOrder: ID активной тренировки не найден");
            errorMessage.postValue("Не удалось обновить порядок упражнений: ID тренировки не найден");
            isLoading.setValue(false);
            return;
        }


        executor.execute(() -> {
            try {

                for (WorkoutExercise exercise : exercises) {

                    workoutRepository.updateExerciseOrderNumber(exercise.getId(), exercise.getOrderNumber());
                }


                UserWorkout currentWorkout = activeWorkout.getValue();
                if (currentWorkout != null) {

                    List<WorkoutExercise> sortedExercises = new ArrayList<>(exercises);
                    sortedExercises.sort((e1, e2) -> Integer.compare(e1.getOrderNumber(), e2.getOrderNumber()));
                    currentWorkout.setExercises(sortedExercises);


                    Log.d(TAG, "updateExerciseOrder: Новый порядок упражнений:");
                    for (WorkoutExercise ex : sortedExercises) {
                        Log.d(TAG, "   " + ex.getOrderNumber() + ": " + ex.getExercise().getName());
                    }


                    new Handler(Looper.getMainLooper()).post(() -> {
                        activeWorkout.setValue(currentWorkout);
                        isLoading.setValue(false);
                    });


                    Log.d(TAG, "updateExerciseOrder: Сохраняем обновленный порядок упражнений в локальную БД");
                    saveCurrentWorkoutStateToDb();
                } else {
                    isLoading.postValue(false);
                }

                Log.d(TAG, "updateExerciseOrder: Порядок упражнений успешно обновлен");
            } catch (Exception e) {
                Log.e(TAG, "updateExerciseOrder: Ошибка при обновлении порядка упражнений", e);
                errorMessage.postValue("Ошибка при обновлении порядка упражнений: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }


    public void updateWorkoutExerciseSets(String workoutExerciseId, List<ExerciseSet> updatedSets) {
        Log.d(TAG, "updateWorkoutExerciseSets: Попытка обновить подходы для WorkoutExercise ID: " + workoutExerciseId);
        if (workoutExerciseId == null || updatedSets == null) {
            Log.e(TAG, "updateWorkoutExerciseSets: Невозможно обновить - ID упражнения или список подходов null.");
            return;
        }

        UserWorkout currentWorkout = activeWorkout.getValue();
        if (currentWorkout == null || currentWorkout.getExercises() == null) {
            Log.e(TAG, "updateWorkoutExerciseSets: Невозможно обновить - активная тренировка или ее список упражнений null.");
            return;
        }

        boolean exerciseFound = false;
        for (WorkoutExercise exercise : currentWorkout.getExercises()) {
            if (workoutExerciseId.equals(exercise.getId())) {
                Log.d(TAG, "updateWorkoutExerciseSets: Найдено упражнение " + exercise.getExercise().getName() +
                        ". Старых подходов: " + (exercise.getSetsCompleted() != null ? exercise.getSetsCompleted().size() : "null") +
                        ", Новых подходов: " + updatedSets.size());


                exercise.setSetsCompleted(new ArrayList<>(updatedSets));
                exerciseFound = true;


                Log.d(TAG, "updateWorkoutExerciseSets: Обновленный список подходов:");

                break;
            }
        }

        if (exerciseFound) {


            activeWorkout.postValue(currentWorkout);
            Log.i(TAG, "updateWorkoutExerciseSets: LiveData activeWorkout обновлена после изменения подходов для ID: " + workoutExerciseId);


            executor.execute(() -> {
                try {
                    Log.d(TAG, "updateWorkoutExerciseSets: Сохранение обновленных подходов в базу данных");
                    UserWorkoutEntity entity = workoutDao.getActiveWorkoutEntity(currentWorkout.getUserId());
                    if (entity != null) {

                        for (ExerciseSet set : updatedSets) {
                            if (set.getId() != null && !set.getId().isEmpty() && !set.getId().startsWith("temp_")) {
                                workoutRepository.updateSet(set.getId(), set);
                                Log.d(TAG, "updateWorkoutExerciseSets: Подход ID: " + set.getId() +
                                        " обновлен в БД (выполнен: " + set.isCompleted() + ")");
                            }
                        }


                        saveCurrentWorkoutStateToDb();
                        Log.d(TAG, "updateWorkoutExerciseSets: Состояние тренировки сохранено в БД");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "updateWorkoutExerciseSets: Ошибка при сохранении в БД: " + e.getMessage(), e);
                }
            });


            if (currentWorkout.getUserId() != null) {
                Log.d(TAG, "updateWorkoutExerciseSets: Запуск автоматического расчета калорий после обновления подходов");
                calculateRealTimeCalories(currentWorkout.getUserId());
            }
        } else {
            Log.w(TAG, "updateWorkoutExerciseSets: Упражнение с ID " + workoutExerciseId + " не найдено в активной тренировке.");
        }
    }


    public void updateWorkoutStartTime(String workoutId, long startTime) throws Exception {
        if (workoutId == null || workoutId.isEmpty()) {
            throw new IllegalArgumentException("ID тренировки не может быть пустым");
        }

        Log.d(TAG, "updateWorkoutStartTime: Обновление времени начала тренировки " + workoutId);


        executor.execute(() -> {
            try {
                workoutRepository.updateWorkoutStartTime(workoutId, startTime);
                UserWorkoutEntity entity = workoutDao.getWorkoutEntityById(workoutId);
                if (entity != null) {

                    entity.setStartTime(startTime);

                    workoutDao.updateWorkout(entity);
                    Log.i(TAG, "Время начала тренировки обновлено в локальной БД: " + startTime);


                    UserWorkout currentWorkout = activeWorkout.getValue();
                    if (currentWorkout != null && workoutId.equals(currentWorkout.getId())) {
                        currentWorkout.setStartTime(startTime);
                        activeWorkout.postValue(currentWorkout);
                        Log.d(TAG, "LiveData обновлено с новым временем начала тренировки");
                    }
                } else {
                    Log.e(TAG, "Не найдена тренировка с ID " + workoutId + " в локальной БД");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении времени начала тренировки в локальной БД: " + e.getMessage(), e);
            }
        });
    }

    public void createSuperset(List<WorkoutExercise> selectedExercises) throws Exception {
        workoutRepository.createSuperset(selectedExercises);
    }


    private void cleanupOldUnfinishedWorkouts(String userId) {
        try {

            List<UserWorkoutEntity> unfinishedWorkouts = workoutDao.getAllUnfinishedWorkouts(userId);

            if (unfinishedWorkouts == null || unfinishedWorkouts.size() <= 1) {

                Log.d(TAG, "cleanupOldUnfinishedWorkouts: Нет старых незавершенных тренировок для очистки");
                return;
            }


            Log.i(TAG, "cleanupOldUnfinishedWorkouts: Найдено " + unfinishedWorkouts.size() +
                    " незавершенных тренировок. Оставляем самую свежую (ID: " + unfinishedWorkouts.get(0).getId() + ")");


            int deletedCount = 0;
            for (int i = 1; i < unfinishedWorkouts.size(); i++) {
                UserWorkoutEntity oldWorkout = unfinishedWorkouts.get(i);
                String workoutId = oldWorkout.getId();

                try {

                    workoutDao.deleteWorkoutCompletely(workoutId);
                    Log.d(TAG, "cleanupOldUnfinishedWorkouts: Удалена старая тренировка из Room: " + workoutId);


                    try {
                        workoutRepository.deleteWorkout(workoutId);
                        Log.d(TAG, "cleanupOldUnfinishedWorkouts: Удалена старая тренировка из Supabase: " + workoutId);
                    } catch (Exception supabaseEx) {

                        Log.w(TAG, "cleanupOldUnfinishedWorkouts: Не удалось удалить тренировку из Supabase (возможно, её там уже нет): " +
                                workoutId + ", ошибка: " + supabaseEx.getMessage());
                    }

                    deletedCount++;
                } catch (Exception ex) {
                    Log.e(TAG, "cleanupOldUnfinishedWorkouts: Ошибка при удалении старой тренировки " + workoutId + ": " + ex.getMessage(), ex);

                }
            }

            if (deletedCount > 0) {
                Log.i(TAG, "cleanupOldUnfinishedWorkouts: ✅ Успешно удалено " + deletedCount + " старых незавершенных тренировок");
            }

        } catch (Exception e) {

            Log.e(TAG, "cleanupOldUnfinishedWorkouts: Общая ошибка при очистке старых тренировок: " + e.getMessage(), e);
        }
    }
}