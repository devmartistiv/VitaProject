package com.martist.vitamove.workout.data.managers;

import android.content.Context;
import android.util.Log;

import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.workout.data.model.WorkoutPlan;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class WorkoutScheduleManager {
    private static final String TAG = "WorkoutScheduleManager";
    private static WorkoutScheduleManager instance;
    private final WorkoutRepository workoutRepository;
    private final Context context;
    private final Executor executor;


    private WorkoutScheduleManager(Context context) {
        this.context = context.getApplicationContext();
        SupabaseClient supabaseClient = SupabaseClient.getInstance(null, null);
        this.workoutRepository = new SupabaseWorkoutRepository(supabaseClient);
        this.executor = Executors.newSingleThreadExecutor();
    }


    public static synchronized WorkoutScheduleManager getInstance(Context context) {
        if (instance == null) {
            instance = new WorkoutScheduleManager(context);
        }
        return instance;
    }


    public WorkoutPlan getTodayWorkoutPlan(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "getTodayWorkoutPlan: userId is null or empty.");
            return null;
        }
        try {
            return workoutRepository.getTodayWorkoutPlan(userId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении плана на сегодня: " + e.getMessage(), e);
            return null;
        }
    }


}