package com.martist.vitamove.exercise.data.managers;

import android.util.Log;

import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.set.CardioSetAdapter;
import com.martist.vitamove.set.ExerciseSetAdapter;
import com.martist.vitamove.set.RepsOnlySetAdapter;


public class ExerciseAdapterFactory {
    private static final String TAG = "ExerciseAdapterFactory";


    public interface AdapterEventListener {
        void onSetClick(ExerciseSet set, int position, boolean isCompleted);

        void onDeleteClick(ExerciseSet set, int position);

        void onDataChange(ExerciseSet set, int position);
    }


    public static CardioSetAdapter createCardioAdapter(AdapterEventListener listener) {
        Log.d(TAG, "createCardioAdapter: Создание адаптера для кардио");

        CardioSetAdapter adapter = new CardioSetAdapter();

        if (listener != null) {
            adapter.setOnSetClickListener(listener::onSetClick);
            adapter.setOnDeleteClickListener(listener::onDeleteClick);
            adapter.setOnDataChangeListener((set, position) -> {

                if (set != null && set.getId() != null && !set.getId().isEmpty()) {
                    listener.onDataChange(set, position);
                } else {
                    Log.d(TAG, "createCardioAdapter: Пропуск обновления - невалидный подход");
                }
            });
        }

        return adapter;
    }


    public static RepsOnlySetAdapter createRepsOnlyAdapter(AdapterEventListener listener) {
        Log.d(TAG, "createRepsOnlyAdapter: Создание адаптера для упражнений с повторениями");

        RepsOnlySetAdapter adapter = new RepsOnlySetAdapter();

        if (listener != null) {
            adapter.setOnSetClickListener(listener::onSetClick);
            adapter.setOnDeleteClickListener(listener::onDeleteClick);
            adapter.setOnDataChangeListener((set, position) -> {

                if (set != null && set.getId() != null && !set.getId().isEmpty()) {
                    listener.onDataChange(set, position);
                } else {
                    Log.d(TAG, "createRepsOnlyAdapter: Пропуск обновления - невалидный подход");
                }
            });
        }

        return adapter;
    }


    public static ExerciseSetAdapter createRegularAdapter(AdapterEventListener listener) {
        Log.d(TAG, "createRegularAdapter: Создание адаптера для силовых упражнений");

        ExerciseSetAdapter adapter = new ExerciseSetAdapter();

        if (listener != null) {
            adapter.setOnSetClickListener(listener::onSetClick);
            adapter.setOnDeleteClickListener(listener::onDeleteClick);
            adapter.setOnDataChangeListener((set, position) -> {

                if (set != null && set.getId() != null && !set.getId().isEmpty()) {
                    listener.onDataChange(set, position);
                } else {
                    Log.d(TAG, "createRegularAdapter: Пропуск обновления - невалидный подход");
                }
            });
        }

        return adapter;
    }


    public static Object createAdapterForExercise(Exercise exercise, AdapterEventListener listener) {
        if (exercise == null) {
            Log.e(TAG, "createAdapterForExercise: exercise is null");
            return null;
        }


        if (exercise.isCardioExercise() || exercise.isStaticExercise()) {
            return createCardioAdapter(listener);
        }


        if (exercise.usesRepsOnly()) {
            return createRepsOnlyAdapter(listener);
        }


        return createRegularAdapter(listener);
    }


    public static AdapterType getAdapterType(Exercise exercise) {
        if (exercise == null) {
            return AdapterType.UNKNOWN;
        }

        if (exercise.isCardioExercise() || exercise.isStaticExercise()) {
            return AdapterType.CARDIO;
        }

        if (exercise.usesRepsOnly()) {
            return AdapterType.REPS_ONLY;
        }

        return AdapterType.REGULAR;
    }


    public enum AdapterType {
        CARDIO,
        REPS_ONLY,
        REGULAR,
        UNKNOWN
    }
}

