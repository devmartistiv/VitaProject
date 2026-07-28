package com.martist.vitamove.nutrition.ui;

import static com.martist.vitamove.VitaMoveApplication.context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.local.entities.RecentFoodEntity;
import com.martist.vitamove.nutrition.data.managers.DishManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.domain.events.FoodAddedEvent;
import com.martist.vitamove.nutrition.ui.adapter.DishAdapter;
import com.martist.vitamove.nutrition.ui.adapter.FoodAdapter;
import com.martist.vitamove.nutrition.ui.adapter.RecentFoodAdapter;
import com.martist.vitamove.nutrition.ui.model.Dish;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Portion;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FoodSelectionActivity extends BaseActivity {
    private static final String TAG = "FoodSelection";
    private FoodAdapter foodAdapter;
    private RecentFoodAdapter recentFoodAdapter;
    private DishAdapter dishAdapter;
    private ActivityResultLauncher<Intent> portionSizeLauncher;
    private ActivityResultLauncher<Intent> barcodeScanLauncher;
    private ActivityResultLauncher<Intent> dishConstructorLauncher;
    private ActivityResultLauncher<Intent> createProductLauncher;
    private ActivityResultLauncher<Intent> addNutrientsLauncher;
    private SearchView searchView;
    private FoodManager foodManager;
    private DishManager dishManager;
    private RecyclerView foodsList;
    private List<Food> allFoods;
    private String mealType;
    private TabLayout tabLayout;
    private boolean isSearchMode = false;
    private boolean isDishesTab = false;


    private boolean isMultiSelectionMode = false;
    private AppDatabase appDatabase;
    private Set<String> selectedFoodIds = new HashSet<>();
    private Map<String, Food> selectedFoodsMap = new HashMap<>();
    private View multiSelectionPanel;
    private com.google.android.material.button.MaterialButton addSelectedButton;
    private com.google.android.material.button.MaterialButton cancelSelectionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_selection);
        this.appDatabase = AppDatabase.getInstance(context);
        mealType = getIntent().getStringExtra(Constants.EXTRA_MEAL_TYPE);
        Log.d(TAG, "Received mealType: " + mealType);

        if (mealType == null) {
            Log.e(TAG, "mealType is null! Intent extras: " + getIntent().getExtras());
            Toast.makeText(this, "Ошибка: тип приема пищи не указан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        setupToolbar();


        View barcodeButton = findViewById(R.id.barcode_scan_button);
        barcodeButton.setOnClickListener(v -> {

            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100);

                Intent intent = new Intent(FoodSelectionActivity.this, BarcodeScannerActivity.class);

                intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
                barcodeScanLauncher.launch(intent);
            }).start();
        });

        try {
            foodManager = FoodManager.getInstance(this);
            dishManager = DishManager.getInstance(this);

            setupPortionSizeLauncher();
            setupBarcodeScanLauncher();
            setupDishConstructorLauncher();
            setupCreateProductLauncher();
            setupAddNutrientsLauncher();
            setupRecyclerView();
            setupSearchView();
            setupTabLayout();
            setupMultiSelectionPanel();
            exitMultiSelectionMode();

            showRecentFoods();


            loadAllFoods();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FoodSelectionActivity: " + e.getMessage());
            Toast.makeText(this, "Ошибка при инициализации", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (getWindow() != null) {
            getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.colorSurface));
        }
    }


    @SuppressLint("ResourceAsColor")
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);


        String title = getMealDisplayName(mealType);

        if (title != null && !title.isEmpty()) {
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
        }
        if (Build.VERSION.SDK_INT >= 24) {
            toolbar.setTitle(Html.fromHtml("<b>" + title + "</b>", 0));

        } else {
            toolbar.setTitle(Html.fromHtml("<b>" + title + "</b>"));

        }


        int titleColor = ContextCompat.getColor(this, R.color.colorOnSurface);
        toolbar.setTitleTextColor(titleColor);


        toolbar.setNavigationOnClickListener(v -> finish());


        View addButton = findViewById(R.id.toolbar_add_button);
        addButton.setOnClickListener(v -> showAddMenu(v));
    }


    private void showAddMenu(View anchor) {

        Context wrapper = new android.view.ContextThemeWrapper(this, R.style.ModernPopupMenu);
        PopupMenu popup = new PopupMenu(wrapper, anchor, android.view.Gravity.END);
        popup.getMenuInflater().inflate(R.menu.food_selection_add_menu, popup.getMenu());


        int iconColor = ContextCompat.getColor(this, R.color.colorAccent);
        for (int i = 0; i < popup.getMenu().size(); i++) {
            MenuItem item = popup.getMenu().getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(iconColor);
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_create_product) {

                Intent intent = new Intent(this, CreateProductActivity.class);
                createProductLauncher.launch(intent);
                return true;
            } else if (itemId == R.id.menu_create_dish) {

                Intent intent = new Intent(this, DishConstructorActivity.class);
                dishConstructorLauncher.launch(intent);
                return true;
            } else if (itemId == R.id.menu_add_vitamins) {

                Intent intent = new Intent(this, AddNutrientsActivity.class);
                intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
                addNutrientsLauncher.launch(intent);
                return true;
            }

            return false;
        });

        popup.show();
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

                    isSearchMode = false;
                    tabLayout.setVisibility(View.VISIBLE);
                    showRecentFoods();
                } else {

                    isSearchMode = true;
                    tabLayout.setVisibility(View.GONE);

                    foodsList.setAdapter(foodAdapter);
                    performSearch(newText);
                }
                return true;
            }
        });
    }

    private void setupTabLayout() {
        tabLayout = findViewById(R.id.tab_layout);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {

                    isDishesTab = false;
                    showRecentFoods();
                } else if (tab.getPosition() == 1) {

                    isDishesTab = false;
                    showFavoriteFoods();
                } else if (tab.getPosition() == 2) {

                    isDishesTab = true;
                    showMyDishes();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        foodsList = findViewById(R.id.food_recycler);
        foodsList.setLayoutManager(new LinearLayoutManager(this));


        foodAdapter = new FoodAdapter(new ArrayList<>(), food -> {

            Intent intent = new Intent(this, PortionSizeActivity.class);
            intent.putExtra(Constants.EXTRA_FOOD, food);
            intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);


            if (food.getPortions() != null && !food.getPortions().isEmpty()) {

                String defaultPortionName = food.getPortions().get(0).getName();
                intent.putExtra("portion_name", defaultPortionName);

                intent.putExtra("portion_quantity", 1f);
            }


            String selectedDateStr = foodManager.getSelectedDateFormatted();
            intent.putExtra(Constants.EXTRA_SELECTED_DATE, selectedDateStr);
            portionSizeLauncher.launch(intent);
        });


        foodAdapter.setOnFoodAddButtonClickListener(food -> {
            if (!isMultiSelectionMode) {


                enterMultiSelectionMode(food);
            } else {

                toggleFoodSelection(food);
            }
        });


        recentFoodAdapter = new RecentFoodAdapter(new ArrayList<>(), (recentFood, food) -> {


            if (food != null) {
                Intent intent = new Intent(this, PortionSizeActivity.class);
                intent.putExtra(Constants.EXTRA_FOOD, food);
                intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);


                intent.putExtra("portion_name", recentFood.getPortionName());
                intent.putExtra("portion_quantity", recentFood.getQuantity());


                String selectedDateStr = foodManager.getSelectedDateFormatted();
                intent.putExtra(Constants.EXTRA_SELECTED_DATE, selectedDateStr);
                portionSizeLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Продукт не найден", Toast.LENGTH_SHORT).show();
            }
        });


        recentFoodAdapter.setOnRecentFoodAddButtonClickListener(food -> {
            if (!isMultiSelectionMode) {

                enterMultiSelectionMode(food);
            } else {

                toggleFoodSelection(food);
            }
        });


        dishAdapter = new DishAdapter(new ArrayList<>(), dish -> {
            Log.d(TAG, "Клик по блюду для редактирования: " + dish.getName());


            Intent intent = new Intent(this, DishConstructorActivity.class);
            intent.putExtra("dish_id", dish.getId());
            dishConstructorLauncher.launch(intent);
        });


        dishAdapter.setOnDishAddListener(dish -> {
            Log.d(TAG, "Добавление блюда в прием пищи: " + dish.getName());


            addDishToMeal(dish);
        });


        dishAdapter.setOnDishLongClickListener(dish -> {
            Log.d(TAG, "Долгое нажатие на блюдо для удаления: " + dish.getName());
            showDeleteDishDialog(dish);
        });


        foodsList.setAdapter(foodAdapter);
    }


    private void loadAllFoods() {
        new Thread(() -> {
            try {

                allFoods = foodManager.getAllFoods();


                if (allFoods != null && !allFoods.isEmpty()) {
                    Log.d(TAG, "Загружены продукты. Несколько примеров:");
                    int count = Math.min(allFoods.size(), 10);
                    for (int i = 0; i < count; i++) {
                        Food food = allFoods.get(i);
                        Log.d(TAG, "  " + i + ": '" + food.getName() + "', категория: '" +
                                food.getCategory() + "', подкатегория: '" + food.getSubcategory() + "'");
                    }


                    Log.d(TAG, "Продукты, содержащие 'кола' в названии:");
                    int kolaCount = 0;
                    for (Food food : allFoods) {
                        if (food.getName().toLowerCase().contains("кола")) {
                            Log.d(TAG, "  - '" + food.getName() + "'");
                            kolaCount++;
                        }
                    }
                    Log.d(TAG, "Всего продуктов с 'кола' в названии: " + kolaCount);
                }

                Log.d(TAG, "Успешно загружено " + allFoods.size() + " продуктов для поиска");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке продуктов: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    private void showRecentFoods() {
        Log.d(TAG, "Показываем недавние продукты");


        runOnUiThread(() -> foodsList.setAdapter(recentFoodAdapter));

        new Thread(() -> {
            try {
                List<RecentFoodEntity> recentFoodEntities = foodManager.getRecentFoods();

                if (recentFoodEntities != null && !recentFoodEntities.isEmpty()) {
                    Log.d(TAG, "Загружено недавних продуктов: " + recentFoodEntities.size());


                    List<RecentFoodAdapter.RecentFoodWithCalories> recentWithCalories = new ArrayList<>();
                    for (RecentFoodEntity recentEntity : recentFoodEntities) {
                        Food food = foodManager.getFoodById(recentEntity.getFoodId());
                        if (food != null) {
                            int calculatedCalories = calculateCaloriesForPortion(food, recentEntity.getQuantity(), recentEntity.getPortionName());
                            recentWithCalories.add(new RecentFoodAdapter.RecentFoodWithCalories(recentEntity, calculatedCalories, food));
                        }
                    }

                    updateRecentFoodList(recentWithCalories);

                    runOnUiThread(() -> {
                        findViewById(R.id.empty_results_text).setVisibility(View.GONE);
                        foodsList.setVisibility(View.VISIBLE);
                        if (!isSearchMode) {
                            tabLayout.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    Log.d(TAG, "Недавних продуктов нет");

                    runOnUiThread(() -> {
                        recentFoodAdapter.updateRecentFoods(new ArrayList<>());
                        foodsList.setVisibility(View.GONE);

                        TextView emptyText = findViewById(R.id.empty_results_text);
                        emptyText.setText("Добавьте продукт в прием пищи, чтобы он отображался в недавних");
                        emptyText.setVisibility(View.VISIBLE);

                        if (!isSearchMode) {
                            tabLayout.setVisibility(View.VISIBLE);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке недавних продуктов: " + e.getMessage());
                runOnUiThread(() -> {
                    recentFoodAdapter.updateRecentFoods(new ArrayList<>());
                    foodsList.setVisibility(View.GONE);
                    TextView emptyText = findViewById(R.id.empty_results_text);
                    emptyText.setText("Ошибка при загрузке недавних продуктов");
                    emptyText.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }


    private void showFavoriteFoods() {

        runOnUiThread(() -> foodsList.setAdapter(foodAdapter));

        new Thread(() -> {
            try {

                List<Food> favoriteFoods = foodManager.getFavoriteFoods();

                if (favoriteFoods != null && !favoriteFoods.isEmpty()) {

                    Log.d(TAG, "Показываем избранные продукты: " + favoriteFoods.size());
                    updateFoodList(favoriteFoods);


                    runOnUiThread(() -> {
                        findViewById(R.id.empty_results_text).setVisibility(View.GONE);
                        foodsList.setVisibility(View.VISIBLE);
                        if (!isSearchMode) {
                            tabLayout.setVisibility(View.VISIBLE);
                        }
                    });
                } else {

                    Log.d(TAG, "Избранных продуктов нет, показываем сообщение");

                    runOnUiThread(() -> {

                        foodAdapter.updateFoods(new ArrayList<>());
                        foodsList.setVisibility(View.GONE);


                        TextView emptyText = findViewById(R.id.empty_results_text);
                        emptyText.setText("Добавьте продукт в избранное, чтобы он отображался здесь");
                        emptyText.setVisibility(View.VISIBLE);

                        if (!isSearchMode) {
                            tabLayout.setVisibility(View.VISIBLE);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке продуктов для показа: " + e.getMessage());

                runOnUiThread(() -> {
                    foodAdapter.updateFoods(new ArrayList<>());
                    foodsList.setVisibility(View.GONE);
                    TextView emptyText = findViewById(R.id.empty_results_text);
                    emptyText.setText("Ошибка при загрузке избранных продуктов");
                    emptyText.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }


    private void showMyDishes() {

        foodsList.setAdapter(dishAdapter);

        dishManager.getAllDishes(new DishManager.OnDishesLoadedListener() {
            @Override
            public void onDishesLoaded(List<Dish> dishes) {
                runOnUiThread(() -> {
                    if (dishes != null && !dishes.isEmpty()) {

                        dishAdapter.updateDishes(dishes);
                        foodsList.setVisibility(View.VISIBLE);
                        findViewById(R.id.empty_results_text).setVisibility(View.GONE);
                        Log.d(TAG, "Показываем блюда: " + dishes.size());
                    } else {

                        dishAdapter.updateDishes(new ArrayList<>());
                        foodsList.setVisibility(View.GONE);

                        TextView emptyText = findViewById(R.id.empty_results_text);
                        emptyText.setText("У Вас пока нет созданных блюд.\nНажмите кнопку \"+\" чтобы создать первое блюдо.");
                        emptyText.setVisibility(View.VISIBLE);
                    }

                    if (!isSearchMode) {
                        tabLayout.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Ошибка при загрузке блюд: " + error);
                    dishAdapter.updateDishes(new ArrayList<>());
                    foodsList.setVisibility(View.GONE);

                    TextView emptyText = findViewById(R.id.empty_results_text);
                    emptyText.setText("Ошибка при загрузке блюд");
                    emptyText.setVisibility(View.VISIBLE);

                    if (!isSearchMode) {
                        tabLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }


    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            Log.d(TAG, "Пустой запрос, показываем избранные продукты");
            showFavoriteFoods();
            return;
        }

        Log.d(TAG, "Поиск продукта: " + query);


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
                } else {

                    Log.d(TAG, "Продукты не найдены в локальной базе");
                    runOnUiThread(() -> {

                        foodAdapter.updateFoods(new ArrayList<>());
                        foodsList.setVisibility(View.GONE);

                        TextView emptyText = findViewById(R.id.empty_results_text);
                        emptyText.setText("Продукты не найдены");
                        emptyText.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при поиске продуктов: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    foodAdapter.updateFoods(new ArrayList<>());
                    foodsList.setVisibility(View.GONE);
                    TextView emptyText = findViewById(R.id.empty_results_text);
                    emptyText.setText("Ошибка при поиске");
                    emptyText.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void setupPortionSizeLauncher() {
        portionSizeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

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

                                Intent intent = new Intent(this, PortionSizeActivity.class);
                                intent.putExtra(Constants.EXTRA_FOOD, scannedFood);
                                intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);

                                String selectedDateStr = foodManager.getSelectedDateFormatted();
                                intent.putExtra(Constants.EXTRA_SELECTED_DATE, selectedDateStr);
                                portionSizeLauncher.launch(intent);
                            }
                        }
                    }
                });
    }

    private void setupDishConstructorLauncher() {
        dishConstructorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {

                        showMyDishes();
                    }
                });
    }

    private void setupCreateProductLauncher() {
        createProductLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {


                        if (!isDishesTab && !isSearchMode) {
                            TabLayout.Tab selectedTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
                            if (selectedTab != null && selectedTab.getPosition() == 1) {
                                showFavoriteFoods();
                            }
                        }

                        loadAllFoods();
                        Toast.makeText(this, "Продукт добавлен в базу данных", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupAddNutrientsLauncher() {
        addNutrientsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {

                        finish();
                    }
                });
    }

    private void updateFoodList(List<Food> foodList) {
        Log.d(TAG, "Updating food list with " + foodList.size() + " items");
        runOnUiThread(() -> {
            foodAdapter.updateFoods(foodList);


            if (isMultiSelectionMode) {
                foodAdapter.setMultiSelectionMode(true);
                foodAdapter.setSelectedFoods(selectedFoodIds);
            }


            foodsList.scrollToPosition(0);
        });
    }


    private void updateRecentFoodList(List<RecentFoodAdapter.RecentFoodWithCalories> recentFoodList) {
        Log.d(TAG, "Обновляем список недавних продуктов: " + recentFoodList.size() + " элементов");
        runOnUiThread(() -> {
            recentFoodAdapter.updateRecentFoods(recentFoodList);


            if (isMultiSelectionMode) {
                recentFoodAdapter.setMultiSelectionMode(true);
                recentFoodAdapter.setSelectedFoods(selectedFoodIds);
            }


            foodsList.scrollToPosition(0);
        });
    }


    private int calculateCaloriesForPortion(Food food, float quantity, String portionName) {
        try {

            float baseCalories = food.getCalories();

            if (portionName.equals("грамм") || portionName.equals("мл")) {

                return Math.round((baseCalories * quantity) / 100f);
            } else {

                if (food.getPortions() != null) {
                    for (Portion portion : food.getPortions()) {
                        if (portion.getName().toLowerCase().contains(portionName.toLowerCase())) {

                            float portionWeight = portion.getWeight();
                            return Math.round((baseCalories * quantity * portionWeight) / 100f);
                        }
                    }
                }


                return Math.round((baseCalories * quantity) / 100f);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при расчете калорий: " + e.getMessage());

            return Math.round(food.getCalories() * quantity / 100f);
        }
    }


    private void addDishToMeal(Dish dish) {
        if (dish == null) {
            Toast.makeText(this, "Ошибка: блюдо не найдено", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mealType == null) {
            Toast.makeText(this, "Ошибка: не указан прием пищи", Toast.LENGTH_SHORT).show();
            return;
        }


        Food virtualFood = createFoodFromDish(dish);
        if (virtualFood == null) {
            Toast.makeText(this, "Ошибка: не удалось подготовить блюдо", Toast.LENGTH_SHORT).show();
            return;
        }


        float totalWeight = getTotalDishWeight(dish);

        Log.d(TAG, String.format("Добавляем блюдо '%s' в прием пищи '%s', общий вес: %.1fг, калории: %.1f",
                dish.getName(), mealType, totalWeight, dish.getTotalCalories()));


        foodManager.addFoodToMeal(mealType, virtualFood, totalWeight, "грамм");


        foodManager.addToRecents(virtualFood, totalWeight, "грамм");


        Toast.makeText(this, "Блюдо \"" + dish.getName() + "\" добавлено в " + getMealDisplayName(mealType),
                Toast.LENGTH_SHORT).show();


        EventBus.getDefault().post(new FoodAddedEvent(virtualFood, totalWeight, mealType));


        setResult(RESULT_OK);
        finish();
    }


    private Food createFoodFromDish(Dish dish) {
        if (dish == null || dish.getIngredients() == null || dish.getIngredients().isEmpty()) {
            Log.w(TAG, "Попытка создать Food из пустого блюда");
            return null;
        }


        float totalWeightGrams = getTotalDishWeight(dish);

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

        Log.d(TAG, String.format("Блюдо '%s': общий вес %.1fг, калории %.1f → %.1f/100г",
                dish.getName(), totalWeightGrams, totalCalories, per100gCalories));

        return new Food.Builder()
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
    }


    private float getTotalDishWeight(Dish dish) {
        float totalWeightGrams = 0;
        for (Dish.DishIngredient ingredient : dish.getIngredients()) {
            totalWeightGrams += ingredient.getTotalWeightInGrams();
        }
        return totalWeightGrams;
    }


    private String getMealDisplayName(String mealType) {
        switch (mealType) {
            case "breakfast":
                return "завтрак";
            case "lunch":
                return "обед";
            case "dinner":
                return "ужин";
            case "snack":
                return "перекус";
            default:
                return mealType;
        }
    }


    private void showDeleteDishDialog(Dish dish) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Удаление блюда")
                .setMessage("Вы уверены, что хотите удалить блюдо \"" + dish.getName() + "\"?\n\nЭто действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteDish(dish);
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void deleteDish(Dish dish) {
        Log.d(TAG, "Начинаем удаление блюда: " + dish.getName());

        dishManager.deleteDish(dish, new DishManager.OnDishDeletedListener() {
            @Override
            public void onDishDeleted() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Блюдо успешно удалено: " + dish.getName());
                    Toast.makeText(FoodSelectionActivity.this, "Блюдо \"" + dish.getName() + "\" удалено", Toast.LENGTH_SHORT).show();


                    showMyDishes();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Ошибка при удалении блюда: " + error);
                    Toast.makeText(FoodSelectionActivity.this, "Ошибка при удалении блюда: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    private void setupMultiSelectionPanel() {
        multiSelectionPanel = findViewById(R.id.multi_selection_panel);
        addSelectedButton = findViewById(R.id.btn_add_selected);
        cancelSelectionButton = findViewById(R.id.btn_cancel_selection);


        addSelectedButton.setOnClickListener(v -> addSelectedFoodsToMeal());


        cancelSelectionButton.setOnClickListener(v -> exitMultiSelectionMode());
    }


    private void enterMultiSelectionMode(Food firstFood) {
        Log.d(TAG, "Входим в режим множественного выбора с продуктом: " + firstFood.getName());

        isMultiSelectionMode = true;
        selectedFoodIds.clear();
        selectedFoodsMap.clear();


        selectedFoodIds.add(firstFood.getId());
        selectedFoodsMap.put(firstFood.getId(), firstFood);


        multiSelectionPanel.setVisibility(View.VISIBLE);


        updateSelectionCounter();


        foodAdapter.setMultiSelectionMode(true);
        foodAdapter.setSelectedFoods(selectedFoodIds);
        recentFoodAdapter.setMultiSelectionMode(true);
        recentFoodAdapter.setSelectedFoods(selectedFoodIds);
    }


    private void exitMultiSelectionMode() {
        Log.d(TAG, "Выходим из режима множественного выбора");

        isMultiSelectionMode = false;
        selectedFoodIds.clear();
        selectedFoodsMap.clear();


        multiSelectionPanel.setVisibility(View.GONE);


        foodAdapter.clearSelection();
        recentFoodAdapter.clearSelection();
    }


    private void toggleFoodSelection(Food food) {
        String foodId = food.getId();

        if (selectedFoodIds.contains(foodId)) {

            selectedFoodIds.remove(foodId);
            selectedFoodsMap.remove(foodId);
            Log.d(TAG, "Убрали продукт из выбора: " + food.getName());
        } else {

            selectedFoodIds.add(foodId);
            selectedFoodsMap.put(foodId, food);
            Log.d(TAG, "Добавили продукт в выбор: " + food.getName());
        }


        if (selectedFoodIds.isEmpty()) {
            exitMultiSelectionMode();
        } else {

            updateSelectionCounter();


            foodAdapter.setSelectedFoods(selectedFoodIds);
            recentFoodAdapter.setSelectedFoods(selectedFoodIds);
        }
    }


    private void updateSelectionCounter() {
        int count = selectedFoodIds.size();
        String text;


        if (count == 1) {
            text = "Добавить (1)";
        } else if (count >= 2 && count <= 4) {
            text = "Добавить (" + count + ")";
        } else {
            text = "Добавить (" + count + ")";
        }

        addSelectedButton.setText(text);
    }


    private void addSelectedFoodsToMeal() {
        if (selectedFoodsMap.isEmpty()) {
            Toast.makeText(this, "Не выбрано ни одного продукта", Toast.LENGTH_SHORT).show();
            return;
        }

        AtomicInteger addedCount = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            for (Food food : selectedFoodsMap.values()) {
                Log.d(TAG, food.getName());
                final float defaultQuantity;
                final String portionName;

                if (appDatabase.recentFoodDao().isFoodInRecents(food.getId())) {
                    RecentFoodEntity recentFood = appDatabase.recentFoodDao().getRecentFoodById(food.getId());
                    defaultQuantity = recentFood.getQuantity();
                    portionName = recentFood.getPortionName();
                } else {
                    if (!food.getPortions().isEmpty()) {
                        defaultQuantity = 1;
                        portionName = food.getPortions().get(0).getName();
                    } else {
                        defaultQuantity = 100f;
                        portionName = food.isLiquid() ? "мл" : "грамм";
                    }

                }

                foodManager.addFoodToMeal(mealType, food, defaultQuantity, portionName);
                Log.d(TAG, "Добавлен продукт: " + food.getName() + ", " + defaultQuantity + " " + portionName);

                foodManager.addToRecents(food, defaultQuantity, portionName);


                EventBus.getDefault().post(new FoodAddedEvent(food, defaultQuantity, mealType));

                addedCount.getAndIncrement();

            }

            runOnUiThread(() -> {
                exitMultiSelectionMode();
                setResult(RESULT_OK);
                finish();
            });
        });


    }

    @Override
    public void onBackPressed() {
        if (isMultiSelectionMode) {

            exitMultiSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
} 