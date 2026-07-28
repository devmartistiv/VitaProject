package com.martist.vitamove.steps;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.martist.vitamove.R;
import com.martist.vitamove.core.ui.views.StepsMarkerView;
import com.martist.vitamove.dashboard.DashboardData;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.steps.data.StepCounterManager;
import com.martist.vitamove.steps.data.StepHistoryRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class StepsStatsFragment extends Fragment {
    private static final String TAG = "StepsStatsFragment";


    private TextView stepsTodayCount;
    private TextView stepsTodayGoal;
    private TextView stepsTodayDistance;
    private ProgressBar stepsTodayProgress;
    private BarChart stepsBarChart;
    private TextView chartTitle;
    private RadioButton radioWeek;
    private ImageButton btnBack;


    private DashboardManager dashboardManager;
    private StepHistoryRepository stepHistoryRepository;


    private static final double STEP_TO_KM_RATIO = 0.0007;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_steps_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.steps_status_bar));


            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }
        initViews(view);
        initManagers();
        setupListeners();
        loadWeeklyData();
    }


    private void initViews(View view) {
        stepsTodayCount = view.findViewById(R.id.steps_today_count);
        stepsTodayGoal = view.findViewById(R.id.steps_today_goal);
        stepsTodayDistance = view.findViewById(R.id.steps_today_distance);
        stepsTodayProgress = view.findViewById(R.id.steps_today_progress);
        stepsBarChart = view.findViewById(R.id.steps_bar_chart);
        chartTitle = view.findViewById(R.id.chart_title);
        radioWeek = view.findViewById(R.id.radio_week);
        btnBack = view.findViewById(R.id.btn_back);
    }


    private void initManagers() {
        dashboardManager = DashboardManager.getInstance(requireContext());
        stepHistoryRepository = StepHistoryRepository.Companion.getInstance(requireContext());


        dashboardManager.checkAndResetDailyDataIfNeeded();


        dashboardManager.syncStepsForStatistics();


        StepCounterManager stepCounterManager = StepCounterManager.getInstance(requireContext());
        stepCounterManager.checkAndResetForNewDayIfNeeded();


        DashboardData dashboardData = dashboardManager.getDashboardData();


        updateStepsUI(dashboardData);

    }


    private void setupListeners() {

        RadioGroup periodToggle = requireView().findViewById(R.id.period_toggle);
        periodToggle.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_week) {
                loadWeeklyData();
            } else {
                loadMonthlyData();
            }
        });


        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }


    private void updateStepsUI(DashboardData data) {
        int stepsToday = data.getStepsToday();
        int stepsGoal = data.getStepsGoal();
        int progress = (int) ((float) stepsToday / stepsGoal * 100);


        progress = Math.min(progress, 100);


        double distanceKm = stepsToday * STEP_TO_KM_RATIO;


        stepsTodayCount.setText(String.format(Locale.getDefault(), "%,d", stepsToday));
        stepsTodayGoal.setText(String.format(Locale.getDefault(), "из %,d", stepsGoal));
        stepsTodayDistance.setText(String.format(Locale.getDefault(), "%.2f км", distanceKm));
        stepsTodayProgress.setProgress(progress);
    }


    private void loadWeeklyData() {

        chartTitle.setText("Шаги за неделю");


        List<Integer> weeklySteps = stepHistoryRepository.getStepsForLastWeek();


        setupBarChart(weeklySteps, getDayNamesForWeek());
    }


    private void loadMonthlyData() {

        chartTitle.setText("Шаги за месяц");


        List<Integer> monthlySteps = stepHistoryRepository.getStepsForCurrentMonth();


        List<Integer> filteredSteps = new ArrayList<>();
        List<String> filteredLabels = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);


        for (int i = 0; i < monthlySteps.size(); i++) {
            int steps = monthlySteps.get(i);
            if (steps > 0) {
                filteredSteps.add(steps);
                filteredLabels.add(String.valueOf(i + 1));
            }
        }


        if (filteredSteps.isEmpty()) {
            setupEmptyChart("Нет данных о шагах за этот месяц");
            return;
        }


        setupBarChart(filteredSteps, filteredLabels);
    }


    private void setupBarChart(List<Integer> stepsData, List<String> labels) {
        ArrayList<BarEntry> entries = new ArrayList<>();


        for (int i = 0; i < stepsData.size(); i++) {
            entries.add(new BarEntry(i, stepsData.get(i)));
        }


        BarDataSet dataSet = new BarDataSet(entries, "Шаги");
        dataSet.setColor(Color.parseColor("#7C4DFF"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);


        dataSet.setDrawValues(false);


        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });


        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);


        stepsBarChart.clear();
        stepsBarChart.setData(barData);


        stepsBarChart.fitScreen();


        XAxis xAxis = stepsBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));


        if (labels.size() > 7) {

            xAxis.setGranularity(1f);
            xAxis.setLabelRotationAngle(0);


            int labelCount = Math.min(7, labels.size() / 5 + 1);
            xAxis.setLabelCount(labelCount, true);


            xAxis.setYOffset(5f);
            xAxis.setAvoidFirstLastClipping(true);


            stepsBarChart.setDragEnabled(true);
            stepsBarChart.setScaleEnabled(true);


            stepsBarChart.setVisibleXRangeMaximum(7);


            xAxis.setAxisMinimum(-0.5f);
            xAxis.setAxisMaximum(labels.size() - 0.5f);


            stepsBarChart.moveViewToX(0);
        } else {

            xAxis.setLabelCount(7);
            xAxis.setAxisMinimum(-0.5f);
            xAxis.setAxisMaximum(6.5f);
            stepsBarChart.setDragEnabled(false);
            stepsBarChart.setScaleEnabled(false);
        }


        StepsMarkerView mv = new StepsMarkerView(getContext(), R.layout.custom_marker_view, labels);
        mv.setChartView(stepsBarChart);
        stepsBarChart.setMarker(mv);


        stepsBarChart.setHighlightPerTapEnabled(true);

        YAxis leftAxis = stepsBarChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setSpaceTop(35f);
        leftAxis.setAxisMinimum(0f);

        stepsBarChart.getAxisRight().setEnabled(false);
        stepsBarChart.getLegend().setEnabled(false);
        stepsBarChart.getDescription().setEnabled(false);


        stepsBarChart.setExtraOffsets(10f, 0f, 10f, 10f);


        stepsBarChart.animateY(500);


        stepsBarChart.invalidate();
    }


    private List<String> getDayNamesForWeek() {
        return new ArrayList<String>() {{
            add("Пн");
            add("Вт");
            add("Ср");
            add("Чт");
            add("Пт");
            add("Сб");
            add("Вс");
        }};
    }


    private void setupEmptyChart(String message) {

        stepsBarChart.clear();
        stepsBarChart.setData(null);
        stepsBarChart.invalidate();


        stepsBarChart.setNoDataText(message);
        stepsBarChart.setNoDataTextColor(Color.GRAY);

        Log.d(TAG, "Отображен пустой график с сообщением: " + message);
    }

    @Override
    public void onResume() {
        super.onResume();


        dashboardManager.checkAndResetDailyDataIfNeeded();


        StepCounterManager stepCounterManager = StepCounterManager.getInstance(requireContext());
        stepCounterManager.checkAndResetForNewDayIfNeeded();


        dashboardManager.syncStepsForStatistics();
        DashboardData dashboardData = dashboardManager.getDashboardData();
        updateStepsUI(dashboardData);


        if (radioWeek.isChecked()) {
            loadWeeklyData();
        } else {
            loadMonthlyData();
        }
    }
} 