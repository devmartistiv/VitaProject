package com.martist.vitamove.core.domain.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.domain.utils.NetworkUtils;
import com.martist.vitamove.workout.data.managers.WorkoutSyncManager;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;


public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";
    private static final String PREFS_NAME = "network_state_prefs";
    private static final String LAST_NETWORK_STATE_KEY = "last_network_state";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) {
            return;
        }

        boolean isConnected = NetworkUtils.isNetworkAvailable(context);
        Log.d(TAG, "onReceive: Изменение состояния сети. Подключено: " + isConnected);


        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean wasConnected = prefs.getBoolean(LAST_NETWORK_STATE_KEY, false);


        prefs.edit().putBoolean(LAST_NETWORK_STATE_KEY, isConnected).apply();


        if (isConnected && !wasConnected) {
            Log.d(TAG, "onReceive: Интернет появился! Запускаем синхронизацию...");
            startSync(context);
        } else if (!isConnected && wasConnected) {
            Log.d(TAG, "onReceive: Интернет пропал");
        }
    }


    private void startSync(Context context) {
        try {

            SharedPreferences authPrefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            String accessToken = authPrefs.getString("accessToken", null);
            String refreshToken = authPrefs.getString("refreshToken", null);


            if (accessToken == null || refreshToken == null) {
                Log.d(TAG, "startSync: Пользователь не авторизован, синхронизация невозможна");
                return;
            }


            SupabaseClient supabaseClient = SupabaseClient.getInstance(Constants.SUPABASE_CLIENT_ID, Constants.SUPABASE_CLIENT_SECRET);


            SupabaseWorkoutRepository repository = new SupabaseWorkoutRepository(supabaseClient);


            WorkoutSyncManager syncManager = WorkoutSyncManager.getInstance(context, repository);


            if (syncManager.hasUnsyncedData()) {
                Log.d(TAG, "startSync: Найдены несинхронизированные данные, запускаем синхронизацию");

                syncManager.syncAllUnsyncedWorkouts(new WorkoutSyncManager.SyncCallback() {
                    @Override
                    public void onSyncCompleted(int syncedCount) {
                        Log.d(TAG, "startSync: Синхронизация завершена успешно. Синхронизировано записей: " + syncedCount);
                    }

                    @Override
                    public void onSyncFailed(String errorMessage) {
                        Log.e(TAG, "startSync: Ошибка синхронизации: " + errorMessage);
                    }
                });
            } else {
                Log.d(TAG, "startSync: Нет несинхронизированных данных");
            }

        } catch (Exception e) {
            Log.e(TAG, "startSync: Ошибка при запуске синхронизации", e);
        }
    }
}
