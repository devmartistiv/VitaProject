package com.martist.vitamove.core.domain.utils;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

public class OtherMonthDecorator implements DayViewDecorator {
    private final CalendarDay today;

    public OtherMonthDecorator() {
        today = CalendarDay.today();
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.getMonth() != today.getMonth() ||
                (day.getMonth() == today.getMonth() && day.isAfter(today));
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new ForegroundColorSpan(Color.parseColor("#BEBEBE")));
    }
} 