package com.martist.vitamove.nutrition.domain.events;

import com.martist.vitamove.nutrition.ui.model.Food;


public class FoodAddedEvent {
    private final Food food;
    private final float amount;
    private final String mealType;

    public FoodAddedEvent(Food food, float amount, String mealType) {
        this.food = food;
        this.amount = amount;
        this.mealType = mealType;
    }

    public Food getFood() {
        return food;
    }

    public float getAmount() {
        return amount;
    }

    public String getMealType() {
        return mealType;
    }
} 