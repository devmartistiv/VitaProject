package com.martist.vitamove.measurement;

import android.content.Context;
import android.util.Log;

import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SupabaseBodyMeasurementRepository {
    private static final String TAG = "SupabaseBodyMeasurementRepo";
    private static final String TABLE_NAME = "body_measurements";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private SupabaseClient supabaseClient;
    private SimpleDateFormat isoDateFormat;
    private SimpleDateFormat[] dateFormats;
    private String userId;


    public interface MeasurementCallback {
        void onSuccess();

        void onError(String error);
    }


    public interface MeasurementListCallback {
        void onSuccess(List<MeasurementRecord> measurements);

        void onError(String error);
    }


    public interface SingleMeasurementCallback {
        void onSuccess(MeasurementRecord measurement);

        void onError(String error);
    }

    public SupabaseBodyMeasurementRepository(Context context) {

        this.supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );


        android.content.SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("accessToken", null);
        String refreshToken = prefs.getString("refreshToken", null);
        this.userId = prefs.getString("userId", null);

        if (accessToken != null && refreshToken != null) {
            supabaseClient.setUserToken(accessToken);
            supabaseClient.setRefreshToken(refreshToken);
            Log.d(TAG, "Токены загружены для работы с замерами тела");
        } else {
            Log.w(TAG, "Токены авторизации не найдены! Пользователь должен войти в систему");
        }

        if (userId != null) {
            Log.d(TAG, "ID пользователя загружен: " + userId);
        } else {
            Log.w(TAG, "ID пользователя не найден! Пользователь должен войти в систему");
        }


        this.isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US);
        this.isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));


        this.dateFormats = new SimpleDateFormat[]{
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        };


        for (SimpleDateFormat format : dateFormats) {
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
    }


    public void addMeasurement(String bodyPart, float value, String note, MeasurementCallback callback) {

        if (supabaseClient.getUserToken() == null || supabaseClient.getUserToken().isEmpty()) {
            Log.e(TAG, "Отсутствует токен авторизации! Пользователь должен войти в систему");
            callback.onError("Пользователь не авторизован. Войдите в систему");
            return;
        }


        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Отсутствует ID пользователя! Пользователь должен войти в систему");
            callback.onError("ID пользователя не найден. Войдите в систему");
            return;
        }

        try {
            JSONObject measurementData = new JSONObject();
            measurementData.put("user_id", userId);
            measurementData.put("body_part", bodyPart);
            measurementData.put("value", value);
            measurementData.put("measurement_date", isoDateFormat.format(new Date()));

            if (note != null && !note.trim().isEmpty()) {
                measurementData.put("note", note.trim());
            }

            Log.d(TAG, "Добавляем замер: " + bodyPart + " = " + value + " для пользователя: " + userId);

            RequestBody body = RequestBody.create(measurementData.toString(), JSON);

            Request request = new Request.Builder()
                    .url(SupabaseClient.SUPABASE_URL + "/rest/v1/" + TABLE_NAME)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + supabaseClient.getUserToken())
                    .addHeader("apikey", supabaseClient.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();


            try {
                java.lang.reflect.Field clientField = SupabaseClient.class.getDeclaredField("client");
                clientField.setAccessible(true);
                okhttp3.OkHttpClient httpClient = (okhttp3.OkHttpClient) clientField.get(supabaseClient);

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Ошибка при добавлении замера: " + e.getMessage(), e);
                        callback.onError("Ошибка сети: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Замер успешно добавлен: " + bodyPart + " = " + value);
                            callback.onSuccess();
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "Неизвестная ошибка";
                            Log.e(TAG, "Ошибка при добавлении замера: " + response.code() + " - " + errorBody);
                            callback.onError("Ошибка сервера: " + response.code());
                        }
                        response.close();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка доступа к HTTP клиенту: " + e.getMessage(), e);
                callback.onError("Ошибка инициализации");
            }

        } catch (JSONException e) {
            Log.e(TAG, "Ошибка JSON при добавлении замера: " + e.getMessage(), e);
            callback.onError("Ошибка формирования данных");
        }
    }


    public void getMeasurementHistory(String bodyPart, int limit, MeasurementListCallback callback) {

        if (supabaseClient.getUserToken() == null || supabaseClient.getUserToken().isEmpty()) {
            Log.e(TAG, "Отсутствует токен авторизации для получения истории замеров");
            callback.onError("Пользователь не авторизован. Войдите в систему");
            return;
        }


        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Отсутствует ID пользователя для получения истории замеров");
            callback.onError("ID пользователя не найден. Войдите в систему");
            return;
        }
        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/" + TABLE_NAME +
                "?body_part=eq." + bodyPart +
                "&order=measurement_date.desc" +
                "&limit=" + limit;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + supabaseClient.getUserToken())
                .addHeader("apikey", supabaseClient.getApiKey())
                .build();

        try {
            java.lang.reflect.Field clientField = SupabaseClient.class.getDeclaredField("client");
            clientField.setAccessible(true);
            okhttp3.OkHttpClient httpClient = (okhttp3.OkHttpClient) clientField.get(supabaseClient);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Ошибка при получении истории замеров: " + e.getMessage(), e);
                    callback.onError("Ошибка сети: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            List<MeasurementRecord> measurements = parseMeasurements(responseBody);
                            Log.d(TAG, "Получено " + measurements.size() + " замеров для " + bodyPart);
                            callback.onSuccess(measurements);
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "Неизвестная ошибка";
                            Log.e(TAG, "Ошибка при получении истории: " + response.code() + " - " + errorBody);
                            callback.onError("Ошибка сервера: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке ответа: " + e.getMessage(), e);
                        callback.onError("Ошибка обработки данных");
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка доступа к HTTP клиенту: " + e.getMessage(), e);
            callback.onError("Ошибка инициализации");
        }
    }


    public void getLatestMeasurement(String bodyPart, SingleMeasurementCallback callback) {
        getMeasurementHistory(bodyPart, 1, new MeasurementListCallback() {
            @Override
            public void onSuccess(List<MeasurementRecord> measurements) {
                if (!measurements.isEmpty()) {
                    callback.onSuccess(measurements.get(0));
                } else {
                    callback.onError("Замеры не найдены");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }


    public void getAllLatestMeasurements(MeasurementListCallback callback) {

        if (supabaseClient.getUserToken() == null || supabaseClient.getUserToken().isEmpty()) {
            Log.e(TAG, "Отсутствует токен авторизации для получения всех замеров");
            callback.onError("Пользователь не авторизован. Войдите в систему");
            return;
        }


        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Отсутствует ID пользователя для получения всех замеров");
            callback.onError("ID пользователя не найден. Войдите в систему");
            return;
        }

        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/latest_body_measurements";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + supabaseClient.getUserToken())
                .addHeader("apikey", supabaseClient.getApiKey())
                .build();

        try {
            java.lang.reflect.Field clientField = SupabaseClient.class.getDeclaredField("client");
            clientField.setAccessible(true);
            okhttp3.OkHttpClient httpClient = (okhttp3.OkHttpClient) clientField.get(supabaseClient);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Ошибка при получении всех замеров: " + e.getMessage(), e);
                    callback.onError("Ошибка сети: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            List<MeasurementRecord> measurements = parseMeasurements(responseBody);
                            Log.d(TAG, "Получено " + measurements.size() + " последних замеров");
                            callback.onSuccess(measurements);
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "Неизвестная ошибка";
                            Log.e(TAG, "Ошибка при получении всех замеров: " + response.code() + " - " + errorBody);
                            callback.onError("Ошибка сервера: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке ответа: " + e.getMessage(), e);
                        callback.onError("Ошибка обработки данных");
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка доступа к HTTP клиенту: " + e.getMessage(), e);
            callback.onError("Ошибка инициализации");
        }
    }


    public void updateMeasurement(String measurementId, String note, MeasurementCallback callback) {
        try {
            JSONObject updateData = new JSONObject();
            if (note != null) {
                updateData.put("note", note.trim());
            }

            RequestBody body = RequestBody.create(updateData.toString(), JSON);

            String url = SupabaseClient.SUPABASE_URL + "/rest/v1/" + TABLE_NAME + "?id=eq." + measurementId;

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("Authorization", "Bearer " + supabaseClient.getUserToken())
                    .addHeader("apikey", supabaseClient.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .build();

            try {
                java.lang.reflect.Field clientField = SupabaseClient.class.getDeclaredField("client");
                clientField.setAccessible(true);
                okhttp3.OkHttpClient httpClient = (okhttp3.OkHttpClient) clientField.get(supabaseClient);

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Ошибка при обновлении замера: " + e.getMessage(), e);
                        callback.onError("Ошибка сети: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Замер успешно обновлен: " + measurementId);
                            callback.onSuccess();
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "Неизвестная ошибка";
                            Log.e(TAG, "Ошибка при обновлении замера: " + response.code() + " - " + errorBody);
                            callback.onError("Ошибка сервера: " + response.code());
                        }
                        response.close();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка доступа к HTTP клиенту: " + e.getMessage(), e);
                callback.onError("Ошибка инициализации");
            }

        } catch (JSONException e) {
            Log.e(TAG, "Ошибка JSON при обновлении замера: " + e.getMessage(), e);
            callback.onError("Ошибка формирования данных");
        }
    }


    public void deleteMeasurement(String measurementId, MeasurementCallback callback) {
        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/" + TABLE_NAME + "?id=eq." + measurementId;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + supabaseClient.getUserToken())
                .addHeader("apikey", supabaseClient.getApiKey())
                .build();

        try {
            java.lang.reflect.Field clientField = SupabaseClient.class.getDeclaredField("client");
            clientField.setAccessible(true);
            okhttp3.OkHttpClient httpClient = (okhttp3.OkHttpClient) clientField.get(supabaseClient);

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Ошибка при удалении замера: " + e.getMessage(), e);
                    callback.onError("Ошибка сети: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Замер успешно удален: " + measurementId);
                        callback.onSuccess();
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Неизвестная ошибка";
                        Log.e(TAG, "Ошибка при удалении замера: " + response.code() + " - " + errorBody);
                        callback.onError("Ошибка сервера: " + response.code());
                    }
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка доступа к HTTP клиенту: " + e.getMessage(), e);
            callback.onError("Ошибка инициализации");
        }
    }


    private List<MeasurementRecord> parseMeasurements(String jsonResponse) throws JSONException {
        List<MeasurementRecord> measurements = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonResponse);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            try {
                String id = jsonObject.optString("id", "");
                float value = (float) jsonObject.getDouble("value");
                String dateString = jsonObject.getString("measurement_date");
                String note = jsonObject.optString("note", "");
                String bodyPart = jsonObject.optString("body_part", "");


                Date date = parseFlexibleDate(dateString);

                MeasurementRecord record = new MeasurementRecord(id, value, date, note);
                record.setBodyPart(bodyPart);
                measurements.add(record);

            } catch (ParseException e) {
                Log.e(TAG, "Ошибка парсинга даты: " + e.getMessage(), e);

            }
        }

        return measurements;
    }


    private Date parseFlexibleDate(String dateString) throws ParseException {

        for (SimpleDateFormat format : dateFormats) {
            try {
                Date parsedDate = format.parse(dateString);
                Log.d(TAG, "Дата успешно распарсена форматом: " + format.toPattern() + " для строки: " + dateString);
                return parsedDate;
            } catch (ParseException e) {

                continue;
            }
        }


        Log.e(TAG, "Не удалось распарсить дату ни одним из доступных форматов: " + dateString);
        throw new ParseException("Неподдерживаемый формат даты: " + dateString, 0);
    }


    public static String convertBodyPartToApi(String russianBodyPart) {
        switch (russianBodyPart.toLowerCase()) {
            case "бицепс левый":
                return "bicep_left";
            case "бицепс правый":
                return "bicep_right";
            case "талия":
                return "waist";
            case "грудь":
                return "chest";
            case "бедра":
                return "hip";
            case "шея":
                return "neck";
            case "бедро левое":
                return "thigh_left";
            case "бедро правое":
                return "thigh_right";
            case "предплечье левое":
                return "forearm_left";
            case "предплечье правое":
                return "forearm_right";
            case "икра левая":
                return "calf_left";
            case "икра правая":
                return "calf_right";
            case "ширина плеч":
                return "shoulder_width";
            case "запястье левое":
                return "wrist_left";
            case "запястье правое":
                return "wrist_right";
            default:
                return russianBodyPart.toLowerCase().replace(" ", "_");
        }
    }


    public static String convertBodyPartFromApi(String apiBodyPart) {
        switch (apiBodyPart.toLowerCase()) {
            case "bicep_left":
                return "Бицепс левый";
            case "bicep_right":
                return "Бицепс правый";
            case "waist":
                return "Талия";
            case "chest":
                return "Грудь";
            case "hip":
                return "Бедра";
            case "neck":
                return "Шея";
            case "thigh_left":
                return "Бедро левое";
            case "thigh_right":
                return "Бедро правое";
            case "forearm_left":
                return "Предплечье левое";
            case "forearm_right":
                return "Предплечье правое";
            case "calf_left":
                return "Икра левая";
            case "calf_right":
                return "Икра правая";
            case "shoulder_width":
                return "Ширина плеч";
            case "wrist_left":
                return "Запястье левое";
            case "wrist_right":
                return "Запястье правое";
            default:
                return apiBodyPart.replace("_", " ");
        }
    }
}
