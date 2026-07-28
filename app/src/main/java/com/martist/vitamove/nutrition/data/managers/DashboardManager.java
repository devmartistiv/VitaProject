package com.martist.vitamove.nutrition.data.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.dashboard.DashboardData;
import com.martist.vitamove.steps.data.StepCounterManager;
import com.martist.vitamove.steps.data.StepHistoryRepository;
import com.martist.vitamove.user.UserProfile;
import com.martist.vitamove.user.UserRepository;
import com.martist.vitamove.water.data.WaterHistoryManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class DashboardManager {
    private static final String TAG = "DashboardManager";

    private static DashboardManager instance;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final UserRepository userRepository;
    private final StepCounterManager stepCounterManager;
    private final StepHistoryRepository stepHistoryRepository;
    private final WaterHistoryManager waterHistoryManager;


    private static final String PREF_FILE = "dashboard_prefs";
    private static final String KEY_STEPS_GOAL = "steps_goal";
    private static final String KEY_WATER_GOAL = "water_goal";
    private static final String KEY_WEIGHT = "current_weight";
    private static final String KEY_PROTEIN_GOAL = "protein_goal";
    private static final String KEY_FATS_GOAL = "fats_goal";
    private static final String KEY_CARBS_GOAL = "carbs_goal";
    private static final String KEY_WATER_CONSUMED = "water_consumed";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";


    private int stepsToday = 0;
    private int stepsGoal = 10000;
    private float stepsProgress = 0.0f;
    private int caloriesGoal = 3178;
    private int caloriesConsumed = 0;
    private final int caloriesBurned = 527;
    private float waterConsumed = 0.0f;
    private float waterGoal = 2.5f;
    private float currentWeight = 78.5f;
    private final float weightChange = -0.5f;


    private int proteinConsumed = 0;
    private int fatsConsumed = 0;
    private int carbsConsumed = 0;
    private int proteinGoal = 120;
    private int fatsGoal = 70;
    private int carbsGoal = 250;


    public static synchronized DashboardManager getInstance(Context context) {
        if (instance == null) {
            instance = new DashboardManager(context.getApplicationContext());
        }
        return instance;
    }


    public static synchronized void resetInstance() {
        if (instance != null) {
            Log.d(TAG, "Сброс экземпляра DashboardManager");
            instance = null;
        }
    }


    private DashboardManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        this.userRepository = new UserRepository(context);
        this.stepHistoryRepository = StepHistoryRepository.Companion.getInstance(context);
        this.waterHistoryManager = WaterHistoryManager.getInstance(context);


        this.stepCounterManager = StepCounterManager.getInstance(context);
        boolean sensorAvailable = this.stepCounterManager.startTracking();
        if (!sensorAvailable) {
            Log.w(TAG, "Сенсор шагомера недоступен, будем использовать имитацию данных");
        }


        stepsGoal = sharedPreferences.getInt(KEY_STEPS_GOAL, 10000);
        waterGoal = sharedPreferences.getFloat(KEY_WATER_GOAL, 2.5f);
        currentWeight = sharedPreferences.getFloat(KEY_WEIGHT, 78.5f);


        waterConsumed = waterHistoryManager.getTotalWaterConsumption();


        proteinGoal = sharedPreferences.getInt(KEY_PROTEIN_GOAL, 120);
        fatsGoal = sharedPreferences.getInt(KEY_FATS_GOAL, 70);
        carbsGoal = sharedPreferences.getInt(KEY_CARBS_GOAL, 250);


        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        caloriesGoal = userPrefs.getInt("target_calories", 3178);


        updateWaterGoalFromProfile();


        checkAndResetDailyDataIfNeeded();


        syncStepsData();


    }


    public DashboardData getDashboardData() {


        syncStepsData();


        waterConsumed = waterHistoryManager.getTotalWaterConsumption();

        DashboardData data = new DashboardData();


        data.setStepsToday(stepsToday);
        data.setStepsGoal(stepsGoal);
        data.setStepsProgress(stepsProgress);
        data.setStepsHistory(generateStepsHistory());


        data.setCaloriesGoal(caloriesGoal);
        data.setCaloriesConsumed(caloriesConsumed);
        data.setCaloriesBurned(caloriesBurned);


        data.setWaterConsumed(waterConsumed);
        data.setWaterGoal(waterGoal);


        data.setCurrentWeight(currentWeight);
        data.setWeightChange(weightChange);
        data.setWeightHistory(generateWeightHistory());


        data.setProteinConsumed(proteinConsumed);
        data.setFatsConsumed(fatsConsumed);
        data.setCarbsConsumed(carbsConsumed);
        data.setProteinGoal(proteinGoal);
        data.setFatsGoal(fatsGoal);
        data.setCarbsGoal(carbsGoal);

        return data;
    }


    public void addWaterConsumption(float amount) {

        waterConsumed = waterConsumed + amount;


        sharedPreferences.edit().putFloat(KEY_WATER_CONSUMED, waterConsumed).apply();


    }


    private List<Integer> generateStepsHistory() {

        List<Integer> weeklySteps = stepHistoryRepository.getStepsForLastWeek();


        return weeklySteps;
    }


    private List<Float> generateWeightHistory() {
        return new ArrayList<>(Arrays.asList(80.2f, 79.8f, 79.5f, 79.1f, 78.7f, currentWeight));
    }


    public void syncStepsData() {

        checkAndResetDailyDataIfNeeded();


        if (stepCounterManager != null) {

            stepCounterManager.checkAndResetForNewDayIfNeeded();


            int previousStepsToday = stepsToday;
            stepsToday = stepCounterManager.getStepsToday();


            if (Math.abs(previousStepsToday - stepsToday) > 1000) {
                Log.w(TAG, "Обнаружено значительное изменение шагов: " + previousStepsToday +
                        " -> " + stepsToday + " (" + (stepsToday - previousStepsToday) + ")");
            }


            stepsProgress = (float) stepsToday / stepsGoal;


            stepHistoryRepository.saveStepsForToday(stepsToday);

            Log.d(TAG, "Синхронизированы данные о шагах: " + stepsToday + " из " + stepsGoal);
        } else {
            Log.w(TAG, "StepCounterManager не инициализирован, невозможно синхронизировать данные о шагах");
        }
    }


    public void syncStepsForStatistics() {

        syncStepsData();


        Log.d(TAG, "Синхронизированы данные для статистики шагов");
    }


    public void resetDailyData() {
        stepsToday = 0;
        waterConsumed = 0.0f;
        sharedPreferences.edit().putFloat(KEY_WATER_CONSUMED, waterConsumed).apply();
        caloriesConsumed = 0;
        proteinConsumed = 0;
        fatsConsumed = 0;
        carbsConsumed = 0;


        waterHistoryManager.resetDailyData();
    }


    public void stopTracking() {
        if (stepCounterManager != null) {
            stepCounterManager.stopTracking();
        }
    }


    public void updateWaterGoalFromProfile() {
        UserProfile profile = userRepository.getCurrentUserProfile();
        if (profile != null) {
            float oldWaterGoal = waterGoal;
            waterGoal = profile.getTargetWater();


            sharedPreferences.edit().putFloat(KEY_WATER_GOAL, waterGoal).apply();


            Log.d(TAG, "Цель по воде обновлена: " + oldWaterGoal + " л -> " + waterGoal + " л");
        }
    }


    public void updateCaloriesGoalFromProfile() {
        UserProfile profile = userRepository.getCurrentUserProfile();
        if (profile != null) {
            int oldCaloriesGoal = caloriesGoal;
            caloriesGoal = profile.getTargetCalories();


            sharedPreferences.edit().putInt("target_calories", caloriesGoal).apply();


            CaloriesManager caloriesManager = CaloriesManager.getInstance(context);
            caloriesManager.setTargetCalories(caloriesGoal);


            Log.d(TAG, "Целевые калории обновлены: " + oldCaloriesGoal + " ккал -> " + caloriesGoal + " ккал");
        }
    }


    public void updateProteinGoal(int proteinGoal) {
        int oldProteinGoal = this.proteinGoal;
        this.proteinGoal = proteinGoal;


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_PROTEIN_GOAL, proteinGoal);
        editor.apply();


        Log.d(TAG, "Цель по белкам обновлена: " + oldProteinGoal + " г -> " + proteinGoal + " г");
    }


    public void updateFatsGoal(int fatsGoal) {
        int oldFatsGoal = this.fatsGoal;
        this.fatsGoal = fatsGoal;


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_FATS_GOAL, fatsGoal);
        editor.apply();


        Log.d(TAG, "Цель по жирам обновлена: " + oldFatsGoal + " г -> " + fatsGoal + " г");
    }


    public void updateCarbsGoal(int carbsGoal) {
        int oldCarbsGoal = this.carbsGoal;
        this.carbsGoal = carbsGoal;


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_CARBS_GOAL, carbsGoal);
        editor.apply();


        Log.d(TAG, "Цель по углеводам обновлена: " + oldCarbsGoal + " г -> " + carbsGoal + " г");
    }


    public void updateMacroGoals(int proteinGoal, int fatsGoal, int carbsGoal) {
        this.proteinGoal = proteinGoal;
        this.fatsGoal = fatsGoal;
        this.carbsGoal = carbsGoal;


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_PROTEIN_GOAL, proteinGoal);
        editor.putInt(KEY_FATS_GOAL, fatsGoal);
        editor.putInt(KEY_CARBS_GOAL, carbsGoal);
        editor.apply();


        Log.d(TAG, "Цели по БЖУ обновлены: Б=" + proteinGoal + "г, Ж=" + fatsGoal + "г, У=" + carbsGoal + "г");
    }


    public void checkAndResetDailyDataIfNeeded() {
        try {

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long todayInMillis = today.getTimeInMillis();


            long lastResetDate = sharedPreferences.getLong(KEY_LAST_RESET_DATE, 0);


            if (lastResetDate < todayInMillis) {

                resetDailyData();


                sharedPreferences.edit().putLong(KEY_LAST_RESET_DATE, todayInMillis).apply();

                Log.d(TAG, "Данные дашборда сброшены при наступлении нового дня");
            } else {
                Log.d(TAG, "Сброс данных не требуется, последний сброс был сегодня");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке даты для сброса данных: " + e.getMessage(), e);
        }
    }
} 