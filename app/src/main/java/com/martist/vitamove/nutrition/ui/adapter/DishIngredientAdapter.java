package com.martist.vitamove.nutrition.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.PluralizationUtil;
import com.martist.vitamove.nutrition.ui.model.Dish;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;


public class DishIngredientAdapter extends RecyclerView.Adapter<DishIngredientAdapter.IngredientViewHolder> {
    private List<Dish.DishIngredient> ingredients;
    private OnIngredientDeleteListener deleteListener;
    private OnIngredientClickListener clickListener;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    public DishIngredientAdapter(List<Dish.DishIngredient> ingredients, OnIngredientDeleteListener deleteListener) {
        this.ingredients = ingredients;
        this.deleteListener = deleteListener;
    }

    public void setOnIngredientClickListener(OnIngredientClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dish_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Dish.DishIngredient ingredient = ingredients.get(position);
        holder.bind(ingredient);
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public class IngredientViewHolder extends RecyclerView.ViewHolder {
        private final TextView ingredientName;
        private final TextView ingredientAmount;
        private final TextView ingredientCalories;
        private final ImageButton btnDelete;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            ingredientName = itemView.findViewById(R.id.ingredient_name);
            ingredientAmount = itemView.findViewById(R.id.ingredient_amount);
            ingredientCalories = itemView.findViewById(R.id.ingredient_calories);
            btnDelete = itemView.findViewById(R.id.btn_delete_ingredient);
        }

        public void bind(Dish.DishIngredient ingredient) {

            ingredientName.setText(ingredient.getFood().getName());


            float quantity = ingredient.getQuantity();
            String portionName = ingredient.getPortionName();

            String amountText;
            if (portionName != null && !portionName.equals("грамм") && !portionName.equals("мл")) {

                String pluralPortionName = PluralizationUtil.getPlural(quantity, portionName);
                if (quantity == (int) quantity) {

                    amountText = String.format(Locale.getDefault(), "%d %s", (int) quantity, pluralPortionName);
                } else {

                    amountText = String.format(Locale.getDefault(), "%.1f %s", quantity, pluralPortionName);
                }
            } else {

                if (quantity == (int) quantity) {
                    amountText = String.format(Locale.getDefault(), "%d %s", (int) quantity, portionName);
                } else {
                    amountText = String.format(Locale.getDefault(), "%.1f %s", quantity, portionName);
                }
            }
            ingredientAmount.setText(amountText);


            float multiplier = ingredient.getTotalWeightInGrams() / 100f;
            float calories = ingredient.getFood().getCalories() * multiplier;
            ingredientCalories.setText(String.format("%.0f ккал", calories));


            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onIngredientClick(ingredient, position);
                }
            });


            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && deleteListener != null) {
                    deleteListener.onIngredientDelete(position);
                }
            });
        }
    }

    public interface OnIngredientDeleteListener {
        void onIngredientDelete(int position);
    }

    public interface OnIngredientClickListener {
        void onIngredientClick(Dish.DishIngredient ingredient, int position);
    }
} 