package com.martist.vitamove.programs.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.Exercise;

import java.util.ArrayList;
import java.util.List;

public class DayExerciseListAdapter extends RecyclerView.Adapter<DayExerciseListAdapter.ExerciseViewHolder> {

    private List<Exercise> exercises = new ArrayList<>();
    private final OnExerciseActionListener listener;


    public interface OnExerciseActionListener {

        void onReplaceExerciseClicked(int position, String exerciseId);
    }


    public DayExerciseListAdapter(OnExerciseActionListener listener) {
        this.listener = listener;
    }


    public void setExercises(List<Exercise> newExercises) {
        this.exercises = newExercises != null ? newExercises : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_exercise, parent, false);

        return new ExerciseViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {

        if (position >= 0 && position < exercises.size()) {
            Exercise exercise = exercises.get(position);
            holder.bind(exercise);
        }
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        ImageView replaceIcon;
        private String currentExerciseId;

        public ExerciseViewHolder(@NonNull View itemView, OnExerciseActionListener listener) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.tv_exercise_name);
            replaceIcon = itemView.findViewById(R.id.iv_replace_exercise);

            if (exerciseName == null) {
                System.err.println("Error: TextView R.id.tv_exercise_name not found...");
            }
            if (replaceIcon == null) {
                System.err.println("Error: ImageView R.id.iv_replace_exercise not found...");
            }


            if (replaceIcon != null && listener != null) {
                replaceIcon.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && currentExerciseId != null) {

                        listener.onReplaceExerciseClicked(position, currentExerciseId);
                    } else {

                        System.err.println("Error: Cannot replace exercise, ID is null or position invalid.");
                    }
                });
            }
        }

        void bind(Exercise exercise) {
            if (exerciseName == null) return;

            if (exercise != null) {
                this.currentExerciseId = exercise.getId();
                if (exercise.getName() != null) {
                    exerciseName.setText(exercise.getName());
                } else {
                    exerciseName.setText("Ошибка: нет названия");
                }
            } else {
                this.currentExerciseId = null;
                exerciseName.setText("Ошибка: нет данных об упражнении");
            }
        }
    }
} 