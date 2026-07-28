package com.martist.vitamove.workout.ui.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.xml.KonfettiView;


public class WorkoutSummaryDialogFragment extends DialogFragment {

    private static final String TAG = "WorkoutSummaryDialog";


    private static final String ARG_WORKOUT_DURATION = "workout_duration";
    private static final String ARG_WORKOUT_CALORIES = "workout_calories";
    private static final String ARG_WORKOUT_TONNAGE = "workout_tonnage";
    private static final String ARG_WORKOUT_NAME = "workout_name";


    private KonfettiView konfettiView;
    private TextView motivationalTitle;
    private TextView workoutDurationValue;
    private TextView workoutTonnageValue;
    private TextView workoutCaloriesValue;
    private MaterialButton doneButton;

    private Party party;


    private final String[] motivationalMessages = {
            "Отличная работа! 💪",
            "Вы сделали это! 🎉",
            "Супер тренировка! 🔥",
            "Вы становитесь сильнее! ⚡",
            "Продолжайте в том же духе! 🚀",
            "Невероятный результат! ⭐"
    };


    public static WorkoutSummaryDialogFragment newInstance(long duration, int calories, int tonnage, String name) {
        WorkoutSummaryDialogFragment fragment = new WorkoutSummaryDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_WORKOUT_DURATION, duration);
        args.putInt(ARG_WORKOUT_CALORIES, calories);
        args.putInt(ARG_WORKOUT_TONNAGE, tonnage);
        args.putString(ARG_WORKOUT_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);


        Window window = dialog.getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();


            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);


            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.7f;
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workout_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        loadWorkoutData();
        setupClickListeners();
        startConfettiAnimation();

        Log.d(TAG, "WorkoutSummaryDialogFragment создан успешно");
    }


    private void initializeViews(View view) {
        konfettiView = view.findViewById(R.id.konfettiView);
        motivationalTitle = view.findViewById(R.id.motivationalTitle);
        workoutDurationValue = view.findViewById(R.id.workoutDurationValue);
        workoutTonnageValue = view.findViewById(R.id.workoutTonnageValue);
        workoutCaloriesValue = view.findViewById(R.id.workoutCaloriesValue);
        doneButton = view.findViewById(R.id.doneButton);
    }


    private void loadWorkoutData() {
        if (getArguments() == null) {
            Log.e(TAG, "Аргументы отсутствуют");
            return;
        }


        long durationMillis = getArguments().getLong(ARG_WORKOUT_DURATION, 0);
        int calories = getArguments().getInt(ARG_WORKOUT_CALORIES, 0);
        int tonnage = getArguments().getInt(ARG_WORKOUT_TONNAGE, 0);
        String workoutName = getArguments().getString(ARG_WORKOUT_NAME, "Тренировка");

        Log.d(TAG, String.format("Загружены данные: duration=%d, calories=%d, tonnage=%d, name=%s",
                durationMillis, calories, tonnage, workoutName));


        setRandomMotivationalMessage();


        displayDuration(durationMillis);
        displayTonnage(tonnage);
        displayCalories(calories);
    }


    private void setRandomMotivationalMessage() {
        Random random = new Random();
        int index = random.nextInt(motivationalMessages.length);
        motivationalTitle.setText(motivationalMessages[index]);
    }


    private void displayDuration(long durationMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;

        String formattedDuration;
        if (hours > 0) {
            formattedDuration = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            formattedDuration = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }

        workoutDurationValue.setText(formattedDuration);
        Log.d(TAG, "Отображено время: " + formattedDuration);
    }


    private void displayTonnage(int tonnage) {
        String formattedTonnage = String.format(Locale.getDefault(), "%,d", tonnage);
        workoutTonnageValue.setText(formattedTonnage);
        Log.d(TAG, "Отображен тоннаж: " + formattedTonnage + " кг");
    }


    private void displayCalories(int calories) {
        String formattedCalories = String.format(Locale.getDefault(), "%,d", calories);
        workoutCaloriesValue.setText(formattedCalories);
        Log.d(TAG, "Отображено калорий: " + formattedCalories);
    }


    private void setupClickListeners() {
        doneButton.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка 'Готово', закрытие диалога");
            dismiss();
        });
    }


    private void startConfettiAnimation() {
        if (getContext() == null) return;


        List<Integer> colors = Arrays.asList(
                ContextCompat.getColor(requireContext(), R.color.orange_500),
                ContextCompat.getColor(requireContext(), R.color.green_500),
                ContextCompat.getColor(requireContext(), R.color.purple_500),
                ContextCompat.getColor(requireContext(), R.color.teal_200),
                Color.YELLOW,
                Color.RED
        );


        EmitterConfig emitterConfig = new Emitter(7L, TimeUnit.SECONDS).perSecond(50);


        party = new PartyFactory(emitterConfig)
                .angle(270)
                .spread(45)
                .setSpeedBetween(30f, 60f)
                .timeToLive(3000L)
                .shapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                .sizes(new Size(8, 5f, 0.2f))
                .position(0.0, 0.0, 1.0, 0.0)
                .colors(colors)
                .build();


        konfettiView.start(party);

        Log.d(TAG, "Анимация конфетти запущена");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (konfettiView != null && party != null) {
            konfettiView.stop(party);
        }
        Log.d(TAG, "WorkoutSummaryDialogFragment уничтожен");
    }
}

