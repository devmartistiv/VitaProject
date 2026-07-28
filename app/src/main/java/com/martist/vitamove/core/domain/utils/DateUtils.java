package com.martist.vitamove.core.domain.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class DateUtils {

    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());


    private static final SimpleDateFormat RUSSIAN_DAY_MONTH_FORMAT = new SimpleDateFormat("d MMMM", new Locale("ru"));


    public static String formatDate(Date date) {
        if (date == null) return "";
        return DEFAULT_DATE_FORMAT.format(date);
    }


    public static String formatDayMonthRussian(Date date) {
        if (date == null) return "";
        return RUSSIAN_DAY_MONTH_FORMAT.format(date);
    }


    public static Date getDateAfterWeeks(float weeksToAdd) {
        Calendar calendar = Calendar.getInstance();


        int daysToAdd = Math.round(weeksToAdd * 7);


        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);

        return calendar.getTime();
    }


    private static final SimpleDateFormat ISO_8601_FORMAT_WITH_MILLIS =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
    private static final SimpleDateFormat ISO_8601_FORMAT_WITHOUT_MILLIS =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
    private static final SimpleDateFormat ISO_8601_FORMAT_DATE_ONLY =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    static {

        ISO_8601_FORMAT_WITH_MILLIS.setTimeZone(TimeZone.getTimeZone("UTC"));
        ISO_8601_FORMAT_WITHOUT_MILLIS.setTimeZone(TimeZone.getTimeZone("UTC"));
        ISO_8601_FORMAT_DATE_ONLY.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    public static long parseIsoDate(String isoString) throws ParseException {
        if (isoString == null || isoString.isEmpty()) {
            return 0;
        }
        try {

            return ISO_8601_FORMAT_WITH_MILLIS.parse(isoString).getTime();
        } catch (ParseException e1) {
            try {

                return ISO_8601_FORMAT_WITHOUT_MILLIS.parse(isoString).getTime();
            } catch (ParseException e2) {
                try {

                    return ISO_8601_FORMAT_DATE_ONLY.parse(isoString).getTime();
                } catch (ParseException e3) {

                    throw e3;
                }
            }
        }
    }


    public static String formatDateToIso(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(new Date(timestamp));
    }
} 