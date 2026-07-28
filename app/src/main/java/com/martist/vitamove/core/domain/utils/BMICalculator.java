package com.martist.vitamove.core.domain.utils;


public class BMICalculator {


    public static final float UNDERWEIGHT_THRESHOLD = 18.5f;


    public static final float NORMAL_THRESHOLD = 25.0f;


    public static final float OVERWEIGHT_THRESHOLD = 30.0f;


    public static final float OBESE_THRESHOLD = 35.0f;


    public static float calculateBMI(float weightKg, float heightCm) {

        float heightM = heightCm / 100.0f;

        return weightKg / (heightM * heightM);
    }


    public static BMICategory getBMICategory(float bmi) {
        if (bmi < UNDERWEIGHT_THRESHOLD) {
            return BMICategory.UNDERWEIGHT;
        } else if (bmi < NORMAL_THRESHOLD) {
            return BMICategory.NORMAL;
        } else if (bmi < OVERWEIGHT_THRESHOLD) {
            return BMICategory.OVERWEIGHT;
        } else if (bmi < OBESE_THRESHOLD) {
            return BMICategory.OBESE;
        } else {
            return BMICategory.SEVERELY_OBESE;
        }
    }


    public enum BMICategory {
        UNDERWEIGHT("Недостаточный вес"),
        NORMAL("Нормальный вес"),
        OVERWEIGHT("Избыточный вес"),
        OBESE("Ожирение"),
        SEVERELY_OBESE("Сильное ожирение");

        private final String description;

        BMICategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
} 