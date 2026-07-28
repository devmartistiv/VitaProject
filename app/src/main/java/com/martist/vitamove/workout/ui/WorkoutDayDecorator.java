package com.martist.vitamove.workout.ui;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.martist.vitamove.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.Collection;
import java.util.HashSet;

public class WorkoutDayDecorator implements DayViewDecorator {
    private static final String TAG = "WorkoutDayDecorator";
    private final HashSet<CalendarDay> dates;
    private final int color;

    public WorkoutDayDecorator(Context context, Collection<CalendarDay> dates) {
        this.color = ContextCompat.getColor(context, R.color.orange_500);
        this.dates = new HashSet<>(dates);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {

        view.addSpan(new DotSpan(6, color));
    }

    public void setDates(Collection<CalendarDay> dates) {
        this.dates.clear();
        this.dates.addAll(dates);
        Log.d(TAG, "setDates: обновлено " + this.dates.size() + " дат с тренировками");
        if (!dates.isEmpty() && Log.isLoggable(TAG, Log.DEBUG)) {

            int count = 0;
            for (CalendarDay date : this.dates) {
                if (count++ < 5) {
                    Log.d(TAG, "  Дата с тренировкой: " + date);
                }
            }
        }
    }

    public HashSet<CalendarDay> getDates() {
        return dates;
    }
} 