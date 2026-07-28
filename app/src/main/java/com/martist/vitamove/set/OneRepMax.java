package com.martist.vitamove.set;


public class OneRepMax {
    private Float value;
    private Float lastWeight;
    private Integer lastReps;
    private boolean hasData;
    private String formula;

    public OneRepMax() {
        this.hasData = false;
    }

    public OneRepMax(Float lastWeight, Integer lastReps) {
        this.lastWeight = lastWeight;
        this.lastReps = lastReps;
        this.hasData = lastWeight != null && lastReps != null && lastWeight > 0 && lastReps > 0;

        if (hasData) {
            this.value = calculateOneRepMax(lastWeight, lastReps);
            this.formula = "Бржицки";
        }
    }


    private Float calculateOneRepMax(Float weight, Integer reps) {
        if (weight == null || reps == null || weight <= 0 || reps <= 0) {
            return null;
        }


        if (reps == 1) {
            return weight;
        }

        if (reps > 15) {

            return weight * (1 + (reps / 30.0f));
        }


        return weight * (36f / (37f - reps));
    }


    public String getBasedOnDescription() {
        if (!hasData) {
            return "Недостаточно данных";
        }

        return String.format("На основе %.1f кг × %d повт.", lastWeight, lastReps);
    }


    public String getFormattedValue() {
        if (!hasData || value == null) {
            return "--";
        }

        return String.format("%.1f", value);
    }


    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public Float getLastWeight() {
        return lastWeight;
    }

    public void setLastWeight(Float lastWeight) {
        this.lastWeight = lastWeight;
    }

    public Integer getLastReps() {
        return lastReps;
    }

    public void setLastReps(Integer lastReps) {
        this.lastReps = lastReps;
    }

    public boolean isHasData() {
        return hasData;
    }

    public void setHasData(boolean hasData) {
        this.hasData = hasData;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }
}
