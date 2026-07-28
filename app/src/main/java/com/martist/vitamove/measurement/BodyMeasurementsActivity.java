package com.martist.vitamove.measurement;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.weight.ui.UserWeightViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class BodyMeasurementsActivity extends BaseActivity {
    private static final String TAG = "BodyMeasurementsActivity";
    private static final int REQUEST_CODE_BODY_MEASUREMENT_DETAIL = 1001;
    private static final String PREFS_BODY_MEASUREMENTS = "body_measurements";


    private static final String[] DEFAULT_MEASUREMENTS = {
            "Бицепс левый", "Бицепс правый", "Талия"
    };


    private TextInputEditText weightInput, heightInput, targetWeightInput;
    private TextInputLayout weightLayout, heightLayout, targetWeightLayout;


    private List<BodyMeasurement> bodyMeasurements;
    private List<BodyMeasurement> additionalBodyMeasurements;
    private View bicepLeftMeasurement, bicepRightMeasurement, waistMeasurementView;


    private SupabaseBodyMeasurementRepository repository;


    private UserWeightViewModel userWeightViewModel;


    private SharedPreferences measurementsPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_measurements);


        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));


        repository = new SupabaseBodyMeasurementRepository(this);


        userWeightViewModel = new UserWeightViewModel(getApplication());

        initializeViews();
        loadSavedMeasurements();


        Log.d(TAG, "BodyMeasurementsActivity инициализирована");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BODY_MEASUREMENT_DETAIL && resultCode == RESULT_OK && data != null) {
            String bodyPart = data.getStringExtra(BodyMeasurementDetailActivity.EXTRA_BODY_PART);
            float newValue = data.getFloatExtra(BodyMeasurementDetailActivity.EXTRA_CURRENT_VALUE, 0);

            Log.d(TAG, "Получен обновленный замер: " + bodyPart + " = " + newValue);


            for (BodyMeasurement measurement : bodyMeasurements) {
                if (measurement.getBodyPart().equals(bodyPart)) {
                    measurement.setPreviousValue(measurement.getCurrentValue());
                    measurement.setCurrentValue(newValue);
                    measurement.setMeasurementDate(new Date());
                    break;
                }
            }


            setupBodyMeasurementViews();


            loadMeasurementsFromSupabase();
        }
    }


    private void initializeBodyMeasurements() {
        bodyMeasurements = new ArrayList<>();
        additionalBodyMeasurements = new ArrayList<>();


        bodyMeasurements.add(new BodyMeasurement("Бицепс левый", 0, R.drawable.ic_biceps));
        bodyMeasurements.add(new BodyMeasurement("Бицепс правый", 0, R.drawable.ic_biceps));
        bodyMeasurements.add(new BodyMeasurement("Талия", 0, R.drawable.ic_waist));


        initializeAdditionalBodyMeasurements();


        loadAddedMeasurements();


        loadMeasurementsFromSupabase();


        loadHiddenMeasurements();
    }


    private void initializeAdditionalBodyMeasurements() {

        additionalBodyMeasurements.add(new BodyMeasurement("Грудь", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Шея", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Бедра", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Ширина плеч", 0, R.drawable.ic_biceps));


        additionalBodyMeasurements.add(new BodyMeasurement("Бедро левое", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Бедро правое", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Икра левая", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Икра правая", 0, R.drawable.ic_biceps));


        additionalBodyMeasurements.add(new BodyMeasurement("Предплечье левое", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Предплечье правое", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Запястье левое", 0, R.drawable.ic_biceps));
        additionalBodyMeasurements.add(new BodyMeasurement("Запястье правое", 0, R.drawable.ic_biceps));
    }


    private void loadMeasurementsFromSupabase() {
        repository.getAllLatestMeasurements(new SupabaseBodyMeasurementRepository.MeasurementListCallback() {
            @Override
            public void onSuccess(List<MeasurementRecord> measurements) {
                runOnUiThread(() -> {
                    updateBodyMeasurementsFromSupabase(measurements);
                    setupBodyMeasurementViews();
                    Log.d(TAG, "Загружено " + measurements.size() + " замеров из Supabase");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Ошибка загрузки замеров: " + error);

                    loadBodyMeasurements();
                    setupBodyMeasurementViews();
                });
            }
        });
    }


    private void updateBodyMeasurementsFromSupabase(List<MeasurementRecord> measurements) {
        for (MeasurementRecord record : measurements) {
            String bodyPartRus = SupabaseBodyMeasurementRepository.convertBodyPartFromApi(
                    getBodyPartFromMeasurement(record)
            );


            for (BodyMeasurement bodyMeasurement : bodyMeasurements) {
                if (bodyMeasurement.getBodyPart().equals(bodyPartRus)) {
                    bodyMeasurement.setCurrentValue(record.getValue());
                    bodyMeasurement.setMeasurementDate(record.getDate());
                    break;
                }
            }
        }
    }


    private String getBodyPartFromMeasurement(MeasurementRecord record) {
        return record.getBodyPart() != null ? record.getBodyPart() : "bicep_left";
    }


    private void setupBodyMeasurementViews() {

        View[] fixedMeasurementViews = {bicepLeftMeasurement, bicepRightMeasurement, waistMeasurementView};

        int visibleFixedCount = 0;
        for (int i = 0; i < Math.min(bodyMeasurements.size(), fixedMeasurementViews.length); i++) {
            BodyMeasurement measurement = bodyMeasurements.get(i);
            if (!measurement.isHidden()) {
                if (visibleFixedCount < fixedMeasurementViews.length) {
                    fixedMeasurementViews[visibleFixedCount].setVisibility(View.VISIBLE);
                    setupSingleMeasurementView(fixedMeasurementViews[visibleFixedCount], measurement, i);
                    visibleFixedCount++;
                }
            }
        }


        for (int i = visibleFixedCount; i < fixedMeasurementViews.length; i++) {
            fixedMeasurementViews[i].setVisibility(View.GONE);
        }


        LinearLayout measurementsContainer = findViewById(R.id.measurementsContainer);
        if (measurementsContainer != null && bodyMeasurements.size() > 3) {

            for (int i = measurementsContainer.getChildCount() - 1; i >= 3; i--) {
                measurementsContainer.removeViewAt(i);
            }


            for (int i = 3; i < bodyMeasurements.size(); i++) {
                BodyMeasurement measurement = bodyMeasurements.get(i);
                if (!measurement.isHidden()) {
                    View dynamicMeasurementView = getLayoutInflater().inflate(R.layout.item_body_measurement, measurementsContainer, false);
                    setupSingleMeasurementView(dynamicMeasurementView, measurement, i);
                    measurementsContainer.addView(dynamicMeasurementView);
                }
            }
        }
    }


    private void setupSingleMeasurementView(View measurementView, BodyMeasurement measurement, int index) {
        if (measurementView == null) return;


        ImageView bodyPartIcon = measurementView.findViewById(R.id.bodyPartIcon);
        TextView bodyPartName = measurementView.findViewById(R.id.bodyPartName);
        TextView lastMeasurementDate = measurementView.findViewById(R.id.lastMeasurementDate);
        TextView currentValue = measurementView.findViewById(R.id.currentValue);
        LinearLayout changeContainer = measurementView.findViewById(R.id.changeContainer);
        ImageView trendIcon = measurementView.findViewById(R.id.trendIcon);
        TextView changeValue = measurementView.findViewById(R.id.changeValue);
        ImageView detailsArrow = measurementView.findViewById(R.id.detailsArrow);


        bodyPartIcon.setImageResource(measurement.getIconResourceId());
        bodyPartName.setText(measurement.getBodyPart());


        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", new Locale("ru", "RU"));
        String dateText = measurement.getMeasurementDate() != null ?
                "Последний замер: " + dateFormat.format(measurement.getMeasurementDate()) :
                "Замеров пока нет";
        lastMeasurementDate.setText(dateText);


        if (measurement.getCurrentValue() > 0) {
            currentValue.setText(String.format(Locale.getDefault(), "%.1f", measurement.getCurrentValue()));
        } else {
            currentValue.setText("--");
        }


        if (measurement.hasChange()) {
            changeContainer.setVisibility(View.VISIBLE);

            float change = measurement.getChange();
            boolean isPositive = measurement.isPositiveTrend();


            trendIcon.setImageResource(isPositive ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
            int trendColor = getResources().getColor(isPositive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark);
            trendIcon.setColorFilter(trendColor);


            String changeText = String.format(Locale.getDefault(), "%+.1f", change);
            changeValue.setText(changeText);
            changeValue.setTextColor(trendColor);
        } else {
            changeContainer.setVisibility(View.GONE);
        }


        measurementView.setOnClickListener(v -> {
            Intent intent = new Intent(this, BodyMeasurementDetailActivity.class);
            intent.putExtra(BodyMeasurementDetailActivity.EXTRA_BODY_PART, measurement.getBodyPart());
            intent.putExtra(BodyMeasurementDetailActivity.EXTRA_ICON_RES_ID, measurement.getIconResourceId());
            intent.putExtra(BodyMeasurementDetailActivity.EXTRA_CURRENT_VALUE, measurement.getCurrentValue());
            startActivityForResult(intent, REQUEST_CODE_BODY_MEASUREMENT_DETAIL);
        });


        measurementView.setOnLongClickListener(v -> {
            showHideShowDialog(measurement, index);
            return true;
        });
    }


    private void showMeasurementInputDialog(BodyMeasurement measurement, int index) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);


        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Введите значение в см");

        if (measurement.getCurrentValue() > 0) {
            input.setText(String.valueOf(measurement.getCurrentValue()));
        }


        TextInputLayout inputLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        inputLayout.addView(input);
        inputLayout.setHint("Замер " + measurement.getBodyPart().toLowerCase() + " (см)");


        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        inputLayout.setPadding(padding, 0, padding, 0);

        builder.setTitle("Новый замер")
                .setMessage("Введите новое значение для " + measurement.getBodyPart().toLowerCase())
                .setView(inputLayout)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String valueStr = input.getText().toString().trim();
                    if (!valueStr.isEmpty()) {
                        try {
                            float newValue = Float.parseFloat(valueStr);
                            updateMeasurement(measurement, newValue, index);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Некорректное значение", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void updateMeasurement(BodyMeasurement measurement, float newValue, int index) {

        if (measurement.getCurrentValue() > 0) {
            measurement.setPreviousValue(measurement.getCurrentValue());
            measurement.setPreviousMeasurementDate(measurement.getMeasurementDate());
        }


        measurement.setCurrentValue(newValue);
        measurement.setMeasurementDate(new Date());


        setupBodyMeasurementViews();


        saveBodyMeasurements();

        Toast.makeText(this, "Замер обновлен", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Обновлен замер " + measurement.getBodyPart() + ": " + newValue);
    }


    private void loadBodyMeasurements() {

        String[] bodyParts = {"bicep_left", "bicep_right", "waist_measurement"};
        String[] bodyPartNames = {"Бицепс левый", "Бицепс правый", "Талия"};
        int[] icons = {R.drawable.ic_biceps, R.drawable.ic_biceps, R.drawable.ic_waist};

        for (int i = 0; i < bodyParts.length; i++) {
            float currentValue = measurementsPrefs.getFloat(bodyParts[i] + "_current", 0);
            float previousValue = measurementsPrefs.getFloat(bodyParts[i] + "_previous", 0);
            long currentDate = measurementsPrefs.getLong(bodyParts[i] + "_date", 0);
            long previousDate = measurementsPrefs.getLong(bodyParts[i] + "_previous_date", 0);

            Date measurementDate = currentDate > 0 ? new Date(currentDate) : new Date();
            Date previousMeasurementDate = previousDate > 0 ? new Date(previousDate) : null;

            BodyMeasurement measurement = new BodyMeasurement(
                    bodyPartNames[i], currentValue, previousValue,
                    measurementDate, previousMeasurementDate, icons[i]
            );

            bodyMeasurements.add(measurement);
        }
    }


    private void saveBodyMeasurements() {
        SharedPreferences.Editor editor = measurementsPrefs.edit();

        String[] bodyParts = {"bicep_left", "bicep_right", "waist_measurement"};

        for (int i = 0; i < bodyMeasurements.size() && i < bodyParts.length; i++) {
            BodyMeasurement measurement = bodyMeasurements.get(i);

            editor.putFloat(bodyParts[i] + "_current", measurement.getCurrentValue());
            editor.putFloat(bodyParts[i] + "_previous", measurement.getPreviousValue());

            if (measurement.getMeasurementDate() != null) {
                editor.putLong(bodyParts[i] + "_date", measurement.getMeasurementDate().getTime());
            }

            if (measurement.getPreviousMeasurementDate() != null) {
                editor.putLong(bodyParts[i] + "_previous_date", measurement.getPreviousMeasurementDate().getTime());
            }
        }

        editor.apply();
    }


    private void initializeViews() {

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        }


        measurementsPrefs = getSharedPreferences(PREFS_BODY_MEASUREMENTS, Context.MODE_PRIVATE);


        weightInput = findViewById(R.id.weightInput);
        heightInput = findViewById(R.id.heightInput);
        targetWeightInput = findViewById(R.id.targetWeightInput);


        weightLayout = findViewById(R.id.weightInput).getParent().getParent() instanceof TextInputLayout ?
                (TextInputLayout) findViewById(R.id.weightInput).getParent().getParent() : null;
        heightLayout = findViewById(R.id.heightInput).getParent().getParent() instanceof TextInputLayout ?
                (TextInputLayout) findViewById(R.id.heightInput).getParent().getParent() : null;
        targetWeightLayout = findViewById(R.id.targetWeightInput).getParent().getParent() instanceof TextInputLayout ?
                (TextInputLayout) findViewById(R.id.targetWeightInput).getParent().getParent() : null;


        setupAutoSaveListeners();


        initializeBodyMeasurements();


        bicepLeftMeasurement = findViewById(R.id.bicepLeftMeasurement);
        bicepRightMeasurement = findViewById(R.id.bicepRightMeasurement);
        waistMeasurementView = findViewById(R.id.waistMeasurement);


        setupBodyMeasurementViews();


        FloatingActionButton addMeasurementFab = findViewById(R.id.addMeasurementFab);
        addMeasurementFab.setOnClickListener(v -> showAdditionalMeasurementsBottomSheet());

    }


    private void loadSavedMeasurements() {

        loadUserProfileData();


        setupBodyMeasurementViews();


    }


    private void setInputValue(TextInputEditText input, float value) {
        if (value > 0) {
            input.setText(String.valueOf(value));
        }
    }

    private void setInputValue(TextInputEditText input, int value) {
        if (value > 0) {
            input.setText(String.valueOf(value));
        }
    }


    private float getFloatValue(TextInputEditText input) {
        try {
            String text = input.getText().toString().trim();
            return text.isEmpty() ? 0 : Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    private int getIntValue(TextInputEditText input) {
        try {
            String text = input.getText().toString().trim();
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    private void loadUserProfileData() {
        SharedPreferences prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE);

        float currentWeight = prefs.getFloat("current_weight", 0);
        float height = prefs.getFloat("height", 0);
        float targetWeight = prefs.getFloat("target_weight", 0);


        if (currentWeight > 0) {
            weightInput.setText(String.format("%.1f", currentWeight));
        }
        if (height > 0) {
            heightInput.setText(String.format("%.0f", height));
        }
        if (targetWeight > 0) {
            targetWeightInput.setText(String.format("%.1f", targetWeight));
        }

        Log.d(TAG, "Данные профиля загружены: вес=" + currentWeight + ", рост=" + height + ", целевой вес=" + targetWeight);
    }


    private void setupAutoSaveListeners() {

        TextWatcher autoSaveWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (autoSaveHandler != null) {
                    autoSaveHandler.removeCallbacks(autoSaveRunnable);
                }
                autoSaveHandler.postDelayed(autoSaveRunnable, 1000);
            }
        };


        weightInput.addTextChangedListener(autoSaveWatcher);
        heightInput.addTextChangedListener(autoSaveWatcher);
        targetWeightInput.addTextChangedListener(autoSaveWatcher);
    }

    private android.os.Handler autoSaveHandler = new android.os.Handler();
    private Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            saveUserProfileData();
        }
    };


    private void saveUserProfileData() {
        if (!validateInputs()) {
            return;
        }

        try {
            float weight = getFloatValue(weightInput);
            float height = getFloatValue(heightInput);
            float targetWeight = getFloatValue(targetWeightInput);


            SharedPreferences prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE);
            float oldWeight = prefs.getFloat("current_weight", 0f);

            SharedPreferences.Editor editor = prefs.edit();

            if (weight > 0) {
                editor.putFloat("current_weight", weight);
            }
            if (height > 0) {
                editor.putFloat("height", height);
            }
            if (targetWeight > 0) {
                editor.putFloat("target_weight", targetWeight);
            }


            if (weight > 0 && height > 0) {
                float bmi = weight / ((height / 100) * (height / 100));
                editor.putFloat("bmi", bmi);
            }

            editor.apply();


            if (weight > 0 && (oldWeight <= 0 || weight != oldWeight)) {
                String note;
                if (oldWeight <= 0) {
                    note = "Первоначальное значение из замеров тела";
                } else {
                    note = "Изменено в замерах тела";
                }


                userWeightViewModel.addWeightRecord(weight, note);


                Intent updateIntent = new Intent("com.martist.vitamove.UPDATE_DASHBOARD");
                updateIntent.putExtra("update_source", "body_measurements");
                sendBroadcast(updateIntent);

                Log.d(TAG, "Вес сохранен: " + oldWeight + " -> " + weight + " (добавлена запись в историю)");
            }


            updateProfileInSupabase(weight, height, targetWeight);

            Log.d(TAG, "Профиль автоматически сохранен: вес=" + weight + ", рост=" + height + ", целевой вес=" + targetWeight);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении профиля: " + e.getMessage(), e);
        }
    }


    private boolean validateInputs() {
        boolean isValid = true;


        String weightStr = weightInput.getText().toString().trim();
        if (!weightStr.isEmpty()) {
            try {
                float weight = Float.parseFloat(weightStr);
                if (weight <= 0 || weight > 500) {
                    if (weightLayout != null)
                        weightLayout.setError("Введите корректный вес (1-500 кг)");
                    isValid = false;
                } else {
                    if (weightLayout != null) weightLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                if (weightLayout != null) weightLayout.setError("Введите числовое значение");
                isValid = false;
            }
        }


        String heightStr = heightInput.getText().toString().trim();
        if (!heightStr.isEmpty()) {
            try {
                float height = Float.parseFloat(heightStr);
                if (height <= 0 || height > 250) {
                    if (heightLayout != null)
                        heightLayout.setError("Введите корректный рост (1-250 см)");
                    isValid = false;
                } else {
                    if (heightLayout != null) heightLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                if (heightLayout != null) heightLayout.setError("Введите числовое значение");
                isValid = false;
            }
        }


        String targetWeightStr = targetWeightInput.getText().toString().trim();
        if (!targetWeightStr.isEmpty()) {
            try {
                float targetWeight = Float.parseFloat(targetWeightStr);
                if (targetWeight <= 0 || targetWeight > 500) {
                    if (targetWeightLayout != null)
                        targetWeightLayout.setError("Введите корректный целевой вес (1-500 кг)");
                    isValid = false;
                } else {
                    if (targetWeightLayout != null) targetWeightLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                if (targetWeightLayout != null)
                    targetWeightLayout.setError("Введите числовое значение");
                isValid = false;
            }
        }

        return isValid;
    }


    private void updateProfileInSupabase(float weight, float height, float targetWeight) {

        Log.d(TAG, "Планируется обновление профиля в Supabase");
    }


    private void showAdditionalMeasurementsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_additional_measurements, null);
        bottomSheetDialog.setContentView(bottomSheetView);


        LinearLayout additionalMeasurementsContainer = bottomSheetView.findViewById(R.id.additionalMeasurementsContainer);
        ImageView closeButton = bottomSheetView.findViewById(R.id.closeButton);


        populateAdditionalAndHiddenMeasurements(additionalMeasurementsContainer, bottomSheetDialog);


        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();

        Log.d(TAG, "Открыт BottomSheet с дополнительными и скрытыми замерами");
    }


    private void populateAdditionalAndHiddenMeasurements(LinearLayout container, BottomSheetDialog dialog) {
        container.removeAllViews();


        boolean hasHiddenMeasurements = false;
        for (BodyMeasurement measurement : bodyMeasurements) {
            if (measurement.isHidden()) {
                if (!hasHiddenMeasurements) {

                    TextView hiddenHeader = new TextView(this);
                    hiddenHeader.setText("💫 Скрытые замеры");
                    hiddenHeader.setTextSize(16);
                    hiddenHeader.setTextColor(getResources().getColor(R.color.profile_text_primary));
                    hiddenHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    hiddenHeader.setPadding(16, 16, 16, 8);
                    container.addView(hiddenHeader);
                    hasHiddenMeasurements = true;
                }

                addMeasurementToBottomSheet(container, measurement, dialog, true);
            }
        }


        boolean hasAdditionalMeasurements = false;
        for (BodyMeasurement measurement : additionalBodyMeasurements) {
            if (!isBodyPartAlreadyAdded(measurement.getBodyPart())) {
                if (!hasAdditionalMeasurements) {

                    TextView additionalHeader = new TextView(this);
                    additionalHeader.setText("➕ Дополнительные замеры");
                    additionalHeader.setTextSize(16);
                    additionalHeader.setTextColor(getResources().getColor(R.color.profile_text_primary));
                    additionalHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    additionalHeader.setPadding(16, hasHiddenMeasurements ? 32 : 16, 16, 8);
                    container.addView(additionalHeader);
                    hasAdditionalMeasurements = true;
                }

                addMeasurementToBottomSheet(container, measurement, dialog, false);
            }
        }


        if (!hasHiddenMeasurements && !hasAdditionalMeasurements) {
            TextView noMeasurementsText = new TextView(this);
            noMeasurementsText.setText("✨ Все возможные замеры уже добавлены!");
            noMeasurementsText.setTextColor(getResources().getColor(R.color.profile_text_secondary));
            noMeasurementsText.setPadding(16, 32, 16, 16);
            noMeasurementsText.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            container.addView(noMeasurementsText);
        }
    }


    private void addMeasurementToBottomSheet(LinearLayout container, BodyMeasurement measurement,
                                             BottomSheetDialog dialog, boolean isHiddenMeasurement) {
        View measurementView = getLayoutInflater().inflate(R.layout.item_additional_measurement, container, false);

        ImageView bodyPartIcon = measurementView.findViewById(R.id.bodyPartIcon);
        TextView bodyPartName = measurementView.findViewById(R.id.bodyPartName);
        TextView bodyPartDescription = measurementView.findViewById(R.id.bodyPartDescription);
        MaterialButton addButton = measurementView.findViewById(R.id.addButton);


        bodyPartIcon.setImageResource(measurement.getIconResourceId());
        bodyPartName.setText(measurement.getBodyPart());
        bodyPartDescription.setText(getDescriptionForBodyPart(measurement.getBodyPart()));

        if (isHiddenMeasurement) {

            addButton.setText("Показать");
            addButton.setOnClickListener(v -> {
                measurement.setHidden(false);
                setupBodyMeasurementViews();
                saveHiddenMeasurements();
                Toast.makeText(this, "Замер \"" + measurement.getBodyPart() + "\" показан", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        } else {

            addButton.setText("Добавить");
            addButton.setOnClickListener(v -> {
                addBodyMeasurementToMain(measurement);
                dialog.dismiss();
            });
        }

        container.addView(measurementView);
    }


    private String getDescriptionForBodyPart(String bodyPart) {
        switch (bodyPart) {
            case "Грудь":
                return "Окружность груди на уровне сосков";
            case "Шея":
                return "Окружность шеи под кадыком";
            case "Бедра":
                return "Окружность бедер в самом широком месте";
            case "Ширина плеч":
                return "Расстояние между плечевыми суставами";
            case "Бедро левое":
                return "Окружность левого бедра в верхней части";
            case "Бедро правое":
                return "Окружность правого бедра в верхней части";
            case "Икра левая":
                return "Окружность левой икры в самом широком месте";
            case "Икра правая":
                return "Окружность правой икры в самом широком месте";
            case "Предплечье левое":
                return "Окружность левого предплечья";
            case "Предплечье правое":
                return "Окружность правого предплечья";
            case "Запястье левое":
                return "Окружность левого запястья";
            case "Запястье правое":
                return "Окружность правого запястья";
            default:
                return "Измерение окружности части тела";
        }
    }


    private boolean isBodyPartAlreadyAdded(String bodyPart) {
        for (BodyMeasurement measurement : bodyMeasurements) {
            if (measurement.getBodyPart().equals(bodyPart)) {
                return true;
            }
        }
        return false;
    }


    private void loadAddedMeasurements() {
        SharedPreferences prefs = getSharedPreferences(PREFS_BODY_MEASUREMENTS, Context.MODE_PRIVATE);
        String addedMeasurementsStr = prefs.getString("added_measurements", "");

        if (!addedMeasurementsStr.isEmpty()) {
            String[] addedParts = addedMeasurementsStr.split(",");
            for (String bodyPart : addedParts) {
                if (!bodyPart.trim().isEmpty() && !isBodyPartAlreadyAdded(bodyPart.trim())) {

                    for (BodyMeasurement additionalMeasurement : additionalBodyMeasurements) {
                        if (additionalMeasurement.getBodyPart().equals(bodyPart.trim())) {
                            BodyMeasurement newMeasurement = new BodyMeasurement(
                                    additionalMeasurement.getBodyPart(),
                                    0,
                                    additionalMeasurement.getIconResourceId()
                            );
                            bodyMeasurements.add(newMeasurement);
                            Log.d(TAG, "Загружен ранее добавленный замер: " + bodyPart.trim());
                            break;
                        }
                    }
                }
            }
        }
    }


    private void saveAddedMeasurements() {
        SharedPreferences.Editor editor = measurementsPrefs.edit();


        StringBuilder addedMeasurementsStr = new StringBuilder();
        for (int i = 3; i < bodyMeasurements.size(); i++) {
            if (addedMeasurementsStr.length() > 0) {
                addedMeasurementsStr.append(",");
            }
            addedMeasurementsStr.append(bodyMeasurements.get(i).getBodyPart());
        }

        editor.putString("added_measurements", addedMeasurementsStr.toString());
        editor.apply();

        Log.d(TAG, "Сохранены добавленные замеры: " + addedMeasurementsStr.toString());
    }


    private void addBodyMeasurementToMain(BodyMeasurement measurement) {

        BodyMeasurement newMeasurement = new BodyMeasurement(
                measurement.getBodyPart(),
                0,
                measurement.getIconResourceId()
        );

        bodyMeasurements.add(newMeasurement);


        setupBodyMeasurementViews();


        saveBodyMeasurements();


        saveAddedMeasurements();


        saveHiddenMeasurements();

        Toast.makeText(this, measurement.getBodyPart() + " добавлен в список замеров", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Добавлен новый замер: " + measurement.getBodyPart());
    }


    private void showHideShowDialog(BodyMeasurement measurement, int index) {

        if (!measurement.isHidden() && isDefaultMeasurement(measurement.getBodyPart())) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Основной замер")
                    .setMessage("\"" + measurement.getBodyPart() + "\" — это основной замер тела, который рекомендуется отслеживать постоянно.\n\nВы уверены, что хотите его скрыть?")
                    .setPositiveButton("Да, скрыть", (dialog, which) -> {
                        toggleMeasurementVisibility(measurement, index);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            return;
        }


        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

        String title = measurement.isHidden() ? "Показать замер" : "Скрыть замер";
        String message = measurement.isHidden() ?
                "Хотите показать замер \"" + measurement.getBodyPart() + "\"?" :
                "Хотите скрыть замер \"" + measurement.getBodyPart() + "\"?";
        String positiveButton = measurement.isHidden() ? "Показать" : "Скрыть";

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    toggleMeasurementVisibility(measurement, index);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void toggleMeasurementVisibility(BodyMeasurement measurement, int index) {
        measurement.setHidden(!measurement.isHidden());


        setupBodyMeasurementViews();


        saveHiddenMeasurements();

        String action = measurement.isHidden() ? "скрыт" : "показан";
        Toast.makeText(this, "Замер \"" + measurement.getBodyPart() + "\" " + action, Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Замер \"" + measurement.getBodyPart() + "\" " + action);
    }


    private void saveHiddenMeasurements() {
        SharedPreferences.Editor editor = measurementsPrefs.edit();

        StringBuilder hiddenMeasurements = new StringBuilder();
        for (BodyMeasurement measurement : bodyMeasurements) {
            if (measurement.isHidden()) {
                if (hiddenMeasurements.length() > 0) {
                    hiddenMeasurements.append(",");
                }
                hiddenMeasurements.append(measurement.getBodyPart());
            }
        }

        editor.putString("hidden_measurements", hiddenMeasurements.toString());
        editor.apply();

        Log.d(TAG, "Сохранены скрытые замеры: " + hiddenMeasurements.toString());
    }


    private void loadHiddenMeasurements() {
        String hiddenMeasurementsStr = measurementsPrefs.getString("hidden_measurements", "");

        if (!hiddenMeasurementsStr.isEmpty()) {
            String[] hiddenParts = hiddenMeasurementsStr.split(",");
            for (String hiddenPart : hiddenParts) {
                if (!hiddenPart.trim().isEmpty()) {

                    if (!isDefaultMeasurement(hiddenPart.trim())) {

                        for (BodyMeasurement measurement : bodyMeasurements) {
                            if (measurement.getBodyPart().equals(hiddenPart.trim())) {
                                measurement.setHidden(true);
                                break;
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Загружены скрытые замеры (исключая основные): " + hiddenMeasurementsStr);
    }


    private boolean isDefaultMeasurement(String bodyPart) {
        for (String defaultMeasurement : DEFAULT_MEASUREMENTS) {
            if (defaultMeasurement.equals(bodyPart)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
