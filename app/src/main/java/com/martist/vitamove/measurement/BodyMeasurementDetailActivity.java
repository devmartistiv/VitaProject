package com.martist.vitamove.measurement;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.core.ui.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class BodyMeasurementDetailActivity extends BaseActivity {
    private static final String TAG = "BodyMeasurementDetail";
    public static final String EXTRA_BODY_PART = "body_part";
    public static final String EXTRA_ICON_RES_ID = "icon_res_id";
    public static final String EXTRA_CURRENT_VALUE = "current_value";
    private static final String PREFS_BODY_MEASUREMENTS = "body_measurements";


    private ImageView bodyPartIcon;
    private TextView bodyPartTitle;
    private TextView currentValueText;
    private TextView lastMeasurementText;
    private MaterialCardView currentValueCard;
    private TextView trendDescription;
    private ImageView trendIcon;
    private TextView changeValue;
    private MaterialButton addMeasurementButton;
    private RecyclerView historyRecyclerView;
    private MeasurementHistoryAdapter historyAdapter;
    private LineChart measurementChart;
    private View emptyChartContainer;
    private MeasurementChartManager chartManager;


    private String bodyPart;
    private int iconResId;
    private float currentValue;
    private List<MeasurementRecord> measurementHistory;
    private SharedPreferences measurementsPrefs;
    private SupabaseBodyMeasurementRepository repository;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_measurement_detail);


        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));


        getIntentData();


        initializeViews();


        loadMeasurementData();


        setupHistoryRecyclerView();

        Log.d(TAG, "BodyMeasurementDetailActivity инициализирована для " + bodyPart);
    }


    private void getIntentData() {
        Intent intent = getIntent();
        bodyPart = intent.getStringExtra(EXTRA_BODY_PART);
        iconResId = intent.getIntExtra(EXTRA_ICON_RES_ID, R.drawable.ic_biceps);
        currentValue = intent.getFloatExtra(EXTRA_CURRENT_VALUE, 0);

        if (bodyPart == null) {
            bodyPart = "Неизвестная часть тела";
        }
    }


    private void initializeViews() {

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(bodyPart);
        }


        measurementsPrefs = getSharedPreferences(PREFS_BODY_MEASUREMENTS, Context.MODE_PRIVATE);
        repository = new SupabaseBodyMeasurementRepository(this);


        bodyPartIcon = findViewById(R.id.bodyPartIcon);
        bodyPartTitle = findViewById(R.id.bodyPartTitle);
        currentValueText = findViewById(R.id.currentValueText);
        lastMeasurementText = findViewById(R.id.lastMeasurementText);
        currentValueCard = findViewById(R.id.currentValueCard);
        trendDescription = findViewById(R.id.trendDescription);
        trendIcon = findViewById(R.id.trendIcon);
        changeValue = findViewById(R.id.changeValue);
        addMeasurementButton = findViewById(R.id.addMeasurementButton);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        measurementChart = findViewById(R.id.measurementChart);
        emptyChartContainer = findViewById(R.id.emptyChartContainer);


        bodyPartIcon.setImageResource(iconResId);
        bodyPartTitle.setText(bodyPart);


        chartManager = new MeasurementChartManager(this, measurementChart, emptyChartContainer);


        addMeasurementButton.setOnClickListener(v -> showAddMeasurementDialog());
    }


    private void loadMeasurementData() {

        showLoadingState(true);


        String apiBodyPart = SupabaseBodyMeasurementRepository.convertBodyPartToApi(bodyPart);


        repository.getMeasurementHistory(apiBodyPart, 50, new SupabaseBodyMeasurementRepository.MeasurementListCallback() {
            @Override
            public void onSuccess(List<MeasurementRecord> measurements) {
                runOnUiThread(() -> {
                    measurementHistory = measurements;


                    if (!measurements.isEmpty()) {
                        currentValue = measurements.get(0).getValue();
                    }


                    updateCurrentValueDisplay();
                    updateHistoryDisplay();
                    showLoadingState(false);

                    Log.d(TAG, "Загружено " + measurements.size() + " замеров для " + bodyPart);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showErrorState(error);
                    Log.e(TAG, "Ошибка загрузки данных: " + error);
                });
            }
        });
    }


    private void updateCurrentValueDisplay() {
        if (currentValue > 0) {
            currentValueText.setText(String.format(Locale.getDefault(), "%.1f см", currentValue));


            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru", "RU"));
            lastMeasurementText.setText("Последний замер: " + dateFormat.format(new Date()));


            updateTrendDisplay();
        } else {
            currentValueText.setText("Замеров пока нет");
            lastMeasurementText.setText("Добавьте первый замер");
            findViewById(R.id.trendContainer).setVisibility(View.GONE);
        }
    }


    private void updateTrendDisplay() {
        View trendContainer = findViewById(R.id.trendContainer);

        if (measurementHistory != null && measurementHistory.size() >= 2) {

            MeasurementRecord current = measurementHistory.get(0);
            MeasurementRecord previous = measurementHistory.get(1);

            float change = current.getValue() - previous.getValue();

            if (Math.abs(change) > 0.01f) {
                trendContainer.setVisibility(View.VISIBLE);

                boolean isPositive = change > 0;
                trendIcon.setImageResource(isPositive ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
                int trendColor = getResources().getColor(isPositive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark);
                trendIcon.setColorFilter(trendColor);

                changeValue.setText(String.format(Locale.getDefault(), "%+.1f см", change));
                changeValue.setTextColor(trendColor);

                String trendText = isPositive ? "Увеличение" : "Уменьшение";
                trendDescription.setText(trendText + " с предыдущего замера");
            } else {
                trendContainer.setVisibility(View.GONE);
            }
        } else {
            trendContainer.setVisibility(View.GONE);
        }
    }


    private void setupHistoryRecyclerView() {
        historyAdapter = new MeasurementHistoryAdapter(measurementHistory);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);
    }


    private void generateMockHistory() {
        measurementHistory = new ArrayList<>();


        if (currentValue > 0) {
            measurementHistory.add(new MeasurementRecord(currentValue, new Date(), "Добавлено вручную"));
        }


        Random random = new Random();
        Calendar calendar = Calendar.getInstance();

        for (int i = 1; i <= 8; i++) {
            calendar.add(Calendar.DAY_OF_MONTH, -7);

            float baseValue = currentValue > 0 ? currentValue : 30.0f;
            float variation = (random.nextFloat() - 0.5f) * 4.0f;
            float value = Math.max(baseValue + variation, 20.0f);

            Date date = new Date(calendar.getTimeInMillis());
            String note = i % 3 == 0 ? "После тренировки" : (i % 2 == 0 ? "Утренний замер" : "");

            measurementHistory.add(new MeasurementRecord(value, date, note));
        }


        updateTrendDisplay();
    }


    private void showAddMeasurementDialog() {
        if (isLoading) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);


        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(16, 16, 16, 16);


        TextInputEditText valueInput = new TextInputEditText(this);
        valueInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        valueInput.setHint("Введите значение в см");

        if (currentValue > 0) {
            valueInput.setText(String.valueOf(currentValue));
        }

        TextInputLayout valueLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        valueLayout.addView(valueInput);
        valueLayout.setHint("Замер " + bodyPart.toLowerCase() + " (см)");


        TextInputEditText noteInput = new TextInputEditText(this);
        noteInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        noteInput.setHint("Заметка (необязательно)");
        noteInput.setMaxLines(3);

        TextInputLayout noteLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        noteLayout.addView(noteInput);
        noteLayout.setHint("Заметка");


        container.addView(valueLayout);


        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        noteParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        noteLayout.setLayoutParams(noteParams);
        container.addView(noteLayout);

        builder.setTitle("Новый замер")
                .setMessage("Введите значение и опционально добавьте заметку")
                .setView(container)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String valueStr = valueInput.getText().toString().trim();
                    String noteStr = noteInput.getText().toString().trim();

                    if (!valueStr.isEmpty()) {
                        try {
                            float newValue = Float.parseFloat(valueStr);
                            addNewMeasurement(newValue, noteStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Некорректное значение", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void addNewMeasurement(float newValue) {
        addNewMeasurement(newValue, "");
    }


    private void addNewMeasurement(float newValue, String note) {
        if (isLoading) return;

        showLoadingState(true);
        String apiBodyPart = SupabaseBodyMeasurementRepository.convertBodyPartToApi(bodyPart);

        repository.addMeasurement(apiBodyPart, newValue, note, new SupabaseBodyMeasurementRepository.MeasurementCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {

                    currentValue = newValue;


                    if (measurementHistory == null) {
                        measurementHistory = new ArrayList<>();
                    }

                    MeasurementRecord newRecord = new MeasurementRecord(newValue, new Date(), note);
                    measurementHistory.add(0, newRecord);


                    updateCurrentValueDisplay();
                    updateHistoryDisplay();
                    showLoadingState(false);

                    Toast.makeText(BodyMeasurementDetailActivity.this, "Замер добавлен", Toast.LENGTH_SHORT).show();


                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_BODY_PART, bodyPart);
                    resultIntent.putExtra(EXTRA_CURRENT_VALUE, newValue);
                    setResult(RESULT_OK, resultIntent);

                    Log.d(TAG, "Добавлен новый замер " + bodyPart + ": " + newValue);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    Toast.makeText(BodyMeasurementDetailActivity.this,
                            "Ошибка добавления замера: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Ошибка добавления замера: " + error);
                });
            }
        });
    }


    private void saveMeasurement(float value) {
        SharedPreferences.Editor editor = measurementsPrefs.edit();


        String key = getPreferenceKey();

        editor.putFloat(key + "_current", value);
        editor.putLong(key + "_date", new Date().getTime());
        editor.apply();
    }


    private void showLoadingState(boolean loading) {
        isLoading = loading;
        addMeasurementButton.setEnabled(!loading);

        if (loading) {
            addMeasurementButton.setText("Загрузка...");
        } else {
            addMeasurementButton.setText("Добавить новый замер");
        }
    }


    private void showErrorState(String error) {
        Toast.makeText(this, "Ошибка загрузки: " + error, Toast.LENGTH_LONG).show();


        if (measurementHistory == null || measurementHistory.isEmpty()) {
            View emptyState = findViewById(R.id.emptyHistoryContainer);
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
            }
        }
    }


    private void updateHistoryDisplay() {
        if (historyAdapter == null) {
            historyAdapter = new MeasurementHistoryAdapter(measurementHistory);
            historyRecyclerView.setAdapter(historyAdapter);
        } else {
            historyAdapter.updateMeasurements(measurementHistory);
        }


        View emptyState = findViewById(R.id.emptyHistoryContainer);
        if (emptyState != null) {
            emptyState.setVisibility(
                    (measurementHistory == null || measurementHistory.isEmpty()) ? View.VISIBLE : View.GONE
            );
        }


        if (chartManager != null) {
            chartManager.updateChartData(measurementHistory);
        }
    }


    private String getPreferenceKey() {
        switch (bodyPart.toLowerCase()) {
            case "бицепс левый":
                return "bicep_left";
            case "бицепс правый":
                return "bicep_right";
            case "талия":
                return "waist_measurement";
            default:
                return bodyPart.toLowerCase().replace(" ", "_");
        }
    }


    public static void start(Context context, String bodyPart, int iconResId, float currentValue) {
        Intent intent = new Intent(context, BodyMeasurementDetailActivity.class);
        intent.putExtra(EXTRA_BODY_PART, bodyPart);
        intent.putExtra(EXTRA_ICON_RES_ID, iconResId);
        intent.putExtra(EXTRA_CURRENT_VALUE, currentValue);
        context.startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
