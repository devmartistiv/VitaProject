package com.martist.vitamove.weight.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.managers.CaloriesManager;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.user.UserProfile;
import com.martist.vitamove.weight.data.local.entities.UserWeightEntity;
import com.martist.vitamove.weight.ui.adapter.WeightHistoryAdapter;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class WeightHistoryActivity extends BaseActivity {

    private static final String TAG = "WeightHistoryActivity";

    private UserWeightViewModel weightViewModel;
    private LineChart weightChart;
    private RecyclerView weightHistoryRecyclerView;
    private WeightHistoryAdapter adapter;

    private TextView currentWeightView;
    private TextView initialWeightView;
    private TextView targetWeightView;
    private TextView weightLostView;
    private TextView weightRemainingView;
    private ImageButton btnBack;
    private MaterialButton btnUpdateWeight;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView syncStatusText;

    private SupabaseClient supabaseClient;
    private String userId;


    private final MutableLiveData<Float> initialWeightLiveData = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_history);


        initSupabaseClient();


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.weight_gradient_start));


        initViews();


        weightViewModel = new ViewModelProvider(this).get(UserWeightViewModel.class);


        adapter = new WeightHistoryAdapter();
        weightHistoryRecyclerView.setAdapter(adapter);


        btnBack.setOnClickListener(v -> finish());


        btnUpdateWeight.setOnClickListener(v -> showUpdateWeightDialog());


        swipeRefreshLayout.setOnRefreshListener(this::syncWeightData);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );


        observeSyncStatus();


        observeWeightData();


        observeInitialWeight();


        syncWeightData();


    }


    private void observeInitialWeight() {

        SharedPreferences prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        float initialWeight = prefs.getFloat("initial_weight", 0f);
        initialWeightLiveData.setValue(initialWeight);


        SharedPreferences.OnSharedPreferenceChangeListener listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                        if ("initial_weight".equals(key)) {
                            float newInitialWeight = sharedPreferences.getFloat(key, 0f);
                            initialWeightLiveData.postValue(newInitialWeight);
                            Log.d(TAG, "Обнаружено изменение начального веса: " + newInitialWeight + " кг");
                        }
                    }
                };

        prefs.registerOnSharedPreferenceChangeListener(listener);


        initialWeightLiveData.observe(this, newInitialWeight -> {
            if (newInitialWeight > 0) {
                initialWeightView.setText(String.format(Locale.getDefault(), "%.1f кг", newInitialWeight));
                updateWeightProgress(newInitialWeight);
            }
        });
    }


    private void updateWeightProgress(float initialWeight) {

        SharedPreferences prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        float currentWeight = prefs.getFloat("current_weight", 0f);
        float targetWeight = prefs.getFloat("target_weight", 0f);

        if (currentWeight > 0 && targetWeight > 0 && initialWeight > 0) {

            boolean isWeightLoss = targetWeight < initialWeight;


            float weightLost;
            float weightRemaining;

            if (isWeightLoss) {

                weightLost = Math.max(0, initialWeight - currentWeight);
                weightRemaining = Math.max(0, currentWeight - targetWeight);
            } else {

                weightLost = Math.max(0, currentWeight - initialWeight);
                weightRemaining = Math.max(0, targetWeight - currentWeight);
            }


            weightLostView.setText(String.format(Locale.getDefault(), "%.1f кг", weightLost));
            weightRemainingView.setText(String.format(Locale.getDefault(), "%.1f кг", weightRemaining));


            TextView weightLostLabel = findViewById(R.id.weight_lost_label);
            TextView weightRemainingLabel = findViewById(R.id.weight_remaining_label);

            if (isWeightLoss) {
                weightLostLabel.setText("Сброшено");
                weightRemainingLabel.setText("Осталось");

                weightLostView.setTextColor(ContextCompat.getColor(this, R.color.green_500));
            } else {
                weightLostLabel.setText("Набрано");
                weightRemainingLabel.setText("Осталось");

                weightLostView.setTextColor(ContextCompat.getColor(this, R.color.green_500));
            }

            Log.d(TAG, "Прогресс веса обновлен: начальный=" + initialWeight +
                    " кг, текущий=" + currentWeight +
                    " кг, целевой=" + targetWeight +
                    " кг, потеряно/набрано=" + weightLost +
                    " кг, осталось=" + weightRemaining + " кг");
        } else {
            Log.d(TAG, "Невозможно рассчитать прогресс веса: currentWeight=" + currentWeight +
                    ", targetWeight=" + targetWeight + ", initialWeight=" + initialWeight);
        }
    }


    private void syncWeightData() {
        if (userId == null || userId.isEmpty()) {
            showSnackbar("Невозможно синхронизировать данные: пользователь не авторизован");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }


        weightViewModel.syncFromSupabase();
    }


    private void observeSyncStatus() {
        weightViewModel.getIsSyncing().observe(this, isSyncing -> {
            progressBar.setVisibility(isSyncing ? View.VISIBLE : View.GONE);
            swipeRefreshLayout.setRefreshing(isSyncing);
        });


        syncStatusText.setVisibility(View.GONE);
    }


    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }


    private void initSupabaseClient() {

        String apiKey = Constants.SUPABASE_CLIENT_SECRET;
        String url = "https://" + Constants.SUPABASE_CLIENT_ID + ".supabase.co";


        supabaseClient = SupabaseClient.getInstance(url, apiKey);


        SharedPreferences authPrefs = getSharedPreferences("auth_data", Context.MODE_PRIVATE);
        userId = authPrefs.getString("user_id", null);

        if (userId == null || userId.isEmpty()) {

            SharedPreferences appPrefs = getSharedPreferences(VitaMoveApplication.PREFS_NAME, MODE_PRIVATE);
            String accessToken = appPrefs.getString("accessToken", null);
            if (accessToken != null) {
                try {
                    String[] jwtParts = accessToken.split("\\.");
                    if (jwtParts.length > 1) {
                        String payload = new String(android.util.Base64.decode(jwtParts[1], android.util.Base64.DEFAULT));
                        JSONObject jwtJson = new JSONObject(payload);
                        userId = jwtJson.getString("sub");
                        Log.d(TAG, "Извлечен ID пользователя из токена: " + userId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при извлечении userId из токена: " + e.getMessage());
                }
            }
        }

        Log.d(TAG, "SupabaseClient инициализирован, userId: " + (userId != null ? userId : "не найден"));
    }


    private void showUpdateWeightDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_weight, null);


        TextInputEditText weightInput = dialogView.findViewById(R.id.weight_input);
        TextInputEditText notesInput = dialogView.findViewById(R.id.notes_input);


        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setTitle("Обновить текущий вес")
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();


        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(ContextCompat.getColor(this, R.color.white_text));
        }

        dialog.show();


        Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        saveButton.setOnClickListener(v -> {
            String weightStr = weightInput.getText().toString().trim();
            if (weightStr.isEmpty()) {
                weightInput.setError("Введите вес");
                return;
            }

            try {
                float weight = Float.parseFloat(weightStr);
                if (weight <= 0 || weight > 500) {
                    weightInput.setError("Введите корректный вес (0-500 кг)");
                    return;
                }


                String notes = notesInput.getText().toString().trim();


                addNewWeightRecord(weight, notes);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                weightInput.setError("Введите числовое значение");
            }
        });
    }


    private void addNewWeightRecord(float weight, String notes) {


        weightViewModel.addWeightRecord(weight, notes);


        updateCurrentWeightInProfile(weight);


        syncCurrentWeightToSupabase(weight);


        Intent updateIntent = new Intent("com.martist.vitamove.UPDATE_DASHBOARD");
        updateIntent.putExtra("update_source", "weight_history");
        sendBroadcast(updateIntent);

        Toast.makeText(this, "Вес успешно обновлен", Toast.LENGTH_SHORT).show();
    }


    private void updateCurrentWeightInProfile(float weight) {
        SharedPreferences prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE);


        String name = prefs.getString("name", "");
        int age = prefs.getInt("age", 30);
        String gender = prefs.getString("gender", "Мужчина");
        float targetWeight = prefs.getFloat("target_weight", weight);
        float height = prefs.getFloat("height", 170f);
        float bodyFat = prefs.getFloat("body_fat", 20f);
        float waist = prefs.getFloat("waist", 80f);


        UserProfile userProfile = new UserProfile(name, age, gender, weight, targetWeight, height, bodyFat, waist);


        userProfile.updateTargetCalories();
        userProfile.updateTargetWater();


        int targetCalories = userProfile.getTargetCalories();
        float targetWater = userProfile.getTargetWater();


        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("current_weight", weight);
        editor.putInt("target_calories", targetCalories);
        editor.putFloat("target_water", targetWater);
        editor.apply();


        CaloriesManager caloriesManager = CaloriesManager.getInstance(this);
        caloriesManager.setTargetCalories(targetCalories);


        DashboardManager dashboardManager = DashboardManager.getInstance(this);
        dashboardManager.updateCaloriesGoalFromProfile();

        Log.d(TAG, "Профиль обновлен: вес=" + weight + " кг, калории=" + targetCalories +
                ", вода=" + targetWater + " л");
    }


    private void syncCurrentWeightToSupabase(float weight) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Невозможно синхронизировать с Supabase: userId не найден");
            return;
        }


        SharedPreferences prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String name = prefs.getString("name", "");
        int age = prefs.getInt("age", 0);
        String gender = prefs.getString("gender", "");
        String fitnessGoal = prefs.getString("fitness_goal", "");
        float height = prefs.getFloat("height", 0);
        float targetWeight = prefs.getFloat("target_weight", 0);
        String fitnessLevel = prefs.getString("fitness_level", "intermediate");
        boolean isMetric = prefs.getBoolean("is_metric", true);


        new Thread(() -> {
            try {
                boolean success = supabaseClient.updateUserProfile(
                        userId, name, age, gender, fitnessGoal, height,
                        weight, targetWeight, fitnessLevel, isMetric);

                if (success) {
                    Log.d(TAG, "Профиль успешно обновлен в Supabase");
                } else {
                    Log.e(TAG, "Не удалось обновить профиль в Supabase");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при синхронизации с Supabase: " + e.getMessage(), e);
            }
        }).start();
    }


    private void initViews() {
        weightChart = findViewById(R.id.weight_chart);
        weightHistoryRecyclerView = findViewById(R.id.weight_history_recycler_view);
        weightHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        currentWeightView = findViewById(R.id.current_weight);
        initialWeightView = findViewById(R.id.initial_weight);
        targetWeightView = findViewById(R.id.target_weight);
        weightLostView = findViewById(R.id.weight_lost);
        weightRemainingView = findViewById(R.id.weight_remaining);
        btnBack = findViewById(R.id.btn_back);
        btnUpdateWeight = findViewById(R.id.btn_update_weight);


        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        syncStatusText = findViewById(R.id.sync_status_text);


        progressBar.setVisibility(View.GONE);
        syncStatusText.setVisibility(View.GONE);
    }


    private void observeWeightData() {

        weightViewModel.getAllWeightRecords().observe(this, weightRecords -> {
            if (weightRecords != null && !weightRecords.isEmpty()) {

                updateWeightSummary(weightRecords);
                setupWeightChart(weightRecords);


                adapter.setWeightRecords(weightRecords);
            }
        });


        adapter.setOnWeightRecordDeleteListener(this::showDeleteWeightDialog);
    }


    private void updateWeightSummary(List<UserWeightEntity> weightRecords) {
        if (weightRecords.isEmpty()) {

            SharedPreferences prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE);
            float currentWeightFromPrefs = prefs.getFloat("current_weight", 0f);
            float targetWeightFromPrefs = prefs.getFloat("target_weight", 0f);

            if (currentWeightFromPrefs > 0) {
                currentWeightView.setText(String.format(Locale.getDefault(), "%.1f кг", currentWeightFromPrefs));
                Log.d(TAG, "Используем вес из SharedPreferences (нет записей): " + currentWeightFromPrefs + " кг");
            } else {
                currentWeightView.setText("--");
            }

            if (targetWeightFromPrefs > 0) {
                targetWeightView.setText(String.format(Locale.getDefault(), "%.1f кг", targetWeightFromPrefs));
            } else {
                targetWeightView.setText("Не установлено");
            }

            weightLostView.setText("0.0 кг");
            weightRemainingView.setText("0.0 кг");
            return;
        }


        SharedPreferences prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        float targetWeight = prefs.getFloat("target_weight", 0f);
        float currentWeightFromPrefs = prefs.getFloat("current_weight", 0f);


        UserWeightEntity latestRecord = weightRecords.get(0);
        float currentWeightFromDb = latestRecord.getWeight();


        boolean usePrefsWeight = false;
        if (currentWeightFromPrefs > 0 && Math.abs(currentWeightFromPrefs - currentWeightFromDb) > 5) {
            Log.w(TAG, "Обнаружено значительное расхождение между весом в БД (" + currentWeightFromDb +
                    " кг) и SharedPreferences (" + currentWeightFromPrefs + " кг). Используем вес из SharedPreferences.");

            usePrefsWeight = true;
        }


        float currentWeight = usePrefsWeight ? currentWeightFromPrefs : currentWeightFromDb;


        currentWeightView.setText(String.format(Locale.getDefault(), "%.1f кг", currentWeight));


        if (targetWeight > 0) {
            targetWeightView.setText(String.format(Locale.getDefault(), "%.1f кг", targetWeight));
        } else {
            targetWeightView.setText("Не установлено");
            weightLostView.setText("0.0 кг");
            weightRemainingView.setText("0.0 кг");
            return;
        }


        if (!usePrefsWeight) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("current_weight", currentWeight);
            editor.apply();

            Log.d(TAG, "Текущий вес обновлен в SharedPreferences: " + currentWeight + " кг");
        }


        float initialWeight = initialWeightLiveData.getValue() != null ? initialWeightLiveData.getValue() : 0f;
        if (initialWeight > 0) {
            updateWeightProgress(initialWeight);
        }
    }


    private void setupWeightChart(List<UserWeightEntity> weightRecords) {
        if (weightRecords.isEmpty()) return;

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());


        for (int i = weightRecords.size() - 1; i >= 0; i--) {
            UserWeightEntity record = weightRecords.get(i);
            entries.add(new Entry(weightRecords.size() - 1 - i, record.getWeight()));
            labels.add(dateFormat.format(record.getDateAsDate()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Вес (кг)");
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);


        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.colorPrimaryLight));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);

        weightChart.setData(lineData);
        weightChart.getDescription().setEnabled(false);


        XAxis xAxis = weightChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45f);

        YAxis leftAxis = weightChart.getAxisLeft();
        leftAxis.setGranularity(1f);


        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;

        for (UserWeightEntity record : weightRecords) {
            minWeight = Math.min(minWeight, record.getWeight());
            maxWeight = Math.max(maxWeight, record.getWeight());
        }


        float padding = (maxWeight - minWeight) * 0.1f;
        leftAxis.setAxisMinimum(Math.max(0, minWeight - padding));
        leftAxis.setAxisMaximum(maxWeight + padding);

        weightChart.getAxisRight().setEnabled(false);


        Legend legend = weightChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);


        weightChart.setTouchEnabled(true);
        weightChart.setDragEnabled(true);
        weightChart.setScaleEnabled(true);
        weightChart.setPinchZoom(true);
        weightChart.setDoubleTapToZoomEnabled(true);
        weightChart.invalidate();
    }


    private void showDeleteWeightDialog(UserWeightEntity weightRecord) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Удаление записи")
                .setMessage("Вы действительно хотите удалить запись о весе?")

                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteWeightRecord(weightRecord);
                })
                .setNegativeButton("Отмена", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();


        dialog.setOnShowListener(dialogInterface -> {
            TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
            if (titleView != null) {
                titleView.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
            }

            TextView messageView = dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
            }
        });

        dialog.show();
    }


    private void deleteWeightRecord(UserWeightEntity weightRecord) {

        weightViewModel.deleteWeightRecord(weightRecord);


        adapter.removeWeightRecord(weightRecord);


        showSnackbar("Запись о весе удалена");
    }


    private void checkAndRemoveDuplicateWeightRecords() {
        if (weightViewModel == null) return;

        weightViewModel.getAllWeightRecords().observe(this, weightRecords -> {
            if (weightRecords != null && weightRecords.size() > 1) {

                Map<String, UserWeightEntity> uniqueRecords = new HashMap<>();
                List<UserWeightEntity> duplicatesToRemove = new ArrayList<>();


                List<UserWeightEntity> correctionRecords = new ArrayList<>();


                for (UserWeightEntity record : weightRecords) {

                    if (record.getNotes() != null && record.getNotes().equals("Корректировка веса")) {
                        correctionRecords.add(record);
                        continue;
                    }


                    String key = record.getDate() + "_" + record.getWeight();


                    if (uniqueRecords.containsKey(key)) {

                        if (record.getNotes() != null &&
                                record.getNotes().equals(uniqueRecords.get(key).getNotes())) {
                            duplicatesToRemove.add(record);
                            Log.d(TAG, "Найдена дублирующаяся запись о весе: " +
                                    record.getWeight() + " кг, дата: " +
                                    new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                            .format(record.getDateAsDate()));
                        }
                    } else {

                        uniqueRecords.put(key, record);
                    }
                }


                if (correctionRecords.size() > 1) {

                    Collections.sort(correctionRecords, (r1, r2) ->
                            r2.getDateAsDate().compareTo(r1.getDateAsDate()));


                    for (int i = 1; i < correctionRecords.size(); i++) {
                        duplicatesToRemove.add(correctionRecords.get(i));
                        Log.d(TAG, "Найдена устаревшая запись корректировки веса: " +
                                correctionRecords.get(i).getWeight() + " кг, дата: " +
                                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                        .format(correctionRecords.get(i).getDateAsDate()));
                    }
                }


                if (!duplicatesToRemove.isEmpty()) {
                    Log.d(TAG, "Удаление " + duplicatesToRemove.size() + " дублирующихся записей о весе");

                    for (UserWeightEntity duplicate : duplicatesToRemove) {
                        weightViewModel.deleteWeightRecord(duplicate);
                    }


                    if (duplicatesToRemove.size() > 0) {
                        showSnackbar("Удалены " + duplicatesToRemove.size() + " дублирующиеся записи о весе");
                    }
                }
            }
        });
    }
} 