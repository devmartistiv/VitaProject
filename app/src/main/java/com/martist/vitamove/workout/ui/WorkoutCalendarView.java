package com.martist.vitamove.workout.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.CalendarView;

import androidx.core.content.ContextCompat;

import com.martist.vitamove.R;

import java.util.HashMap;
import java.util.Map;

public class WorkoutCalendarView extends CalendarView {
    private Map<Long, Integer> dateMarkers;
    private Paint markerPaint;
    public static final int MARKER_COMPLETED = 1;
    public static final int MARKER_MISSED = 2;

    public WorkoutCalendarView(Context context) {
        super(context);
        init();
    }

    public WorkoutCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WorkoutCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dateMarkers = new HashMap<>();
        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setStyle(Paint.Style.FILL);
    }

    public void addMarker(long date, int markerType) {
        dateMarkers.put(date, markerType);
        invalidate();
    }

    public void clearMarkers() {
        dateMarkers.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        for (Map.Entry<Long, Integer> entry : dateMarkers.entrySet()) {
            long date = entry.getKey();
            int markerType = entry.getValue();


            int color;
            switch (markerType) {
                case MARKER_COMPLETED:
                    color = ContextCompat.getColor(getContext(), R.color.green_500);
                    break;
                case MARKER_MISSED:
                    color = ContextCompat.getColor(getContext(), R.color.red_500);
                    break;
                default:
                    continue;
            }

            markerPaint.setColor(color);


        }
    }
} 