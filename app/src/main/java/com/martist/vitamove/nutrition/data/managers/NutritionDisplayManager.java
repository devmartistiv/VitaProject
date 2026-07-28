package com.martist.vitamove.nutrition.data.managers;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;

import com.martist.vitamove.R;
import com.martist.vitamove.nutrition.domain.PersonalizedNormsCalculator;
import com.martist.vitamove.nutrition.ui.model.Food;

public class NutritionDisplayManager {
    private TextView fiberValue;
    private TextView sugarValue;
    private TextView cholesterolValue;
    private TextView saturatedFatsValue;
    private TextView transFatsValue;
    private TextView fiberPercent;
    private TextView sugarPercent;
    private TextView cholesterolPercent;
    private TextView saturatedFatsPercent;
    private TextView transFatsPercent;


    private TextView calciumValue;
    private TextView ironValue;
    private TextView magnesiumValue;
    private TextView phosphorusValue;
    private TextView potassiumValue;
    private TextView sodiumValue;
    private TextView zincValue;

    private ProgressBar calciumProgress;
    private ProgressBar ironProgress;
    private ProgressBar magnesiumProgress;
    private ProgressBar phosphorusProgress;
    private ProgressBar potassiumProgress;
    private ProgressBar sodiumProgress;
    private ProgressBar zincProgress;


    private TextView vitaminAValue;
    private TextView vitaminB1Value;
    private TextView vitaminB2Value;
    private TextView vitaminB3Value;
    private TextView vitaminB5Value;
    private TextView vitaminB6Value;
    private TextView vitaminB9Value;
    private TextView vitaminB12Value;
    private TextView vitaminCValue;
    private TextView vitaminDValue;
    private TextView vitaminEValue;
    private TextView vitaminKValue;
    private Activity activity;

    public void initViews(Activity activity) {
        this.activity = activity;
        fiberValue = activity.findViewById(R.id.fiber_value);
        sugarValue = activity.findViewById(R.id.sugar_value);
        cholesterolValue = activity.findViewById(R.id.cholesterol_value);
        saturatedFatsValue = activity.findViewById(R.id.saturated_fats_value);
        transFatsValue = activity.findViewById(R.id.trans_fats_value);

        fiberPercent = activity.findViewById(R.id.fiber_percent);
        sugarPercent = activity.findViewById(R.id.sugar_percent);
        cholesterolPercent = activity.findViewById(R.id.cholesterol_percent);
        saturatedFatsPercent = activity.findViewById(R.id.saturated_fats_percent);
        transFatsPercent = activity.findViewById(R.id.trans_fats_percent);


        calciumValue = activity.findViewById(R.id.calcium_value);
        ironValue = activity.findViewById(R.id.iron_value);
        magnesiumValue = activity.findViewById(R.id.magnesium_value);
        phosphorusValue = activity.findViewById(R.id.phosphorus_value);
        potassiumValue = activity.findViewById(R.id.potassium_value);
        sodiumValue = activity.findViewById(R.id.sodium_value);
        zincValue = activity.findViewById(R.id.zinc_value);

        calciumProgress = activity.findViewById(R.id.calcium_progress);
        ironProgress = activity.findViewById(R.id.iron_progress);
        magnesiumProgress = activity.findViewById(R.id.magnesium_progress);
        phosphorusProgress = activity.findViewById(R.id.phosphorus_progress);
        potassiumProgress = activity.findViewById(R.id.potassium_progress);
        sodiumProgress = activity.findViewById(R.id.sodium_progress);
        zincProgress = activity.findViewById(R.id.zinc_progress);


        vitaminAValue = activity.findViewById(R.id.vitamin_a_value);
        vitaminB1Value = activity.findViewById(R.id.vitamin_b1_value);
        vitaminB2Value = activity.findViewById(R.id.vitamin_b2_value);
        vitaminB3Value = activity.findViewById(R.id.vitamin_b3_value);
        vitaminB5Value = activity.findViewById(R.id.vitamin_b5_value);
        vitaminB6Value = activity.findViewById(R.id.vitamin_b6_value);
        vitaminB9Value = activity.findViewById(R.id.vitamin_b9_value);
        vitaminB12Value = activity.findViewById(R.id.vitamin_b12_value);
        vitaminCValue = activity.findViewById(R.id.vitamin_c_value);
        vitaminDValue = activity.findViewById(R.id.vitamin_d_value);
        vitaminEValue = activity.findViewById(R.id.vitamin_e_value);
        vitaminKValue = activity.findViewById(R.id.vitamin_k_value);
    }

    public void updateAllNutrition(Food food, double multiplier) {
        updateVitamins(food, multiplier);
        updateMinerals(food, multiplier);
        updateAdditionalNutrients(food, multiplier);
    }

