package com.martist.vitamove.water.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.water.data.model.WaterConsumptionRecord;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class WaterHistoryManager {
    private static final String TAG = "WaterHistoryManager";

    private static WaterHistoryManager instance;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;


    private static final String PREF_FILE = "water_history_prefs";
    private static final String KEY_HISTORY = "water_history";
    private static final String KEY_HISTORY_DATE = "water_history_date";
    private static final String KEY_WEEKLY_HISTORY = "water_weekly_history";
    private static final String KEY_MONTHLY_HISTORY = "water_monthly_history";


    public static synchronized WaterHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new WaterHistoryManager(context.getApplicationContext());
        }
        return instance;
    }


    public static synchronized void resetInstance() {
        if (instance != null) {
            Log.d(TAG, "Сброс экземпляра WaterHistoryManager");
            instance = null;
        }
    }

    private WaterHistoryManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        this.gson = new Gson();


        checkAndResetDailyDataIfNeeded();
    }


    public boolean addWaterRecord(float amount, String description) {
        if (amount <= 0) {
            return false;
        }

        List<WaterConsumptionRecord> history = getWaterHistory();


        WaterConsumptionRecord record = new WaterConsumptionRecord(new Date(), amount, description);


        history.add(record);


        saveWaterHistory(history);


        updateWeeklyData(amount);
        updateMonthlyData(amount);

        updateDailyWaterData(amount);

        Log.d(TAG, "Добавлена запись: " + amount + " л, описание: " +
                (description.isEmpty() ? "не указано" : description));

        return true;
    }


    public boolean deleteWaterRecord(int position) {
        List<WaterConsumptionRecord> history = getWaterHistory();


        if (position < 0 || position >= history.size()) {
            Log.e(TAG, "Невозможно удалить запись: неверный индекс " + position);
            return false;
        }


        WaterConsumptionRecord recordToDelete = history.get(position);
        float amountToRemove = recordToDelete.getAmount();


        history.remove(position);


        saveWaterHistory(history);


        updateWeeklyDataAfterDelete(amountToRemove);
        updateMonthlyDataAfterDelete(amountToRemove);
        updateDailyWaterDataAfterDelete(amountToRemove);

        Log.d(TAG, "Удалена запись: " + amountToRemove + " л");

        return true;
    }


    private void updateWeeklyDataAfterDelete(float amount) {
        List<Float> weeklyData = getWeeklyWaterData();


        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);


        int dayIndex = dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - 2;


        float currentValue = weeklyData.get(dayIndex);
        weeklyData.set(dayIndex, Math.max(0, currentValue - amount));


        saveWeeklyData(weeklyData);
    }


    private void updateMonthlyDataAfterDelete(float amount) {
        List<Float> monthlyData = getMonthlyWaterData();


        Calendar calendar = Calendar.getInstance();
        int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH) - 1;


        if (weekOfMonth >= 0 && weekOfMonth < 4) {

            float currentValue = monthlyData.get(weekOfMonth);
            monthlyData.set(weekOfMonth, Math.max(0, currentValue - amount));


            saveMonthlyData(monthlyData);
        }
    }


    private void updateDailyWaterDataAfterDelete(float amount) {
        Calendar calendar = Calendar.getInstance();
        int currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);


        List<Float> dailyData = getDailyWaterDataForMonth();


        float currentValue = dailyData.get(currentDayOfMonth - 1);
        dailyData.set(currentDayOfMonth - 1, Math.max(0, currentValue - amount));


        saveDailyWaterData(dailyData);
    }


    public List<WaterConsumptionRecord> getWaterHistory() {
        String historyJson = sharedPreferences.getString(KEY_HISTORY, "");

        if (historyJson.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<WaterConsumptionRecord>>() {
        }.getType();
        List<WaterConsumptionRecord> history = gson.fromJson(historyJson, type);

        return history != null ? history : new ArrayList<>();
    }


    private void saveWaterHistory(List<WaterConsumptionRecord> history) {
        String historyJson = gson.toJson(history);
        sharedPreferences.edit().putString(KEY_HISTORY, historyJson).apply();
    }


    public float getTotalWaterConsumption() {
        List<WaterConsumptionRecord> history = getWaterHistory();
        float total = 0f;

        for (WaterConsumptionRecord record : history) {
            total += record.getAmount();
        }

        return total;
    }


    public List<Float> getWeeklyWaterData() {

        String weeklyJson = sharedPreferences.getString(KEY_WEEKLY_HISTORY, "");
        List<Float> weeklyData = new ArrayList<>();

        if (!weeklyJson.isEmpty()) {
            Type type = new TypeToken<List<Float>>() {
            }.getType();
            weeklyData = gson.fromJson(weeklyJson, type);
        }


        if (weeklyData == null || weeklyData.size() != 7) {
            weeklyData = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                weeklyData.add(0f);
            }
        }


        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);


        int dayIndex = dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - 2;


        weeklyData.set(dayIndex, getTotalWaterConsumption());


        saveWeeklyData(weeklyData);

        return weeklyData;
    }


    private void updateWeeklyData(float amount) {
        List<Float> weeklyData = getWeeklyWaterData();


        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);


        int dayIndex = dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - 2;


        weeklyData.set(dayIndex, weeklyData.get(dayIndex) + amount);


        saveWeeklyData(weeklyData);
    }


    private void saveWeeklyData(List<Float> weeklyData) {
        String weeklyJson = gson.toJson(weeklyData);
        sharedPreferences.edit().putString(KEY_WEEKLY_HISTORY, weeklyJson).apply();
    }


    public List<Float> getMonthlyWaterData() {

        String monthlyJson = sharedPreferences.getString(KEY_MONTHLY_HISTORY, "");
        List<Float> monthlyData = new ArrayList<>();

        if (!monthlyJson.isEmpty()) {
            Type type = new TypeToken<List<Float>>() {
            }.getType();
            monthlyData = gson.fromJson(monthlyJson, type);
        }


        if (monthlyData == null || monthlyData.size() != 4) {
            monthlyData = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                monthlyData.add(0f);
            }
        }


        Calendar calendar = Calendar.getInstance();
        int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH) - 1;


        if (weekOfMonth >= 0 && weekOfMonth < 4) {

            float weeklySum = 0;
            List<Float> weeklyData = getWeeklyWaterData();
            for (float dayAmount : weeklyData) {
                weeklySum += dayAmount;
            }


            monthlyData.set(weekOfMonth, weeklySum);


            saveMonthlyData(monthlyData);
        }

        return monthlyData;
    }


    public List<Float> getDailyWaterDataForMonth() {
        Calendar calendar = Calendar.getInstance();
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);


        List<Float> dailyData = new ArrayList<>();
        for (int i = 0; i < daysInMonth; i++) {
            dailyData.add(0f);
        }


        if (currentDayOfMonth == 1 && getTotalWaterConsumption() == 0) {
            return dailyData;
        }


        String key = "daily_water_" + currentYear + "_" + currentMonth;
        String dailyJson = sharedPreferences.getString(key, "");

        if (!dailyJson.isEmpty()) {
            Type type = new TypeToken<List<Float>>() {
            }.getType();
            List<Float> savedData = gson.fromJson(dailyJson, type);


            if (savedData != null && savedData.size() == daysInMonth) {
                dailyData = savedData;
            }
        }


        dailyData.set(currentDayOfMonth - 1, getTotalWaterConsumption());


        saveDailyWaterData(dailyData);

        return dailyData;
    }


    private void saveDailyWaterData(List<Float> dailyData) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);


        String key = "daily_water_" + currentYear + "_" + currentMonth;

        String dailyJson = gson.toJson(dailyData);
        sharedPreferences.edit().putString(key, dailyJson).apply();
    }


    public void updateDailyWaterData(float amount) {
        Calendar calendar = Calendar.getInstance();
        int currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);


        List<Float> dailyData = getDailyWaterDataForMonth();


        float currentValue = dailyData.get(currentDayOfMonth - 1);
        dailyData.set(currentDayOfMonth - 1, currentValue + amount);


        saveDailyWaterData(dailyData);
    }


    private void updateMonthlyData(float amount) {
        List<Float> monthlyData = getMonthlyWaterData();


        Calendar calendar = Calendar.getInstance();
        int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH) - 1;


        if (weekOfMonth >= 0 && weekOfMonth < 4) {

            monthlyData.set(weekOfMonth, monthlyData.get(weekOfMonth) + amount);


            saveMonthlyData(monthlyData);
        }
    }


    private void saveMonthlyData(List<Float> monthlyData) {
        String monthlyJson = gson.toJson(monthlyData);
        sharedPreferences.edit().putString(KEY_MONTHLY_HISTORY, monthlyJson).apply();
    }


    public void resetDailyData() {

        saveWaterHistory(new ArrayList<>());


        Calendar calendar = Calendar.getInstance();
        int currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        List<Float> dailyData = getDailyWaterDataForMonth();
        dailyData.set(currentDayOfMonth - 1, 0f);
        saveDailyWaterData(dailyData);
    }


    public void resetMonthlyData() {

        List<Float> emptyWeekly = new ArrayList<>();
        List<Float> emptyMonthly = new ArrayList<>();


        for (int i = 0; i < 7; i++) {
            emptyWeekly.add(0f);
        }


        for (int i = 0; i < 4; i++) {
            emptyMonthly.add(0f);
        }


        saveWeeklyData(emptyWeekly);
        saveMonthlyData(emptyMonthly);

        Log.d(TAG, "Данные месячной статистики сброшены при смене месяца");
    }


    public void checkAndResetMonthlyDataIfNeeded() {

        Calendar lastSaved = Calendar.getInstance();
        lastSaved.setTimeInMillis(sharedPreferences.getLong(KEY_HISTORY_DATE, 0));


        Calendar current = Calendar.getInstance();


        if (lastSaved.get(Calendar.MONTH) != current.get(Calendar.MONTH) ||
                lastSaved.get(Calendar.YEAR) != current.get(Calendar.YEAR)) {
            resetMonthlyData();
        }
    }


    public void checkAndResetDailyDataIfNeeded() {

        long lastResetDate = sharedPreferences.getLong(KEY_HISTORY_DATE, 0);


        if (lastResetDate == 0) {
            saveCurrentDateAsResetDate();
            return;
        }


        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);


        Calendar lastReset = Calendar.getInstance();
        lastReset.setTimeInMillis(lastResetDate);
        lastReset.set(Calendar.HOUR_OF_DAY, 0);
        lastReset.set(Calendar.MINUTE, 0);
        lastReset.set(Calendar.SECOND, 0);
        lastReset.set(Calendar.MILLISECOND, 0);


        if (today.getTimeInMillis() > lastReset.getTimeInMillis()) {

            resetDailyData();

            saveCurrentDateAsResetDate();


            checkAndResetMonthlyDataIfNeeded();

            Log.d(TAG, "Данные истории воды сброшены из-за наступления нового дня");
        }
    }


    private void saveCurrentDateAsResetDate() {
        long currentTime = System.currentTimeMillis();
        sharedPreferences.edit().putLong(KEY_HISTORY_DATE, currentTime).apply();
    }
} 