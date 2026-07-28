package com.martist.vitamove.nutrition.data.local.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "dish_ingredients",
        foreignKeys = @ForeignKey(entity = DishEntity.class,
                parentColumns = "id",
                childColumns = "dishId",
                onDelete = ForeignKey.CASCADE),
        indices = {@androidx.room.Index(value = "dishId")})
public class DishIngredientEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String dishId;
    private String foodId;
    private String foodName;
    private float quantity;
    private String portionName;
    private long createdAt;


    public DishIngredientEntity() {
        this.createdAt = System.currentTimeMillis();
    }


    @androidx.room.Ignore
    public DishIngredientEntity(String dishId, String foodId, String foodName,
                                float quantity, String portionName) {
        this.dishId = dishId;
        this.foodId = foodId;
        this.foodName = foodName;
        this.quantity = quantity;
        this.portionName = portionName;
        this.createdAt = System.currentTimeMillis();
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDishId() {
        return dishId;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public float getQuantity() {
        return quantity;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public String getPortionName() {
        return portionName;
    }

    public void setPortionName(String portionName) {
        this.portionName = portionName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 