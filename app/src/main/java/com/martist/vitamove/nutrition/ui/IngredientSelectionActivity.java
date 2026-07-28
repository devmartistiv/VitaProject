package com.martist.vitamove.nutrition.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.ui.adapter.IngredientSelectionAdapter;
import com.martist.vitamove.nutrition.ui.model.Dish;
import com.martist.vitamove.nutrition.ui.model.Food;

import java.util.ArrayList;
import java.util.List;

public class IngredientSelectionActivity extends BaseActivity {
    private static final String TAG = "IngredientSelection";

    private IngredientSelectionAdapter adapter;
    private ActivityResultLauncher<Intent> portionSizeLauncher;
    private ActivityResultLauncher<Intent> barcodeScanLauncher;
    private SearchView searchView;
    private FoodManager foodManager;
    private RecyclerView foodsList;
    private List<Food> allFoods;
    private MaterialButton btnAddSelected;
    private TextView selectedCountText;

    private List<Dish.DishIngredient> selectedIngredients = new ArrayList<>();
    private Food currentSelectedFood = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_selection);

        try {
            foodManager = FoodManager.getInstance(this);

            setupToolbar();
            setupPortionSizeLauncher();
            setupBarcodeScanLauncher();
            setupRecyclerView();
            setupSearchView();
            setupBottomBar();


            showFavoriteFoods();


            loadAllFoods();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing IngredientSelectionActivity: " + e.getMessage());
            Toast.makeText(this, "Ошибка при инициализации", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Выбор ингредиентов");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());


        View barcodeButton = findViewById(R.id.barcode_scan_button);
        barcodeButton.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100);
                Intent intent = new Intent(IngredientSelectionActivity.this, BarcodeScannerActivity.class);
                barcodeScanLauncher.launch(intent);
            }).start();
        });
    }

    private void setupRecyclerView() {
        foodsList = findViewById(R.id.food_recycler);
        foodsList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new IngredientSelectionAdapter(new ArrayList<>(), new IngredientSelectionAdapter.OnFoodSelectListener() {
            @Override
            public void onFoodSelected(Food food) {

                currentSelectedFood = food;
                Intent intent = new Intent(IngredientSelectionActivity.this, PortionSizeActivity.class);
                intent.putExtra(Constants.EXTRA_FOOD, food);
                intent.putExtra("is_ingredient_selection", true);


                if (food.getPortions() != null && !food.getPortions().isEmpty()) {
                    String defaultPortionName = food.getPortions().get(0).getName();
                    intent.putExtra("portion_name", defaultPortionName);
                    intent.putExtra("portion_quantity", 1f);
                }

                portionSizeLauncher.launch(intent);
            }

            @Override
            public void onFoodRemoved(Food food) {

                selectedIngredients.removeIf(ingredient -> ingredient.getFood().getId().equals(food.getId()));
                adapter.notifyDataSetChanged();
                updateBottomBar();
            }

            @Override
            public void onFoodAddedDirectly(Food food, float quantity, String portionName) {


                selectedIngredients.removeIf(ingredient -> ingredient.getFood().getId().equals(food.getId()));


                Dish.DishIngredient newIngredient = new Dish.DishIngredient(food, quantity, portionName);
                selectedIngredients.add(newIngredient);


                adapter.notifyDataSetChanged();
                updateBottomBar();
            }
        }, selectedIngredients);

        foodsList.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView = findViewById(R.id.search_edit);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Поиск продуктов");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    showFavoriteFoods();
                } else {
                    performSearch(newText);
                }
                return true;
            }
        });
    }

    private void setupBottomBar() {
        btnAddSelected = findViewById(R.id.btn_add_selected);
        selectedCountText = findViewById(R.id.selected_count_text);

        btnAddSelected.setOnClickListener(v -> {

            Intent resultIntent = new Intent();
            resultIntent.putParcelableArrayListExtra("selected_ingredients",
                    new ArrayList<>(selectedIngredients));
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        updateBottomBar();
    }

    private void updateBottomBar() {
        int count = selectedIngredients.size();
        if (count > 0) {
            selectedCountText.setText("Выбрано: " + count);
            selectedCountText.setVisibility(View.VISIBLE);
            btnAddSelected.setVisibility(View.VISIBLE);
            btnAddSelected.setTextColor(getColor(R.color.white_only));
            btnAddSelected.setText("Добавить (" + count + ")");
        } else {
            selectedCountText.setVisibility(View.GONE);
            btnAddSelected.setVisibility(View.GONE);
        }
    }

    private void setupPortionSizeLauncher() {
        portionSizeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentSelectedFood != null) {
                        Intent data = result.getData();
                        if (data != null) {

                            float quantity = data.getFloatExtra("portion_quantity", 1f);
                            String portionName = data.getStringExtra("portion_name");

                            if (portionName == null) {
                                portionName = currentSelectedFood.isLiquid() ? "мл" : "грамм";
                            }


                            Dish.DishIngredient ingredient = new Dish.DishIngredient(currentSelectedFood, quantity, portionName);


                            boolean found = false;
                            for (int i = 0; i < selectedIngredients.size(); i++) {
                                if (selectedIngredients.get(i).getFood().getId() == currentSelectedFood.getId()) {

                                    selectedIngredients.set(i, ingredient);
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                selectedIngredients.add(ingredient);
                            }


                            adapter.notifyDataSetChanged();
                            updateBottomBar();

                            Log.d(TAG, "Добавлен ингредиент: " + currentSelectedFood.getName() +
                                    " (" + quantity + " " + portionName + ")");
                        }
                    }
                    currentSelectedFood = null;
                });
    }

    private void setupBarcodeScanLauncher() {
        barcodeScanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.hasExtra(Constants.EXTRA_FOOD)) {
                            Food scannedFood = data.getParcelableExtra(Constants.EXTRA_FOOD);
                            if (scannedFood != null) {

                                currentSelectedFood = scannedFood;
                                Intent intent = new Intent(this, PortionSizeActivity.class);
                                intent.putExtra(Constants.EXTRA_FOOD, scannedFood);
                                intent.putExtra("is_ingredient_selection", true);
                                portionSizeLauncher.launch(intent);
                            }
                        }
                    }
                });
    }

    private void loadAllFoods() {
        new Thread(() -> {
            try {
                allFoods = foodManager.getAllFoods();
                Log.d(TAG, "Успешно загружено " + allFoods.size() + " продуктов для поиска");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке продуктов: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showFavoriteFoods() {
        new Thread(() -> {
            try {
                List<Food> favoriteFoods = foodManager.getFavoriteFoods();

                if (favoriteFoods != null && !favoriteFoods.isEmpty()) {
                    Log.d(TAG, "Показываем избранные продукты: " + favoriteFoods.size());
                    updateFoodList(favoriteFoods);

                    runOnUiThread(() -> {
                        findViewById(R.id.empty_results_text).setVisibility(View.GONE);
                        foodsList.setVisibility(View.VISIBLE);
                    });
                } else {
                    Log.d(TAG, "Избранных продуктов нет, показываем сообщение");
                    runOnUiThread(() -> {
                        adapter.updateFoods(new ArrayList<>());
                        foodsList.setVisibility(View.GONE);

                        TextView emptyText = findViewById(R.id.empty_results_text);
                        emptyText.setText("Добавьте продукт в избранное, чтобы он отображался здесь");
                        emptyText.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке продуктов: " + e.getMessage());
                runOnUiThread(() -> {
                    adapter.updateFoods(new ArrayList<>());
                    foodsList.setVisibility(View.GONE);
                    TextView emptyText = findViewById(R.id.empty_results_text);
                    emptyText.setText("Ошибка при загрузке избранных продуктов");
                    emptyText.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            showFavoriteFoods();
            return;
        }

        new Thread(() -> {
            try {
                List<Food> localResults = foodManager.searchFoods(query);
                if (!localResults.isEmpty()) {
                    Log.d(TAG, "Найдены продукты в локальной базе: " + localResults.size());
                    updateFoodList(localResults);

                    runOnUiThread(() -> {
                        findViewById(R.id.empty_results_text).setVisibility(View.GONE);
                        foodsList.setVisibility(View.VISIBLE);
                    });
                    return;
                }


                Log.d(TAG, "Продукты не найдены в локальной базе");
                runOnUiThread(() -> {
                    adapter.updateFoods(new ArrayList<>());
                    foodsList.setVisibility(View.GONE);

                    TextView emptyText = findViewById(R.id.empty_results_text);
                    emptyText.setText("Продукты не найдены");
                    emptyText.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    adapter.updateFoods(new ArrayList<>());
                    foodsList.setVisibility(View.GONE);
                    TextView emptyText = findViewById(R.id.empty_results_text);
                    emptyText.setText("Ошибка при поиске");
                    emptyText.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void updateFoodList(List<Food> foodList) {
        Log.d(TAG, "Updating food list with " + foodList.size() + " items");
        runOnUiThread(() -> {
            adapter.updateFoods(foodList);
            foodsList.scrollToPosition(0);
        });
    }
}