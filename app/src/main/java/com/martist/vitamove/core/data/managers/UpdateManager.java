package com.martist.vitamove.core.data.managers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager;
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory;
import ru.rustore.sdk.appupdate.model.AppUpdateInfo;
import ru.rustore.sdk.appupdate.model.AppUpdateOptions;
import ru.rustore.sdk.appupdate.model.AppUpdateType;
import ru.rustore.sdk.appupdate.model.InstallStatus;
import ru.rustore.sdk.appupdate.model.UpdateAvailability;


public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static final String PREFS_NAME = "update_preferences";
    private static final String KEY_UPDATE_DISMISSED_VERSION = "update_dismissed_version";

    private static UpdateManager instance;
    private final Context context;
    private final RuStoreAppUpdateManager ruStoreAppUpdateManager;
    private final SharedPreferences updatePrefs;

    private Activity currentActivity;

    private UpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.ruStoreAppUpdateManager = RuStoreAppUpdateManagerFactory.INSTANCE.create(this.context);
        this.updatePrefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public static synchronized UpdateManager getInstance(Context context) {
        if (instance == null) {
            instance = new UpdateManager(context);
        }
        return instance;
    }


    public void setCurrentActivity(Activity activity) {
        this.currentActivity = activity;
    }


    public void checkForUpdates() {
        if (currentActivity == null || currentActivity.isFinishing()) {
            Log.w(TAG, "Активность недоступна для показа диалога обновления");
            return;
        }

        Log.d(TAG, "Начинаем проверку обновлений...");

        ruStoreAppUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    handleUpdateInfo(appUpdateInfo);
                })
                .addOnFailureListener(throwable -> {
                    Log.e(TAG, "Ошибка при проверке обновлений", throwable);

                });
    }


    private void handleUpdateInfo(AppUpdateInfo appUpdateInfo) {
        int availability = appUpdateInfo.getUpdateAvailability();

        Log.d(TAG, "Статус обновления: " + availability);

        if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
            long availableVersionCode = appUpdateInfo.getAvailableVersionCode();


            long dismissedVersion = updatePrefs.getLong(KEY_UPDATE_DISMISSED_VERSION, -1);

            if (dismissedVersion == availableVersionCode) {
                Log.d(TAG, "Пользователь уже отклонил обновление версии " + availableVersionCode);
                return;
            }

            Log.d(TAG, "Доступно обновление до версии " + availableVersionCode + ". Запускаем обновление через RuStore UI");
            startFlexibleUpdate(appUpdateInfo);

        } else if (availability == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
            Log.d(TAG, "Обновление недоступно - используется последняя версия");

        } else if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            Log.d(TAG, "Обновление уже в процессе");

            checkInstallStatus();
        }
    }


    private void startFlexibleUpdate(AppUpdateInfo appUpdateInfo) {
        if (currentActivity == null || currentActivity.isFinishing()) {
            return;
        }

        Log.d(TAG, "Запуск отложенного обновления через RuStore UI...");

        AppUpdateOptions updateOptions = new AppUpdateOptions.Builder()
                .appUpdateType(AppUpdateType.FLEXIBLE)
                .build();

        ruStoreAppUpdateManager
                .startUpdateFlow(appUpdateInfo, updateOptions)
                .addOnSuccessListener(result -> {
                    if (result == 1) {
                        ruStoreAppUpdateManager
                                .completeUpdate(new AppUpdateOptions.Builder().appUpdateType(AppUpdateType.FLEXIBLE).build())
                                .addOnFailureListener(throwable ->
                                        Log.d(TAG, "Throwable: " + throwable)
                                );
                    }
                    Log.d(TAG, "RuStore UI для обновления успешно запущен");
                })

                .addOnFailureListener(throwable -> {
                    Log.e(TAG, "Ошибка при запуске обновления", throwable);


                    if (throwable.getMessage() != null && throwable.getMessage().contains("ABORTED")) {
                        updatePrefs.edit()
                                .putLong(KEY_UPDATE_DISMISSED_VERSION, appUpdateInfo.getAvailableVersionCode())
                                .apply();
                        Log.d(TAG, "Пользователь отклонил обновление версии " + appUpdateInfo.getAvailableVersionCode());
                    }
                });
    }


    private void checkInstallStatus() {
        ruStoreAppUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.getInstallStatus() == InstallStatus.DOWNLOADED) {
                        Log.d(TAG, "Обнаружено скачанное обновление (RuStore покажет UI)");
                    }
                })
                .addOnFailureListener(throwable -> {
                    Log.e(TAG, "Ошибка при проверке статуса установки", throwable);
                });
    }


    public void cleanup() {
        currentActivity = null;
    }


    public void resetDismissedVersions() {
        updatePrefs.edit().remove(KEY_UPDATE_DISMISSED_VERSION).apply();
        Log.d(TAG, "Сброшена информация о пропущенных версиях");
    }
}
