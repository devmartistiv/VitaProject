package com.martist.vitamove.nutrition.ui.adapter;

import static com.martist.vitamove.VitaMoveApplication.context;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.nutrition.data.local.entities.RecentFoodEntity;
import com.martist.vitamove.nutrition.ui.model.Food;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class RecentFoodAdapter extends RecyclerView.Adapter<RecentFoodAdapter.ViewHolder> {
    private List<RecentFoodWithCalories> recentFoodsWithCalories;
    private OnRecentFoodClickListener listener;
    private OnRecentFoodAddButtonClickListener addButtonListener;
    private AppDatabase appDatabase = AppDatabase.getInstance(context);
    private static final String TAG = "RecentFoodAdapter";


    private boolean isMultiSelectionMode = false;
    private Set<String> selectedFoodIds = new HashSet<>();


    public static class RecentFoodWithCalories {
        private final RecentFoodEntity recentFood;
        private final int calculatedCalories;
        private final Food food;

        public RecentFoodWithCalories(RecentFoodEntity recentFood, int calculatedCalories, Food food) {
            this.recentFood = recentFood;
            this.calculatedCalories = calculatedCalories;
            this.food = food;
        }

        public RecentFoodEntity getRecentFood() {
            return recentFood;
        }

        public int getCalculatedCalories() {
            return calculatedCalories;
        }

        public Food getFood() {
            return food;
        }
    }

    public interface OnRecentFoodClickListener {

        void onRecentFoodClick(RecentFoodEntity recentFood, Food food);
    }

    public interface OnRecentFoodAddButtonClickListener {
        void onRecentFoodAddButtonClick(Food food);
    }

    public RecentFoodAdapter(List<RecentFoodWithCalories> recentFoodsWithCalories, OnRecentFoodClickListener listener) {
        this.recentFoodsWithCalories = recentFoodsWithCalories;
        this.listener = listener;
    }

    public void setOnRecentFoodClickListener(OnRecentFoodClickListener listener) {
        this.listener = listener;
    }

    public void setOnRecentFoodAddButtonClickListener(OnRecentFoodAddButtonClickListener listener) {
        this.addButtonListener = listener;
    }

    public void updateRecentFoods(List<RecentFoodWithCalories> newRecentFoodsWithCalories) {
        Log.d(TAG, "Обновляем адаптер с " + (newRecentFoodsWithCalories != null ? newRecentFoodsWithCalories.size() : 0) + " недавними продуктами");
        this.recentFoodsWithCalories = newRecentFoodsWithCalories;
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
        RecentFoodWithCalories item = recentFoodsWithCalories.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return recentFoodsWithCalories != null ? recentFoodsWithCalories.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView calories;
        private final MaterialButton addButton;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.food_name);
            calories = view.findViewById(R.id.food_calories);
            addButton = view.findViewById(R.id.add_button);
        }

        void bind(RecentFoodWithCalories item) {
            RecentFoodEntity recentFood = item.getRecentFood();
            Food food = item.getFood();


            name.setText(recentFood.getFoodName());


            String caloriesInfo = String.format("%d ккал, %.0f %s",
                    item.getCalculatedCalories(),
                    recentFood.getQuantity(),
                    recentFood.getPortionName());
            calories.setText(caloriesInfo);
            boolean isSelected = food != null && selectedFoodIds.contains(food.getId());
            AtomicBoolean abs = new AtomicBoolean(false);
            new Thread(() -> {
                boolean flag = false;
                for (RecentFoodEntity recentFood1 : appDatabase.recentFoodDao().getAllRecentFoods()) {
                    if (item.recentFood.getId().equals(recentFood1.getId())) flag = true;
                }
                if (flag) {

                }

            }).start();


            if (isMultiSelectionMode && isSelected) {
                addButton.setIconResource(R.drawable.ic_check);
            } else {
                addButton.setIconResource(R.drawable.ic_add);
            }


            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecentFoodClick(recentFood, food);
                }
            });


            addButton.setOnClickListener(v -> {
                if (food != null) {
                    if (isMultiSelectionMode) {


                        if (addButtonListener != null) {
                            addButtonListener.onRecentFoodAddButtonClick(food);
                        }
                    } else {


                        if (addButtonListener != null) {
                            addButtonListener.onRecentFoodAddButtonClick(food);
                        }
                    }
                }
            });
        }
    }
}
