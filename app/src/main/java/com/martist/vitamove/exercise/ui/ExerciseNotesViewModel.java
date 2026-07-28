package com.martist.vitamove.exercise.ui;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.exercise.ui.model.ExerciseNoteHistory;
import com.martist.vitamove.history.ExerciseNoteHistoryRaw;
import com.martist.vitamove.set.OneRepMax;
import com.martist.vitamove.workout.data.dao.WorkoutDao;
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity;
import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.data.model.WorkoutExercise;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ExerciseNotesViewModel extends AndroidViewModel {
    private static final String TAG = "ExerciseNotesViewModel";

    private final WorkoutDao workoutDao;
    private final WorkoutRepository workoutRepository;
    private final Executor executor;


    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<ExerciseNoteHistory>> notesHistory = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noteSaved = new MutableLiveData<>(false);
    private final MutableLiveData<OneRepMax> oneRepMax = new MutableLiveData<>();

    public ExerciseNotesViewModel(@NonNull Application application) {
        super(application);

        VitaMoveApplication app = (VitaMoveApplication) application;
        this.workoutDao = app.getDatabase().workoutDao();
        this.workoutRepository = app.getWorkoutRepository();
        this.executor = Executors.newSingleThreadExecutor();
    }


    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<List<ExerciseNoteHistory>> getNotesHistory() {
        return notesHistory;
    }

    public LiveData<Boolean> getNoteSaved() {
        return noteSaved;
    }

    public LiveData<OneRepMax> getOneRepMax() {
        return oneRepMax;
    }


    public void loadNotesHistory(String baseExerciseId) {
        if (baseExerciseId == null) {
            Log.w(TAG, "loadNotesHistory: baseExerciseId is null");
            errorMessage.setValue("Не удалось загрузить историю заметок: ID упражнения не найден");
            return;
        }

        String userId = ((VitaMoveApplication) getApplication()).getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "loadNotesHistory: userId is null");
            errorMessage.setValue("Пользователь не авторизован");
            return;
        }

        Log.d(TAG, "Загрузка истории заметок для упражнения: " + baseExerciseId);
        isLoading.setValue(true);

        executor.execute(() -> {
            try {
                Log.d(TAG, "ОТЛАДКА: Загружаем историю для baseExerciseId=" + baseExerciseId + ", userId=" + userId);


                List<ExerciseNoteHistoryRaw> allRecords =
                        workoutDao.getAllExerciseNotesForDebug(baseExerciseId);
                Log.d(TAG, "ОТЛАДКА: ВСЕГО записей для упражнения в базе (включая без заметок): " + allRecords.size());
                for (ExerciseNoteHistoryRaw record : allRecords) {
                    Log.d(TAG, "ОТЛАДКА: Запись - заметка: '" + record.getNotes() + "', время: " + record.getStart_time() +
                            ", тренировка: " + record.getWorkout_name() + ", exercise_id: " + record.getExercise_id());
                }

                List<ExerciseNoteHistoryRaw> rawHistory =
                        workoutDao.getExerciseNotesHistory(baseExerciseId, userId);

                Log.d(TAG, "ОТЛАДКА: Найдено " + rawHistory.size() + " записей в основном запросе (только с заметками)");


                if (rawHistory.isEmpty()) {
                    Log.d(TAG, "ОТЛАДКА: Пробуем альтернативный метод без join");
                    rawHistory = workoutDao.getExerciseNotesHistorySimple(baseExerciseId);
                    Log.d(TAG, "ОТЛАДКА: Альтернативный метод нашел " + rawHistory.size() + " записей");
                }


                if (rawHistory.isEmpty()) {
                    Log.d(TAG, "ОТЛАДКА: Пробуем загрузить историю из Supabase");
                    try {
                        rawHistory = loadNotesHistoryFromSupabase(baseExerciseId, userId);
                        Log.d(TAG, "ОТЛАДКА: Из Supabase получено " + rawHistory.size() + " записей");
                    } catch (Exception e) {
                        Log.w(TAG, "ОТЛАДКА: Не удалось загрузить из Supabase: " + e.getMessage());
                    }
                }


                if (rawHistory.isEmpty()) {
                    Log.w(TAG, "ОТЛАДКА: Не удалось найти заметки ни одним из способов для упражнения " + baseExerciseId);
                    errorMessage.postValue("Заметки не найдены. Возможно, данные не синхронизированы.");
                }

                List<ExerciseNoteHistory> history = new ArrayList<>();

                for (ExerciseNoteHistoryRaw raw : rawHistory) {
                    Log.d(TAG, "ОТЛАДКА: Обрабатываем запись - заметка: " + raw.getNotes() +
                            ", время: " + raw.getStart_time() + " (" + new Date(raw.getStart_time()) + ")" +
                            ", тренировка: " + raw.getWorkout_name());
                    ExerciseNoteHistory noteHistory = new ExerciseNoteHistory();
                    noteHistory.setNoteText(raw.getNotes());


                    Date workoutDate;
                    if (raw.getStart_time() > 0) {
                        workoutDate = new Date(raw.getStart_time());
                    } else {

                        workoutDate = new Date();
                        Log.w(TAG, "ОТЛАДКА: Время тренировки не задано, используем текущее время");
                    }
                    noteHistory.setWorkoutDate(workoutDate);


                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    noteHistory.setWorkoutTime(timeFormat.format(workoutDate));

                    noteHistory.setWorkoutName(raw.getWorkout_name());
                    noteHistory.setExerciseId(raw.getExercise_id());
                    noteHistory.setWorkoutId(raw.getWorkout_id());


                    try {
                        int setsCompleted = workoutDao.getCompletedSetsCount(raw.getExercise_id());
                        long durationMinutes = workoutDao.getExerciseDurationMinutes(raw.getExercise_id());

                        noteHistory.setSetsCompleted(setsCompleted);
                        noteHistory.setExerciseDurationMinutes(durationMinutes);
                    } catch (Exception e) {


                    }

                    history.add(noteHistory);
                }

                Log.d(TAG, "Загружено " + history.size() + " записей истории заметок");
                notesHistory.postValue(history);

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке истории заметок", e);
                errorMessage.postValue("Ошибка при загрузке истории заметок: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }


    private List<ExerciseNoteHistoryRaw> loadNotesHistoryFromSupabase(String baseExerciseId, String userId) throws Exception {
        List<ExerciseNoteHistoryRaw> result = new ArrayList<>();

        if (workoutRepository instanceof com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository) {
            com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository supabaseRepo =
                    (com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository) workoutRepository;


            List<UserWorkout> workouts = supabaseRepo.getWorkoutHistory(userId,
                    System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000),
                    System.currentTimeMillis(), 0, 100);


            for (UserWorkout workout : workouts) {
                if (workout.getExercises() != null) {
                    for (WorkoutExercise exercise : workout.getExercises()) {
                        if (exercise.getExercise() != null &&
                                baseExerciseId.equals(exercise.getExercise().getId()) &&
                                exercise.getNotes() != null &&
                                !exercise.getNotes().trim().isEmpty()) {

                            ExerciseNoteHistoryRaw raw = new ExerciseNoteHistoryRaw(exercise.getNotes(), workout.getStartTime(), workout.getName(), exercise.getId(), workout.getId());


                            result.add(raw);
                        }
                    }
                }
            }
        }

        return result;
    }


    public void saveNote(String exerciseId, String noteText) {
        if (exerciseId == null) {
            Log.e(TAG, "saveNote: exerciseId is null");
            errorMessage.setValue("Не удалось сохранить заметку: ID упражнения не найден");
            return;
        }

        Log.d(TAG, "Сохранение заметки для упражнения: " + exerciseId +
                ", текст: " + (noteText == null || noteText.trim().isEmpty() ? "(пусто)" : noteText) +
                ", пользователь: " + ((VitaMoveApplication) getApplication()).getCurrentUserId());

        isLoading.setValue(true);
        noteSaved.setValue(false);

        executor.execute(() -> {
            try {

                WorkoutExerciseEntity exerciseEntity = workoutDao.getWorkoutExerciseById(exerciseId);
                if (exerciseEntity == null) {
                    Log.e(TAG, "Упражнение с ID " + exerciseId + " не найдено в базе данных");
                    errorMessage.postValue("Упражнение не найдено");
                    return;
                }


                List<ExerciseNoteHistoryRaw> beforeSave =
                        workoutDao.getAllExerciseNotesForDebug(exerciseEntity.getBaseExerciseId());
                Log.d(TAG, "ОТЛАДКА СОХРАНЕНИЯ: До сохранения найдено " + beforeSave.size() + " записей");


                exerciseEntity.setNotes(noteText);


                workoutDao.updateWorkoutExercise(exerciseEntity);
                Log.d(TAG, "Заметка упражнения ID: " + exerciseId + " обновлена в локальной БД");


                List<ExerciseNoteHistoryRaw> afterSave =
                        workoutDao.getAllExerciseNotesForDebug(exerciseEntity.getBaseExerciseId());
                Log.d(TAG, "ОТЛАДКА СОХРАНЕНИЯ: После сохранения найдено " + afterSave.size() + " записей");


                try {
                    workoutRepository.updateExerciseNote(exerciseId, noteText);
                    Log.d(TAG, "Заметка синхронизирована с Supabase");
                } catch (Exception e) {
                    Log.w(TAG, "Не удалось синхронизировать заметку с Supabase: " + e.getMessage(), e);

                }

                noteSaved.postValue(true);
                Log.d(TAG, "Заметка успешно сохранена");

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при сохранении заметки", e);
                errorMessage.postValue("Ошибка при сохранении заметки: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }


    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }


    public void loadOneRepMax(String baseExerciseId) {
        if (baseExerciseId == null) {
            Log.w(TAG, "loadOneRepMax: baseExerciseId is null");
            oneRepMax.setValue(new OneRepMax());
            return;
        }

        Log.d(TAG, "Загрузка данных 1ПМ для упражнения: " + baseExerciseId);

        executor.execute(() -> {
            try {

                Float lastWeight = workoutRepository.getLastWeightForExercise(baseExerciseId);
                Integer lastReps = workoutRepository.getLastRepsForExercise(baseExerciseId);

                Log.d(TAG, "1ПМ данные: lastWeight=" + lastWeight + ", lastReps=" + lastReps);


                OneRepMax oneRm = new OneRepMax(lastWeight, lastReps);

                oneRepMax.postValue(oneRm);

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке данных 1ПМ", e);
                oneRepMax.postValue(new OneRepMax());
            }
        });
    }


    public void resetNoteSaved() {
        noteSaved.setValue(false);
    }
}
