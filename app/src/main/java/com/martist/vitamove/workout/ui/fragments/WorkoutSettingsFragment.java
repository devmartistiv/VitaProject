package com.martist.vitamove.workout.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.martist.vitamove.R;
import com.martist.vitamove.workout.data.managers.WorkoutSettingsManager;


public class WorkoutSettingsFragment extends Fragment {
    private static final String TAG = "WorkoutSettingsFragment";


    private static final int MIN_TIMER_SECONDS = 15;
    private static final int MAX_TIMER_SECONDS = 300;
    private static final int TIMER_STEP = 15;


    private MaterialButton backButton;
    private TextView timerSecondsText;
    private MaterialButton decreaseTimerButton;
    private MaterialButton increaseTimerButton;
    private SwitchMaterial autoNextExerciseSwitch;
    private SwitchMaterial restTimerEnabledSwitch;


    private int timerSeconds;
    private boolean autoNextExercise;
    private boolean restTimerEnabled;


    private WorkoutSettingsManager settingsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsManager = WorkoutSettingsManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workout_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        initViews(view);


        loadSettings();


        updateUI();


        setupListeners();
    }


    private void initViews(View view) {
        backButton = view.findViewById(R.id.back_button);
        timerSecondsText = view.findViewById(R.id.timer_seconds_text);
        decreaseTimerButton = view.findViewById(R.id.decrease_timer_button);
        increaseTimerButton = view.findViewById(R.id.increase_timer_button);
        autoNextExerciseSwitch = view.findViewById(R.id.auto_next_exercise_switch);
        restTimerEnabledSwitch = view.findViewById(R.id.rest_timer_enabled_switch);
    }


    private void loadSettings() {
        timerSeconds = settingsManager.getRestTimerSeconds();
        autoNextExercise = settingsManager.isAutoNextExerciseEnabled();
        restTimerEnabled = settingsManager.isRestTimerEnabled();

        Log.d(TAG, "Загружены настройки: timerSeconds=" + timerSeconds +
                ", autoNextExercise=" + autoNextExercise +
                ", restTimerEnabled=" + restTimerEnabled);
    }


    private void updateUI() {
        timerSecondsText.setText(String.valueOf(timerSeconds));
        autoNextExerciseSwitch.setChecked(autoNextExercise);
        restTimerEnabledSwitch.setChecked(restTimerEnabled);


        boolean timerControlsEnabled = restTimerEnabled;
        decreaseTimerButton.setEnabled(timerControlsEnabled);
        increaseTimerButton.setEnabled(timerControlsEnabled);
        timerSecondsText.setAlpha(timerControlsEnabled ? 1.0f : 0.5f);
    }


    private void setupListeners() {

        backButton.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });


        decreaseTimerButton.setOnClickListener(v -> {
            if (timerSeconds > MIN_TIMER_SECONDS) {
                timerSeconds -= TIMER_STEP;
                timerSecondsText.setText(String.valueOf(timerSeconds));
                saveSettings();
                animateButtonClick(v);
                showTimerChangedToast();
            }
        });


        increaseTimerButton.setOnClickListener(v -> {
            if (timerSeconds < MAX_TIMER_SECONDS) {
                timerSeconds += TIMER_STEP;
                timerSecondsText.setText(String.valueOf(timerSeconds));
                saveSettings();
                animateButtonClick(v);
                showTimerChangedToast();
            }
        });


        autoNextExerciseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoNextExercise = isChecked;
            saveSettings();
            showAutoNextChangedToast(isChecked);
        });


        restTimerEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            restTimerEnabled = isChecked;
            saveSettings();
            updateUI();
            showRestTimerEnabledToast(isChecked);
        });
    }


    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start())
                .start();
    }


    private void saveSettings() {
        settingsManager.setRestTimerSeconds(timerSeconds);
        settingsManager.setAutoNextExerciseEnabled(autoNextExercise);
        settingsManager.setRestTimerEnabled(restTimerEnabled);

        Log.d(TAG, "Сохранены настройки: timerSeconds=" + timerSeconds +
                ", autoNextExercise=" + autoNextExercise +
                ", restTimerEnabled=" + restTimerEnabled);
    }


    private void showTimerChangedToast() {
        Toast.makeText(requireContext(),
                "Таймер отдыха установлен на " + timerSeconds + " сек",
                Toast.LENGTH_SHORT).show();
    }


    private void showAutoNextChangedToast(boolean isEnabled) {
        Toast.makeText(requireContext(),
                isEnabled ? "Автопереход включен" : "Автопереход выключен",
                Toast.LENGTH_SHORT).show();
    }


    private void showRestTimerEnabledToast(boolean isEnabled) {
        Toast.makeText(requireContext(),
                isEnabled ? "Таймер отдыха включен" : "Таймер отдыха выключен",
                Toast.LENGTH_SHORT).show();
    }
} 