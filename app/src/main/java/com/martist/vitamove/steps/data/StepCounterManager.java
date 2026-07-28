package com.martist.vitamove.steps.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Calendar;


public class StepCounterManager implements SensorEventListener {
    private static final String TAG = "StepCounterManager";
    private static StepCounterManager instance;

    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor stepSensor;
    private final SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "step_counter_prefs";
    private static final String KEY_INITIAL_STEPS = "initial_steps";
    private static final String KEY_PREVIOUS_STEPS = "previous_steps";
    private static final String KEY_CURRENT_STEPS = "current_steps";
    private static final String KEY_LAST_SAVE_DATE = "last_save_date";
    private static final String KEY_STEPS_BEFORE_REBOOT = "steps_before_reboot";

    private int initialSteps = -1;
    private int currentSteps = 0;
    private int stepsToday = 0;
    private int stepsBeforeReboot = 0;
    private boolean isListening = false;

    private StepCounterManager(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


        loadSavedData();
    }

    public static synchronized StepCounterManager getInstance(Context context) {
        if (instance == null) {
            instance = new StepCounterManager(context);
        }
        return instance;
    }


    public boolean startTracking() {
        if (stepSensor == null) {
            Log.e(TAG, "Сенсор шагомера не доступен на этом устройстве");
            return false;
        }

        if (!isListening) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isListening = true;
            Log.d(TAG, "Отслеживание шагов начато");
        }

