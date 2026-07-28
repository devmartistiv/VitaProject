package com.martist.vitamove.nutrition.domain.events;


public class MealUpdatedEvent {
    private final String mealType;

    public MealUpdatedEvent(String mealType) {
        this.mealType = mealType;
    }

    public String getMealType() {
        return mealType;
    }
} 