package com.martist.vitamove.workout.ui.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.ExerciseViewModel;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.workout.data.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActiveWorkoutAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EXERCISE = 0;
    private static final int TYPE_ADD_BUTTON = 1;
    private static final int TYPE_SUPERSET_GROUP = 2;
    private ExerciseViewModel viewModel;

    public List<WorkoutExercise> exercises;
    private List<Object> displayItems;
    private final OnExerciseClickListener listener;

    public interface OnExerciseClickListener {
        void onExerciseClick(WorkoutExercise exercise, int position);

        void onDeleteExercise(WorkoutExercise exercise, int position);

        void onAddSuperset(WorkoutExercise exercise, int position);

        void onAddExerciseNote(WorkoutExercise exercise, int position);

        void onReplaceExercise(WorkoutExercise exercise, int position);

        void onExerciseOrderChanged(List<WorkoutExercise> exercises);

        void onAddExerciseClick();
    }


    public static class SupersetGroup {
        public String supersetId;
        public List<WorkoutExercise> exercises;
        public int color;

        public SupersetGroup(String supersetId, List<WorkoutExercise> exercises) {
            this.supersetId = supersetId;
            this.exercises = new ArrayList<>(exercises);
            this.color = generateSupersetColor(supersetId);
        }

        private static int generateSupersetColor(String supersetId) {
            int hash = supersetId.hashCode();
            int[] colors = {
                    0xFF2196F3, 0xFFFF9800, 0xFF4CAF50, 0xFF9C27B0,
                    0xFFFF5722, 0xFF607D8B, 0xFF795548, 0xFFE91E63
            };
            return colors[Math.abs(hash) % colors.length];
        }
    }

    public ActiveWorkoutAdapter(List<WorkoutExercise> exercises, OnExerciseClickListener listener) {
        this.exercises = new ArrayList<>(exercises);
        this.displayItems = new ArrayList<>();
        this.listener = listener;
        updateDisplayItems();
        Log.d("ActiveWorkoutAdapter", "Создан адаптер с " + exercises.size() + " упражнениями");
    }


    private void updateDisplayItems() {
        displayItems.clear();

        Map<String, List<WorkoutExercise>> supersetGroups = new HashMap<>();
        Set<String> addedSupersets = new HashSet<>();


        for (WorkoutExercise exercise : exercises) {
            String supersetId = exercise.getSuperset_id();
            if (supersetId != null && !supersetId.isEmpty()) {
                supersetGroups.computeIfAbsent(supersetId, k -> new ArrayList<>()).add(exercise);
            }
        }


        for (List<WorkoutExercise> supersetExercises : supersetGroups.values()) {
            supersetExercises.sort((a, b) -> Integer.compare(a.getSuperset_order(), b.getSuperset_order()));
        }


        for (WorkoutExercise exercise : exercises) {
            String supersetId = exercise.getSuperset_id();

            if (supersetId != null && !supersetId.isEmpty()) {

                if (!addedSupersets.contains(supersetId)) {
                    List<WorkoutExercise> supersetExercises = supersetGroups.get(supersetId);
                    displayItems.add(new SupersetGroup(supersetId, supersetExercises));
                    addedSupersets.add(supersetId);
                }
            } else {

                displayItems.add(exercise);
            }
        }

        Log.d("ActiveWorkoutAdapter", "updateDisplayItems: " + displayItems.size() + " элементов для отображения");
    }

    @Override
    public int getItemViewType(int position) {
        if (position == displayItems.size()) {
            return TYPE_ADD_BUTTON;
        }

        Object item = displayItems.get(position);
        if (item instanceof SupersetGroup) {
            return TYPE_SUPERSET_GROUP;
        } else {
            return TYPE_EXERCISE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_ADD_BUTTON:
                View addView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_add_exercise, parent, false);
                return new AddExerciseViewHolder(addView);

            case TYPE_SUPERSET_GROUP:
                View supersetView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_superset_group, parent, false);
                return new SupersetGroupViewHolder(supersetView);

            default:
                View exerciseView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_active_exercise, parent, false);
                return new ExerciseViewHolder(exerciseView);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_EXERCISE) {
            WorkoutExercise exercise = (WorkoutExercise) displayItems.get(position);
            ((ExerciseViewHolder) holder).bind(exercise, position);
            Log.d("ActiveWorkoutAdapter", "Привязан элемент " + position + ": " +
                    exercise.getExercise().getName());

        } else if (viewType == TYPE_SUPERSET_GROUP) {
            SupersetGroup supersetGroup = (SupersetGroup) displayItems.get(position);
            ((SupersetGroupViewHolder) holder).bind(supersetGroup, position);
            Log.d("ActiveWorkoutAdapter", "Привязана группа суперсета " + position + ": " +
                    supersetGroup.exercises.size() + " упражнений");

        } else {

        }
    }

    @Override
    public int getItemCount() {

        return displayItems.size() + 1;
    }

    public void updateExercises(List<WorkoutExercise> newExercises) {
        Log.d("ActiveWorkoutAdapter", "Обновление списка упражнений: старый размер=" +
                exercises.size() + ", новый размер=" + newExercises.size());
        this.exercises = new ArrayList<>(newExercises);


        updateOrderNumbers();


        updateDisplayItems();

        notifyDataSetChanged();
    }


    public void refreshSupersetDisplay() {
        Log.d("ActiveWorkoutAdapter", "Обновление отображения суперсетов");

        updateDisplayItems();

        notifyDataSetChanged();
    }


    public boolean moveExercise(int fromPosition, int toPosition) {

        if (fromPosition == exercises.size() || toPosition == exercises.size()) {
            return false;
        }

        if (fromPosition < 0 || fromPosition >= exercises.size() ||
                toPosition < 0 || toPosition >= exercises.size()) {
            return false;
        }

        Collections.swap(exercises, fromPosition, toPosition);

        updateOrderNumbers();


        updateDisplayItems();
        notifyDataSetChanged();

        if (listener != null) {
            listener.onExerciseOrderChanged(exercises);
        }

        return true;
    }

    private void updateOrderNumbers() {
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).setOrderNumber(i);
        }
    }


    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView nameText;
        private final TextView muscleGroupsText;
        private final ImageButton menuButton;
        private final TextView setsText;
        private final TextView repsText;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            nameText = itemView.findViewById(R.id.exercise_name);
            muscleGroupsText = itemView.findViewById(R.id.exercise_muscle_groups);
            menuButton = itemView.findViewById(R.id.menu_button);
            setsText = itemView.findViewById(R.id.exercise_sets);
            repsText = itemView.findViewById(R.id.exercise_reps);


            cardView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onExerciseClick(exercises.get(position), position);
                }
            });


            menuButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    showPopupMenu(v, exercises.get(position), position);
                }
            });
        }

        private void showPopupMenu(View view, WorkoutExercise exercise, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.inflate(R.menu.exercise_menu);


            String supersetId = exercise.getSuperset_id();
            if (supersetId != null && !supersetId.isEmpty()) {
                popup.getMenu().findItem(R.id.action_add_superset).setVisible(false);
            }

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_delete_exercise) {
                    if (listener != null) {
                        listener.onDeleteExercise(exercise, position);
                    }
                    return true;
                } else if (itemId == R.id.action_add_note) {
                    if (listener != null) {
                        listener.onAddExerciseNote(exercise, position);
                    }
                    return true;
                } else if (itemId == R.id.action_replace_exercise) {
                    if (listener != null) {
                        listener.onReplaceExercise(exercise, position);
                    }
                    return true;
                } else if (itemId == R.id.action_add_superset) {
                    if (listener != null) {
                        listener.onAddSuperset(exercise, position);
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        }


        void bind(WorkoutExercise exercise, int position) {
            nameText.setText(exercise.getExercise().getName());

            List<String> muscleGroupRussianNames = exercise.getExercise().getMuscleGroupRussianNames();
            String muscleGroups = String.join(", ", muscleGroupRussianNames);
            muscleGroupsText.setText(muscleGroups);


            setupSupersetDisplay(exercise, position);
            List<ExerciseSet> sets = exercise.getSetsCompleted();
            int totalSets = sets.size();

            boolean isCardioExercise = exercise.getExercise().isCardioExercise();
            boolean isStaticExercise = exercise.getExercise().isStaticExercise();


            boolean isWarmupStretching = exercise.getExercise().getExerciseType() != null &&
                    (exercise.getExercise().getExerciseType().equalsIgnoreCase("разминка") ||
                            exercise.getExercise().getExerciseType().equalsIgnoreCase("warm-up") ||
                            exercise.getExercise().getExerciseType().equalsIgnoreCase("растяжка") ||
                            exercise.getExercise().getExerciseType().equalsIgnoreCase("stretching"));

            ImageView repsIcon = itemView.findViewById(R.id.exercise_reps_icon);
            ImageView setsIcon = itemView.findViewById(R.id.exercise_sets_icon);
            TextView separator = itemView.findViewById(R.id.stats_separator);


            if (isWarmupStretching) {

                boolean isCompleted = exercise.isRated();


                if (!isCompleted && sets != null && !sets.isEmpty()) {
                    for (ExerciseSet set : sets) {
                        if (set.isCompleted()) {
                            isCompleted = true;
                            break;
                        }
                    }
                }


                setsIcon.setVisibility(View.GONE);
                setsText.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);
                repsIcon.setVisibility(View.VISIBLE);
                repsIcon.setImageResource(isCompleted ?
                        R.drawable.ic_check : R.drawable.ic_time);
                repsText.setVisibility(View.VISIBLE);
                repsText.setText(isCompleted ? "Выполнено" : "Не выполнено");

                Log.d("ActiveWorkoutAdapter", "Отображаем статус разминки/растяжки: " +
                        (isCompleted ? "Выполнено" : "Не выполнено") +
                        ", isRated=" + exercise.isRated() +
                        ", подходов всего: " + (sets != null ? sets.size() : 0));
            } else if (isCardioExercise || isStaticExercise) {

                int totalSeconds = 0;


                for (ExerciseSet set : sets) {
                    if (set.getDurationSeconds() != null) {
                        totalSeconds += set.getDurationSeconds();
                    }
                }


                int totalMinutes = totalSeconds / 60;


                setsIcon.setVisibility(View.GONE);
                setsText.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);


                repsIcon.setVisibility(View.VISIBLE);
                repsIcon.setImageResource(R.drawable.ic_timer);
                repsText.setVisibility(View.VISIBLE);
                repsText.setText(totalMinutes > 0 ? totalMinutes + " минут" : totalSeconds > 0 ? totalSeconds + " секунд" : "0 минут");

                Log.d("ActiveWorkoutAdapter", "Отображаем время: " + repsText.getText().toString());
            } else {

                int completedSets = exercise.getCompletedSetsCount();
                Log.d("ActiveWorkoutAdapter", "Выполненных подходов: " + completedSets);

                int targetSets = exercise.getExercise().getDefaultSets();


                setsText.setVisibility(View.VISIBLE);


                if (repsIcon != null) {
                    repsIcon.setImageResource(R.drawable.ic_fitness);
                }


                if (setsIcon != null) {
                    setsIcon.setVisibility(View.VISIBLE);
                }

                if (separator != null) {
                    separator.setVisibility(View.VISIBLE);
                }

                if (totalSets > 0) {
                    setsText.setText(completedSets + "/" + totalSets + " подходов");
                    Log.d("ActiveWorkoutAdapter", "Sets text: " + completedSets + "/" + totalSets + " подходов");
                } else if (targetSets > 0) {
                    setsText.setText("0/" + targetSets + " подходов");
                    Log.d("ActiveWorkoutAdapter", "Sets text (using default): 0/" + targetSets + " подходов");
                } else {
                    setsText.setText("0 подходов");
                    Log.d("ActiveWorkoutAdapter", "Sets text (fallback): 0 подходов");
                }

                String defaultReps = exercise.getExercise().getDefaultReps();

                if (totalSets > 0 && !sets.isEmpty()) {
                    ExerciseSet firstSet = sets.get(0);
                    Integer reps = firstSet.getTargetReps() != null
                            ? firstSet.getTargetReps()
                            : firstSet.getReps();
                    Log.d("dddd", "Reps : " + reps + " повторений");
                    if (reps != null) {
                        repsText.setText(reps + " повторений");
                        Log.d("ActiveWorkoutAdapter", "Reps text: " + reps + " повторений");
                    } else if (firstSet.getDurationSeconds() != null) {
                        int seconds = firstSet.getDurationSeconds();
                        repsText.setText(seconds + " секунд");
                        Log.d("ActiveWorkoutAdapter", "Reps text (duration): " + seconds + " секунд");
                    } else if (defaultReps != null && !defaultReps.isEmpty()) {
                        repsText.setText(defaultReps + " повторений");
                        Log.d("ActiveWorkoutAdapter", "Reps text (using default): " + defaultReps + " повторений");
                    } else {
                        repsText.setText("-- повторений");
                        Log.d("ActiveWorkoutAdapter", "Reps text (fallback): -- повторений");
                    }
                } else if (defaultReps != null && !defaultReps.isEmpty()) {
                    repsText.setText(defaultReps + " повторений");
                    Log.d("ActiveWorkoutAdapter", "Reps text (using default): " + defaultReps + " повторений");
                } else {
                    repsText.setText("-- повторений");
                    Log.d("ActiveWorkoutAdapter", "Reps text (fallback): -- повторений");
                }
            }
        }


        private void setupSupersetDisplay(WorkoutExercise exercise, int position) {
            String supersetId = exercise.getSuperset_id();

            if (supersetId != null && !supersetId.isEmpty()) {

                int supersetOrder = exercise.getSuperset_order();


                addSupersetIndicator(exercise, supersetOrder);


                setSupersetCardColor(supersetId, supersetOrder);

                Log.d("ActiveWorkoutAdapter", "Суперсет: " + exercise.getExercise().getName() +
                        " (ID: " + supersetId + ", Order: " + supersetOrder + ")");
            } else {

                removeSupersetIndicator();
                setNormalCardColor();
            }
        }


        private void addSupersetIndicator(WorkoutExercise exercise, int supersetOrder) {
            String exerciseName = exercise.getExercise().getName();
            String indicator = getSupersetLetter(supersetOrder);
            nameText.setText(indicator + " " + exerciseName);
        }


        private void removeSupersetIndicator() {

        }


        private String getSupersetLetter(int order) {
            if (order >= 0 && order < 26) {
                return String.valueOf((char) ('A' + order));
            }
            return String.valueOf(order + 1);
        }


        private void setSupersetCardColor(String supersetId, int order) {

            int color = generateSupersetColor(supersetId);
            cardView.setStrokeColor(color);
            cardView.setStrokeWidth(4);
        }


        private void setNormalCardColor() {
            cardView.setStrokeWidth(0);
        }


        private int generateSupersetColor(String supersetId) {

            int hash = supersetId.hashCode();


            int[] colors = {
                    0xFF2196F3,
                    0xFFFF9800,
                    0xFF4CAF50,
                    0xFF9C27B0,
                    0xFFFF5722,
                    0xFF607D8B,
                    0xFF795548,
                    0xFFE91E63
            };

            return colors[Math.abs(hash) % colors.length];
        }
    }

    class AddExerciseViewHolder extends RecyclerView.ViewHolder {
        AddExerciseViewHolder(@NonNull View itemView) {
            super(itemView);


            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddExerciseClick();
                }
            });
        }
    }


    class SupersetGroupViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView supersetCard;
        private final TextView supersetTitle;
        private final RecyclerView supersetExercisesList;
        private final TextView supersetInfo;

        SupersetGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            supersetCard = itemView.findViewById(R.id.superset_card);
            supersetTitle = itemView.findViewById(R.id.superset_title);
            supersetExercisesList = itemView.findViewById(R.id.superset_exercises_list);
            supersetInfo = itemView.findViewById(R.id.superset_info);


            supersetExercisesList.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            supersetExercisesList.setNestedScrollingEnabled(false);
        }

        void bind(SupersetGroup supersetGroup, int position) {

            supersetCard.setStrokeColor(supersetGroup.color);


            int exerciseCount = supersetGroup.exercises.size();
            supersetTitle.setText("Суперсет (" + exerciseCount + " упражнений)");


            supersetInfo.setText("Выполните упражнения друг за другом без отдыха");


            SupersetExerciseAdapter exerciseAdapter = new SupersetExerciseAdapter(
                    supersetGroup.exercises,
                    supersetGroup.color,
                    new SupersetExerciseAdapter.OnExerciseClickListener() {
                        @Override
                        public void onExerciseClick(WorkoutExercise exercise, int exercisePosition) {

                            if (listener != null) {
                                listener.onExerciseClick(exercise, position);
                            }
                        }

                        @Override
                        public void onDeleteExercise(WorkoutExercise exercise, int exercisePosition) {
                            if (listener != null) {
                                listener.onDeleteExercise(exercise, position);
                            }
                        }

                        @Override
                        public void onAddExerciseNote(WorkoutExercise exercise, int exercisePosition) {
                            if (listener != null) {
                                listener.onAddExerciseNote(exercise, position);
                            }
                        }

                        @Override
                        public void onReplaceExercise(WorkoutExercise exercise, int exercisePosition) {
                            if (listener != null) {
                                listener.onReplaceExercise(exercise, position);
                            }
                        }
                    }
            );
            supersetExercisesList.setAdapter(exerciseAdapter);


        }
    }


    private static class SupersetExerciseAdapter extends RecyclerView.Adapter<SupersetExerciseAdapter.SupersetExerciseViewHolder> {
        private final List<WorkoutExercise> exercises;
        private final int supersetColor;
        private final OnExerciseClickListener listener;

        public interface OnExerciseClickListener {
            void onExerciseClick(WorkoutExercise exercise, int position);

            void onDeleteExercise(WorkoutExercise exercise, int position);

            void onAddExerciseNote(WorkoutExercise exercise, int position);

            void onReplaceExercise(WorkoutExercise exercise, int position);
        }

        SupersetExerciseAdapter(List<WorkoutExercise> exercises, int supersetColor, OnExerciseClickListener listener) {
            this.exercises = exercises;
            this.supersetColor = supersetColor;
            this.listener = listener;
        }

        @NonNull
        @Override
        public SupersetExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_active_exercise, parent, false);
            return new SupersetExerciseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SupersetExerciseViewHolder holder, int position) {
            holder.bind(exercises.get(position), position, supersetColor);
        }

        @Override
        public int getItemCount() {
            return exercises.size();
        }


        private void showPopupMenuForSupersetExercise(View view, WorkoutExercise exercise, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.inflate(R.menu.exercise_menu);


            popup.getMenu().findItem(R.id.action_add_superset).setVisible(false);

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_delete_exercise) {
                    if (listener != null) {
                        listener.onDeleteExercise(exercise, position);
                    }
                    return true;
                } else if (itemId == R.id.action_add_note) {
                    if (listener != null) {
                        listener.onAddExerciseNote(exercise, position);
                    }
                    return true;
                } else if (itemId == R.id.action_replace_exercise) {
                    if (listener != null) {
                        listener.onReplaceExercise(exercise, position);
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        }

        class SupersetExerciseViewHolder extends RecyclerView.ViewHolder {
            private final MaterialCardView cardView;
            private final TextView exerciseName;
            private final TextView exerciseSets;
            private final TextView exerciseReps;
            private final ImageButton menuButton;

            SupersetExerciseViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                exerciseName = itemView.findViewById(R.id.exercise_name);
                exerciseSets = itemView.findViewById(R.id.exercise_sets);
                exerciseReps = itemView.findViewById(R.id.exercise_reps);
                menuButton = itemView.findViewById(R.id.menu_button);
            }

            void bind(WorkoutExercise exercise, int position, int color) {

                cardView.setStrokeColor(color);
                cardView.setStrokeWidth(3);


                char letter = (char) ('A' + exercise.getSuperset_order());
                exerciseName.setText(letter + " " + exercise.getExercise().getName());


                int completedSets = exercise.getCompletedSetsCount();
                int totalSets = exercise.getSetsCompleted().size();
                int targetSets = exercise.getExercise().getDefaultSets();

                if (totalSets > 0) {
                    exerciseSets.setText(completedSets + "/" + totalSets + " подходов");
                } else if (targetSets > 0) {
                    exerciseSets.setText("0/" + targetSets + " подходов");
                } else {
                    exerciseSets.setText("0 подходов");
                }


                String defaultReps = exercise.getExercise().getDefaultReps();
                if (defaultReps != null && !defaultReps.isEmpty()) {
                    exerciseReps.setText(defaultReps + " повторений");
                } else {
                    exerciseReps.setText("-- повторений");
                }


                cardView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onExerciseClick(exercise, position);
                    }
                });


                menuButton.setOnClickListener(v -> {
                    showPopupMenuForSupersetExercise(v, exercise, position);
                });
            }
        }
    }
} 