package com.martist.vitamove.nutrition.data.dto;

import com.google.gson.annotations.SerializedName;
import com.martist.vitamove.nutrition.ui.model.Portion;

import java.util.List;


public class FoodDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("category")
    private String category;

    @SerializedName("subcategory")
    private String subcategory;

    @SerializedName("calories")
    private int calories;

    @SerializedName("proteins")
    private float proteins;

    @SerializedName("fats")
    private float fats;

    @SerializedName("carbs")
    private float carbs;

    @SerializedName("popularity")
    private int popularity;

    @SerializedName("calcium")
    private float calcium;

    @SerializedName("iron")
    private float iron;

    @SerializedName("magnesium")
    private float magnesium;

    @SerializedName("phosphorus")
    private float phosphorus;

    @SerializedName("potassium")
    private float potassium;

    @SerializedName("sodium")
    private float sodium;

    @SerializedName("zinc")
    private float zinc;

    @SerializedName("vitamin_a")
    private float vitaminA;

    @SerializedName("vitamin_b1")
    private float vitaminB1;

    @SerializedName("vitamin_b2")
    private float vitaminB2;

    @SerializedName("vitamin_b3")
    private float vitaminB3;

    @SerializedName("vitamin_b5")
    private float vitaminB5;

    @SerializedName("vitamin_b6")
    private float vitaminB6;

    @SerializedName("vitamin_b9")
    private float vitaminB9;

    @SerializedName("vitamin_b12")
    private float vitaminB12;

    @SerializedName("vitamin_c")
    private float vitaminC;

    @SerializedName("vitamin_d")
    private float vitaminD;

    @SerializedName("vitamin_e")
    private float vitaminE;

    @SerializedName("vitamin_k")
    private float vitaminK;

    @SerializedName("cholesterol")
    private float cholesterol;

    @SerializedName("saturated_fats")
    private float saturatedFats;

    @SerializedName("trans_fats")
    private float transFats;

    @SerializedName("fiber")
    private float fiber;

    @SerializedName("sugar")
    private float sugar;

    @SerializedName("usefulness_index")
    private int usefulnessIndex = 5;

    @SerializedName("is_liquid")
    private boolean isLiquid;

    @SerializedName("is_moderated")
    private boolean isModerated = true;

    @SerializedName("portions")
    private List<Portion> portions;

    @SerializedName("updated_at")
    private String updatedAt;


    public FoodDto() {
    }


    public String getId() {
        return id != null ? id : "";
    }

    public String getName() {
        return name != null ? name : "Неизвестный продукт";
    }

    public String getCategory() {
        return category != null ? category : "Другое";
    }

    public String getSubcategory() {
        return subcategory != null ? subcategory : "";
    }

    public int getCalories() {
        return calories;
    }

    public float getProteins() {
        return proteins;
    }

    public float getFats() {
        return fats;
    }

    public float getCarbs() {
        return carbs;
    }

    public int getPopularity() {
        return popularity;
    }

    public float getCalcium() {
        return calcium;
    }

    public float getIron() {
        return iron;
    }

    public float getMagnesium() {
        return magnesium;
    }

    public float getPhosphorus() {
        return phosphorus;
    }

    public float getPotassium() {
        return potassium;
    }

    public float getSodium() {
        return sodium;
    }

    public float getZinc() {
        return zinc;
    }

    public float getVitaminA() {
        return vitaminA;
    }

    public float getVitaminB1() {
        return vitaminB1;
    }

    public float getVitaminB2() {
        return vitaminB2;
    }

    public float getVitaminB3() {
        return vitaminB3;
    }

    public float getVitaminB5() {
        return vitaminB5;
    }

    public float getVitaminB6() {
        return vitaminB6;
    }

    public float getVitaminB9() {
        return vitaminB9;
    }

    public float getVitaminB12() {
        return vitaminB12;
    }

    public float getVitaminC() {
        return vitaminC;
    }

    public float getVitaminD() {
        return vitaminD;
    }

    public float getVitaminE() {
        return vitaminE;
    }

    public float getVitaminK() {
        return vitaminK;
    }

    public float getCholesterol() {
        return cholesterol;
    }

    public float getSaturatedFats() {
        return saturatedFats;
    }

    public float getTransFats() {
        return transFats;
    }

    public float getFiber() {
        return fiber;
    }

    public float getSugar() {
        return sugar;
    }

    public int getUsefulnessIndex() {
        return usefulnessIndex;
    }

    public boolean isLiquid() {
        return isLiquid;
    }

    public boolean isModerated() {
        return isModerated;
    }

    public List<Portion> getPortions() {
        return portions;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
