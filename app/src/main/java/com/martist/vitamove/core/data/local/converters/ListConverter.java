package com.martist.vitamove.core.data.local.converters;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.nutrition.ui.model.Portion;

import java.lang.reflect.Type;
import java.util.List;


public class ListConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<String> fromString(String value) {
        if (value == null) {
            return null;
        }

        Type listType = new TypeToken<List<String>>() {
        }.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null) {
            return null;
        }

        return gson.toJson(list);
    }

    @TypeConverter
    public static String fromList2(List<Portion> list) {
        if (list == null) {
            return null;
        }

        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Portion> fromString2(String value) {
        if (value == null) {
            return null;
        }

        Type listType = new TypeToken<List<Portion>>() {
        }.getType();
        return gson.fromJson(value, listType);
    }
} 