    private void updateVitamins(Food selectedFood, double multiplier) {

        View vitaminsContainer = activity.findViewById(R.id.vitamins_container);


        updateVitaminValue(vitaminAValue, selectedFood.getVitaminA(), multiplier, PersonalizedNormsCalculator.getVitaminANorm(), "мкг");
        updateVitaminValue(vitaminB1Value, selectedFood.getVitaminB1(), multiplier, PersonalizedNormsCalculator.getVitaminB1Norm(), "мг");
        updateVitaminValue(vitaminB2Value, selectedFood.getVitaminB2(), multiplier, PersonalizedNormsCalculator.getVitaminB2Norm(), "мг");
        updateVitaminValue(vitaminB3Value, selectedFood.getVitaminB3(), multiplier, PersonalizedNormsCalculator.getVitaminB3Norm(), "мг");
        updateVitaminValue(vitaminB5Value, selectedFood.getVitaminB5(), multiplier, PersonalizedNormsCalculator.getVitaminB5Norm(), "мг");
        updateVitaminValue(vitaminB6Value, selectedFood.getVitaminB6(), multiplier, PersonalizedNormsCalculator.getVitaminB6Norm(), "мг");
        updateVitaminValue(vitaminB9Value, selectedFood.getVitaminB9(), multiplier, PersonalizedNormsCalculator.getVitaminB9Norm(), "мкг");
        updateVitaminValue(vitaminB12Value, selectedFood.getVitaminB12(), multiplier, PersonalizedNormsCalculator.getVitaminB12Norm(), "мкг");
        updateVitaminValue(vitaminCValue, selectedFood.getVitaminC(), multiplier, PersonalizedNormsCalculator.getVitaminCNorm(), "мг");
        updateVitaminValue(vitaminDValue, selectedFood.getVitaminD(), multiplier, PersonalizedNormsCalculator.getVitaminDNorm(), "мкг");
        updateVitaminValue(vitaminEValue, selectedFood.getVitaminE(), multiplier, PersonalizedNormsCalculator.getVitaminENorm(), "мг");
        updateVitaminValue(vitaminKValue, selectedFood.getVitaminK(), multiplier, PersonalizedNormsCalculator.getVitaminKNorm(), "мкг");


        boolean hasVisibleElements =
                (vitaminAValue.getParent() != null && ((View) vitaminAValue.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminB1Value.getParent() != null && ((View) vitaminB1Value.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminB2Value.getParent() != null && ((View) vitaminB2Value.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminB3Value.getParent() != null && ((View) vitaminB3Value.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminB5Value.getParent() != null && ((View) vitaminB5Value.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminB6Value.getParent() != null && ((View) vitaminB6Value.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminB9Value.getParent() != null && ((View) vitaminB9Value.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminB12Value.getParent() != null && ((View) vitaminB12Value.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminCValue.getParent() != null && ((View) vitaminCValue.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminDValue.getParent() != null && ((View) vitaminDValue.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminEValue.getParent() != null && ((View) vitaminEValue.getParent()).getVisibility() == View.VISIBLE) ||
                        (vitaminKValue.getParent() != null && ((View) vitaminKValue.getParent()).getVisibility() == View.VISIBLE);


        vitaminsContainer.setVisibility(hasVisibleElements ? View.VISIBLE : View.GONE);
    }

    private void updateVitaminValue(TextView valueView, float value, double multiplier, float dailyNorm, String unit) {
        TableRow parentRow = (TableRow) valueView.getParent();

        if (!Float.isNaN(value) && value > 0) {
            float calculatedValue = value * (float) multiplier;
            valueView.setText(String.format("%.1f %s (%.0f%%)",
                    calculatedValue, unit, calculatedValue / dailyNorm * 100));


            if (parentRow != null) {
                parentRow.setVisibility(View.VISIBLE);
            }
        } else {

            if (parentRow != null) {
                parentRow.setVisibility(View.GONE);
            }
        }
    }

    private void updateMinerals(Food selectedFood, double multiplier) {

        View mineralsCardView = activity.findViewById(R.id.minerals_container);


        LinearLayout mineralsContent = activity.findViewById(R.id.minerals_content);


        updateMineralWithProgress(
                calciumValue, calciumProgress,
                selectedFood.getCalcium(), multiplier,
                PersonalizedNormsCalculator.getCalciumNorm());

        updateMineralWithProgress(
                ironValue, ironProgress,
                selectedFood.getIron(), multiplier,
                PersonalizedNormsCalculator.getIronNorm());

        updateMineralWithProgress(
                magnesiumValue, magnesiumProgress,
                selectedFood.getMagnesium(), multiplier,
                PersonalizedNormsCalculator.getMagnesiumNorm());

        updateMineralWithProgress(
                phosphorusValue, phosphorusProgress,
                selectedFood.getPhosphorus(), multiplier,
                PersonalizedNormsCalculator.getPhosphorusNorm());

        updateMineralWithProgress(
                potassiumValue, potassiumProgress,
                selectedFood.getPotassium(), multiplier,
                PersonalizedNormsCalculator.getPotassiumNorm());

        updateMineralWithProgress(
                sodiumValue, sodiumProgress,
                selectedFood.getSodium(), multiplier,
                PersonalizedNormsCalculator.getSodiumNorm());

        updateMineralWithProgress(
                zincValue, zincProgress,
                selectedFood.getZinc(), multiplier,
                PersonalizedNormsCalculator.getZincNorm());


        boolean hasVisibleElements = false;
        if (mineralsContent != null) {
            for (int i = 0; i < mineralsContent.getChildCount(); i++) {
                View child = mineralsContent.getChildAt(i);
                if (child != null && child.getVisibility() == View.VISIBLE) {
                    hasVisibleElements = true;
                    break;
                }
            }
        }


        if (mineralsCardView != null) {
            mineralsCardView.setVisibility(hasVisibleElements ? View.VISIBLE : View.GONE);
        }
    }

    private void updateMineralWithProgress(TextView valueView, ProgressBar progressBar,
                                           float value, double multiplier,
                                           float dailyNorm) {
        LinearLayout parentRow = (LinearLayout) valueView.getParent().getParent();

        if (!Float.isNaN(value) && value > 0) {
            float calculatedValue = value * (float) multiplier;
            int percent = Math.min(100, Math.round(calculatedValue / dailyNorm * 100));

            valueView.setText(String.format("%.1f мг (%.0f%%)", calculatedValue, calculatedValue / dailyNorm * 100));
            progressBar.setProgress(percent);


            valueView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            if (parentRow != null) {
                parentRow.setVisibility(View.VISIBLE);
            }
        } else {

            if (parentRow != null) {
                parentRow.setVisibility(View.GONE);
            }
        }
    }

    private void updateAdditionalNutrients(Food selectedFood, double multiplier) {

        View additionalNutrientsContainer = activity.findViewById(R.id.additional_nutrients_container);


        updateNutrientWithPercent(
                fiberValue, fiberPercent,
                selectedFood.getFiber(), multiplier,
                PersonalizedNormsCalculator.getFiberNorm(), "г");

        updateNutrientWithPercent(
                sugarValue, sugarPercent,
                selectedFood.getSugar(), multiplier,
                PersonalizedNormsCalculator.getSugarNorm(), "г");

        updateNutrientWithPercent(
                cholesterolValue, cholesterolPercent,
                selectedFood.getCholesterol(), multiplier,
                PersonalizedNormsCalculator.getCholesterolNorm(), "мг");

        updateNutrientWithPercent(
                saturatedFatsValue, saturatedFatsPercent,
                selectedFood.getSaturatedFats(), multiplier,
                PersonalizedNormsCalculator.getSaturatedFatsNorm(), "г");

        updateNutrientWithPercent(
                transFatsValue, transFatsPercent,
                selectedFood.getTransFats(), multiplier,
                PersonalizedNormsCalculator.getTransFatsNorm(), "г");


        boolean hasVisibleElements =
                (fiberValue.getParent() != null && ((View) fiberValue.getParent()).getVisibility() == View.VISIBLE) ||
                        (sugarValue.getParent() != null && ((View) sugarValue.getParent()).getVisibility() == View.VISIBLE) ||
                        (cholesterolValue.getParent() != null && ((View) cholesterolValue.getParent()).getVisibility() == View.VISIBLE) ||
                        (saturatedFatsValue.getParent() != null && ((View) saturatedFatsValue.getParent()).getVisibility() == View.VISIBLE) ||
                        (transFatsValue.getParent() != null && ((View) transFatsValue.getParent()).getVisibility() == View.VISIBLE);


        additionalNutrientsContainer.setVisibility(hasVisibleElements ? View.VISIBLE : View.GONE);
    }

    private void updateNutrientWithPercent(TextView valueView, TextView percentView,
                                           float value, double multiplier,
                                           float dailyNorm, String unit) {
        TableRow parentRow = (TableRow) valueView.getParent();

        if (!Float.isNaN(value) && value > 0) {
            float calculatedValue = value * (float) multiplier;
            valueView.setText(String.format("%.1f %s", calculatedValue, unit));

            int percentValue = Math.round(calculatedValue / dailyNorm * 100);
            percentView.setText(String.format("%d%%", percentValue));


            if (parentRow != null) {
                parentRow.setVisibility(View.VISIBLE);
            }
        } else {

            if (parentRow != null) {
                parentRow.setVisibility(View.GONE);
            }
        }
    }

}
