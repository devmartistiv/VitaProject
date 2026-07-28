package com.martist.vitamove.nutrition.data.local.dao;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.martist.vitamove.nutrition.data.local.entities.DishEntity;
import com.martist.vitamove.nutrition.data.local.entities.DishIngredientEntity;

import java.util.List;

public class DishWithIngredients {
    @Embedded
    public DishEntity dish;

    @Relation(
            parentColumn = "id",
            entityColumn = "dishId"
    )
    public List<DishIngredientEntity> ingredients;


    public DishWithIngredients() {
    }


    public DishWithIngredients(DishEntity dish, List<DishIngredientEntity> ingredients) {
        this.dish = dish;
        this.ingredients = ingredients;
    }


    public DishEntity getDish() {
        return dish;
    }

    public List<DishIngredientEntity> getIngredients() {
        return ingredients;
    }


    public int getTotalCalories() {


        return 0;
    }

    public float getTotalProteins() {
        return 0f;
    }

    public float getTotalFats() {
        return 0f;
    }

    public float getTotalCarbs() {
        return 0f;
    }
} 