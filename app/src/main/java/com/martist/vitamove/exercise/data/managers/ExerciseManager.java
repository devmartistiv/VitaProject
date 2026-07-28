package com.martist.vitamove.exercise.data.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ExerciseManager {
    private static final String TAG = "ExerciseManager";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFqb3BiZGlhZmdiYnN0a3dtaHB0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MDgyNjkzNzYsImV4cCI6MjAyMzg0NTM3Nn0.bHAKqkF0OwsEZgPZRVXGsJME09LZBDU0vDY4YjsCPmE";

    private final WorkoutRepository repository;
    private final Executor executor;
    private final Handler mainHandler;
    private final SupabaseClient supabaseClient;


    public interface AsyncCallback<T> {
        void onSuccess(T result);

        void onFailure(Exception e);
    }


    public ExerciseManager(Context context) {

        supabaseClient = SupabaseClient.getInstance(SupabaseClient.SUPABASE_URL, SUPABASE_API_KEY);


        SharedPreferences prefs = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        String userToken = prefs.getString("accessToken", null);


        if (userToken != null && !userToken.isEmpty()) {
            supabaseClient.setUserToken(userToken);
        }


        this.repository = new SupabaseWorkoutRepository(supabaseClient);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());


    }


    public Exercise getExerciseById(String id) throws Exception {
        return repository.getExerciseById(id);
    }


    public void getExerciseByIdAsync(String id, AsyncCallback<Exercise> callback) {
        executor.execute(() -> {
            try {
                Exercise exercise = repository.getExerciseById(id);
                mainHandler.post(() -> callback.onSuccess(exercise));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении упражнения с ID: " + id, e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


}