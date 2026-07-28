package com.martist.vitamove.core.data.local.converters;

import androidx.room.TypeConverter;

import java.util.Date;

public class DateConverter {


    @TypeConverter
    public static Date fromTimestampToDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }


} 