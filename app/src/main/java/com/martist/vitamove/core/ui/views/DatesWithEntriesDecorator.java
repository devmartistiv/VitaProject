package com.martist.vitamove.core.ui.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Collection;
import java.util.HashSet;


public class DatesWithEntriesDecorator implements DayViewDecorator {
    private final HashSet<CalendarDay> dates;
    private final int color;

    public DatesWithEntriesDecorator(Collection<CalendarDay> dates, int color) {
        this.dates = new HashSet<>(dates);
        this.color = color;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpan(8, color));
    }


    private static class DotSpan implements LineBackgroundSpan {
        private final float radius;
        private final int color;

        public DotSpan(float radius, int color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void drawBackground(
                Canvas canvas, Paint paint,
                int left, int right, int top, int baseline, int bottom,
                CharSequence charSequence,
                int start, int end, int lineNum
        ) {
            int oldColor = paint.getColor();
            paint.setColor(color);


            float centerX = (left + right) / 2f;
            float centerY = bottom + radius * 2;

            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setColor(oldColor);
        }
    }
}
