package com.martist.vitamove.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.weight.ui.UserWeightViewModel;

import org.json.JSONObject;

public class RegisterActivity extends BaseActivity {
    private static final String TAG = "RegisterActivity";
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton registerButton;
    private MaterialButton googleRegisterButton;
    private TextView loginLink;
    private SupabaseClient supabaseClient;
    private UserWeightViewModel userWeightViewModel;


    private CredentialManager credentialManager;
    private boolean isGoogleSignInInProgress = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.activity_register);


        supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );


        userWeightViewModel = new ViewModelProvider(this).get(UserWeightViewModel.class);


        credentialManager = CredentialManager.create(this);

        initializeViews();
        setupClickListeners();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: Конфигурация экрана изменилась");


        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "onConfigurationChanged: Горизонтальная ориентация");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "onConfigurationChanged: Вертикальная ориентация");
        }
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.register_email_text);
        passwordInput = findViewById(R.id.register_password_text);
        confirmPasswordInput = findViewById(R.id.register_confirm_password_input);
        registerButton = findViewById(R.id.register_button);
        googleRegisterButton = findViewById(R.id.google_register_button);
        loginLink = findViewById(R.id.login_link);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegistration());

        googleRegisterButton.setOnClickListener(v -> signInWithGoogle());

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }

    private void attemptRegistration() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();


        if (email.isEmpty()) {
            showError("Пожалуйста, введите email");
            return;
        }

        if (password.isEmpty()) {
            showError("Пожалуйста, введите пароль");
            return;
        }

        if (confirmPassword.isEmpty()) {
            showError("Пожалуйста, подтвердите пароль");
            return;
        }


        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Пожалуйста, введите корректный email адрес");
            return;
        }


        if (password.length() < 6) {
            showError("Пароль должен содержать не менее 6 символов");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Пароли не совпадают");
            return;
        }


        registerButton.setEnabled(false);
        sign_up(email, password);


    }

    public void sign_up(String email, String password) {
        new Thread(() -> {
            try {

                String responseJson = supabaseClient.signUp(email, password);
                JSONObject jsonResponse = new JSONObject(responseJson);


                String accessToken = jsonResponse.getString("access_token");
                String refreshToken = jsonResponse.optString("refresh_token", "");


                String userId = null;
                try {
                    String[] jwtParts = accessToken.split("\\.");
                    if (jwtParts.length > 1) {
                        String payload = new String(Base64.decode(jwtParts[1], Base64.DEFAULT));
                        JSONObject jwtJson = new JSONObject(payload);
                        userId = jwtJson.getString("sub");
                        Log.d(TAG, "Получен ID пользователя из токена: " + userId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка получения ID пользователя из токена", e);
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


                        addInitialWeightRecord();

                        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();


                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка сохранения данных: " + e.getMessage());
                        showError("Ошибка сохранения данных: " + e.getMessage());
                        registerButton.setEnabled(true);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Registration error: " + e.getMessage());
                String errorMessage = e.getMessage();


                String userFriendlyMessage;
                if (errorMessage.contains("уже существует")) {
                    userFriendlyMessage = "Этот email уже зарегистрирован. Попробуйте войти или используйте другой email.";
                } else if (errorMessage.contains("Неверный формат")) {
                    userFriendlyMessage = "Неверный формат email адреса. Пожалуйста, проверьте ввод.";
                } else if (errorMessage.contains("не менее 6 символов")) {
                    userFriendlyMessage = "Пароль должен содержать не менее 6 символов.";
                } else if (errorMessage.contains("422")) {
                    userFriendlyMessage = "Неверный формат данных. Проверьте правильность email и пароля.";
                } else {
                    userFriendlyMessage = "Ошибка регистрации: " + errorMessage;
                }

                final String finalMessage = userFriendlyMessage;
                runOnUiThread(() -> {
                    showError(finalMessage);
                    registerButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    private void addInitialWeightRecord() {

        SharedPreferences userDataPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
        float currentWeight = userDataPrefs.getFloat("current_weight", 0f);


        if (currentWeight > 0) {

            new Thread(() -> {
                try {

                    Thread.sleep(1000);


                    userWeightViewModel.addWeightRecordOnlyToSupabase(currentWeight, "Начальный вес");


                    SharedPreferences.Editor editor = userDataPrefs.edit();
                    editor.putBoolean("weight_record_created", true);
                    editor.apply();

                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при добавлении записи о весе: " + e.getMessage(), e);
                }
            }).start();
        } else {
            Log.w(TAG, "Не удалось добавить запись о весе: текущий вес равен 0");
        }
    }


    private void signInWithGoogle() {
        if (isGoogleSignInInProgress) {
            Log.d(TAG, "Google Sign-In уже выполняется, игнорируем повторный запрос");
            return;
        }

        Log.d(TAG, "signInWithGoogle: Начинаем регистрацию через Google (Credential Manager API)");

        isGoogleSignInInProgress = true;
        googleRegisterButton.setEnabled(false);
        googleRegisterButton.setText("Подключение к Google...");


        trySignInWithAllAccounts();
    }


    private void trySignInWithAllAccounts() {
        try {
            Log.d(TAG, "Показываем все доступные Google аккаунты для регистрации");

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
                            handleGoogleSignInError(e);
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при показе Google аккаунтов для регистрации: " + e.getMessage(), e);
            showError("Ошибка инициализации Google регистрации");
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

            Log.d(TAG, "Google регистрация успешна: " + displayName + " (" + email + ")");


            authenticateWithSupabase(idToken, email, displayName);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обработке Google регистрации: " + e.getMessage(), e);
            showError("Ошибка обработки Google регистрации");
            resetGoogleButton();
        }
    }


    private void handleGoogleSignInError(GetCredentialException e) {
        Log.e(TAG, "Google регистрация ошибка: " + e.getMessage(), e);

        String errorMessage;
        String exceptionMessage = e.getMessage() != null ? e.getMessage() : "";

        if (e instanceof GetCredentialCancellationException) {
            errorMessage = "Регистрация через Google отменена пользователем";
        } else if (exceptionMessage.contains("cancell") || exceptionMessage.contains("user_cancel")) {
            errorMessage = "Регистрация через Google отменена пользователем";
        } else if (exceptionMessage.contains("network") || exceptionMessage.contains("connection")) {
            errorMessage = "Проблемы с сетью. Проверьте интернет соединение";
        } else if (exceptionMessage.contains("16:") || exceptionMessage.contains("Not signed in") || exceptionMessage.contains("no credentials")) {
            errorMessage = "На устройстве нет Google аккаунтов. Добавьте Google аккаунт в настройки устройства";
        } else if (exceptionMessage.contains("developer_error") || exceptionMessage.contains("invalid_client")) {
            Log.e(TAG, "Ошибка конфигурации Google OAuth");
            errorMessage = "Ошибка конфигурации приложения. Обратитесь к разработчику";
        } else {
            errorMessage = "Ошибка регистрации через Google: " + exceptionMessage;
        }

        showError(errorMessage);
        resetGoogleButton();
    }


    private void authenticateWithSupabase(String idToken, String email, String displayName) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Аутентификация в Supabase с Google ID Token для регистрации");


                GoogleAuthResult authResult = supabaseClient.signInWithGoogleExtended(idToken);

                JSONObject jsonResponse = new JSONObject(authResult.getResponseJson());
                String accessToken = jsonResponse.getString("access_token");
                String refreshToken = jsonResponse.optString("refresh_token", "");
                String userId = authResult.getUserId();
                boolean isNewUser = authResult.isNewUser();

                Log.d(TAG, "Google аутентификация для регистрации успешна. Новый пользователь: " + isNewUser);

                runOnUiThread(() -> {
                    try {
                        isGoogleSignInInProgress = false;

                        if (!isNewUser) {

                            Log.d(TAG, "Google пользователь уже зарегистрирован");
                            showError("Этот Google аккаунт уже зарегистрирован.\nПерейдите на экран входа для авторизации.");
                            resetGoogleButton();
                            return;
                        }


                        Log.d(TAG, "Новый Google пользователь - проверяем данные опроса");

                        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLogged", true)
                                .putString("accessToken", accessToken)
                                .putString("refreshToken", refreshToken)
                                .putString("userId", userId)
                                .putString("userEmail", email)
                                .putString("userName", displayName)
                                .putBoolean("isGoogleRegistration", true)
                                .apply();


                        SharedPreferences userDataPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
                        String name = userDataPrefs.getString("name", "");

                        if (!name.isEmpty()) {

                            Log.d(TAG, "Данные опроса найдены - создаем полный профиль и переходим в MainActivity");
                            createFullProfileAndFinish(userId, userDataPrefs, displayName);
                        } else {

                            Log.d(TAG, "Данных опроса нет - переход на SurveyActivity");

                            Toast.makeText(this, "Добро пожаловать в VitaMove! Давайте настроим ваш профиль", Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(this, SurveyActivity.class);
                            intent.putExtra("fromGoogleAuth", true);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка сохранения данных после Google регистрации: " + e.getMessage());
                        showError("Ошибка сохранения данных: " + e.getMessage());
                        resetGoogleButton();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка Google аутентификации в Supabase: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showError("Ошибка подключения к серверу: " + e.getMessage());
                    resetGoogleButton();
                });
            }
        }).start();
    }


    private void createFullProfileAndFinish(String userId, SharedPreferences userDataPrefs, String displayName) {

        String name = userDataPrefs.getString("name", displayName);
        int age = userDataPrefs.getInt("age", 30);
        String gender = userDataPrefs.getString("gender", "");
        String fitnessGoal = userDataPrefs.getString("fitness_goal", "weight_loss");
        float height = userDataPrefs.getFloat("height", 170);
        float currentWeight = userDataPrefs.getFloat("current_weight", 70);
        float targetWeight = userDataPrefs.getFloat("target_weight", 65);
        String fitnessLevel = userDataPrefs.getString("user_fitness_level", "intermediate");
        boolean isMetric = userDataPrefs.getBoolean("is_metric", true);

        Log.d(TAG, "Создание полного профиля с данными опроса: name=" + name + ", age=" + age +
                ", gender=" + gender + ", height=" + height + ", weight=" + currentWeight);


        new Thread(() -> {
            try {

                SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                String accessToken = prefs.getString("accessToken", null);
                String refreshToken = prefs.getString("refreshToken", null);

                if (accessToken != null && refreshToken != null) {
                    supabaseClient.setUserToken(accessToken);
                    supabaseClient.setRefreshToken(refreshToken);

                    boolean success = supabaseClient.updateUserProfile(
                            userId,
                            name,
                            age,
                            gender,
                            fitnessGoal,
                            height,
                            currentWeight,
                            targetWeight,
                            fitnessLevel,
                            isMetric
                    );

                    runOnUiThread(() -> {
                        if (success) {
                            Log.d(TAG, "Полный профиль Google пользователя успешно создан");


                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("isFirstRun", false);
                            editor.apply();


                            copyDataToMainPreferences(userDataPrefs);


                            addInitialWeightRecord();

                            Toast.makeText(this, "Профиль создан! Добро пожаловать в VitaMove!", Toast.LENGTH_SHORT).show();


                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e(TAG, "Не удалось создать полный профиль в Supabase");
                            showError("Ошибка создания профиля. Попробуйте еще раз.");
                            resetGoogleButton();
                        }
                    });
                } else {
                    Log.e(TAG, "Отсутствуют токены для создания профиля");
                    runOnUiThread(() -> {
                        showError("Ошибка аутентификации. Попробуйте еще раз.");
                        resetGoogleButton();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при создании полного профиля: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showError("Ошибка создания профиля: " + e.getMessage());
                    resetGoogleButton();
                });
            }
        }).start();
    }


    private void copyDataToMainPreferences(SharedPreferences userDataPrefs) {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor appEditor = appPrefs.edit();


        String fitnessGoal = userDataPrefs.getString("fitness_goal", "weight_loss");
        String fitnessLevel = userDataPrefs.getString("user_fitness_level", "intermediate");

        appEditor.putString("fitness_goal", fitnessGoal);
        appEditor.putString("user_fitness_level", fitnessLevel);
        appEditor.apply();

        Log.d(TAG, "Данные скопированы в основные настройки приложения");
    }


    private void resetGoogleButton() {
        isGoogleSignInInProgress = false;
        googleRegisterButton.setEnabled(true);
        googleRegisterButton.setText("Войти через Google");
    }
}
