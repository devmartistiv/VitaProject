package com.martist.vitamove.workout.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Callback;
import com.martist.vitamove.core.domain.utils.NetworkUtils;
import com.martist.vitamove.exercise.data.local.entities.ExerciseEntity;
import com.martist.vitamove.exercise.data.local.mappers.ExerciseDBToExercise;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.programs.data.local.ProgramRoomCache;
import com.martist.vitamove.set.ExerciseSetEntity;
import com.martist.vitamove.workout.data.dao.WorkoutDao;
import com.martist.vitamove.workout.data.entities.UserWorkoutEntity;
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity;
import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.data.model.WorkoutExercise;
import com.martist.vitamove.workout.data.model.WorkoutPlan;
import com.martist.vitamove.workout.utils.WorkoutRepositoryParsingHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SupabaseWorkoutRepository implements WorkoutRepository {
    private static final String TAG = "SupabaseWorkoutRepo";
    private final SupabaseClient supabaseClient;
    public static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private final Context context;


    private final Map<String, Long> exercisesLoadedTimestamp = new HashMap<>();


    private final Map<String, Exercise> exerciseCache = new HashMap<>();
    private final Map<String, Long> exerciseCacheTimestamps = new HashMap<>();
    WorkoutRepositoryParsingHelper workoutRepositoryParsingHelper = new WorkoutRepositoryParsingHelper();

    private static final long EXERCISE_CACHE_TIMEOUT_MS = 3600000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseWorkoutRepository(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
        this.context = VitaMoveApplication.getContext();

    }

    @Override
    public String createWorkout(String userId) throws Exception {
        try {
            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "createWorkout: userId равен null или пуст");
                throw new IllegalArgumentException("ID пользователя не может быть пустым");
            }

            Log.d(TAG, "createWorkout: Начало создания тренировки для пользователя " + userId);


            String workoutId = java.util.UUID.randomUUID().toString();
            boolean isOnline = NetworkUtils.isNetworkAvailable(context);

            Log.d(TAG, "createWorkout: Статус сети: " + (isOnline ? "ОНЛАЙН" : "ОФЛАЙН"));

            if (isOnline) {

                try {

                    if (supabaseClient.getUserToken() == null) {
                        Log.w(TAG, "createWorkout: Отсутствует токен авторизации, работаем в офлайн-режиме");
                        return createWorkoutOffline(userId, workoutId);
                    }


                    String currentTime = ISO_DATE_FORMAT.format(new Date());

                    JSONObject workoutJson = new JSONObject()
                            .put("id", workoutId)
                            .put("user_id", userId)
                            .put("name", "Тренировка " + LocalDateTime.now().toLocalDate())
                            .put("start_time", JSONObject.NULL)
                            .put("end_time", JSONObject.NULL)
                            .put("total_calories", 0)
                            .put("notes", "")
                            .put("created_at", currentTime)
                            .put("updated_at", currentTime);

                    Log.d(TAG, "createWorkout: Подготовлен JSON для создания: " + workoutJson);

                    JSONArray result = supabaseClient.from("workouts")
                            .insert(workoutJson)
                            .executeAndGetArray();

                    Log.d(TAG, "createWorkout: Получен ответ от сервера: " + result.toString());

                    if (result.length() > 0) {
                        String returnedId = result.getJSONObject(0).getString("id");
                        Log.d(TAG, "createWorkout: Успешно создана тренировка с ID: " + returnedId);
                        return returnedId;
                    }

                    Log.e(TAG, "createWorkout: Пустой результат от сервера, переключаемся на офлайн-режим");
                    return createWorkoutOffline(userId, workoutId);

                } catch (SupabaseClient.TokenRefreshedException e) {
                    Log.d(TAG, "createWorkout: Токен устарел, пробуем обновить");
                    handleTokenRefresh();
                    return createWorkout(userId);
                } catch (Exception e) {

                    if (NetworkUtils.isNetworkError(e.getMessage())) {
                        Log.w(TAG, "createWorkout: Сетевая ошибка, переключаемся на офлайн-режим", e);
                        return createWorkoutOffline(userId, workoutId);
                    } else {
                        throw e;
                    }
                }
            } else {

                Log.d(TAG, "createWorkout: Нет подключения к интернету, создаем тренировку локально");
                return createWorkoutOffline(userId, workoutId);
            }

        } catch (Exception e) {
            Log.e(TAG, "createWorkout: Критическая ошибка создания тренировки", e);
            throw new Exception("Ошибка создания тренировки: " + e.getMessage());
        }
    }


    private String createWorkoutOffline(String userId, String workoutId) {
        Log.d(TAG, "createWorkoutOffline: Создание тренировки в офлайн-режиме с ID: " + workoutId);

        try {
            long currentTime = System.currentTimeMillis();


            UserWorkoutEntity workoutEntity =
                    new UserWorkoutEntity();
            workoutEntity.setId(workoutId);
            workoutEntity.setUserId(userId);
            workoutEntity.setName("Тренировка " + new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(new Date(currentTime)));
            workoutEntity.setStartTime(currentTime);
            workoutEntity.setEndTime(null);
            workoutEntity.setTotalCalories(0);
            workoutEntity.setNotes("");
            workoutEntity.setSynced(false);
            workoutEntity.setLocalCreatedAt(currentTime);


            WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
            workoutDao.insertWorkout(workoutEntity);

            Log.d(TAG, "createWorkoutOffline: Тренировка успешно создана локально с ID: " + workoutId);
            Log.d(TAG, "createWorkoutOffline: Время начала: " + new Date(currentTime));
            Log.d(TAG, "createWorkoutOffline: Тренировка будет синхронизирована при появлении интернета");

            return workoutId;

        } catch (Exception e) {
            Log.e(TAG, "createWorkoutOffline: Ошибка при создании локальной тренировки", e);
            throw new RuntimeException("Не удалось создать тренировку локально: " + e.getMessage());
        }
    }


    @Override
    public String addExerciseToWorkout(String workoutId, String exerciseId, int orderNumber) throws Exception {
        try {

            if (!workoutExists(workoutId)) {
                throw new IllegalArgumentException("Тренировка не найдена");
            }


            String existingId = checkExerciseInWorkout(workoutId, exerciseId);
            if (existingId != null) {
                return existingId;
            }


            boolean isOnline = NetworkUtils.isNetworkAvailable(context);

            if (!isOnline) {
                Log.d(TAG, "addExerciseToWorkout: Нет интернета, создаем упражнение только локально");
                return addExerciseOffline(workoutId, exerciseId, orderNumber);
            }

            String currentTime = ISO_DATE_FORMAT.format(new Date());
            JSONObject exerciseJson = new JSONObject()
                    .put("workout_id", workoutId)
                    .put("exercise_id", exerciseId)
                    .put("order_number", orderNumber)
                    .put("created_at", currentTime);

            JSONArray result = supabaseClient.from("workout_exercises")
                    .insert(exerciseJson)
                    .executeAndGetArray();

            if (result.length() > 0) {
                String newId = result.getJSONObject(0).getString("id");
                Log.d(TAG, "Упражнение добавлено с ID: " + newId);
                return newId;
            }
            throw new Exception("Не удалось добавить упражнение");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка добавления упражнения: " + e.getMessage());

            if (NetworkUtils.isNetworkError(e.getMessage())) {
                Log.w(TAG, "addExerciseToWorkout: Сетевая ошибка, создаем упражнение локально");
                return addExerciseOffline(workoutId, exerciseId, orderNumber);
            }
            throw e;
        }
    }


    private String addExerciseOffline(String workoutId, String exerciseId, int orderNumber) {
        Log.d(TAG, "addExerciseOffline: Добавление упражнения в офлайн-режиме");


        String exerciseWorkoutId = java.util.UUID.randomUUID().toString();


        WorkoutExerciseEntity exerciseEntity =
                new WorkoutExerciseEntity();
        exerciseEntity.setId(exerciseWorkoutId);
        exerciseEntity.setWorkoutId(workoutId);
        exerciseEntity.setBaseExerciseId(exerciseId);
        exerciseEntity.setOrderNumber(orderNumber);
        exerciseEntity.setNotes("");
        exerciseEntity.setRated(false);


        WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
        workoutDao.insertWorkoutExercise(exerciseEntity);


        workoutDao.markWorkoutAsUnsynced(workoutId);

        Log.d(TAG, "addExerciseOffline: Упражнение добавлено локально с ID: " + exerciseWorkoutId);
        return exerciseWorkoutId;
    }

    @Override
    public String addSet(String workoutExerciseId, ExerciseSet set) throws Exception {

        boolean isOnline = NetworkUtils.isNetworkAvailable(context);

        if (!isOnline) {
            Log.d(TAG, "addSet: Нет интернета, создаем подход только локально");
            return addSetOffline(workoutExerciseId, set);
        }

        try {
            JSONObject setJson = new JSONObject()
                    .put("workout_exercise_id", workoutExerciseId)
                    .put("set_number", set.getSetNumber())
                    .put("weight", set.getWeight())
                    .put("reps", set.getReps())
                    .put("is_completed", set.isCompleted())
                    .put("created_at", ISO_DATE_FORMAT.format(new Date()));


            if (set.getExerciseId() != null && !set.getExerciseId().isEmpty()) {
                setJson.put("exercise_id", set.getExerciseId());
                Log.d(TAG, "Добавление подхода с exercise_id: " + set.getExerciseId());
            } else {
                Log.d(TAG, "Добавление подхода без exercise_id, так как он не указан в объекте ExerciseSet");
            }


            if (set.getDurationSeconds() != null) {
                setJson.put("duration_seconds", set.getDurationSeconds());
                Log.d(TAG, "Добавление подхода с длительностью: " + set.getDurationSeconds() + " секунд");
            }


            JSONArray result = supabaseClient.from("exercise_sets")
                    .insert(setJson)
                    .executeAndGetArray();

            if (result.length() > 0) {
                String newSetId = result.getJSONObject(0).getString("id");
                Log.d(TAG, "Подход успешно создан с ID: " + newSetId);
                return newSetId;
            }
            throw new Exception("Не удалось добавить подход");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка добавления подхода: " + e.getMessage());

            if (NetworkUtils.isNetworkError(e.getMessage())) {
                Log.w(TAG, "addSet: Сетевая ошибка, создаем подход локально");
                return addSetOffline(workoutExerciseId, set);
            }
            throw e;
        }
    }


    private String addSetOffline(String workoutExerciseId, ExerciseSet set) {
        Log.d(TAG, "addSetOffline: Добавление подхода в офлайн-режиме");


        String setId = set.getId();
        if (setId == null || setId.isEmpty()) {
            setId = java.util.UUID.randomUUID().toString();
            set.setId(setId);
        }


        if (set.getCreatedAt() == null) {
            set.setCreatedAt(System.currentTimeMillis());
        }


        ExerciseSetEntity setEntity =
                ExerciseSetEntity.fromModel(set, workoutExerciseId);


        WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
        workoutDao.insertExerciseSet(setEntity);


        try {
            WorkoutExerciseEntity exerciseEntity =
                    workoutDao.getWorkoutExerciseById(workoutExerciseId);
            if (exerciseEntity != null) {
                workoutDao.markWorkoutAsUnsynced(exerciseEntity.getWorkoutId());
            }
        } catch (Exception e) {
            Log.e(TAG, "addSetOffline: Не удалось пометить тренировку как несинхронизированную", e);
        }

        Log.d(TAG, "addSetOffline: Подход добавлен локально с ID: " + setId);
        return setId;
    }

    @Override
    public void updateSet(String setId, ExerciseSet set) throws Exception {
        try {
            JSONObject setJson = new JSONObject()
                    .put("weight", set.getWeight())
                    .put("reps", set.getReps())
                    .put("is_completed", set.isCompleted());


            if (set.getExerciseId() != null && !set.getExerciseId().isEmpty()) {
                setJson.put("exercise_id", set.getExerciseId());
                Log.d(TAG, "Обновление подхода с exercise_id: " + set.getExerciseId());
            }


            if (set.getDurationSeconds() != null) {
                setJson.put("duration_seconds", set.getDurationSeconds());
                Log.d(TAG, "Обновление подхода с длительностью: " + set.getDurationSeconds() + " секунд");
            }


            supabaseClient.from("exercise_sets")
                    .update(setJson)
                    .eq("id", setId)
                    .executeUpdate();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обновления подхода: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Exercise> getAllExercises() throws Exception {
        try {
            Log.d(TAG, "Запрос всех упражнений из базы данных");

            JSONArray exercisesArray = supabaseClient.from("exercises")
                    .select("*")

                    .executeAndGetArray();

            List<Exercise> exercises = new ArrayList<>();

            for (int i = 0; i < exercisesArray.length(); i++) {
                JSONObject exerciseJson = exercisesArray.getJSONObject(i);
                Exercise exercise = workoutRepositoryParsingHelper.parseExerciseFromJson(exerciseJson);

                if (exercise != null) {
                    exercises.add(exercise);
                }
            }

            Log.d(TAG, "Получено " + exercises.size() + " упражнений из базы данных");
            return exercises;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении упражнений: " + e.getMessage(), e);
            throw e;
        }
    }


    private boolean workoutExists(String workoutId) throws Exception {

        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "workoutExists: Нет интернета, проверяем в локальной БД");
            WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
            UserWorkoutEntity workout = workoutDao.getWorkoutEntityById(workoutId);
            return workout != null;
        }

        try {
            JSONArray result = supabaseClient.from("workouts")
                    .select("id")
                    .eq("id", workoutId)
                    .executeAndGetArray();
            return result.length() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке существования тренировки: " + e.getMessage());

            if (NetworkUtils.isNetworkError(e.getMessage())) {
                Log.w(TAG, "workoutExists: Сетевая ошибка, проверяем в локальной БД");
                WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
                UserWorkoutEntity workout = workoutDao.getWorkoutEntityById(workoutId);
                return workout != null;
            }
            throw e;
        }
    }

    private String checkExerciseInWorkout(String workoutId, String exerciseId) throws Exception {

        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "checkExerciseInWorkout: Нет интернета, проверяем в локальной БД");
            WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
            List<com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity> exercises = workoutDao.getExercisesForWorkout(workoutId);
            for (WorkoutExerciseEntity exercise : exercises) {
                if (exercise.getBaseExerciseId().equals(exerciseId)) {
                    Log.d(TAG, String.format("Упражнение (ID: %s) уже существует в локальной БД для тренировки (ID: %s) с ID: %s",
                            exerciseId, workoutId, exercise.getId()));
                    return exercise.getId();
                }
            }
            return null;
        }

        try {
            JSONArray result = supabaseClient.from("workout_exercises")
                    .select("id")
                    .eq("workout_id", workoutId)
                    .eq("exercise_id", exerciseId)
                    .executeAndGetArray();

            if (result.length() > 0) {
                String existingId = result.getJSONObject(0).getString("id");
                Log.d(TAG, String.format("Упражнение (ID: %s) уже существует в тренировке (ID: %s) с ID: %s",
                        exerciseId, workoutId, existingId));
                return existingId;
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке существования упражнения в тренировке: " + e.getMessage());

            if (NetworkUtils.isNetworkError(e.getMessage())) {
                Log.w(TAG, "checkExerciseInWorkout: Сетевая ошибка, проверяем в локальной БД");
                WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
                List<WorkoutExerciseEntity> exercises = workoutDao.getExercisesForWorkout(workoutId);
                for (com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity exercise : exercises) {
                    if (exercise.getBaseExerciseId().equals(exerciseId)) {
                        return exercise.getId();
                    }
                }
                return null;
            }
            throw e;
        }
    }


    public void deleteWorkout(String workoutId) throws Exception {
        try {
            supabaseClient.from("workouts")
                    .delete()
                    .eq("id", workoutId)
                    .executeDelete();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting workout: " + e.getMessage());
            throw e;
        }
    }


    public void deleteUnfinishedWorkouts(String userId) throws Exception {
        try {
            supabaseClient.from("workouts")
                    .delete()
                    .eq("user_id", userId)
                    .is("end_time", "null")
                    .executeDelete();
            Log.i(TAG, "Незавершенные тренировки пользователя " + userId + " успешно удалены");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при удалении незавершенных тренировок: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void updateWorkoutStartTime(String workoutId, long startTime) throws Exception {
        try {
            if (workoutId == null || workoutId.isEmpty()) {
                throw new IllegalArgumentException("ID тренировки не может быть пустым");
            }


            String startTimeFormatted = formatDateTimeForDb(startTime);

            JSONObject updateJson = new JSONObject()
                    .put("start_time", startTimeFormatted);


            supabaseClient.from("workouts")
                    .update(updateJson)
                    .eq("id", workoutId)
                    .executeUpdate();

        } catch (SupabaseClient.TokenRefreshedException e) {
            handleTokenRefresh();
            updateWorkoutStartTime(workoutId, startTime);
        } catch (Exception e) {

            throw new Exception("Ошибка обновления времени начала тренировки: " + e.getMessage());
        }
    }

    @Override
    public String createSuperset(List<WorkoutExercise> selectedExercises) throws Exception {
        try {

            String supersetId = java.util.UUID.randomUUID().toString();


            for (int i = 0; i < selectedExercises.size(); i++) {
                WorkoutExercise exercise = selectedExercises.get(i);


                JSONObject updateJson = new JSONObject()
                        .put("superset_id", supersetId)
                        .put("superset_order", i);

                supabaseClient.from("workout_exercises")
                        .update(updateJson)
                        .eq("id", exercise.getId())
                        .executeUpdate();

                Log.d(TAG, "createSuperset: Обновлено в Supabase - " +
                        exercise.getExercise().getName() + ", order=" + i);


                updateSupersetInRoom(exercise.getId(), supersetId, i);


                exercise.setSuperset_id(supersetId);
                exercise.setSuperset_order(i);
            }

            Log.d(TAG, "createSuperset: Суперсет успешно создан: " + supersetId +
                    " с " + selectedExercises.size() + " упражнениями");

            return supersetId;

        } catch (SupabaseClient.TokenRefreshedException e) {
            Log.d(TAG, "createSuperset: Токен устарел, пробуем обновить");
            handleTokenRefresh();
            return createSuperset(selectedExercises);
        } catch (Exception e) {
            Log.e(TAG, "createSuperset: Ошибка создания суперсета", e);
            throw new Exception("Ошибка создания суперсета: " + e.getMessage());
        }
    }


    private void updateSupersetInRoom(String exerciseId, String supersetId, int supersetOrder) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            WorkoutDao dao = db.workoutDao();


            WorkoutExerciseEntity entity = dao.getWorkoutExerciseById(exerciseId);
            if (entity != null) {

                entity.setSuperset_id(supersetId);
                entity.setSuperset_order(supersetOrder);


                dao.updateWorkoutExercise(entity);

                Log.d(TAG, "updateSupersetInRoom: Обновлено в Room - exerciseId=" +
                        exerciseId + ", supersetId=" + supersetId + ", order=" + supersetOrder);
            } else {
                Log.w(TAG, "updateSupersetInRoom: Упражнение не найдено в Room: " + exerciseId);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateSupersetInRoom: Ошибка обновления Room", e);

        }
    }

    @Override
    public void deleteSet(String setId) throws Exception {
        try {
            supabaseClient.from("exercise_sets")
                    .delete()
                    .eq("id", setId)
                    .executeDelete();
            Log.d(TAG, "Подход успешно удален: " + setId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка удаления подхода: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<WorkoutPlan> getWorkoutPlansByDateRange(String userId, long startDate, long endDate) throws Exception {

        String startDateISO = formatDateTimeForDb(startDate);
        String endDateISO = formatDateTimeForDb(endDate);

        JSONArray jsonArray = supabaseClient.from("workout_plans")
                .select("*")
                .eq("user_id", userId)
                .gte("planned_date", startDateISO)
                .lte("planned_date", endDateISO)
                .executeAndGetArray();
        return workoutRepositoryParsingHelper.parseWorkoutPlans(jsonArray);
    }


    private String formatDateTimeForDb(long timestamp) {
        try {
            Date date = new Date(timestamp);

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String result = isoFormat.format(date);

            Log.d(TAG, "!!! VITAMOVE_DEBUG: formatDateTimeForDb - Input timestamp: " + timestamp +
                    ", Java Date: " + date + ", Formatted ISO: " + result);

            return result;
        } catch (Exception e) {
            Log.e(TAG, "!!! VITAMOVE_DEBUG: Ошибка при форматировании даты: " + e.getMessage(), e);

            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.HOUR_OF_DAY, 12);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            return new SimpleDateFormat("yyyy-MM-dd'T'12:00:00.000'Z'", Locale.US).format(cal.getTime());
        }
    }


    public WorkoutPlan getWorkoutPlanById(String id) throws Exception {
        JSONArray jsonArray = supabaseClient.from("workout_plans")
                .select("*")
                .eq("id", id)
                .executeAndGetArray();
        if (jsonArray.length() > 0) {
            return workoutRepositoryParsingHelper.parseWorkoutPlan(jsonArray.getJSONObject(0));
        } else {
            throw new Exception("Workout plan not found");
        }
    }


    @Override
    public Exercise getExerciseById(String id) throws Exception {
        JSONArray jsonArray = supabaseClient.from("exercises")
                .select("*")
                .eq("id", id)
                .executeAndGetArray();
        if (jsonArray.length() > 0) {
            return workoutRepositoryParsingHelper.parseExerciseFromJson(jsonArray.getJSONObject(0));
        }
        return null;
    }


    @Override
    public List<ExerciseSet> getExerciseSetsHistoryById(String exerciseId) throws Exception {
        try {


            JSONArray result = supabaseClient.from("exercise_sets")
                    .select("*")
                    .eq("exercise_id", exerciseId)
                    .order("created_at", true)
                    .executeAndGetArray();

            List<ExerciseSet> sets = new ArrayList<>();

            for (int i = 0; i < result.length(); i++) {
                JSONObject setJson = result.getJSONObject(i);


                if (!setJson.getBoolean("is_completed")) {
                    continue;
                }

                ExerciseSet set = new ExerciseSet();
                set.setId(setJson.getString("id"));
                set.setSetNumber(setJson.getInt("set_number"));


                if (setJson.has("weight") && !setJson.isNull("weight")) {
                    set.setWeight((float) setJson.getDouble("weight"));
                }


                if (setJson.has("reps") && !setJson.isNull("reps")) {
                    set.setReps(setJson.getInt("reps"));
                }

                set.setCompleted(true);
                set.setExerciseId(exerciseId);


                if (setJson.has("workout_exercise_id") && !setJson.isNull("workout_exercise_id")) {
                    set.setWorkoutExerciseId(setJson.getString("workout_exercise_id"));
                }


                if (setJson.has("created_at") && !setJson.isNull("created_at")) {

                    String createdAtStr = setJson.getString("created_at");
                    try {

                        long timestamp = workoutRepositoryParsingHelper.parseIsoDateTime(createdAtStr);
                        set.setCreatedAt(timestamp);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при парсинге даты создания: " + e.getMessage());

                        set.setCreatedAt(System.currentTimeMillis());
                    }
                } else {
                    set.setCreatedAt(System.currentTimeMillis());
                }

                sets.add(set);
            }


            Log.d(TAG, "Загружено " + sets.size() + " подходов для упражнения " + exerciseId);

            return sets;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении истории подходов: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public UserWorkout getWorkoutById(String workoutId) throws Exception {
        if (workoutId == null || workoutId.isEmpty()) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: ID тренировки равен null или пуст");
            throw new IllegalArgumentException("ID тренировки не может быть пустым");
        }

        try {
            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Запрос тренировки с СЕРВЕРА по ID: " + workoutId);
            long startRequestTime = System.currentTimeMillis();


            JSONArray workoutArray = supabaseClient
                    .from("workouts")
                    .select("*")
                    .eq("id", workoutId)
                    .executeAndGetArray();

            long endRequestTime = System.currentTimeMillis();
            long requestDuration = endRequestTime - startRequestTime;

            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Получен ответ от СЕРВЕРА за " + requestDuration +
                    " мс, найдено записей: " + workoutArray.length());

            if (workoutArray.length() == 0) {
                Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Тренировка с ID " + workoutId + " не найдена на СЕРВЕРЕ");
                return null;
            }

            JSONObject workoutJson = workoutArray.getJSONObject(0);

            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Начинаем парсинг данных тренировки с ID: " + workoutId);
            long startParseTime = System.currentTimeMillis();


            UserWorkout workout = new UserWorkout(
                    workoutJson.getString("id"),
                    workoutJson.getString("user_id"),
                    workoutJson.optString("name", "Тренировка"),
                    workoutRepositoryParsingHelper.parseIsoDateTime(workoutJson.getString("start_time")),
                    workoutJson.has("end_time") && !workoutJson.isNull("end_time") ?
                            workoutRepositoryParsingHelper.parseIsoDateTime(workoutJson.getString("end_time")) : null,
                    workoutJson.optInt("total_calories", 0),
                    workoutJson.optString("notes", ""),
                    workoutJson.has("program_id") && !workoutJson.isNull("program_id") ?
                            workoutJson.getString("program_id") : null,
                    workoutJson.has("program_day_number") && !workoutJson.isNull("program_day_number") ?
                            workoutJson.getInt("program_day_number") : 0,
                    workoutJson.has("program_day_id") && !workoutJson.isNull("program_day_id") ?
                            workoutJson.getString("program_day_id") : null,
                    workoutJson.has("plan_id") && !workoutJson.isNull("plan_id") ?
                            workoutJson.getString("plan_id") : null,
                    new ArrayList<>());

            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Тренировка успешно создана из данных СЕРВЕРА, ID: " + workout.getId() +
                    ", название: " + workout.getName() + ", загружаем упражнения");


            loadWorkoutExercises(workout);

            long endParseTime = System.currentTimeMillis();
            long parseDuration = endParseTime - startParseTime;

            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Тренировка с упражнениями успешно загружена с СЕРВЕРА за " +
                    parseDuration + " мс, ID: " + workout.getId() +
                    ", количество упражнений: " + workout.getExercises().size());

            return workout;
        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при получении тренировки с СЕРВЕРА по ID: " + workoutId, e);
            throw new Exception("Не удалось загрузить тренировку: " + e.getMessage());
        }
    }


    @Override
    public void updateWorkoutPlan(WorkoutPlan plan) throws Exception {
        JSONObject planJson = new JSONObject();
        planJson.put("name", plan.getName());


        String plannedDateISO = formatDateTimeForDb(plan.getPlannedDate());
        planJson.put("planned_date", plannedDateISO);

        planJson.put("notes", plan.getNotes());


        String updatedAtISO = formatDateTimeForDb(System.currentTimeMillis());
        planJson.put("updated_at", updatedAtISO);


        if (plan.getProgramId() != null) {
            planJson.put("program_id", plan.getProgramId());
        }
        if (plan.getProgramDayId() != null) {
            planJson.put("program_day_id", plan.getProgramDayId());
        }


        if (plan.getStatus() != null) {
            planJson.put("status", plan.getStatus());
        }

        Log.d(TAG, "Обновление плана тренировки: " + plan.getId() + ", дата: " + new Date(plan.getPlannedDate()) + " -> ISO: " + plannedDateISO);

        supabaseClient.from("workout_plans")
                .update(planJson)
                .eq("id", plan.getId())
                .executeUpdate();


        if (plan.getProgramId() != null) {
            List<WorkoutPlan> plans = new ArrayList<>();
            plans.add(plan);
            ProgramRoomCache.saveWorkoutPlans(plan.getProgramId(), plans);
        }
    }


    @Override
    public List<ExerciseSet> getExerciseSets(String workoutExerciseId) throws Exception {
        try {
            JSONArray result = supabaseClient.from("exercise_sets")
                    .select("*")
                    .eq("workout_exercise_id", workoutExerciseId)
                    .order("set_number", true)
                    .executeAndGetArray();

            List<ExerciseSet> sets = new ArrayList<>();
            for (int i = 0; i < result.length(); i++) {
                JSONObject setJson = result.getJSONObject(i);
                ExerciseSet set = new ExerciseSet();
                set.setId(setJson.getString("id"));
                set.setSetNumber(setJson.getInt("set_number"));
                set.setWeight(setJson.has("weight") ? (float) setJson.getDouble("weight") : null);
                set.setReps(setJson.has("reps") ? setJson.getInt("reps") : null);
                set.setCompleted(setJson.getBoolean("is_completed"));
                set.setWorkoutExerciseId(workoutExerciseId);


                if (setJson.has("exercise_id") && !setJson.isNull("exercise_id")) {
                    set.setExerciseId(setJson.getString("exercise_id"));
                    Log.d(TAG, "getExerciseSets: Загружен подход с exercise_id: " + set.getExerciseId());
                }

                sets.add(set);
            }
            return sets;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения подходов: " + e.getMessage(), e);
            throw e;
        }
    }


    private void handleTokenRefresh() throws Exception {
        try {
            Log.d(TAG, "handleTokenRefresh: Начало обновления токена");
            String newToken = supabaseClient.refreshAccessToken();
            Log.d(TAG, "handleTokenRefresh: Токен успешно обновлен");


            SharedPreferences prefs = VitaMoveApplication.getContext()
                    .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("accessToken", newToken)
                    .putString("refreshToken", supabaseClient.getRefreshToken())
                    .putLong("tokenUpdateTime", System.currentTimeMillis())
                    .apply();

            Log.d(TAG, "handleTokenRefresh: Токены сохранены в SharedPreferences");
        } catch (SupabaseClient.AuthException e) {
            Log.e(TAG, "handleTokenRefresh: Ошибка аутентификации", e);


            SharedPreferences prefs = VitaMoveApplication.getContext()
                    .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .remove("accessToken")
                    .remove("refreshToken")
                    .remove("userId")
                    .apply();

            throw new Exception("Требуется повторная авторизация. Пожалуйста, войдите снова.", e);
        } catch (IOException e) {
            Log.e(TAG, "handleTokenRefresh: Ошибка обновления токена", e);


            if (e.getMessage() != null &&
                    (e.getMessage().contains("недействителен или истек") ||
                            e.getMessage().contains("token is not available") ||
                            e.getMessage().contains("Failed to refresh token"))) {

                SharedPreferences prefs = VitaMoveApplication.getContext()
                        .getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .remove("accessToken")
                        .remove("refreshToken")
                        .remove("userId")
                        .apply();

                throw new Exception("Требуется повторная авторизация. Пожалуйста, войдите снова.", e);
            }

            throw e;
        } catch (Exception e) {
            Log.e(TAG, "handleTokenRefresh: Непредвиденная ошибка", e);
            throw new Exception("Ошибка при обновлении токена: " + e.getMessage(), e);
        }
    }


    @Override
    public String createWorkoutFromPlan(WorkoutPlan plan) throws Exception {
        Log.d(TAG, "Создание тренировки из плана через SQL-функцию: " + plan.getId() + " для пользователя: " + plan.getUserId());

        WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
        String newWorkoutId = null;


        boolean isOnline = NetworkUtils.isNetworkAvailable(context);

        if (!isOnline) {
            Log.w(TAG, "createWorkoutFromPlan: Нет подключения к интернету, создаем пустую тренировку локально");

            return createWorkout(plan.getUserId());
        }

        try {

            JSONObject rpcResult = supabaseClient.rpc("create_workout_from_plan_func")
                    .param("p_user_id", plan.getUserId())
                    .param("p_plan_id", plan.getId())
                    .executeAndGetSingle();


            if (rpcResult == null) {
                Log.e(TAG, "RPC вызов create_workout_from_plan_func вернул null объект JSON.");
                throw new Exception("SQL-функция create_workout_from_plan_func вернула неожиданный null.");
            }

            boolean success = rpcResult.optBoolean("success", false);
            newWorkoutId = rpcResult.optString("workout_id", null);


            if (!success || newWorkoutId == null || newWorkoutId.isEmpty() || newWorkoutId.equals("null")) {
                String errorMessage = rpcResult.optString("error", "SQL-функция не вернула действительный ID или сообщила об ошибке.");
                Log.e(TAG, "Ошибка выполнения SQL-функции create_workout_from_plan_func: " + errorMessage + " (Результат RPC: " + rpcResult + ")");
                throw new Exception(errorMessage);
            }

            Log.i(TAG, "SQL-функция успешно создала тренировку в Supabase, ID: " + newWorkoutId);


            UserWorkout createdWorkout = getWorkoutById(newWorkoutId);
            if (createdWorkout == null) {
                Log.e(TAG, "Не удалось загрузить детали тренировки ID: " + newWorkoutId + " после создания через RPC.");
                throw new Exception("Не удалось получить детали созданной тренировки.");
            }


            workoutDao.saveFullWorkout(createdWorkout);
            Log.i(TAG, "Созданная через RPC тренировка ID: " + newWorkoutId + " сохранена в Room.");


            return newWorkoutId;

        } catch (JSONException jsonEx) {
            Log.e(TAG, "Ошибка парсинга JSON ответа от RPC функции create_workout_from_plan_func", jsonEx);
            throw new Exception("Ошибка обработки ответа от SQL-функции: " + jsonEx.getMessage(), jsonEx);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании тренировки из плана через SQL-функцию: " + plan.getId(), e);


            if (NetworkUtils.isNetworkError(e.getMessage())) {
                Log.w(TAG, "createWorkoutFromPlan: Сетевая ошибка, создаем пустую тренировку локально");
                return createWorkout(plan.getUserId());
            }


            throw new Exception("Не удалось создать тренировку из плана (SQL): " + e.getMessage(), e);
        }
    }

    @Override
    public List<UserWorkout> getRecentWorkouts(String userId, int limit) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("ID пользователя не может быть пустым");
        }

        try {

            long currentTimeMillis = System.currentTimeMillis();


            JSONArray workoutsArray = supabaseClient
                    .from("workouts")
                    .select("*")
                    .eq("user_id", userId)
                    .order("start_time", false)
                    .executeAndGetArray();

            List<UserWorkout> workouts = new ArrayList<>();
            for (int i = 0; i < workoutsArray.length(); i++) {
                JSONObject workoutJson = workoutsArray.getJSONObject(i);


                if (!workoutJson.has("start_time") || workoutJson.isNull("start_time") ||
                        !workoutJson.has("end_time") || workoutJson.isNull("end_time")) {
                    continue;
                }

                long startTime = workoutRepositoryParsingHelper.parseIsoDateTime(workoutJson.getString("start_time"));
                long endTime = workoutRepositoryParsingHelper.parseIsoDateTime(workoutJson.getString("end_time"));


                if (endTime <= startTime) {
                    continue;
                }


                if (startTime > currentTimeMillis) {
                    Log.d(TAG, "Пропускаем тренировку с датой из будущего: " + new Date(startTime));
                    continue;
                }

                UserWorkout workout = new UserWorkout(
                        workoutJson.getString("id"),
                        workoutJson.getString("user_id"),
                        workoutJson.optString("name", "Тренировка"),
                        startTime,
                        endTime,
                        workoutJson.optInt("total_calories", 0),
                        workoutJson.optString("notes", ""),
                        workoutJson.has("program_id") && !workoutJson.isNull("program_id") ?
                                workoutJson.getString("program_id") : null,
                        workoutJson.has("program_day_number") && !workoutJson.isNull("program_day_number") ?
                                workoutJson.getInt("program_day_number") : 0,
                        workoutJson.has("program_day_id") && !workoutJson.isNull("program_day_id") ?
                                workoutJson.getString("program_day_id") : null,
                        workoutJson.has("plan_id") && !workoutJson.isNull("plan_id") ?
                                workoutJson.getString("plan_id") : null,
                        new ArrayList<>());
                loadWorkoutExercises(workout);
                workouts.add(workout);
            }


            workouts.sort((w1, w2) -> Long.compare(w2.getStartTime(), w1.getStartTime()));

            Log.d(TAG, "Найдено " + workouts.size() + " тренировок, сортировка: от новых к старым");


            if (workouts.size() > limit) {
                workouts = workouts.subList(0, limit);
            }

            Log.d(TAG, String.format("Загружено %d недавних тренировок для пользователя %s", workouts.size(), userId));
            return workouts;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении последних тренировок", e);
            throw new Exception("Не удалось загрузить последние тренировки: " + e.getMessage());
        }
    }

    @Override
    public WorkoutPlan getTodayWorkoutPlan(String userId) throws Exception {
        Log.d(TAG, "Получение плана тренировки на сегодня для пользователя: " + userId);


        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "getTodayWorkoutPlan: Невозможно выполнить запрос, userId is null or empty.");

            return null;
        }


        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "getTodayWorkoutPlan: Нет подключения к интернету, работаем в офлайн-режиме");
            return null;
        }


        updatePastUncompletedPlans(userId);

        Calendar calendar = Calendar.getInstance();
        long todayStart = getDayStart(calendar.getTimeInMillis());
        long todayEnd = getDayEnd(calendar.getTimeInMillis());

        try {
            JSONArray result = supabaseClient.from("workout_plans")
                    .select("*")
                    .eq("user_id", userId)
                    .eq("status", "planned")
                    .gte("planned_date", formatDateTimeForDb(todayStart))
                    .lte("planned_date", formatDateTimeForDb(todayEnd))
                    .executeAndGetArray();

            if (result.length() > 0) {
                WorkoutPlan plan = workoutRepositoryParsingHelper.parseWorkoutPlan(result.getJSONObject(0));
                Log.d(TAG, "Найден план тренировки на сегодня: " + plan.getId() + ", статус: " + plan.getStatus());
                return plan;
            } else {
                Log.d(TAG, "Планов тренировок со статусом 'planned' на сегодня не найдено");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении плана тренировки на сегодня: " + e.getMessage());

            if (NetworkUtils.isNetworkError(e.getMessage())) {
                Log.w(TAG, "getTodayWorkoutPlan: Сетевая ошибка, работаем в офлайн-режиме");
                return null;
            }
            throw e;
        }
    }


    private void updatePastUncompletedPlans(String userId) {

        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "updatePastUncompletedPlans: Нет подключения к интернету, пропускаем обновление статусов");
            return;
        }

        try {

            Calendar calendar = Calendar.getInstance();
            long todayStart = getDayStart(calendar.getTimeInMillis());


            JSONArray pastPlans = supabaseClient.from("workout_plans")
                    .select("*")
                    .eq("user_id", userId)
                    .eq("status", "planned")
                    .lt("planned_date", formatDateTimeForDb(todayStart))
                    .executeAndGetArray();

            Log.d(TAG, "Найдено " + pastPlans.length() + " незавершенных планов тренировок за прошлые дни");


            for (int i = 0; i < pastPlans.length(); i++) {
                JSONObject planJson = pastPlans.getJSONObject(i);
                String planId = planJson.getString("id");


                JSONObject updateData = new JSONObject();
                updateData.put("status", "skipped");

                supabaseClient.from("workout_plans")
                        .update(updateData)
                        .eq("id", planId)
                        .execute();

                Log.d(TAG, "Обновлен статус плана " + planId + " на 'skipped'");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении статусов прошлых тренировок: " + e.getMessage(), e);

        }
    }


    private long getDayStart(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


    private long getDayEnd(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    @Override
    public List<UserWorkout> getWorkoutHistory(String userId, long startDate, long endDate, int page, int pageSize) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("ID пользователя не может быть пустым");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Номер страницы не может быть отрицательным");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть положительным числом");
        }


        final int MAX_RETRIES = 3;
        int retries = 0;
        long retryDelayMs = 1000;
        Exception lastException = null;

        while (retries < MAX_RETRIES) {
            try {

                String startDateStr = formatDateTimeForDb(startDate);
                String endDateStr = formatDateTimeForDb(endDate);

                Log.d(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Запрос тренировок с пагинацией для пользователя " + userId);
                Log.d(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Период с " + startDateStr + " по " + endDateStr);
                Log.d(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Страница " + page + ", размер " + pageSize);


                int offset = page * pageSize;


                JSONArray allWorkouts = supabaseClient.from("workouts")
                        .select("*")
                        .eq("user_id", userId)
                        .gte("start_time", startDateStr)
                        .lte("start_time", endDateStr)
                        .order("start_time", false)
                        .executeAndGetArray();


                JSONArray jsonArray = new JSONArray();
                int endIndex = Math.min(offset + pageSize, allWorkouts.length());

                for (int i = offset; i < endIndex; i++) {
                    try {
                        jsonArray.put(allWorkouts.getJSONObject(i));
                    } catch (JSONException e) {
                        Log.e(TAG, "Ошибка при обработке тренировки " + i, e);
                    }
                }

                Log.d(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Получено " + jsonArray.length() +
                        " тренировок для страницы " + page + " (всего: " + allWorkouts.length() + ")");

                List<UserWorkout> workouts = new ArrayList<>();
                List<String> workoutIds = new ArrayList<>();


                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject workoutJson = jsonArray.getJSONObject(i);

                    try {
                        UserWorkout workout = new UserWorkout(
                                workoutJson.getString("id"),
                                workoutJson.getString("user_id"),
                                workoutJson.optString("name", "Тренировка"),
                                workoutRepositoryParsingHelper.parseIsoDateTime(workoutJson.getString("start_time")),
                                workoutJson.has("end_time") && !workoutJson.isNull("end_time") ?
                                        workoutRepositoryParsingHelper.parseIsoDateTime(workoutJson.getString("end_time")) : null,
                                workoutJson.optInt("total_calories", 0),
                                workoutJson.optString("notes", ""),
                                workoutJson.has("program_id") && !workoutJson.isNull("program_id") ?
                                        workoutJson.getString("program_id") : null,
                                workoutJson.has("program_day_number") && !workoutJson.isNull("program_day_number") ?
                                        workoutJson.getInt("program_day_number") : 0,
                                workoutJson.has("program_day_id") && !workoutJson.isNull("program_day_id") ?
                                        workoutJson.getString("program_day_id") : null,
                                workoutJson.has("plan_id") && !workoutJson.isNull("plan_id") ?
                                        workoutJson.getString("plan_id") : null,
                                new ArrayList<>());

                        workouts.add(workout);
                        workoutIds.add(workout.getId());
                    } catch (Exception e) {
                        Log.e(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Ошибка при обработке тренировки " + i, e);

                    }
                }


                if (!workoutIds.isEmpty()) {
                    loadWorkoutExercisesBatch(workouts, workoutIds);
                }

                return workouts;
            } catch (Exception e) {
                lastException = e;
                retries++;

                String errorMessage = e.getMessage();
                Log.e(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Ошибка (попытка " + retries + "/" + MAX_RETRIES + "): " + errorMessage, e);


                boolean isNetworkError = errorMessage != null &&
                        (errorMessage.contains("Connection reset") ||
                                errorMessage.contains("timeout") ||
                                errorMessage.contains("network") ||
                                errorMessage.contains("connection"));

                if (!isNetworkError) {
                    break;
                }


                try {
                    Log.d(TAG, "!!! VITAMOVE_DEBUG: getWorkoutHistory: Ожидание " + retryDelayMs +
                            "мс перед повторной попыткой");
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }


        throw lastException != null ?
                lastException :
                new Exception("Не удалось получить историю тренировок");
    }


    private void loadWorkoutExercisesBatch(List<UserWorkout> workouts, List<String> workoutIds) throws Exception {
        try {
            long batchStartTime = System.currentTimeMillis();
            Log.d(TAG, "loadWorkoutExercisesBatch: ✅ НАЧАЛО ОПТИМИЗИРОВАННОЙ пакетной загрузки для " + workoutIds.size() + " тренировок");


            Map<String, String> exerciseToWorkoutMap = new HashMap<>();
            Map<String, WorkoutExercise> exerciseMap = new HashMap<>();
            Map<String, List<WorkoutExercise>> workoutExercisesMap = new HashMap<>();
            List<String> exerciseIds = new ArrayList<>();
            List<String> workoutExerciseIds = new ArrayList<>();


            long step1Start = System.currentTimeMillis();


            StringBuilder workoutIdsInClause = new StringBuilder("(");
            for (int i = 0; i < workoutIds.size(); i++) {
                if (i > 0) workoutIdsInClause.append(",");
                workoutIdsInClause.append(workoutIds.get(i));
            }
            workoutIdsInClause.append(")");

            JSONArray allWorkoutExercises = supabaseClient.from("workout_exercises")
                    .select("*")
                    .in("workout_id", workoutIdsInClause.toString())
                    .order("order_number", true)
                    .executeAndGetArray();

            long step1Time = System.currentTimeMillis() - step1Start;
            Log.d(TAG, "loadWorkoutExercisesBatch: ШАГ 1/3 - Получено " + allWorkoutExercises.length() +
                    " workout_exercises за " + step1Time + " мс (было бы " + workoutIds.size() + " запросов без оптимизации)");


            for (int i = 0; i < allWorkoutExercises.length(); i++) {
                JSONObject workoutExerciseJson = allWorkoutExercises.getJSONObject(i);
                String workoutExerciseId = workoutExerciseJson.getString("id");
                String exerciseId = workoutExerciseJson.getString("exercise_id");
                String workoutId = workoutExerciseJson.getString("workout_id");


                WorkoutExercise workoutExercise = new WorkoutExercise();
                workoutExercise.setId(workoutExerciseId);
                workoutExercise.setOrderNumber(workoutExerciseJson.getInt("order_number"));


                if (!workoutExerciseJson.isNull("superset_id")) {
                    workoutExercise.setSuperset_id(workoutExerciseJson.getString("superset_id"));
                }
                workoutExercise.setSuperset_order(workoutExerciseJson.optInt("superset_order", 0));


                exerciseToWorkoutMap.put(workoutExerciseId, workoutId);
                exerciseMap.put(workoutExerciseId, workoutExercise);
                exerciseIds.add(exerciseId);
                workoutExerciseIds.add(workoutExerciseId);


                workoutExercisesMap.computeIfAbsent(workoutId, k -> new ArrayList<>())
                        .add(workoutExercise);
            }


            long step2Start = System.currentTimeMillis();
            Map<String, Exercise> exercisesById = new HashMap<>();


            final Set<String> uniqueExerciseIds = new HashSet<>(exerciseIds);

            if (!uniqueExerciseIds.isEmpty()) {

                StringBuilder exerciseIdsInClause = new StringBuilder("(");
                int idx = 0;
                for (String exerciseId : uniqueExerciseIds) {
                    if (idx > 0) exerciseIdsInClause.append(",");
                    exerciseIdsInClause.append(exerciseId);
                    idx++;
                }
                exerciseIdsInClause.append(")");

                JSONArray allExercises = supabaseClient.from("exercises")
                        .select("*")
                        .in("id", exerciseIdsInClause.toString())
                        .executeAndGetArray();

                long step2Time = System.currentTimeMillis() - step2Start;
                Log.d(TAG, "loadWorkoutExercisesBatch: ШАГ 2/3 - Получено " + allExercises.length() +
                        " базовых упражнений за " + step2Time + " мс (было бы " + uniqueExerciseIds.size() + " запросов без оптимизации)");


                for (int i = 0; i < allExercises.length(); i++) {
                    Exercise exercise = workoutRepositoryParsingHelper.parseExerciseFromJson(allExercises.getJSONObject(i));
                    exercisesById.put(exercise.getId(), exercise);


                    exerciseCache.put(exercise.getId(), exercise);
                    exerciseCacheTimestamps.put(exercise.getId(), System.currentTimeMillis());
                }
            }


            for (WorkoutExercise we : exerciseMap.values()) {
                String exerciseId = null;

                for (Map.Entry<String, String> entry : exerciseToWorkoutMap.entrySet()) {
                    if (entry.getKey().equals(we.getId())) {

                        int weIndex = workoutExerciseIds.indexOf(we.getId());
                        if (weIndex >= 0 && weIndex < exerciseIds.size()) {
                            exerciseId = exerciseIds.get(weIndex);
                            break;
                        }
                    }
                }

                if (exerciseId != null) {
                    Exercise exercise = exercisesById.get(exerciseId);
                    if (exercise != null) {
                        we.setExercise(exercise);
                    }
                }
            }


            long step3Start = System.currentTimeMillis();
            Map<String, List<ExerciseSet>> setsByExerciseId = new HashMap<>();

            if (!workoutExerciseIds.isEmpty()) {

                StringBuilder workoutExerciseIdsInClause = new StringBuilder("(");
                for (int i = 0; i < workoutExerciseIds.size(); i++) {
                    if (i > 0) workoutExerciseIdsInClause.append(",");
                    workoutExerciseIdsInClause.append(workoutExerciseIds.get(i));
                }
                workoutExerciseIdsInClause.append(")");

                JSONArray allSets = supabaseClient.from("exercise_sets")
                        .select("*")
                        .in("workout_exercise_id", workoutExerciseIdsInClause.toString())
                        .order("set_number", true)
                        .executeAndGetArray();

                long step3Time = System.currentTimeMillis() - step3Start;
                Log.d(TAG, "loadWorkoutExercisesBatch: ШАГ 3/3 - Получено " + allSets.length() +
                        " подходов за " + step3Time + " мс (было бы " + workoutExerciseIds.size() + " запросов без оптимизации)");


                for (int i = 0; i < allSets.length(); i++) {
                    JSONObject setJson = allSets.getJSONObject(i);
                    String workoutExerciseId = setJson.getString("workout_exercise_id");

                    ExerciseSet set = new ExerciseSet();
                    set.setId(setJson.getString("id"));
                    set.setSetNumber(setJson.getInt("set_number"));
                    set.setWeight(setJson.has("weight") && !setJson.isNull("weight") ?
                            (float) setJson.getDouble("weight") : null);
                    set.setReps(setJson.has("reps") && !setJson.isNull("reps") ?
                            setJson.getInt("reps") : null);
                    set.setCompleted(setJson.getBoolean("is_completed"));
                    set.setWorkoutExerciseId(workoutExerciseId);

                    if (setJson.has("exercise_id") && !setJson.isNull("exercise_id")) {
                        set.setExerciseId(setJson.getString("exercise_id"));
                    }

                    setsByExerciseId.computeIfAbsent(workoutExerciseId, k -> new ArrayList<>()).add(set);
                }
            }


            for (String workoutExerciseId : exerciseMap.keySet()) {
                WorkoutExercise workoutExercise = exerciseMap.get(workoutExerciseId);
                List<ExerciseSet> sets = setsByExerciseId.getOrDefault(workoutExerciseId, new ArrayList<>());
                workoutExercise.setSetsCompleted(sets);
            }


            if (workouts != null) {
                for (UserWorkout workout : workouts) {
                    List<WorkoutExercise> exercises = workoutExercisesMap.getOrDefault(workout.getId(), new ArrayList<>());
                    workout.setExercises(exercises);
                    Log.d(TAG, String.format("loadWorkoutExercisesBatch: Установлено %d упражнений для тренировки %s",
                            exercises.size(), workout.getId()));
                }
            } else {


                try {
                    AppDatabase db = AppDatabase.getInstance(context);
                    WorkoutDao dao = db.workoutDao();


                    for (Exercise exercise : exercisesById.values()) {
                        ExerciseEntity exerciseEntity = new ExerciseEntity(exercise.getId(),
                                exercise.getName(),
                                exercise.getDescription(),
                                exercise.getDifficulty(),
                                exercise.getExerciseType(), exercise.getMet(),
                                exercise.getCategories(), exercise.getMuscleGroups(),
                                exercise.getMuscleGroupRussianNames(), exercise.getEquipmentRequired(),
                                exercise.getInstructions(), exercise.getCategory());

                        try {
                            db.exerciseDao().insertExercise(exerciseEntity);
                        } catch (Exception e) {

                            Log.w(TAG, "loadWorkoutExercisesBatch: Не удалось сохранить базовое упражнение: " + e.getMessage());
                        }
                    }


                    for (String workoutId : workoutExercisesMap.keySet()) {
                        List<WorkoutExercise> exercises = workoutExercisesMap.get(workoutId);
                        for (WorkoutExercise exercise : exercises) {
                            WorkoutExerciseEntity entity = new WorkoutExerciseEntity();
                            entity.setId(exercise.getId());
                            entity.setWorkoutId(workoutId);
                            entity.setBaseExerciseId(exercise.getExercise().getId());
                            entity.setOrderNumber(exercise.getOrderNumber());

                            try {
                                dao.insertWorkoutExercise(entity);
                            } catch (Exception e) {

                                Log.w(TAG, "loadWorkoutExercisesBatch: Не удалось сохранить workout_exercise: " + e.getMessage());
                            }
                        }
                    }


                    for (String workoutExerciseId : setsByExerciseId.keySet()) {
                        List<ExerciseSet> sets = setsByExerciseId.get(workoutExerciseId);
                        List<ExerciseSetEntity> setEntities = new ArrayList<>();

                        for (ExerciseSet set : sets) {
                            ExerciseSetEntity entity = new ExerciseSetEntity();
                            entity.setId(set.getId());
                            entity.setWorkoutExerciseId(workoutExerciseId);
                            entity.setSetNumber(set.getSetNumber());
                            entity.setWeight(set.getWeight());
                            entity.setReps(set.getReps());
                            entity.setCompleted(set.isCompleted());
                            setEntities.add(entity);
                        }

                        try {
                            dao.insertExerciseSets(setEntities);
                        } catch (Exception e) {

                            Log.w(TAG, "loadWorkoutExercisesBatch: Не удалось сохранить подходы: " + e.getMessage());
                        }
                    }

                    Log.d(TAG, "loadWorkoutExercisesBatch: Данные сохранены в Room");
                } catch (Exception e) {
                    Log.e(TAG, "loadWorkoutExercisesBatch: Ошибка при сохранении в Room: " + e.getMessage(), e);

                }
            }


            long totalTime = System.currentTimeMillis() - batchStartTime;
            int oldRequestCount = workoutIds.size() + uniqueExerciseIds.size() + workoutExerciseIds.size();
            int newRequestCount = 3;

            Log.d(TAG, "loadWorkoutExercisesBatch: ✅ ОПТИМИЗАЦИЯ ЗАВЕРШЕНА");
            Log.d(TAG, "loadWorkoutExercisesBatch: Обработано " + workoutIds.size() + " тренировок");
            Log.d(TAG, "loadWorkoutExercisesBatch: Полная загрузка заняла " + totalTime + " мс");
            Log.d(TAG, "loadWorkoutExercisesBatch: Выполнено " + newRequestCount + " запросов вместо " + oldRequestCount +
                    " (экономия " + (oldRequestCount - newRequestCount) + " запросов, " +
                    String.format("%.1f", ((float) (oldRequestCount - newRequestCount) / oldRequestCount) * 100) + "%)");

        } catch (Exception e) {
            Log.e(TAG, "loadWorkoutExercisesBatch: Ошибка при пакетной загрузке упражнений: " + e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public void saveCompletedWorkout(UserWorkout workout) throws Exception {
        if (workout == null || workout.getId() == null || workout.getUserId() == null || workout.getEndTime() == null) {
            Log.e(TAG, "saveCompletedWorkout (finish only): Некорректные данные для завершения тренировки.");
            throw new IllegalArgumentException("Workout data is invalid for finishing workout record.");
        }
        String workoutId = workout.getId();
        String userId = workout.getUserId();
        Log.d(TAG, "saveCompletedWorkout (finish only): Начало завершения записи тренировки ID: " + workoutId + " в Supabase");


        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "saveCompletedWorkout: Нет интернета, завершение только локально");
            WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
            UserWorkoutEntity workoutEntity = workoutDao.getWorkoutEntityById(workoutId);
            if (workoutEntity != null) {
                workoutEntity.setEndTime(workout.getEndTime());
                workoutEntity.setTotalCalories(workout.getTotalCalories());
                workoutEntity.setNotes(workout.getNotes() != null ? workout.getNotes() : "");
                workoutEntity.setSynced(false);
                workoutDao.updateWorkout(workoutEntity);
                Log.d(TAG, "saveCompletedWorkout: Тренировка завершена локально, будет синхронизирована позже");
            }
            return;
        }

        try {

            JSONObject workoutUpdateJson = new JSONObject();
            workoutUpdateJson.put("user_id", userId);
            workoutUpdateJson.put("end_time", formatDateTimeForDb(workout.getEndTime()));
            workoutUpdateJson.put("total_calories", workout.getTotalCalories());
            workoutUpdateJson.put("notes", workout.getNotes());


            String currentUserIdFromToken = "UNKNOWN";
            try {
                String token = supabaseClient.getUserToken();
                if (token != null) {
                    String[] jwtParts = token.split("\\.");
                    if (jwtParts.length > 1) {
                        String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                        JSONObject jwtJson = new JSONObject(payload);
                        currentUserIdFromToken = jwtJson.optString("sub", "UNKNOWN");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка извлечения ID пользователя из токена: " + e.getMessage());
            }
            Log.d(TAG, "saveCompletedWorkout (finish only): Проверка ID перед UPDATE. Workout UserID: " + userId + ", Current Token UserID: " + currentUserIdFromToken);


            if (!currentUserIdFromToken.equals(userId)) {
                Log.e(TAG, "saveCompletedWorkout (finish only): КРИТИЧЕСКАЯ ОШИБКА! ID пользователя в тренировке (" + userId +
                        ") не совпадает с ID пользователя в текущем токене (" + currentUserIdFromToken + "). Обновление отменено.");
                throw new Exception("Ошибка безопасности: Попытка обновить тренировку другого пользователя.");
            }


            supabaseClient.from("workouts")
                    .update(workoutUpdateJson)
                    .eq("id", workoutId)
                    .executeUpdate();
            Log.i(TAG, "saveCompletedWorkout (finish only): Запись тренировки ID: " + workoutId + " успешно завершена (обновлена) в Supabase.");

        } catch (SupabaseClient.TokenRefreshedException e) {
            Log.w(TAG, "saveCompletedWorkout (finish only): Токен обновлен во время завершения. Повторная попытка...");
            handleTokenRefresh();
            saveCompletedWorkout(workout);
        } catch (Exception e) {
            Log.e(TAG, "saveCompletedWorkout (finish only): Ошибка при завершении записи тренировки ID: " + workoutId + " в Supabase", e);


            if (NetworkUtils.isNetworkError(e.getMessage())) {
                Log.w(TAG, "saveCompletedWorkout: Сетевая ошибка, завершение только локально");
                WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();
                UserWorkoutEntity workoutEntity = workoutDao.getWorkoutEntityById(workoutId);
                if (workoutEntity != null) {
                    workoutEntity.setEndTime(workout.getEndTime());
                    workoutEntity.setTotalCalories(workout.getTotalCalories());
                    workoutEntity.setNotes(workout.getNotes() != null ? workout.getNotes() : "");
                    workoutEntity.setSynced(false);
                    workoutDao.updateWorkout(workoutEntity);
                    Log.d(TAG, "saveCompletedWorkout: Тренировка завершена локально из-за сетевой ошибки");
                }
                return;
            }


            throw new Exception("Ошибка синхронизации завершения тренировки с сервером: " + e.getMessage(), e);
        }
    }


    @Override
    public void loadWorkoutExercises(UserWorkout workout) throws Exception {
        try {
            String workoutId = workout.getId();


            if (exercisesLoadedTimestamp.containsKey(workoutId)) {
                long lastLoaded = exercisesLoadedTimestamp.get(workoutId);
                long currentTime = System.currentTimeMillis();

                if (currentTime - lastLoaded < 30000 && workout.getExercises() != null && !workout.getExercises().isEmpty()) {
                    Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Используем КЭШ упражнений для тренировки " + workoutId +
                            ", загруженный " + (currentTime - lastLoaded) + "мс назад, количество упражнений: " +
                            workout.getExercises().size());
                    return;
                }
            }


            try {
                AppDatabase db = AppDatabase.getInstance(context);
                WorkoutDao workoutDao = db.workoutDao();


                List<WorkoutExerciseEntity> localExercises = workoutDao.getExercisesForWorkout(workoutId);

                if (localExercises != null && !localExercises.isEmpty()) {
                    List<WorkoutExercise> exercises = new ArrayList<>();

                    for (WorkoutExerciseEntity exerciseEntity : localExercises) {
                        String exerciseId = exerciseEntity.getBaseExerciseId();
                        String workoutExerciseId = exerciseEntity.getId();


                        Exercise exercise = null;
                        if (exerciseCache.containsKey(exerciseId)) {
                            exercise = exerciseCache.get(exerciseId);
                        } else {

                            try {
                                ExerciseEntity exerciseEntityObj =
                                        db.exerciseDao().getExerciseById(exerciseId);
                                exercise = new ExerciseDBToExercise().invoke(exerciseEntityObj);

                            } catch (Exception e) {
                                Log.w(TAG, "Не удалось загрузить базовое упражнение из локальной БД: " + e.getMessage());
                            }
                        }


                        if (exercise == null) {
                            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Упражнение не найдено в локальной БД, поэтому загружаем с сервера");

                            break;
                        }


                        List<ExerciseSetEntity> localSets = workoutDao.getSetsForExercise(workoutExerciseId);
                        List<ExerciseSet> sets = new ArrayList<>();

                        if (localSets != null && !localSets.isEmpty()) {
                            for (ExerciseSetEntity setEntity : localSets) {
                                sets.add(setEntity.toModel());
                            }
                        }


                        WorkoutExercise workoutExercise = exerciseEntity.toModel(exercise, sets);
                        exercises.add(workoutExercise);
                    }

                    if (exercises.size() == localExercises.size()) {

                        workout.setExercises(exercises);
                        exercisesLoadedTimestamp.put(workoutId, System.currentTimeMillis());

                        return;
                    }
                }

            } catch (Exception e) {


            }

            long startLoadTime = System.currentTimeMillis();


            JSONArray workoutExercises = supabaseClient.from("workout_exercises")
                    .select("id, exercise_id, order_number")
                    .eq("workout_id", workout.getId())
                    .order("order_number", true)
                    .executeAndGetArray();

            long step1Time = System.currentTimeMillis() - startLoadTime;
            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: ШАГ 1/3 - Получено " + workoutExercises.length() +
                    " записей workout_exercises за " + step1Time + " мс");

            if (workoutExercises.length() == 0) {
                workout.setExercises(new ArrayList<>());
                return;
            }


            List<String> exerciseIds = new ArrayList<>();
            List<String> workoutExerciseIds = new ArrayList<>();
            Map<String, JSONObject> workoutExerciseMap = new HashMap<>();

            for (int i = 0; i < workoutExercises.length(); i++) {
                JSONObject workoutExerciseJson = workoutExercises.getJSONObject(i);
                String workoutExerciseId = workoutExerciseJson.getString("id");
                String exerciseId = workoutExerciseJson.getString("exercise_id");

                workoutExerciseIds.add(workoutExerciseId);
                workoutExerciseMap.put(workoutExerciseId, workoutExerciseJson);


                if (!exerciseCache.containsKey(exerciseId) ||
                        (System.currentTimeMillis() - exerciseCacheTimestamps.getOrDefault(exerciseId, 0L) >= EXERCISE_CACHE_TIMEOUT_MS)) {
                    exerciseIds.add(exerciseId);
                }
            }


            Map<String, Exercise> exercisesById = new HashMap<>();


            for (int i = 0; i < workoutExercises.length(); i++) {
                JSONObject workoutExerciseJson = workoutExercises.getJSONObject(i);
                String exerciseId = workoutExerciseJson.getString("exercise_id");

                if (exerciseCache.containsKey(exerciseId) &&
                        (System.currentTimeMillis() - exerciseCacheTimestamps.getOrDefault(exerciseId, 0L) < EXERCISE_CACHE_TIMEOUT_MS)) {
                    exercisesById.put(exerciseId, exerciseCache.get(exerciseId));
                    Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Упражнение " + exerciseId + " взято из КЭША");
                }
            }


            if (!exerciseIds.isEmpty()) {
                long step2Start = System.currentTimeMillis();


                StringBuilder inClause = new StringBuilder("(");
                for (int i = 0; i < exerciseIds.size(); i++) {
                    if (i > 0) inClause.append(",");
                    inClause.append(exerciseIds.get(i));
                }
                inClause.append(")");


                JSONArray exercisesResult = supabaseClient.from("exercises")
                        .select("*")
                        .in("id", inClause.toString())
                        .executeAndGetArray();

                long step2Time = System.currentTimeMillis() - step2Start;
                Log.d(TAG, "ИСТОЧНИК ДАННЫХ: ШАГ 2/3 - Получено " + exercisesResult.length() +
                        " базовых упражнений за " + step2Time + " мс (было бы " + exerciseIds.size() + " запросов без оптимизации)");


                for (int i = 0; i < exercisesResult.length(); i++) {
                    JSONObject exerciseJson = exercisesResult.getJSONObject(i);
                    Exercise exercise = workoutRepositoryParsingHelper.parseExerciseFromJson(exerciseJson);
                    exercisesById.put(exercise.getId(), exercise);
                    exerciseCache.put(exercise.getId(), exercise);
                    exerciseCacheTimestamps.put(exercise.getId(), System.currentTimeMillis());
                }
            }


            long step3Start = System.currentTimeMillis();


            StringBuilder setsInClause = new StringBuilder("(");
            for (int i = 0; i < workoutExerciseIds.size(); i++) {
                if (i > 0) setsInClause.append(",");
                setsInClause.append(workoutExerciseIds.get(i));
            }
            setsInClause.append(")");


            JSONArray allSets = supabaseClient.from("exercise_sets")
                    .select("*")
                    .in("workout_exercise_id", setsInClause.toString())
                    .order("set_number", true)
                    .executeAndGetArray();

            long step3Time = System.currentTimeMillis() - step3Start;
            Log.d(TAG, "ИСТОЧНИК ДАННЫХ: ШАГ 3/3 - Получено " + allSets.length() +
                    " подходов за " + step3Time + " мс (было бы " + workoutExerciseIds.size() + " запросов без оптимизации)");


            Map<String, List<ExerciseSet>> setsByWorkoutExerciseId = new HashMap<>();
            for (int i = 0; i < allSets.length(); i++) {
                JSONObject setJson = allSets.getJSONObject(i);
                String workoutExerciseId = setJson.getString("workout_exercise_id");

                ExerciseSet set = new ExerciseSet();
                set.setId(setJson.getString("id"));
                set.setSetNumber(setJson.getInt("set_number"));
                set.setWeight(setJson.has("weight") && !setJson.isNull("weight") ? (float) setJson.getDouble("weight") : null);
                set.setReps(setJson.has("reps") && !setJson.isNull("reps") ? setJson.getInt("reps") : null);
                set.setCompleted(setJson.getBoolean("is_completed"));
                set.setWorkoutExerciseId(workoutExerciseId);

                if (setJson.has("exercise_id") && !setJson.isNull("exercise_id")) {
                    set.setExerciseId(setJson.getString("exercise_id"));
                }

                setsByWorkoutExerciseId.computeIfAbsent(workoutExerciseId, k -> new ArrayList<>()).add(set);
            }


            List<WorkoutExercise> exercises = new ArrayList<>();

            for (int i = 0; i < workoutExercises.length(); i++) {
                JSONObject workoutExerciseJson = workoutExercises.getJSONObject(i);
                String workoutExerciseId = workoutExerciseJson.getString("id");
                String exerciseId = workoutExerciseJson.getString("exercise_id");

                try {
                    WorkoutExercise workoutExercise = new WorkoutExercise();
                    workoutExercise.setId(workoutExerciseId);
                    workoutExercise.setOrderNumber(workoutExerciseJson.getInt("order_number"));

                    Exercise exercise = exercisesById.get(exerciseId);
                    if (exercise != null) {
                        workoutExercise.setExercise(exercise);


                        List<ExerciseSet> sets = setsByWorkoutExerciseId.getOrDefault(workoutExerciseId, new ArrayList<>());
                        workoutExercise.getSetsCompleted().addAll(sets);
                        exercises.add(workoutExercise);

                        Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Добавлено упражнение " + exercise.getName() +
                                " с " + sets.size() + " подходами в объект тренировки");
                    } else {
                        Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Упражнение с ID " + exerciseId + " не найдено");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при обработке упражнения, ID: " +
                            exerciseId + ", ошибка: " + e.getMessage(), e);
                }
            }


            try {
                AppDatabase db = AppDatabase.getInstance(context);
                WorkoutDao dao = db.workoutDao();


                for (Exercise exercise : exercisesById.values()) {
                    try {
                        ExerciseEntity exerciseEntity = new ExerciseEntity(exercise.getId(),
                                exercise.getName(),
                                exercise.getDescription(),
                                exercise.getDifficulty(),
                                exercise.getExerciseType(), exercise.getMet(),
                                exercise.getCategories(), exercise.getMuscleGroups(),
                                exercise.getMuscleGroupRussianNames(), exercise.getEquipmentRequired(),
                                exercise.getInstructions(), exercise.getCategory());

                        db.exerciseDao().insertExercise(exerciseEntity);
                    } catch (Exception e) {

                    }
                }


                List<WorkoutExerciseEntity> workoutExerciseEntities = new ArrayList<>();
                for (WorkoutExercise we : exercises) {
                    workoutExerciseEntities.add(WorkoutExerciseEntity.fromModel(we, workoutId));
                }
                dao.insertWorkoutExercises(workoutExerciseEntities);


                List<ExerciseSetEntity> allSetEntities = new ArrayList<>();
                for (WorkoutExercise we : exercises) {
                    for (ExerciseSet set : we.getSetsCompleted()) {
                        allSetEntities.add(ExerciseSetEntity.fromModel(set, we.getId()));
                    }
                }
                dao.insertExerciseSets(allSetEntities);

                Log.d(TAG, "ИСТОЧНИК ДАННЫХ: Сохранено в Room: " + exercises.size() + " упражнений, " +
                        allSetEntities.size() + " подходов");
            } catch (Exception e) {
                Log.w(TAG, "Не удалось сохранить данные в локальную БД: " + e.getMessage());
            }

            workout.setExercises(exercises);


            exercisesLoadedTimestamp.put(workoutId, System.currentTimeMillis());

        } catch (Exception e) {
            Log.e(TAG, "ИСТОЧНИК ДАННЫХ: Ошибка при загрузке упражнений: " + e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public void updateWorkoutPlanStatus(String planId, String newStatus) throws Exception {
        if (planId == null || planId.isEmpty()) {
            throw new IllegalArgumentException("Plan ID не может быть пустым");
        }


        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "updateWorkoutPlanStatus: Нет интернета, обновление только локально");

            ProgramRoomCache.updateWorkoutPlanStatus(planId, newStatus);
            Log.d(TAG, "updateWorkoutPlanStatus: Статус плана обновлен локально, будет синхронизирован позже");
            return;
        }

        try {
            JSONObject updateData = new JSONObject();
            updateData.put("status", newStatus);


            supabaseClient.from("workout_plans")
                    .eq("id", planId)
                    .update(updateData)
                    .executeUpdate();

            Log.i(TAG, "Статус для WorkoutPlan ID: " + planId + " успешно обновлен на " + newStatus);


            ProgramRoomCache.updateWorkoutPlanStatus(planId, newStatus);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении статуса WorkoutPlan ID: " + planId, e);


            if (NetworkUtils.isNetworkError(e.getMessage())) {
                Log.w(TAG, "updateWorkoutPlanStatus: Сетевая ошибка, обновление только локально");
                ProgramRoomCache.updateWorkoutPlanStatus(planId, newStatus);
                return;
            }

            throw new Exception("Не удалось обновить статус плана тренировки: " + e.getMessage(), e);
        }
    }


    @Override
    public void updateExerciseOrderNumber(String exerciseId, int orderNumber) throws Exception {
        try {
            if (exerciseId == null || exerciseId.isEmpty()) {
                throw new IllegalArgumentException("ID упражнения не может быть пустым");
            }

            Log.d(TAG, "updateExerciseOrderNumber: Обновление порядкового номера упражнения " + exerciseId + " на " + orderNumber);

            JSONObject updateJson = new JSONObject()
                    .put("order_number", orderNumber);


            supabaseClient.from("workout_exercises")
                    .update(updateJson)
                    .eq("id", exerciseId)
                    .executeUpdate();

            Log.d(TAG, "updateExerciseOrderNumber: Порядковый номер упражнения успешно обновлен");
        } catch (SupabaseClient.TokenRefreshedException e) {
            Log.d(TAG, "updateExerciseOrderNumber: Токен устарел, пробуем обновить");
            handleTokenRefresh();
            updateExerciseOrderNumber(exerciseId, orderNumber);
        } catch (Exception e) {
            Log.e(TAG, "updateExerciseOrderNumber: Ошибка обновления порядкового номера упражнения", e);
            throw new Exception("Ошибка обновления порядкового номера упражнения: " + e.getMessage());
        }
    }

    @Override
    public void updateExerciseNote(String exerciseId, String notes) throws Exception {
        try {
            if (exerciseId == null || exerciseId.isEmpty()) {
                throw new IllegalArgumentException("ID упражнения не может быть пустым");
            }

            Log.d(TAG, "updateExerciseNote: Обновление заметки упражнения " + exerciseId +
                    ", текст: " + (notes == null || notes.trim().isEmpty() ? "(пусто)" : notes));

            JSONObject updateJson = new JSONObject()
                    .put("notes", notes != null ? notes : "");


            supabaseClient.from("workout_exercises")
                    .update(updateJson)
                    .eq("id", exerciseId)
                    .executeUpdate();

            Log.d(TAG, "updateExerciseNote: Заметка упражнения успешно обновлена в Supabase");
        } catch (SupabaseClient.TokenRefreshedException e) {
            Log.d(TAG, "updateExerciseNote: Токен устарел, пробуем обновить");
            handleTokenRefresh();
            updateExerciseNote(exerciseId, notes);
        } catch (Exception e) {
            Log.e(TAG, "updateExerciseNote: Ошибка обновления заметки упражнения", e);
            throw new Exception("Ошибка обновления заметки упражнения: " + e.getMessage());
        }
    }


    public Float getLastWeightForExercise(String exerciseId) {
        try {
            if (exerciseId == null || exerciseId.isEmpty()) {
                Log.e(TAG, "getLastWeightForExercise: exerciseId равен null или пуст");
                return null;
            }


            WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();


            Float lastWeight = workoutDao.getLastWeightForExercise(exerciseId);

            if (lastWeight != null && lastWeight > 0) {
                Log.d(TAG, "getLastWeightForExercise: Найден последний использованный вес " + lastWeight + " для упражнения " + exerciseId);
                return lastWeight;
            } else {
                Log.d(TAG, "getLastWeightForExercise: Не найден последний использованный вес для упражнения " + exerciseId);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastWeightForExercise: Ошибка при получении последнего веса: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Integer getLastRepsForExercise(String exerciseId) {
        try {
            if (exerciseId == null || exerciseId.isEmpty()) {
                Log.e(TAG, "getLastRepsForExercise: exerciseId равен null или пуст");
                return null;
            }


            WorkoutDao workoutDao = AppDatabase.getInstance(context).workoutDao();


            Integer lastReps = workoutDao.getLastRepsForExercise(exerciseId);

            if (lastReps != null && lastReps > 0) {
                Log.d(TAG, "getLastRepsForExercise: Найдено последнее использованное количество повторений " + lastReps + " для упражнения " + exerciseId);
                return lastReps;
            } else {
                Log.d(TAG, "getLastRepsForExercise: Не найдено последнее использованное количество повторений для упражнения " + exerciseId);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastRepsForExercise: Ошибка при получении последних повторений: " + e.getMessage(), e);
            return null;
        }
    }


    public void syncUserWorkouts(String userId, Runnable onComplete, Callback<Exception> onError) {
        if (userId == null || userId.isEmpty()) {
            if (onError != null) {
                onError.call(new IllegalArgumentException("ID пользователя не может быть пустым"));
            }
            return;
        }


        final int THREAD_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());
        final ExecutorService parallelExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        executor.execute(() -> {
            try {

                Calendar calendar = Calendar.getInstance();
                long endDate = calendar.getTimeInMillis();
                calendar.add(Calendar.MONTH, -6);
                long startDate = calendar.getTimeInMillis();

                String formattedStartDate = formatDateTimeForDb(startDate);
                String formattedEndDate = formatDateTimeForDb(endDate);


                JSONArray workoutsArray = supabaseClient.from("workouts")
                        .select("*")
                        .eq("user_id", userId)
                        .gte("start_time", formattedStartDate)
                        .lte("start_time", formattedEndDate)
                        .order("start_time", false)
                        .executeAndGetArray();

                int totalWorkouts = workoutsArray.length();

                AppDatabase db = AppDatabase.getInstance(context);
                WorkoutDao dao = db.workoutDao();


                List<String> workoutIds = new ArrayList<>();
                List<UserWorkoutEntity> workoutEntities = new ArrayList<>();


                for (int i = 0; i < totalWorkouts; i++) {
                    JSONObject workoutJson = workoutsArray.getJSONObject(i);
                    String workoutId = workoutJson.getString("id");
                    workoutIds.add(workoutId);


                    UserWorkoutEntity entity = new UserWorkoutEntity();
                    entity.setId(workoutId);
                    entity.setUserId(userId);
                    entity.setName(workoutJson.getString("name"));
                    entity.setStartTime(workoutRepositoryParsingHelper.parseIsoDateTime(workoutJson.getString("start_time")));


                    if (!workoutJson.isNull("end_time")) {
                        entity.setEndTime(workoutRepositoryParsingHelper.parseIsoDateTime(workoutJson.getString("end_time")));
                    } else {
                        entity.setEndTime(null);
                    }

                    entity.setTotalCalories(workoutJson.optInt("total_calories", 0));
                    entity.setNotes(workoutJson.optString("notes", ""));


                    if (!workoutJson.isNull("program_id")) {
                        entity.setProgramId(workoutJson.getString("program_id"));
                    }

                    entity.setProgramDayNumber(workoutJson.optInt("program_day_number", 0));

                    if (!workoutJson.isNull("program_day_id")) {
                        entity.setProgramDayId(workoutJson.getString("program_day_id"));
                    }

                    if (!workoutJson.isNull("plan_id")) {
                        entity.setPlanId(workoutJson.getString("plan_id"));
                    }

                    workoutEntities.add(entity);
                }


                for (UserWorkoutEntity entity : workoutEntities) {
                    dao.insertWorkout(entity);
                }

                Log.d(TAG, "Сохранено " + workoutEntities.size() + " тренировок в локальную базу данных");


                if (workoutIds.isEmpty()) {
                    Log.d(TAG, "Нет тренировок для синхронизации");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    parallelExecutor.shutdown();
                    return;
                }


                List<String> priorityWorkoutIds = new ArrayList<>();
                int priorityCount = Math.min(3, workoutIds.size());
                for (int i = 0; i < priorityCount; i++) {
                    priorityWorkoutIds.add(workoutIds.get(i));
                }


                Log.d(TAG, "Приоритетная загрузка " + priorityCount + " последних тренировок");
                for (String workoutId : priorityWorkoutIds) {
                    try {
                        UserWorkout workout = getWorkoutById(workoutId);
                        if (workout != null) {

                            dao.saveFullWorkout(workout);
                            Log.d(TAG, "Приоритетная тренировка " + workoutId + " полностью загружена и сохранена");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при загрузке приоритетной тренировки " + workoutId + ": " + e.getMessage());
                    }
                }


                workoutIds.removeAll(priorityWorkoutIds);


                final int BATCH_SIZE = Math.max(5, workoutIds.size() / THREAD_POOL_SIZE);
                List<List<String>> workoutBatches = new ArrayList<>();

                for (int i = 0; i < workoutIds.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, workoutIds.size());
                    workoutBatches.add(workoutIds.subList(i, endIndex));
                }


                final java.util.concurrent.atomic.AtomicInteger completedBatches = new java.util.concurrent.atomic.AtomicInteger(0);
                final int totalBatches = workoutBatches.size();

                Log.d(TAG, "Начало параллельной загрузки " + totalBatches + " групп тренировок, по " + BATCH_SIZE + " тренировок в каждой");


                for (List<String> batch : workoutBatches) {
                    parallelExecutor.submit(() -> {
                        try {

                            loadWorkoutExercisesBatch(null, batch);


                            int completed = completedBatches.incrementAndGet();
                            Log.d(TAG, String.format("Загружено %d/%d групп тренировок (%.1f%%)",
                                    completed, totalBatches, (completed * 100.0f / totalBatches)));


                            if (completed == totalBatches) {
                                Log.d(TAG, "Синхронизация тренировок успешно завершена");
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                                parallelExecutor.shutdown();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при загрузке группы тренировок: " + e.getMessage(), e);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при синхронизации тренировок: " + e.getMessage(), e);
                if (onError != null) {
                    onError.call(e);
                }
                parallelExecutor.shutdown();
            }
        });
    }

    public SupabaseClient getSupabaseClient() {
        return supabaseClient;
    }


}