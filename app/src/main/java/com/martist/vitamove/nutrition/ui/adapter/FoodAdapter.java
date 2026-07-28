package com.martist.vitamove.nutrition.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.nutrition.ui.model.Food;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {
    private List<Food> foods;
    private static OnFoodClickListener listener;
    private static OnFoodAddButtonClickListener addButtonListener;
    private static final String TAG = "FoodAdapter";


    private boolean isMultiSelectionMode = false;
    private Set<String> selectedFoodIds = new HashSet<>();

    public interface OnFoodClickListener {
        void onFoodClick(Food food);
    }

    public interface OnFoodAddButtonClickListener {
        void onFoodAddButtonClick(Food food);
    }

    public FoodAdapter(List<Food> foods, OnFoodClickListener listener) {
        this.foods = foods;
        FoodAdapter.listener = listener;
    }

    public void setOnFoodClickListener(OnFoodClickListener listener) {
        FoodAdapter.listener = listener;
    }

    public void setOnFoodAddButtonClickListener(OnFoodAddButtonClickListener listener) {
        FoodAdapter.addButtonListener = listener;
    }

    public void updateFoods(List<Food> newFoods) {
        Log.d(TAG, "Updating adapter with " + (newFoods != null ? newFoods.size() : 0) + " items");
        if (newFoods != null) {
            for (Food food : newFoods) {
                Log.d(TAG, "Item in adapter: " + food.getName());
            }
        }
        this.foods = newFoods;
        notifyDataSetChanged();
    }


    public void setMultiSelectionMode(boolean enabled) {
        this.isMultiSelectionMode = enabled;
        notifyDataSetChanged();
    }

    public void setSelectedFoods(Set<String> selectedIds) {
        this.selectedFoodIds = selectedIds != null ? new HashSet<>(selectedIds) : new HashSet<>();
        notifyDataSetChanged();
    }

    public Set<String> getSelectedFoods() {
        return new HashSet<>(selectedFoodIds);
    }

    public void clearSelection() {
        selectedFoodIds.clear();
        isMultiSelectionMode = false;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Food food = foods.get(position);
        holder.bind(food);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFoodClick(food);
            }
        });
    }

    @Override
    public int getItemCount() {
        return foods != null ? foods.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView calories;
        private final MaterialButton addButton;
        private final ImageView iconNotModerated;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.food_name);
            calories = view.findViewById(R.id.food_calories);
            addButton = view.findViewById(R.id.add_button);
            iconNotModerated = view.findViewById(R.id.icon_not_moderated);
        }

        void bind(Food food) {
            name.setText(food.getName());


            if (iconNotModerated != null) {
                boolean isModerated = food.isModerated();
                iconNotModerated.setVisibility(isModerated ? View.GONE : View.VISIBLE);


                if (!isModerated) {
                    iconNotModerated.setOnClickListener(v -> {
                        android.widget.Toast.makeText(v.getContext(),
                                R.string.user_added_product_warning,
                                android.widget.Toast.LENGTH_LONG).show();
                    });
                }
            }


            String unit = food.isLiquid() ? "100 мл" : "100 г";

            if (food.getPortions() != null && !food.getPortions().isEmpty()) {
                String portionName = "1 " + food.getPortions().get(0).getName();
                calories.setText(String.format("%d ккал, %s",
                        Math.round(food.getCaloriesPerPorition()), portionName));
            } else {
                calories.setText(String.format("%d ккал, %s",
                        Math.round(food.getCalories()), unit));
            }


            boolean isSelected = selectedFoodIds.contains(food.getId());
            if (isMultiSelectionMode && isSelected) {
                addButton.setIconResource(R.drawable.ic_check);

            } else {
                addButton.setIconResource(R.drawable.ic_add);
            }


            addButton.setOnClickListener(v -> {
                if (isMultiSelectionMode) {

                    if (addButtonListener != null) {
                        addButtonListener.onFoodAddButtonClick(food);
                    }
                } else {

                    if (addButtonListener != null) {
                        addButtonListener.onFoodAddButtonClick(food);
                    }
                }
            });
        }
    }
} 