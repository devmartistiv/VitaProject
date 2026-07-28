package com.martist.vitamove.workout.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.workout.data.model.UserWorkout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.WorkoutViewHolder> {
    private List<UserWorkout> workouts;
    private final OnWorkoutClickListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnWorkoutClickListener {
        void onWorkoutClick(UserWorkout workout);
    }

    public WorkoutHistoryAdapter(OnWorkoutClickListener listener) {
        this.workouts = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"));
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_history, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        UserWorkout workout = workouts.get(position);
        holder.bind(workout);
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    public void updateWorkouts(List<UserWorkout> newWorkouts) {
        this.workouts = newWorkouts;
        notifyDataSetChanged();
    }


    public List<UserWorkout> getWorkouts() {
        return workouts;
    }

    class WorkoutViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;
        private final TextView durationText;
        private final TextView exercisesCountText;
        private final TextView caloriesCountText;
        private final MaterialButton detailsButton;

        WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.workout_date);
            durationText = itemView.findViewById(R.id.workout_duration);
            exercisesCountText = itemView.findViewById(R.id.exercises_count);
            caloriesCountText = itemView.findViewById(R.id.calories_count);
            detailsButton = itemView.findViewById(R.id.details_button);
        }

        void bind(UserWorkout workout) {

            dateText.setText(dateFormat.format(workout.getStartTime()));


            if (workout.getEndTime() != null) {
                long duration = workout.getEndTime() - workout.getStartTime();
                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
                durationText.setText(String.format(Locale.getDefault(), "%d:%02d", hours, minutes));
            } else {
                durationText.setText("-");
            }


            int exercisesCount = workout.getExercises().size();
            exercisesCountText.setText(String.format(Locale.getDefault(), "%d", exercisesCount));


            int calories = workout.getTotalCalories();
            caloriesCountText.setText(String.format(Locale.getDefault(), "%d", calories));


            detailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onWorkoutClick(workout);
                }
            });
        }
    }
} 