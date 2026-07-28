package com.martist.vitamove.nutrition.ui.adapter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.PluralizationUtil;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Meal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MealFoodsAdapter extends RecyclerView.Adapter<MealFoodsAdapter.ViewHolder> {
    private static final String TAG = "MealFoodsAdapter";
    private List<Meal.FoodPortion> foods;
    private final String mealType;
    private final int dailyCaloriesNorm;
    private ItemTouchHelper itemTouchHelper;
    private OnFoodRemovedListener onFoodRemovedListener;
    private OnFoodClickListener onFoodClickListener;
    private final Paint textPaint;
    private final ColorDrawable background;
    private Drawable deleteIcon;


    public interface OnFoodRemovedListener {
        void onFoodRemoved(int position, Meal.FoodPortion removedPortion);
    }


    public interface OnFoodClickListener {
        void onFoodClick(Meal.FoodPortion foodPortion, String mealType);
    }

    public MealFoodsAdapter(List<Meal.FoodPortion> foods, String mealType, int dailyCaloriesNorm) {
        this.foods = foods != null ? foods : new ArrayList<>();
        this.mealType = mealType;
        this.dailyCaloriesNorm = dailyCaloriesNorm > 0 ? dailyCaloriesNorm : 2000;
        this.textPaint = new Paint();
        this.background = new ColorDrawable(Color.RED);
    }

    public void setOnFoodRemovedListener(OnFoodRemovedListener listener) {
        this.onFoodRemovedListener = listener;
    }

    public void setOnFoodClickListener(OnFoodClickListener listener) {
        this.onFoodClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_food, parent, false);


        if (deleteIcon == null) {
            deleteIcon = ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_delete);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Meal.FoodPortion foodPortion = foods.get(position);
            Food food = foodPortion.getFood();
            float quantity = foodPortion.getQuantity();
            String portionName = foodPortion.getPortionName();
            int totalWeight = foodPortion.getTotalWeightInGrams();


            holder.foodName.setText(food.getName());


            setCategoryIndicatorColor(holder.categoryIndicator, food.getCategory());


            String portionText;
            if (portionName != null && !portionName.equals("грамм") && !portionName.equals("мл")) {
                String pluralPortionName = PluralizationUtil.getPlural(quantity, portionName);
                if (quantity == (int) quantity) {
                    portionText = String.format(Locale.getDefault(), "%d %s", (int) quantity, pluralPortionName);
                } else {
                    portionText = String.format(Locale.getDefault(), "%.1f %s", quantity, pluralPortionName);
                }
            } else {
                String unit = food.isLiquid() ? "мл" : "г";
                portionText = String.format(Locale.getDefault(), "%.0f %s", quantity, unit);
            }
            if (Objects.equals(food.getName(), "Быстрое добавление нутриентов")) {
                holder.portionChip.setVisibility(View.INVISIBLE);
            } else
                holder.portionChip.setText(portionText);


            float proteins = food.getProteins() * totalWeight / 100f;
            float fats = food.getFats() * totalWeight / 100f;
            float carbs = food.getCarbs() * totalWeight / 100f;

            holder.proteinsChip.setText(String.format(Locale.getDefault(), "Б %.1f", proteins));
            holder.fatsChip.setText(String.format(Locale.getDefault(), "Ж %.1f", fats));
            holder.carbsChip.setText(String.format(Locale.getDefault(), "У %.1f", carbs));


            int calories = Math.round(food.getCalories() * totalWeight / 100f);
            holder.foodCalories.setText(String.format("%d ккал", calories));


            int percent = Math.round((calories / (float) dailyCaloriesNorm) * 100);
            holder.caloriesPercent.setText(String.format("%d%% от нормы", percent));


            if (holder.portionSize != null) {
                holder.portionSize.setText(portionText);
            }
            if (holder.foodNutrients != null) {
                String nutrients = String.format("Б %.1f • Ж %.1f • У %.1f", proteins, fats, carbs);
                holder.foodNutrients.setText(nutrients);
            }

            holder.itemView.setVisibility(View.VISIBLE);


            holder.itemView.setOnClickListener(v -> {
                if (onFoodClickListener != null) {
                    onFoodClickListener.onFoodClick(foodPortion, mealType);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder at position " + position, e);
        }
    }


    private void setCategoryIndicatorColor(View indicator, String category) {
        if (indicator == null || category == null) return;

        int colorResId;
        switch (category.toLowerCase()) {
            case "мясо":
            case "птица":
                colorResId = R.color.red_500;
                break;
            case "рыба и морепродукты":
            case "рыба":
                colorResId = R.color.water_blue;
                break;
            case "молочные продукты":
            case "молоко":
                colorResId = R.color.orange_200;
                break;
            case "фрукты":
            case "ягоды":
                colorResId = R.color.green_500;
                break;
            case "овощи":
                colorResId = R.color.light_green_500;
                break;
            case "крупы и злаки":
            case "хлеб и выпечка":
                colorResId = R.color.yellow_500;
                break;
            case "готовые блюда":
            case "супы":
                colorResId = R.color.orange_500;
                break;
            default:
                colorResId = R.color.gray_400;
                break;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            indicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(indicator.getContext(), colorResId)));
        }
    }

    @Override
    public int getItemCount() {
        return foods != null ? foods.size() : 0;
    }

    public void updateFoods(List<Meal.FoodPortion> newFoods) {
        Log.d(TAG, "Updating foods list. Size: " + (newFoods != null ? newFoods.size() : 0));
        this.foods = newFoods != null ? newFoods : new ArrayList<>();
        notifyDataSetChanged();
    }


    public void setupSwipeToDelete(RecyclerView recyclerView) {

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    if (onFoodRemovedListener != null) {
                        Meal.FoodPortion removedPortion = foods.get(position);
                        onFoodRemovedListener.onFoodRemoved(position, removedPortion);
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;


                    float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth();
                    itemView.setAlpha(alpha);


                    if (dX < 0) {

                        background.setBounds(
                                itemView.getRight() + (int) dX,
                                itemView.getTop(),
                                itemView.getRight(),
                                itemView.getBottom()
                        );
                        background.draw(c);


                        if (deleteIcon != null) {
                            int itemHeight = itemView.getBottom() - itemView.getTop();

                            int iconSize = (int) (itemHeight * 0.6f);


                            int iconMargin = (itemHeight - iconSize) / 2;


                            int iconLeft = itemView.getRight() - iconMargin - iconSize;
                            int iconRight = itemView.getRight() - iconMargin;
                            int iconTop = itemView.getTop() + (itemHeight - iconSize) / 2;
                            int iconBottom = iconTop + iconSize;


                            float scaleFactor = Math.min(1.0f, Math.abs(dX) / (itemView.getWidth() / 3f));
                            float iconScale = 0.9f + 0.3f * scaleFactor;

                            int iconCenterX = (iconLeft + iconRight) / 2;
                            int iconCenterY = (iconTop + iconBottom) / 2;

                            int scaledIconSize = (int) (iconSize * iconScale);
                            iconLeft = iconCenterX - scaledIconSize / 2;
                            iconRight = iconCenterX + scaledIconSize / 2;
                            iconTop = iconCenterY - scaledIconSize / 2;
                            iconBottom = iconCenterY + scaledIconSize / 2;


                            if (Math.abs(dX) < itemView.getWidth() / 3) {
                                iconLeft = Math.max(itemView.getRight() - iconMargin - scaledIconSize,
                                        itemView.getRight() + (int) dX + iconMargin);
                                iconRight = Math.max(itemView.getRight() - iconMargin,
                                        itemView.getRight() + (int) dX + iconMargin + scaledIconSize);
                            }


                            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            deleteIcon.setAlpha(255);
                            deleteIcon.draw(c);
                        }
                    }


                    float maxSwipe = itemView.getWidth() / 2.5f;
                    if (Math.abs(dX) > maxSwipe) {
                        dX = -maxSwipe;
                    }

                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.4f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 5;
            }

            @Override
            public float getSwipeVelocityThreshold(float defaultValue) {
                return defaultValue * 0.5f;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setTranslationX(0);
            }
        };


        itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView foodName;
        final TextView portionSize;
        final TextView foodNutrients;
        final TextView foodCalories;


        final View categoryIndicator;
        final Chip portionChip;
        final Chip proteinsChip;
        final Chip fatsChip;
        final Chip carbsChip;
        final TextView caloriesPercent;

        ViewHolder(View view) {
            super(view);

            foodName = view.findViewById(R.id.food_name);
            portionSize = view.findViewById(R.id.portion_size);
            foodNutrients = view.findViewById(R.id.food_nutrients);
            foodCalories = view.findViewById(R.id.food_calories);


            categoryIndicator = view.findViewById(R.id.category_indicator);
            portionChip = view.findViewById(R.id.portion_chip);
            proteinsChip = view.findViewById(R.id.proteins_chip);
            fatsChip = view.findViewById(R.id.fats_chip);
            carbsChip = view.findViewById(R.id.carbs_chip);
            caloriesPercent = view.findViewById(R.id.calories_percent);
        }
    }
} 