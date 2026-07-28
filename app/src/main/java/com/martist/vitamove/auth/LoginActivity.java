package com.martist.vitamove.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.preference.PreferenceManager;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.nutrition.data.managers.CaloriesManager;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialButton googleLoginButton;
    private TextView registerLink;
    private SupabaseClient supabaseClient;


    private CredentialManager credentialManager;


    private boolean isGoogleSignInInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting LoginActivity");


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.activity_login);


        credentialManager = CredentialManager.create(this);

        Log.d(TAG, "onCreate: Initializing Supabase client");
        supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );

        initializeViews();
        setupClickListeners();


        clearPreviousUserCache();
    }


    private void clearPreviousUserCache() {
        try {

            SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
            String prevUserId = prefs.getString("userId", null);

            if (prevUserId != null && !prevUserId.isEmpty()) {
                Log.d(TAG, "Очистка кэша предыдущего пользователя: " + prevUserId);


                CaloriesManager.resetInstance();
                FoodManager.resetInstance();
                DashboardManager.resetInstance();

            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке кэша: " + e.getMessage());
        }
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.login_email);
        passwordInput = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        googleLoginButton = findViewById(R.id.google_login_button);
        registerLink = findViewById(R.id.register_link);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());


        googleLoginButton.setOnClickListener(v -> signInWithGoogle());
        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SurveyActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();


        if (email.isEmpty()) {
            showError("Пожалуйста, введите email");
            return;
        }

        if (password.isEmpty()) {
            showError("Пожалуйста, введите пароль");
            return;
        }


        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Пожалуйста, введите корректный email адрес");
            return;
        }


        loginButton.setEnabled(false);


        sign_in(email, password);
    }

    public void sign_in(String email, String password) {
        new Thread(() -> {
            try {

                FoodManager.resetInstance();
                CaloriesManager.resetInstance();
                DashboardManager.resetInstance();
                Log.d(TAG, "Все менеджеры данных сброшены перед входом в аккаунт");


                String responseJson = supabaseClient.signIn(email, password);
                JSONObject jsonResponse = new JSONObject(responseJson);


                String accessToken = jsonResponse.getString("access_token");
                String refreshToken = jsonResponse.optString("refresh_token", "");


                String userId = null;

                String[] jwtParts = accessToken.split("\\.");
                if (jwtParts.length > 1) {
                    String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                    JSONObject jwtJson = new JSONObject(payload);
                    userId = jwtJson.getString("sub");
                    Log.d(TAG, "Получен ID пользователя из токена: " + userId);
                }


                final String finalUserId = userId;

                runOnUiThread(() -> {
                    try {

                        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLogged", true)
                                .putBoolean("isFirstRun", false)
                                .putString("accessToken", accessToken)
                                .putString("refreshToken", refreshToken)
                                .putString("userId", finalUserId)
                                .putString("userEmail", email)
                                .apply();


                        loadUserProfileFromSupabase(finalUserId);


                        isGoogleSignInInProgress = false;


                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка сохранения данных: " + e.getMessage());
                        showError("Ошибка сохранения данных: " + e.getMessage());
                        loginButton.setEnabled(true);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Login error: " + e.getMessage());
                String errorMessage = e.getMessage();


                String userFriendlyMessage;
                if (errorMessage.contains("Invalid login credentials")) {
                    userFriendlyMessage = "Неверный email или пароль. Пожалуйста, проверьте данные и попробуйте снова.";
                } else if (errorMessage.contains("Email not confirmed")) {
                    userFriendlyMessage = "Email не подтвержден. Проверьте вашу почту и подтвердите регистрацию.";
                } else if (errorMessage.contains("User not found")) {
                    userFriendlyMessage = "Пользователь с таким email не найден. Возможно, вы еще не зарегистрированы.";
                } else if (errorMessage.contains("not activated")) {
                    userFriendlyMessage = "Ваша учетная запись не активирована. Пожалуйста, свяжитесь с поддержкой.";
                } else {
                    userFriendlyMessage = "Ошибка входа: " + errorMessage;
                }

                final String finalMessage = userFriendlyMessage;
                runOnUiThread(() -> {
                    showError(finalMessage);
                    loginButton.setEnabled(true);
                });
            }
        }).start();
    }


    private void signInWithGoogle() {

        if (isGoogleSignInInProgress) {
            Log.d(TAG, "Google Sign-In уже выполняется, игнорируем повторный запрос");
            return;
        }


        isGoogleSignInInProgress = true;


        googleLoginButton.setEnabled(false);
        googleLoginButton.setText("Подключение к Google...");


        trySignInWithAuthorizedAccounts();
    }


    private void trySignInWithAuthorizedAccounts() {
        try {
            Log.d(TAG, "Попытка входа с ранее авторизованными аккаунтами");


            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(Constants.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build();

            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build();

            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    this.getMainExecutor(),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleGoogleSignInResult(result);
                        }

                        @Override
                        public void onError(GetCredentialException e) {
                            Log.d(TAG, "Ошибка при поиске авторизованных аккаунтов: " + e.getMessage());


                            if (e instanceof androidx.credentials.exceptions.GetCredentialCancellationException) {
                                Log.e(TAG, "GetCredentialCancellationException на первом этапе - проблемы с настройками OAuth");
                                handleGoogleSignInError(e);
                                return;
                            }

                            Log.d(TAG, "Не найдено авторизованных аккаунтов, пробуем показать все доступные");

                            trySignInWithAllAccounts();
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при попытке входа с авторизованными аккаунтами: " + e.getMessage(), e);

            trySignInWithAllAccounts();
        }
    }


    private void trySignInWithAllAccounts() {
        try {
            Log.d(TAG, "Показываем все доступные Google аккаунты (включая новую регистрацию)");


            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(Constants.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build();

            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build();

            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    this.getMainExecutor(),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleGoogleSignInResult(result);
                        }

                        @Override
                        public void onError(GetCredentialException e) {
                            handleAuthorizedAccountsError(e);
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при показе всех доступных Google аккаунтов: " + e.getMessage(), e);
            showError("Ошибка инициализации Google входа");
            resetGoogleButton();
        }
    }


    private void handleGoogleSignInResult(GetCredentialResponse result) {
        try {

            GoogleIdTokenCredential credential = GoogleIdTokenCredential.createFrom(
                    result.getCredential().getData()
            );

            String idToken = credential.getIdToken();
            String displayName = credential.getDisplayName();
            String email = credential.getId();

            Log.d(TAG, "Google Sign-In успешен: " + displayName + " (" + email + ")");


            authenticateWithSupabase(idToken, email, displayName);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обработке Google Sign-In результата: " + e.getMessage(), e);
            showError("Ошибка обработки Google входа");
            resetGoogleButton();
        }
    }


    private void handleAuthorizedAccountsError(GetCredentialException e) {


        boolean isCancellationException = e instanceof androidx.credentials.exceptions.GetCredentialCancellationException;
        boolean hasUserCancelMessage = false;
        boolean hasNoCredentialsMessage = false;

        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();
            hasUserCancelMessage = message.contains("cancel") || message.contains("user_cancel") || message.contains("отмен");
            hasNoCredentialsMessage = message.contains("16:") ||
                    message.contains("not signed in") ||
                    message.contains("no credentials") ||
                    message.contains("no account");
        }


        if (isCancellationException || hasUserCancelMessage || hasNoCredentialsMessage ||

                e.getClass().getSimpleName().contains("Cancel") ||
                (e.getCause() != null && e.getCause().getMessage() != null &&
                        e.getCause().getMessage().toLowerCase().contains("cancel"))) {
            Log.d(TAG, "ПЕРЕХОДИМ К ВЫБОРУ ВСЕХ АККАУНТОВ");


            runOnUiThread(() -> {
                googleLoginButton.setText("Выбор аккаунта Google...");
            });


            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                trySignInWithAllAccounts();
            }, 100);
            return;
        }


        Log.d(TAG, "Не удалось определить тип ошибки точно - попробуем показать все аккаунты");


        boolean isNetworkError = e.getMessage() != null &&
                (e.getMessage().toLowerCase().contains("network") ||
                        e.getMessage().toLowerCase().contains("connection") ||
                        e.getMessage().toLowerCase().contains("timeout"));

        if (!isNetworkError) {
            Log.d(TAG, "Не сетевая ошибка - пробуем показать все аккаунты как fallback");
            runOnUiThread(() -> {
                googleLoginButton.setText("Попытка альтернативного входа...");
            });

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                trySignInWithAllAccounts();
            }, 200);
        } else {
            Log.d(TAG, "Сетевая ошибка - обрабатываем как обычную ошибку");
            handleGoogleSignInError(e);
        }
    }


    private void handleGoogleSignInError(GetCredentialException e) {
        Log.e(TAG, "Google Sign-In ошибка: " + e.getMessage(), e);
        Log.e(TAG, "Тип исключения: " + e.getClass().getSimpleName());

        String errorMessage;
        String exceptionMessage = e.getMessage() != null ? e.getMessage() : "";


        if (e instanceof androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d(TAG, "GetCredentialCancellationException на этапе всех аккаунтов - пользователь отменил");
            errorMessage = "Вход через Google отменён пользователем";
        } else if (exceptionMessage.contains("cancell") || exceptionMessage.contains("user_cancel")) {
            errorMessage = "Вход через Google отменён пользователем";
        } else if (exceptionMessage.contains("network") || exceptionMessage.contains("connection")) {
            errorMessage = "Проблемы с сетью. Проверьте интернет соединение";
        } else if (exceptionMessage.contains("16:") || exceptionMessage.contains("Not signed in") || exceptionMessage.contains("no credentials")) {
            errorMessage = "На устройстве нет Google аккаунтов. Добавьте Google аккаунт в настройки устройства";
        } else if (exceptionMessage.contains("developer_error") || exceptionMessage.contains("invalid_client")) {
            errorMessage = "Ошибка конфигурации приложения. Неверные настройки Google OAuth";
        } else {
            errorMessage = "Ошибка входа через Google: " + exceptionMessage;
        }

        showError(errorMessage);
        resetGoogleButton();
    }


    private void authenticateWithSupabase(String idToken, String email, String displayName) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Аутентификация в Supabase с Google ID Token");


                GoogleAuthResult authResult = supabaseClient.signInWithGoogleExtended(idToken);

                JSONObject jsonResponse = new JSONObject(authResult.getResponseJson());
                String accessToken = jsonResponse.getString("access_token");
                String refreshToken = jsonResponse.optString("refresh_token", "");
                String userId = authResult.getUserId();
                boolean isNewUser = authResult.isNewUser();

                Log.d(TAG, "Google аутентификация успешна. Новый пользователь: " + isNewUser);

                runOnUiThread(() -> {
                    try {

                        isGoogleSignInInProgress = false;


                        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLogged", true)
                                .putString("accessToken", accessToken)
                                .putString("refreshToken", refreshToken)
                                .putString("userId", userId)
                                .putString("userEmail", email)
                                .putString("userName", displayName)
                                .apply();

                        if (isNewUser) {

                            Log.d(TAG, "Новый Google пользователь - переход на SurveyActivity");
                            Toast.makeText(this, "Добро пожаловать в VitaMove! Давайте настроим ваш профиль", Toast.LENGTH_LONG).show();


                            prefs.edit().putBoolean("isGoogleRegistration", true).apply();

                            Intent intent = new Intent(this, SurveyActivity.class);
                            intent.putExtra("fromGoogleAuth", true);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {

                            Log.d(TAG, "Существующий Google пользователь - переход в главное приложение");


                            prefs.edit()
                                    .putBoolean("isFirstRun", false)
                                    .putBoolean("isGoogleRegistration", false)
                                    .apply();


                            loadUserProfileFromSupabase(userId);

                            Toast.makeText(this, "С возвращением!", Toast.LENGTH_SHORT).show();


                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка сохранения данных после Google входа: " + e.getMessage());
                        showError("Ошибка сохранения данных: " + e.getMessage());
                        resetGoogleButton();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка аутентификации в Supabase через Google: " + e.getMessage(), e);

                final String errorMessage;
                if (e.getMessage().contains("Invalid token")) {
                    errorMessage = "Недействительный Google токен";
                } else if (e.getMessage().contains("Email not verified")) {
                    errorMessage = "Email не подтвержден в Google аккаунте";
                } else {
                    errorMessage = "Ошибка аутентификации: " + e.getMessage();
                }

                runOnUiThread(() -> {
                    showError(errorMessage);
                    resetGoogleButton();
                });
            }
        }).start();
    }


    private void resetGoogleButton() {
        isGoogleSignInInProgress = false;
        googleLoginButton.setEnabled(true);
        googleLoginButton.setText("Войти через Google");
    }


    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: LoginActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: LoginActivity paused");
    }


    private void loadUserProfileFromSupabase(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "loadUserProfileFromSupabase: userId пустой или null");
            return;
        }


        new Thread(() -> {
            try {
                Log.d(TAG, "Загрузка профиля пользователя из Supabase: " + userId);


                JSONArray result = supabaseClient.from("users")
                        .select("*")
                        .eq("id", userId)
                        .executeAndGetArray();

                if (result.length() > 0) {

                    JSONObject userProfile = result.getJSONObject(0);


                    SharedPreferences userDataPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
                    SharedPreferences.Editor editor = userDataPrefs.edit();


                    editor.putString("name", userProfile.optString("name", ""));
                    editor.putInt("age", userProfile.optInt("age", 30));
                    editor.putString("gender", userProfile.optString("gender", ""));
                    editor.putString("fitness_goal", userProfile.optString("fitness_goal", ""));


                    editor.putFloat("height", (float) userProfile.optDouble("height", 0));
                    editor.putFloat("current_weight", (float) userProfile.optDouble("current_weight", 0));
                    editor.putFloat("target_weight", (float) userProfile.optDouble("target_weight", 0));
                    editor.putFloat("body_fat", (float) userProfile.optDouble("body_fat", 0));
                    editor.putFloat("waist", (float) userProfile.optDouble("waist", 0));


                    editor.putFloat("bmi", (float) userProfile.optDouble("bmi", 0));


                    editor.putString("fitness_level", userProfile.optString("fitness_level", ""));
                    editor.putBoolean("is_metric", userProfile.optBoolean("is_metric", true));


                    editor.putInt("target_calories", userProfile.optInt("target_calories", 0));


                    editor.putFloat("target_water", (float) userProfile.optDouble("target_water", 0));


                    editor.putBoolean("is_synchronized", true);

                    editor.apply();


                    try {
                        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor appEditor = appPrefs.edit();


                        appEditor.putString("fitness_goal", userProfile.optString("fitness_goal", "weight_loss"));
                        appEditor.putString("user_fitness_level", userProfile.optString("fitness_level", "intermediate"));
                        appEditor.apply();

                        Log.d(TAG, "Настройки синхронизированы между SharedPreferences: fitness_goal=" +
                                userProfile.optString("fitness_goal", "weight_loss") +
                                ", fitness_level=" + userProfile.optString("fitness_level", "intermediate"));
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при синхронизации настроек между SharedPreferences: " + e.getMessage(), e);
                    }

                    Log.d(TAG, "Профиль пользователя успешно загружен и сохранен в SharedPreferences.");


                    runOnUiThread(() -> {
                        FoodManager.getInstance(LoginActivity.this).refreshNutrientNorms();
                    });


                    try {
                        com.martist.vitamove.workout.data.repository.WorkoutRepository workoutRepository =
                                ((VitaMoveApplication) getApplication()).getWorkoutRepository();

                        if (workoutRepository instanceof com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository) {
                            Log.d(TAG, "Запуск синхронизации тренировок для пользователя: " + userId);

                            com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository supabaseRepo =
                                    (com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository) workoutRepository;

                            supabaseRepo.syncUserWorkouts(
                                    userId,
                                    () -> Log.d(TAG, "Синхронизация тренировок успешно завершена"),
                                    error -> Log.e(TAG, "Ошибка при синхронизации тренировок: " + error.getMessage(), error)
                            );
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при синхронизации тренировок: " + e.getMessage(), e);
                    }

                } else {
                    Log.w(TAG, "Профиль пользователя не найден в Supabase для ID: " + userId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке профиля: " + e.getMessage(), e);
            }
        }).start();
    }
}