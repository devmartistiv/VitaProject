package com.martist.vitamove.nutrition.ui.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Meal implements Parcelable {
    private final String title;
    private final int iconResId;
    private final List<FoodPortion> foods;
    private float calories;
    private float proteins;
    private float fats;
    private float carbs;
    private static final String TAG = "Meal";

    public Meal(String title, int iconResId) {
        this.title = title;
        this.iconResId = iconResId;
        this.foods = new ArrayList<>();
        this.calories = 0;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public List<FoodPortion> getFoods() {
        return foods;
    }


    public Food getFood(int index) {
        if (foods != null && index >= 0 && index < foods.size()) {
            return foods.get(index).getFood();
        }
        return null;
    }

    public float getCalories() {
        float totalCalories = 0;
        if (foods != null) {
            for (FoodPortion foodPortion : foods) {
                Food food = foodPortion.getFood();
                float portionMultiplier = foodPortion.getTotalWeightInGrams() / 100f;
                totalCalories += food.getCalories() * portionMultiplier;
            }
        }
        return totalCalories;
    }

    public float getProteins() {
        float totalProteins = 0;
        if (foods != null) {
            for (FoodPortion foodPortion : foods) {
                Food food = foodPortion.getFood();
                float portionMultiplier = foodPortion.getTotalWeightInGrams() / 100f;
                totalProteins += food.getProteins() * portionMultiplier;
            }
        }
        return totalProteins;
    }

    public float getFats() {
        float totalFats = 0;
        if (foods != null) {
            for (FoodPortion foodPortion : foods) {
                Food food = foodPortion.getFood();
                float portionMultiplier = foodPortion.getTotalWeightInGrams() / 100f;
                totalFats += food.getFats() * portionMultiplier;
            }
        }
        return totalFats;
    }

    public float getCarbs() {
        float totalCarbs = 0;
        if (foods != null) {
            for (FoodPortion foodPortion : foods) {
                Food food = foodPortion.getFood();
                float portionMultiplier = foodPortion.getTotalWeightInGrams() / 100f;
                totalCarbs += food.getCarbs() * portionMultiplier;
            }
        }
        return totalCarbs;
    }

    public float getTotalProteins() {
        float total = 0;
        for (FoodPortion portion : foods) {
            Food food = portion.getFood();
            float multiplier = portion.getTotalWeightInGrams() / 100f;
            total += food.getProteins() * multiplier;
        }
        return total;
    }

    public float getTotalFats() {
        float total = 0;
        for (FoodPortion portion : foods) {
            Food food = portion.getFood();
            float multiplier = portion.getTotalWeightInGrams() / 100f;
            total += food.getFats() * multiplier;
        }
        return total;
    }

    public float getTotalCarbs() {
        float total = 0;
        for (FoodPortion portion : foods) {
            Food food = portion.getFood();
            float multiplier = portion.getTotalWeightInGrams() / 100f;
            total += food.getCarbs() * multiplier;
        }
        return total;
    }

    public void addFood(Food food, float quantity, String portionName) {
        foods.add(new FoodPortion(food, quantity, portionName));
        updateNutrients();
    }

    public void addFood(SelectedFood selectedFood) {

        Food food = selectedFood.getFood();
        float quantity = (float) selectedFood.getAmount();
        addFood(food, quantity, food.isLiquid() ? "мл" : "грамм");
    }

    private void updateNutrients() {
        calories = 0;
        proteins = 0;
        fats = 0;
        carbs = 0;

        for (FoodPortion food : foods) {
            float multiplier = food.getTotalWeightInGrams() / 100f;
            calories += food.getFood().getCalories() * multiplier;
            proteins += food.getFood().getProteins() * multiplier;
            fats += food.getFood().getFats() * multiplier;
            carbs += food.getFood().getCarbs() * multiplier;
        }
    }


    public boolean removeFood(int position) {
        Log.d(TAG, "Удаление продукта с позиции: " + position);
        if (foods != null && position >= 0 && position < foods.size()) {
            foods.remove(position);
            updateNutrients();
            Log.d(TAG, "Продукт успешно удален, осталось продуктов: " + foods.size());
            return true;
        }
        Log.e(TAG, "Ошибка при удалении продукта: некорректная позиция " + position);
        return false;
    }


    public boolean updateFoodPortion(String foodId, float newQuantity, String newPortionName) {
        Log.d(TAG, "Обновление размера порции для продукта ID: " + foodId + " на " + newQuantity + " " + newPortionName);
        if (foods != null) {
            for (int i = 0; i < foods.size(); i++) {
                FoodPortion portion = foods.get(i);
                if (portion.getFood().getId().equals(foodId)) {

                    FoodPortion newPortion = new FoodPortion(portion.getFood(), newQuantity, newPortionName);

                    foods.set(i, newPortion);

                    updateNutrients();
                    Log.d(TAG, "Размер порции успешно обновлен");
                    return true;
                }
            }
        }
        Log.d(TAG, "Продукт с ID " + foodId + " не найден в приеме пищи");
        return false;
    }


    public boolean addOrUpdateFood(Food food, float quantity, String portionName) {
        if (food == null) {
            Log.e(TAG, "Невозможно добавить null продукт");
            return false;
        }


        if (foods != null) {
            for (int i = 0; i < foods.size(); i++) {
                FoodPortion portion = foods.get(i);
                if (portion.getFood().getId() == food.getId()) {

                    FoodPortion newPortion = new FoodPortion(food, quantity, portionName);
                    foods.set(i, newPortion);
                    updateNutrients();
                    Log.d(TAG, "Обновлен продукт: " + food.getName() + ", новая порция: " + quantity + " " + portionName);
                    return true;
                }
            }
        }


        foods.add(new FoodPortion(food, quantity, portionName));
        updateNutrients();
        Log.d(TAG, "Добавлен новый продукт: " + food.getName() + ", порция: " + quantity + " " + portionName);
        return false;
    }


    protected Meal(Parcel in) {
        title = in.readString();
        iconResId = in.readInt();
        foods = new ArrayList<>();
        in.readTypedList(foods, FoodPortion.CREATOR);
        calories = in.readFloat();
        proteins = in.readFloat();
        fats = in.readFloat();
        carbs = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeInt(iconResId);
        dest.writeTypedList(foods);
        dest.writeFloat(calories);
        dest.writeFloat(proteins);
        dest.writeFloat(fats);
        dest.writeFloat(carbs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Meal> CREATOR = new Creator<Meal>() {
        @Override
        public Meal createFromParcel(Parcel in) {
            return new Meal(in);
        }

        @Override
        public Meal[] newArray(int size) {
            return new Meal[size];
        }
    };

    public static class FoodPortion implements Parcelable {
        private final Food food;
        private final float quantity;
        private final String portionName;

        public FoodPortion(Food food, float quantity, String portionName) {
            this.food = food;
            this.quantity = quantity;
            this.portionName = portionName;
        }

        public Food getFood() {
            return food;
        }

        public float getQuantity() {
            return quantity;
        }

        public String getPortionName() {
            return portionName;
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


        protected FoodPortion(Parcel in) {
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

        public static final Creator<FoodPortion> CREATOR = new Creator<FoodPortion>() {
            @Override
            public FoodPortion createFromParcel(Parcel in) {
                return new FoodPortion(in);
            }

            @Override
            public FoodPortion[] newArray(int size) {
                return new FoodPortion[size];
            }
        };
    }


} 