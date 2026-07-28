package com.martist.vitamove.nutrition.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.nutrition.data.local.dao.FoodCacheDao;
import com.martist.vitamove.nutrition.data.local.entities.FoodCacheEntity;
import com.martist.vitamove.nutrition.data.repository.SupabaseFoodRepository;
import com.martist.vitamove.nutrition.ui.model.Food;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FoodSyncService {
    private static final String TAG = "FoodSyncService";
    private static final String PREFS_NAME = "FoodSyncPrefs";
    private static final String KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp";
    private static final String KEY_IS_FIRST_SYNC_COMPLETED = "is_first_sync_completed";

    private final Context context;
    private final SupabaseFoodRepository foodRepository;
    private final FoodCacheDao foodCacheDao;
    private final SharedPreferences prefs;
    private final ExecutorService executor;


    public interface SyncCallback {
        void onSyncStarted();

        void onSyncProgress(int current, int total);

        void onSyncCompleted(int syncedCount);

        void onSyncError(String error);
    }

    public FoodSyncService(Context context, SupabaseFoodRepository foodRepository) {
        this.context = context.getApplicationContext();
        this.foodRepository = foodRepository;
        this.foodCacheDao = AppDatabase.getInstance(context).foodCacheDao();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
    }


    public void syncFoods(boolean forceFullSync, SyncCallback callback) {
        executor.execute(() -> {

            try {
                if (callback != null) {
                    callback.onSyncStarted();
                }

                logSeparator();


                validateAndCleanInvalidData();

                int syncedCount;
                if (forceFullSync || isFirstSync()) {

                    syncedCount = performFullSync(callback);
                } else {

                    syncedCount = performIncrementalSync(callback);
                }


                logSeparator();

                if (callback != null) {
                    callback.onSyncCompleted(syncedCount);
                }

            } catch (Exception e) {
                logSeparator();

                if (callback != null) {
                    callback.onSyncError(e.getMessage());
                }
            }
        });
    }


    private void validateAndCleanInvalidData() {
        String lastSyncTime = prefs.getString(KEY_LAST_SYNC_TIMESTAMP, null);

        if (lastSyncTime != null) {

            if (lastSyncTime.matches(".*\\d{6}\\s\\d{2}:\\d{2}")) {

                prefs.edit()
                        .remove(KEY_LAST_SYNC_TIMESTAMP)
                        .remove(KEY_IS_FIRST_SYNC_COMPLETED)
                        .apply();

            }
        }
    }


    private boolean isFirstSync() {
        boolean isCompleted = prefs.getBoolean(KEY_IS_FIRST_SYNC_COMPLETED, false);
        int cachedCount = foodCacheDao.getFoodCount();
        return !isCompleted || cachedCount == 0;
    }


    private int performFullSync(SyncCallback callback) {
        long startTime = System.currentTimeMillis();

        try {
            List<Food> allFoods = foodRepository.getAllFoodsFromSupabase();
            if (allFoods == null || allFoods.isEmpty()) {
                return 0;
            }


            List<FoodCacheEntity> entities = new ArrayList<>();
            String maxTimestamp = null;

            for (int i = 0; i < allFoods.size(); i++) {
                Food food = allFoods.get(i);
                FoodCacheEntity entity = FoodCacheEntity.fromFood(food);
                entities.add(entity);


                if (food.getUpdatedAt() != null &&
                        (maxTimestamp == null || food.getUpdatedAt().compareTo(maxTimestamp) > 0)) {
                    maxTimestamp = food.getUpdatedAt();
                }


                if (entities.size() >= 500) {
                    foodCacheDao.insertAll(entities);
                    entities.clear();
                    if (callback != null) {
                        callback.onSyncProgress(i + 1, allFoods.size());
                    }
                }
            }


            if (!entities.isEmpty()) {

                foodCacheDao.insertAll(entities);
            }


            if (maxTimestamp != null) {
                saveLastSyncTime(maxTimestamp);

            }


            prefs.edit().putBoolean(KEY_IS_FIRST_SYNC_COMPLETED, true).apply();
            return allFoods.size();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "❌ Ошибка при полной синхронизации после " + formatDuration(duration) + ": " + e.getMessage(), e);
            throw e;
        }
    }


    private int performIncrementalSync(SyncCallback callback) {
        long startTime = System.currentTimeMillis();


        try {

            String lastSyncTime = getLastSyncTime();

            if (lastSyncTime == null) {

                return performFullSync(callback);
            }


            List<Food> updatedFoods = foodRepository.getFoodsSinceTimestamp(lastSyncTime);

            if (updatedFoods == null || updatedFoods.isEmpty()) {

                return 0;
            }


            if (updatedFoods.size() <= 10) {

                for (int i = 0; i < updatedFoods.size(); i++) {
                    Food food = updatedFoods.get(i);
                    Log.d(TAG, "      " + (i + 1) + ". " + food.getName() + " [" + food.getCategory() + "] - " + food.getUpdatedAt());
                }
            }

            List<FoodCacheEntity> entities = new ArrayList<>();
            String maxTimestamp = lastSyncTime;

            for (int i = 0; i < updatedFoods.size(); i++) {
                Food food = updatedFoods.get(i);
                FoodCacheEntity entity = FoodCacheEntity.fromFood(food);
                entities.add(entity);


                if (food.getUpdatedAt() != null && food.getUpdatedAt().compareTo(maxTimestamp) > 0) {
                    maxTimestamp = food.getUpdatedAt();
                }


                if (entities.size() >= 100) {

                    foodCacheDao.insertAll(entities);

                    entities.clear();

                    if (callback != null) {
                        callback.onSyncProgress(i + 1, updatedFoods.size());
                    }
                }
            }


            if (!entities.isEmpty()) {

                foodCacheDao.insertAll(entities);

            }


            if (!maxTimestamp.equals(lastSyncTime)) {
                saveLastSyncTime(maxTimestamp);

            }


            return updatedFoods.size();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, "❌ Ошибка при инкрементальной синхронизации после " + formatDuration(duration) + ": " + e.getMessage(), e);
            throw e;
        }
    }


    private void saveLastSyncTime(String timestamp) {

        String normalizedTimestamp = normalizeTimestamp(timestamp);
        prefs.edit().putString(KEY_LAST_SYNC_TIMESTAMP, normalizedTimestamp).apply();
        Log.d(TAG, "Сохранено время последней синхронизации: " + normalizedTimestamp);
    }


    private String normalizeTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return timestamp;
        }

        String original = timestamp;


        if (timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}.*")) {
            timestamp = timestamp.replaceFirst(" ", "T");
            Log.d(TAG, "Заменен пробел между датой и временем на T: " + timestamp);
        }


        if (timestamp.matches(".*\\d{6}\\s\\d{2}:\\d{2}")) {

            timestamp = timestamp.replaceAll("(\\d{6})\\s(\\d{2}:\\d{2})", "$1+$2");
            Log.d(TAG, "Заменен пробел перед часовым поясом на +: " + timestamp);
        }


        if (timestamp.matches(".*\\+\\d{2}$")) {
            timestamp = timestamp + ":00";
            Log.d(TAG, "Добавлено :00 к timezone: " + timestamp);
        }

        if (!original.equals(timestamp)) {
            Log.d(TAG, "Timestamp нормализован: '" + original + "' -> '" + timestamp + "'");
        }

        return timestamp;
    }


    private String getLastSyncTime() {
        String timestamp = prefs.getString(KEY_LAST_SYNC_TIMESTAMP, null);


        if (timestamp == null) {
            timestamp = foodCacheDao.getLastSyncTimestamp();
            if (timestamp != null) {

                saveLastSyncTime(timestamp);

                timestamp = normalizeTimestamp(timestamp);
            }
        } else {


            timestamp = normalizeTimestamp(timestamp);
        }

        return timestamp;
    }

    public boolean shouldSync() {
        return true;
    }


    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + " мс";
        } else if (durationMs < 60000) {
            return String.format("%.2f сек", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return minutes + " мин " + seconds + " сек";
        }
    }


    private void logSeparator() {
        Log.i(TAG, "");
    }


}

