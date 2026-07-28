package com.martist.vitamove.nutrition.data.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.workout.data.dao.WorkoutDao;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CaloriesManager {
    private static final String TAG = "CaloriesManager";
    private static final String PREFS_NAME = "calories_data";
    private static final String KEY_COMPLETED_CALORIES = "completed_workout_calories";
    private static final String KEY_ACTIVE_WORKOUT_CALORIES = "active_workout_calories";
    private static final String KEY_CONSUMED_CALORIES = "consumed_calories";
    private static final String KEY_DATE = "calories_date";

    private static CaloriesManager instance;
    private final SharedPreferences prefs;
    private final Context context;
    private final MutableLiveData<Integer> burnedCaloriesLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> consumedCaloriesLiveData = new MutableLiveData<>(0);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WorkoutDao workoutDao;


    public static synchronized CaloriesManager getInstance(Context context) {
        if (instance == null) {
            instance = new CaloriesManager(context.getApplicationContext());
        }
        return instance;
    }


    public static synchronized void resetInstance() {
        if (instance != null) {
            Log.d(TAG, "Сброс экземпляра CaloriesManager");
            instance = null;
        }
    }

    private CaloriesManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


        AppDatabase database = AppDatabase.getInstance(context);
        workoutDao = database.workoutDao();


        resetActiveWorkoutCalories();


        checkDateAndResetIfNeeded();


        loadCaloriesFromDatabase();
    }


    public LiveData<Integer> getBurnedCaloriesLiveData() {
        return burnedCaloriesLiveData;
    }


    public LiveData<Integer> getConsumedCaloriesLiveData() {
        return consumedCaloriesLiveData;
    }


    public void addCompletedWorkoutCalories(int calories) {
        if (calories <= 0) {
            Log.w(TAG, "Попытка добавить отрицательное или нулевое количество калорий: " + calories);
            return;
        }


        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                if (userId != null) {
                    int caloriesFromDb = loadTodayCaloriesFromDatabase(userId);


                    int newTotal = caloriesFromDb + calories;


                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(KEY_COMPLETED_CALORIES, newTotal);
                    editor.apply();

                    Log.d(TAG, "Добавлены калории завершенной тренировки: +" + calories + " кал. Всего из базы: " + caloriesFromDb + " кал. Новый общий итог: " + newTotal + " кал.");


                    updateBurnedCaloriesLiveData();
                } else {
                    Log.w(TAG, "Не удалось получить ID пользователя для обновления калорий");


                    int currentCalories = prefs.getInt(KEY_COMPLETED_CALORIES, 0);
                    int newTotal = currentCalories + calories;

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(KEY_COMPLETED_CALORIES, newTotal);
                    editor.apply();

                    Log.d(TAG, "Добавлены сожженные калории (fallback): +" + calories + " кал. (Всего: " + newTotal + " кал.)");


                    updateBurnedCaloriesLiveData();
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении калорий из базы", e);


                int currentCalories = prefs.getInt(KEY_COMPLETED_CALORIES, 0);
                int newTotal = currentCalories + calories;

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_COMPLETED_CALORIES, newTotal);
                editor.apply();

                Log.d(TAG, "Добавлены сожженные калории (fallback): +" + calories + " кал. (Всего: " + newTotal + " кал.)");


                updateBurnedCaloriesLiveData();
            }
        });
    }


    public void setConsumedCalories(int calories) {
        if (calories < 0) {
            Log.w(TAG, "Попытка установить отрицательное количество потребленных калорий: " + calories);
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_CONSUMED_CALORIES, calories);
        editor.apply();

        updateConsumedCaloriesLiveData();
    }


    public void updateActiveWorkoutCalories(int calories) {
        if (calories < 0) {
            Log.w(TAG, "Попытка установить отрицательное количество калорий для активной тренировки: " + calories);
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_ACTIVE_WORKOUT_CALORIES, calories);
        editor.apply();

        updateBurnedCaloriesLiveData();
    }


    public void resetActiveWorkoutCalories() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_ACTIVE_WORKOUT_CALORIES, 0);
        editor.apply();

        Log.d(TAG, "Сброшены калории активной тренировки");


        updateBurnedCaloriesLiveData();
    }


    public int getCompletedWorkoutCalories() {
        int savedCalories = prefs.getInt(KEY_COMPLETED_CALORIES, -1);


        if (savedCalories == -1) {
            return loadTodayCaloriesFromDatabaseSync();
        }

        return savedCalories;
    }


    public int getActiveWorkoutCalories() {
        return prefs.getInt(KEY_ACTIVE_WORKOUT_CALORIES, 0);
    }


    public int getConsumedCalories() {
        return prefs.getInt(KEY_CONSUMED_CALORIES, 0);
    }


    public int getTotalBurnedCalories() {
        return getCompletedWorkoutCalories() + getActiveWorkoutCalories();
    }


    private void updateBurnedCaloriesLiveData() {
        int totalCalories = getTotalBurnedCalories();
        burnedCaloriesLiveData.postValue(totalCalories);
        Log.d(TAG, "Обновлен LiveData сожженных калорий: " + totalCalories + " кал.");
    }


    private void updateConsumedCaloriesLiveData() {
        int calories = getConsumedCalories();
        consumedCaloriesLiveData.postValue(calories);
        Log.d(TAG, "Обновлен LiveData потребленных калорий: " + calories + " кал.");
    }


    private void checkDateAndResetIfNeeded() {
        String savedDateStr = prefs.getString(KEY_DATE, "");
        String currentDateStr = java.time.LocalDate.now().toString();

        if (!currentDateStr.equals(savedDateStr)) {

            Log.i(TAG, "Обнаружена новая дата. Обновление данных о калориях.");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_ACTIVE_WORKOUT_CALORIES, 0);
            editor.putInt(KEY_CONSUMED_CALORIES, 0);
            editor.putString(KEY_DATE, currentDateStr);

            editor.putInt(KEY_COMPLETED_CALORIES, -1);
            editor.apply();
        }
    }


    public void setTargetCalories(int targetCalories) {
        int minCalories = 1500;
        if (targetCalories < minCalories) {
            targetCalories = minCalories;
        }
        if (targetCalories <= 0) {
            Log.w(TAG, "Попытка установить некорректное целевое количество калорий: " + targetCalories);
            return;
        }


        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        userPrefs.edit().putInt("target_calories", targetCalories).apply();

        Log.d(TAG, "Обновлено целевое количество калорий: " + targetCalories + " ккал");


    }


    private void loadCaloriesFromDatabase() {
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                if (userId == null) {
                    Log.w(TAG, "Не удалось получить ID пользователя для загрузки калорий");

                    initializeDefaultLiveData();
                    return;
                }

                int caloriesFromDb = loadTodayCaloriesFromDatabase(userId);


                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_COMPLETED_CALORIES, caloriesFromDb);
                editor.apply();


                int activeWorkoutCalories = getActiveWorkoutCalories();
                int totalBurnedCalories = caloriesFromDb + activeWorkoutCalories;
                int consumedCalories = getConsumedCalories();

                burnedCaloriesLiveData.postValue(totalBurnedCalories);
                consumedCaloriesLiveData.postValue(consumedCalories);

                Log.d(TAG, "CaloriesManager инициализирован из базы. Завершенные тренировки: " + caloriesFromDb +
                        " кал., Активная тренировка: " + activeWorkoutCalories + " кал., Всего сожжено: " + totalBurnedCalories +
                        " кал., Потреблено: " + consumedCalories + " кал.");

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке калорий из базы данных", e);

                initializeDefaultLiveData();
            }
        });
    }


    private void initializeDefaultLiveData() {
        int activeWorkoutCalories = getActiveWorkoutCalories();
        int completedCalories = prefs.getInt(KEY_COMPLETED_CALORIES, 0);
        int totalBurnedCalories = completedCalories + activeWorkoutCalories;
        int consumedCalories = getConsumedCalories();

        burnedCaloriesLiveData.postValue(totalBurnedCalories);
        consumedCaloriesLiveData.postValue(consumedCalories);

        Log.d(TAG, "CaloriesManager инициализирован дефолтными значениями. Завершенные тренировки: " + completedCalories +
                " кал., Активная тренировка: " + activeWorkoutCalories + " кал., Всего сожжено: " + totalBurnedCalories +
                " кал., Потреблено: " + consumedCalories + " кал.");
    }


    private int loadTodayCaloriesFromDatabaseSync() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                Log.w(TAG, "Не удалось получить ID пользователя для синхронной загрузки калорий");
                return 0;
            }

            return loadTodayCaloriesFromDatabase(userId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при синхронной загрузке калорий из базы данных", e);
            return 0;
        }
    }


    private int loadTodayCaloriesFromDatabase(String userId) {

        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);

        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);

        int totalCalories = workoutDao.getTotalCaloriesForToday(userId,
                todayStart.getTimeInMillis(), todayEnd.getTimeInMillis());

        Log.d(TAG, "Загружено " + totalCalories + " калорий за сегодня из базы данных для пользователя: " + userId);
        return totalCalories;
    }


    private String getCurrentUserId() {
        try {
            if (context instanceof VitaMoveApplication) {
                return ((VitaMoveApplication) context).getCurrentUserId();
            } else {
                VitaMoveApplication app = (VitaMoveApplication) context.getApplicationContext();
                return app.getCurrentUserId();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении ID пользователя", e);
            return null;
        }
    }


    public void forceReloadFromDatabase() {
        Log.d(TAG, "Принудительная перезагрузка калорий из базы данных");
        loadCaloriesFromDatabase();
    }
} 