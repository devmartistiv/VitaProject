package com.martist.vitamove.measurement;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.martist.vitamove.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MeasurementChartManager {
    private static final String TAG = "MeasurementChartManager";
    private static final int MIN_MEASUREMENTS_FOR_CHART = 2;
    private static final int MAX_VISIBLE_POINTS = 30;

    private final Context context;
    private final LineChart chart;
    private final View emptyChartContainer;


    public MeasurementChartManager(Context context, LineChart chart, View emptyChartContainer) {
        this.context = context;
        this.chart = chart;
        this.emptyChartContainer = emptyChartContainer;

        setupChart();
    }


    private void setupChart() {

        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);


        chart.setExtraOffsets(8, 8, 8, 8);


        Description description = new Description();
        description.setEnabled(false);
        chart.setDescription(description);


        Legend legend = chart.getLegend();
        legend.setEnabled(false);


        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(getColor(R.color.chart_grid_color));
        xAxis.setGridLineWidth(0.5f);
        xAxis.setTextColor(getColor(R.color.profile_text_secondary));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setAvoidFirstLastClipping(true);


        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(getColor(R.color.chart_grid_color));
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setTextColor(getColor(R.color.profile_text_secondary));
        leftAxis.setTextSize(10f);
        leftAxis.setGranularity(0.5f);
        leftAxis.setGranularityEnabled(true);


        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });


        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);


        chart.setMinimumHeight(600);
    }


    public void updateChartData(List<MeasurementRecord> measurements) {
        if (measurements == null || measurements.size() < MIN_MEASUREMENTS_FOR_CHART) {
            showEmptyState();
            return;
        }


        showChart();


        List<Entry> entries = prepareChartEntries(measurements);

        if (entries.isEmpty()) {
            showEmptyState();
            return;
        }


        LineDataSet dataSet = createDataSet(entries);


        setupXAxisFormatter(measurements);


        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        lineData.setDrawValues(true);
        lineData.setValueTextSize(9f);
        lineData.setValueTextColor(getColor(R.color.profile_text_primary));


        lineData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });

        chart.setData(lineData);


        chart.animateX(800);


        chart.invalidate();
    }


    private List<Entry> prepareChartEntries(List<MeasurementRecord> measurements) {
        List<Entry> entries = new ArrayList<>();


        List<MeasurementRecord> sortedMeasurements = new ArrayList<>(measurements);
        Collections.reverse(sortedMeasurements);


        int startIndex = Math.max(0, sortedMeasurements.size() - MAX_VISIBLE_POINTS);
        List<MeasurementRecord> visibleMeasurements = sortedMeasurements.subList(
                startIndex,
                sortedMeasurements.size()
        );


        for (int i = 0; i < visibleMeasurements.size(); i++) {
            MeasurementRecord record = visibleMeasurements.get(i);
            entries.add(new Entry(i, record.getValue()));
        }

        return entries;
    }


    private LineDataSet createDataSet(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Замеры");


        int primaryColor = getColor(R.color.colorPrimary);
        dataSet.setColor(primaryColor);
        dataSet.setLineWidth(2.5f);


        dataSet.setDrawFilled(true);
        dataSet.setFillColor(primaryColor);
        dataSet.setFillAlpha(50);


        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(primaryColor);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setDrawCircleHole(true);


        dataSet.setHighLightColor(getColor(R.color.colorAccent));
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.setDrawHighlightIndicators(true);


        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.15f);

        return dataSet;
    }


    private void setupXAxisFormatter(List<MeasurementRecord> measurements) {

        List<MeasurementRecord> sortedMeasurements = new ArrayList<>(measurements);
        Collections.reverse(sortedMeasurements);


        int startIndex = Math.max(0, sortedMeasurements.size() - MAX_VISIBLE_POINTS);
        List<MeasurementRecord> visibleMeasurements = sortedMeasurements.subList(
                startIndex,
                sortedMeasurements.size()
        );

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", new Locale("ru", "RU"));

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < visibleMeasurements.size()) {
                    Date date = visibleMeasurements.get(index).getDate();
                    return dateFormat.format(date);
                }
                return "";
            }
        });


        int labelCount = Math.min(visibleMeasurements.size(), 6);
        xAxis.setLabelCount(labelCount, false);
    }


    private void showChart() {
        chart.setVisibility(View.VISIBLE);
        emptyChartContainer.setVisibility(View.GONE);
    }


    private void showEmptyState() {
        chart.setVisibility(View.GONE);
        emptyChartContainer.setVisibility(View.VISIBLE);
        chart.clear();
    }


    public void clearChart() {
        chart.clear();
        showEmptyState();
    }


    private int getColor(int colorResId) {
        return ContextCompat.getColor(context, colorResId);
    }
}




