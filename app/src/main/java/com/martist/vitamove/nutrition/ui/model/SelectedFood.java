package com.martist.vitamove.nutrition.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

public class SelectedFood implements Parcelable {
    private final Food food;
    private final double amount;

    public SelectedFood(Food food, double amount) {
        this.food = food;
        this.amount = amount;
    }

    public Food getFood() {
        return food;
    }

    public double getAmount() {
        return amount;
    }

    public double getCalories() {
        return food.getCalories() * amount / 100.0;
    }

    public double getProteins() {
        return food.getProteins() * amount / 100.0;
    }

    public double getFats() {
        return food.getFats() * amount / 100.0;
    }

    public double getCarbs() {
        return food.getCarbs() * amount / 100.0;
    }

    public float getVitaminA() {
        return food.getVitaminA() * (float) amount / 100f;
    }

    public float getVitaminC() {
        return food.getVitaminC() * (float) amount / 100f;
    }

    public float getVitaminD() {
        return food.getVitaminD() * (float) amount / 100f;
    }

    public float getCalcium() {
        return food.getCalcium() * (float) amount / 100f;
    }

    public float getIron() {
        return food.getIron() * (float) amount / 100f;
    }

    protected SelectedFood(Parcel in) {
        food = in.readParcelable(Food.class.getClassLoader());
        amount = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(food, flags);
        dest.writeDouble(amount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SelectedFood> CREATOR = new Creator<SelectedFood>() {
        @Override
        public SelectedFood createFromParcel(Parcel in) {
            return new SelectedFood(in);
        }

        @Override
        public SelectedFood[] newArray(int size) {
            return new SelectedFood[size];
        }
    };
} 