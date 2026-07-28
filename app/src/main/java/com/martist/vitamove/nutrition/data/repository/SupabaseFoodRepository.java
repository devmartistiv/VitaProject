package com.martist.vitamove.nutrition.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.nutrition.data.dto.FoodDto;
import com.martist.vitamove.nutrition.data.local.dao.FoodCacheDao;
import com.martist.vitamove.nutrition.data.local.entities.FoodCacheEntity;
import com.martist.vitamove.nutrition.ui.model.Food;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class SupabaseFoodRepository {
    private static final String TAG = "SupabaseFoodRepo";
    private final SupabaseClient supabaseClient;
    private final Context context;
    private final FoodCacheDao foodCacheDao;

    public SupabaseFoodRepository(SupabaseClient supabaseClient, Context context) {
        this.supabaseClient = supabaseClient;
        this.context = context;
        this.foodCacheDao = AppDatabase.getInstance(context).foodCacheDao();
    }


    public List<Food> getPopularFoods() {
        try {
            Log.d(TAG, "Получение популярных продуктов из локального кэша");

            List<FoodCacheEntity> entities =
                    foodCacheDao.getPopularFoods(20);

            List<Food> popularFoods = new ArrayList<>();
            for (com.martist.vitamove.nutrition.data.local.entities.FoodCacheEntity entity : entities) {
                popularFoods.add(entity.toFood());
            }

            Log.d(TAG, "Получено " + popularFoods.size() + " популярных продуктов из кэша");
            return popularFoods;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении популярных продуктов: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public List<Food> searchFoodsByQuery(String query) {

        if (query == null || query.trim().isEmpty()) {
            return getPopularFoods();
        }

        String normalizedQuery = query.trim().toLowerCase();
        Log.d(TAG, "Поиск продуктов по запросу в кэше: '" + normalizedQuery + "'");

        try {

            String[] searchWords = normalizedQuery.split("\\s+");


            String firstWordPattern = "%" + searchWords[0] + "%";
            List<com.martist.vitamove.nutrition.data.local.entities.FoodCacheEntity> entities =
                    foodCacheDao.searchFoods(firstWordPattern);

            List<Food> matchingFoods = new ArrayList<>();


            for (FoodCacheEntity entity : entities) {
                String foodName = entity.getName().toLowerCase();
                boolean allWordsMatch = true;


                for (String word : searchWords) {
                    if (!foodName.contains(word)) {
                        allWordsMatch = false;
                        break;
                    }
                }

                if (allWordsMatch) {
                    matchingFoods.add(entity.toFood());
                }
            }

            Log.d(TAG, "Найдено " + matchingFoods.size() + " продуктов по запросу '" + normalizedQuery + "' (поиск независимо от порядка слов)");
            return matchingFoods;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при поиске продуктов: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public Food getFoodById(String foodId) {
        try {
            Log.d(TAG, "Поиск продукта с ID " + foodId + " в локальном кэше");


            com.martist.vitamove.nutrition.data.local.entities.FoodCacheEntity entity = foodCacheDao.getFoodById(foodId);

            if (entity != null) {
                Log.d(TAG, "Продукт найден в кэше: " + entity.getName());
                return entity.toFood();
            }


            Log.d(TAG, "Продукт не найден в кэше, запрашиваем из Supabase");

            JSONArray foodArray = supabaseClient.from("foods")
                    .select("*")
                    .eq("id", String.valueOf(foodId))
                    .executeAndGetArray();

            if (foodArray != null && foodArray.length() > 0) {
                JSONObject foodJson = foodArray.getJSONObject(0);
                Food food = parseFoodFromJson(foodJson);


                foodCacheDao.insert(FoodCacheEntity.fromFood(food));

                Log.d(TAG, "Продукт загружен из Supabase и сохранен в кэш: " + food.getName());
                return food;
            } else {
                Log.d(TAG, "Продукт с ID " + foodId + " не найден");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении продукта с ID " + foodId + ": " + e.getMessage(), e);
            return null;
        }
    }


    public List<Food> getAllFoods() {
        try {
            Log.d(TAG, "Получение всех продуктов из локального кэша Room");

            List<FoodCacheEntity> entities = foodCacheDao.getAllFoods();
            List<Food> foods = new ArrayList<>();

            for (FoodCacheEntity entity : entities) {
                foods.add(entity.toFood());
            }

            Log.d(TAG, "Загружено " + foods.size() + " продуктов из локального кэша");
            return foods;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении всех продуктов из кэша: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public List<Food> getAllFoodsFromSupabase() {
        try {
            Log.d(TAG, "Запрос всех продуктов из Supabase для синхронизации");

            List<Food> foods = new ArrayList<>();
            int pageSize = 1000;
            int offset = 0;
            boolean hasMoreData = true;


            while (hasMoreData) {
                Log.d(TAG, "Загрузка порции продуктов: offset=" + offset + ", limit=" + pageSize);

                JSONArray foodsArray = null;
                int maxRetries = 3;
                int currentRetry = 0;


                while (currentRetry < maxRetries && foodsArray == null) {
                    try {
                        foodsArray = supabaseClient.from("foods")
                                .select("*,portions(*)")
                                .limit(pageSize)
                                .offset(offset)
                                .executeAndGetArray();
                    } catch (SupabaseClient.TokenRefreshedException e) {

                        Log.d(TAG, "Токен обновлен, повторяем запрос. Попытка " + (currentRetry + 1));
                        currentRetry++;

                        if (currentRetry >= maxRetries) {
                            Log.e(TAG, "Не удалось выполнить запрос после " + maxRetries + " попыток");
                            throw e;
                        }
                    }
                }

                if (foodsArray == null) {
                    Log.e(TAG, "Не удалось получить данные после обновления токена");
                    break;
                }

                Log.d(TAG, "Получено " + foodsArray.length() + " продуктов в текущей порции");


                for (int i = 0; i < foodsArray.length(); i++) {
                    JSONObject foodJson = foodsArray.getJSONObject(i);
                    Food food = parseFoodFromJson(foodJson);

                    if (food != null) {
                        foods.add(food);
                    }
                }


                if (foodsArray.length() < pageSize) {
                    hasMoreData = false;
                } else {

                    offset += pageSize;
                }
            }

            Log.d(TAG, "Всего загружено " + foods.size() + " продуктов из Supabase");
            return foods;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении всех продуктов из Supabase: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public List<Food> getFoodsSinceTimestamp(String timestamp) {
        try {


            String normalizedTimestamp = timestamp;
            if (timestamp != null && timestamp.contains(" ")) {
                normalizedTimestamp = timestamp.replace(" ", "+");
                Log.d(TAG, "Нормализован timestamp: " + timestamp + " -> " + normalizedTimestamp);
            }

            Log.d(TAG, "Запрос обновленных продуктов с timestamp: " + normalizedTimestamp);

            List<Food> foods = new ArrayList<>();
            int pageSize = 1000;
            int offset = 0;
            boolean hasMoreData = true;

            while (hasMoreData) {
                JSONArray foodsArray = null;
                int maxRetries = 3;
                int currentRetry = 0;

                while (currentRetry < maxRetries && foodsArray == null) {
                    try {
                        foodsArray = supabaseClient.from("foods")
                                .select("*,portions(*)")
                                .gt("updated_at", normalizedTimestamp)
                                .order("updated_at", true)
                                .limit(pageSize)
                                .offset(offset)
                                .executeAndGetArray();
                    } catch (SupabaseClient.TokenRefreshedException e) {
                        Log.d(TAG, "Токен обновлен, повторяем запрос. Попытка " + (currentRetry + 1));
                        currentRetry++;

                        if (currentRetry >= maxRetries) {
                            Log.e(TAG, "Не удалось выполнить запрос после " + maxRetries + " попыток");
                            throw e;
                        }
                    }
                }

                if (foodsArray == null || foodsArray.length() == 0) {
                    break;
                }

                Log.d(TAG, "Получено " + foodsArray.length() + " обновленных продуктов");

                for (int i = 0; i < foodsArray.length(); i++) {
                    JSONObject foodJson = foodsArray.getJSONObject(i);
                    Food food = parseFoodFromJson(foodJson);

                    if (food != null) {
                        foods.add(food);
                    }
                }

                if (foodsArray.length() < pageSize) {
                    hasMoreData = false;
                } else {
                    offset += pageSize;
                }
            }

            Log.d(TAG, "Всего получено " + foods.size() + " обновленных продуктов");
            return foods;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении обновленных продуктов: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    private Food parseFoodFromJson(JSONObject json) {
        try {

            Gson gson = new Gson();

            FoodDto foodDto = gson.fromJson(json.toString(), FoodDto.class);

            Food food = new Food.Builder()
                    .fromDto(foodDto)
                    .build();

            return food;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании Food из JSON: " + e.getMessage(), e);
            return null;
        }
    }


    public String addFood(Food food) {
        try {
            Log.d(TAG, "Добавление нового продукта в Supabase: " + food.getName());


            String foodName = food.getName();
            if (foodName == null || foodName.trim().isEmpty()) {
                Log.e(TAG, "Имя продукта пустое или null");
                return null;
            }


            Food existingFood = getFoodByName(foodName);
            if (existingFood != null) {

                String existingId = String.valueOf(existingFood.getId());
                Log.d(TAG, "Продукт с именем '" + foodName + "' уже существует в базе с ID: " + existingId);
                return existingId;
            }

            Log.d(TAG, "Продукт с именем '" + foodName + "' не найден в базе, создаем новый");


            String newUUID = java.util.UUID.randomUUID().toString();
            Log.d(TAG, "Сгенерирован UUID для нового продукта: " + newUUID);


            JSONObject foodData = new JSONObject();


            foodData.put("id", newUUID);
            foodData.put("name", foodName);


            if (food.getCategory() != null && !food.getCategory().isEmpty()) {
                foodData.put("category", food.getCategory());
            } else {
                foodData.put("category", "Другое");
            }

            if (food.getSubcategory() != null && !food.getSubcategory().isEmpty()) {
                foodData.put("subcategory", food.getSubcategory());
            } else {
                foodData.put("subcategory", "Другое");
            }


            if (food.getCalories() > 0) {
                foodData.put("calories", food.getCalories());
            } else {
                foodData.put("calories", 0);
            }


            addFloatIfValid(foodData, "proteins", food.getProteins());
            addFloatIfValid(foodData, "fats", food.getFats());
            addFloatIfValid(foodData, "carbs", food.getCarbs());

            if (food.getPopularity() > 0) {
                foodData.put("popularity", food.getPopularity());
            }

            addFloatIfValid(foodData, "calcium", food.getCalcium());
            addFloatIfValid(foodData, "iron", food.getIron());
            addFloatIfValid(foodData, "magnesium", food.getMagnesium());
            addFloatIfValid(foodData, "phosphorus", food.getPhosphorus());
            addFloatIfValid(foodData, "potassium", food.getPotassium());
            addFloatIfValid(foodData, "sodium", food.getSodium());
            addFloatIfValid(foodData, "zinc", food.getZinc());
            addFloatIfValid(foodData, "vitamin_a", food.getVitaminA());
            addFloatIfValid(foodData, "vitamin_b1", food.getVitaminB1());
            addFloatIfValid(foodData, "vitamin_b2", food.getVitaminB2());
            addFloatIfValid(foodData, "vitamin_b3", food.getVitaminB3());
            addFloatIfValid(foodData, "vitamin_b5", food.getVitaminB5());
            addFloatIfValid(foodData, "vitamin_b6", food.getVitaminB6());
            addFloatIfValid(foodData, "vitamin_b9", food.getVitaminB9());
            addFloatIfValid(foodData, "vitamin_b12", food.getVitaminB12());
            addFloatIfValid(foodData, "vitamin_c", food.getVitaminC());
            addFloatIfValid(foodData, "vitamin_d", food.getVitaminD());
            addFloatIfValid(foodData, "vitamin_e", food.getVitaminE());
            addFloatIfValid(foodData, "vitamin_k", food.getVitaminK());
            addFloatIfValid(foodData, "cholesterol", food.getCholesterol());
            addFloatIfValid(foodData, "saturated_fats", food.getSaturatedFats());
            addFloatIfValid(foodData, "trans_fats", food.getTransFats());
            addFloatIfValid(foodData, "fiber", food.getFiber());
            addFloatIfValid(foodData, "sugar", food.getSugar());

            if (food.getUsefulnessIndex() > 0) {
                foodData.put("usefulness_index", food.getUsefulnessIndex());
            }


            foodData.put("is_liquid", food.isLiquid());


            foodData.put("is_moderated", false);


            Log.d(TAG, "Оптимизированный JSON для создания продукта: " + foodData);
            Log.d(TAG, "Количество полей в JSON: " + foodData.length());


            try {
                JSONArray result = supabaseClient.from("foods")
                        .insert(foodData)
                        .executeAndGetArray();

                if (result != null && result.length() > 0) {
                    Log.d(TAG, "Продукт успешно добавлен: " + result);

                } else {
                    Log.d(TAG, "Продукт добавлен, но сервер не вернул данные");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при добавлении продукта в Supabase: " + e.getMessage(), e);

            }

            return newUUID;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при добавлении продукта: " + e.getMessage(), e);
            return null;
        }
    }


    private void addFloatIfValid(JSONObject json, String key, float value) {
        try {
            if (!Float.isNaN(value) && value > 0) {
                json.put(key, value);
            } else if (Float.isNaN(value)) {
                Log.d(TAG, "Пропущено NaN значение для ключа '" + key + "'");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при добавлении значения для ключа '" + key + "': " + e.getMessage(), e);
        }
    }


    public Food getFoodByName(String foodName) {
        try {
            Log.d(TAG, "Поиск продукта по точному имени: '" + foodName + "'");

            if (foodName == null || foodName.trim().isEmpty()) {
                Log.e(TAG, "Пустое имя продукта в запросе");
                return null;
            }


            String normalizedFoodName = foodName.trim();

            JSONArray foodsArray = null;
            int maxRetries = 3;
            int currentRetry = 0;


            while (currentRetry < maxRetries && foodsArray == null) {
                try {
                    foodsArray = supabaseClient.from("foods")
                            .select("*")
                            .eq("name", normalizedFoodName)
                            .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {
                    Log.d(TAG, "Токен обновлен, повторяем запрос поиска по имени. Попытка " + (currentRetry + 1));
                    currentRetry++;

                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос поиска по имени после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }

            if (foodsArray == null) {
                Log.e(TAG, "Не удалось получить результаты поиска по имени после обновления токена");
                return null;
            }


            if (foodsArray.length() > 0) {
                JSONObject foodJson = foodsArray.getJSONObject(0);
                Food food = parseFoodFromJson(foodJson);
                Log.d(TAG, "Найден продукт по имени: " + food.getName() + " (ID: " + food.getId() + ")");
                return food;
            } else {
                Log.d(TAG, "Продукт с именем '" + normalizedFoodName + "' не найден в базе данных");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при поиске продукта по имени: " + e.getMessage(), e);
            return null;
        }
    }


    public List<String> getAllUniqueCategories() {
        try {
            Log.d(TAG, "Получение всех уникальных категорий из кэша");

            List<String> categories = foodCacheDao.getAllCategories();

            Log.d(TAG, "Получено " + categories.size() + " уникальных категорий");
            return categories;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении уникальных категорий: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public List<String> getUniqueSubcategoriesForCategory(String category) {
        try {
            Log.d(TAG, "Получение уникальных подкатегорий для категории: " + category);

            List<String> subcategories = foodCacheDao.getSubcategoriesForCategory(category);

            Log.d(TAG, "Получено " + subcategories.size() + " уникальных подкатегорий");
            return subcategories;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении уникальных подкатегорий: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public String addPortion(String foodId, String portionName, int weight) {
        try {
            Log.d(TAG, "Добавление порции для продукта " + foodId + ": " + portionName + " = " + weight + "г");


            String newUUID = java.util.UUID.randomUUID().toString();
            Log.d(TAG, "Сгенерирован UUID для новой порции: " + newUUID);


            JSONObject portionData = new JSONObject();
            portionData.put("id", newUUID);
            portionData.put("food_id", foodId);
            portionData.put("name", portionName);
            portionData.put("weight", weight);

            Log.d(TAG, "JSON для создания порции: " + portionData);


            try {
                JSONArray result = supabaseClient.from("portions")
                        .insert(portionData)
                        .executeAndGetArray();

                if (result != null && result.length() > 0) {
                    Log.d(TAG, "Порция успешно добавлена: " + result);
                } else {
                    Log.d(TAG, "Порция добавлена, но сервер не вернул данные");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при добавлении порции в Supabase: " + e.getMessage(), e);
            }

            return newUUID;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при добавлении порции: " + e.getMessage(), e);
            return null;
        }
    }
} 