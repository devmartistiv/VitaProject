package com.martist.vitamove.nutrition.domain.events;

import com.martist.vitamove.nutrition.ui.model.NutrientType;

import java.util.List;


public class TrackedNutrientsChangedEvent {
    private final List<NutrientType> trackedNutrients;

    public TrackedNutrientsChangedEvent(List<NutrientType> trackedNutrients) {
        this.trackedNutrients = trackedNutrients;
    }

    public List<NutrientType> getTrackedNutrients() {
        return trackedNutrients;
    }
} 