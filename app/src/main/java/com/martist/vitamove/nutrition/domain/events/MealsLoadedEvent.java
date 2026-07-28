package com.martist.vitamove.nutrition.domain.events;

import com.martist.vitamove.nutrition.ui.model.Meal;

import java.util.Map;

public class MealsLoadedEvent {
    private final Map<String, Meal> meals;

    public MealsLoadedEvent(Map<String, Meal> meals) {
        this.meals = meals;
    }

    public Map<String, Meal> getMeals() {
        return meals;
    }
} 