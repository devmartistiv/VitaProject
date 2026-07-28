package com.martist.vitamove.nutrition.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.user.UserProfile;

public class CaloriesExplanationActivity extends BaseActivity {

    private static final String PREFS_USER_DATA = "user_data";
    private static final String TAG = "CaloriesExplanationActivity";

    private TextView currentCaloriesValue;
    private TextView calculatedCaloriesText;
    private TextView bmrValueText;
    private TextView activityFactorText;
    private TextView goalAdjustmentText;
    private TextView finalCaloriesText;

    private TextInputLayout customGoalLayout;
    private TextInputEditText customGoalInput;
    private MaterialButton setCustomGoalButton;
    private MaterialButton resetToCalculatedButton;


    private TextView currentProteinValue;
    private TextView currentFatsValue;
    private TextView currentCarbsValue;

    private TextInputLayout customProteinLayout;
    private TextInputLayout customFatsLayout;
    private TextInputLayout customCarbsLayout;

    private TextInputEditText customProteinInput;
    private TextInputEditText customFatsInput;
    private TextInputEditText customCarbsInput;

    private MaterialButton setMacrosButton;
    private MaterialButton resetMacrosButton;

    private UserProfile userProfile;
    private int calculatedCalories;
    private int calculatedProtein;
    private int calculatedFats;
    private int calculatedCarbs;
    private boolean isCustomGoalSet = false;
    private boolean isCustomMacrosSet = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calories_explanation);

        initViews();
        setupToolbar();
        loadUserData();
        calculateAndDisplayCalories();
        calculateAndDisplayMacros();
        setupCustomGoalFunctionality();
        setupCustomMacrosFunctionality();
    }

    private void initViews() {
        currentCaloriesValue = findViewById(R.id.currentCaloriesValue);
        calculatedCaloriesText = findViewById(R.id.calculatedCaloriesText);
        bmrValueText = findViewById(R.id.bmrValueText);
        activityFactorText = findViewById(R.id.activityFactorText);
        goalAdjustmentText = findViewById(R.id.goalAdjustmentText);
        finalCaloriesText = findViewById(R.id.finalCaloriesText);

        customGoalLayout = findViewById(R.id.customGoalLayout);
        customGoalInput = findViewById(R.id.customGoalInput);
        setCustomGoalButton = findViewById(R.id.setCustomGoalButton);
        resetToCalculatedButton = findViewById(R.id.resetToCalculatedButton);


        currentProteinValue = findViewById(R.id.currentProteinValue);
        currentFatsValue = findViewById(R.id.currentFatsValue);
        currentCarbsValue = findViewById(R.id.currentCarbsValue);

        customProteinLayout = findViewById(R.id.customProteinLayout);
        customFatsLayout = findViewById(R.id.customFatsLayout);
        customCarbsLayout = findViewById(R.id.customCarbsLayout);

        customProteinInput = findViewById(R.id.customProteinInput);
        customFatsInput = findViewById(R.id.customFatsInput);
        customCarbsInput = findViewById(R.id.customCarbsInput);

        setMacrosButton = findViewById(R.id.setMacrosButton);
        resetMacrosButton = findViewById(R.id.resetMacrosButton);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Дневная норма калорий");
        }

        toolbar.setNavigationOnClickListener(v -> this.getOnBackPressedDispatcher().onBackPressed());

    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);

        String name = prefs.getString("name", "Пользователь");
        int age = prefs.getInt("age", 30);
        String gender = prefs.getString("gender", "Мужчина");
        float currentWeight = prefs.getFloat("current_weight", 70f);
        float targetWeight = prefs.getFloat("target_weight", 65f);
        float height = prefs.getFloat("height", 170f);
        float bodyFat = prefs.getFloat("body_fat", 15f);
        float waist = prefs.getFloat("waist", 80f);

        userProfile = new UserProfile(name, age, gender, currentWeight, targetWeight, height, bodyFat, waist);


        int customCalories = prefs.getInt("custom_calories", 0);
        if (customCalories > 0) {
            isCustomGoalSet = true;
            currentCaloriesValue.setText(String.format("%d ккал", customCalories));
            customGoalInput.setText(String.valueOf(customCalories));
            resetToCalculatedButton.setVisibility(View.VISIBLE);
        }
    }

    private void calculateAndDisplayCalories() {

        calculatedCalories = userProfile.calculateTargetCalories();

        if (!isCustomGoalSet) {
            currentCaloriesValue.setText(String.format("%d ккал", calculatedCalories));
        }


        displayCalculationDetails();
    }

    private void displayCalculationDetails() {

        boolean isMale = userProfile.getGender().equalsIgnoreCase("Мужчина");
        float bmr;

        if (isMale) {
            bmr = 10 * userProfile.getCurrentWeight() + 6.25f * userProfile.getHeight() - 5 * userProfile.getAge() + 5;
        } else {
            bmr = 10 * userProfile.getCurrentWeight() + 6.25f * userProfile.getHeight() - 5 * userProfile.getAge() - 161;
        }

        bmrValueText.setText(String.format("%.0f ккал", bmr));


        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String activityLevel = appPrefs.getString("activity_level", "moderate");
        float activityFactor = getActivityFactor(activityLevel);

        String activityDescription = getActivityDescription(activityLevel);
        activityFactorText.setText(String.format("%s (коэффициент: %.1f)", activityDescription, activityFactor));


        String fitnessGoal = appPrefs.getString("fitness_goal", "weight_loss");
        String goalDescription = getGoalAdjustmentDescription(fitnessGoal);
        goalAdjustmentText.setText(goalDescription);


        finalCaloriesText.setText(String.format("Рассчитанная норма: %d ккал", calculatedCalories));

        calculatedCaloriesText.setText(String.format(
                "На основе ваших данных (возраст: %d лет, вес: %.1f кг, рост: %.0f см) " +
                        "ваша базовая потребность в энергии составляет %.0f ккал/день.",
                userProfile.getAge(), userProfile.getCurrentWeight(), userProfile.getHeight(), bmr
        ));
    }

    private float getActivityFactor(String activityLevel) {
        switch (activityLevel) {
            case "low":
                return 1.2f;
            case "light":
                return 1.375f;
            case "moderate":
                return 1.55f;
            case "high":
                return 1.725f;
            case "very_high":
                return 1.9f;
            default:
                return 1.55f;
        }
    }

    private String getActivityDescription(String activityLevel) {
        switch (activityLevel) {
            case "low":
                return "Низкая активность (сидячий образ жизни)";
            case "light":
                return "Легкая активность (1-3 тренировки в неделю)";
            case "moderate":
                return "Умеренная активность (3-5 тренировок в неделю)";
            case "high":
                return "Высокая активность (6-7 тренировок в неделю)";
            case "very_high":
                return "Очень высокая активность (2+ тренировки в день)";
            default:
                return "Умеренная активность";
        }
    }

    private String getGoalAdjustmentDescription(String fitnessGoal) {
        switch (fitnessGoal) {
            case "weight_loss":
                return "Снижение веса: -500 ккал (дефицит для потери ~0.5 кг/неделю)";
            case "muscle_gain":
                return "Набор мышечной массы: +500 ккал (профицит для роста мышц)";
            case "endurance":
                return "Тренировки на выносливость: +250 ккал (дополнительная энергия)";
            case "general_fitness":
            default:
                return "Общая физическая форма: без изменений (поддержание веса)";
        }
    }

    private void setupCustomGoalFunctionality() {

        setCustomGoalButton.setOnClickListener(v -> saveAllGoals());


        resetToCalculatedButton.setOnClickListener(v -> resetAllGoals());
    }

    private void saveAllGoals() {
        String caloriesText = customGoalInput.getText().toString().trim();
        String proteinText = customProteinInput.getText().toString().trim();
        String fatsText = customFatsInput.getText().toString().trim();
        String carbsText = customCarbsInput.getText().toString().trim();


        SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
        SharedPreferences dashboardPrefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);

        int currentCalories = prefs.getInt("target_calories", calculatedCalories);
        int currentProtein = dashboardPrefs.getInt("protein_goal", calculatedProtein);
        int currentFats = dashboardPrefs.getInt("fats_goal", calculatedFats);
        int currentCarbs = dashboardPrefs.getInt("carbs_goal", calculatedCarbs);


        Integer calories = null;
        Integer protein = null;
        Integer fats = null;
        Integer carbs = null;

        try {

            if (!caloriesText.isEmpty()) {
                calories = Integer.parseInt(caloriesText);
                if (calories < 1500) {
                    customGoalLayout.setError("Минимум: 1500 ккал");
                    return;
                }
                customGoalLayout.setError(null);
            }


            if (!proteinText.isEmpty()) {
                protein = Integer.parseInt(proteinText);
                if (protein < 30 || protein > 400) {
                    customProteinLayout.setError("30-400 г");
                    return;
                }
                customProteinLayout.setError(null);
            }


            if (!fatsText.isEmpty()) {
                fats = Integer.parseInt(fatsText);
                if (fats < 20 || fats > 200) {
                    customFatsLayout.setError("20-200 г");
                    return;
                }
                customFatsLayout.setError(null);
            }


            if (!carbsText.isEmpty()) {
                carbs = Integer.parseInt(carbsText);
                if (carbs < 50 || carbs > 600) {
                    customCarbsLayout.setError("50-600 г");
                    return;
                }
                customCarbsLayout.setError(null);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Введите корректные числа", Toast.LENGTH_SHORT).show();
            return;
        }


        final int finalCalories = calories != null ? calories : currentCalories;
        final int finalProtein = protein != null ? protein : currentProtein;
        final int finalFats = fats != null ? fats : currentFats;
        final int finalCarbs = carbs != null ? carbs : currentCarbs;


        final boolean caloriesChanged = calories != null;
        final boolean macrosChanged = protein != null || fats != null || carbs != null;
        boolean hasChanges = caloriesChanged || macrosChanged;

        if (!hasChanges) {
            Toast.makeText(this, "Нет изменений для сохранения", Toast.LENGTH_SHORT).show();
            return;
        }


        StringBuilder message = new StringBuilder("Новые цели:\n\n");

        if (caloriesChanged) {
            message.append(String.format("✓ Калории: %d ккал (было: %d)\n", finalCalories, currentCalories));
        }
        if (protein != null) {
            message.append(String.format("✓ Белки: %d г (было: %d)\n", finalProtein, currentProtein));
        }
        if (fats != null) {
            message.append(String.format("✓ Жиры: %d г (было: %d)\n", finalFats, currentFats));
        }
        if (carbs != null) {
            message.append(String.format("✓ Углеводы: %d г (было: %d)\n", finalCarbs, currentCarbs));
        }

        int totalCaloriesFromMacros = (finalProtein * 4) + (finalFats * 9) + (finalCarbs * 4);
        message.append(String.format("\nИтого из БЖУ: %d ккал", totalCaloriesFromMacros));


        new MaterialAlertDialogBuilder(this)
                .setTitle("Сохранить изменения?")
                .setMessage(message.toString())
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    applyAllGoals(finalCalories, finalProtein, finalFats, finalCarbs,
                            caloriesChanged, macrosChanged);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void applyAllGoals(int calories, int protein, int fats, int carbs,
                               boolean caloriesChanged, boolean macrosChanged) {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
        SharedPreferences dashboardPrefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);


        if (caloriesChanged) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("custom_calories", calories);
            editor.putInt("target_calories", calories);
            editor.apply();

            isCustomGoalSet = true;
            currentCaloriesValue.setText(String.format("%d ккал", calories));

            FoodManager foodManager = FoodManager.getInstance(this);
            foodManager.updateTargetCalories(calories);
        }


        if (macrosChanged) {
            SharedPreferences.Editor dashboardEditor = dashboardPrefs.edit();
            dashboardEditor.putInt("protein_goal", protein);
            dashboardEditor.putInt("fats_goal", fats);
            dashboardEditor.putInt("carbs_goal", carbs);
            dashboardEditor.apply();

            SharedPreferences foodPrefs = getSharedPreferences("FoodManagerPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor foodEditor = foodPrefs.edit();
            foodEditor.putFloat("proteins_norm", protein);
            foodEditor.putFloat("fats_norm", fats);
            foodEditor.putFloat("carbs_norm", carbs);
            foodEditor.apply();

            try {
                DashboardManager dashboardManager = DashboardManager.getInstance(this);
                dashboardManager.updateMacroGoals(protein, fats, carbs);
            } catch (Exception e) {
                Log.w(TAG, "Не удалось обновить DashboardManager: " + e.getMessage());
            }

            try {
                FoodManager foodManager = FoodManager.getInstance(this);
                foodManager.updateCustomMacroGoals(protein, fats, carbs);
            } catch (Exception e) {
                Log.w(TAG, "Не удалось обновить FoodManager: " + e.getMessage());
            }

            isCustomMacrosSet = true;
            currentProteinValue.setText(String.format("%d г", protein));
            currentFatsValue.setText(String.format("%d г", fats));
            currentCarbsValue.setText(String.format("%d г", carbs));
        }


        if (isCustomGoalSet || isCustomMacrosSet) {
            resetToCalculatedButton.setVisibility(View.VISIBLE);
        }


        customGoalInput.setText("");
        customProteinInput.setText("");
        customFatsInput.setText("");
        customCarbsInput.setText("");


        android.content.Intent intent = new android.content.Intent("com.martist.vitamove.UPDATE_DASHBOARD");
        intent.putExtra("update_source", "goals_changed");
        sendBroadcast(intent);

        Toast.makeText(this, "Цели успешно сохранены", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Сохранены цели: " + calories + " ккал, Б=" + protein + "г, Ж=" + fats + "г, У=" + carbs + "г");
    }

    private void saveCustomGoal(int customCalories) {

        SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
        SharedPreferences dashboardPrefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("custom_calories", customCalories);
        editor.putInt("target_calories", customCalories);
        editor.apply();

        isCustomGoalSet = true;
        currentCaloriesValue.setText(String.format("%d ккал", customCalories));
        resetToCalculatedButton.setVisibility(View.VISIBLE);
        customGoalLayout.setError(null);


        int customProtein = dashboardPrefs.getInt("protein_goal", 0);
        int customFats = dashboardPrefs.getInt("fats_goal", 0);
        int customCarbs = dashboardPrefs.getInt("carbs_goal", 0);
        boolean hasCustomMacros = (customProtein > 0 && customFats > 0 && customCarbs > 0);


        FoodManager foodManager = FoodManager.getInstance(this);
        foodManager.updateTargetCalories(customCalories);


        if (!hasCustomMacros) {
            calculateAndDisplayMacros();
            Log.d(TAG, "БЖУ автоматически пересчитаны для новых калорий: " + customCalories + " ккал");
        } else {

            Toast.makeText(this, "Собственная цель установлена. БЖУ остались без изменений.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Калории изменены, но БЖУ остались кастомными");
        }


        android.content.Intent intent = new android.content.Intent("com.martist.vitamove.UPDATE_DASHBOARD");
        intent.putExtra("update_source", "calories_goal_changed");
        sendBroadcast(intent);
        Log.d(TAG, "Отправлен broadcast для обновления CaloriesFragment");

        Toast.makeText(this, "Собственная цель установлена", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Установлена пользовательская цель: " + customCalories + " ккал");
    }

    private void resetAllGoals() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Сбросить все цели?")
                .setMessage(String.format(
                        "Вернуться к автоматически рассчитанным значениям?\n\n" +
                                "Калории: %d ккал\n" +
                                "Белки: %d г\n" +
                                "Жиры: %d г\n" +
                                "Углеводы: %d г",
                        calculatedCalories, calculatedProtein, calculatedFats, calculatedCarbs
                ))
                .setPositiveButton("Сбросить", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
                    SharedPreferences dashboardPrefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);


                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("custom_calories");
                    editor.putInt("target_calories", calculatedCalories);
                    editor.apply();


                    SharedPreferences.Editor dashboardEditor = dashboardPrefs.edit();
                    dashboardEditor.remove("protein_goal");
                    dashboardEditor.remove("fats_goal");
                    dashboardEditor.remove("carbs_goal");
                    dashboardEditor.apply();

                    SharedPreferences foodPrefs = getSharedPreferences("FoodManagerPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor foodEditor = foodPrefs.edit();
                    foodEditor.remove("proteins_norm");
                    foodEditor.remove("fats_norm");
                    foodEditor.remove("carbs_norm");
                    foodEditor.apply();


                    try {
                        FoodManager foodManager = FoodManager.getInstance(this);
                        foodManager.updateTargetCalories(calculatedCalories);
                        foodManager.refreshNutrientNorms();
                    } catch (Exception e) {
                        Log.w(TAG, "Не удалось обновить FoodManager: " + e.getMessage());
                    }


                    isCustomGoalSet = false;
                    isCustomMacrosSet = false;

                    currentCaloriesValue.setText(String.format("%d ккал", calculatedCalories));
                    currentProteinValue.setText(String.format("%d г", calculatedProtein));
                    currentFatsValue.setText(String.format("%d г", calculatedFats));
                    currentCarbsValue.setText(String.format("%d г", calculatedCarbs));

                    resetToCalculatedButton.setVisibility(View.GONE);

                    customGoalInput.setText("");
                    customProteinInput.setText("");
                    customFatsInput.setText("");
                    customCarbsInput.setText("");


                    android.content.Intent intent = new android.content.Intent("com.martist.vitamove.UPDATE_DASHBOARD");
                    intent.putExtra("update_source", "all_goals_reset");
                    sendBroadcast(intent);

                    Toast.makeText(this, "Все цели сброшены к рассчитанным значениям", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Все цели сброшены к рассчитанным значениям");
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void calculateAndDisplayMacros() {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, Context.MODE_PRIVATE);
        SharedPreferences dashboardPrefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);


        FoodManager foodManager = FoodManager.getInstance(this);


        int targetCals = prefs.getInt("target_calories", calculatedCalories);


        calculatedProtein = Math.round(foodManager.getDailyNorm("proteins"));
        calculatedFats = Math.round(foodManager.getDailyNorm("fats"));
        calculatedCarbs = Math.round(foodManager.getDailyNorm("carbs"));


        int customProtein = dashboardPrefs.getInt("protein_goal", 0);
        int customFats = dashboardPrefs.getInt("fats_goal", 0);
        int customCarbs = dashboardPrefs.getInt("carbs_goal", 0);

        if (customProtein > 0 && customFats > 0 && customCarbs > 0) {
            isCustomMacrosSet = true;
            currentProteinValue.setText(String.format("%d г", customProtein));
            currentFatsValue.setText(String.format("%d г", customFats));
            currentCarbsValue.setText(String.format("%d г", customCarbs));

            customProteinInput.setText(String.valueOf(customProtein));
            customFatsInput.setText(String.valueOf(customFats));
            customCarbsInput.setText(String.valueOf(customCarbs));

            resetMacrosButton.setVisibility(View.VISIBLE);
        } else {

            currentProteinValue.setText(String.format("%d г", calculatedProtein));
            currentFatsValue.setText(String.format("%d г", calculatedFats));
            currentCarbsValue.setText(String.format("%d г", calculatedCarbs));
        }

        Log.d(TAG, "БЖУ получены из FoodManager: Б=" + calculatedProtein + "г, Ж=" + calculatedFats + "г, У=" + calculatedCarbs + "г");
    }

    private void setupCustomMacrosFunctionality() {


    }

    private void setCustomMacros() {
        String proteinText = customProteinInput.getText().toString().trim();
        String fatsText = customFatsInput.getText().toString().trim();
        String carbsText = customCarbsInput.getText().toString().trim();

        try {

            SharedPreferences dashboardPrefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);
            int currentProtein = dashboardPrefs.getInt("protein_goal", calculatedProtein);
            int currentFats = dashboardPrefs.getInt("fats_goal", calculatedFats);
            int currentCarbs = dashboardPrefs.getInt("carbs_goal", calculatedCarbs);


            Integer protein = null;
            Integer fats = null;
            Integer carbs = null;

            if (!proteinText.isEmpty()) {
                protein = Integer.parseInt(proteinText);
                if (protein < 30 || protein > 400) {
                    customProteinLayout.setError("Белки: от 30 до 400 г");
                    return;
                }
                customProteinLayout.setError(null);
            }

            if (!fatsText.isEmpty()) {
                fats = Integer.parseInt(fatsText);
                if (fats < 20 || fats > 200) {
                    customFatsLayout.setError("Жиры: от 20 до 200 г");
                    return;
                }
                customFatsLayout.setError(null);
            }

            if (!carbsText.isEmpty()) {
                carbs = Integer.parseInt(carbsText);
                if (carbs < 50 || carbs > 600) {
                    customCarbsLayout.setError("Углеводы: от 50 до 600 г");
                    return;
                }
                customCarbsLayout.setError(null);
            }


            int finalProtein = protein != null ? protein : currentProtein;
            int finalFats = fats != null ? fats : currentFats;
            int finalCarbs = carbs != null ? carbs : currentCarbs;


            int totalCalories = (finalProtein * 4) + (finalFats * 9) + (finalCarbs * 4);


            StringBuilder message = new StringBuilder();
            boolean hasChanges = protein != null || fats != null || carbs != null;

            if (hasChanges) {
                message.append("Новые цели:\n\n");
            } else {
                message.append("Будут установлены текущие значения:\n\n");
            }

            if (protein != null) {
                message.append(String.format("✓ Белки: %d г (%d ккал) — изменено\n", finalProtein, finalProtein * 4));
            } else {
                message.append(String.format("Белки: %d г (%d ккал)\n", finalProtein, finalProtein * 4));
            }

            if (fats != null) {
                message.append(String.format("✓ Жиры: %d г (%d ккал) — изменено\n", finalFats, finalFats * 9));
            } else {
                message.append(String.format("Жиры: %d г (%d ккал)\n", finalFats, finalFats * 9));
            }

            if (carbs != null) {
                message.append(String.format("✓ Углеводы: %d г (%d ккал) — изменено\n", finalCarbs, finalCarbs * 4));
            } else {
                message.append(String.format("Углеводы: %d г (%d ккал)\n", finalCarbs, finalCarbs * 4));
            }

            message.append(String.format("\nИтого: %d ккал", totalCalories));


            new MaterialAlertDialogBuilder(this)
                    .setTitle("Установить цели по БЖУ?")
                    .setMessage(message.toString())
                    .setPositiveButton("Установить", (dialog, which) -> {
                        saveCustomMacros(finalProtein, finalFats, finalCarbs);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Введите корректные числа", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCustomMacros(int protein, int fats, int carbs) {

        SharedPreferences dashboardPrefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor dashboardEditor = dashboardPrefs.edit();
        dashboardEditor.putInt("protein_goal", protein);
        dashboardEditor.putInt("fats_goal", fats);
        dashboardEditor.putInt("carbs_goal", carbs);
        dashboardEditor.apply();


        SharedPreferences foodPrefs = getSharedPreferences("FoodManagerPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor foodEditor = foodPrefs.edit();
        foodEditor.putFloat("proteins_norm", protein);
        foodEditor.putFloat("fats_norm", fats);
        foodEditor.putFloat("carbs_norm", carbs);
        foodEditor.apply();


        try {
            DashboardManager dashboardManager = DashboardManager.getInstance(this);
            dashboardManager.updateMacroGoals(protein, fats, carbs);
        } catch (Exception e) {
            Log.w(TAG, "Не удалось обновить DashboardManager: " + e.getMessage());
        }


        try {
            FoodManager foodManager = FoodManager.getInstance(this);
            foodManager.updateCustomMacroGoals(protein, fats, carbs);
            Log.d(TAG, "FoodManager обновлен с новыми БЖУ целями");
        } catch (Exception e) {
            Log.w(TAG, "Не удалось обновить FoodManager: " + e.getMessage());
        }

        isCustomMacrosSet = true;
        currentProteinValue.setText(String.format("%d г", protein));
        currentFatsValue.setText(String.format("%d г", fats));
        currentCarbsValue.setText(String.format("%d г", carbs));

        resetMacrosButton.setVisibility(View.VISIBLE);


        customProteinLayout.setError(null);
        customFatsLayout.setError(null);
        customCarbsLayout.setError(null);

        Toast.makeText(this, "Цели по БЖУ установлены", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Установлены пользовательские цели БЖУ: Б=" + protein + "г, Ж=" + fats + "г, У=" + carbs + "г");
    }

    private void resetMacros() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Сбросить к рассчитанным значениям?")
                .setMessage(String.format(
                        "Вернуться к автоматически рассчитанным значениям?\n\n" +
                                "Белки: %d г\n" +
                                "Жиры: %d г\n" +
                                "Углеводы: %d г",
                        calculatedProtein, calculatedFats, calculatedCarbs
                ))
                .setPositiveButton("Сбросить", (dialog, which) -> {

                    SharedPreferences dashboardPrefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor dashboardEditor = dashboardPrefs.edit();
                    dashboardEditor.remove("protein_goal");
                    dashboardEditor.remove("fats_goal");
                    dashboardEditor.remove("carbs_goal");
                    dashboardEditor.apply();


                    SharedPreferences foodPrefs = getSharedPreferences("FoodManagerPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor foodEditor = foodPrefs.edit();
                    foodEditor.remove("proteins_norm");
                    foodEditor.remove("fats_norm");
                    foodEditor.remove("carbs_norm");
                    foodEditor.apply();


                    try {
                        FoodManager foodManager = FoodManager.getInstance(this);
                        foodManager.refreshNutrientNorms();
                        Log.d(TAG, "FoodManager пересчитал БЖУ нормы");
                    } catch (Exception e) {
                        Log.w(TAG, "Не удалось обновить FoodManager: " + e.getMessage());
                    }

                    isCustomMacrosSet = false;
                    currentProteinValue.setText(String.format("%d г", calculatedProtein));
                    currentFatsValue.setText(String.format("%d г", calculatedFats));
                    currentCarbsValue.setText(String.format("%d г", calculatedCarbs));

                    resetMacrosButton.setVisibility(View.GONE);

                    customProteinInput.setText("");
                    customFatsInput.setText("");
                    customCarbsInput.setText("");

                    Toast.makeText(this, "Возвращено к рассчитанным значениям БЖУ", Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "Сброшены пользовательские цели БЖУ");
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
