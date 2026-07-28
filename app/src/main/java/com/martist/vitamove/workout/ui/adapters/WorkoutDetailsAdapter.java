package com.martist.vitamove.workout.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.workout.data.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;


public class WorkoutDetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EXERCISE_HEADER = 0;
    private static final int TYPE_EXERCISE_SET = 1;

    private final List<Object> items = new ArrayList<>();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_EXERCISE_HEADER) {
            View view = inflater.inflate(R.layout.item_workout_details_exercise, parent, false);
            return new ExerciseHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_workout_details_set, parent, false);
            return new SetViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof ExerciseHeaderViewHolder && item instanceof WorkoutExercise) {
            ((ExerciseHeaderViewHolder) holder).bind((WorkoutExercise) item);
        } else if (holder instanceof SetViewHolder && item instanceof SetItem) {
            ((SetViewHolder) holder).bind((SetItem) item);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof WorkoutExercise) {
            return TYPE_EXERCISE_HEADER;
        } else {
            return TYPE_EXERCISE_SET;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    public void updateExercises(List<WorkoutExercise> exercises) {
        items.clear();

        for (WorkoutExercise exercise : exercises) {

            items.add(exercise);


            List<ExerciseSet> sets = exercise.getSetsCompleted();
            for (int i = 0; i < sets.size(); i++) {
                items.add(new SetItem(sets.get(i), i + 1, exercise.getExercise().getName()));
            }
        }

        notifyDataSetChanged();
    }


    private static class SetItem {
        final ExerciseSet set;
        final int setNumber;
        final String exerciseName;

        SetItem(ExerciseSet set, int setNumber, String exerciseName) {
            this.set = set;
            this.setNumber = setNumber;
            this.exerciseName = exerciseName;
        }
    }


    static class ExerciseHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView exerciseNameText;
        private final TextView muscleGroupsText;

        ExerciseHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseNameText = itemView.findViewById(R.id.exercise_name);
            muscleGroupsText = itemView.findViewById(R.id.muscle_groups);
        }

        void bind(WorkoutExercise exercise) {
            exerciseNameText.setText(exercise.getExercise().getName());


            List<String> muscleGroupNames = exercise.getExercise().getMuscleGroupRussianNames();
            String muscleGroups = String.join(", ", muscleGroupNames);
            muscleGroupsText.setText(muscleGroups);
        }
    }


    static class SetViewHolder extends RecyclerView.ViewHolder {
        private final TextView setNumberText;
        private final TextView weightText;
        private final TextView repsText;

        SetViewHolder(@NonNull View itemView) {
            super(itemView);
            setNumberText = itemView.findViewById(R.id.set_number);
            weightText = itemView.findViewById(R.id.weight);
            repsText = itemView.findViewById(R.id.reps);
        }

        void bind(SetItem setItem) {
            setNumberText.setText(String.format("Подход %d", setItem.setNumber));

            ExerciseSet set = setItem.set;


            if (set.isWeightBasedSet()) {
                weightText.setVisibility(View.VISIBLE);
                weightText.setText(String.format("%.1f кг", set.getWeight()));
            } else {
                weightText.setVisibility(View.GONE);
            }


            if (set.isTimeBasedSet()) {
                repsText.setText(String.format("%d сек", set.getDurationSeconds()));
            } else if (set.getReps() != null) {
                repsText.setText(String.format("%d повт", set.getReps()));
            } else {
                repsText.setVisibility(View.GONE);
            }
        }
    }
} 