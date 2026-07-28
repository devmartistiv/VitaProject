package com.martist.vitamove.nutrition.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.domain.utils.ProfanityFilter;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.FoodSyncService;
import com.martist.vitamove.nutrition.data.repository.SupabaseBarcodeRepository;
import com.martist.vitamove.nutrition.data.repository.SupabaseFoodRepository;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Portion;

import java.util.ArrayList;
import java.util.List;


public class CreateProductActivity extends BaseActivity {
    private static final String TAG = "CreateProductActivity";
    private static final int REQUEST_BARCODE_SCAN = 1001;


    private TextInputEditText etProductName;
    private AutoCompleteTextView etCategory;
    private AutoCompleteTextView etSubcategory;
    private TextInputEditText etCalories;
    private TextInputEditText etProteins;
    private TextInputEditText etFats;
    private TextInputEditText etCarbs;


    private TextInputEditText etFiber;
    private TextInputEditText etSugar;


    private AutoCompleteTextView etUnit;


    private TextInputEditText etBarcode;
    private MaterialButton btnScanBarcode;
    private String scannedBarcode = null;


    private TextInputEditText etVitaminA, etVitaminB1, etVitaminB2, etVitaminB3, etVitaminB5, etVitaminB6;
    private TextInputEditText etVitaminB9, etVitaminB12, etVitaminC, etVitaminD, etVitaminE, etVitaminK;


    private TextInputEditText etCalcium, etIron, etMagnesium, etPhosphorus, etPotassium, etSodium, etZinc;


    private TextInputEditText etCholesterol, etSaturatedFats, etTransFats;


    private TextInputLayout tilProductName;
    private TextInputLayout tilCategory;
    private TextInputLayout tilSubcategory;
    private TextInputLayout tilCalories;
    private TextInputLayout tilProteins;
    private TextInputLayout tilFats;
    private TextInputLayout tilCarbs;


    private View progressOverlay;
    private LinearLayout containerAdditionalFields;
    private ImageView ivExpandIcon;
    private boolean isAdditionalFieldsExpanded = true;
    private View tvProductNotFoundMessage;


    private SupabaseFoodRepository foodRepository;


    private List<String> categories = new ArrayList<>();
    private List<String> subcategories = new ArrayList<>();


