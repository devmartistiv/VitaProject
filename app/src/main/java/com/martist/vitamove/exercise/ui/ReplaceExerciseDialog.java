package com.martist.vitamove.exercise.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.adapters.ExerciseAdapter;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.workout.data.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReplaceExerciseDialog extends DialogFragment {
    private static final String TAG = "ReplaceExerciseDialog";
    private static final String ARG_ORIGINAL_EXERCISE = "original_exercise";

    private WorkoutExercise originalExercise;


    private RecyclerView exerciseRecyclerView;
    private ExerciseAdapter exerciseAdapter;
    private SearchView searchView;
    private TextView titleText;
    private TextView emptyStateText;
    private ProgressBar progressBar;
    ExercisesViewModel exercisesViewModel;

    private List<Exercise> allExercises = new ArrayList<>();
    private List<Exercise> similarExercises = new ArrayList<>();


    public interface OnExerciseSelectedListener {
        void onExerciseSelected(Exercise selectedExercise, WorkoutExercise originalExercise);
    }

    private OnExerciseSelectedListener listener;

    public static ReplaceExerciseDialog newInstance(WorkoutExercise originalExercise) {
        ReplaceExerciseDialog dialog = new ReplaceExerciseDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ORIGINAL_EXERCISE, originalExercise);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            originalExercise = getArguments().getParcelable(ARG_ORIGINAL_EXERCISE);
        }

        exercisesViewModel = new ViewModelProvider(this).get(ExercisesViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_replace_exercise, null);

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupObservers();


        exercisesViewModel.getAllExercises();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setNegativeButton("Отмена", null)
                .create();
    }

    private void initViews(View view) {
        titleText = view.findViewById(R.id.title_text);
        exerciseRecyclerView = view.findViewById(R.id.exercise_recycler_view);
        searchView = view.findViewById(R.id.search_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        progressBar = view.findViewById(R.id.progress_bar);

        if (originalExercise != null) {
            titleText.setText("Заменить упражнение: " + originalExercise.getExercise().getName());
        }
    }

    private void setupRecyclerView() {
        exerciseAdapter = new ExerciseAdapter(getContext(), new ExerciseAdapter.OnExerciseClickListener() {
            @Override
            public void onExerciseClick(Exercise exercise) {
                Log.d(TAG, "onExerciseClick: выбрано упражнение " + exercise.getName());
                Log.d(TAG, "onExerciseClick: listener = " + (listener != null ? "установлен" : "null"));
                Log.d(TAG, "onExerciseClick: originalExercise = " + (originalExercise != null ? originalExercise.getExercise().getName() : "null"));

                if (listener != null) {
                    listener.onExerciseSelected(exercise, originalExercise);
                    Log.d(TAG, "onExerciseClick: вызван listener.onExerciseSelected");
                } else {
                    Log.e(TAG, "onExerciseClick: listener is null!");
                }
                dismiss();
            }

            @Override
            public void onAddExerciseClick(Exercise exercise) {
                Log.d(TAG, "onAddExerciseClick: выбрано упражнение " + exercise.getName());

                if (listener != null) {
                    listener.onExerciseSelected(exercise, originalExercise);
                    Log.d(TAG, "onAddExerciseClick: вызван listener.onExerciseSelected");
                } else {
                    Log.e(TAG, "onAddExerciseClick: listener is null!");
                }
                dismiss();
            }
        });

        exerciseRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        exerciseRecyclerView.setAdapter(exerciseAdapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterExercises(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterExercises(newText);
                return true;
            }
        });
    }

    private void setupObservers() {
        exercisesViewModel.getExercisesLiveData().observe(this, exercises -> {

            Log.d(TAG, "Получены упражнения: " + exercises.size());
            allExercises = exercises;
            findSimilarExercises();
            progressBar.setVisibility(View.GONE);

        });

        exercisesViewModel.isLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        exercisesViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), "Ошибка загрузки упражнений: " + errorMessage, Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }


    private void findSimilarExercises() {
        if (originalExercise == null || allExercises.isEmpty()) {
            updateEmptyState();
            return;
        }

        Exercise original = originalExercise.getExercise();
        List<String> originalMuscleGroups = original.getMuscleGroups();
        String originalExerciseType = original.getExerciseType();

        Log.d(TAG, "Поиск похожих упражнений для: " + original.getName());
        Log.d(TAG, "Исходные группы мышц: " + originalMuscleGroups);
        Log.d(TAG, "Исходный тип упражнения: " + originalExerciseType);

        similarExercises = allExercises.stream()
                .filter(exercise -> {

                    if (exercise.getId().equals(original.getId())) {
                        return false;
                    }


                    boolean exerciseTypeMatches = originalExerciseType != null &&
                            originalExerciseType.equals(exercise.getExerciseType());


                    boolean muscleGroupsMatch = false;
                    List<String> exerciseMuscleGroups = exercise.getMuscleGroups();

                    if (originalMuscleGroups != null && exerciseMuscleGroups != null) {


                        boolean hasAllOriginalMuscles = exerciseMuscleGroups.containsAll(originalMuscleGroups);
                        boolean similarSize = Math.abs(originalMuscleGroups.size() - exerciseMuscleGroups.size()) <= 1;
                        muscleGroupsMatch = hasAllOriginalMuscles && similarSize;
                    }

                    return exerciseTypeMatches && muscleGroupsMatch;
                })
                .collect(Collectors.toList());

        Log.d(TAG, "Найдено похожих упражнений: " + similarExercises.size());

        exerciseAdapter.setExercises(similarExercises);
        updateEmptyState();
    }

    private void filterExercises(String query) {
        if (query == null || query.trim().isEmpty()) {

            exerciseAdapter.setExercises(similarExercises);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            List<Exercise> filteredExercises = similarExercises.stream()
                    .filter(exercise -> {
                        boolean nameMatch = exercise.getName() != null &&
                                exercise.getName().toLowerCase().contains(lowerCaseQuery);

                        boolean muscleGroupMatch = false;
                        List<String> muscleGroups = exercise.getMuscleGroupRussianNames();
                        if (muscleGroups != null) {
                            muscleGroupMatch = muscleGroups.stream()
                                    .anyMatch(muscle -> muscle != null &&
                                            muscle.toLowerCase().contains(lowerCaseQuery));
                        }

                        return nameMatch || muscleGroupMatch;
                    })
                    .collect(Collectors.toList());

            exerciseAdapter.setExercises(filteredExercises);
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = exerciseAdapter.getItemCount() == 0;
        emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        exerciseRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            if (similarExercises.isEmpty()) {
                emptyStateText.setText("Похожие упражнения не найдены");
            } else {
                emptyStateText.setText("Нет упражнений, соответствующих поиску");
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {

            Fragment parentFragment = getParentFragment();
            if (parentFragment instanceof OnExerciseSelectedListener) {
                listener = (OnExerciseSelectedListener) parentFragment;
                Log.d(TAG, "Listener установлен от parentFragment");
            } else if (getTargetFragment() instanceof OnExerciseSelectedListener) {
                listener = (OnExerciseSelectedListener) getTargetFragment();
                Log.d(TAG, "Listener установлен от targetFragment");
            } else {
                Log.w(TAG, "Не удалось найти OnExerciseSelectedListener");
            }
        } catch (ClassCastException e) {
            Log.w(TAG, "Target fragment does not implement OnExerciseSelectedListener", e);
        }
    }

    public void setOnExerciseSelectedListener(OnExerciseSelectedListener listener) {
        this.listener = listener;
    }
}