        return true;
    }


    public void stopTracking() {
        if (isListening) {
            sensorManager.unregisterListener(this);
            isListening = false;


            saveCurrentSteps();

            Log.d(TAG, "Отслеживание шагов остановлено");
        }
    }


    public int getStepsToday() {

        final int MAX_REASONABLE_STEPS = 100000;


        int validSteps = Math.max(0, stepsToday);
        validSteps = Math.min(validSteps, MAX_REASONABLE_STEPS);


        if (stepsToday > MAX_REASONABLE_STEPS) {


            stepsToday = validSteps;
            saveCurrentSteps();
        }

        return validSteps;
    }


    private void loadSavedData() {


        initialSteps = sharedPreferences.getInt(KEY_INITIAL_STEPS, -1);
        currentSteps = sharedPreferences.getInt(KEY_CURRENT_STEPS, 0);
        stepsToday = sharedPreferences.getInt(KEY_PREVIOUS_STEPS, 0);
        stepsBeforeReboot = sharedPreferences.getInt(KEY_STEPS_BEFORE_REBOOT, 0);


        long lastSaveDate = sharedPreferences.getLong(KEY_LAST_SAVE_DATE, 0);
        long currentTime = System.currentTimeMillis();

        Log.d(TAG, "Загружены сохраненные данные: initialSteps=" + initialSteps +
                ", currentSteps=" + currentSteps + ", stepsToday=" + stepsToday +
                ", stepsBeforeReboot=" + stepsBeforeReboot);


        boolean isNewDay = !isSameDay(lastSaveDate, currentTime);

        if (isNewDay) {


            final int MAX_REASONABLE_STEPS = 100000;


            if (stepsToday > MAX_REASONABLE_STEPS) {
                Log.e(TAG, "Обнаружено аномально большое количество шагов при загрузке: " + stepsToday +
                        ". Сбрасываем счетчик.");
                stepsToday = 0;
            }


            if (stepsToday > 0) {

                try {

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(lastSaveDate);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    String previousDay = sdf.format(calendar.getTime());

                    Log.d(TAG, "Сохраняем шаги за предыдущий день (" + previousDay + "): " + stepsToday);


                    new Thread(() -> {
                        try {

                            StepHistoryRepository repository =
                                    StepHistoryRepository.Companion.getInstance(context);


                            repository.saveStepsForDate(previousDay, stepsToday);

                            Log.d(TAG, "Шаги за предыдущий день успешно сохранены: " + stepsToday);
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при сохранении шагов за предыдущий день: " + e.getMessage(), e);
                        }
                    }).start();
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при попытке сохранить шаги за предыдущий день: " + e.getMessage(), e);
                }
            }


            initialSteps = sharedPreferences.getInt(KEY_CURRENT_STEPS, -1);
            stepsToday = 0;
            stepsBeforeReboot = 0;

            Log.d(TAG, "Новый день. initialSteps установлен на " + initialSteps + " (последнее значение сенсора).");


            saveData();
        }
    }


    private void saveCurrentSteps() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_PREVIOUS_STEPS, stepsToday);
        editor.putInt(KEY_CURRENT_STEPS, currentSteps);
        editor.putLong(KEY_LAST_SAVE_DATE, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "Данные о шагах сохранены: stepsToday=" + stepsToday + ", currentSteps=" + currentSteps);
    }


    private void saveData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_INITIAL_STEPS, initialSteps);
        editor.putInt(KEY_PREVIOUS_STEPS, stepsToday);
        editor.putInt(KEY_CURRENT_STEPS, currentSteps);
        editor.putInt(KEY_STEPS_BEFORE_REBOOT, stepsBeforeReboot);
        editor.putLong(KEY_LAST_SAVE_DATE, System.currentTimeMillis());
        editor.apply();

    }


    private boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            int sensorSteps = (int) event.values[0];


            long lastSaveDate = sharedPreferences.getLong(KEY_LAST_SAVE_DATE, 0);
            if (!isSameDay(lastSaveDate, System.currentTimeMillis())) {

                Log.d(TAG, "День изменился с момента последнего сохранения. Перезагружаем данные.");
                loadSavedData();
            }

            if (initialSteps == -1) {

                initialSteps = sensorSteps;
                stepsBeforeReboot = 0;
                saveData();
                Log.d(TAG, "Установлено начальное значение шагов: " + initialSteps);
            }


            if (sensorSteps < initialSteps) {
                Log.d(TAG, "Обнаружена перезагрузка. Учитываем шаги до перезагрузки: " + stepsToday);


                if (stepsToday > 0) {

                    Log.d(TAG, "Перезагрузка в середине дня. Накоплено: " + stepsToday);
                    stepsBeforeReboot = stepsToday;
                    initialSteps = sensorSteps;
                } else {


                    Log.d(TAG, "Перезагрузка обнаружена при первом запуске. Шаги с сенсора: " + sensorSteps);
                    stepsBeforeReboot = sensorSteps;
                    initialSteps = sensorSteps;
                }


                saveData();
            }


            currentSteps = sensorSteps;
            int calculatedSteps = (currentSteps - initialSteps) + stepsBeforeReboot;


            final int MAX_REASONABLE_STEPS = 100000;


            if (calculatedSteps > MAX_REASONABLE_STEPS && stepsToday <= MAX_REASONABLE_STEPS) {
                Log.e(TAG, "АНОМАЛЬНЫЙ СКАЧОК ШАГОВ! Предыдущее значение: " + stepsToday +
                        ", Новое значение: " + calculatedSteps +
                        ", Показания сенсора: " + sensorSteps +
                        ", Начальное значение: " + initialSteps +
                        ", Шаги до перезагрузки: " + stepsBeforeReboot);


                initialSteps = sensorSteps;
                stepsBeforeReboot = 0;
                calculatedSteps = 0;
                saveData();
                Log.e(TAG, "Счетчик сброшен из-за аномального значения");
            }


            final int REASONABLE_STEP_DIFF = 1000;
            if (Math.abs(calculatedSteps - stepsToday) > REASONABLE_STEP_DIFF) {
                Log.w(TAG, "Обнаружено подозрительное изменение числа шагов: " + stepsToday +
                        " -> " + calculatedSteps + ". Разница: " + (calculatedSteps - stepsToday));
            }

            stepsToday = calculatedSteps;


            if (stepsToday % 10 == 0) {
                Log.d(TAG, "Обновлены шаги: текущее значение сенсора=" + sensorSteps +
                        ", initialSteps=" + initialSteps + ", stepsToday=" + stepsToday);
            }


            if (stepsToday % 100 == 0 || System.currentTimeMillis() - lastSaveDate > 5 * 60 * 1000) {
                saveCurrentSteps();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public void checkAndResetForNewDayIfNeeded() {
        long lastSaveDate = sharedPreferences.getLong(KEY_LAST_SAVE_DATE, 0);
        long currentTime = System.currentTimeMillis();

        if (!isSameDay(lastSaveDate, currentTime)) {
            Log.d(TAG, "Обнаружен новый день при явной проверке. Перезагружаем данные.");
            loadSavedData();
        }
    }
} 