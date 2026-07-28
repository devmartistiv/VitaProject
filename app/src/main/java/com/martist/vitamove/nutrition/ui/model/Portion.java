package com.martist.vitamove.nutrition.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Portion implements Parcelable {
    private String id;
    private String food_id;
    private String name;
    private int weight;


    public Portion() {
    }


    protected Portion(Parcel in) {
        id = in.readString();
        food_id = in.readString();
        name = in.readString();
        weight = in.readInt();
    }

    public static final Creator<Portion> CREATOR = new Creator<Portion>() {
        @Override
        public Portion createFromParcel(Parcel in) {
            return new Portion(in);
        }

        @Override
        public Portion[] newArray(int size) {
            return new Portion[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(food_id);
        dest.writeString(name);
        dest.writeInt(weight);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFoodId() {
        return food_id;
    }

    public void setFoodId(String food_id) {
        this.food_id = food_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
