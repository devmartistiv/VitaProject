package com.martist.vitamove.nutrition.data.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.R;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.local.MealsDatabase;
import com.martist.vitamove.core.data.local.entities.DayMeal;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.gigachat.GigaChatProductService;
import com.martist.vitamove.nutrition.data.local.entities.RecentFoodEntity;
import com.martist.vitamove.nutrition.data.repository.SupabaseBarcodeRepository;
import com.martist.vitamove.nutrition.data.repository.SupabaseFoodRepository;
import com.martist.vitamove.nutrition.domain.events.MealUpdatedEvent;
import com.martist.vitamove.nutrition.domain.events.MealsLoadedEvent;
import com.martist.vitamove.nutrition.domain.events.NutrientsNormsUpdatedEvent;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Meal;
import com.martist.vitamove.nutrition.ui.model.NutrientType;
import com.martist.vitamove.nutrition.ui.model.SelectedFood;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FoodManager {
    private static final String PREFS_NAME = "FoodManagerPrefs";
    private static final String SELECTED_FOODS_KEY = "selectedFoods";
    private static FoodManager instance;
    private final Map<String, List<SelectedFood>> selectedFoods = new HashMap<>();


    private final Map<String, Float> dailyNorms = new HashMap<>();
    private String currentFitnessGoal = "weight_loss";
    private int targetCalories = 2000;

    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context context;
    private final Map<String, Meal> meals;
    private final SupabaseFoodRepository foodRepository;
    private final SupabaseBarcodeRepository barcodeRepository;
    private final GigaChatProductService gigaChatProductService;
    private final FavouriteProductsHelper favouriteProductsHelper;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private List<Food> foods;
    private final Date currentDate;
    private Date selectedDateForView;
    private final MealsDatabase database;
    private final AppDatabase appDatabase;
    private static final String TAG = "FoodManager";
    private final Map<String, Boolean> foodExistsCache = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler();
    private final String userId;


    private final MutableLiveData<Float> caloriesNormLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> proteinsNormLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> fatsNormLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> carbsNormLiveData = new MutableLiveData<>();

    public FoodManager(Context context) {
        this.context = context;
        this.database = MealsDatabase.getInstance(context);
        this.appDatabase = AppDatabase.getInstance(context);

        this.gson = new com.google.gson.GsonBuilder()
                .serializeSpecialFloatingPointValues()
                .create();

        this.currentDate = new Date();
        this.meals = new HashMap<>();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


        SharedPreferences userPrefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        this.userId = userPrefs.getString("userId", "default_user");


        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET);


        this.foodRepository = new SupabaseFoodRepository(supabaseClient, context);
        this.barcodeRepository = new SupabaseBarcodeRepository(supabaseClient, context);


        this.gigaChatProductService = new GigaChatProductService();
        this.favouriteProductsHelper = new FavouriteProductsHelper(appDatabase, executor, this);

        initDefaultNorms();
        loadUserSettings();

        loadSelectedFoods();
        loadMealsForCurrentDate();
    }

    public static FoodManager getInstance(Context context) {
        if (instance == null) {
            instance = new FoodManager(context);
        }
        return instance;
    }


    public LiveData<Float> getCaloriesNormLiveData() {
        return caloriesNormLiveData;
    }

    public LiveData<Float> getProteinsNormLiveData() {
        return proteinsNormLiveData;
    }

    public LiveData<Float> getFatsNormLiveData() {
        return fatsNormLiveData;
    }

    public LiveData<Float> getCarbsNormLiveData() {
        return carbsNormLiveData;
    }

    public float getDailyNorm(String nutrient) {
        return dailyNorms.getOrDefault(nutrient, 0f);
    }

    private void loadSelectedFoods() {
        try {

            String json = prefs.getString(SELECTED_FOODS_KEY, "{}");
            Type mapType = new TypeToken<HashMap<String, List<SelectedFood>>>() {
            }.getType();
            Map<String, List<SelectedFood>> loaded = gson.fromJson(json, mapType);
            if (loaded != null) {
                selectedFoods.clear();
                selectedFoods.putAll(loaded);
            }
        } catch (JsonSyntaxException e) {
            try {

                String json = prefs.getString(SELECTED_FOODS_KEY, "[]");
                Type listType = new TypeToken<ArrayList<SelectedFood>>() {
                }.getType();
                List<SelectedFood> oldData = gson.fromJson(json, listType);


                if (oldData != null && !oldData.isEmpty()) {
                    selectedFoods.clear();

                    selectedFoods.put("breakfast", new ArrayList<>(oldData));

                    saveSelectedFoods();

                    prefs.edit().putString(SELECTED_FOODS_KEY, "{}").apply();
                }
            } catch (Exception ignored) {

                selectedFoods.clear();
                saveSelectedFoods();
            }
        }
    }

    private void saveSelectedFoods() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SELECTED_FOODS_KEY, gson.toJson(selectedFoods));
        editor.apply();
    }


    public double getTotalCaloriesForCurrentDate() {
        double total = 0;


        Map<String, Meal> savedMeals = new HashMap<>(meals);


        loadMealsForDate(currentDate);


        Meal breakfast = getMeal("breakfast");
        Meal lunch = getMeal("lunch");
        Meal dinner = getMeal("dinner");
        Meal snack = getMeal("snack");


        if (breakfast != null) {
            total += breakfast.getCalories();
        }
        if (lunch != null) {
            total += lunch.getCalories();
        }
        if (dinner != null) {
            total += dinner.getCalories();
        }
        if (snack != null) {
            total += snack.getCalories();
        }


        meals.clear();
        meals.putAll(savedMeals);
        return total;
    }


    public double getTotalCaloriesForSelectedDate() {
        double total = 0;


        Meal breakfast = getMeal("breakfast");
        Meal lunch = getMeal("lunch");
        Meal dinner = getMeal("dinner");
        Meal snack = getMeal("snack");


        if (breakfast != null) {
            total += breakfast.getCalories();
        }
        if (lunch != null) {
            total += lunch.getCalories();
        }
        if (dinner != null) {
            total += dinner.getCalories();
        }
        if (snack != null) {
            total += snack.getCalories();
        }
        return total;
    }

    public double getTotalProteins() {
        double total = 0;
        for (List<SelectedFood> mealFoods : selectedFoods.values()) {
            for (SelectedFood selectedFood : mealFoods) {
                double amount = selectedFood.getAmount() / 100.0;
                total += selectedFood.getFood().getProteins() * amount;
            }
        }
        return total;
    }


    public double getTotalProtein() {
        return getTotalProteins();
    }

    public double getTotalFat() {
        double total = 0;
        for (List<SelectedFood> mealFoods : selectedFoods.values()) {
            for (SelectedFood selectedFood : mealFoods) {
                double amount = selectedFood.getAmount() / 100.0;
                total += selectedFood.getFood().getFats() * amount;
            }
        }
        return total;
    }

    public double getTotalCarbs() {
        double total = 0;
        for (List<SelectedFood> mealFoods : selectedFoods.values()) {
            for (SelectedFood selectedFood : mealFoods) {
                double amount = selectedFood.getAmount() / 100.0;
                total += selectedFood.getFood().getCarbs() * amount;
            }
        }
        return total;
    }

    public void addFoodToMeal(String mealType, Food food, float quantity, String portionName) {
        if (mealType == null) {
            return;
        }

        if (food == null) {

            return;
        }


        Meal meal = meals.get(mealType);
        if (meal == null) {

            meal = new Meal(getMealTitle(mealType), getMealIcon(mealType));
        }


        meal.addFood(food, quantity, portionName);


        meals.put(mealType, meal);


        final String dateStr = dateFormat.format(getSelectedDateForView());
        final Meal finalMeal = meal;

        executor.execute(() -> {
            try {

                DayMeal existingMeal = database.mealDao().getMealByDateAndType(dateStr, mealType, userId);


                DayMeal dayMeal = new DayMeal();
                dayMeal.date = dateStr;
                dayMeal.mealType = mealType;
                dayMeal.mealData = gson.toJson(finalMeal);
                dayMeal.createdAt = new Date();
                dayMeal.userId = userId;

                if (existingMeal != null) {

                    existingMeal.mealData = dayMeal.mealData;
                    existingMeal.updatedAt = new Date();
                    database.mealDao().update(existingMeal);

                } else {

                    database.mealDao().insert(dayMeal);

                }


                int totalCalories = (int) getTotalCaloriesForSelectedDate();


                CaloriesManager caloriesManager = CaloriesManager.getInstance(context);
                caloriesManager.setConsumedCalories(totalCalories);


                mainHandler.post(() -> EventBus.getDefault().post(new MealUpdatedEvent(mealType)));
            } catch (Exception ignored) {
            }
        });
    }

    private int getMealIcon(String mealType) {
        switch (mealType) {
            case "breakfast":
                return R.drawable.ic_breakfast;
            case "lunch":
                return R.drawable.ic_lunch;
            case "dinner":
                return R.drawable.ic_dinner;
            case "snack":
                return R.drawable.ic_snack;
            default:
                return R.drawable.ic_food;
        }
    }

    private String getMealTitle(String mealType) {
        switch (mealType.toLowerCase()) {
            case "breakfast":
                return "Завтрак";
            case "lunch":
                return "Обед";
            case "dinner":
                return "Ужин";
            case "snack":
                return "Перекус";
            default:
                return "Прием пищи";
        }
    }

    public void setSelectedDateForView(Date date) {
        this.selectedDateForView = date;
        loadMealsForSelectedDate();
    }

    public Date getSelectedDateForView() {
        return selectedDateForView != null ? selectedDateForView : currentDate;
    }

    public String getSelectedDateFormatted() {
        return dateFormat.format(getSelectedDateForView());
    }

    public Meal getMeal(String mealType) {
        return meals.get(mealType);
    }

    private void loadMealsForSelectedDate() {
        loadMealsForDate(selectedDateForView);
    }

    private void loadMealsForCurrentDate() {
        loadMealsForDate(currentDate);
    }

    private void loadMealsForDate(Date date) {
        if (date == null) {

            return;
        }

        new Thread(() -> {
            String dateStr = dateFormat.format(date);

            List<DayMeal> dayMeals = database.mealDao().getMealsForDate(dateStr, userId);
            meals.clear();

            for (DayMeal dayMeal : dayMeals) {
                try {
                    Meal meal = gson.fromJson(dayMeal.mealData, Meal.class);
                    meals.put(dayMeal.mealType, meal);

                } catch (JsonSyntaxException e) {

                }
            }


            foodExistsCache.clear();

            EventBus.getDefault().post(new MealsLoadedEvent(meals));
        }).start();
    }

    public List<Food> searchFoods(String query) {
        if (query == null || query.trim().isEmpty()) {
            return foodRepository.getPopularFoods();
        }


        return foodRepository.searchFoodsByQuery(query);
    }

    public List<Food> getAllFoods() {
        return foodRepository.getAllFoods();
    }


    public Food getFoodById(String foodId) {

        if (foodId != null && foodId.startsWith("virtual_")) {

            return null;
        }
        return foodRepository.getFoodById(foodId);
    }

    public boolean hasFoodForDate(String date) {

        if (foodExistsCache.containsKey(date)) {
            return foodExistsCache.get(date);
        }

        try {

            List<DayMeal> dayMeals = database.mealDao().getMealsForDate(date, userId);

            for (DayMeal dayMeal : dayMeals) {
                try {
                    Meal meal = gson.fromJson(dayMeal.mealData, Meal.class);
                    if (meal != null && !meal.getFoods().isEmpty()) {
                        foodExistsCache.put(date, true);
                        return true;
                    }
                } catch (JsonSyntaxException e) {

                }
            }

            foodExistsCache.put(date, false);
            return false;

        } catch (Exception e) {
            return false;
        }
    }


    public List<String> getAllDatesWithFood() {
        List<String> datesWithFood = new ArrayList<>();

        try {

            List<DayMeal> allDayMeals = database.mealDao().getAllMeals(userId);

            for (DayMeal dayMeal : allDayMeals) {
                try {
                    Meal meal = gson.fromJson(dayMeal.mealData, Meal.class);

                    if (meal != null && !meal.getFoods().isEmpty()) {
                        String date = dayMeal.date;
                        if (!datesWithFood.contains(date)) {
                            datesWithFood.add(date);
                        }
                    }
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "Error parsing meal data: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting dates with food: " + e.getMessage());
        }

        return datesWithFood;
    }


    private void initDefaultNorms() {
        dailyNorms.put("calories", 2000f);
        dailyNorms.put("proteins", 90f);
        dailyNorms.put("fats", 70f);
        dailyNorms.put("carbs", 250f);

    }


    private void loadUserSettings() {

        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String fitnessGoal = appPrefs.getString("fitness_goal", "weight_loss");


        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String userDataGoal = userPrefs.getString("fitness_goal", "");


        if (!userDataGoal.isEmpty() && !userDataGoal.equals(fitnessGoal)) {
            fitnessGoal = userDataGoal;

            SharedPreferences.Editor editor = appPrefs.edit();
            editor.putString("fitness_goal", fitnessGoal);
            editor.apply();

        }

        this.currentFitnessGoal = fitnessGoal;


        this.targetCalories = userPrefs.getInt("target_calories", 2000);


        Log.d(TAG, "Загружены настройки: fitnessGoal=" + fitnessGoal + ", targetCalories=" + targetCalories);


        updateNutrientsNorms();
    }


    private void updateNutrientsNorms() {

        dailyNorms.put("calories", (float) targetCalories);


        SharedPreferences dashboardPrefs = context.getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE);
        int customProtein = dashboardPrefs.getInt("protein_goal", 0);
        int customFats = dashboardPrefs.getInt("fats_goal", 0);
        int customCarbs = dashboardPrefs.getInt("carbs_goal", 0);


        if (customProtein > 0 && customFats > 0 && customCarbs > 0) {
            dailyNorms.put("proteins", (float) customProtein);
            dailyNorms.put("fats", (float) customFats);
            dailyNorms.put("carbs", (float) customCarbs);

            Log.d(TAG, "Используются пользовательские БЖУ цели: Б=" + customProtein +
                    "г, Ж=" + customFats + "г, У=" + customCarbs + "г");


            caloriesNormLiveData.postValue(dailyNorms.get("calories"));
            proteinsNormLiveData.postValue(dailyNorms.get("proteins"));
            fatsNormLiveData.postValue(dailyNorms.get("fats"));
            carbsNormLiveData.postValue(dailyNorms.get("carbs"));


            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("calories_norm", dailyNorms.getOrDefault("calories", 2000f));
            editor.putFloat("proteins_norm", (float) customProtein);
            editor.putFloat("fats_norm", (float) customFats);
            editor.putFloat("carbs_norm", (float) customCarbs);
            editor.apply();


            EventBus.getDefault().post(new NutrientsNormsUpdatedEvent(dailyNorms));
            return;
        }


        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        float currentWeight = userPrefs.getFloat("current_weight", 70f);


        float proteinPerKg = 0f;
        float fatPercentage = 0f;
        float carbPercentage = 0f;


        float maxProteinGrams = currentWeight * 2.2f;

        switch (currentFitnessGoal) {
            case "muscle_gain":

                proteinPerKg = 2.0f;
                fatPercentage = 0.25f;


                float proteinGrams = currentWeight * proteinPerKg;

                float proteinCalories = proteinGrams * 4f;
                float proteinPercentage = proteinCalories / targetCalories;


                carbPercentage = 1f - proteinPercentage - fatPercentage;


                if (carbPercentage < 0.4f) {
                    fatPercentage = 0.22f;
                    carbPercentage = 1f - proteinPercentage - fatPercentage;
                }


                dailyNorms.put("proteins", proteinGrams);
                dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
                dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
                break;

            case "weight_loss":

                proteinPerKg = 1.8f;
                fatPercentage = 0.25f;


                proteinGrams = currentWeight * proteinPerKg;

                proteinCalories = proteinGrams * 4f;
                proteinPercentage = proteinCalories / targetCalories;


                carbPercentage = 1f - proteinPercentage - fatPercentage;


                dailyNorms.put("proteins", proteinGrams);
                dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
                dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
                break;

            case "endurance":
                proteinPerKg = 1.4f;
                fatPercentage = 0.25f;


                proteinGrams = currentWeight * proteinPerKg;

                proteinCalories = proteinGrams * 4f;
                proteinPercentage = proteinCalories / targetCalories;


                carbPercentage = 1f - proteinPercentage - fatPercentage;
                if (carbPercentage < 0.55f) {
                    fatPercentage = 1f - proteinPercentage - 0.55f;
                    carbPercentage = 0.55f;
                }


                dailyNorms.put("proteins", proteinGrams);
                dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
                dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
                break;


            default:
                proteinPerKg = 1.5f;
                fatPercentage = 0.28f;


                proteinGrams = currentWeight * proteinPerKg;

                proteinCalories = proteinGrams * 4f;
                proteinPercentage = proteinCalories / targetCalories;


                carbPercentage = 1f - proteinPercentage - fatPercentage;


                dailyNorms.put("proteins", proteinGrams);
                dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
                dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
                break;
        }


        float currentProteinGrams = dailyNorms.get("proteins");
        if (currentProteinGrams > maxProteinGrams) {

            dailyNorms.put("proteins", maxProteinGrams);


            float proteinCalories = maxProteinGrams * 4f;
            float proteinPercentage = proteinCalories / targetCalories;


            if (currentFitnessGoal.equals("endurance")) {

                carbPercentage = 0.6f;
                fatPercentage = 1f - proteinPercentage - carbPercentage;
            } else {

                fatPercentage = 0.25f;
                carbPercentage = 1f - proteinPercentage - fatPercentage;
            }


            dailyNorms.put("fats", targetCalories * fatPercentage / 9f);
            dailyNorms.put("carbs", targetCalories * carbPercentage / 4f);
        }


        float currentFatGrams = dailyNorms.get("fats");
        float minFatGrams = 0.5f * currentWeight;

        if (currentFatGrams < minFatGrams) {
            Log.d(TAG, "Увеличиваем жиры с " + currentFatGrams + "г до минимально необходимого " + minFatGrams + "г");
            dailyNorms.put("fats", minFatGrams);


            float proteinCalories = dailyNorms.get("proteins") * 4f;
            float fatCalories = minFatGrams * 9f;
            float carbCalories = targetCalories - proteinCalories - fatCalories;


            if (carbCalories > 0) {
                dailyNorms.put("carbs", carbCalories / 4f);
            }
        }


        caloriesNormLiveData.postValue(dailyNorms.get("calories"));
        proteinsNormLiveData.postValue(dailyNorms.get("proteins"));
        fatsNormLiveData.postValue(dailyNorms.get("fats"));
        carbsNormLiveData.postValue(dailyNorms.get("carbs"));


        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("calories_norm", dailyNorms.getOrDefault("calories", 2000f));
        editor.putFloat("proteins_norm", dailyNorms.getOrDefault("proteins", 0f));
        editor.putFloat("fats_norm", dailyNorms.getOrDefault("fats", 0f));
        editor.putFloat("carbs_norm", dailyNorms.getOrDefault("carbs", 0f));
        editor.apply();


        EventBus.getDefault().post(new NutrientsNormsUpdatedEvent(dailyNorms));
    }

    public void updateTargetCalories(int newTargetCalories) {
        this.targetCalories = newTargetCalories;
        updateNutrientsNorms();
    }

    public void updateFitnessGoal(String newFitnessGoal) {
        this.currentFitnessGoal = newFitnessGoal;
        updateNutrientsNorms();
    }

    public void refreshNutrientNorms() {
        loadUserSettings();
        updateNutrientsNorms();
    }


    public void updateCustomMacroGoals(int protein, int fats, int carbs) {

        dailyNorms.put("proteins", (float) protein);
        dailyNorms.put("fats", (float) fats);
        dailyNorms.put("carbs", (float) carbs);


        proteinsNormLiveData.postValue((float) protein);
        fatsNormLiveData.postValue((float) fats);
        carbsNormLiveData.postValue((float) carbs);


        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("proteins_norm", (float) protein);
        editor.putFloat("fats_norm", (float) fats);
        editor.putFloat("carbs_norm", (float) carbs);
        editor.apply();


        EventBus.getDefault().post(new NutrientsNormsUpdatedEvent(dailyNorms));
    }

    public void updateMeal(String mealType, Meal meal) {
        Log.d(TAG, "Обновление приема пищи: " + mealType);


        meals.put(mealType, meal);


        executor.execute(() -> {
            try {

                String dateStr = dateFormat.format(getSelectedDateForView());


                DayMeal dayMeal = new DayMeal();
                dayMeal.date = dateStr;
                dayMeal.mealType = mealType;
                dayMeal.mealData = gson.toJson(meal);
                dayMeal.createdAt = new Date();
                dayMeal.userId = userId;


                DayMeal existingMeal = database.mealDao().getMealByDateAndType(dateStr, mealType, userId);

                if (existingMeal != null) {

                    existingMeal.mealData = dayMeal.mealData;
                    existingMeal.updatedAt = new Date();
                    database.mealDao().update(existingMeal);
                    Log.d(TAG, "Прием пищи обновлен в БД: " + mealType);
                } else {

                    database.mealDao().insert(dayMeal);
                    Log.d(TAG, "Прием пищи сохранен в БД: " + mealType);
                }


                int totalCalories = (int) getTotalCaloriesForSelectedDate();


                CaloriesManager caloriesManager = CaloriesManager.getInstance(context);
                caloriesManager.setConsumedCalories(totalCalories);


                mainHandler.post(() -> EventBus.getDefault().post(new MealUpdatedEvent(mealType)));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении приема пищи: " + e.getMessage(), e);
            }
        });


        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        defaultPrefs.edit().putString("fitness_goal", currentFitnessGoal).apply();


        caloriesNormLiveData.postValue(dailyNorms.get("calories"));
        proteinsNormLiveData.postValue(dailyNorms.get("proteins"));
        fatsNormLiveData.postValue(dailyNorms.get("fats"));
        carbsNormLiveData.postValue(dailyNorms.get("carbs"));


        EventBus.getDefault().post(new NutrientsNormsUpdatedEvent(dailyNorms));
    }

    public void searchProductWithGigaChat(String productName, GigaChatProductService.ProductListener listener) {
        Log.d(TAG, "Поиск продукта через GigaChat: " + productName);
        gigaChatProductService.searchProduct(productName, new GigaChatProductService.ProductListener() {
            @Override
            public void onProductFound(Food food) {

                listener.onProductFound(food);
            }

            @Override
            public void onProductNotFound() {
                listener.onProductNotFound();
            }

            @Override
            public void onError(String message) {

                listener.onError(message);
            }
        });
    }

    public Food findFoodByExactName(String name) {

        if (foods == null || foods.isEmpty() || name == null || name.isEmpty()) {

            return null;
        }


        String normalizedName = name.trim().toLowerCase();
        Log.d(TAG, "Поиск продукта по точному имени: '" + normalizedName + "'");

        for (Food food : foods) {
            if (food.getName() != null && food.getName().trim().toLowerCase().equals(normalizedName)) {

                return food;
            }
        }

        return null;
    }

    public void findFoodByExactNameAsync(String name, FoodSearchCallback callback) {
        if (name == null || name.isEmpty()) {
            callback.onFoodSearchResult(null);
            return;
        }

        executor.execute(() -> {

            if (foods == null || foods.isEmpty()) {
                loadFoods();
            }

            if (foods == null || foods.isEmpty()) {

                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFoodSearchResult(null));
                }
                return;
            }

            String normalizedName = name.trim().toLowerCase();


            Food foundFood = null;
            for (Food food : foods) {
                if (food.getName() != null && food.getName().trim().toLowerCase().equals(normalizedName)) {

                    foundFood = food;
                    break;
                }
            }


            final Food result = foundFood;
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFoodSearchResult(result));
            }
        });
    }


    public interface FoodSearchCallback {
        void onFoodSearchResult(Food food);
    }


    public void loadFoodsAsync(Runnable onComplete) {
        executor.execute(() -> {
            loadFoods();
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    private void loadFoods() {
        foods = foodRepository.getAllFoods();
    }

    public static void resetInstance() {
        if (instance != null) {
            instance.executor.shutdown();
            instance = null;

            MealsDatabase.resetInstance();
            Log.d(TAG, "FoodManager сброшен");
        }
    }

    public SupabaseBarcodeRepository getBarcodeRepository() {
        return barcodeRepository;
    }

    public SupabaseFoodRepository getFoodRepository() {
        return foodRepository;
    }

    public Map<NutrientType, Float> getConsumedNutrients(String date) {
        Map<NutrientType, Float> nutrients = new HashMap<>();


        Meal breakfast = getMealForDate("breakfast", date);
        Meal lunch = getMealForDate("lunch", date);
        Meal dinner = getMealForDate("dinner", date);
        Meal snack = getMealForDate("snack", date);


        addNutrientsFromMeal(nutrients, breakfast);
        addNutrientsFromMeal(nutrients, lunch);
        addNutrientsFromMeal(nutrients, dinner);
        addNutrientsFromMeal(nutrients, snack);

        return nutrients;
    }


    private void addNutrientsFromMeal(Map<NutrientType, Float> nutrients, Meal meal) {
        if (meal == null || meal.getFoods().isEmpty())
            return;

        for (Meal.FoodPortion portion : meal.getFoods()) {
            float multiplier = portion.getTotalWeightInGrams() / 100f;
            for (NutrientType type : NutrientType.values()) {
                float value = type.getProductValue(portion.getFood());
                nutrients.merge(
                        type,
                        value * multiplier,
                        Float::sum
                );

            }

        }
    }

    public Meal getMealForDate(String mealType, String date) {
        try {
            Meal currentMeal = getMeal(mealType);

            if (date.equals(getSelectedDateFormatted()))
                return currentMeal;

            DayMeal dayMeal = database.mealDao().getMealForDateAndType(date, mealType, userId);


            if (dayMeal != null && dayMeal.mealData != null && !dayMeal.mealData.isEmpty()) {
                try {
                    Meal meal = gson.fromJson(dayMeal.mealData, Meal.class);
                    if (meal != null)
                        return meal;
                } catch (JsonSyntaxException e) {
                }
            }

            return new Meal(getMealTitle(mealType), getMealIcon(mealType));

        } catch (Exception e) {
        }


        return new Meal(getMealTitle(mealType), getMealIcon(mealType));
    }

    public void addToRecents(Food food, float quantity, String portionName) {
        executor.execute(() -> {
            try {

                if (food.getId() != null && food.getId().startsWith("virtual_")) {

                    return;
                }


                RecentFoodEntity existingEntity = appDatabase.recentFoodDao()
                        .getRecentFoodById(food.getId());

                if (existingEntity != null) {


                    appDatabase.recentFoodDao().updateLastUsedTimeById(existingEntity.getId(), new Date());
                    RecentFoodEntity recentFood = appDatabase.recentFoodDao().getRecentFoodById(food.getId());
                    appDatabase.recentFoodDao().updatePortion(recentFood.getId(), quantity, portionName);

                } else {

                    RecentFoodEntity recentFoodEntity = new RecentFoodEntity(
                            UUID.randomUUID().toString(),
                            food.getId(),
                            food.getName(),
                            quantity,
                            portionName, new Date(), new Date());

                    appDatabase.recentFoodDao().addToRecents(recentFoodEntity);

                }
            } catch (Exception e) {
            }
        });
    }

    public List<RecentFoodEntity> getRecentFoods() {
        try {
            return appDatabase.recentFoodDao().getAllRecentFoods();

        } catch (Exception e) {

            return new ArrayList<>();
        }
    }

    public void addToFavorites(Food food) {
        favouriteProductsHelper.addToFavorites(food);
    }

    public void removeFromFavorites(String foodId) {
        favouriteProductsHelper.removeFromFavorites(foodId);
    }

    public boolean isFoodInFavorites(String foodId) {
        return favouriteProductsHelper.isFoodInFavorites(foodId);
    }

    public List<Food> getFavoriteFoods() {
        return favouriteProductsHelper.getFavoriteFoods();
    }

}