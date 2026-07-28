package com.martist.vitamove.exercise.data.managers;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.Exercise;


public class ExerciseUIStateManager {


    public enum UIState {
        RESTING,
        ACTIVE_SET,
        IDLE,
        WARMUP_COMPLETED,
        WARMUP_ACTIVE
    }

    private final View restTimerContainer;
    private final View activeSetTimerContainer;
    private final MaterialButton completeSetButton;
    private final MaterialButton startSetButton;
    private final View setsList;

    public ExerciseUIStateManager(
            View restTimerContainer,
            View activeSetTimerContainer,
            MaterialButton completeSetButton,
            MaterialButton startSetButton,
            View setsList) {
        this.restTimerContainer = restTimerContainer;
        this.activeSetTimerContainer = activeSetTimerContainer;
        this.completeSetButton = completeSetButton;
        this.startSetButton = startSetButton;
        this.setsList = setsList;
    }


    public void applyState(UIState state) {
        switch (state) {
            case RESTING:
                showRestingState();
                break;
            case ACTIVE_SET:
                showActiveSetState();
                break;
            case IDLE:
                showIdleState();
                break;
            case WARMUP_COMPLETED:
                showWarmupCompletedState();
                break;
            case WARMUP_ACTIVE:
                showWarmupActiveState();
                break;
        }
    }


    private void showRestingState() {
        restTimerContainer.setVisibility(View.VISIBLE);
        completeSetButton.setVisibility(View.GONE);
        startSetButton.setVisibility(View.GONE);
        activeSetTimerContainer.setVisibility(View.GONE);
    }


    private void showActiveSetState() {
        restTimerContainer.setVisibility(View.GONE);
        completeSetButton.setVisibility(View.VISIBLE);
        startSetButton.setVisibility(View.GONE);
        activeSetTimerContainer.setVisibility(View.VISIBLE);
    }


    private void showIdleState() {
        restTimerContainer.setVisibility(View.GONE);
        completeSetButton.setVisibility(View.GONE);
        activeSetTimerContainer.setVisibility(View.GONE);
        startSetButton.setVisibility(View.VISIBLE);
    }


    private void showWarmupCompletedState() {
        setsList.setVisibility(View.GONE);
        restTimerContainer.setVisibility(View.GONE);
        activeSetTimerContainer.setVisibility(View.GONE);
        startSetButton.setVisibility(View.GONE);

        completeSetButton.setVisibility(View.VISIBLE);
        completeSetButton.setText("ВЫПОЛНЕНО");
        completeSetButton.setClickable(false);
        completeSetButton.setBackgroundTintList(
                ColorStateList.valueOf(completeSetButton.getContext().getResources().getColor(R.color.green_500)));
        completeSetButton.setIconResource(R.drawable.ic_check);
        completeSetButton.setIconTint(ColorStateList.valueOf(Color.WHITE));
        completeSetButton.setIconGravity(MaterialButton.ICON_GRAVITY_START);
        completeSetButton.setIconPadding(16);
    }


    private void showWarmupActiveState() {
        setsList.setVisibility(View.GONE);
        restTimerContainer.setVisibility(View.GONE);
        activeSetTimerContainer.setVisibility(View.GONE);
        startSetButton.setVisibility(View.GONE);

        completeSetButton.setVisibility(View.VISIBLE);
        completeSetButton.setText("ЗАВЕРШИТЬ УПРАЖНЕНИЕ");
        completeSetButton.setClickable(true);
        completeSetButton.setBackgroundTintList(
                ColorStateList.valueOf(completeSetButton.getContext().getResources().getColor(R.color.orange_500)));
        completeSetButton.setIcon(null);
    }


    public void setStartButtonText(Exercise exercise, boolean isCardio) {
        if (isCardio) {
            if (exercise.isStaticExercise()) {
                startSetButton.setText("НАЧАТЬ УДЕРЖАНИЕ");
            } else {
                startSetButton.setText("НАЧАТЬ КАРДИО");
            }
        } else {
            startSetButton.setText("НАЧАТЬ ПОДХОД");
        }
    }


    public void setStartButtonVisible(boolean visible) {
        startSetButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }


    public void setSetsListVisible(boolean visible) {
        setsList.setVisibility(visible ? View.VISIBLE : View.GONE);
    }


    public void invalidateStartButton() {
        startSetButton.invalidate();
    }
}

