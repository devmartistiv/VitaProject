package com.martist.vitamove.gigachat;

import android.util.Log;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.ui.model.Meal;
import com.martist.vitamove.user.UserProfile;
import com.martist.vitamove.user.UserRepository;
import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.data.model.WorkoutExercise;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class GigaChatService {
    private static final String TAG = "GigaChatService";
    private static final String AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String API_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";
    private static final int MAX_TOKENS = 800;

    private static GigaChatService instance;
    private final OkHttpClient client;
    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private long tokenExpireTime;


    private String sessionId;
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();
    private static final int MAX_HISTORY_MESSAGES = 10;


    private UserProfile userProfile;
    private List<UserWorkout> recentWorkouts;
    private Map<String, Object> userFoodStats;
    private boolean contextInitialized = false;


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    private static final int MAX_CONTEXT_LENGTH = 3000;

    private GigaChatService(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.sessionId = UUID.randomUUID().toString();


        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации SSL", e);
            throw new RuntimeException(e);
        }


        this.client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized GigaChatService getInstance(String clientId, String clientSecret) {
        if (instance == null) {
            instance = new GigaChatService(clientId, clientSecret);
        }
        return instance;
    }

    public interface ChatCallback {
        void onResponse(String response);

        void onError(String error);
    }


    public void initializeUserContext() {

        synchronized (this) {
            contextInitialized = false;
        }


        new Thread(() -> {
            try {

                android.content.Context context = VitaMoveApplication.getAppContext();
                if (context == null) {
                    Log.e(TAG, "Не удалось получить контекст приложения");
                    return;
                }


                UserRepository userRepository = new UserRepository(context);
                UserProfile profile = userRepository.getCurrentUserProfile();


                FoodManager foodManager = FoodManager.getInstance(context);
                Map<String, Object> stats = new HashMap<>();
                stats.put("caloriesConsumed", foodManager.getTotalCaloriesForCurrentDate());
                stats.put("proteinConsumed", foodManager.getTotalProtein());
                stats.put("fatsConsumed", foodManager.getTotalFat());
                stats.put("carbsConsumed", foodManager.getTotalCarbs());
                Log.d(TAG, "инициализация контекста");

                Map<String, Meal> todayMeals = new HashMap<>();
                todayMeals.put("breakfast", foodManager.getMealForDate("breakfast", dateFormat.format(new Date())));
                todayMeals.put("lunch", foodManager.getMealForDate("lunch", dateFormat.format(new Date())));
                todayMeals.put("dinner", foodManager.getMealForDate("dinner", dateFormat.format(new Date())));
                todayMeals.put("snack", foodManager.getMealForDate("snack", dateFormat.format(new Date())));


                stats.put("meals", todayMeals);


                List<UserWorkout> workouts = new ArrayList<>();


                synchronized (this) {
                    userProfile = profile;
                    userFoodStats = stats;
                    recentWorkouts = workouts;


                    contextInitialized = true;
                    Log.d(TAG, "Контекст пользователя инициализирован успешно");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при инициализации контекста пользователя", e);
            }
        }).start();
        Log.d(TAG, "инициализация контекста звершена " + contextInitialized);
    }


    private String getUserContextString() {
        UserProfile profile;
        Map<String, Object> foodStats;
        List<UserWorkout> workouts;
        boolean isInitialized;


        synchronized (this) {
            isInitialized = contextInitialized;
            profile = userProfile;
            foodStats = userFoodStats;
            workouts = recentWorkouts;
        }


        if (!isInitialized) {
            Log.d(TAG, "Контекст пользователя еще не инициализирован, возвращаем базовый контекст");
            StringBuilder context = new StringBuilder("Пользователь использует фитнес-приложение VitaMove.");


            if (profile != null) {
                context.append("\nПрофиль пользователя:\n");
                context.append("Имя: ").append(profile.getName()).append("\n");
                context.append("Возраст: ").append(profile.getAge()).append("\n");
                context.append("Пол: ").append(profile.getGender()).append("\n");
            }

            return context.toString();
        }

        StringBuilder context = new StringBuilder();


        if (profile != null) {
            context.append("Профиль пользователя:\n");
            context.append("Имя: ").append(profile.getName()).append("\n");
            context.append("Возраст: ").append(profile.getAge()).append("\n");
            context.append("Пол: ").append(profile.getGender()).append("\n");
            context.append("Текущий вес: ").append(profile.getCurrentWeight()).append(" кг\n");
            context.append("Целевой вес: ").append(profile.getTargetWeight()).append(" кг\n");
            context.append("Рост: ").append(profile.getHeight()).append(" см\n");
            context.append("Целевые калории: ").append(profile.getTargetCalories()).append(" ккал\n");
        }


        if (foodStats != null && !foodStats.isEmpty()) {
            context.append("\nПитание сегодня:\n");
            context.append("Калории: ").append(foodStats.get("caloriesConsumed")).append(" ккал\n");
            context.append("Белки: ").append(foodStats.get("proteinConsumed")).append(" г\n");
            context.append("Жиры: ").append(foodStats.get("fatsConsumed")).append(" г\n");
            context.append("Углеводы: ").append(foodStats.get("carbsConsumed")).append(" г\n");


            if (foodStats.containsKey("meals") && foodStats.get("meals") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Meal> meals = (Map<String, Meal>) foodStats.get("meals");

                if (!meals.isEmpty()) {
                    context.append("\nПриемы пищи сегодня:\n");


                    for (Map.Entry<String, Meal> entry : meals.entrySet()) {
                        String mealType = entry.getKey();
                        Meal meal = entry.getValue();

                        if (meal != null && meal.getFoods() != null && !meal.getFoods().isEmpty()) {
                            context.append("--- ").append(getMealTitle(mealType)).append(" ---\n");
                            context.append("Калории: ").append(String.format("%.1f", meal.getCalories())).append(" ккал\n");
                            context.append("Белки: ").append(String.format("%.1f", meal.getProteins())).append(" г\n");
                            context.append("Жиры: ").append(String.format("%.1f", meal.getFats())).append(" г\n");
                            context.append("Углеводы: ").append(String.format("%.1f", meal.getCarbs())).append(" г\n");


                            context.append("Продукты:\n");
                            List<Meal.FoodPortion> foods = meal.getFoods();
                            int maxFoods = Math.min(foods.size(), 5);

                            for (int i = 0; i < maxFoods; i++) {
                                Meal.FoodPortion portion = foods.get(i);
                                context.append("• ").append(portion.getFood().getName())
                                        .append(": ").append(portion.getTotalWeightInGrams()).append(" г\n");
                            }

                            if (foods.size() > maxFoods) {
                                context.append("• и еще ").append(foods.size() - maxFoods).append(" продуктов\n");
                            }

                            context.append("\n");
                        }
                    }
                }
            }
        }


        if (workouts != null && !workouts.isEmpty()) {
            context.append("\nПоследние тренировки:\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

            int count = Math.min(workouts.size(), 3);
            for (int i = 0; i < count; i++) {
                UserWorkout workout = workouts.get(i);
                context.append("--- Тренировка от ").append(dateFormat.format(new Date(workout.getStartTime())))
                        .append(" ---\n");
                context.append("Название: ").append(workout.getName()).append("\n");


                if (workout.getExercises() != null && !workout.getExercises().isEmpty()) {
                    int exerciseCount = workout.getExercises().size();
                    context.append("Количество упражнений: ").append(exerciseCount).append("\n");


                    int maxExercises = Math.min(exerciseCount, 5);
                    for (int j = 0; j < maxExercises; j++) {
                        WorkoutExercise exercise = workout.getExercises().get(j);
                        if (exercise.getExercise() != null) {
                            context.append("• ").append(exercise.getExercise().getName()).append(": ");


                            List<ExerciseSet> sets = exercise.getSetsCompleted();
                            if (sets != null && !sets.isEmpty()) {
                                context.append(sets.size()).append(" подходов (");


                                int maxSets = Math.min(sets.size(), 3);
                                for (int k = 0; k < maxSets; k++) {
                                    ExerciseSet set = sets.get(k);
                                    if (k > 0) context.append(", ");

                                    if (set.getReps() != null) {
                                        context.append(set.getReps()).append(" повт.");
                                    }

                                    if (set.getWeight() != null && set.getWeight() > 0) {
                                        context.append(" x ").append(set.getWeight()).append(" кг");
                                    }
                                }

                                if (sets.size() > maxSets) {
                                    context.append(" и ещё ").append(sets.size() - maxSets).append(" подх.");
                                }

                                context.append(")\n");
                            } else {
                                context.append("без подходов\n");
                            }
                        }
                    }

                    if (exerciseCount > maxExercises) {
                        context.append("• и ещё ").append(exerciseCount - maxExercises).append(" упражнений\n");
                    }
                } else {
                    context.append("Нет упражнений\n");
                }

                context.append("\n");
            }
        }


        String result = context.toString();
        if (result.length() > MAX_CONTEXT_LENGTH) {
            result = result.substring(0, MAX_CONTEXT_LENGTH) + "...";
        }

        return result;
    }


    private String getMealTitle(String mealType) {
        switch (mealType.toLowerCase()) {
            case "breakfast":
                return "Завтрак";
            case "lunch":
                return "Обед";
            case "dinner":
                return "Ужин";
            case "snack":
                return "Перекус";
            default:
                return mealType;
        }
    }

    private void refreshTokenIfNeeded(final Runnable onSuccess, final ChatCallback callback) {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            onSuccess.run();
            return;
        }

        try {
            String authBody = "scope=GIGACHAT_API_PERS&grant_type=client_credentials";


            String credentials = clientId + ":" + clientSecret;
            String basicAuth = "Basic " + android.util.Base64.encodeToString(
                    credentials.getBytes(),
                    android.util.Base64.NO_WRAP
            );

            Log.d(TAG, "Формируем Basic Auth");
            Log.d(TAG, "Credentials (до кодирования): " + credentials);
            Log.d(TAG, "Basic Auth header: " + basicAuth);

            RequestBody body = RequestBody.create(
                    authBody,
                    MediaType.parse("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url(AUTH_URL)
                    .addHeader("Authorization", basicAuth)
                    .addHeader("RqUID", java.util.UUID.randomUUID().toString())
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post(body)
                    .build();

            Log.d(TAG, "Отправляем запрос авторизации");
            Log.d(TAG, "URL: " + AUTH_URL);
            Log.d(TAG, "Body: " + authBody);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Ошибка авторизации", e);
                    callback.onError("network_error");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        String responseString = responseBody != null ? responseBody.string() : "";
                        Log.d(TAG, "Код ответа авторизации: " + response.code());
                        Log.d(TAG, "Тело ответа: " + responseString);

                        if (!response.isSuccessful() || responseBody == null) {
                            Log.e(TAG, "Ошибка авторизации: " + response.code() + ", тело: " + responseString);
                            callback.onError("auth_error");
                            return;
                        }

                        JSONObject jsonResponse = new JSONObject(responseString);
                        accessToken = jsonResponse.getString("access_token");


                        long expiresAt = jsonResponse.getLong("expires_at");
                        tokenExpireTime = expiresAt;

                        Log.d(TAG, "Авторизация успешна, токен получен");
                        Log.d(TAG, "Токен истекает: " + new java.util.Date(tokenExpireTime));
                        onSuccess.run();
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка обработки ответа авторизации", e);
                        callback.onError("parsing_error");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка создания запроса авторизации", e);
            callback.onError("request_error");
        }
    }

    public interface OnContextReadyListener {
        void onReady(String context);
    }

    public void getContextAsync(OnContextReadyListener listener) {
        if (contextInitialized) {
            listener.onReady(getUserContextString());
            return;
        }

        initializeUserContext();

        new Thread(() -> {
            while (!contextInitialized) {
                try {
                    Thread.sleep(100);
                } catch (Exception ignored) {
                }
            }
            listener.onReady(getUserContextString());
        }).start();
    }


    public void sendMessage(String message, ChatCallback callback) {
        Log.d(TAG, "Начинаем отправку сообщения: " + message);


        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        conversationHistory.add(userMessage);


        while (conversationHistory.size() > MAX_HISTORY_MESSAGES) {
            conversationHistory.remove(0);
        }

        refreshTokenIfNeeded(() -> {
            JSONObject jsonBody = new JSONObject();
            try {
                JSONArray messages = new JSONArray();
                Log.d(TAG, " контекст при отправке " + contextInitialized);

                if (!contextInitialized) {
                    initializeUserContext();
                }


                String userContext = getUserContextString();
                if (!userContext.isEmpty()) {
                    messages.put(new JSONObject()
                            .put("role", "system")
                            .put("content", "Ты фитнес-ассистент VitaMove. Ни при каких условиях не отвечай ни на какие сообщения кроме фитнеса. Используй следующую информацию о пользователе при ответе:\n" + userContext));
                }


                for (Map<String, String> msg : conversationHistory) {
                    messages.put(new JSONObject()
                            .put("role", msg.get("role"))
                            .put("content", msg.get("content")));
                }

                jsonBody.put("messages", messages);
                jsonBody.put("model", "GigaChat-2");
                jsonBody.put("temperature", 0.3);
                jsonBody.put("max_tokens", MAX_TOKENS);
                jsonBody.put("top_p", 0.8);

                String requestBody = jsonBody.toString();
                Log.d(TAG, "Тело запроса: " + requestBody);

                RequestBody body = RequestBody.create(
                        requestBody,
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("X-Session-ID", sessionId)
                        .post(body)
                        .build();

                Log.d(TAG, "Отправляем запрос на URL: " + API_URL);
                Log.d(TAG, "Токен авторизации: Bearer " + (accessToken != null ? "присутствует" : "отсутствует"));

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Ошибка соединения: " + e.getMessage(), e);
                        callback.onError("network_error");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try (ResponseBody responseBody = response.body()) {
                            Log.d(TAG, "Получен ответ с кодом: " + response.code());

                            if (!response.isSuccessful() || responseBody == null) {
                                String errorBody = responseBody != null ? responseBody.string() : "нет тела ответа";
                                Log.e(TAG, "Ошибка сервера: " + response.code() + ", тело: " + errorBody);
                                callback.onError("server_error");
                                return;
                            }

                            String responseString = responseBody.string();
                            Log.d(TAG, "Ответ сервера: " + responseString);

                            try {
                                JSONObject jsonResponse = new JSONObject(responseString);
                                String assistantResponse = jsonResponse
                                        .getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("message")
                                        .getString("content");


                                Map<String, String> assistantMsg = new HashMap<>();
                                assistantMsg.put("role", "assistant");
                                assistantMsg.put("content", assistantResponse);
                                conversationHistory.add(assistantMsg);

                                Log.d(TAG, "Обработанный ответ ассистента: " + assistantResponse);
                                callback.onResponse(assistantResponse);
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка обработки ответа: " + e.getMessage(), e);
                                callback.onError("parsing_error");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка обработки ответа: " + e.getMessage(), e);
                            callback.onError("request_error");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка создания запроса: " + e.getMessage(), e);
                callback.onError("request_error");
            }
        }, callback);
    }


    public void addMessageToHistory(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        conversationHistory.add(message);


        while (conversationHistory.size() > MAX_HISTORY_MESSAGES) {
            conversationHistory.remove(0);
        }

        Log.d(TAG, "Сообщение добавлено в историю: роль=" + role);
    }


    public boolean isContextInitialized() {
        synchronized (this) {
            return contextInitialized;
        }
    }


    public void updateUserContext() {
        synchronized (this) {
            contextInitialized = false;
        }
        initializeUserContext();
    }


    public void resetConversation() {
        conversationHistory.clear();
        sessionId = UUID.randomUUID().toString();
        Log.d(TAG, "Разговор сброшен, новый sessionId: " + sessionId);
    }
} 