package com.martist.vitamove.core.data.remote;

import static com.martist.vitamove.VitaMoveApplication.context;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.auth.AuthManager;
import com.martist.vitamove.auth.GoogleAuthResult;
import com.martist.vitamove.core.domain.utils.BMICalculator;
import com.martist.vitamove.core.domain.utils.SupabaseCallback;
import com.martist.vitamove.core.domain.utils.TokenRefreshInterceptor;
import com.martist.vitamove.user.UserProfile;
import com.martist.vitamove.user.UserRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {
    public static final String SUPABASE_URL = "https://qjopbdiafgbbstkwmhpt.supabase.co";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static SupabaseClient instance;
    private OkHttpClient client;
    private final String apiKey;
    private String userToken;
    private String refreshToken;

    public SupabaseClient(String clientId, String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }


    public void initializeWithInterceptor(AuthManager authManager) {
        if (authManager == null) {
            Log.e("SupabaseClient", "AuthManager не может быть null");
            return;
        }


        this.client = this.client.newBuilder()
                .addInterceptor(new TokenRefreshInterceptor(this, authManager))
                .build();

        Log.d("SupabaseClient", "OkHttpClient инициализирован с TokenRefreshInterceptor");
    }

    public void setUserToken(String token) {
        this.userToken = token;

        saveTokensToPrefs();
    }

    public String getUserToken() {
        return userToken;
    }

    public String getApiKey() {
        return apiKey;
    }

    public static synchronized SupabaseClient getInstance(String clientId, String apiKey) {
        if (instance == null) {
            instance = new SupabaseClient(clientId, apiKey);
        }
        return instance;
    }


    public String signIn(String email, String password) throws IOException, JSONException {
        Log.d("SupabaseClient", "Вход пользователя: " + email);

        JSONObject signInData = new JSONObject();
        signInData.put("email", email);
        signInData.put("password", password);

        RequestBody body = RequestBody.create(signInData.toString(), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {

                if (responseBody.contains("Invalid login credentials")) {
                    throw new IOException("Неверный email или пароль");
                } else if (responseBody.contains("Email not confirmed")) {
                    throw new IOException("Email не подтвержден");
                }
                throw new IOException("Ошибка входа: " + response.code());
            }

            JSONObject jsonResponse = new JSONObject(responseBody);
            if (!jsonResponse.has("access_token")) {
                throw new IOException("Неверный ответ сервера: отсутствует access_token");
            }

            String accessToken = jsonResponse.getString("access_token");
            String refreshToken = jsonResponse.optString("refresh_token", "");
            setUserToken(accessToken);
            setRefreshToken(refreshToken);

            return responseBody;
        }
    }

    public String signUp(String email, String password) throws IOException, JSONException {
        if (password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать не менее 6 символов");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Некорректный формат email" + email);
        }

        Log.d("SupabaseClient", "Регистрация пользователя: " + email);

        JSONObject signUpData = new JSONObject();
        signUpData.put("email", email);
        signUpData.put("password", password);


        JSONObject metadata = new JSONObject();
        metadata.put("app_version", "1.0");
        signUpData.put("data", metadata);

        RequestBody body = RequestBody.create(signUpData.toString(), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/signup")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();


            if (!response.isSuccessful()) {

                if (responseBody.contains("User already registered")) {
                    throw new IOException("Пользователь с таким email уже зарегистрирован");
                } else if (responseBody.contains("invalid_input")) {
                    throw new IOException("Некорректные данные для регистрации");
                }
                Toast.makeText(VitaMoveApplication.getContext(), "Не удалось авторизоваться", Toast.LENGTH_LONG).show();
            }


            JSONObject jsonResponse = new JSONObject(responseBody);
            if (!jsonResponse.has("access_token")) {
                throw new IOException("Неверный ответ сервера: отсутствует access_token");
            }


            String accessToken = jsonResponse.getString("access_token");
            String refreshToken = jsonResponse.optString("refresh_token", "");
            setUserToken(accessToken);
            setRefreshToken(refreshToken);


            String userId = null;


            try {
                String[] jwtParts = accessToken.split("\\.");
                if (jwtParts.length > 1) {
                    String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                    JSONObject jwtJson = new JSONObject(payload);
                    userId = jwtJson.getString("sub");
                    Log.d("SupabaseClient", "Извлечен ID пользователя из токена: " + userId);


                    try {
                        JSONObject userData = new JSONObject();
                        SharedPreferences prefs = null;

                        try {

                            Context context = VitaMoveApplication.getAppContext();
                            if (context != null) {
                                prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            }
                        } catch (Exception e) {

                            Log.e("UserProfile", "Не удалось получить контекст для доступа к настройкам", e);
                        }
                        String fitnessGoal = prefs.getString("fitness_goal", "weight_loss");
                        String level = prefs.getString("user_fitness_level", "intermediate");

                        UserRepository profile_rep = new UserRepository(context);
                        UserProfile profile = profile_rep.getCurrentUserProfile();
                        float bmi = BMICalculator.calculateBMI(profile.getCurrentWeight(), profile.getHeight());
                        userData.put("id", userId);
                        userData.put("name", profile.getName());
                        userData.put("age", profile.getAge());
                        userData.put("target_calories", profile.getTargetCalories());
                        userData.put("gender", profile.getGender());
                        userData.put("fitness_goal", profile.getWaist());
                        userData.put("height", profile.getName());
                        userData.put("fitness_goal", fitnessGoal);
                        userData.put("height", profile.getHeight());
                        userData.put("current_weight", profile.getCurrentWeight());
                        userData.put("target_weight", profile.getTargetWeight());
                        userData.put("fitness_level", level);
                        userData.put("bmi", bmi);
                        userData.put("target_water", profile.getTargetWater());

                        this.from("users")
                                .insert(userData)
                                .executeInsert();

                        Log.d("SupabaseClient", "Базовый профиль пользователя создан в таблице users");
                    } catch (Exception e) {

                        Log.e("SupabaseClient", "Не удалось создать запись в таблице users: " + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка извлечения ID пользователя из токена: " + e.getMessage(), e);
            }

            return responseBody;
        }
    }


    public GoogleAuthResult signInWithGoogleExtended(String idToken) throws IOException, JSONException {
        Log.d("SupabaseClient", "Расширенная аутентификация через Google ID Token");

        JSONObject signInData = new JSONObject();
        signInData.put("provider", "google");
        signInData.put("id_token", idToken);

        RequestBody body = RequestBody.create(signInData.toString(), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=id_token")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                Log.e("SupabaseClient", "Ошибка Google аутентификации: " + response.code() + " - " + responseBody);


                if (responseBody.contains("Invalid token")) {
                    throw new IOException("Недействительный Google токен");
                } else if (responseBody.contains("User not found")) {
                    throw new IOException("Пользователь не найден в Google");
                } else if (responseBody.contains("Email not verified")) {
                    throw new IOException("Email не подтвержден в Google аккаунте");
                }
                throw new IOException("Ошибка Google аутентификации: " + response.code());
            }

            JSONObject jsonResponse = new JSONObject(responseBody);
            if (!jsonResponse.has("access_token")) {
                throw new IOException("Неверный ответ сервера: отсутствует access_token");
            }

            String accessToken = jsonResponse.getString("access_token");
            String refreshToken = jsonResponse.optString("refresh_token", "");
            setUserToken(accessToken);
            setRefreshToken(refreshToken);


            String userId = null;
            String email = "";
            String name = "";
            boolean isNewUser = false;

            try {
                String[] jwtParts = accessToken.split("\\.");
                if (jwtParts.length > 1) {
                    String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                    JSONObject jwtJson = new JSONObject(payload);
                    userId = jwtJson.getString("sub");
                    email = jwtJson.optString("email", "");
                    name = jwtJson.optString("name", "");
                    Log.d("SupabaseClient", "Извлечены данные Google пользователя: " + userId + ", " + email + ", " + name);


                    try {
                        Log.d("SupabaseClient", "Проверка существования пользователя в таблице users: " + userId);
                        JSONArray result = this.from("users")
                                .select("*")
                                .eq("id", userId)
                                .executeAndGetArray();

                        Log.d("SupabaseClient", "Результат поиска пользователя: найдено записей " + result.length());

                        if (result.length() == 0) {

                            isNewUser = true;
                            Log.d("SupabaseClient", "Обнаружен новый Google пользователь - создаем базовую запись");

                            try {
                                JSONObject userData = new JSONObject();
                                userData.put("id", userId);
                                userData.put("name", name.isEmpty() ? email.split("@")[0] : name);
                                userData.put("age", 30);
                                userData.put("gender", "");
                                userData.put("fitness_goal", "weight_loss");
                                userData.put("height", 170);
                                userData.put("current_weight", 70);
                                userData.put("target_weight", 65);
                                userData.put("fitness_level", "intermediate");
                                userData.put("is_metric", true);
                                userData.put("bmi", 24.2);
                                userData.put("target_calories", 2000);
                                userData.put("target_water", 2.5);

                                this.from("users")
                                        .insert(userData)
                                        .executeInsert();

                                Log.d("SupabaseClient", "Базовая запись Google пользователя создана в таблице users");
                            } catch (Exception insertError) {
                                Log.e("SupabaseClient", "Ошибка создания базовой записи пользователя: " + insertError.getMessage(), insertError);

                            }
                        } else {

                            isNewUser = false;
                            Log.d("SupabaseClient", "Запись Google пользователя уже существует в таблице users");
                        }
                    } catch (Exception e) {
                        Log.e("SupabaseClient", "Ошибка при проверке записи Google пользователя: " + e.getMessage(), e);

                        isNewUser = true;
                    }
                }
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка извлечения данных Google пользователя из токена: " + e.getMessage(), e);
                throw new IOException("Ошибка обработки токена Google");
            }

            Log.d("SupabaseClient", "Google пользователь успешно аутентифицирован. Новый: " + isNewUser);
            return new GoogleAuthResult(responseBody, isNewUser, userId, email, name);
        }
    }


    public void clearCredentialState(androidx.credentials.CredentialManager credentialManager) {
        Log.d("SupabaseClient", "Очистка состояния учетных данных");


        if (credentialManager != null) {
            try {
                Context context = VitaMoveApplication.getContext();
                if (context != null) {
                    credentialManager.clearCredentialStateAsync(
                            new androidx.credentials.ClearCredentialStateRequest(),
                            null,
                            context.getMainExecutor(),
                            new androidx.credentials.CredentialManagerCallback<Void, androidx.credentials.exceptions.ClearCredentialException>() {
                                @Override
                                public void onResult(Void result) {
                                    Log.d("SupabaseClient", "Состояние учетных данных успешно очищено через CredentialManager");
                                }

                                @Override
                                public void onError(androidx.credentials.exceptions.ClearCredentialException e) {
                                    Log.w("SupabaseClient", "Ошибка при очистке состояния учетных данных через CredentialManager: " + e.getMessage(), e);
                                }
                            }
                    );
                }
            } catch (Exception e) {
                Log.w("SupabaseClient", "Не удалось очистить состояние через CredentialManager: " + e.getMessage(), e);
            }
        }


        this.userToken = null;
        this.refreshToken = null;


        try {
            Context context = VitaMoveApplication.getContext();
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .remove("accessToken")
                        .remove("refreshToken")
                        .remove("userId")
                        .remove("userEmail")
                        .remove("userName")
                        .putBoolean("isLogged", false)
                        .apply();
                Log.d("SupabaseClient", "Учетные данные очищены из SharedPreferences");
            }
        } catch (Exception e) {
            Log.e("SupabaseClient", "Ошибка при очистке учетных данных", e);
        }
    }


    public QueryBuilder from(String table) {
        return new QueryBuilder(table);
    }


    public class QueryBuilder {
        private final String table;
        private final StringBuilder query;
        private final List<String> conditions;
        private String selectClause = "*";
        private String orderClause = null;
        private boolean orderAscending = true;
        private String jsonData;
        private String method;
        private final JSONObject headers = new JSONObject();

        public QueryBuilder(String table) {
            this.table = table;
            this.query = new StringBuilder();
            this.conditions = new ArrayList<>();
        }

        private Request.Builder getRequestBuilder() {
            StringBuilder urlBuilder = new StringBuilder(SUPABASE_URL)
                    .append("/rest/v1/")
                    .append(table);

            if (!conditions.isEmpty() || selectClause != null || orderClause != null) {
                urlBuilder.append("?");
                if (selectClause != null) {
                    urlBuilder.append("select=").append(selectClause);
                }
                for (String condition : conditions) {
                    urlBuilder.append("&").append(condition);
                }
                if (orderClause != null) {
                    urlBuilder.append("&order=").append(orderClause)
                            .append(".").append(orderAscending ? "asc" : "desc");
                }
            }

            Request.Builder builder = new Request.Builder()
                    .url(urlBuilder.toString())
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("Content-Type", "application/json");

            if (jsonData != null) {
                builder.addHeader("Prefer", "return=representation");
            }

            return builder;
        }

        public QueryBuilder select(String fields) {
            this.selectClause = fields;
            return this;
        }

        public QueryBuilder eq(String field, Object value) {
            conditions.add(field + "=eq." + urlEncode(value));
            return this;
        }

        public QueryBuilder gt(String field, Object value) {
            conditions.add(field + "=gt." + urlEncode(value));
            return this;
        }

        public QueryBuilder gte(String field, Object value) {
            conditions.add(field + "=gte." + urlEncode(value));
            return this;
        }

        public QueryBuilder lt(String field, Object value) {
            conditions.add(field + "=lt." + urlEncode(value));
            return this;
        }

        public QueryBuilder lte(String field, Object value) {
            conditions.add(field + "=lte." + urlEncode(value));
            return this;
        }


        private String urlEncode(Object value) {
            if (value == null) {
                return "null";
            }
            try {
                return java.net.URLEncoder.encode(value.toString(), "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {

                Log.e("SupabaseClient", "Ошибка URL-кодирования: " + e.getMessage());
                return value.toString();
            }
        }

        public QueryBuilder ilike(String field, String value) {

            conditions.add(field + "=ilike." + urlEncode("*" + value + "*"));
            return this;
        }

        public QueryBuilder is(String field, String value) {
            conditions.add(field + "=is." + value);
            return this;
        }


        public QueryBuilder in(String field, String values) {

            conditions.add(field + "=in." + values);
            return this;
        }


        public QueryBuilder limit(int count) {
            query.append("&limit=").append(count);
            return this;
        }


        public QueryBuilder offset(int count) {
            conditions.add("offset=" + count);
            return this;
        }

        public QueryBuilder order(String field, boolean ascending) {
            this.orderClause = field;
            this.orderAscending = ascending;
            return this;
        }

        public void execute() throws Exception {
            Request.Builder requestBuilder = getRequestBuilder();

            if (jsonData != null) {
                RequestBody body = RequestBody.create(jsonData, JSON);
                requestBuilder.post(body);
            } else {
                requestBuilder.get();
            }

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                handleResponse(response);
            }
        }


        public QueryBuilder insert(JSONObject jsonData) {
            try {
                this.jsonData = jsonData.toString();
                this.method = "POST";
                this.headers.put("Prefer", "return=representation");
                Log.d("SupabaseClient", "Подготовлен JSON для вставки: " + this.jsonData);
                return this;
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка при создании JSON для вставки", e);
                throw new RuntimeException("Ошибка при создании JSON для вставки", e);
            }
        }


        public JSONObject executeAndGetSingle() throws Exception {
            int maxRetries = 3;
            int currentTry = 0;
            Exception lastException = null;


            String requestUrl = getRequestBuilder().build().url().toString();
            Log.d("SupabaseClient", "Выполнение запроса к: " + requestUrl);

            while (currentTry < maxRetries) {
                try {
                    Request.Builder requestBuilder = getRequestBuilder();

                    if (jsonData != null) {
                        RequestBody body = RequestBody.create(jsonData, JSON);
                        requestBuilder.post(body);
                        Log.d("SupabaseClient", "POST запрос с данными: " + jsonData);
                    } else {
                        requestBuilder.get();
                        Log.d("SupabaseClient", "GET запрос");
                    }


                    try (Response response = client.newCall(requestBuilder.build()).execute()) {
                        int responseCode = response.code();
                        Log.d("SupabaseClient", "Код ответа: " + responseCode);

                        if (!response.isSuccessful()) {
                            String responseBody = response.body() != null ? response.body().string() : "null";
                            Log.e("SupabaseClient", "Неудачный запрос, код: " + responseCode + ", тело: " + responseBody);

                            if (responseCode == 401) {
                                throw new IOException("Ошибка авторизации. Будет выполнена автоматическая повторная попытка.");
                            } else if (responseCode >= 500) {

                                Log.w("SupabaseClient", "Серверная ошибка, повторная попытка " + (currentTry + 1));
                                Thread.sleep(1000L * (currentTry + 1));
                                currentTry++;
                                continue;
                            } else {
                                throw new IOException("Запрос не удался: " + response.code() + ", тело: " + responseBody);
                            }
                        }

                        String responseBody = response.body().string();
                        Log.d("SupabaseClient", "Успешный ответ: " + responseBody);

                        if (responseBody == null || responseBody.isEmpty()) {
                            Log.e("SupabaseClient", "Ответ пуст");
                            throw new IOException("Пустой ответ");
                        }


                        if (responseBody.trim().startsWith("[")) {

                            JSONArray array = new JSONArray(responseBody);
                            if (array.length() > 0) {
                                return array.getJSONObject(0);
                            } else {

                                Log.w("SupabaseClient", "Получен пустой массив в ответе, возвращаем null");
                                return null;
                            }
                        } else {

                            return new JSONObject(responseBody);
                        }
                    }
                } catch (Exception e) {
                    lastException = e;
                    Log.e("SupabaseClient", "Ошибка запроса: " + e.getMessage(), e);
                    if (currentTry < maxRetries - 1) {
                        Log.w("SupabaseClient", "Попытка " + (currentTry + 1) + " из " + maxRetries + " не удалась: " + e.getMessage());

                        currentTry++;
                    } else {
                        break;
                    }
                }
            }

            Log.e("SupabaseClient", "Все попытки запроса не удались", lastException);
            throw new IOException("Запрос не удался после " + maxRetries + " попыток: " +
                    (lastException != null ? lastException.getMessage() : "неизвестная ошибка"));
        }


        public JSONArray executeAndGetArray() throws Exception {
            int maxRetries = 3;
            int currentTry = 0;
            Exception lastException = null;


            String requestUrl = getRequestBuilder().build().url().toString();
            Log.d("SupabaseClient", "Выполнение запроса к: " + requestUrl);

            while (currentTry < maxRetries) {
                try {
                    Request.Builder requestBuilder = getRequestBuilder();

                    if (jsonData != null) {
                        RequestBody body = RequestBody.create(jsonData, JSON);


                        if ("POST".equals(method)) {
                            requestBuilder.post(body);
                            Log.d("SupabaseClient", "POST запрос с данными: " + jsonData);
                        } else if ("PATCH".equals(method)) {
                            requestBuilder.patch(body);
                            Log.d("SupabaseClient", "PATCH запрос с данными: " + jsonData);
                        } else if ("PUT".equals(method)) {
                            requestBuilder.put(body);
                            Log.d("SupabaseClient", "PUT запрос с данными: " + jsonData);
                        } else {
                            requestBuilder.post(body);
                            Log.d("SupabaseClient", "Неизвестный метод, используем POST с данными: " + jsonData);
                        }
                    } else {
                        requestBuilder.get();
                        Log.d("SupabaseClient", "GET запрос");
                    }


                    try (Response response = client.newCall(requestBuilder.build()).execute()) {
                        int responseCode = response.code();
                        Log.d("SupabaseClient", "Код ответа: " + responseCode);

                        if (!response.isSuccessful()) {
                            String responseBody = response.body() != null ? response.body().string() : "null";
                            Log.e("SupabaseClient", "Неудачный запрос, код: " + responseCode + ", тело: " + responseBody);

                            if (responseCode == 401) {
                                throw new IOException("Ошибка авторизации. Будет выполнена автоматическая повторная попытка.");
                            } else if (responseCode >= 500) {

                                Log.w("SupabaseClient", "Серверная ошибка, повторная попытка " + (currentTry + 1));

                                currentTry++;
                                continue;
                            } else {
                                throw new IOException("Запрос не удался: " + response.code() + ", тело: " + responseBody);
                            }
                        }

                        String responseBody = response.body().string();
                        Log.d("SupabaseClient", "Успешный ответ: " + responseBody);

                        if (responseBody == null || responseBody.isEmpty()) {
                            Log.e("SupabaseClient", "Ответ пуст");
                            throw new IOException("Пустой ответ");
                        }


                        if (responseBody.equals("[]")) {
                            Log.d("SupabaseClient", "Получен пустой массив");
                            return new JSONArray();
                        }

                        return new JSONArray(responseBody);
                    }
                } catch (Exception e) {
                    lastException = e;
                    Log.e("SupabaseClient", "Ошибка запроса: " + e.getMessage(), e);
                    if (currentTry < maxRetries - 1) {
                        Log.w("SupabaseClient", "Попытка " + (currentTry + 1) + " из " + maxRetries + " не удалась: " + e.getMessage());
                        Thread.sleep(1000L * (currentTry + 1));
                        currentTry++;
                    } else {
                        break;
                    }
                }
            }

            Log.e("SupabaseClient", "Все попытки запроса не удались", lastException);
            throw new IOException("Запрос не удался после " + maxRetries + " попыток: " +
                    (lastException != null ? lastException.getMessage() : "неизвестная ошибка"));
        }


        public QueryBuilder update(JSONObject jsonData) {
            try {
                this.jsonData = jsonData.toString();
                this.method = "PATCH";
                this.headers.put("Prefer", "return=representation");
                return this;
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка при создании JSON для обновления", e);
                throw new RuntimeException("Ошибка при создании JSON для обновления", e);
            }
        }

        public QueryBuilder delete() {
            return this;
        }

        public void executeDelete() throws Exception {
            Request.Builder requestBuilder = getRequestBuilder()
                    .addHeader("Prefer", "return=minimal")
                    .delete();

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                handleResponse(response);
            }
        }

        public void executeUpdate() throws Exception {
            if (jsonData == null) {
                throw new Exception("No data to update");
            }

            RequestBody body = RequestBody.create(jsonData, JSON);
            Request.Builder requestBuilder = getRequestBuilder()
                    .addHeader("Prefer", "return=representation")
                    .patch(body);

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                handleResponse(response);
            }
        }

        public void executeInsert() throws Exception {
            if (jsonData == null) {
                throw new Exception("No data to insert");
            }

            RequestBody body = RequestBody.create(jsonData, JSON);
            Request.Builder requestBuilder = getRequestBuilder()
                    .addHeader("Prefer", "return=representation")
                    .post(body);

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                handleResponse(response);
            }
        }

        private void handleResponse(Response response) throws Exception {
            if (!response.isSuccessful()) {
                String responseBody = "";
                try {
                    responseBody = response.body().string();
                } catch (Exception e) {

                }


                if (response.code() == 300) {
                    Log.w("SupabaseClient", "Получен код 300, проблема с перенаправлением");
                    throw new IOException("Проблема с перенаправлением запроса. Тело ответа: " + responseBody);
                } else if (response.code() >= 500) {
                    Log.e("SupabaseClient", "Серверная ошибка " + response.code() + ": " + responseBody);
                    throw new IOException("Серверная ошибка: " + response.code());
                } else {
                    Log.e("SupabaseClient", "Запрос не удался с кодом " + response.code() + ": " + responseBody);
                    throw new IOException("Запрос не удался: " + response.code() + ". Тело ответа: " + responseBody);
                }
            }
        }


    }

    public void setRefreshToken(String token) {
        this.refreshToken = token;

        saveTokensToPrefs();
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String refreshAccessToken() throws IOException, JSONException, AuthException {
        if (refreshToken == null) {
            throw new IOException("Refresh token is not available");
        }

        JSONObject refreshData = new JSONObject();
        refreshData.put("refresh_token", refreshToken);
        refreshData.put("grant_type", "refresh_token");

        RequestBody body = RequestBody.create(
                refreshData.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();


        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            try {
                responseBody = response.body().string();
                Log.d("SupabaseClient", "Ответ на запрос обновления токена: " + response.code());
            } catch (Exception e) {

            }

            if (!response.isSuccessful()) {
                Log.e("SupabaseClient", "Ошибка обновления токена: " + response.code() + " - " + responseBody);

                if (response.code() == 400) {

                    if (responseBody.contains("refresh_token_already_used") ||
                            responseBody.contains("Already Used") ||
                            responseBody.contains("Invalid Refresh Token")) {


                        Log.e("SupabaseClient", "Refresh token был использован ранее, требуется повторная авторизация");


                        this.userToken = null;
                        this.refreshToken = null;


                        try {
                            Context context = VitaMoveApplication.getContext();
                            if (context != null) {
                                SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
                                prefs.edit()
                                        .remove("accessToken")
                                        .remove("refreshToken")
                                        .apply();
                                Log.d("SupabaseClient", "Токены удалены из SharedPreferences");
                            }
                        } catch (Exception e) {
                            Log.e("SupabaseClient", "Ошибка при удалении токенов из SharedPreferences", e);
                        }

                        throw new TokenInvalidatedException("Refresh token недействителен. Необходима повторная авторизация.");
                    }


                    throw new AuthException("Токен обновления недействителен или истек. Необходима повторная авторизация.");
                }

                throw new IOException("Failed to refresh token: " + response.code() + " - " + responseBody);
            }

            JSONObject jsonResponse = new JSONObject(responseBody);

            String newAccessToken = jsonResponse.getString("access_token");
            String newRefreshToken = jsonResponse.getString("refresh_token");

            Log.d("SupabaseClient", "Токен успешно обновлен");

            setUserToken(newAccessToken);
            setRefreshToken(newRefreshToken);

            return newAccessToken;
        } catch (JSONException e) {
            Log.e("SupabaseClient", "Ошибка парсинга JSON ответа", e);
            throw new IOException("Ошибка при обработке ответа сервера: " + e.getMessage(), e);
        }
    }

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }


    }

    public static class TokenRefreshedException extends AuthException {
        public TokenRefreshedException() {
            super("Token refreshed, please retry the request");
        }
    }


    public static class TokenInvalidatedException extends AuthException {
        public TokenInvalidatedException(String message) {
            super(message);
        }
    }

    public RpcBuilder rpc(String functionName) {
        return new RpcBuilder(functionName);
    }

    public class RpcBuilder {
        private final String functionName;
        private final JSONObject params;

        public RpcBuilder(String functionName) {
            this.functionName = functionName;
            this.params = new JSONObject();
        }

        public RpcBuilder param(String name, Object value) {
            try {
                params.put(name, value);
            } catch (JSONException e) {
                Log.e("SupabaseClient", "Ошибка при добавлении параметра: " + e.getMessage());
            }
            return this;
        }

        public JSONArray executeAndGetArray() throws Exception {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/rpc/" + functionName)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            RequestBody body = RequestBody.create(params.toString(), JSON);
            Request request = requestBuilder.post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    Log.e("SupabaseClient", "Ошибка RPC: " + errorBody);
                    throw new IOException("Ошибка при вызове RPC функции: " + response.code() + " " + errorBody);
                }

                String responseBody = response.body().string();
                return new JSONArray(responseBody);
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка при выполнении RPC: " + e.getMessage());
                throw e;
            }
        }

        public JSONObject executeAndGetSingle() throws Exception {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/rpc/" + functionName)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            RequestBody body = RequestBody.create(params.toString(), JSON);
            Request request = requestBuilder.post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    Log.e("SupabaseClient", "Ошибка RPC: " + errorBody);
                    throw new IOException("Ошибка при вызове RPC функции: " + response.code() + " " + errorBody);
                }

                String responseBody = response.body().string();
                return new JSONObject(responseBody);
            } catch (Exception e) {
                Log.e("SupabaseClient", "Ошибка при выполнении RPC: " + e.getMessage());
                throw e;
            }
        }


        public <T> void executeAsync(SupabaseCallback<T> callback) {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/rpc/" + functionName)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            RequestBody body = RequestBody.create(params.toString(), JSON);
            Request request = requestBuilder.post(body).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("SupabaseClient", "Ошибка выполнения RPC запроса: " + e.getMessage(), e);


                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onFailure(e);
                    });
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    try (Response responseClone = response) {
                        int responseCode = responseClone.code();
                        Log.d("SupabaseClient", "Код ответа RPC: " + responseCode);

                        if (!responseClone.isSuccessful()) {
                            String responseBody = responseClone.body() != null ? responseClone.body().string() : "null";
                            Log.e("SupabaseClient", "Неудачный RPC запрос, код: " + responseCode + ", тело: " + responseBody);


                            Exception error = new IOException("Ошибка сервера: " + responseCode + " - " + responseBody);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                callback.onFailure(error);
                            });
                            return;
                        }

                        try {
                            String responseBody = responseClone.body() != null ? responseClone.body().string() : "null";


                            if (responseBody.startsWith("{")) {

                                JSONObject jsonResult = new JSONObject(responseBody);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    try {
                                        callback.onSuccess((T) jsonResult);
                                    } catch (ClassCastException e) {
                                        callback.onFailure(new Exception("Неправильный тип данных в ответе", e));
                                    }
                                });
                            } else if (responseBody.startsWith("[")) {

                                JSONArray jsonArray = new JSONArray(responseBody);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    try {
                                        callback.onSuccess((T) jsonArray);
                                    } catch (ClassCastException e) {
                                        callback.onFailure(new Exception("Неправильный тип данных в ответе", e));
                                    }
                                });
                            } else {

                                new Handler(Looper.getMainLooper()).post(() -> {
                                    try {
                                        callback.onSuccess((T) responseBody);
                                    } catch (ClassCastException e) {
                                        callback.onFailure(new Exception("Неправильный тип данных в ответе", e));
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e("SupabaseClient", "Ошибка обработки ответа RPC: " + e.getMessage(), e);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                callback.onFailure(e);
                            });
                        }
                    }
                }
            });
        }
    }


    private void executeAsync(Request request, AsyncCallback callback) {
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("SupabaseClient", "Ошибка выполнения запроса: " + e.getMessage(), e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (Response responseClone = response) {
                    int responseCode = responseClone.code();
                    Log.d("SupabaseClient", "Код ответа: " + responseCode);

                    if (!responseClone.isSuccessful()) {
                        String responseBody = responseClone.body() != null ? responseClone.body().string() : "null";
                        Log.e("SupabaseClient", "Неудачный запрос, код: " + responseCode + ", тело: " + responseBody);


                        callback.onFailure(new IOException("Ошибка сервера: " + responseCode + " - " + responseBody));
                        return;
                    }

                    String responseBody = responseClone.body() != null ? responseClone.body().string() : "null";
                    callback.onSuccess(responseBody);
                } catch (Exception e) {
                    Log.e("SupabaseClient", "Ошибка обработки ответа: " + e.getMessage(), e);
                    callback.onFailure(e);
                }
            }
        });
    }


    public interface AsyncCallback {
        void onSuccess(String responseBody);

        void onFailure(Exception e);
    }


    public JSONObject insertRecord(String tableName, JSONObject data) throws IOException, JSONException {
        Log.d("SupabaseClient", "Вставка записи в таблицу " + tableName + ": " + data.toString());


        String url = SUPABASE_URL + "/rest/v1/" + tableName;


        RequestBody body = RequestBody.create(
                data.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );


        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка запроса: " + response.code());
            }

            String responseBody = response.body().string();
            JSONArray jsonArray = new JSONArray(responseBody);
            JSONObject jsonResponse = new JSONObject();

            if (jsonArray.length() > 0) {
                jsonResponse.put("data", jsonArray.getJSONObject(0));
            } else {
                jsonResponse.put("data", new JSONObject());
            }

            return jsonResponse;
        }
    }


    private void saveTokensToPrefs() {
        try {
            Context context = VitaMoveApplication.getContext();
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
                if (userToken != null) {
                    prefs.edit()
                            .putString("accessToken", userToken)
                            .apply();
                    Log.d("SupabaseClient", "Access token сохранен в SharedPreferences");
                }
                if (refreshToken != null) {
                    prefs.edit()
                            .putString("refreshToken", refreshToken)
                            .putLong("tokenUpdateTime", System.currentTimeMillis())
                            .apply();
                    Log.d("SupabaseClient", "Refresh token сохранен в SharedPreferences");
                }
            }
        } catch (Exception e) {
            Log.e("SupabaseClient", "Ошибка при сохранении токенов в SharedPreferences", e);
        }
    }


    public boolean updateUserProfile(String userId, String name, int age, String gender,
                                     String fitnessGoal, float height, float currentWeight,
                                     float targetWeight, String fitnessLevel, boolean isMetric) {
        Log.d("SupabaseClient", "Обновление/создание профиля пользователя в Supabase: " + userId);

        try {

            float bmi = 0;
            if (height > 0 && currentWeight > 0) {
                if (isMetric) {

                    bmi = currentWeight / ((height / 100) * (height / 100));
                } else {


                    bmi = currentWeight / ((height / 100) * (height / 100));
                }
            }


            UserProfile tempProfile = new UserProfile(name, age, gender, currentWeight, targetWeight, height, 0, 0);
            int targetCalories = tempProfile.calculateTargetCalories();
            float targetWater = tempProfile.calculateTargetWater();


            JSONObject data = new JSONObject();
            data.put("id", userId);
            data.put("name", name);
            data.put("age", age);
            data.put("gender", gender);
            data.put("fitness_goal", fitnessGoal);
            data.put("height", height);
            data.put("current_weight", currentWeight);
            data.put("target_weight", targetWeight);
            data.put("bmi", bmi);
            data.put("fitness_level", fitnessLevel);
            data.put("is_metric", isMetric);
            data.put("target_calories", targetCalories);
            data.put("target_water", targetWater);


            try {
                Log.d("SupabaseClient", "Попытка обновления профиля пользователя: " + userId);
                from("users")
                        .eq("id", userId)
                        .update(data)
                        .executeUpdate();

                Log.d("SupabaseClient", "Профиль пользователя успешно обновлен: " + userId);
                return true;
            } catch (Exception e) {

                Log.d("SupabaseClient", "Запись не найдена, создаем новую для пользователя: " + userId);
                try {

                    from("users")
                            .insert(data)
                            .executeInsert();

                    Log.d("SupabaseClient", "Профиль пользователя успешно создан: " + userId);
                    return true;
                } catch (Exception insertError) {
                    Log.e("SupabaseClient", "Ошибка при создании профиля: " + insertError.getMessage(), insertError);
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e("SupabaseClient", "Ошибка при обновлении профиля: " + e.getMessage(), e);
            return false;
        }
    }


    public void deleteUserAccount(String userId, AsyncCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("ID пользователя не может быть пустым"));
            }
            return;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("userId", userId);


            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/functions/v1/delete-user")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .addHeader("Authorization", "Bearer " + userToken)
                    .addHeader("apikey", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            executeAsync(request, callback);

        } catch (JSONException e) {
            Log.e("SupabaseClient", "Ошибка при создании запроса для удаления пользователя: " + e.getMessage(), e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }
}