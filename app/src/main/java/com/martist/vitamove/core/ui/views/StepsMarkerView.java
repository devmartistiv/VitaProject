package com.martist.vitamove.core.ui.views;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.martist.vitamove.R;

import java.util.List;
import java.util.Locale;


public class StepsMarkerView extends MarkerView {

    private final TextView tvContent;
    private final List<String> labels;


    public StepsMarkerView(Context context, int layoutResource, List<String> labels) {
        super(context, layoutResource);
        this.labels = labels;
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        int steps = (int) e.getY();

        String label = "";
        if (labels != null && index >= 0 && index < labels.size()) {
            label = labels.get(index);
        }


        tvContent.setText(String.format(Locale.getDefault(), "%s: %d шагов", label, steps));

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {

        return new MPPointF(-(getWidth() / 2f), -getHeight() - 10);
    }
} 