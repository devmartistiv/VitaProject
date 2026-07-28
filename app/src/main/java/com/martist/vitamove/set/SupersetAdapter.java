package com.martist.vitamove.set;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.workout.data.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;

public class SupersetAdapter extends RecyclerView.Adapter<SupersetAdapter.supersetViewHolder> {
    private List<WorkoutExercise> workoutExercises;
    private final OnExerciseClickListener listener;

    private List<WorkoutExercise> selectedExercises = new ArrayList<>();


    private boolean isSelected(WorkoutExercise exercise) {
        return selectedExercises.contains(exercise);
    }


    public List<WorkoutExercise> getSelectedExercises() {
        return new ArrayList<>(selectedExercises);
    }

    public SupersetAdapter(OnExerciseClickListener listener) {
        this.listener = listener;
    }

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);

    }

    @NonNull
    @Override
    public SupersetAdapter.supersetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new SupersetAdapter.supersetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SupersetAdapter.supersetViewHolder holder, int position) {
        holder.bind(workoutExercises.get(position));

    }

    @Override
    public int getItemCount() {
        return workoutExercises.size();
    }

    public void setExercises(List<WorkoutExercise> workoutExercises) {
        this.workoutExercises = workoutExercises;
        notifyDataSetChanged();
    }

    class supersetViewHolder extends RecyclerView.ViewHolder {
        private TextView exercise_name;
        private CardView cardView;
        private ImageView add_exercise_button, added_exercise_button;

        supersetViewHolder(@NonNull View itemView) {
            super(itemView);
            exercise_name = itemView.findViewById(R.id.exercise_name);
            cardView = itemView.findViewById(R.id.cardView);
            add_exercise_button = itemView.findViewById(R.id.add_exercise_button);
            added_exercise_button = itemView.findViewById(R.id.added_exercise_button);
        }

        private void updateCardAppearance(WorkoutExercise exercise) {
            if (isSelected(exercise)) {
                add_exercise_button.setVisibility(View.GONE);
                added_exercise_button.setVisibility(View.VISIBLE);
            } else {
                add_exercise_button.setVisibility(View.VISIBLE);
                added_exercise_button.setVisibility(View.GONE);
            }
        }

        void bind(WorkoutExercise exercise) {
            exercise_name.setText(exercise.getExercise().getName());


            updateCardAppearance(exercise);

            cardView.setOnClickListener(v -> {

                if (selectedExercises.contains(exercise)) {
                    selectedExercises.remove(exercise);
                } else {
                    selectedExercises.add(exercise);
                }


                notifyDataSetChanged();


                if (listener != null) {
                    listener.onExerciseClick(exercise.getExercise());
                }
            });
        }
    }


}
