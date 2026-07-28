package com.martist.vitamove.exercise.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.ExerciseCategory;

import java.util.ArrayList;
import java.util.List;


public class ExerciseCategoryAdapter extends RecyclerView.Adapter<ExerciseCategoryAdapter.CategoryViewHolder> {
    private final Context context;
    private List<ExerciseCategory> categories;
    private final OnCategoryClickListener listener;


    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }


    public ExerciseCategoryAdapter(Context context, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = new ArrayList<>();
        this.listener = listener;
    }


    public void setCategories(List<ExerciseCategory> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(categories.get(position));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }


    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryName;
        private final TextView exerciseCount;
        private final ImageView categoryIcon;
        private final MaterialCardView categoryCard;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryName);
            exerciseCount = itemView.findViewById(R.id.exerciseCount);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            categoryCard = itemView.findViewById(R.id.categoryCard);
        }

        void bind(ExerciseCategory category) {
            categoryName.setText(category.getName());


            String countText = getExerciseCountText(category.getExerciseCount());
            exerciseCount.setText(countText);


            setIconForCategory(category.getName());


            categoryCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category.getName());
                }
            });
        }


        private String getExerciseCountText(int count) {
            if (count == 0) {
                return "Нет упражнений";
            } else if (count == 1) {
                return "1 упражнение";
            } else if (count >= 2 && count <= 4) {
                return count + " упражнения";
            } else {
                return count + " упражнений";
            }
        }


        private void setIconForCategory(String categoryName) {
            int iconResId;
            switch (categoryName) {

                case "Бицепс":
                    iconResId = R.drawable.ic_biceps;
                    break;
                case "Грудь":
                    iconResId = R.drawable.ic_chest;
                    break;
                case "Кор":
                    iconResId = R.drawable.ic_core;
                    break;
                case "Ноги":
                    iconResId = R.drawable.ic_legs;
                    break;
                case "Плечи":
                    iconResId = R.drawable.ic_shoulders;
                    break;
                case "Предплечье":
                    iconResId = R.drawable.ic_forearms;
                    break;
                case "Спина":
                    iconResId = R.drawable.ic_back;
                    break;
                case "Трицепс":
                    iconResId = R.drawable.ic_triceps;
                    break;
                case "Ягодицы":
                    iconResId = R.drawable.ic_glutes;
                    break;


                case "кардио":
                    iconResId = R.drawable.ic_cardio;
                    break;
                case "разминка":
                    iconResId = R.drawable.ic_warmup;
                    break;
                case "растяжка":
                    iconResId = R.drawable.ic_stretching;
                    break;
                case "реабилитационное":
                    iconResId = R.drawable.ic_rehabilitation;
                    break;
                case "силовое":
                    iconResId = R.drawable.ic_strength;
                    break;
                case "с собственным весом":
                    iconResId = R.drawable.ic_bodyweight;
                    break;
                case "статическое":
                    iconResId = R.drawable.ic_static;
                    break;
                case "функциональное":
                    iconResId = R.drawable.ic_functional;
                    break;

                default:
                    iconResId = R.drawable.ic_exercise_default;
                    break;
            }


            try {
                categoryIcon.setImageResource(iconResId);
            } catch (Exception e) {
                categoryIcon.setImageResource(R.drawable.ic_exercise_default);
            }
        }
    }
}