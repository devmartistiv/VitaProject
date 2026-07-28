package com.martist.vitamove.nutrition.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.managers.DishManager;
import com.martist.vitamove.nutrition.ui.adapter.DishIngredientAdapter;
import com.martist.vitamove.nutrition.ui.model.Dish;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class DishConstructorActivity extends BaseActivity {
    private static final String TAG = "DishConstructor";

    private EditText editDishName;
    private EditText editDishDescription;
    private RecyclerView ingredientsList;
    private Button btnAddIngredient;
    private Button btnSaveDish;
    private TextView emptyIngredientsText;

    private DishIngredientAdapter ingredientAdapter;
    private Dish currentDish;
    private DishManager dishManager;
    private ActivityResultLauncher<Intent> ingredientSelectionLauncher;
    private ActivityResultLauncher<Intent> portionSizeLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dish_constructor);

        dishManager = DishManager.getInstance(this);

        initViews();
        setupToolbar();
        setupIngredientSelectionLauncher();
        setupPortionSizeLauncher();


        String dishId = getIntent().getStringExtra("dish_id");
        Log.d(TAG, "Получен dish_id из Intent: " + dishId);

        if (!Objects.equals(dishId, "")) {

            Log.d(TAG, "Запускаем режим редактирования для блюда с ID: " + dishId);
            loadDishForEditing(dishId);
        } else {

            Log.d(TAG, "Запускаем режим создания нового блюда");
            currentDish = new Dish();
            setupRecyclerView();
            setupListeners();
        }
    }


    private void loadDishForEditing(String dishId) {
        dishManager.getDishWithIngredients(dishId, new DishManager.OnDishLoadedListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onDishLoaded(Dish dish) {
                runOnUiThread(() -> {
                    if (dish != null) {
                        currentDish = dish;
                        Log.d(TAG, "Загружено блюдо для редактирования: " + dish.getName());


                        editDishName.setText(dish.getName());
                        editDishDescription.setText(dish.getDescription());


                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Редактирование блюда");
                        }
                        btnSaveDish.setText("Сохранить изменения");
                        btnSaveDish.setTextColor(getColor(R.color.white_only));


                        setupRecyclerView();
                        setupListeners();

                        Log.d(TAG, "UI настроен для редактирования. Ингредиентов: " +
                                currentDish.getIngredients().size());
                    } else {
                        currentDish = new Dish();
                        setupRecyclerView();
                        setupListeners();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {

                    currentDish = new Dish();
                    setupRecyclerView();
                    setupListeners();
                });
            }
        });
    }

    private void initViews() {
        editDishName = findViewById(R.id.edit_dish_name);
        editDishDescription = findViewById(R.id.edit_dish_description);
        ingredientsList = findViewById(R.id.ingredients_list);
        btnAddIngredient = findViewById(R.id.btn_add_ingredient);
        btnSaveDish = findViewById(R.id.btn_save_dish);
        emptyIngredientsText = findViewById(R.id.empty_ingredients_text);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Конструктор блюда");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupIngredientSelectionLauncher() {
        ingredientSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.hasExtra("selected_ingredients")) {

                            ArrayList<Dish.DishIngredient> selectedIngredients =
                                    data.getParcelableArrayListExtra("selected_ingredients");

                            if (selectedIngredients != null && !selectedIngredients.isEmpty()) {

                                for (Dish.DishIngredient ingredient : selectedIngredients) {

                                    boolean found = false;
                                    List<Dish.DishIngredient> currentIngredients = currentDish.getIngredients();
                                    for (int i = 0; i < currentIngredients.size(); i++) {
                                        if (currentIngredients.get(i).getFood().getId() == ingredient.getFood().getId()) {

                                            currentIngredients.set(i, ingredient);
                                            found = true;
                                            break;
                                        }
                                    }

                                    if (!found) {
                                        currentDish.addIngredient(ingredient.getFood(),
                                                ingredient.getQuantity(), ingredient.getPortionName());
                                    }
                                }


                                ingredientAdapter.notifyDataSetChanged();
                                updateSaveButtonState();

                                Log.d(TAG, "Добавлено ингредиентов: " + selectedIngredients.size());
                            }
                        }
                    }
                });
    }

    private void setupPortionSizeLauncher() {
        portionSizeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {

                            int ingredientPosition = data.getIntExtra("ingredient_position", -1);
                            float newQuantity = data.getFloatExtra("portion_quantity", 0f);
                            String newPortionName = data.getStringExtra("portion_name");

                            Log.d(TAG, "Обновляем ингредиент на позиции " + ingredientPosition +
                                    ": количество=" + newQuantity + ", порция=" + newPortionName);

                            if (ingredientPosition >= 0 && ingredientPosition < currentDish.getIngredients().size()) {

                                Dish.DishIngredient ingredient = currentDish.getIngredients().get(ingredientPosition);
                                ingredient.setQuantity(newQuantity);
                                ingredient.setPortionName(newPortionName != null ? newPortionName : "грамм");


                                ingredientAdapter.notifyItemChanged(ingredientPosition);
                                updateSaveButtonState();

                                Log.d(TAG, "Ингредиент обновлен: " + ingredient.getFood().getName() +
                                        " - " + newQuantity + " " + ingredient.getPortionName());
                            }
                        }
                    }
                });
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView() - Ингредиентов в currentDish: " +
                currentDish.getIngredients().size());


        for (int i = 0; i < currentDish.getIngredients().size(); i++) {
            Dish.DishIngredient ingredient = currentDish.getIngredients().get(i);
            Log.d(TAG, "Ингредиент " + i + ": " + ingredient.getFood().getName() +
                    " - " + ingredient.getQuantity() + " " + ingredient.getPortionName());
        }

        ingredientAdapter = new DishIngredientAdapter(
                currentDish.getIngredients(),
                position -> {

                    currentDish.removeIngredient(position);
                    ingredientAdapter.notifyItemRemoved(position);
                    updateSaveButtonState();
                }
        );


        ingredientAdapter.setOnIngredientClickListener((ingredient, position) -> {
            Log.d(TAG, "Клик по ингредиенту: " + ingredient.getFood().getName() +
                    " на позиции " + position);


            Intent intent = new Intent(this, PortionSizeActivity.class);
            intent.putExtra(Constants.EXTRA_FOOD, ingredient.getFood());
            intent.putExtra("ingredient_position", position);
            intent.putExtra("is_ingredient_selection", true);
            intent.putExtra("portion_quantity", ingredient.getQuantity());
            intent.putExtra("portion_name", ingredient.getPortionName());
            intent.putExtra(Constants.EXTRA_PORTION_SIZE, (int) ingredient.getQuantity());

            portionSizeLauncher.launch(intent);
        });

        ingredientsList.setLayoutManager(new LinearLayoutManager(this));
        ingredientsList.setAdapter(ingredientAdapter);


        ingredientAdapter.notifyDataSetChanged();


        updateSaveButtonState();

        Log.d(TAG, "setupRecyclerView() завершен");
    }

    private void setupListeners() {

        editDishName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSaveButtonState();
            }
        });

        btnAddIngredient.setOnClickListener(v -> {

            Intent intent = new Intent(DishConstructorActivity.this, IngredientSelectionActivity.class);
            ingredientSelectionLauncher.launch(intent);
        });

        btnSaveDish.setOnClickListener(v -> {
            if (editDishName.getText().length() == 0) {
                Toast.makeText(this, "Вы не ввели название блюда", Toast.LENGTH_SHORT).show();
            } else {
                saveDish();
            }

        });
    }

    private void saveDish() {
        String name = editDishName.getText().toString().trim();
        String description = editDishDescription.getText().toString().trim();

        if (name.isEmpty()) {
            editDishName.setError("Введите название блюда");
            return;
        }

        if (currentDish.getIngredients().isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы один ингредиент", Toast.LENGTH_SHORT).show();
            return;
        }

        currentDish.setName(name);
        currentDish.setDescription(description);


        dishManager.saveDish(currentDish, new DishManager.OnDishSavedListener() {
            @Override
            public void onDishSaved(Dish dish) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Блюдо сохранено: " + dish.getName());
                    Toast.makeText(DishConstructorActivity.this, "Блюдо сохранено", Toast.LENGTH_SHORT).show();


                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Ошибка при сохранении блюда: " + error);
                    Toast.makeText(DishConstructorActivity.this, "Ошибка при сохранении: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateSaveButtonState() {
        boolean hasIngredients = !currentDish.getIngredients().isEmpty();
        if (hasIngredients) {
            emptyIngredientsText.setVisibility(View.GONE);
            ingredientsList.setVisibility(View.VISIBLE);
        } else {
            emptyIngredientsText.setVisibility(View.VISIBLE);
            ingredientsList.setVisibility(View.GONE);
        }
    }
} 