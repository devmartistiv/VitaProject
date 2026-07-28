package com.martist.vitamove.workout.data.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.NetworkUtils;
import com.martist.vitamove.exercise.data.remote.model.ExerciseDto;
import com.martist.vitamove.set.ExerciseSetEntity;
import com.martist.vitamove.workout.data.dao.WorkoutDao;
import com.martist.vitamove.workout.data.entities.UserWorkoutEntity;
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class WorkoutSyncManager {
    private static final String TAG = "WorkoutSyncManager";
    private static final String PREFS_NAME = "workout_sync_prefs";
    private static final String LAST_SYNC_TIME_KEY = "last_sync_time";

    private static WorkoutSyncManager instance;
    private final Context context;
    private final WorkoutDao workoutDao;
    private final SupabaseWorkoutRepository workoutRepository;
    private final ExecutorService executor;
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);
    private final SharedPreferences syncPrefs;
    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());

    private WorkoutSyncManager(Context context, SupabaseWorkoutRepository workoutRepository) {
        this.context = context.getApplicationContext();
        this.workoutRepository = workoutRepository;
        this.workoutDao = AppDatabase.getInstance(context).workoutDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.syncPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public static synchronized WorkoutSyncManager getInstance(Context context, SupabaseWorkoutRepository repository) {
        if (instance == null) {
            instance = new WorkoutSyncManager(context, repository);
        }
        return instance;
    }


    public void syncAllUnsyncedWorkouts(SyncCallback callback) {

        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "syncAllUnsyncedWorkouts: Нет подключения к интернету, синхронизация отложена");
            if (callback != null) {
                callback.onSyncFailed("Нет подключения к интернету");
            }
            return;
        }


        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(TAG, "syncAllUnsyncedWorkouts: Синхронизация уже выполняется");
            if (callback != null) {
                callback.onSyncFailed("Синхронизация уже выполняется");
            }
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "syncAllUnsyncedWorkouts: Начало синхронизации");


                List<UserWorkoutEntity> unsyncedWorkouts = workoutDao.getUnsyncedWorkouts();

                if (unsyncedWorkouts == null || unsyncedWorkouts.isEmpty()) {
                    Log.d(TAG, "syncAllUnsyncedWorkouts: Нет несинхронизированных тренировок");
                    if (callback != null) {
                        callback.onSyncCompleted(0);
                    }
                    return;
                }

                Log.d(TAG, "syncAllUnsyncedWorkouts: Найдено " + unsyncedWorkouts.size() + " несинхронизированных тренировок");

                int successCount = 0;
                int failCount = 0;


                for (UserWorkoutEntity workout : unsyncedWorkouts) {
                    try {
                        boolean success = syncSingleWorkout(workout);
                        if (success) {
                            successCount++;
                            Log.d(TAG, "syncAllUnsyncedWorkouts: Успешно синхронизирована тренировка " + workout.getId());
                        } else {
                            failCount++;
                            Log.e(TAG, "syncAllUnsyncedWorkouts: Не удалось синхронизировать тренировку " + workout.getId());
                        }
                    } catch (Exception e) {
                        failCount++;
                        Log.e(TAG, "syncAllUnsyncedWorkouts: Ошибка при синхронизации тренировки " + workout.getId(), e);
                    }
                }


                syncPrefs.edit().putLong(LAST_SYNC_TIME_KEY, System.currentTimeMillis()).apply();

                Log.d(TAG, "syncAllUnsyncedWorkouts: Синхронизация завершена. Успешно: " + successCount + ", Ошибок: " + failCount);

                if (callback != null) {
                    if (failCount == 0) {
                        callback.onSyncCompleted(successCount);
                    } else {
                        callback.onSyncFailed("Синхронизировано: " + successCount + ", Ошибок: " + failCount);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "syncAllUnsyncedWorkouts: Критическая ошибка синхронизации", e);
                if (callback != null) {
                    callback.onSyncFailed("Критическая ошибка: " + e.getMessage());
                }
            } finally {
                isSyncing.set(false);
            }
        });
    }


    private boolean syncSingleWorkout(UserWorkoutEntity workout) {
        try {
            Log.d(TAG, "syncSingleWorkout: Начало синхронизации тренировки " + workout.getId());


            List<WorkoutExerciseEntity> exercises = workoutDao.getExercisesForWorkout(workout.getId());


            String currentTime = ISO_DATE_FORMAT.format(new Date());

            JSONObject workoutJson = new JSONObject();
            workoutJson.put("id", workout.getId());
            workoutJson.put("user_id", workout.getUserId());
            workoutJson.put("name", workout.getName());
            workoutJson.put("start_time", workout.getStartTime() > 0 ?
                    ISO_DATE_FORMAT.format(new Date(workout.getStartTime())) : JSONObject.NULL);
            workoutJson.put("end_time", workout.getEndTime() != null ?
                    ISO_DATE_FORMAT.format(new Date(workout.getEndTime())) : JSONObject.NULL);
            workoutJson.put("total_calories", workout.getTotalCalories());
            workoutJson.put("notes", workout.getNotes() != null ? workout.getNotes() : "");
            workoutJson.put("program_id", workout.getProgramId() != null ? workout.getProgramId() : JSONObject.NULL);
            workoutJson.put("program_day_number", workout.getProgramDayNumber());
            workoutJson.put("program_day_id", workout.getProgramDayId() != null ? workout.getProgramDayId() : JSONObject.NULL);
            workoutJson.put("plan_id", workout.getPlanId() != null ? workout.getPlanId() : JSONObject.NULL);
            workoutJson.put("created_at", currentTime);
            workoutJson.put("updated_at", currentTime);


            boolean success = syncWorkoutToServer(workoutJson);

            if (success) {

                for (WorkoutExerciseEntity exercise : exercises) {
                    syncWorkoutExercise(exercise);
                }


                workoutDao.markWorkoutAsSynced(workout.getId());
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "syncSingleWorkout: Ошибка синхронизации тренировки " + workout.getId(), e);
            return false;
        }
    }

    public boolean syncWorkoutToServer(JSONObject workoutJson) {
        try {
            SupabaseClient supabaseClient = workoutRepository.getSupabaseClient();
            String workoutId = workoutJson.optString("id");

            JSONArray existingWorkouts = supabaseClient.from("workouts")
                    .select("id")
                    .eq("id", workoutId)
                    .executeAndGetArray();

            JSONArray result;
            if (existingWorkouts != null && existingWorkouts.length() > 0) {

                Log.d(TAG, "syncWorkoutToServer: Тренировка существует, обновляем");
                result = supabaseClient.from("workouts")
                        .update(workoutJson)
                        .eq("id", workoutId)
                        .executeAndGetArray();
            } else {

                result = supabaseClient.from("workouts")
                        .insert(workoutJson)
                        .executeAndGetArray();
            }

            if (result != null && result.length() > 0) {
                Log.d(TAG, "syncWorkoutToServer: Тренировка успешно синхронизирована");
                return true;
            } else {
                Log.e(TAG, "syncWorkoutToServer: Пустой ответ от сервера");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "syncWorkoutToServer: Ошибка синхронизации", e);
            return false;
        }
    }


    private void syncWorkoutExercise(WorkoutExerciseEntity exercise) throws Exception {

        List<ExerciseSetEntity> sets = workoutDao.getSetsForExercise(exercise.getId());


        syncWorkoutExerciseToServer(exercise, sets);
    }

    public void syncWorkoutExerciseToServer(com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity exercise,
                                            List<ExerciseSetEntity> sets) throws Exception {
        try {
            SupabaseClient supabaseClient = workoutRepository.getSupabaseClient();
            String exerciseId = exercise.getId();
            String currentTime = ISO_DATE_FORMAT.format(new Date());
            ExerciseDto exerciseDto = new ExerciseDto(exercise, currentTime);
            Gson gson = new Gson();
            JSONObject exerciseJson = new JSONObject(gson.toJson(exerciseDto));


            JSONArray existingExercise = supabaseClient.from("workout_exercises")
                    .select("id")
                    .eq("id", exerciseId)
                    .executeAndGetArray();

            if (existingExercise != null && existingExercise.length() > 0) {

                supabaseClient.from("workout_exercises")
                        .update(exerciseJson)
                        .eq("id", exerciseId)
                        .executeAndGetArray();
            } else {

                supabaseClient.from("workout_exercises")
                        .insert(exerciseJson)
                        .executeAndGetArray();
            }


            if (sets != null && !sets.isEmpty()) {
                for (ExerciseSetEntity set : sets) {
                    String setId = set.getId();
                    JSONObject setJson = new JSONObject();
                    setJson.put("id", setId);
                    setJson.put("workout_exercise_id", set.getWorkoutExerciseId());
                    setJson.put("exercise_id", set.getExerciseId());
                    setJson.put("set_number", set.getSetNumber());
                    setJson.put("weight", set.getWeight() != null ? set.getWeight() : JSONObject.NULL);
                    setJson.put("reps", set.getReps() != null ? set.getReps() : JSONObject.NULL);
                    setJson.put("duration_seconds", set.getDurationSeconds() != null ? set.getDurationSeconds() : JSONObject.NULL);
                    setJson.put("completed", set.isCompleted());
                    setJson.put("created_at", set.getCreatedAt() != null ?
                            ISO_DATE_FORMAT.format(new Date(set.getCreatedAt())) : currentTime);


                    JSONArray existingSet = supabaseClient.from("exercise_sets")
                            .select("id")
                            .eq("id", setId)
                            .executeAndGetArray();

                    if (existingSet != null && existingSet.length() > 0) {

                        supabaseClient.from("exercise_sets")
                                .update(setJson)
                                .eq("id", setId)
                                .executeAndGetArray();
                    } else {

                        supabaseClient.from("exercise_sets")
                                .insert(setJson)
                                .executeAndGetArray();
                    }
                }
            }

            Log.d(TAG, "syncWorkoutExerciseToServer: Упражнение и подходы успешно синхронизированы");
        } catch (Exception e) {
            Log.e(TAG, "syncWorkoutExerciseToServer: Ошибка синхронизации упражнения", e);
            throw e;
        }
    }


    public long getLastSyncTime() {
        return syncPrefs.getLong(LAST_SYNC_TIME_KEY, 0);
    }


    public boolean hasUnsyncedData() {
        try {
            List<UserWorkoutEntity> unsynced = workoutDao.getUnsyncedWorkouts();
            return unsynced != null && !unsynced.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "hasUnsyncedData: Ошибка проверки", e);
            return false;
        }
    }


    public interface SyncCallback {

        void onSyncCompleted(int syncedCount);


        void onSyncFailed(String errorMessage);
    }
}
