package com.martist.vitamove.nutrition.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;


public class Dish implements Parcelable {
    private String id;
    private String name;
    private String description;
    private List<DishIngredient> ingredients;
    private long createdAt;
    private long updatedAt;


    public Dish() {
        this.ingredients = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }


    public Dish(String name, String description) {
        this.name = name;
        this.description = description;
        this.ingredients = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }


    public static class DishIngredient implements Parcelable {
        private Food food;
        private float quantity;
        private String portionName;

        public DishIngredient() {
        }

        public DishIngredient(Food food, float quantity, String portionName) {
            this.food = food;
            this.quantity = quantity;
            this.portionName = portionName;
        }


        public Food getFood() {
            return food;
        }

        public void setFood(Food food) {
            this.food = food;
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


        public int getTotalWeightInGrams() {
            if (portionName == null || food == null) return (int) quantity;

            if (portionName.equals("грамм") || portionName.equals("мл")) {
                return (int) quantity;
            }

            if (food.getPortions() != null) {
                for (Portion p : food.getPortions()) {
                    if (p.getName().equals(portionName)) {
                        return (int) (quantity * p.getWeight());
                    }
                }
            }
            return (int) quantity;
        }


        protected DishIngredient(Parcel in) {
            food = in.readParcelable(Food.class.getClassLoader());
            quantity = in.readFloat();
            portionName = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(food, flags);
            dest.writeFloat(quantity);
            dest.writeString(portionName);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<DishIngredient> CREATOR = new Creator<DishIngredient>() {
            @Override
            public DishIngredient createFromParcel(Parcel in) {
                return new DishIngredient(in);
            }

            @Override
            public DishIngredient[] newArray(int size) {
                return new DishIngredient[size];
            }
        };
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<DishIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<DishIngredient> ingredients) {
        this.ingredients = ingredients;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }


    public void addIngredient(Food food, float quantity, String portionName) {
        ingredients.add(new DishIngredient(food, quantity, portionName));
        this.updatedAt = System.currentTimeMillis();
    }

    public void removeIngredient(int index) {
        if (index >= 0 && index < ingredients.size()) {
            ingredients.remove(index);
            this.updatedAt = System.currentTimeMillis();
        }
    }


    public float getTotalCalories() {
        float totalCalories = 0;
        for (DishIngredient ingredient : ingredients) {
            if (ingredient.getFood() != null) {
                float multiplier = ingredient.getTotalWeightInGrams() / 100f;
                totalCalories += ingredient.getFood().getCalories() * multiplier;
            }
        }
        return totalCalories;
    }

    public float getTotalProteins() {
        float totalProteins = 0;
        for (DishIngredient ingredient : ingredients) {
            if (ingredient.getFood() != null) {
                float multiplier = ingredient.getTotalWeightInGrams() / 100f;
                totalProteins += ingredient.getFood().getProteins() * multiplier;
            }
        }
        return totalProteins;
    }

    public float getTotalFats() {
        float totalFats = 0;
        for (DishIngredient ingredient : ingredients) {
            if (ingredient.getFood() != null) {
                float multiplier = ingredient.getTotalWeightInGrams() / 100f;
                totalFats += ingredient.getFood().getFats() * multiplier;
            }
        }
        return totalFats;
    }

    public float getTotalCarbs() {
        float totalCarbs = 0;
        for (DishIngredient ingredient : ingredients) {
            if (ingredient.getFood() != null) {
                float multiplier = ingredient.getTotalWeightInGrams() / 100f;
                totalCarbs += ingredient.getFood().getCarbs() * multiplier;
            }
        }
        return totalCarbs;
    }


    protected Dish(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        ingredients = in.createTypedArrayList(DishIngredient.CREATOR);
        createdAt = in.readLong();
        updatedAt = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeTypedList(ingredients);
        dest.writeLong(createdAt);
        dest.writeLong(updatedAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Dish> CREATOR = new Creator<Dish>() {
        @Override
        public Dish createFromParcel(Parcel in) {
            return new Dish(in);
        }

        @Override
        public Dish[] newArray(int size) {
            return new Dish[size];
        }
    };
} 