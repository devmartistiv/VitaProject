package com.martist.vitamove.nutrition.data.managers;

import android.content.Context;
import android.util.Log;

import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.nutrition.data.local.dao.DishDao;
import com.martist.vitamove.nutrition.data.local.dao.DishWithIngredients;
import com.martist.vitamove.nutrition.data.local.entities.DishEntity;
import com.martist.vitamove.nutrition.data.local.entities.DishIngredientEntity;
import com.martist.vitamove.nutrition.ui.model.Dish;
import com.martist.vitamove.nutrition.ui.model.Food;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DishManager {
    private static final String TAG = "DishManager";
    private static DishManager instance;
    private final DishDao dishDao;
    private final FoodManager foodManager;
    private final ExecutorService executor;

    private DishManager(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.dishDao = database.dishDao();
        this.foodManager = FoodManager.getInstance(context);
        this.executor = Executors.newFixedThreadPool(2);
    }

    public static synchronized DishManager getInstance(Context context) {
        if (instance == null) {
            instance = new DishManager(context.getApplicationContext());
        }
        return instance;
    }


    public void saveDish(Dish dish, OnDishSavedListener listener) {
        executor.execute(() -> {
            try {
                if (dish.getId() != null) {

                    Log.d(TAG, "Обновляем существующее блюдо с ID: " + dish.getId());
                    updateExistingDish(dish, listener);
                } else {

                    Log.d(TAG, "Создаем новое блюдо: " + dish.getName());
                    createNewDish(dish, listener);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при сохранении блюда: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError("Ошибка при сохранении блюда");
                }
            }
        });
    }


    private void createNewDish(Dish dish, OnDishSavedListener listener) {
        try {
            String dishID = UUID.randomUUID().toString();

            DishEntity dishEntity = new DishEntity(dishID, dish.getName(), dish.getDescription(), System.currentTimeMillis(), System.currentTimeMillis());

            dish.setId(dishID);
            List<DishIngredientEntity> ingredientEntities = new ArrayList<>();


            for (Dish.DishIngredient ingredient : dish.getIngredients()) {
                DishIngredientEntity entity = new DishIngredientEntity(
                        dishID,
                        ingredient.getFood().getId(),
                        ingredient.getFood().getName(),
                        ingredient.getQuantity(),
                        ingredient.getPortionName()
                );
                ingredientEntities.add(entity);
                Log.d(TAG, "Сохраняем ингредиент: " + ingredient.getFood().getName() +
                        ", количество: " + ingredient.getQuantity() + " " + ingredient.getPortionName());
            }

            Log.d(TAG, "Общее количество ингредиентов для сохранения: " + ingredientEntities.size());

            dishDao.createDishWithIngredients(dishEntity, ingredientEntities);


            Log.d(TAG, "Новое блюдо сохранено с ID: " + dishID);

            if (listener != null) {
                listener.onDishSaved(dish);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании нового блюда: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Ошибка при создании блюда");
            }
        }
    }


    private void updateExistingDish(Dish dish, OnDishSavedListener listener) {
        try {

            DishEntity dishEntity = new DishEntity(dish.getId(), dish.getName(), dish.getDescription(), dish.getCreatedAt(), System.currentTimeMillis());


            List<DishIngredientEntity> ingredientEntities = new ArrayList<>();
            for (Dish.DishIngredient ingredient : dish.getIngredients()) {
                DishIngredientEntity entity = new DishIngredientEntity(
                        dish.getId(),
                        ingredient.getFood().getId(),
                        ingredient.getFood().getName(),
                        ingredient.getQuantity(),
                        ingredient.getPortionName()
                );
                ingredientEntities.add(entity);
                Log.d(TAG, "Обновляем ингредиент: " + ingredient.getFood().getName() +
                        ", количество: " + ingredient.getQuantity() + " " + ingredient.getPortionName());
            }

            Log.d(TAG, "Общее количество ингредиентов для обновления: " + ingredientEntities.size());


            dishDao.updateDishWithIngredients(dishEntity, ingredientEntities);
            dish.setUpdatedAt(dishEntity.getUpdatedAt());

            Log.d(TAG, "Блюдо обновлено с ID: " + dish.getId());

            if (listener != null) {
                listener.onDishSaved(dish);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении блюда: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Ошибка при обновлении блюда");
            }
        }
    }


    public void getAllDishes(OnDishesLoadedListener listener) {
        executor.execute(() -> {
            try {
                List<DishWithIngredients> dishesWithIngredients = dishDao.getAllDishesWithIngredients();
                List<Dish> dishes = new ArrayList<>();

                for (DishWithIngredients dishWithIngredients : dishesWithIngredients) {
                    Dish dish = convertDishWithIngredientsToDish(dishWithIngredients);
                    dishes.add(dish);
                }

                Log.d(TAG, "Загружено блюд с ингредиентами: " + dishes.size());

                if (listener != null) {
                    listener.onDishesLoaded(dishes);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке блюд: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError("Ошибка при загрузке блюд");
                }
            }
        });
    }


    public void getDishWithIngredients(String dishId, OnDishLoadedListener listener) {
        executor.execute(() -> {
            try {
                DishWithIngredients dishWithIngredients = dishDao.getDishWithIngredients(dishId);
                if (dishWithIngredients != null) {
                    Dish dish = convertDishWithIngredientsToDish(dishWithIngredients);
                    if (listener != null) {
                        listener.onDishLoaded(dish);
                    }
                } else {
                    if (listener != null) {
                        listener.onError("Блюдо не найдено");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке блюда: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError("Ошибка при загрузке блюда");
                }
            }
        });
    }


    public void deleteDish(Dish dish, OnDishDeletedListener listener) {
        executor.execute(() -> {
            try {
                dishDao.deleteDishWithIngredients(dish.getId());


                if (listener != null) {
                    listener.onDishDeleted();
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при удалении блюда: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError("Ошибка при удалении блюда");
                }
            }
        });
    }


    public void searchDishes(String query, OnDishesLoadedListener listener) {
        executor.execute(() -> {
            try {
                List<DishEntity> dishEntities = dishDao.searchDishesByName(query);
                List<Dish> dishes = new ArrayList<>();

                for (DishEntity entity : dishEntities) {
                    Dish dish = convertEntityToDish(entity);
                    dishes.add(dish);
                }

                Log.d(TAG, "Найдено блюд по запросу '" + query + "': " + dishes.size());

                if (listener != null) {
                    listener.onDishesLoaded(dishes);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при поиске блюд: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError("Ошибка при поиске блюд");
                }
            }
        });
    }


    private Dish convertEntityToDish(DishEntity entity) {
        Dish dish = new Dish();
        dish.setId(entity.getId());
        dish.setName(entity.getName());
        dish.setDescription(entity.getDescription());
        dish.setCreatedAt(entity.getCreatedAt());
        dish.setUpdatedAt(entity.getUpdatedAt());
        return dish;
    }

    private Dish convertDishWithIngredientsToDish(DishWithIngredients dishWithIngredients) {
        Dish dish = convertEntityToDish(dishWithIngredients.getDish());

        Log.d(TAG, "Конвертируем блюдо: " + dish.getName() +
                ", ингредиентов в БД: " + dishWithIngredients.getIngredients().size());


        List<Dish.DishIngredient> ingredients = new ArrayList<>();
        for (DishIngredientEntity ingredientEntity : dishWithIngredients.getIngredients()) {
            Log.d(TAG, "Обрабатываем ингредиент с ID: " + ingredientEntity.getFoodId() +
                    ", название: " + ingredientEntity.getFoodName());


            Food food = foodManager.getFoodById(ingredientEntity.getFoodId());
            if (food != null) {
                Dish.DishIngredient ingredient = new Dish.DishIngredient(
                        food,
                        ingredientEntity.getQuantity(),
                        ingredientEntity.getPortionName()
                );
                ingredients.add(ingredient);
                Log.d(TAG, "Загружен ингредиент: " + food.getName() +
                        ", количество: " + ingredientEntity.getQuantity() + " " + ingredientEntity.getPortionName());
            } else {
                Log.w(TAG, "Не удалось найти продукт с ID: " + ingredientEntity.getFoodId() +
                        " для ингредиента: " + ingredientEntity.getFoodName());
            }
        }
        dish.setIngredients(ingredients);

        Log.d(TAG, "Блюдо '" + dish.getName() + "' загружено с " + ingredients.size() + " ингредиентами из " +
                dishWithIngredients.getIngredients().size() + " записей в БД, калорий: " + dish.getTotalCalories());

        return dish;
    }


    public interface OnDishSavedListener {
        void onDishSaved(Dish dish);

        void onError(String error);
    }

    public interface OnDishesLoadedListener {
        void onDishesLoaded(List<Dish> dishes);

        void onError(String error);
    }

    public interface OnDishLoadedListener {
        void onDishLoaded(Dish dish);

        void onError(String error);
    }

    public interface OnDishDeletedListener {
        void onDishDeleted();

        void onError(String error);
    }
} 