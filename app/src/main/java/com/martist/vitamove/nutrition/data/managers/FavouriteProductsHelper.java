package com.martist.vitamove.nutrition.data.managers;

import android.util.Log;

import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.nutrition.data.local.entities.FavoriteFoodEntity;
import com.martist.vitamove.nutrition.ui.model.Food;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class FavouriteProductsHelper {
    private final Executor executor;
    private final AppDatabase appDatabase;
    private final FoodManager foodManager;
    private static final String TAG = "FAVOURITE";

    public FavouriteProductsHelper(AppDatabase appDatabase, ExecutorService executor, FoodManager foodManager) {
        this.appDatabase = appDatabase;
        this.executor = executor;
        this.foodManager = foodManager;
    }

    public void addToFavorites(Food food) {
        executor.execute(() -> {
            try {

                if (food.getId() != null && food.getId().startsWith("virtual_")) {

                    return;
                }

                FavoriteFoodEntity favoriteEntity = new FavoriteFoodEntity(
                        food.getId(),
                        food.getName(),
                        food.getCategory(),
                        food.getSubcategory(),
                        food.getCalories(),
                        food.getProteins(),
                        food.getFats(),
                        food.getCarbs(),
                        new Date()
                );

                appDatabase.favoriteFoodDao().addToFavorites(favoriteEntity);
            } catch (Exception e) {
            }
        });
    }


    public void removeFromFavorites(String foodId) {
        executor.execute(() -> {
            try {
                appDatabase.favoriteFoodDao().removeFromFavoritesByFoodId(foodId);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при удалении продукта из избранного: " + e.getMessage());
            }
        });
    }


    public boolean isFoodInFavorites(String foodId) {
        try {
            return appDatabase.favoriteFoodDao().isFoodInFavorites(foodId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке избранного: " + e.getMessage());
            return false;
        }
    }


    public List<Food> getFavoriteFoods() {
        try {
            List<FavoriteFoodEntity> favoriteEntities = appDatabase.favoriteFoodDao().getAllFavoriteFoods();
            List<Food> favoriteFoods = new ArrayList<>();
            favoriteEntities.forEach(value -> favoriteFoods.add(foodManager.getFoodById(value.getFoodId())));
            return favoriteFoods;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке избранных продуктов: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public int getFavoriteFoodsCount() {
        try {
            return appDatabase.favoriteFoodDao().getFavoriteFoodsCount();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении количества избранных: " + e.getMessage());
            return 0;
        }
    }


    public void clearAllFavorites() {
        executor.execute(() -> {
            try {
                appDatabase.favoriteFoodDao().clearAllFavorites();
                Log.d(TAG, "Все избранные продукты очищены");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при очистке избранных: " + e.getMessage());
            }
        });
    }
}
