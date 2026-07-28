package com.martist.vitamove.nutrition.domain.events;

import java.util.Map;


public class NutrientsNormsUpdatedEvent {

    private final Map<String, Float> nutrientNorms;


    public NutrientsNormsUpdatedEvent(Map<String, Float> nutrientNorms) {
        this.nutrientNorms = nutrientNorms;
    }


    public Map<String, Float> getNutrientNorms() {
        return nutrientNorms;
    }


    public float getNormForNutrient(String nutrientKey) {
        return nutrientNorms.getOrDefault(nutrientKey, 0f);
    }
} 