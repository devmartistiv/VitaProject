package com.martist.vitamove.exercise.data.managers;

import android.os.CountDownTimer;
import android.util.Log;


public class ExerciseTimerManager {
    private static final String TAG = "ExerciseTimerManager";

    private CountDownTimer restTimer;
    private CountDownTimer activeSetTimer;
    private long activeSetStartTime = 0;
    private long activeSetDuration = 0;

    private TimerUpdateListener listener;


    public interface TimerUpdateListener {
        void onRestTimerUpdate(long remainingMillis);

        void onRestTimerFinish();

        void onActiveSetTimerUpdate(long elapsedMillis);
    }

    public ExerciseTimerManager(TimerUpdateListener listener) {
        this.listener = listener;
    }


    public void startRestTimer(long durationMillis) {
        cancelRestTimer();

        restTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (listener != null) {
                    listener.onRestTimerUpdate(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                if (listener != null) {
                    listener.onRestTimerFinish();
                }
            }
        };

        restTimer.start();
        Log.d(TAG, "startRestTimer: Запущен таймер отдыха на " + durationMillis + " мс");
    }


    public void cancelRestTimer() {
        if (restTimer != null) {
            restTimer.cancel();
            restTimer = null;
            Log.d(TAG, "cancelRestTimer: Таймер отдыха остановлен");
        }
    }


    public void startActiveSetTimer() {
        cancelActiveSetTimer();

        activeSetStartTime = System.currentTimeMillis();

        activeSetTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedMillis = System.currentTimeMillis() - activeSetStartTime;
                activeSetDuration = elapsedMillis;

                if (listener != null) {
                    listener.onActiveSetTimerUpdate(elapsedMillis);
                }
            }

            @Override
            public void onFinish() {

            }
        };

        activeSetTimer.start();
        Log.d(TAG, "startActiveSetTimer: Запущен таймер активного подхода");
    }


    public void cancelActiveSetTimer() {
        if (activeSetTimer != null) {
            activeSetTimer.cancel();
            activeSetTimer = null;
            Log.d(TAG, "cancelActiveSetTimer: Таймер активного подхода остановлен");
        }
    }


    public void resumeActiveSetTimer(long savedDurationMillis) {
        cancelActiveSetTimer();

        activeSetStartTime = System.currentTimeMillis() - savedDurationMillis;
        activeSetDuration = savedDurationMillis;

        activeSetTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedMillis = System.currentTimeMillis() - activeSetStartTime;
                activeSetDuration = elapsedMillis;

                if (listener != null) {
                    listener.onActiveSetTimerUpdate(elapsedMillis);
                }
            }

            @Override
            public void onFinish() {

            }
        };

        activeSetTimer.start();
        Log.d(TAG, "resumeActiveSetTimer: Возобновлен таймер с " + savedDurationMillis + " мс");
    }


    public long getActiveSetDuration() {
        return activeSetDuration;
    }


    public long getActiveSetStartTime() {
        return activeSetStartTime;
    }


    public void setActiveSetStartTime(long startTime) {
        this.activeSetStartTime = startTime;
    }


    public void setActiveSetDuration(long duration) {
        this.activeSetDuration = duration;
    }


    public void cancelAllTimers() {
        cancelRestTimer();
        cancelActiveSetTimer();
        Log.d(TAG, "cancelAllTimers: Все таймеры остановлены");
    }


    public boolean isRestTimerActive() {
        return restTimer != null;
    }


    public boolean isActiveSetTimerActive() {
        return activeSetTimer != null;
    }


    public void cleanup() {
        cancelAllTimers();
        listener = null;
        Log.d(TAG, "cleanup: Ресурсы освобождены");
    }
}

