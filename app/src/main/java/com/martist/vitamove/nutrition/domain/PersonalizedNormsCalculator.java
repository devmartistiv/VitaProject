package com.martist.vitamove.nutrition.domain;

import android.content.Context;
import android.content.SharedPreferences;

import com.martist.vitamove.VitaMoveApplication;


public class PersonalizedNormsCalculator {


    private static class UserData {
        final String gender;
        final int age;

        UserData(String gender, int age) {
            this.gender = gender;
            this.age = age;
        }
    }


    private static UserData getUserData() {
        Context context = VitaMoveApplication.getContext();
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        return new UserData(
                prefs.getString("gender", "Мужчина"),
                prefs.getInt("age", 30)
        );
    }

    UserData userData = getUserData();

    public static float getVitaminANorm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 600.0f;
        if (userData.gender.equals("Мужчина")) return 900.0f;
        return 700.0f;
    }

    public static float getVitaminB1Norm() {
        UserData userData = getUserData();
        if (userData.age < 14) return 1.0f;
        if (userData.gender.equals("Мужчина")) return 1.2f;
        return 1.1f;
    }

    public static float getVitaminB2Norm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 1.1f;
        if (userData.gender.equals("Мужчина")) return 1.3f;
        return 1.1f;
    }

    public static float getVitaminB3Norm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 12.0f;
        if (userData.gender.equals("Мужчина")) return 16.0f;
        return 14.0f;
    }

    public static float getVitaminB5Norm() {
        UserData userData = getUserData();
        if (userData.age < 14) return 4.0f;
        return 5.0f;
    }

    public static float getVitaminB6Norm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 1.2f;
        if (userData.age >= 51) {
            return userData.gender.equals("Мужчина") ? 1.7f : 1.5f;
        }
        return userData.gender.equals("Мужчина") ? 1.3f : 1.2f;
    }

    public static float getVitaminB9Norm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 300.0f;
        return 400.0f;
    }

    public static float getVitaminB12Norm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 1.8f;
        return 2.4f;
    }

    public static float getVitaminCNorm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 45.0f;
        if (userData.gender.equals("Мужчина")) return 90.0f;
        return 75.0f;
    }

    public static float getVitaminDNorm() {
        UserData userData = getUserData();
        if (userData.age >= 71) return 20.0f;
        return 15.0f;
    }

    public static float getVitaminENorm() {
        UserData userData = getUserData();

        if (userData.age < 14) return 11.0f;
        return 15.0f;
    }

    public static float getVitaminKNorm() {
        UserData userData = getUserData();
        if (userData.age < 14) return 60.0f;
        if (userData.gender.equals("Мужчина")) return 120.0f;
        return 90.0f;
    }


    public static float getIronNorm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 8.0f;
        if (userData.gender.equals("Мужчина")) return 8.0f;


        if (userData.age <= 50) return 18.0f;
        return 8.0f;
    }

    public static float getCalciumNorm() {
        UserData userData = getUserData();


        if (userData.age >= 9 && userData.age <= 18) return 1300.0f;
        if (userData.age >= 51) {
            return userData.gender.equals("Женщина") ? 1200.0f : 1000.0f;
        }
        return 1000.0f;
    }

    public static float getMagnesiumNorm() {
        UserData userData = getUserData();


        if (userData.age < 14) return 240.0f;
        if (userData.age <= 18) {
            return userData.gender.equals("Мужчина") ? 410.0f : 360.0f;
        }
        if (userData.age <= 30) {
            return userData.gender.equals("Мужчина") ? 400.0f : 310.0f;
        }
        return userData.gender.equals("Мужчина") ? 420.0f : 320.0f;
    }

    public static float getPhosphorusNorm() {
        UserData userData = getUserData();
        if (userData.age >= 9 && userData.age <= 18) return 1250.0f;
        return 700.0f;
    }

    public static float getPotassiumNorm() {

        UserData userData = getUserData();
        if (userData.age < 14) return 2300.0f;
        return 2600.0f;
    }

    public static float getSodiumNorm() {
        UserData userData = getUserData();
        if (userData.age < 14) return 1500.0f;
        return 2300.0f;
    }

    public static float getZincNorm() {


        UserData userData = getUserData();
        if (userData.age < 14) return 8.0f;
        if (userData.gender.equals("Мужчина")) return 11.0f;
        return 8.0f;
    }


    public static float getFiberNorm() {
        UserData userData = getUserData();
        if (userData.age <= 50) {
            return userData.gender.equals("Мужчина") ? 38.0f : 25.0f;
        }
        return userData.gender.equals("Мужчина") ? 30.0f : 21.0f;
    }

    public static float getSugarNorm() {
        return 50.0f;
    }

    public static float getCholesterolNorm() {
        return 300.0f;
    }

    public static float getSaturatedFatsNorm() {
        return 20.0f;
    }

    public static float getTransFatsNorm() {
        return 2.0f;
    }

}