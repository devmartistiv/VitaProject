package com.martist.vitamove.nutrition.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.managers.FavoriteManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.data.managers.NutritionDisplayManager;
import com.martist.vitamove.nutrition.data.repository.SupabaseBarcodeRepository;
import com.martist.vitamove.nutrition.data.repository.SupabaseFoodRepository;
import com.martist.vitamove.nutrition.domain.events.FoodAddedEvent;
import com.martist.vitamove.nutrition.ui.model.Dish;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Meal;
import com.martist.vitamove.nutrition.ui.model.Portion;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PortionSizeActivity extends BaseActivity {
    private static final String TAG = "PortionSizeActivity";
    private FoodManager foodManager;
    private Food selectedFood;
    private Dish selectedDish;
    private String mealType;
    private String barcode;
    private boolean isIngredientSelection = false;
    private EditText portionSizeInput;
    private TextView caloriesText;
    private AutoCompleteTextView portionSpinner;
    private TextInputLayout portionUnitInputLayout;
    private String selectedPortionName;

    private TextView foodCategoryText;
    private TextView proteinsValue;
    private TextView fatsValue;
    private TextView carbsValue;
    private TextInputLayout portionSizeInputLayout;

    private TextView usefulnessIndexValue;
    private ProgressBar usefulnessIndexProgress;
    private TextView usefulnessIndexDescription;

    private ImageButton backButton;
    private ImageButton favoriteButton;
    private AppBarLayout appBarLayout;

    FavoriteManager favoriteManager;
    NutritionDisplayManager nutritionDisplayManager = new NutritionDisplayManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portion_size);

        foodManager = FoodManager.getInstance(this);


        selectedFood = getIntent().getParcelableExtra(Constants.EXTRA_FOOD);
        selectedDish = getIntent().getParcelableExtra(Constants.EXTRA_DISH);
        mealType = getIntent().getStringExtra(Constants.EXTRA_MEAL_TYPE);
        barcode = getIntent().getStringExtra(Constants.EXTRA_BARCODE);

        isIngredientSelection = getIntent().getBooleanExtra("is_ingredient_selection", false);


        if (selectedDish != null && selectedFood == null) {
            selectedFood = createFoodFromDish(selectedDish);

        }


        float portionQuantity = getIntent().getFloatExtra("portion_quantity", 0f);
        int portionSize = portionQuantity > 0 ? (int) portionQuantity : getIntent().getIntExtra(Constants.EXTRA_PORTION_SIZE, 100);


        String currentPortionName = getIntent().getStringExtra("portion_name");
        selectedPortionName = currentPortionName;


        String selectedDateStr = getIntent().getStringExtra(Constants.EXTRA_SELECTED_DATE);
        if (selectedDateStr != null && !selectedDateStr.isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date selectedDate = dateFormat.parse(selectedDateStr);
                if (selectedDate != null) {

                    foodManager.setSelectedDateForView(selectedDate);

                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при парсинге даты: " + e.getMessage());
            }
        }
        if (selectedFood == null) {


            Toast.makeText(this, "Ошибка: не передан продукт", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        nutritionDisplayManager.initViews(this);
        initViews();
        removeAllNutrientRows();
        setupClickListeners();
        setupTextChangedListener();


        if (portionQuantity > 0) {

            if (portionQuantity == (int) portionQuantity) {
                portionSizeInput.setText(String.valueOf((int) portionQuantity));
            } else {
                portionSizeInput.setText(String.valueOf(portionQuantity));
            }
        } else {

            portionSizeInput.setText(String.valueOf(portionSize));
        }


        setupFoodInfo();
        favoriteManager = new FavoriteManager(this, selectedFood, this);

        favoriteManager.initializeButton();
        favoriteManager.checkFavoriteStatus();


        setupPortionSpinner();
    }

    private void initViews() {

        portionSizeInput = findViewById(R.id.custom_portion_input);
        portionSizeInputLayout = findViewById(R.id.portion_size_input_layout);
        portionSpinner = findViewById(R.id.portion_spinner);
        portionUnitInputLayout = findViewById(R.id.portion_unit_input_layout);
        caloriesText = findViewById(R.id.calories_text);
        foodCategoryText = findViewById(R.id.food_category_text);
        proteinsValue = findViewById(R.id.proteins_value);
        fatsValue = findViewById(R.id.fats_value);
        carbsValue = findViewById(R.id.carbs_value);
        usefulnessIndexValue = findViewById(R.id.usefulness_index_value);
        usefulnessIndexProgress = findViewById(R.id.usefulness_index_progress);
        usefulnessIndexDescription = findViewById(R.id.usefulness_index_description);
        backButton = findViewById(R.id.back_button);
        favoriteButton = findViewById(R.id.favorite_button);
        appBarLayout = findViewById(R.id.app_bar);
        setupAppBarListener();
    }


    private void setupAppBarListener() {
        if (appBarLayout == null) {
            return;
        }

        int surfaceColor = ContextCompat.getColor(this, R.color.colorSurface);

        boolean isLightTheme = (surfaceColor == getResources().getColor(android.R.color.white));
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {

            float scrollRange = appBarLayout.getTotalScrollRange();
            float collapseProgress = Math.abs(verticalOffset) / scrollRange;

            final float threshold = 0.9f;
            boolean isCollapsed = collapseProgress >= threshold;

            int iconColor;
            if (isCollapsed) {

                iconColor = isLightTheme
                        ? getResources().getColor(R.color.textColorPrimary)
                        : getResources().getColor(android.R.color.white);
            } else {

                iconColor = getResources().getColor(android.R.color.white);
            }


            if (backButton != null) {
                backButton.setColorFilter(iconColor);
            }
            if (favoriteButton != null) {
                favoriteButton.setColorFilter(iconColor);
            }


            if (getWindow() != null) {
                if (isCollapsed) {

                    getWindow().setStatusBarColor(surfaceColor);

                    if (isLightTheme) {

                        int flags = getWindow().getDecorView().getSystemUiVisibility();
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        getWindow().getDecorView().setSystemUiVisibility(flags);
                    } else {

                        int flags = getWindow().getDecorView().getSystemUiVisibility();
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        getWindow().getDecorView().setSystemUiVisibility(flags);
                    }
                } else {

                    getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.statusbar_color));
                    int flags = getWindow().getDecorView().getSystemUiVisibility();
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    getWindow().getDecorView().setSystemUiVisibility(flags);
                }
            }
        });
    }


    private void setupPortionSpinner() {
        if (selectedFood == null) {
            Log.w(TAG, "selectedFood is null, cannot setup portion spinner");
            return;
        }


        List<String> portionNames = new ArrayList<>();


        if (selectedFood.isLiquid()) {
            portionNames.add("мл");
        } else {
            portionNames.add("грамм");
        }


        if (selectedFood.getPortions() != null && !selectedFood.getPortions().isEmpty()) {
            for (Portion portion : selectedFood.getPortions()) {
                if (portion.getName() != null && !portion.getName().trim().isEmpty()) {
                    portionNames.add(portion.getName());
                }
            }
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.item_portion_dropdown, R.id.portion_text, portionNames) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);


                View divider = view.findViewById(R.id.portion_divider);
                if (divider != null) {
                    divider.setVisibility(position == portionNames.size() - 1 ? View.GONE : View.VISIBLE);
                }

                return view;
            }
        };
        portionSpinner.setAdapter(adapter);


        portionSpinner.setDropDownBackgroundResource(R.drawable.bg_portion_dropdown_popup);


        int verticalOffset = (int) (8 * getResources().getDisplayMetrics().density);
        portionSpinner.setDropDownVerticalOffset(verticalOffset);


        if (selectedPortionName != null && portionNames.contains(selectedPortionName)) {
            portionSpinner.setText(selectedPortionName, false);
        } else {

            if (!portionNames.isEmpty()) {
                selectedPortionName = portionNames.get(0);
                portionSpinner.setText(selectedPortionName, false);
            }
        }


        portionSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedPortionName = portionNames.get(position);
            Log.d(TAG, "Выбрана порция: " + selectedPortionName);


            String currentPortionSize = portionSizeInput.getText().toString();
            if (!currentPortionSize.isEmpty()) {
                updateNutrients(currentPortionSize);
            }
        });
    }

    private void setupClickListeners() {

        Button addButton = findViewById(R.id.add_button);
        if (isIngredientSelection) {
            addButton.setText("Выбрать");
        }

        addButton.setOnClickListener(v -> {
            String portionSizeStr = portionSizeInput.getText().toString();
            if (!portionSizeStr.isEmpty()) {
                float portionSize = Float.parseFloat(portionSizeStr);
                if (portionSize > 0) {

                    String portionName = selectedPortionName != null ? selectedPortionName :
                            (selectedFood.isLiquid() ? "мл" : "грамм");


                    if (isIngredientSelection) {


                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("portion_quantity", portionSize);
                        resultIntent.putExtra("portion_name", portionName);


                        int ingredientPosition = getIntent().getIntExtra("ingredient_position", -1);
                        int recognizedFoodPosition = getIntent().getIntExtra("recognized_food_position", -1);

                        if (ingredientPosition >= 0) {
                            resultIntent.putExtra("ingredient_position", ingredientPosition);
                        }
                        if (recognizedFoodPosition >= 0) {
                            resultIntent.putExtra("recognized_food_position", recognizedFoodPosition);
                        }

                        setResult(RESULT_OK, resultIntent);
                        finish();
                        return;
                    }

                    if (mealType == null) {
                        Toast.makeText(this, "Не указан прием пищи", Toast.LENGTH_SHORT).show();
                        return;
                    }


                    portionName = selectedPortionName != null ? selectedPortionName :
                            (selectedFood.isLiquid() ? "мл" : "грамм");

                    Log.d(TAG, "Добавление продукта: " + selectedFood.getName() +
                            " (ID: " + selectedFood.getId() + ") в прием пищи: " + mealType);


                    String selectedDateStr = foodManager.getSelectedDateFormatted();
                    Meal currentMeal = foodManager.getMealForDate(mealType, selectedDateStr);


                    if (currentMeal == null) {

                        foodManager.addFoodToMeal(mealType, selectedFood, portionSize, portionName);
                        Log.d(TAG, "Создан новый прием пищи и добавлен продукт: " + selectedFood.getName());
                        foodManager.addToRecents(selectedFood, portionSize, portionName);

                        EventBus.getDefault().post(new FoodAddedEvent(selectedFood, (int) portionSize, mealType));
                    } else {


                        Meal newMeal = new Meal(currentMeal.getTitle(), currentMeal.getIconResId());


                        boolean foundExisting = false;


                        Log.d(TAG, "Текущий прием пищи '" + mealType + "' содержит " + currentMeal.getFoods().size() + " продуктов:");


                        int existingFoodIndex = -1;
                        for (int i = 0; i < currentMeal.getFoods().size(); i++) {
                            Meal.FoodPortion currentPortion = currentMeal.getFoods().get(i);
                            Food currentFood = currentPortion.getFood();


                            Log.d(TAG, "Продукт #" + i + ": " + currentFood.getName() +
                                    " (ID: " + currentFood.getId() + "), порция: " +
                                    currentPortion.getQuantity() + " " + currentPortion.getPortionName());


                            boolean sameId = Objects.equals(currentFood.getId(), selectedFood.getId());
                            boolean sameName = currentFood.getName().equals(selectedFood.getName());


                            Log.d(TAG, "Сравнение с выбранным продуктом: " +
                                    "sameId=" + sameId +
                                    ", sameName=" + sameName);


                            if (sameId && sameName) {
                                foundExisting = true;
                                existingFoodIndex = i;
                                Log.d(TAG, "НАЙДЕН СУЩЕСТВУЮЩИЙ продукт по ID и имени: " +
                                        "id=" + currentFood.getId() + ", name=" + currentFood.getName() +
                                        ", будет обновлен размер порции");
                                break;
                            }
                        }


                        for (int i = 0; i < currentMeal.getFoods().size(); i++) {
                            Meal.FoodPortion currentPortion = currentMeal.getFoods().get(i);
                            Food currentFood = currentPortion.getFood();

                            if (i == existingFoodIndex) {


                                newMeal.addFood(selectedFood, portionSize, portionName);

                                foodManager.addToRecents(selectedFood, portionSize, portionName);

                            } else {

                                newMeal.addFood(currentFood, currentPortion.getQuantity(), currentPortion.getPortionName());

                            }
                        }


                        if (!foundExisting) {
                            newMeal.addFood(selectedFood, portionSize, portionName);
                            foodManager.addToRecents(selectedFood, portionSize, portionName);

                        }


                        foodManager.updateMeal(mealType, newMeal);


                        if (foundExisting) {
                            Toast.makeText(this, "Размер порции обновлен", Toast.LENGTH_SHORT).show();

                        } else {

                            EventBus.getDefault().post(new FoodAddedEvent(selectedFood, portionSize, mealType));
                            Toast.makeText(this, "Продукт добавлен", Toast.LENGTH_SHORT).show();

                        }
                    }


                    if (barcode != null && !barcode.isEmpty() && selectedFood != null) {

                        new Thread(() -> {

                            try {
                                SupabaseBarcodeRepository barcodeRepository = foodManager.getBarcodeRepository();
                                SupabaseFoodRepository foodRepository = foodManager.getFoodRepository();

                                if (barcodeRepository == null || foodRepository == null) {

                                    return;
                                }


                                Food existingFoodByBarcode = barcodeRepository.findFoodByBarcode(barcode);
                                if (existingFoodByBarcode != null) {

                                    return;
                                }


                                String uuid = selectedFood.getId();


                                if (uuid == null || uuid.isEmpty()) {


                                    Food existingFood = foodRepository.getFoodByName(selectedFood.getName());

                                    if (existingFood != null) {

                                        uuid = existingFood.getId();
                                    } else {

                                        uuid = foodRepository.addFood(selectedFood);

                                        if (uuid == null) {

                                            return;
                                        }

                                    }
                                }


                                if (uuid == null || uuid.isEmpty()) {
                                    Log.e(TAG, "Не удалось получить UUID продукта для сохранения штрихкода");
                                    return;
                                }


                                Food foodWithUUID = new Food.Builder()
                                        .id(selectedFood.getId())
                                        .name(selectedFood.getName())
                                        .category(selectedFood.getCategory())
                                        .subcategory(selectedFood.getSubcategory())
                                        .calories(selectedFood.getCalories())
                                        .proteins(selectedFood.getProteins())
                                        .fats(selectedFood.getFats())
                                        .carbs(selectedFood.getCarbs())
                                        .build();


                                barcodeRepository.addBarcode(barcode, foodWithUUID);


                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка при сохранении штрихкода: " + e.getMessage(), e);
                            }
                        }).start();
                    }

                    finish();
                } else {
                    Toast.makeText(this, "Размер порции должен быть больше 0", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Введите размер порции", Toast.LENGTH_SHORT).show();
            }
        });


        findViewById(R.id.back_button).setOnClickListener(v -> finish());


    }

    private void setupTextChangedListener() {

        portionSizeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateNutrients(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFoodInfo() {
        if (selectedFood != null) {
            TextView foodNameView = findViewById(R.id.food_name_text);
            foodNameView.setText(selectedFood.getName());


            ImageView iconNotModeratedNutrition = findViewById(R.id.icon_not_moderated_nutrition);
            if (iconNotModeratedNutrition != null) {
                boolean isModerated = selectedFood.isModerated();
                iconNotModeratedNutrition.setVisibility(isModerated ? View.GONE : View.VISIBLE);


                if (!isModerated) {
                    iconNotModeratedNutrition.setOnClickListener(v -> {
                        Toast.makeText(this,
                                R.string.user_added_product_warning,
                                Toast.LENGTH_LONG).show();
                    });
                }
            }


            foodCategoryText.setText(selectedFood.getCategory() + " / " + selectedFood.getSubcategory());


            updateUsefulnessIndex(selectedFood.getUsefulnessIndex());

            boolean isFromGigaChat = (selectedFood.getId() != null && selectedFood.getId().isEmpty()) &&
                    (Float.isNaN(selectedFood.getVitaminA()) &&
                            Float.isNaN(selectedFood.getCalcium()) &&
                            Float.isNaN(selectedFood.getFiber()));


            boolean isVirtualNutrients = "Быстрое добавление нутриентов".equals(selectedFood.getName());


            boolean isAlreadyInMeal = false;
            if (mealType != null) {
                Meal currentMeal = foodManager.getMeal(mealType);
                if (currentMeal != null) {

                    for (Meal.FoodPortion portion : currentMeal.getFoods()) {
                        Food mealFood = portion.getFood();

                        if (mealFood.getName().equals(selectedFood.getName())) {
                            isAlreadyInMeal = true;
                            break;
                        }
                    }
                }
            }


            ImageButton editCategoryButton = findViewById(R.id.edit_category_button);
            editCategoryButton.setVisibility((isFromGigaChat && !isAlreadyInMeal) ? View.VISIBLE : View.GONE);


            editCategoryButton.setOnClickListener(v -> showCategoryPickerDialog());


            if (isVirtualNutrients) {
                View portionCard = findViewById(R.id.portion_card);
                if (portionCard != null) {
                    portionCard.setVisibility(View.GONE);
                }
            }
        }
    }


    private void updateUsefulnessIndex(int usefulnessIndex) {
        usefulnessIndexValue.setText(String.valueOf(usefulnessIndex));
        usefulnessIndexProgress.setProgress(usefulnessIndex);


        if (usefulnessIndex >= 8) {

            usefulnessIndexProgress.setProgressDrawable(getResources().getDrawable(R.drawable.usefulness_progress_high));
            usefulnessIndexValue.setTextColor(getResources().getColor(R.color.green_500));
        } else if (usefulnessIndex >= 4) {

            usefulnessIndexProgress.setProgressDrawable(getResources().getDrawable(R.drawable.usefulness_progress_medium));
            usefulnessIndexValue.setTextColor(getResources().getColor(R.color.yellow_500));
        } else {

            usefulnessIndexProgress.setProgressDrawable(getResources().getDrawable(R.drawable.usefulness_progress_low));
            usefulnessIndexValue.setTextColor(getResources().getColor(R.color.red_500));
        }


        String description;
        if (usefulnessIndex >= 8) {
            description = "Очень полезный продукт, богатый питательными веществами";
        } else if (usefulnessIndex >= 6) {
            description = "Хороший выбор, содержит много полезных элементов";
        } else if (usefulnessIndex >= 4) {
            description = "Продукт средней пищевой ценности";
        } else if (usefulnessIndex >= 2) {
            description = "Умеренно полезный продукт, используйте в ограниченном количестве";
        } else {
            description = "Продукт с низкой пищевой ценностью, рекомендуется ограничить употребление";
        }

        usefulnessIndexDescription.setText(description);
    }

    private void updateNutrients(String portionStr) {

        if (selectedFood == null) {
            Log.w(TAG, "updateNutrients: selectedFood is null, skipping update");
            return;
        }

        try {
            float portion = Float.parseFloat(portionStr);


            float portionInGrams = convertToGrams(portion, selectedPortionName);
            double multiplier = portionInGrams / 100.0;


            if (selectedFood.getCalories() >= 0) {
                double calories = selectedFood.getCalories() * multiplier;
                caloriesText.setText(String.format("%d ккал", Math.round(calories)));
            } else {
                caloriesText.setText("нет данных");
            }


            updateMacronutrient(proteinsValue, selectedFood.getProteins(), multiplier);
            updateMacronutrient(fatsValue, selectedFood.getFats(), multiplier);
            updateMacronutrient(carbsValue, selectedFood.getCarbs(), multiplier);


            nutritionDisplayManager.updateAllNutrition(selectedFood, multiplier);

        } catch (NumberFormatException e) {
            resetValues();
        }
    }


    private void updateMacronutrient(TextView textView, float value, double multiplier) {
        if (!Float.isNaN(value) && value >= 0) {
            textView.setText(String.format("%.1f г", value * multiplier));
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setText("нет данных");
        }
    }


    private void removeAllNutrientRows() {


        setRowVisibility(findViewById(R.id.fiber_row), View.GONE);
        setRowVisibility(findViewById(R.id.sugar_row), View.GONE);
        setRowVisibility(findViewById(R.id.cholesterol_row), View.GONE);
        setRowVisibility(findViewById(R.id.saturated_fats_row), View.GONE);
        setRowVisibility(findViewById(R.id.trans_fats_row), View.GONE);


        setRowVisibility(findViewById(R.id.vitamin_a_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_b1_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_b2_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_b3_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_b5_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_b6_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_b9_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_b12_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_c_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_d_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_e_row), View.GONE);
        setRowVisibility(findViewById(R.id.vitamin_k_row), View.GONE);


        LinearLayout mineralsContent = findViewById(R.id.minerals_content);
        if (mineralsContent != null) {
            for (int i = 0; i < mineralsContent.getChildCount(); i++) {
                View child = mineralsContent.getChildAt(i);
                if (child != null) {
                    child.setVisibility(View.GONE);
                }
            }
        }
    }


    private void setRowVisibility(TableRow row, int visibility) {
        if (row != null) {
            row.setVisibility(visibility);
        }
    }

    private void resetValues() {

        caloriesText.setText("нет данных");


        proteinsValue.setText("нет данных");
        fatsValue.setText("нет данных");
        carbsValue.setText("нет данных");


        findViewById(R.id.additional_nutrients_container).setVisibility(View.GONE);
        findViewById(R.id.minerals_container).setVisibility(View.GONE);
        findViewById(R.id.vitamins_container).setVisibility(View.GONE);
    }


    private void showCategoryPickerDialog() {

        SupabaseFoodRepository foodRepository = foodManager.getFoodRepository();


        if (foodRepository == null) {
            Toast.makeText(this, "Не удалось получить доступ к базе данных", Toast.LENGTH_SHORT).show();
            return;
        }


        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Загрузка категорий...");
        progressDialog.setCancelable(false);
        progressDialog.show();


        new Thread(() -> {
            try {

                List<String> categoryList = foodRepository.getAllUniqueCategories();


                if (selectedFood.getCategory() != null && !selectedFood.getCategory().isEmpty()) {
                    if (!categoryList.contains(selectedFood.getCategory())) {
                        categoryList.add(selectedFood.getCategory());
                    }
                }


                if (!categoryList.contains("Другое")) {
                    categoryList.add("Другое");
                }


                Collections.sort(categoryList);


                final String[] categories = categoryList.toArray(new String[0]);


                final Map<String, String[]> subcategories = new HashMap<>();


                for (String category : categories) {
                    List<String> subcategoryList = foodRepository.getUniqueSubcategoriesForCategory(category);


                    if (selectedFood.getCategory() != null &&
                            selectedFood.getCategory().equals(category) &&
                            selectedFood.getSubcategory() != null &&
                            !selectedFood.getSubcategory().isEmpty()) {
                        if (!subcategoryList.contains(selectedFood.getSubcategory())) {
                            subcategoryList.add(selectedFood.getSubcategory());
                        }
                    }


                    if (!subcategoryList.contains("Другое")) {
                        subcategoryList.add("Другое");
                    }


                    Collections.sort(subcategoryList);


                    subcategories.put(category, subcategoryList.toArray(new String[0]));
                }


                runOnUiThread(() -> {

                    progressDialog.dismiss();


                    String currentCategory = selectedFood.getCategory();
                    String currentSubcategory = selectedFood.getSubcategory();


                    int currentCategoryIndex = 0;
                    for (int i = 0; i < categories.length; i++) {
                        if (categories[i].equalsIgnoreCase(currentCategory)) {
                            currentCategoryIndex = i;
                            break;
                        }
                    }


                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_picker, null);


                    NumberPicker categoryPicker = dialogView.findViewById(R.id.category_picker);
                    NumberPicker subcategoryPicker = dialogView.findViewById(R.id.subcategory_picker);


                    categoryPicker.setMinValue(0);
                    categoryPicker.setMaxValue(categories.length - 1);
                    categoryPicker.setDisplayedValues(categories);
                    categoryPicker.setValue(currentCategoryIndex);


                    Runnable updateSubcategories = () -> {
                        String selectedCategory = categories[categoryPicker.getValue()];
                        String[] subcat = subcategories.get(selectedCategory);


                        subcategoryPicker.setDisplayedValues(null);


                        subcategoryPicker.setMinValue(0);
                        subcategoryPicker.setMaxValue(subcat.length - 1);


                        subcategoryPicker.setDisplayedValues(subcat);


                        if (selectedCategory.equalsIgnoreCase(currentCategory)) {
                            String[] currentSubcategories = subcategories.get(selectedCategory);
                            int index = 0;
                            for (int i = 0; i < currentSubcategories.length; i++) {
                                if (currentSubcategories[i].equalsIgnoreCase(currentSubcategory)) {
                                    index = i;
                                    break;
                                }
                            }
                            subcategoryPicker.setValue(index);
                        } else {
                            subcategoryPicker.setValue(0);
                        }
                    };


                    updateSubcategories.run();


                    categoryPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSubcategories.run());


                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Выберите категорию и подкатегорию")
                            .setView(dialogView)
                            .setPositiveButton("Сохранить", (dialog, which) -> {

                                String selectedCategory = categories[categoryPicker.getValue()];
                                String[] subcat = subcategories.get(selectedCategory);
                                String selectedSubcategory = subcat[subcategoryPicker.getValue()];

                                selectedFood = new Food.Builder()
                                        .id(selectedFood.getId())
                                        .name(selectedFood.getName())
                                        .category(selectedCategory)
                                        .subcategory(selectedSubcategory)
                                        .calories(selectedFood.getCalories())
                                        .proteins(selectedFood.getProteins())
                                        .fats(selectedFood.getFats())
                                        .carbs(selectedFood.getCarbs())
                                        .fiber(selectedFood.getFiber())
                                        .sugar(selectedFood.getSugar())
                                        .saturatedFats(selectedFood.getSaturatedFats())
                                        .transFats(selectedFood.getTransFats())
                                        .cholesterol(selectedFood.getCholesterol())
                                        .sodium(selectedFood.getSodium())
                                        .calcium(selectedFood.getCalcium())
                                        .iron(selectedFood.getIron())
                                        .magnesium(selectedFood.getMagnesium())
                                        .phosphorus(selectedFood.getPhosphorus())
                                        .potassium(selectedFood.getPotassium())
                                        .zinc(selectedFood.getZinc())
                                        .vitaminA(selectedFood.getVitaminA())
                                        .vitaminB1(selectedFood.getVitaminB1())
                                        .vitaminB2(selectedFood.getVitaminB2())
                                        .vitaminB3(selectedFood.getVitaminB3())
                                        .vitaminB5(selectedFood.getVitaminB5())
                                        .vitaminB6(selectedFood.getVitaminB6())
                                        .vitaminB9(selectedFood.getVitaminB9())
                                        .vitaminB12(selectedFood.getVitaminB12())
                                        .vitaminC(selectedFood.getVitaminC())
                                        .vitaminD(selectedFood.getVitaminD())
                                        .vitaminE(selectedFood.getVitaminE())
                                        .vitaminK(selectedFood.getVitaminK())
                                        .popularity(selectedFood.getPopularity())
                                        .usefulness_index(selectedFood.getUsefulnessIndex())
                                        .portions(selectedFood.getPortions())
                                        .build();


                                foodCategoryText.setText(selectedCategory + " / " + selectedSubcategory);
                                Toast.makeText(this, "Категория обновлена", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Отмена", null);

                    builder.create().show();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Ошибка загрузки категорий: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Ошибка при загрузке категорий: " + e.getMessage(), e);


                    showStaticCategoryPickerDialog();
                });
            }
        }).start();
    }


    private void showStaticCategoryPickerDialog() {

        String[] categories = {
                "Молочные продукты", "Мясо", "Рыба и морепродукты", "Яйца", "Крупы и злаки",
                "Фрукты", "Ягоды", "Овощи", "Бобовые", "Орехи и семена",
                "Хлеб и выпечка", "Сладости", "Напитки", "Соусы и приправы", "Другое"
        };


        java.util.Map<String, String[]> subcategories = new java.util.HashMap<>();
        subcategories.put("Молочные продукты", new String[]{"Молоко", "Творог", "Сыр", "Йогурт", "Сметана", "Кефир", "Другое"});
        subcategories.put("Мясо", new String[]{"Говядина", "Свинина", "Курица", "Индейка", "Утка", "Кролик", "Баранина", "Другое"});
        subcategories.put("Рыба и морепродукты", new String[]{"Морская рыба", "Речная рыба", "Креветки", "Кальмары", "Мидии", "Крабы", "Осьминоги", "Другое"});
        subcategories.put("Яйца", new String[]{"Куриные", "Перепелиные", "Другое"});
        subcategories.put("Крупы и злаки", new String[]{"Рис", "Гречка", "Овсянка", "Пшеница", "Ячмень", "Кукуруза", "Киноа", "Другое"});
        subcategories.put("Фрукты", new String[]{"Яблоки", "Груши", "Бананы", "Апельсины", "Мандарины", "Лимоны", "Другое"});
        subcategories.put("Ягоды", new String[]{"Клубника", "Малина", "Черника", "Голубика", "Ежевика", "Вишня", "Другое"});
        subcategories.put("Овощи", new String[]{"Картофель", "Морковь", "Капуста", "Лук", "Чеснок", "Огурцы", "Помидоры", "Перец", "Другое"});
        subcategories.put("Бобовые", new String[]{"Горох", "Фасоль", "Чечевица", "Нут", "Соя", "Другое"});
        subcategories.put("Орехи и семена", new String[]{"Грецкие орехи", "Миндаль", "Фундук", "Кешью", "Семена льна", "Семена чиа", "Другое"});
        subcategories.put("Хлеб и выпечка", new String[]{"Хлеб белый", "Хлеб ржаной", "Булочки", "Пирожки", "Другое"});
        subcategories.put("Сладости", new String[]{"Шоколад", "Конфеты", "Печенье", "Торты", "Мороженое", "Другое"});
        subcategories.put("Напитки", new String[]{"Вода", "Чай", "Кофе", "Соки", "Газированные напитки", "Алкоголь", "Другое"});
        subcategories.put("Соусы и приправы", new String[]{"Майонез", "Кетчуп", "Горчица", "Соевый соус", "Соль", "Перец", "Другое"});
        subcategories.put("Другое", new String[]{"Другое"});


        String currentCategory = selectedFood.getCategory();
        String currentSubcategory = selectedFood.getSubcategory();


        int currentCategoryIndex = 0;
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(currentCategory)) {
                currentCategoryIndex = i;
                break;
            }
        }


        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_picker, null);


        NumberPicker categoryPicker = dialogView.findViewById(R.id.category_picker);
        NumberPicker subcategoryPicker = dialogView.findViewById(R.id.subcategory_picker);


        categoryPicker.setMinValue(0);
        categoryPicker.setMaxValue(categories.length - 1);
        categoryPicker.setDisplayedValues(categories);
        categoryPicker.setValue(currentCategoryIndex);


        Runnable updateSubcategories = () -> {
            String selectedCategory = categories[categoryPicker.getValue()];
            String[] subcat = subcategories.get(selectedCategory);


            subcategoryPicker.setDisplayedValues(null);


            subcategoryPicker.setMinValue(0);
            subcategoryPicker.setMaxValue(subcat.length - 1);


            subcategoryPicker.setDisplayedValues(subcat);


            if (selectedCategory.equalsIgnoreCase(currentCategory)) {
                String[] currentSubcategories = subcategories.get(selectedCategory);
                int index = 0;
                for (int i = 0; i < currentSubcategories.length; i++) {
                    if (currentSubcategories[i].equalsIgnoreCase(currentSubcategory)) {
                        index = i;
                        break;
                    }
                }
                subcategoryPicker.setValue(index);
            } else {
                subcategoryPicker.setValue(0);
            }
        };


        updateSubcategories.run();


        categoryPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSubcategories.run());


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите категорию и подкатегорию")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {

                    String selectedCategory = categories[categoryPicker.getValue()];
                    String[] subcat = subcategories.get(selectedCategory);
                    String selectedSubcategory = subcat[subcategoryPicker.getValue()];

                    selectedFood = new Food.Builder()
                            .id(selectedFood.getId())
                            .name(selectedFood.getName())
                            .category(selectedCategory)
                            .subcategory(selectedSubcategory)
                            .calories(selectedFood.getCalories())
                            .proteins(selectedFood.getProteins())
                            .fats(selectedFood.getFats())
                            .carbs(selectedFood.getCarbs())
                            .fiber(selectedFood.getFiber())
                            .sugar(selectedFood.getSugar())
                            .saturatedFats(selectedFood.getSaturatedFats())
                            .transFats(selectedFood.getTransFats())
                            .cholesterol(selectedFood.getCholesterol())
                            .sodium(selectedFood.getSodium())
                            .calcium(selectedFood.getCalcium())
                            .iron(selectedFood.getIron())
                            .magnesium(selectedFood.getMagnesium())
                            .phosphorus(selectedFood.getPhosphorus())
                            .potassium(selectedFood.getPotassium())
                            .zinc(selectedFood.getZinc())
                            .vitaminA(selectedFood.getVitaminA())
                            .vitaminB1(selectedFood.getVitaminB1())
                            .vitaminB2(selectedFood.getVitaminB2())
                            .vitaminB3(selectedFood.getVitaminB3())
                            .vitaminB5(selectedFood.getVitaminB5())
                            .vitaminB6(selectedFood.getVitaminB6())
                            .vitaminB9(selectedFood.getVitaminB9())
                            .vitaminB12(selectedFood.getVitaminB12())
                            .vitaminC(selectedFood.getVitaminC())
                            .vitaminD(selectedFood.getVitaminD())
                            .vitaminE(selectedFood.getVitaminE())
                            .vitaminK(selectedFood.getVitaminK())
                            .popularity(selectedFood.getPopularity())
                            .usefulness_index(selectedFood.getUsefulnessIndex())
                            .portions(selectedFood.getPortions())
                            .build();


                    foodCategoryText.setText(selectedCategory + " / " + selectedSubcategory);
                    Toast.makeText(this, "Категория обновлена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null);

        builder.create().show();
    }


    private Food createFoodFromDish(Dish dish) {
        if (dish == null || dish.getIngredients() == null || dish.getIngredients().isEmpty()) {
            Log.w(TAG, "Попытка создать Food из пустого блюда");
            return null;
        }


        float totalWeightGrams = 0;
        for (Dish.DishIngredient ingredient : dish.getIngredients()) {
            totalWeightGrams += ingredient.getTotalWeightInGrams();
        }

        if (totalWeightGrams <= 0) {
            Log.w(TAG, "Общий вес блюда равен 0");
            return null;
        }


        float totalCalories = dish.getTotalCalories();
        float totalProteins = dish.getTotalProteins();
        float totalFats = dish.getTotalFats();
        float totalCarbs = dish.getTotalCarbs();


        float per100gCalories = (totalCalories / totalWeightGrams) * 100f;
        float per100gProteins = (totalProteins / totalWeightGrams) * 100f;
        float per100gFats = (totalFats / totalWeightGrams) * 100f;
        float per100gCarbs = (totalCarbs / totalWeightGrams) * 100f;


        Food virtualFood = new Food.Builder()
                .id(UUID.randomUUID().toString())
                .dishId(dish.getId())
                .name(dish.getName())
                .category("Блюда")
                .subcategory("Собственные")
                .calories(Math.round(per100gCalories))
                .proteins(per100gProteins)
                .fats(per100gFats)
                .carbs(per100gCarbs)
                .isLiquid(false)
                .portions(Collections.emptyList())
                .build();

        return virtualFood;
    }


    private float convertToGrams(float quantity, String portionName) {
        if (portionName == null) {
            return quantity;
        }


        if (portionName.equals("грамм") || portionName.equals("мл")) {
            return quantity;
        }


        if (selectedFood.getPortions() != null) {
            for (Portion portion : selectedFood.getPortions()) {
                if (portion.getName().equals(portionName)) {
                    float result = quantity * portion.getWeight();
                    Log.d(TAG, quantity + " " + portionName + " = " + result + " г (вес порции: " + portion.getWeight() + " г)");
                    return result;
                }
            }
        }

        return quantity;
    }
} 