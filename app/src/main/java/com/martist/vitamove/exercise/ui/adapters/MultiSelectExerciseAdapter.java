package com.martist.vitamove.exercise.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.Exercise;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class MultiSelectExerciseAdapter extends ListAdapter<Exercise, MultiSelectExerciseAdapter.ExerciseViewHolder> {

    private final Set<String> selectedExerciseIds = new HashSet<>();
    private final boolean isSingleSelectionMode;
    private final Context context;
    private OnExerciseClickListener listener;


    public interface OnExerciseClickListener {

        void onExerciseItemClick(Exercise exercise);
    }

    public MultiSelectExerciseAdapter(Context context, boolean isSingleSelectionMode) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.isSingleSelectionMode = isSingleSelectionMode;
    }

    public MultiSelectExerciseAdapter(Context context, boolean isSingleSelectionMode, OnExerciseClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.isSingleSelectionMode = isSingleSelectionMode;
        this.listener = listener;
    }

    public void setOnExerciseClickListener(OnExerciseClickListener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Exercise> DIFF_CALLBACK = new DiffUtil.ItemCallback<Exercise>() {
        @Override
        public boolean areItemsTheSame(@NonNull Exercise oldItem, @NonNull Exercise newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Exercise oldItem, @NonNull Exercise newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName());
        }
    };

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise currentExercise = getItem(position);
        if (currentExercise != null && currentExercise.getId() != null) {
            holder.bind(currentExercise, selectedExerciseIds.contains(currentExercise.getId()));
        }
    }

    public List<String> getSelectedExerciseIds() {
        return new ArrayList<>(selectedExerciseIds);
    }

    public void clearSelection() {
        selectedExerciseIds.clear();
        notifyDataSetChanged();
    }


    public void setPreviouslySelectedIds(List<String> selectedIds) {
        if (selectedIds != null) {
            selectedExerciseIds.clear();
            selectedExerciseIds.addAll(selectedIds);
            notifyDataSetChanged();
        }
    }

    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView muscleGroupText;
        private final MaterialCardView cardView;
        private final ImageView addButton;
        private String currentExerciseId;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.exercise_name);
            muscleGroupText = itemView.findViewById(R.id.exercise_muscle_group);
            cardView = itemView.findViewById(R.id.cardView);
            addButton = itemView.findViewById(R.id.add_exercise_button);


            cardView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Exercise clickedExercise = getItem(position);
                    if (clickedExercise != null && clickedExercise.getId() != null) {

                        if (listener != null) {
                            listener.onExerciseItemClick(clickedExercise);
                        } else {

                            toggleSelection(clickedExercise);
                        }
                    }
                }
            });


            addButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Exercise clickedExercise = getItem(position);
                    if (clickedExercise != null && clickedExercise.getId() != null) {
                        toggleSelection(clickedExercise);
                    }
                }
            });
        }

        void bind(Exercise exercise, boolean isSelected) {
            nameText.setText(exercise.getName() != null ? exercise.getName() : "Нет названия");

            List<String> muscleGroupNames = exercise.getMuscleGroupRussianNames();
            if (muscleGroupNames != null && !muscleGroupNames.isEmpty()) {
                String muscleGroups = String.join(", ", muscleGroupNames);
                muscleGroupText.setText(muscleGroups);
                muscleGroupText.setVisibility(View.VISIBLE);
            } else {
                muscleGroupText.setVisibility(View.GONE);
            }


            cardView.setChecked(isSelected);
            cardView.setStrokeWidth(isSelected ? 2 : 0);


            if (isSelected) {
                addButton.setImageResource(R.drawable.ic_check_circle);
            } else {
                addButton.setImageResource(R.drawable.ic_add_circle);
            }

            this.currentExerciseId = (exercise != null) ? exercise.getId() : null;
        }

        private void toggleSelection(Exercise exercise) {
            String exerciseId = exercise.getId();
            if (exerciseId == null) return;

            boolean wasSelected = selectedExerciseIds.contains(exerciseId);

            if (isSingleSelectionMode) {
                if (!wasSelected) {
                    selectedExerciseIds.clear();
                    selectedExerciseIds.add(exerciseId);
                    notifyDataSetChanged();
                }
            } else {

                if (wasSelected) {
                    selectedExerciseIds.remove(exerciseId);
                } else {
                    selectedExerciseIds.add(exerciseId);
                }
                notifyItemChanged(getAdapterPosition());
            }
        }
    }
} 