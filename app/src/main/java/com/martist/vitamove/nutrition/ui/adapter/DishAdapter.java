package com.martist.vitamove.nutrition.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.martist.vitamove.R;
import com.martist.vitamove.nutrition.ui.model.Dish;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class DishAdapter extends RecyclerView.Adapter<DishAdapter.DishViewHolder> {
    private List<Dish> dishes;
    private OnDishClickListener onDishClickListener;
    private OnDishAddListener onDishAddListener;
    private OnDishLongClickListener onDishLongClickListener;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    public DishAdapter(List<Dish> dishes, OnDishClickListener clickListener) {
        this.dishes = dishes != null ? dishes : new ArrayList<>();
        this.onDishClickListener = clickListener;
    }

    public void setOnDishAddListener(OnDishAddListener listener) {
        this.onDishAddListener = listener;
    }

    public void setOnDishLongClickListener(OnDishLongClickListener listener) {
        this.onDishLongClickListener = listener;
    }

    @NonNull
    @Override
    public DishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dish, parent, false);
        return new DishViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DishViewHolder holder, int position) {
        Dish dish = dishes.get(position);
        holder.bind(dish);
    }

    @Override
    public int getItemCount() {
        return dishes.size();
    }

    public void updateDishes(List<Dish> newDishes) {
        this.dishes.clear();
        if (newDishes != null) {
            this.dishes.addAll(newDishes);
        }
        notifyDataSetChanged();
    }

    public class DishViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView dishName;
        private final TextView dishDescription;
        private final TextView caloriesText;
        private final TextView nutrientsText;
        private final FloatingActionButton addButton;

        public DishViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.dish_card);
            dishName = itemView.findViewById(R.id.dish_name);
            dishDescription = itemView.findViewById(R.id.dish_description);
            caloriesText = itemView.findViewById(R.id.calories_text);
            nutrientsText = itemView.findViewById(R.id.nutrients_text);
            addButton = itemView.findViewById(R.id.add_button);
        }

        public void bind(Dish dish) {
            dishName.setText(dish.getName());


            if (dish.getDescription() != null && !dish.getDescription().trim().isEmpty()) {
                dishDescription.setVisibility(View.VISIBLE);
                dishDescription.setText(dish.getDescription());
            } else {
                dishDescription.setVisibility(View.GONE);
            }


            float totalCalories = dish.getTotalCalories();
            caloriesText.setText(String.format("%.0f ккал", totalCalories));


            float proteins = dish.getTotalProteins();
            float fats = dish.getTotalFats();
            float carbs = dish.getTotalCarbs();

            String nutrientsStr = String.format("Б: %s • Ж: %s • У: %s",
                    decimalFormat.format(proteins),
                    decimalFormat.format(fats),
                    decimalFormat.format(carbs));
            nutrientsText.setText(nutrientsStr);


            addButton.setOnClickListener(v -> {
                if (onDishAddListener != null) {
                    onDishAddListener.onDishAdd(dish);
                }
            });


            cardView.setOnClickListener(v -> {
                if (onDishClickListener != null) {
                    onDishClickListener.onDishClick(dish);
                }
            });


            cardView.setOnLongClickListener(v -> {
                if (onDishLongClickListener != null) {
                    onDishLongClickListener.onDishLongClick(dish);
                    return true;
                }
                return false;
            });
        }
    }

    public interface OnDishClickListener {
        void onDishClick(Dish dish);
    }

    public interface OnDishAddListener {
        void onDishAdd(Dish dish);
    }

    public interface OnDishLongClickListener {
        void onDishLongClick(Dish dish);
    }
} 