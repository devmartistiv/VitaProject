package com.martist.vitamove.core.domain.utils;


public class TextUtils {


    public static String getDaysPerWeekText(int days) {
        if (days == 1) {
            return "день в неделю";
        } else if (days >= 2 && days <= 4) {
            return "дня в неделю";
        } else {
            return "дней в неделю";
        }
    }


    public static String getWeeksText(int weeks) {
        if (weeks == 1) {
            return "неделя";
        } else if (weeks >= 2 && weeks <= 4) {
            return "недели";
        } else {
            return "недель";
        }
    }


    public static String getWorkoutsText(int workouts) {
        if (workouts == 1) {
            return "тренировка";
        } else if (workouts >= 2 && workouts <= 4) {
            return "тренировки";
        } else {
            return "тренировок";
        }
    }


    public static String formatProgramDuration(int weeks, int daysPerWeek) {
        return String.format("%d %s, %d %s",
                weeks, getWeeksText(weeks),
                daysPerWeek, getDaysPerWeekText(daysPerWeek));
    }


    public static String formatWorkoutFrequency(int daysPerWeek) {
        return String.format("%d %s", daysPerWeek, getDaysPerWeekText(daysPerWeek));
    }


    public static String formatWorkoutsPerWeek(int workouts) {
        return String.format("%d %s в неделю", workouts, getWorkoutsText(workouts));
    }
}
