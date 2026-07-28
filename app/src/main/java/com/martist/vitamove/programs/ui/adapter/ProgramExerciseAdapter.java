package com.martist.vitamove.programs.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.data.managers.ExerciseManager;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.programs.ui.model.ProgramExercise;

import java.util.List;
import java.util.Locale;

public class ProgramExerciseAdapter extends RecyclerView.Adapter<ProgramExerciseAdapter.ExerciseViewHolder> {
    private List<ProgramExercise> exercises;
    private final OnExerciseClickListener listener;

    public interface OnExerciseClickListener {
        void onExerciseClick(ProgramExercise exercise);


    }

    public ProgramExerciseAdapter(List<ProgramExercise> exercises, OnExerciseClickListener listener) {
        this.exercises = exercises;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_program_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        ProgramExercise exercise = exercises.get(position);
        holder.bind(exercise, position);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public void updateExercises(List<ProgramExercise> newExercises) {
        this.exercises = newExercises;
        notifyDataSetChanged();
    }

    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView exerciseName;
        private final TextView exerciseDetails;
        private final ExerciseManager exerciseManager;

        ExerciseViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            exerciseName = itemView.findViewById(R.id.exercise_name);
            exerciseDetails = itemView.findViewById(R.id.exercise_details);
            exerciseManager = new ExerciseManager(itemView.getContext());
        }

        void bind(ProgramExercise exercise, int position) {
            if (exercise == null) {
                Log.e("ProgramExerciseAdapter", "Передано null-упражнение в методе bind()");
                return;
            }


            if (exerciseName != null) {

                exerciseName.setText("Загрузка...");


                String exerciseId = exercise.getExerciseId();
                if (exerciseId != null && !exerciseId.isEmpty()) {
                    exerciseManager.getExerciseByIdAsync(exerciseId, new ExerciseManager.AsyncCallback<Exercise>() {
                        @Override
                        public void onSuccess(Exercise result) {
                            if (result != null && exerciseName != null) {
                                exerciseName.setText(result.getName());
                            } else if (exerciseName != null) {
                                exerciseName.setText("Упражнение #" + position);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("ProgramExerciseAdapter", "Ошибка при загрузке упражнения: " + e.getMessage());
                            if (exerciseName != null) {
                                exerciseName.setText("Упражнение #" + position);
                            }
                        }
                    });
                } else {
                    exerciseName.setText("Упражнение #" + position);
                }
            }


            if (exerciseDetails != null) {
                String details = String.format(Locale.getDefault(),
                        "%d подхода × %d повторений",
                        exercise.getTargetSets(),
                        exercise.getTargetReps());
                exerciseDetails.setText(details);
            }


            if (cardView != null) {
                cardView.setOnClickListener(v -> {

                    cardView.setCardBackgroundColor(cardView.getContext().getResources().getColor(R.color.ripple_color));


                    new android.os.Handler().postDelayed(() -> {
                        cardView.setCardBackgroundColor(cardView.getContext().getResources().getColor(R.color.card_background));
                    }, 150);


                    Log.d("ProgramExerciseAdapter", "onClick: Клик по упражнению, позиция = " + position);


                    if (listener != null) {
                        listener.onExerciseClick(exercise);
                    } else {
                        Log.e("ProgramExerciseAdapter", "onClick: listener is null");
                        Toast.makeText(v.getContext(), "Ошибка: обработчик клика не настроен", Toast.LENGTH_SHORT).show();
                    }
                });


                cardView.setClickable(true);
                cardView.setFocusable(true);
                cardView.setForeground(cardView.getContext().getResources().getDrawable(R.drawable.ripple_effect));
            }


        }
    }
} 