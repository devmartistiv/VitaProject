package com.martist.vitamove.nutrition.ui.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.martist.vitamove.R;
import com.martist.vitamove.nutrition.ui.model.Dish;
import com.martist.vitamove.nutrition.ui.model.Food;

import java.util.ArrayList;
import java.util.List;

public class IngredientSelectionAdapter extends RecyclerView.Adapter<IngredientSelectionAdapter.IngredientViewHolder> {
    private List<Food> foods;
    private OnFoodSelectListener listener;
    private List<Dish.DishIngredient> selectedIngredients;

    public IngredientSelectionAdapter(List<Food> foods, OnFoodSelectListener listener,
                                      List<Dish.DishIngredient> selectedIngredients) {
        this.foods = foods != null ? foods : new ArrayList<>();
        this.listener = listener;
        this.selectedIngredients = selectedIngredients != null ? selectedIngredients : new ArrayList<>();
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_selection, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Food food = foods.get(position);
        holder.bind(food);
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    public void updateFoods(List<Food> newFoods) {
        this.foods.clear();
        if (newFoods != null) {
            this.foods.addAll(newFoods);
        }
        notifyDataSetChanged();
    }

    private Dish.DishIngredient findSelectedIngredient(String foodId) {
        for (Dish.DishIngredient ingredient : selectedIngredients) {
            if (ingredient.getFood().getId().equals(foodId)) {
                return ingredient;
            }
        }
        return null;
    }

    public class IngredientViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView foodName;
        private final MaterialButton selection;
        private final TextView calories;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.food_card);
            foodName = itemView.findViewById(R.id.food_name);
            calories = itemView.findViewById(R.id.food_calories);
            selection = itemView.findViewById(R.id.add_button);
        }

        public void bind(Food food) {
            foodName.setText(food.getName());
            Dish.DishIngredient selectedIngredient = findSelectedIngredient(food.getId());
            selection.setOnClickListener(v -> {
                if (listener != null) {
                    if (selectedIngredient != null) {
                        listener.onFoodRemoved(food);
                    } else {
                        if (food.getPortions() != null && !food.getPortions().isEmpty()) {
                            String portionName = food.getPortions().get(0).getName();
                            listener.onFoodAddedDirectly(food, 1f, portionName);
                        } else {
                            String portionName = food.isLiquid() ? "мл" : "грамм";
                            listener.onFoodAddedDirectly(food, 100f, portionName);
                        }
                    }
                }
            });
            if (selectedIngredient != null) {
                selection.setIconResource(R.drawable.ic_check);
                cardView.setStrokeWidth(2);
                cardView.setStrokeColor(itemView.getContext().getColor(R.color.colorAccent));
            } else {
                cardView.setStrokeWidth(2);
                cardView.setStrokeColor(itemView.getContext().getColor(R.color.colorGrey));
                selection.setIconResource(R.drawable.ic_add);
            }
            String unit = food.isLiquid() ? "100 мл" : "100 г";
            if (food.getPortions() != null && !food.getPortions().isEmpty()) {
                String portionName = "1 " + food.getPortions().get(0).getName();
                calories.setText(String.format("%d ккал, %s", Math.round(food.getCaloriesPerPorition()), portionName));
            } else {
                calories.setText(String.format("%d ккал, %s", Math.round(food.getCalories()), unit));
            }


            cardView.setOnClickListener(v -> {
                if (selectedIngredient != null) {
                    if (listener != null) {
                        listener.onFoodSelected(food);
                    }
                } else {
                    if (listener != null) {
                        listener.onFoodSelected(food);
                    }
                }
            });
        }
    }

    public interface OnFoodSelectListener {
        void onFoodSelected(Food food);

        void onFoodRemoved(Food food);

        void onFoodAddedDirectly(Food food, float quantity, String portionName);
    }

    public void updateSelectedIngredients(List<Dish.DishIngredient> newSelected) {
        this.selectedIngredients.clear();
        if (newSelected != null) {
            this.selectedIngredients.addAll(newSelected);
        }
        notifyDataSetChanged();
    }
}

