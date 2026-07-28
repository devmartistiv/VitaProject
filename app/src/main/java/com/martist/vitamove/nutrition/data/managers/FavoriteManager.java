package com.martist.vitamove.nutrition.data.managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.martist.vitamove.R;
import com.martist.vitamove.nutrition.ui.model.Food;

public class FavoriteManager {
    private ImageButton favoriteButton;
    private Food selectedFood;
    private Activity activity;
    private boolean isFoodInFavorites;
    private FoodManager foodManager;

    public FavoriteManager(Context context, Food selectedFood, Activity activity) {
        this.foodManager = FoodManager.getInstance(context);
        this.selectedFood = selectedFood;
        this.activity = activity;
    }

    public void initializeButton() {
        favoriteButton = activity.findViewById(R.id.favorite_button);
        favoriteButton.setOnClickListener(v -> {
            toggleFavorite();
        });

    }

    public void checkFavoriteStatus() {
        if (selectedFood == null || favoriteButton == null) {
            Log.w("TAG", "selectedFood или favoriteButton равен null, пропускаем проверку избранного");
            return;
        }


        new Thread(() -> {
            try {

                isFoodInFavorites = foodManager.isFoodInFavorites(selectedFood.getId());


                activity.runOnUiThread(() -> {
                    updateFavoriteButtonIcon();
                    Log.d("TAG", "Статус избранного для продукта '" + selectedFood.getName() + "': " + isFoodInFavorites);
                });
            } catch (Exception e) {
                Log.e("TAG", "Ошибка при проверке статуса избранного: " + e.getMessage());

                activity.runOnUiThread(() -> {
                    isFoodInFavorites = false;
                    updateFavoriteButtonIcon();
                });
            }
        }).start();
    }

    private void toggleFavorite() {
        if (selectedFood != null) {

            new Thread(() -> {
                try {
                    if (isFoodInFavorites) {

                        foodManager.removeFromFavorites(selectedFood.getId());
                        activity.runOnUiThread(() -> {
                            isFoodInFavorites = false;
                            updateFavoriteButtonIcon();
                            Toast.makeText(activity, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                            Log.d("TAG", "Продукт '" + selectedFood.getName() + "' удален из избранного");
                        });
                    } else {

                        foodManager.addToFavorites(selectedFood);
                        activity.runOnUiThread(() -> {
                            isFoodInFavorites = true;
                            updateFavoriteButtonIcon();
                            Toast.makeText(activity, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                            Log.d("TAG", "Продукт '" + selectedFood.getName() + "' добавлен в избранное");
                        });
                    }
                } catch (Exception e) {
                    Log.e("DEBUG", "Ошибка при работе с избранным: " + e.getMessage());
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Ошибка при обновлении избранного", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }

    private void updateFavoriteButtonIcon() {
        if (favoriteButton != null) {
            if (isFoodInFavorites) {
                favoriteButton.setImageResource(R.drawable.ic_heart_filled);
            } else {
                favoriteButton.setImageResource(R.drawable.ic_heart_border);
            }
        }
    }
}