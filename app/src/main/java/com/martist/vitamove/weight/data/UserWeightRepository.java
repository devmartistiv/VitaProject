package com.martist.vitamove.weight.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.weight.data.local.dao.UserWeightDao;
import com.martist.vitamove.weight.data.local.entities.UserWeightEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class UserWeightRepository {

    private static final String TAG = "UserWeightRepository";
    private final UserWeightDao userWeightDao;
    private final SupabaseClient supabaseClient;
    private final Executor executor;
    private final String userId;
    private final SimpleDateFormat isoDateFormat;
    private final Context context;


    public UserWeightRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userWeightDao = db.userWeightDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.context = context.getApplicationContext();


        this.isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));


        this.supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );


        SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        this.userId = prefs.getString("userId", null);
    }


    public void addWeightRecord(float weight, Date date, String notes) {
        if (userId == null) {
            Log.e(TAG, "Невозможно добавить запись о весе: userId == null");
            return;
        }

        UserWeightEntity entity = new UserWeightEntity(userId, weight, date, notes);
        executor.execute(() -> {

            long insertId = userWeightDao.insert(entity);
            Log.d(TAG, "Запись о весе добавлена в локальную БД: " + insertId);


            syncWithSupabase(entity);
        });
    }


    public void updateWeightRecord(UserWeightEntity weightEntity) {
        executor.execute(() -> {
            userWeightDao.update(weightEntity);

            weightEntity.setSynced(false);
            userWeightDao.update(weightEntity);


            syncWithSupabase(weightEntity);
        });
    }


    public void deleteWeightRecord(UserWeightEntity weightEntity) {
        executor.execute(() -> {

            userWeightDao.delete(weightEntity);


            if (userId != null && supabaseClient != null) {
                try {
                    supabaseClient.from("user_weight_history")
                            .eq("id", weightEntity.getId())
                            .delete()
                            .executeDelete();
                    Log.d(TAG, "Запись о весе удалена из Supabase: " + weightEntity.getId());
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при удалении записи о весе из Supabase: " + e.getMessage(), e);
                }
            }
        });
    }


    public LiveData<List<UserWeightEntity>> getAllWeightRecords() {
        return userWeightDao.getAllWeightRecordsForUser(userId);
    }


    public LiveData<UserWeightEntity> getLatestWeightRecord() {
        return userWeightDao.getLatestWeightRecord(userId);
    }


    public LiveData<List<UserWeightEntity>> getWeightRecordsInRange(Date startDate, Date endDate) {
        return userWeightDao.getWeightRecordsInRange(userId,
                startDate.getTime(), endDate.getTime());
    }


    public LiveData<UserWeightEntity> getWeightRecordById(String id) {
        return userWeightDao.getWeightRecordById(id);
    }


    public void syncAllUnsyncedRecords() {
        if (userId == null || supabaseClient == null) {
            Log.e(TAG, "Невозможно синхронизировать записи: userId == null или supabaseClient == null");
            return;
        }

        executor.execute(() -> {
            List<UserWeightEntity> unsyncedRecords = userWeightDao.getUnsyncedRecords(userId);
            Log.d(TAG, "Найдено несинхронизированных записей: " + unsyncedRecords.size());

            for (UserWeightEntity record : unsyncedRecords) {
                syncWithSupabase(record);
            }
        });
    }


    private void syncWithSupabase(UserWeightEntity entity) {
        if (userId == null || supabaseClient == null) {
            Log.e(TAG, "Невозможно синхронизировать запись: userId == null или supabaseClient == null");
            return;
        }

        try {

            String formattedDate = isoDateFormat.format(entity.getDateAsDate());


            JSONObject data = new JSONObject();
            data.put("id", entity.getId());
            data.put("user_id", entity.getUserId());
            data.put("weight", entity.getWeight());
            data.put("date", formattedDate);
            data.put("notes", entity.getNotes() != null ? entity.getNotes() : "");


            try {
                JSONArray result = supabaseClient.from("user_weight_history")
                        .eq("id", entity.getId())
                        .executeAndGetArray();

                if (result != null && result.length() > 0) {

                    supabaseClient.from("user_weight_history")
                            .eq("id", entity.getId())
                            .update(data)
                            .executeUpdate();
                    Log.d(TAG, "Запись о весе обновлена в Supabase: " + entity.getId());
                } else {

                    supabaseClient.from("user_weight_history")
                            .insert(data)
                            .executeInsert();
                    Log.d(TAG, "Запись о весе добавлена в Supabase: " + entity.getId());
                }


                userWeightDao.updateSyncStatus(entity.getId(), true);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при синхронизации записи о весе с Supabase: " + e.getMessage(), e);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при создании JSON для Supabase: " + e.getMessage(), e);
        }
    }


    public void syncFromSupabase(final SyncCallback callback) {
        if (userId == null || supabaseClient == null) {
            Log.e(TAG, "Невозможно загрузить историю веса: userId == null или supabaseClient == null");
            if (callback != null) {
                callback.onSyncCompleted(false, "Невозможно загрузить историю веса: userId == null");
            }
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "Начинаем загрузку истории веса из Supabase для пользователя: " + userId);


                JSONArray result = supabaseClient.from("user_weight_history")
                        .eq("user_id", userId)
                        .order("date", false)
                        .executeAndGetArray();

                if (result != null) {
                    List<UserWeightEntity> weightEntities = new ArrayList<>();


                    for (int i = 0; i < result.length(); i++) {
                        try {
                            JSONObject record = result.getJSONObject(i);

                            String id = record.getString("id");
                            String userIdFromRecord = record.getString("user_id");
                            float weight = (float) record.getDouble("weight");
                            String dateStr = record.getString("date");
                            String notes = record.optString("notes", "");


                            Date date;
                            try {
                                date = isoDateFormat.parse(dateStr);
                            } catch (ParseException e) {

                                try {
                                    SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                                    altFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    date = altFormat.parse(dateStr);
                                } catch (ParseException e2) {
                                    Log.e(TAG, "Не удалось распарсить дату: " + dateStr, e2);

                                    date = new Date();
                                }
                            }


                            UserWeightEntity entity = new UserWeightEntity();
                            entity.setId(id);
                            entity.setUserId(userIdFromRecord);


                            if (weight <= 0 || weight > 500) {
                                Log.w(TAG, "Получено некорректное значение веса из Supabase: " + weight +
                                        " кг для записи " + id + ", пропускаем запись");
                                continue;
                            }

                            entity.setWeight(weight);
                            entity.setDateFromDate(date);
                            entity.setNotes(notes);
                            entity.setSynced(true);

                            weightEntities.add(entity);

                        } catch (JSONException e) {
                            Log.e(TAG, "Ошибка при обработке записи о весе из Supabase", e);
                        }
                    }


                    if (!weightEntities.isEmpty()) {
                        userWeightDao.insertAll(weightEntities);
                        Log.d(TAG, "Загружено записей о весе из Supabase: " + weightEntities.size());


                        if (!weightEntities.isEmpty()) {
                            updateCurrentWeightInPreferences(weightEntities.get(0).getWeight());
                        }


                        updateInitialWeightInPreferences();


                        if (callback != null) {
                            callback.onSyncCompleted(true, "Загружено " + weightEntities.size() + " записей");
                        }
                    } else {
                        Log.d(TAG, "Нет записей о весе в Supabase");
                        if (callback != null) {
                            callback.onSyncCompleted(true, "Нет записей о весе");
                        }
                    }
                } else {
                    Log.e(TAG, "Ошибка при загрузке истории веса: результат null");
                    if (callback != null) {
                        callback.onSyncCompleted(false, "Ошибка при загрузке истории веса");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке истории веса из Supabase", e);
                if (callback != null) {
                    callback.onSyncCompleted(false, "Ошибка: " + e.getMessage());
                }
            }
        });
    }


    private void updateCurrentWeightInPreferences(float weight) {
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("current_weight", weight);
        editor.apply();

        Log.d(TAG, "Текущий вес обновлен в SharedPreferences: " + weight + " кг");
    }


    private void updateInitialWeightInPreferences() {
        if (userId == null) return;

        executor.execute(() -> {
            try {

                List<UserWeightEntity> allRecords = userWeightDao.getAllWeightRecordsForUserDirect(userId);

                if (allRecords == null || allRecords.isEmpty()) {
                    Log.d(TAG, "Нет записей о весе для определения начального веса");
                    return;
                }


                UserWeightEntity earliestRecord = allRecords.get(0);
                for (UserWeightEntity record : allRecords) {
                    if (record.getDate() < earliestRecord.getDate()) {
                        earliestRecord = record;
                    }
                }


                float initialWeight = earliestRecord.getWeight();
                SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("initial_weight", initialWeight);
                editor.apply();

                Log.d(TAG, "Начальный вес обновлен в SharedPreferences: " + initialWeight +
                        " кг (дата: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",
                        java.util.Locale.getDefault()).format(earliestRecord.getDateAsDate()) + ")");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении начального веса: " + e.getMessage(), e);
            }
        });
    }


    public void addWeightRecordOnlyToSupabase(float weight, Date date, String notes) {

        String effectiveUserId = userId;
        if (effectiveUserId == null) {

            SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
            effectiveUserId = prefs.getString("userId", null);

            if (effectiveUserId == null) {

                SharedPreferences authPrefs = context.getSharedPreferences("auth_data", Context.MODE_PRIVATE);
                effectiveUserId = authPrefs.getString("user_id", null);

                if (effectiveUserId == null) {

                    String accessToken = prefs.getString("accessToken", null);
                    if (accessToken != null) {
                        try {
                            String[] jwtParts = accessToken.split("\\.");
                            if (jwtParts.length > 1) {
                                String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                                JSONObject jwtJson = new JSONObject(payload);
                                effectiveUserId = jwtJson.getString("sub");
                                Log.d(TAG, "Извлечен ID пользователя из токена: " + effectiveUserId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при извлечении userId из токена: " + e.getMessage(), e);
                        }
                    }
                }
            }
        }


        if (effectiveUserId == null || supabaseClient == null) {
            Log.e(TAG, "Невозможно добавить запись о весе в Supabase: userId == null или supabaseClient == null");
            return;
        }

        final String finalUserId = effectiveUserId;

        executor.execute(() -> {
            try {

                UserWeightEntity entity = new UserWeightEntity(finalUserId, weight, date, notes);


                String formattedDate = isoDateFormat.format(entity.getDateAsDate());


                JSONObject data = new JSONObject();
                data.put("id", entity.getId());
                data.put("user_id", entity.getUserId());
                data.put("weight", entity.getWeight());
                data.put("date", formattedDate);
                data.put("notes", entity.getNotes() != null ? entity.getNotes() : "");


                supabaseClient.from("user_weight_history")
                        .insert(data)
                        .executeInsert();

                Log.d(TAG, "Запись о весе добавлена только в Supabase: " + entity.getId() +
                        " для пользователя: " + finalUserId + " с весом: " + weight + " кг");


                updateCurrentWeightInPreferences(weight);


                SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
                if (prefs.getFloat("initial_weight", 0f) <= 0) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putFloat("initial_weight", weight);
                    editor.apply();
                    Log.d(TAG, "Начальный вес установлен в SharedPreferences: " + weight + " кг");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при добавлении записи в Supabase: " + e.getMessage(), e);
            }
        });
    }


    public interface SyncCallback {

        void onSyncCompleted(boolean success, String message);
    }
} 