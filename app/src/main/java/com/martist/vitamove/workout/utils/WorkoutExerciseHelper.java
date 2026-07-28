package com.martist.vitamove.workout.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.martist.vitamove.exercise.data.managers.ExerciseManager;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.programs.data.ProgramManager;
import com.martist.vitamove.programs.ui.model.ProgramExercise;


public class WorkoutExerciseHelper {
    private static final String TAG = "WorkoutExerciseHelper";

    private final Context context;
    private final ProgramManager programManager;
    private final ExerciseManager exerciseManager;

    public WorkoutExerciseHelper(Context context) {
        this.context = context;
        this.programManager = new ProgramManager(context);
        this.exerciseManager = new ExerciseManager(context);
    }


    public void showExerciseInfoDialog(ProgramExercise exercise) {
        try {

            String exerciseId = exercise.getExerciseId();

            exerciseManager.getExerciseByIdAsync(exerciseId, new ExerciseManager.AsyncCallback<Exercise>() {
                @Override
                public void onSuccess(Exercise result) {
                    displayExerciseDialog(exercise, result);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Ошибка при загрузке базового упражнения: " + e.getMessage(), e);
                    displayExerciseDialog(exercise, null);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка: " + e.getMessage(), e);
            Toast.makeText(context,
                    "Ошибка при отображении информации об упражнении: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void displayExerciseDialog(ProgramExercise exercise, Exercise baseExercise) {
        try {
            String exerciseName = baseExercise != null ? baseExercise.getName() : "Упражнение";

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(exerciseName);


            StringBuilder message = new StringBuilder();
            message.append("Подходы: ").append(exercise.getTargetSets()).append("\n\n");
            message.append("Повторения: ").append(exercise.getTargetReps()).append("\n\n");

            if (exercise.getTargetWeight() > 0) {
                message.append("Вес: ").append(exercise.getTargetWeight()).append(" кг\n\n");
            }

            if (exercise.getNotes() != null && !exercise.getNotes().isEmpty()) {
                message.append("Заметки: \n").append(exercise.getNotes());
            }

            builder.setMessage(message.toString());
            builder.setPositiveButton("Закрыть", (dialog, which) -> dialog.dismiss());

            builder.create().show();

            Log.d(TAG, "Диалог отображен для упражнения " + exerciseName);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отображении диалога: " + e.getMessage(), e);
            Toast.makeText(context,
                    "Ошибка при отображении диалога: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }


    public interface OnExerciseDeletedListener {
        void onExerciseDeleted();
    }
} 