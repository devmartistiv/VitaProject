package com.martist.vitamove.user;

import static android.content.Context.MODE_PRIVATE;
import static com.martist.vitamove.VitaMoveApplication.context;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.auth.OnboardingActivity;
import com.martist.vitamove.core.data.managers.NotificationManager;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.BMICalculator;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.nutrition.data.managers.CaloriesManager;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);


        initPreferences();


        Preference editProfilePref = findPreference("edit_profile");
        if (editProfilePref != null) {
            editProfilePref.setOnPreferenceClickListener(preference -> {
                showEditProfileDialog();
                return true;
            });
        }

        Preference deleteAccountPref = findPreference("delete_account");
        if (deleteAccountPref != null) {
            deleteAccountPref.setOnPreferenceClickListener(preference -> {
                showDeleteAccountDialog();
                return true;
            });
        }


        setupTimePreferences();
    }


    private void setupTimePreferences() {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();


        Preference dinnerTimePref = findPreference("dinner_time");
        if (dinnerTimePref != null) {
            dinnerTimePref.setOnPreferenceClickListener(preference -> {
                showTimePickerDialog("dinner_time", "Выберите время напоминания об ужине", 19, 30);
                return true;
            });

            int dinnerHour = prefs.getInt("dinner_time_hour", 19);
            int dinnerMinute = prefs.getInt("dinner_time_minute", 30);
            updateTimeSummary("dinner_time", dinnerHour, dinnerMinute);
        }


        Preference waterTimePref = findPreference("water_time");
        if (waterTimePref != null) {
            waterTimePref.setOnPreferenceClickListener(preference -> {
                showTimePickerDialog("water_time", "Выберите время напоминания о воде", 15, 0);
                return true;
            });

            int waterHour = prefs.getInt("water_time_hour", 15);
            int waterMinute = prefs.getInt("water_time_minute", 0);
            updateTimeSummary("water_time", waterHour, waterMinute);
        }


        Preference workoutTimePref = findPreference("workout_time");
        if (workoutTimePref != null) {
            workoutTimePref.setOnPreferenceClickListener(preference -> {
                showTimePickerDialog("workout_time", "Выберите время напоминания о тренировке", 10, 0);
                return true;
            });

            int workoutHour = prefs.getInt("workout_time_hour", 10);
            int workoutMinute = prefs.getInt("workout_time_minute", 0);
            updateTimeSummary("workout_time", workoutHour, workoutMinute);
        }
    }


    private void showTimePickerDialog(String preferenceKey, String title, int defaultHour, int defaultMinute) {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();


        int hour = prefs.getInt(preferenceKey + "_hour", defaultHour);
        int minute = prefs.getInt(preferenceKey + "_minute", defaultMinute);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> {

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(preferenceKey + "_hour", selectedHour);
                    editor.putInt(preferenceKey + "_minute", selectedMinute);
                    editor.apply();


                    updateTimeSummary(preferenceKey, selectedHour, selectedMinute);


                    NotificationManager notificationManager = NotificationManager.getInstance(requireContext());
                    notificationManager.scheduleNotifications();

                    Toast.makeText(requireContext(),
                            "Время уведомления обновлено: " + formatTime(selectedHour, selectedMinute),
                            Toast.LENGTH_SHORT).show();
                },
                hour,
                minute,
                true
        );

        timePickerDialog.setTitle(title);
        timePickerDialog.show();
    }


    private void updateTimeSummary(String preferenceKey, int hour, int minute) {
        Preference preference = findPreference(preferenceKey);
        if (preference != null) {
            preference.setSummary(formatTime(hour, minute));
        }
    }


    private String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }


    private void initPreferences() {

        PreferenceManager.setDefaultValues(requireContext(), R.xml.preferences, false);


        updatePreferenceSummaries();
    }


    private void updatePreferenceSummaries() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();


        ListPreference goalPref = findPreference("fitness_goal");
        if (goalPref != null) {
            String value = sharedPreferences.getString("fitness_goal", "weight_loss");
            switch (value) {
                case "weight_loss":
                    goalPref.setSummary("Снижение веса");
                    break;
                case "muscle_gain":
                    goalPref.setSummary("Набор мышечной массы");
                    break;
                case "endurance":
                    goalPref.setSummary("Улучшение выносливости");
                    break;
                default:
                    goalPref.setSummary("Общая физическая подготовка");
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    public void unregisterListener() {
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        Log.d("SettingsFragment", "Слушатель SharedPreferences принудительно отписан.");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        updatePreferenceSummaries();


        if (key.equals("notifications_enabled")) {
            boolean notificationsEnabled = sharedPreferences.getBoolean(key, true);


            NotificationManager notificationManager = NotificationManager.getInstance(requireContext());
            notificationManager.setNotificationsEnabled(notificationsEnabled);


            Toast.makeText(requireContext(),
                    notificationsEnabled ? "Уведомления включены" : "Уведомления отключены",
                    Toast.LENGTH_SHORT).show();
        } else if (key.equals("dinner_notifications")) {
            boolean enabled = sharedPreferences.getBoolean(key, true);
            NotificationManager notificationManager = NotificationManager.getInstance(requireContext());
            notificationManager.setDinnerNotificationsEnabled(enabled);
            Toast.makeText(requireContext(),
                    enabled ? "Напоминания об ужине включены" : "Напоминания об ужине отключены",
                    Toast.LENGTH_SHORT).show();
        } else if (key.equals("water_notifications")) {
            boolean enabled = sharedPreferences.getBoolean(key, true);
            NotificationManager notificationManager = NotificationManager.getInstance(requireContext());
            notificationManager.setWaterNotificationsEnabled(enabled);
            Toast.makeText(requireContext(),
                    enabled ? "Напоминания о воде включены" : "Напоминания о воде отключены",
                    Toast.LENGTH_SHORT).show();
        } else if (key.equals("workout_notifications")) {
            boolean enabled = sharedPreferences.getBoolean(key, true);
            NotificationManager notificationManager = NotificationManager.getInstance(requireContext());
            notificationManager.setWorkoutNotificationsEnabled(enabled);
            Toast.makeText(requireContext(),
                    enabled ? "Напоминания о тренировках включены" : "Напоминания о тренировках отключены",
                    Toast.LENGTH_SHORT).show();
        } else if (key.equals("dark_mode")) {
            String darkModeValue = sharedPreferences.getString(key, "system");
            applyDarkMode(darkModeValue);

            Toast.makeText(requireContext(), "Тема изменена", Toast.LENGTH_SHORT).show();
        } else if (key.equals("fitness_goal")) {
            String fitnessGoal = sharedPreferences.getString(key, "weight_loss");


            FoodManager foodManager = FoodManager.getInstance(requireContext());
            foodManager.updateFitnessGoal(fitnessGoal);


            SharedPreferences userPrefs = requireContext().getSharedPreferences("user_data", MODE_PRIVATE);
            userPrefs.edit().putString("fitness_goal", fitnessGoal).apply();


            UserRepository userRepository =
                    new UserRepository(requireContext());
            UserProfile userProfile = userRepository.getCurrentUserProfile();

            if (userProfile != null) {

                userProfile.updateTargetCalories();
                userProfile.updateTargetWater();


                int updatedTargetCalories = userProfile.getTargetCalories();
                float updatedTargetWater = userProfile.getTargetWater();

                SharedPreferences.Editor editor = userPrefs.edit();
                editor.putInt("target_calories", updatedTargetCalories);
                editor.putFloat("target_water", updatedTargetWater);
                editor.apply();


                DashboardManager dashboardManager =
                        DashboardManager.getInstance(requireContext());
                dashboardManager.updateCaloriesGoalFromProfile();
                dashboardManager.updateWaterGoalFromProfile();

                Log.d("SettingsActivity", "Обновлены целевые значения: калории=" + updatedTargetCalories +
                        ", вода=" + updatedTargetWater + " л для цели: " + fitnessGoal);


                if (getActivity() != null) {

                    if (getActivity() instanceof MainActivity) {

                        ((MainActivity) getActivity()).updateNavigationHeader();
                    }
                }

                String fitnessLevel = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("user_fitness_level", "intermediate");
                syncUpdatedDataWithSupabase(userProfile, fitnessGoal, fitnessLevel);
            }


            String goalMessage;
            switch (fitnessGoal) {
                case "weight_loss":
                    goalMessage = "Цель изменена: снижение веса. Обновлены нормы калорий и воды.";
                    break;
                case "muscle_gain":
                    goalMessage = "Цель изменена: набор мышечной массы. Обновлены нормы калорий и воды.";
                    break;
                case "endurance":
                    goalMessage = "Цель изменена: повышение выносливости. Обновлены нормы калорий и воды.";
                    break;
                default:
                    goalMessage = "Цель изменена: общий фитнес. Обновлены нормы калорий и воды.";
                    break;
            }
            Toast.makeText(requireContext(), goalMessage, Toast.LENGTH_LONG).show();
        }
    }


    private void applyDarkMode(String darkModeValue) {
        int nightMode;
        switch (darkModeValue) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "system":
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }


    private void showDeleteAccountDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить свой аккаунт? Это действие нельзя отменить. Все ваши данные будут удалены.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteAccount();
                })
                .setNegativeButton("Отмена", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void deleteAccount() {
        Context context = requireContext();


        Toast.makeText(context, "Удаление аккаунта...", Toast.LENGTH_SHORT).show();

        try {

            SharedPreferences appPrefs = context.getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
            String userId = appPrefs.getString("userId", null);

            if (userId != null) {

                SupabaseClient supabaseClient =
                        SupabaseClient.getInstance(
                                Constants.SUPABASE_CLIENT_ID,
                                Constants.SUPABASE_CLIENT_SECRET
                        );


                Log.d("SettingsActivity", "Запрос на удаление аккаунта пользователя: " + userId);

                supabaseClient.deleteUserAccount(userId, new SupabaseClient.AsyncCallback() {
                    @Override
                    public void onSuccess(String responseBody) {

                        Log.d("SettingsActivity", "Аккаунт успешно удален на сервере: " + responseBody);


                        clearLocalUserData(context);


                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(context, "Аккаунт успешно удален", Toast.LENGTH_LONG).show();


                                Intent intent = new Intent(context, OnboardingActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);


                                if (getActivity() != null) {
                                    getActivity().finish();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {

                        Log.e("SettingsActivity", "Ошибка при удалении аккаунта: " + e.getMessage(), e);


                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(context, "Произошла ошибка при удалении аккаунта: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
            } else {
                Toast.makeText(context, "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при удалении аккаунта: " + e.getMessage(), e);
            Toast.makeText(context, "Произошла ошибка при удалении аккаунта", Toast.LENGTH_SHORT).show();
        }
    }


    private void clearLocalUserData(Context context) {
        try {

            context.getSharedPreferences("user_data", MODE_PRIVATE).edit().clear().apply();
            context.getSharedPreferences("VitaMovePrefs", MODE_PRIVATE).edit().clear().apply();
            context.getSharedPreferences("workout_history_cache", MODE_PRIVATE).edit().clear().apply();


            FoodManager.resetInstance();
            CaloriesManager.resetInstance();
            DashboardManager.resetInstance();

            Log.d("SettingsActivity", "Локальные данные пользователя успешно очищены");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке локальных данных: " + e.getMessage(), e);
        }
    }


    private void syncUpdatedDataWithSupabase(UserProfile userProfile, String fitnessGoal, String fitnessLevel) {
        try {

            SharedPreferences appPrefs = requireContext().getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);

            String userId = appPrefs.getString("userId", null);
            boolean isMetric = appPrefs.getBoolean("use_metric", true);

            if (userId == null || userId.isEmpty()) {
                Log.w("SettingsFragment", "Невозможно синхронизировать данные с Supabase - userId отсутствует");
                return;
            }


            SupabaseClient supabaseClient = SupabaseClient.getInstance(
                    Constants.SUPABASE_CLIENT_ID,
                    Constants.SUPABASE_CLIENT_SECRET
            );


            String accessToken = appPrefs.getString("accessToken", null);
            String refreshToken = appPrefs.getString("refreshToken", null);

            if (accessToken == null || refreshToken == null) {
                Log.w("SettingsFragment", "Токены авторизации отсутствуют, синхронизация невозможна");
                return;
            }

            supabaseClient.setUserToken(accessToken);
            supabaseClient.setRefreshToken(refreshToken);


            new Thread(() -> {
                try {

                    boolean success = supabaseClient.updateUserProfile(
                            userId,
                            userProfile.getName(),
                            userProfile.getAge(),
                            userProfile.getGender(),
                            fitnessGoal,
                            userProfile.getHeight(),
                            userProfile.getCurrentWeight(),
                            userProfile.getTargetWeight(),
                            fitnessLevel,
                            isMetric
                    );

                    if (success) {
                        Log.d("SettingsFragment", "Данные успешно синхронизированы с Supabase после изменения цели тренировки");
                    } else {
                        Log.e("SettingsFragment", "Не удалось синхронизировать данные с Supabase");
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Ошибка при синхронизации данных с Supabase: " + e.getMessage(), e);
                }
            }).start();
        } catch (Exception e) {
            Log.e("SettingsFragment", "Ошибка при подготовке синхронизации: " + e.getMessage(), e);
        }
    }


    private void showEditProfileDialog() {
        Context context = requireContext();


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);


        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        TextInputEditText ageInput = dialogView.findViewById(R.id.ageInput);
        AutoCompleteTextView genderInput = dialogView.findViewById(R.id.genderDropdown);


        String[] genders = new String[]{"Мужчина", "Женщина"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, genders);
        genderInput.setAdapter(adapter);


        loadCurrentProfileData(nameInput, ageInput, genderInput);

        AlertDialog dialog = builder.create();


        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.saveButton).setOnClickListener(v -> {
            if (validateDialogInputs(nameInput, ageInput, genderInput)) {
                saveProfileDataFromDialog(nameInput, ageInput, genderInput);
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    private void loadCurrentProfileData(TextInputEditText nameInput, TextInputEditText ageInput, AutoCompleteTextView genderInput) {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_data", MODE_PRIVATE);

        String name = prefs.getString("name", "");
        int age = prefs.getInt("age", 30);
        String gender = prefs.getString("gender", "Женщина");

        nameInput.setText(name);
        ageInput.setText(String.valueOf(age));
        genderInput.setText(gender, false);
    }


    private boolean validateDialogInputs(TextInputEditText nameInput, TextInputEditText ageInput, AutoCompleteTextView genderInput) {
        boolean isValid = true;


        if (nameInput.getText().toString().trim().isEmpty()) {
            nameInput.setError("Введите имя");
            isValid = false;
        }


        try {
            int age = Integer.parseInt(ageInput.getText().toString());
            if (age <= 0 || age > 120) {
                ageInput.setError("Введите корректный возраст");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            ageInput.setError("Введите числовое значение");
            isValid = false;
        }


        if (genderInput.getText().toString().trim().isEmpty()) {
            genderInput.setError("Выберите пол");
            isValid = false;
        }

        return isValid;
    }


    private void saveProfileDataFromDialog(TextInputEditText nameInput, TextInputEditText ageInput, AutoCompleteTextView genderInput) {
        Context context = requireContext();

        String name = nameInput.getText().toString().trim();
        int age = Integer.parseInt(ageInput.getText().toString());
        String gender = genderInput.getText().toString();


        SharedPreferences prefs = context.getSharedPreferences("user_data", MODE_PRIVATE);
        float currentWeight = prefs.getFloat("current_weight", 70.0f);
        float targetWeight = prefs.getFloat("target_weight", 60.0f);
        float height = prefs.getFloat("height", 170.0f);
        float bodyFat = prefs.getFloat("body_fat", 25.0f);
        float waist = prefs.getFloat("waist", 75.0f);


        UserProfile userProfile = new UserProfile(name, age, gender, currentWeight, targetWeight, height, bodyFat, waist);


        userProfile.updateTargetCalories();
        userProfile.updateTargetWater();


        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", name);
        editor.putInt("age", age);
        editor.putString("gender", gender);
        editor.putInt("target_calories", userProfile.getTargetCalories());
        editor.putFloat("target_water", userProfile.getTargetWater());


        float bmi = BMICalculator.calculateBMI(currentWeight, height);
        editor.putFloat("bmi", bmi);


        editor.apply();


        SharedPreferences appPrefs = context.getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        boolean isMetric = appPrefs.getBoolean("use_metric", true);
        String userId = appPrefs.getString("userId", null);


        String fitnessGoal = prefs.getString("fitness_goal",
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("fitness_goal", "weight_loss"));

        String fitnessLevel = prefs.getString("fitness_level",
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("user_fitness_level", "intermediate"));

        Log.d("SettingsFragment", "Сохранение профиля: имя=" + name + ", возраст=" + age
                + ", пол=" + gender + ", калории=" + userProfile.getTargetCalories()
                + ", вода=" + userProfile.getTargetWater() + " л");


        if (userId != null) {

            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Обновление профиля...");
            progressDialog.setCancelable(false);
            progressDialog.show();


            SupabaseClient supabaseClient = SupabaseClient.getInstance(
                    Constants.SUPABASE_CLIENT_ID,
                    Constants.SUPABASE_CLIENT_SECRET
            );


            new Thread(() -> {
                try {
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


                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {

                            progressDialog.dismiss();

                            if (success) {
                                Toast.makeText(context, "Профиль успешно обновлен", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Профиль сохранен локально", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Ошибка при обновлении профиля пользователя: " + e.getMessage(), e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(context, "Профиль сохранен локально", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).start();
        } else {
            Log.w("SettingsFragment", "ID пользователя не найден, данные не синхронизированы с Supabase");
            Toast.makeText(context, "Профиль обновлен", Toast.LENGTH_SHORT).show();
        }
    }
}