    private List<String> units = new ArrayList<>();
    private List<Portion> customPortions = new ArrayList<>();
    private static final String UNIT_GRAMS = "граммы";
    private static final String UNIT_ML = "мл";
    private static final String UNIT_CREATE_NEW = "создать новую порцию";
    private String selectedUnit = UNIT_GRAMS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_product);


        SupabaseClient supabaseClient = SupabaseClient.getInstance(Constants.SUPABASE_CLIENT_ID, Constants.SUPABASE_CLIENT_SECRET);


        SharedPreferences authPrefs = getSharedPreferences("auth_data", Context.MODE_PRIVATE);
        String accessToken = authPrefs.getString("access_token", null);
        String refreshToken = authPrefs.getString("refresh_token", null);

        if (accessToken != null) {
            supabaseClient.setUserToken(accessToken);
            if (refreshToken != null) {
                supabaseClient.setRefreshToken(refreshToken);
            }
            Log.d(TAG, "Токены установлены для авторизации запросов");
        } else {
            Log.w(TAG, "Токен доступа не найден, запросы могут не работать");
        }

        foodRepository = new SupabaseFoodRepository(supabaseClient, this);


        if (getWindow() != null) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorSurface));
        }

        initViews();
        setupToolbar();
        setupCategoryDropdowns();
        setupUnitDropdown();
        setupBarcode();
        setupAdditionalFields();
        initExpandIcon();
        loadCategories();
        handleIncomingIntent();
    }


    private void initViews() {

        etProductName = findViewById(R.id.et_product_name);
        etCategory = findViewById(R.id.et_category);
        etSubcategory = findViewById(R.id.et_subcategory);
        etCalories = findViewById(R.id.et_calories);
        etProteins = findViewById(R.id.et_proteins);
        etFats = findViewById(R.id.et_fats);
        etCarbs = findViewById(R.id.et_carbs);


        tilProductName = findViewById(R.id.til_product_name);
        tilCategory = findViewById(R.id.til_category);
        tilSubcategory = findViewById(R.id.til_subcategory);
        tilCalories = findViewById(R.id.til_calories);
        tilProteins = findViewById(R.id.til_proteins);
        tilFats = findViewById(R.id.til_fats);
        tilCarbs = findViewById(R.id.til_carbs);


        etFiber = findViewById(R.id.et_fiber);
        etSugar = findViewById(R.id.et_sugar);


        etUnit = findViewById(R.id.et_unit);


        etBarcode = findViewById(R.id.et_barcode);
        btnScanBarcode = findViewById(R.id.btn_scan_barcode);


        etVitaminA = findViewById(R.id.et_vitamin_a);
        etVitaminB1 = findViewById(R.id.et_vitamin_b1);
        etVitaminB2 = findViewById(R.id.et_vitamin_b2);
        etVitaminB3 = findViewById(R.id.et_vitamin_b3);
        etVitaminB5 = findViewById(R.id.et_vitamin_b5);
        etVitaminB6 = findViewById(R.id.et_vitamin_b6);
        etVitaminB9 = findViewById(R.id.et_vitamin_b9);
        etVitaminB12 = findViewById(R.id.et_vitamin_b12);
        etVitaminC = findViewById(R.id.et_vitamin_c);
        etVitaminD = findViewById(R.id.et_vitamin_d);
        etVitaminE = findViewById(R.id.et_vitamin_e);
        etVitaminK = findViewById(R.id.et_vitamin_k);


        etCalcium = findViewById(R.id.et_calcium);
        etIron = findViewById(R.id.et_iron);
        etMagnesium = findViewById(R.id.et_magnesium);
        etPhosphorus = findViewById(R.id.et_phosphorus);
        etPotassium = findViewById(R.id.et_potassium);
        etSodium = findViewById(R.id.et_sodium);
        etZinc = findViewById(R.id.et_zinc);


        etCholesterol = findViewById(R.id.et_cholesterol);
        etSaturatedFats = findViewById(R.id.et_saturated_fats);
        etTransFats = findViewById(R.id.et_trans_fats);


        progressOverlay = findViewById(R.id.progress_overlay);
        containerAdditionalFields = findViewById(R.id.container_additional_fields);
        ivExpandIcon = findViewById(R.id.iv_expand_icon);
        tvProductNotFoundMessage = findViewById(R.id.tv_product_not_found_message);
    }


    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent != null) {

            String productName = intent.getStringExtra("PRODUCT_NAME");
            if (productName != null && !productName.isEmpty()) {
                etProductName.setText(productName);
                Log.d(TAG, "Предзаполнено название продукта: " + productName);
            }


            String barcode = intent.getStringExtra("BARCODE");
            if (barcode != null && !barcode.isEmpty()) {
                scannedBarcode = barcode;
                etBarcode.setText(barcode);
                Log.d(TAG, "Предзаполнен штрихкод: " + barcode);
            }


            boolean fromNotFound = intent.getBooleanExtra("FROM_NOT_FOUND", false);
            if (fromNotFound) {

                tvProductNotFoundMessage.setVisibility(View.VISIBLE);
                Log.d(TAG, "Показано сообщение о том, что продукт не найден");
            }
        }
    }


    private void setupToolbar() {
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());


        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());


        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> validateAndSaveProduct());
    }


    private void setupCategoryDropdowns() {

        etCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = (String) parent.getItemAtPosition(position);
            Log.d(TAG, "Выбрана категория: " + selectedCategory);
            loadSubcategories(selectedCategory);


            etSubcategory.setText("");
            tilSubcategory.setError(null);
        });
    }


    private void setupUnitDropdown() {

        units.clear();
        units.add(UNIT_GRAMS);
        units.add(UNIT_ML);
        units.add(UNIT_CREATE_NEW);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                units
        );
        etUnit.setAdapter(adapter);
        etUnit.setText(UNIT_GRAMS, false);


        etUnit.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);

            if (UNIT_CREATE_NEW.equals(selected)) {

                showCreatePortionDialog();

                etUnit.setText(selectedUnit, false);
            } else {
                selectedUnit = selected;
                Log.d(TAG, "Выбрана единица измерения: " + selectedUnit);
            }
        });
    }


    private void setupBarcode() {

        etBarcode.setOnClickListener(v -> openBarcodeScanner());


        btnScanBarcode.setOnClickListener(v -> openBarcodeScanner());
    }


    private void openBarcodeScanner() {
        Intent intent = new Intent(this, BarcodeScannerActivity.class);
        intent.putExtra("MODE", "SCAN_ONLY");
        startActivityForResult(intent, REQUEST_BARCODE_SCAN);
    }


    private void setupAdditionalFields() {
        View btnToggleAdditional = findViewById(R.id.btn_toggle_additional);
        btnToggleAdditional.setOnClickListener(v -> toggleAdditionalFields());
    }


    private void toggleAdditionalFields() {
        isAdditionalFieldsExpanded = !isAdditionalFieldsExpanded;

        if (isAdditionalFieldsExpanded) {

            containerAdditionalFields.setVisibility(View.VISIBLE);
            ObjectAnimator rotation = ObjectAnimator.ofFloat(ivExpandIcon, "rotation", 0f, 180f);
            rotation.setDuration(300);
            rotation.setInterpolator(new DecelerateInterpolator());
            rotation.start();
        } else {

            containerAdditionalFields.setVisibility(View.GONE);
            ObjectAnimator rotation = ObjectAnimator.ofFloat(ivExpandIcon, "rotation", 180f, 0f);
            rotation.setDuration(300);
            rotation.setInterpolator(new DecelerateInterpolator());
            rotation.start();
        }
    }


    private void initExpandIcon() {
        if (isAdditionalFieldsExpanded) {
            ivExpandIcon.setRotation(180f);
        }
    }


    private void showCreatePortionDialog() {
        CreatePortionDialog dialog = new CreatePortionDialog(this, portion -> {

            customPortions.add(portion);


            int insertIndex = units.size() - 1;
            units.add(insertIndex, portion.getName());


            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    units
            );
            etUnit.setAdapter(adapter);


            selectedUnit = portion.getName();
            etUnit.setText(selectedUnit, false);

            Toast.makeText(this, "Порция добавлена: " + portion.getName(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Добавлена новая порция: " + portion.getName() + " = " + portion.getWeight() + "г");
        });
        dialog.show();
    }


    private void loadCategories() {
        new Thread(() -> {
            try {
                categories = foodRepository.getAllUniqueCategories();

                if (categories == null) {
                    categories = new ArrayList<>();
                }


                if (!categories.contains("Другое")) {
                    categories.add("Другое");
                }


                java.util.Collections.sort(categories);

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            categories
                    );
                    etCategory.setAdapter(adapter);
                    Log.d(TAG, "Загружено категорий: " + categories.size());
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке категорий: " + e.getMessage(), e);
                runOnUiThread(() -> {

                    categories = new ArrayList<>();
                    categories.add("Другое");
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            categories
                    );
                    etCategory.setAdapter(adapter);
                });
            }
        }).start();
    }


    private void loadSubcategories(String category) {
        new Thread(() -> {
            try {
                subcategories = foodRepository.getUniqueSubcategoriesForCategory(category);

                if (subcategories == null) {
                    subcategories = new ArrayList<>();
                }


                if (!subcategories.contains("Другое")) {
                    subcategories.add("Другое");
                }


                java.util.Collections.sort(subcategories);

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            subcategories
                    );
                    etSubcategory.setAdapter(adapter);
                    tilSubcategory.setEnabled(true);
                    Log.d(TAG, "Загружено подкатегорий: " + subcategories.size());
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке подкатегорий: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    subcategories = new ArrayList<>();
                    subcategories.add("Другое");
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            subcategories
                    );
                    etSubcategory.setAdapter(adapter);
                    tilSubcategory.setEnabled(true);
                });
            }
        }).start();
    }


    private void validateAndSaveProduct() {

        clearErrors();

        boolean isValid = true;


        String name = etProductName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            tilProductName.setError("Введите название продукта");
            isValid = false;
        } else if (ProfanityFilter.containsProfanity(name)) {
            tilProductName.setError("Название содержит недопустимые слова");
            Toast.makeText(this, "Пожалуйста, используйте корректное название продукта", Toast.LENGTH_LONG).show();
            isValid = false;
        }


        String category = etCategory.getText().toString().trim();
        if (TextUtils.isEmpty(category)) {
            tilCategory.setError("Выберите категорию");
            isValid = false;
        }


        String subcategory = etSubcategory.getText().toString().trim();
        if (TextUtils.isEmpty(subcategory)) {
            tilSubcategory.setError("Выберите подкатегорию");
            isValid = false;
        }


        String caloriesStr = etCalories.getText().toString().trim();
        if (TextUtils.isEmpty(caloriesStr)) {
            tilCalories.setError("Введите калории");
            isValid = false;
        }


        String proteinsStr = etProteins.getText().toString().trim();
        if (TextUtils.isEmpty(proteinsStr)) {
            tilProteins.setError("Введите белки");
            isValid = false;
        }


        String fatsStr = etFats.getText().toString().trim();
        if (TextUtils.isEmpty(fatsStr)) {
            tilFats.setError("Введите жиры");
            isValid = false;
        }


        String carbsStr = etCarbs.getText().toString().trim();
        if (TextUtils.isEmpty(carbsStr)) {
            tilCarbs.setError("Введите углеводы");
            isValid = false;
        }

        if (!isValid) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
            return;
        }


        try {
            int calories = Integer.parseInt(caloriesStr);
            float proteins = Float.parseFloat(proteinsStr);
            float fats = Float.parseFloat(fatsStr);
            float carbs = Float.parseFloat(carbsStr);


            float fiber = 0f;
            String fiberStr = etFiber.getText().toString().trim();
            if (!TextUtils.isEmpty(fiberStr)) {
                fiber = Float.parseFloat(fiberStr);
            }

            float sugar = 0f;
            String sugarStr = etSugar.getText().toString().trim();
            if (!TextUtils.isEmpty(sugarStr)) {
                sugar = Float.parseFloat(sugarStr);
            }


            float vitaminA = parseFloatOrZero(etVitaminA.getText().toString().trim());
            float vitaminB1 = parseFloatOrZero(etVitaminB1.getText().toString().trim());
            float vitaminB2 = parseFloatOrZero(etVitaminB2.getText().toString().trim());
            float vitaminB3 = parseFloatOrZero(etVitaminB3.getText().toString().trim());
            float vitaminB5 = parseFloatOrZero(etVitaminB5.getText().toString().trim());
            float vitaminB6 = parseFloatOrZero(etVitaminB6.getText().toString().trim());
            float vitaminB9 = parseFloatOrZero(etVitaminB9.getText().toString().trim());
            float vitaminB12 = parseFloatOrZero(etVitaminB12.getText().toString().trim());
            float vitaminC = parseFloatOrZero(etVitaminC.getText().toString().trim());
            float vitaminD = parseFloatOrZero(etVitaminD.getText().toString().trim());
            float vitaminE = parseFloatOrZero(etVitaminE.getText().toString().trim());
            float vitaminK = parseFloatOrZero(etVitaminK.getText().toString().trim());


            float calcium = parseFloatOrZero(etCalcium.getText().toString().trim());
            float iron = parseFloatOrZero(etIron.getText().toString().trim());
            float magnesium = parseFloatOrZero(etMagnesium.getText().toString().trim());
            float phosphorus = parseFloatOrZero(etPhosphorus.getText().toString().trim());
            float potassium = parseFloatOrZero(etPotassium.getText().toString().trim());
            float sodium = parseFloatOrZero(etSodium.getText().toString().trim());
            float zinc = parseFloatOrZero(etZinc.getText().toString().trim());


            float cholesterol = parseFloatOrZero(etCholesterol.getText().toString().trim());
            float saturatedFats = parseFloatOrZero(etSaturatedFats.getText().toString().trim());
            float transFats = parseFloatOrZero(etTransFats.getText().toString().trim());


            Food food = new Food.Builder()
                    .name(name)
                    .category(category)
                    .subcategory(subcategory)
                    .calories(calories)
                    .proteins(proteins)
                    .fats(fats)
                    .carbs(carbs)
                    .fiber(fiber)
                    .sugar(sugar)
                    .isLiquid(false)
                    .vitaminA(vitaminA)
                    .vitaminB1(vitaminB1)
                    .vitaminB2(vitaminB2)
                    .vitaminB3(vitaminB3)
                    .vitaminB5(vitaminB5)
                    .vitaminB6(vitaminB6)
                    .vitaminB9(vitaminB9)
                    .vitaminB12(vitaminB12)
                    .vitaminC(vitaminC)
                    .vitaminD(vitaminD)
                    .vitaminE(vitaminE)
                    .vitaminK(vitaminK)
                    .calcium(calcium)
                    .iron(iron)
                    .magnesium(magnesium)
                    .phosphorus(phosphorus)
                    .potassium(potassium)
                    .sodium(sodium)
                    .zinc(zinc)
                    .cholesterol(cholesterol)
                    .saturatedFats(saturatedFats)
                    .transFats(transFats)
                    .build();


            saveProduct(food);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ошибка: неверный формат числа", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Ошибка парсинга числовых значений: " + e.getMessage());
        }
    }


    private void saveProduct(Food food) {

        progressOverlay.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String productId = foodRepository.addFood(food);

                if (productId != null) {

                    if (!customPortions.isEmpty()) {
                        Log.d(TAG, "Сохранение " + customPortions.size() + " кастомных порций для продукта " + productId);

                        for (Portion portion : customPortions) {
                            String portionId = foodRepository.addPortion(
                                    productId,
                                    portion.getName(),
                                    portion.getWeight()
                            );

                            if (portionId != null) {
                                Log.d(TAG, "Порция сохранена успешно: " + portion.getName());
                            } else {
                                Log.e(TAG, "Ошибка при сохранении порции: " + portion.getName());
                            }
                        }
                    }


                    if (scannedBarcode != null && !scannedBarcode.isEmpty()) {
                        Log.d(TAG, "Сохранение штрихкода: " + scannedBarcode + " для продукта " + productId);

                        try {

                            SupabaseClient supabaseClient = SupabaseClient.getInstance(
                                    Constants.SUPABASE_CLIENT_ID,
                                    Constants.SUPABASE_CLIENT_SECRET
                            );

                            SharedPreferences authPrefs = getSharedPreferences("auth_data", Context.MODE_PRIVATE);
                            String accessToken = authPrefs.getString("access_token", null);
                            String refreshToken = authPrefs.getString("refresh_token", null);

                            if (accessToken != null) {
                                supabaseClient.setUserToken(accessToken);
                                if (refreshToken != null) {
                                    supabaseClient.setRefreshToken(refreshToken);
                                }
                            }

                            SupabaseBarcodeRepository barcodeRepository = new SupabaseBarcodeRepository(supabaseClient, this);


                            Food foodWithId = new Food.Builder()
                                    .id(productId)
                                    .build();

                            boolean barcodeSaved = barcodeRepository.addBarcode(scannedBarcode, foodWithId);

                            if (barcodeSaved) {
                                Log.d(TAG, "Штрихкод сохранен успешно");
                            } else {
                                Log.e(TAG, "Ошибка при сохранении штрихкода");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при сохранении штрихкода: " + e.getMessage(), e);
                        }
                    }
                }


                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Прерывание ожидания", e);
                }

                runOnUiThread(() -> {
                    progressOverlay.setVisibility(View.GONE);

                    if (productId != null) {
                        String message = "Продукт успешно добавлен!";
                        if (!customPortions.isEmpty()) {
                            message += " (" + customPortions.size() + " порций)";
                        }

                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();


                        setResult(RESULT_OK);
                        finish();


                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                startIncrementalSync();
                            } catch (InterruptedException e) {
                                Log.e(TAG, "Прерывание синхронизации", e);
                            }
                        }).start();
                    } else {
                        Toast.makeText(this,
                                "Ошибка при добавлении продукта",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при сохранении продукта: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Ошибка: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }


    private void clearErrors() {
        tilProductName.setError(null);
        tilCategory.setError(null);
        tilSubcategory.setError(null);
        tilCalories.setError(null);
        tilProteins.setError(null);
        tilFats.setError(null);
        tilCarbs.setError(null);
    }


    private float parseFloatOrZero(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0f;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }


    private void startIncrementalSync() {
        new Thread(() -> {
            try {

                Thread.sleep(1000);

                Log.d(TAG, "Запуск инкрементальной синхронизации после добавления продукта");


                SupabaseClient supabaseClient = SupabaseClient.getInstance(
                        Constants.SUPABASE_CLIENT_ID,
                        Constants.SUPABASE_CLIENT_SECRET
                );


                SharedPreferences authPrefs = getSharedPreferences("auth_data", Context.MODE_PRIVATE);
                String accessToken = authPrefs.getString("access_token", null);
                String refreshToken = authPrefs.getString("refresh_token", null);

                if (accessToken != null) {
                    supabaseClient.setUserToken(accessToken);
                    if (refreshToken != null) {
                        supabaseClient.setRefreshToken(refreshToken);
                    }
                }


                SupabaseFoodRepository foodRepository = new SupabaseFoodRepository(supabaseClient, this);
                FoodSyncService foodSyncService = new FoodSyncService(this, foodRepository);


                foodSyncService.syncFoods(false, new FoodSyncService.SyncCallback() {
                    @Override
                    public void onSyncStarted() {
                        Log.d(TAG, "Инкрементальная синхронизация начата");
                    }

                    @Override
                    public void onSyncProgress(int current, int total) {
                        Log.d(TAG, "Прогресс синхронизации: " + current + "/" + total);
                    }

                    @Override
                    public void onSyncCompleted(int syncedCount) {
                        Log.d(TAG, "Инкрементальная синхронизация завершена. Синхронизировано: " + syncedCount);
                    }

                    @Override
                    public void onSyncError(String error) {
                        Log.w(TAG, "Ошибка инкрементальной синхронизации (не критично): " + error);


                    }
                });

            } catch (InterruptedException e) {
                Log.d(TAG, "Синхронизация прервана");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.w(TAG, "Ошибка при запуске инкрементальной синхронизации (не критично): " + e.getMessage());

            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BARCODE_SCAN && resultCode == RESULT_OK && data != null) {
            String barcode = data.getStringExtra("BARCODE");
            if (barcode != null && !barcode.isEmpty()) {
                scannedBarcode = barcode;
                etBarcode.setText(barcode);
                Log.d(TAG, "Получен штрихкод: " + barcode);
                Toast.makeText(this, "Штрихкод добавлен: " + barcode, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
