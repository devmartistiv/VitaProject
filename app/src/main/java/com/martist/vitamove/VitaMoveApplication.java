package com.martist.vitamove;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.martist.vitamove.auth.AuthManager;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.managers.NotificationManager;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class VitaMoveApplication extends Application {
    private static final String TAG = "VitaMoveApplication";
    public static Context context;
    private WorkoutRepository workoutRepository;
    public static final String PREFS_NAME = "VitaMovePrefs";
    private static AuthManager authManager;
    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();


        context = getApplicationContext();

        database = AppDatabase.getInstance(this);


        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String accessToken = prefs.getString("accessToken", null);
        String refreshToken = prefs.getString("refreshToken", null);


        authManager = AuthManager.getInstance(supabaseClient);

        if (accessToken != null && refreshToken != null) {

            supabaseClient.setUserToken(accessToken);
            supabaseClient.setRefreshToken(refreshToken);
        }


        supabaseClient.initializeWithInterceptor(authManager);


        workoutRepository = new SupabaseWorkoutRepository(supabaseClient);


        initializeAppSettings();


        initializeNotifications();

        if (getCurrentUserId() != null) {
            initializeWorkoutCleanup();
        }
    }

    public static Context getContext() {
        return context;
    }

    public static Context getAppContext() {
        return context;
    }


    public static AppDatabase getDatabase() {
        return database;
    }

    public WorkoutRepository getWorkoutRepository() {
        return workoutRepository;
    }

    public String getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("userId", null);
    }

    private void initializeAppSettings() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        String darkModeValue = sharedPreferences.getString("dark_mode", "system");
        applyDarkMode(darkModeValue);
    }

    private void applyDarkMode(String darkModeValue) {
        int nightMode;
        switch (darkModeValue) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "system":
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private void initializeWorkoutCleanup() {
        new Thread(() -> {
            try {

                String userId = getCurrentUserId();
                if (userId != null && !userId.isEmpty()) {


                    workoutRepository.getTodayWorkoutPlan(userId);

                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при проверке невыполненных тренировок: " + e.getMessage(), e);
            }
        }).start();
    }

    private void initializeNotifications() {
        try {
            NotificationManager notificationManager = NotificationManager.getInstance(this);

            notificationManager.initializeNotifications();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации системы уведомлений: " + e.getMessage(), e);
        }
    }
} 