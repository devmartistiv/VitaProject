package com.martist.vitamove.core.data.managers;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.martist.vitamove.R;
import com.martist.vitamove.core.data.local.MealsDatabase;
import com.martist.vitamove.core.data.local.entities.DayMeal;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.user.UserRepository;
import com.martist.vitamove.water.data.WaterHistoryManager;
import com.martist.vitamove.workout.data.model.WorkoutPlan;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class NotificationManager {
    private static final String TAG = "NotificationManager";


    public static final String CHANNEL_MEALS = "meals_notifications";
    public static final String CHANNEL_WATER = "water_notifications";
    public static final String CHANNEL_WORKOUT = "workout_notifications";


    public static final String NOTIFICATION_TYPE_DINNER = "dinner_reminder";
    public static final String NOTIFICATION_TYPE_WATER = "water_reminder";
    public static final String NOTIFICATION_TYPE_WORKOUT = "workout_reminder";


    private static final int NOTIFICATION_ID_DINNER = 1001;
    private static final int NOTIFICATION_ID_WATER = 1002;
    private static final int NOTIFICATION_ID_WORKOUT = 1003;


    private static NotificationManager instance;
    private final Context context;
    private final NotificationManagerCompat notificationManagerCompat;
    private final SharedPreferences sharedPreferences;
    private final WaterHistoryManager waterHistoryManager;
    private final UserRepository userRepository;


    private static final String PREF_FILE = "notification_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_DINNER_NOTIFICATIONS = "dinner_notifications";
    private static final String KEY_WATER_NOTIFICATIONS = "water_notifications";
    private static final String KEY_WORKOUT_NOTIFICATIONS = "workout_notifications";


    public static synchronized NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context.getApplicationContext());
        }
        return instance;
    }


    public static synchronized void resetInstance() {
        if (instance != null) {
            Log.d(TAG, "Сброс экземпляра NotificationManager");
            instance.cancelAllNotifications();
            instance = null;
        }
    }


    private NotificationManager(Context context) {
        this.context = context;
        this.notificationManagerCompat = NotificationManagerCompat.from(context);
        this.sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        this.waterHistoryManager = WaterHistoryManager.getInstance(context);
        this.userRepository = new UserRepository(context);


        createNotificationChannels();

        Log.d(TAG, "NotificationManager инициализирован");
    }


    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel mealsChannel = new NotificationChannel(
                    CHANNEL_MEALS,
                    "Напоминания о приемах пищи",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            mealsChannel.setDescription("Уведомления о необходимости записать прием пищи");


            NotificationChannel waterChannel = new NotificationChannel(
                    CHANNEL_WATER,
                    "Напоминания о воде",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            waterChannel.setDescription("Уведомления о необходимости выпить воду");


            NotificationChannel workoutChannel = new NotificationChannel(
                    CHANNEL_WORKOUT,
                    "Напоминания о тренировках",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            workoutChannel.setDescription("Уведомления о предстоящих тренировках");


            android.app.NotificationManager systemNotificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            systemNotificationManager.createNotificationChannel(mealsChannel);
            systemNotificationManager.createNotificationChannel(waterChannel);
            systemNotificationManager.createNotificationChannel(workoutChannel);

            Log.d(TAG, "Каналы уведомлений созданы");
        }
    }


    public void initializeNotifications() {
        if (!areNotificationsEnabled()) {
            Log.d(TAG, "Уведомления отключены в настройках");
            return;
        }


        scheduleNotifications();

        Log.d(TAG, "Уведомления инициализированы и запланированы");
    }


    public void scheduleNotifications() {

        WorkManager workManager = WorkManager.getInstance(context);


        PeriodicWorkRequest notificationWork = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                15, TimeUnit.MINUTES
        ).build();

        workManager.enqueueUniquePeriodicWork(
                "VitaMoveNotifications",
                ExistingPeriodicWorkPolicy.REPLACE,
                notificationWork
        );

        Log.d(TAG, "Запланированы повторяющиеся проверки уведомлений");
    }


    public void checkAndSendNotifications() {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);


        SharedPreferences defaultPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);


        int workoutHour = defaultPrefs.getInt("workout_time_hour", 10);
        int workoutMinute = defaultPrefs.getInt("workout_time_minute", 0);
        if (isTimeInRange(currentHour, currentMinute, workoutHour, workoutMinute)) {
            checkAndSendWorkoutNotification();
        }


        int waterHour = defaultPrefs.getInt("water_time_hour", 15);
        int waterMinute = defaultPrefs.getInt("water_time_minute", 0);
        if (isTimeInRange(currentHour, currentMinute, waterHour, waterMinute)) {
            sendWaterReminderNotification();
        }


        int dinnerHour = defaultPrefs.getInt("dinner_time_hour", 19);
        int dinnerMinute = defaultPrefs.getInt("dinner_time_minute", 30);
        if (isTimeInRange(currentHour, currentMinute, dinnerHour, dinnerMinute)) {
            checkAndSendDinnerNotification();
        }
    }


    private boolean isTimeInRange(int currentHour, int currentMinute, int targetHour, int targetMinute) {
        int currentTotalMinutes = currentHour * 60 + currentMinute;
        int targetTotalMinutes = targetHour * 60 + targetMinute;


        return Math.abs(currentTotalMinutes - targetTotalMinutes) <= 15;
    }


    private void checkAndSendDinnerNotification() {
        if (!isDinnerNotificationsEnabled()) {
            return;
        }


        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        try {
            SharedPreferences appPrefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            String userId = appPrefs.getString("userId", null);

            if (userId != null) {
                MealsDatabase mealsDatabase = MealsDatabase.getInstance(context);
                DayMeal dinnerMeal = mealsDatabase.mealDao().getMealByDateAndType(today, "dinner", userId);

                if (dinnerMeal == null) {

                    sendDinnerReminderNotification();
                    Log.d(TAG, "Отправлено уведомление о записи ужина");
                } else {
                    Log.d(TAG, "Ужин уже записан, уведомление не требуется");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке записи ужина: " + e.getMessage(), e);
        }
    }


    private void checkAndSendWorkoutNotification() {
        if (!isWorkoutNotificationsEnabled()) {
            return;
        }

        try {
            SharedPreferences appPrefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            String userId = appPrefs.getString("userId", null);

            if (userId != null) {

                SupabaseWorkoutRepository workoutRepository = new SupabaseWorkoutRepository(
                        SupabaseClient.getInstance(SupabaseClient.SUPABASE_URL, Constants.SUPABASE_CLIENT_SECRET)
                );


                new Thread(() -> {
                    try {

                        WorkoutPlan todayWorkout = workoutRepository.getTodayWorkoutPlan(userId);

                        if (todayWorkout != null && !todayWorkout.isCompleted()) {
                            sendWorkoutReminderNotification(todayWorkout);
                            Log.d(TAG, "Отправлено уведомление о тренировке: " + todayWorkout.getName());
                        } else if (todayWorkout == null) {
                            Log.d(TAG, "На сегодня тренировки не запланированы");
                        } else {
                            Log.d(TAG, "Тренировка на сегодня уже завершена");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при проверке тренировок: " + e.getMessage(), e);
                    }
                }).start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке тренировок: " + e.getMessage(), e);
        }
    }


    private void sendDinnerReminderNotification() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_fragment", "calories");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MEALS)
                .setSmallIcon(R.drawable.ic_notification_new)

                .setContentTitle("Время записать ужин!")
                .setContentText("Не забудьте записать, что вы ели на ужин сегодня")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (notificationManagerCompat.areNotificationsEnabled()) {
            notificationManagerCompat.notify(NOTIFICATION_ID_DINNER, builder.build());
        }
    }


    private void sendWaterReminderNotification() {
        if (!isWaterNotificationsEnabled()) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_fragment", "water");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_WATER)
                .setSmallIcon(R.drawable.ic_notification_new)

                .setContentTitle("Время выпить воды!")
                .setContentText("Не забудьте записать потребление воды")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (notificationManagerCompat.areNotificationsEnabled()) {
            notificationManagerCompat.notify(NOTIFICATION_ID_WATER, builder.build());
        }
    }


    private void sendWorkoutReminderNotification(WorkoutPlan workout) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_fragment", "programs");
        intent.putExtra("workout_tab_index", 2);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String workoutName = workout.getName() != null ? workout.getName() : "тренировка";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_WORKOUT)
                .setSmallIcon(R.drawable.ic_notification_new)

                .setContentTitle("Время тренировки!")
                .setContentText("У Вас запланирована " + workoutName + " на сегодня")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (notificationManagerCompat.areNotificationsEnabled()) {
            notificationManagerCompat.notify(NOTIFICATION_ID_WORKOUT, builder.build());
        }
    }


    private void cancelAllNotifications() {
        notificationManagerCompat.cancel(NOTIFICATION_ID_DINNER);
        notificationManagerCompat.cancel(NOTIFICATION_ID_WATER);
        notificationManagerCompat.cancel(NOTIFICATION_ID_WORKOUT);


        WorkManager.getInstance(context).cancelUniqueWork("VitaMoveNotifications");
    }


    public boolean areNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }


    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();

        if (enabled) {
            initializeNotifications();
        } else {
            cancelAllNotifications();
        }
    }


    public boolean isDinnerNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_DINNER_NOTIFICATIONS, true);
    }


    public boolean isWaterNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_WATER_NOTIFICATIONS, true);
    }


    public boolean isWorkoutNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_WORKOUT_NOTIFICATIONS, true);
    }


    public void setDinnerNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DINNER_NOTIFICATIONS, enabled).apply();
    }


    public void setWaterNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_WATER_NOTIFICATIONS, enabled).apply();
    }


    public void setWorkoutNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_WORKOUT_NOTIFICATIONS, enabled).apply();
    }
}
