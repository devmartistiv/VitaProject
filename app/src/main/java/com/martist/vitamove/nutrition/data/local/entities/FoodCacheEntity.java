package com.martist.vitamove.nutrition.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.martist.vitamove.core.data.local.converters.ListConverter;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Portion;

import java.util.List;


@Entity(
        tableName = "food_cache",
        indices = {
                @Index(value = {"name"}),
                @Index(value = {"category"}),
                @Index(value = {"popularity"}),
                @Index(value = {"updated_at"})
        }
)
public class FoodCacheEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "subcategory")
    private String subcategory;

    @ColumnInfo(name = "calories")
    private int calories;

    @ColumnInfo(name = "proteins")
    private float proteins;

    @ColumnInfo(name = "fats")
    private float fats;

    @ColumnInfo(name = "carbs")
    private float carbs;

    @ColumnInfo(name = "popularity")
    private int popularity;


    @ColumnInfo(name = "calcium")
    private float calcium;

    @ColumnInfo(name = "iron")
    private float iron;

    @ColumnInfo(name = "magnesium")
    private float magnesium;

    @ColumnInfo(name = "phosphorus")
    private float phosphorus;

    @ColumnInfo(name = "potassium")
    private float potassium;

    @ColumnInfo(name = "sodium")
    private float sodium;

    @ColumnInfo(name = "zinc")
    private float zinc;


    @ColumnInfo(name = "vitamin_a")
    private float vitaminA;

    @ColumnInfo(name = "vitamin_b1")
    private float vitaminB1;

    @ColumnInfo(name = "vitamin_b2")
    private float vitaminB2;

    @ColumnInfo(name = "vitamin_b3")
    private float vitaminB3;

    @ColumnInfo(name = "vitamin_b5")
    private float vitaminB5;

    @ColumnInfo(name = "vitamin_b6")
    private float vitaminB6;

    @ColumnInfo(name = "vitamin_b9")
    private float vitaminB9;

    @ColumnInfo(name = "vitamin_b12")
    private float vitaminB12;

    @ColumnInfo(name = "vitamin_c")
    private float vitaminC;

    @ColumnInfo(name = "vitamin_d")
    private float vitaminD;

    @ColumnInfo(name = "vitamin_e")
    private float vitaminE;

    @ColumnInfo(name = "vitamin_k")
    private float vitaminK;


    @ColumnInfo(name = "cholesterol")
    private float cholesterol;

    @ColumnInfo(name = "saturated_fats")
    private float saturatedFats;

    @ColumnInfo(name = "trans_fats")
    private float transFats;

    @ColumnInfo(name = "fiber")
    private float fiber;

    @ColumnInfo(name = "sugar")
    private float sugar;

    @ColumnInfo(name = "usefulness_index")
    private int usefulnessIndex;

    @ColumnInfo(name = "is_liquid")
    private boolean isLiquid;

    @ColumnInfo(name = "is_moderated")
    private boolean isModerated;


    @ColumnInfo(name = "updated_at")
    private String updatedAt;


    @ColumnInfo(name = "portions")
    @TypeConverters(ListConverter.class)
    private List<Portion> portions;


    public FoodCacheEntity() {
    }


    public static FoodCacheEntity fromFood(Food food) {
        FoodCacheEntity entity = new FoodCacheEntity();
        entity.id = food.getId();
        entity.name = food.getName();
        entity.category = food.getCategory();
        entity.subcategory = food.getSubcategory();
        entity.calories = food.getCalories();
        entity.proteins = food.getProteins();
        entity.fats = food.getFats();
        entity.carbs = food.getCarbs();
        entity.popularity = food.getPopularity();
        entity.calcium = food.getCalcium();
        entity.iron = food.getIron();
        entity.magnesium = food.getMagnesium();
        entity.phosphorus = food.getPhosphorus();
        entity.potassium = food.getPotassium();
        entity.sodium = food.getSodium();
        entity.zinc = food.getZinc();
        entity.vitaminA = food.getVitaminA();
        entity.vitaminB1 = food.getVitaminB1();
        entity.vitaminB2 = food.getVitaminB2();
        entity.vitaminB3 = food.getVitaminB3();
        entity.vitaminB5 = food.getVitaminB5();
        entity.vitaminB6 = food.getVitaminB6();
        entity.vitaminB9 = food.getVitaminB9();
        entity.vitaminB12 = food.getVitaminB12();
        entity.vitaminC = food.getVitaminC();
        entity.vitaminD = food.getVitaminD();
        entity.vitaminE = food.getVitaminE();
        entity.vitaminK = food.getVitaminK();
        entity.cholesterol = food.getCholesterol();
        entity.saturatedFats = food.getSaturatedFats();
        entity.transFats = food.getTransFats();
        entity.fiber = food.getFiber();
        entity.sugar = food.getSugar();
        entity.usefulnessIndex = food.getUsefulnessIndex();
        entity.isLiquid = food.isLiquid();
        entity.isModerated = food.isModerated();
        entity.portions = food.getPortions();


        String updatedAt = food.getUpdatedAt();
        if (updatedAt != null && updatedAt.contains(" ")) {
            updatedAt = updatedAt.replace(" ", "+");
        }
        entity.updatedAt = updatedAt;

        return entity;
    }


    public Food toFood() {
        return new Food.Builder()
                .id(id)
                .name(name)
                .category(category)
                .subcategory(subcategory)
                .calories(calories)
                .proteins(proteins)
                .fats(fats)
                .carbs(carbs)
                .popularity(popularity)
                .calcium(calcium)
                .iron(iron)
                .magnesium(magnesium)
                .phosphorus(phosphorus)
                .potassium(potassium)
                .sodium(sodium)
                .zinc(zinc)
                .vitaminA(vitaminA)
                .vitaminB1(vitaminB1)
                .vitaminB2(vitaminB2)
                .vitaminB3(vitaminB3)
                .vitaminB5(vitaminB5)
                .vitaminB6(vitaminB6)
                .vitaminB9(vitaminB9)
                .vitaminB12(vitaminB12)
                .vitaminC(vitaminC)
                .vitaminD(vitaminD)
                .vitaminE(vitaminE)
                .vitaminK(vitaminK)
                .cholesterol(cholesterol)
                .saturatedFats(saturatedFats)
                .transFats(transFats)
                .fiber(fiber)
                .sugar(sugar)
                .usefulness_index(usefulnessIndex)
                .isLiquid(isLiquid)
                .isModerated(isModerated)
                .updatedAt(updatedAt)
                .portions(portions)
                .build();
    }


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public float getProteins() {
        return proteins;
    }

    public void setProteins(float proteins) {
        this.proteins = proteins;
    }

    public float getFats() {
        return fats;
    }

    public void setFats(float fats) {
        this.fats = fats;
    }

    public float getCarbs() {
        return carbs;
    }

    public void setCarbs(float carbs) {
        this.carbs = carbs;
    }

    public int getPopularity() {
        return popularity;
    }

    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }

    public float getCalcium() {
        return calcium;
    }

    public void setCalcium(float calcium) {
        this.calcium = calcium;
    }

    public float getIron() {
        return iron;
    }

    public void setIron(float iron) {
        this.iron = iron;
    }

    public float getMagnesium() {
        return magnesium;
    }

    public void setMagnesium(float magnesium) {
        this.magnesium = magnesium;
    }

    public float getPhosphorus() {
        return phosphorus;
    }

    public void setPhosphorus(float phosphorus) {
        this.phosphorus = phosphorus;
    }

    public float getPotassium() {
        return potassium;
    }

    public void setPotassium(float potassium) {
        this.potassium = potassium;
    }

    public float getSodium() {
        return sodium;
    }

    public void setSodium(float sodium) {
        this.sodium = sodium;
    }

    public float getZinc() {
        return zinc;
    }

    public void setZinc(float zinc) {
        this.zinc = zinc;
    }

    public float getVitaminA() {
        return vitaminA;
    }

    public void setVitaminA(float vitaminA) {
        this.vitaminA = vitaminA;
    }

    public float getVitaminB1() {
        return vitaminB1;
    }

    public void setVitaminB1(float vitaminB1) {
        this.vitaminB1 = vitaminB1;
    }

    public float getVitaminB2() {
        return vitaminB2;
    }

    public void setVitaminB2(float vitaminB2) {
        this.vitaminB2 = vitaminB2;
    }

    public float getVitaminB3() {
        return vitaminB3;
    }

    public void setVitaminB3(float vitaminB3) {
        this.vitaminB3 = vitaminB3;
    }

    public float getVitaminB5() {
        return vitaminB5;
    }

    public void setVitaminB5(float vitaminB5) {
        this.vitaminB5 = vitaminB5;
    }

    public float getVitaminB6() {
        return vitaminB6;
    }

    public void setVitaminB6(float vitaminB6) {
        this.vitaminB6 = vitaminB6;
    }

    public float getVitaminB9() {
        return vitaminB9;
    }

    public void setVitaminB9(float vitaminB9) {
        this.vitaminB9 = vitaminB9;
    }

    public float getVitaminB12() {
        return vitaminB12;
    }

    public void setVitaminB12(float vitaminB12) {
        this.vitaminB12 = vitaminB12;
    }

    public float getVitaminC() {
        return vitaminC;
    }

    public void setVitaminC(float vitaminC) {
        this.vitaminC = vitaminC;
    }

    public float getVitaminD() {
        return vitaminD;
    }

    public void setVitaminD(float vitaminD) {
        this.vitaminD = vitaminD;
    }

    public float getVitaminE() {
        return vitaminE;
    }

    public void setVitaminE(float vitaminE) {
        this.vitaminE = vitaminE;
    }

    public float getVitaminK() {
        return vitaminK;
    }

    public void setVitaminK(float vitaminK) {
        this.vitaminK = vitaminK;
    }

    public float getCholesterol() {
        return cholesterol;
    }

    public void setCholesterol(float cholesterol) {
        this.cholesterol = cholesterol;
    }

    public float getSaturatedFats() {
        return saturatedFats;
    }

    public void setSaturatedFats(float saturatedFats) {
        this.saturatedFats = saturatedFats;
    }

    public float getTransFats() {
        return transFats;
    }

    public void setTransFats(float transFats) {
        this.transFats = transFats;
    }

    public float getFiber() {
        return fiber;
    }

    public void setFiber(float fiber) {
        this.fiber = fiber;
    }

    public float getSugar() {
        return sugar;
    }

    public void setSugar(float sugar) {
        this.sugar = sugar;
    }

    public int getUsefulnessIndex() {
        return usefulnessIndex;
    }

    public void setUsefulnessIndex(int usefulnessIndex) {
        this.usefulnessIndex = usefulnessIndex;
    }

    public boolean isLiquid() {
        return isLiquid;
    }

    public void setLiquid(boolean liquid) {
        isLiquid = liquid;
    }

    public boolean isModerated() {
        return isModerated;
    }

    public void setModerated(boolean moderated) {
        isModerated = moderated;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Portion> getPortions() {
        return portions;
    }

    public void setPortions(List<Portion> portions) {
        this.portions = portions;
    }
}

