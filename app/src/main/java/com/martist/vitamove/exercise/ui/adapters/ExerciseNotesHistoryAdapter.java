package com.martist.vitamove.exercise.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.ExerciseNoteHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ExerciseNotesHistoryAdapter extends RecyclerView.Adapter<ExerciseNotesHistoryAdapter.NoteHistoryViewHolder> {

    private List<ExerciseNoteHistory> notes;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;

    public ExerciseNotesHistoryAdapter() {
        this.notes = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("ru", "RU"));
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public NoteHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise_note_history, parent, false);
        return new NoteHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteHistoryViewHolder holder, int position) {
        ExerciseNoteHistory note = notes.get(position);
        holder.bind(note);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<ExerciseNoteHistory> newNotes) {
        this.notes.clear();
        if (newNotes != null) {
            this.notes.addAll(newNotes);
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return notes.isEmpty();
    }

    class NoteHistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView workoutDate;
        private final TextView workoutTime;
        private final TextView workoutName;
        private final TextView noteText;
        private final TextView setsCompleted;
        private final TextView exerciseDuration;
        private final LinearLayout exerciseStatsLayout;

        public NoteHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            workoutDate = itemView.findViewById(R.id.workout_date);
            workoutTime = itemView.findViewById(R.id.workout_time);
            workoutName = itemView.findViewById(R.id.workout_name);
            noteText = itemView.findViewById(R.id.note_text);
            setsCompleted = itemView.findViewById(R.id.sets_completed);
            exerciseDuration = itemView.findViewById(R.id.exercise_duration);
            exerciseStatsLayout = itemView.findViewById(R.id.exercise_stats_layout);
        }

        public void bind(ExerciseNoteHistory note) {

            if (note.getWorkoutDate() != null) {
                workoutDate.setText(dateFormat.format(note.getWorkoutDate()));


                String timeText = note.getWorkoutTime();
                if (timeText != null && !timeText.trim().isEmpty()) {
                    workoutTime.setText(timeText);
                } else {
                    workoutTime.setText(timeFormat.format(note.getWorkoutDate()));
                }
            } else {
                workoutDate.setText("Неизвестная дата");
                workoutTime.setText("");
            }


            if (note.getWorkoutName() != null && !note.getWorkoutName().trim().isEmpty()) {
                workoutName.setText(note.getWorkoutName());
                workoutName.setVisibility(View.VISIBLE);
            } else {
                workoutName.setVisibility(View.GONE);
            }


            noteText.setText(note.getNoteText());


            boolean hasStats = note.getSetsCompleted() > 0 || note.getExerciseDurationMinutes() > 0;

            if (hasStats) {
                exerciseStatsLayout.setVisibility(View.VISIBLE);


                if (note.getSetsCompleted() > 0) {
                    setsCompleted.setText(note.getSetsCompleted() + " подходов");
                    setsCompleted.setVisibility(View.VISIBLE);
                } else {
                    setsCompleted.setVisibility(View.GONE);
                }


                if (note.getExerciseDurationMinutes() > 0) {
                    exerciseDuration.setText(note.getExerciseDurationMinutes() + " мин");
                    exerciseDuration.setVisibility(View.VISIBLE);
                } else {
                    exerciseDuration.setVisibility(View.GONE);
                }
            } else {
                exerciseStatsLayout.setVisibility(View.GONE);
            }
        }
    }
}
