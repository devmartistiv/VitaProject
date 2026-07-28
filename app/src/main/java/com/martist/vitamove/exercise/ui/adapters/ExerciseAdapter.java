package com.martist.vitamove.exercise.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.Exercise;

import java.util.ArrayList;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {
    private final Context context;
    private List<Exercise> exercises;
    private final OnExerciseClickListener listener;


    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);

        void onAddExerciseClick(Exercise exercise);
    }


    public ExerciseAdapter(Context context, OnExerciseClickListener listener) {
        this.context = context;
        this.exercises = new ArrayList<>();
        this.listener = listener;
    }


    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        holder.bind(exercises.get(position));
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }


    public void updateExercises(List<Exercise> newExercises) {
        this.exercises = newExercises;
        notifyDataSetChanged();
    }

    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView muscleGroupText;
        private final MaterialCardView cardView;
        private final ImageView addButton;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.exercise_name);
            muscleGroupText = itemView.findViewById(R.id.exercise_muscle_group);
            cardView = itemView.findViewById(R.id.cardView);
            addButton = itemView.findViewById(R.id.add_exercise_button);
        }

        void bind(Exercise exercise) {
            nameText.setText(exercise.getName());


            List<String> muscleGroupNames = exercise.getMuscleGroupRussianNames();
            String muscleGroups = String.join(", ", muscleGroupNames);
            muscleGroupText.setText(muscleGroups);


            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExerciseClick(exercise);
                }
            });


            addButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddExerciseClick(exercise);
                }
            });
        }
    }
} 