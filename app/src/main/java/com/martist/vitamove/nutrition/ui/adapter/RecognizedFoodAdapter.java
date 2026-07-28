package com.martist.vitamove.nutrition.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.core.data.services.VoiceInputService;
import com.martist.vitamove.nutrition.ui.model.Food;

import java.util.List;
import java.util.Locale;


public class RecognizedFoodAdapter extends RecyclerView.Adapter<RecognizedFoodAdapter.ViewHolder> {

    private final List<VoiceInputService.RecognizedFood> recognizedFoods;


    public interface OnItemClickListener {
        void onItemClick(VoiceInputService.RecognizedFood recognizedFood, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(VoiceInputService.RecognizedFood recognizedFood, int position);
    }

    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;


    public RecognizedFoodAdapter(List<VoiceInputService.RecognizedFood> recognizedFoods) {
        this.recognizedFoods = recognizedFoods;
    }


    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }


    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recognized_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoiceInputService.RecognizedFood recognizedFood = recognizedFoods.get(position);
        holder.bind(recognizedFood);
    }

    @Override
    public int getItemCount() {
        return recognizedFoods.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView foodNameText;
        private final TextView quantityText;
        private final TextView caloriesText;
        private final TextView macrosText;
        private final ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            foodNameText = itemView.findViewById(R.id.food_name_text);
            quantityText = itemView.findViewById(R.id.quantity_text);
            caloriesText = itemView.findViewById(R.id.calories_text);
            macrosText = itemView.findViewById(R.id.macros_text);
            deleteButton = itemView.findViewById(R.id.delete_button);


            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(recognizedFoods.get(position), position);
                }
            });


            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClick(recognizedFoods.get(position), position);
                }
            });
        }


        public void bind(VoiceInputService.RecognizedFood recognizedFood) {
            Food food = recognizedFood.getFoundFood();


            android.util.Log.d("RecognizedFoodAdapter", "bind() вызван для продукта: " + recognizedFood.getName() +
                    ", quantity=" + recognizedFood.getQuantity() +
                    ", unit=" + recognizedFood.getUnit() +
                    ", displayQuantity=" + recognizedFood.getDisplayQuantity());

            if (food != null) {

                foodNameText.setText(food.getName());


                String displayQuantity = recognizedFood.getDisplayQuantity();
                quantityText.setText(displayQuantity);
                android.util.Log.d("RecognizedFoodAdapter", "Установили quantityText: " + displayQuantity);


                float totalWeightInGrams = recognizedFood.getTotalWeightInGrams();
                float multiplier = totalWeightInGrams / 100f;

                int calories = Math.round(food.getCalories() * multiplier);
                float proteins = food.getProteins() * multiplier;
                float fats = food.getFats() * multiplier;
                float carbs = food.getCarbs() * multiplier;

                android.util.Log.d("RecognizedFoodAdapter", "Пересчитанные данные: calories=" + calories +
                        ", proteins=" + proteins + ", fats=" + fats + ", carbs=" + carbs +
                        ", totalWeight=" + totalWeightInGrams);


                String caloriesDisplay;
                if (recognizedFood.getFoundPortion() != null) {
                    caloriesDisplay = String.format(Locale.getDefault(),
                            "%d ккал (%.0fг)", calories, totalWeightInGrams);
                } else {
                    caloriesDisplay = String.format(Locale.getDefault(), "%d ккал", calories);
                }
                caloriesText.setText(caloriesDisplay);
                android.util.Log.d("RecognizedFoodAdapter", "Установили caloriesText: " + caloriesDisplay);


                String macrosDisplay = String.format(Locale.getDefault(),
                        "Б: %.1fг, Ж: %.1fг, У: %.1fг", proteins, fats, carbs);
                macrosText.setText(macrosDisplay);
                android.util.Log.d("RecognizedFoodAdapter", "Установили macrosText: " + macrosDisplay);
            } else {

                foodNameText.setText(recognizedFood.getName() + " (не найден в базе)");
                quantityText.setText(recognizedFood.getDisplayQuantity());
                caloriesText.setText("—");
                macrosText.setText("—");
            }
        }
    }
}
