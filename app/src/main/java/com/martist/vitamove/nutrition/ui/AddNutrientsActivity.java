package com.martist.vitamove.nutrition.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.domain.events.FoodAddedEvent;
import com.martist.vitamove.nutrition.ui.model.Food;

import org.greenrobot.eventbus.EventBus;


public class AddNutrientsActivity extends BaseActivity {
    private static final String TAG = "AddNutrientsActivity";


    private TextInputEditText etVitaminA, etVitaminB1, etVitaminB2, etVitaminB3, etVitaminB5, etVitaminB6;
    private TextInputEditText etVitaminB9, etVitaminB12, etVitaminC, etVitaminD, etVitaminE, etVitaminK;


    private TextInputEditText etCalcium, etIron, etMagnesium, etPhosphorus, etPotassium, etSodium, etZinc;


    private TextInputEditText etCholesterol, etSaturatedFats;

    private String mealType;
    private FoodManager foodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_nutrients);


        mealType = getIntent().getStringExtra(Constants.EXTRA_MEAL_TYPE);

        if (mealType == null) {
            Log.e(TAG, "mealType is null!");
            Toast.makeText(this, "Ошибка: тип приема пищи не указан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        if (getWindow() != null) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorSurface));
        }

        foodManager = FoodManager.getInstance(this);

        initViews();
        setupToolbar();
    }


    private void initViews() {

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
    }


    private void setupToolbar() {
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());


        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());


        MaterialButton btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> validateAndAddNutrients());
    }


    private void validateAndAddNutrients() {

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


        if (vitaminA == 0 && vitaminB1 == 0 && vitaminB2 == 0 && vitaminB3 == 0 &&
                vitaminB5 == 0 && vitaminB6 == 0 && vitaminB9 == 0 && vitaminB12 == 0 &&
                vitaminC == 0 && vitaminD == 0 && vitaminE == 0 && vitaminK == 0 &&
                calcium == 0 && iron == 0 && magnesium == 0 && phosphorus == 0 &&
                potassium == 0 && sodium == 0 && zinc == 0 &&
                cholesterol == 0 && saturatedFats == 0) {

            Toast.makeText(this, "Заполните хотя бы одно поле", Toast.LENGTH_SHORT).show();
            return;
        }


        Food virtualFood = new Food.Builder()
                .id("virtual_nutrients_" + System.currentTimeMillis())
                .name("Быстрое добавление нутриентов")
                .category("Добавки")
                .subcategory("Нутриенты")
                .calories(0)
                .proteins(0)
                .fats(0)
                .carbs(0)
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
                .build();


        foodManager.addFoodToMeal(mealType, virtualFood, 100f, "грамм");


        EventBus.getDefault().post(new FoodAddedEvent(virtualFood, 1, mealType));

        Toast.makeText(this, "Нутриенты добавлены в прием пищи", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Нутриенты успешно добавлены в прием пищи: " + mealType);


        setResult(RESULT_OK);
        finish();
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
}
