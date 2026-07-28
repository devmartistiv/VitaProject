package com.martist.vitamove.core.data.managers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class NotificationWorker extends Worker {
    private static final String TAG = "NotificationWorker";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Запуск проверки уведомлений");


            NotificationManager notificationManager = NotificationManager.getInstance(getApplicationContext());


            notificationManager.checkAndSendNotifications();

            Log.d(TAG, "Проверка уведомлений завершена успешно");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке уведомлений: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
