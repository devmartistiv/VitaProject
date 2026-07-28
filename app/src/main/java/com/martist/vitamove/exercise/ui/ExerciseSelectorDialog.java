package com.martist.vitamove.exercise.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class ExerciseSelectorDialog extends BottomSheetDialogFragment {

    private static final String TAG = "ExerciseSelectorDialog";


    public interface OnExerciseSelectedListener {
        void onExerciseSelected(Exercise exercise);
    }

    private RecyclerView recyclerView;
    private ExerciseAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private EditText searchEditText;
    private WorkoutRepository repository;
    private final List<Exercise> allExercises = new ArrayList<>();
    private final List<Exercise> filteredExercises = new ArrayList<>();
    private final Executor executor = Executors.newSingleThreadExecutor();
    private OnExerciseSelectedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);


        if (getParentFragment() instanceof OnExerciseSelectedListener) {
            listener = (OnExerciseSelectedListener) getParentFragment();
        } else if (context instanceof OnExerciseSelectedListener) {
            listener = (OnExerciseSelectedListener) context;
        } else {
            throw new RuntimeException(context + " должен реализовать OnExerciseSelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_exercise_selector, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        repository = ((VitaMoveApplication) requireActivity().getApplication()).getWorkoutRepository();


        recyclerView = view.findViewById(R.id.exercise_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyText = view.findViewById(R.id.empty_text);
        searchEditText = view.findViewById(R.id.search_edit_text);


        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ExerciseAdapter(filteredExercises, this::onExerciseClicked);
        recyclerView.setAdapter(adapter);


        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterExercises(s.toString());
            }
        });


        MaterialButton closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dismiss());


        loadExercises();
    }

    private void loadExercises() {
        showLoading(true);

        executor.execute(() -> {
            try {

                List<Exercise> exercises = repository.getAllExercises();

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        allExercises.clear();
                        allExercises.addAll(exercises);
                        filteredExercises.clear();
                        filteredExercises.addAll(exercises);
                        adapter.notifyDataSetChanged();
                        showLoading(false);


                        updateEmptyState();
                    });
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        emptyText.setText("Ошибка загрузки данных: " + e.getMessage());
                        emptyText.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void filterExercises(String query) {

        if (query.isEmpty()) {

            filteredExercises.clear();
            filteredExercises.addAll(allExercises);
        } else {

            String lowerCaseQuery = query.toLowerCase();
            filteredExercises.clear();
            filteredExercises.addAll(allExercises.stream()
                    .filter(exercise -> exercise.getName().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList()));
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        if (filteredExercises.isEmpty()) {
            emptyText.setText("Упражнения не найдены");
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    private void onExerciseClicked(Exercise exercise) {
        if (listener != null) {
            listener.onExerciseSelected(exercise);
        }
        dismiss();
    }


    private static class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

        private final List<Exercise> exercises;
        private final OnExerciseClickListener listener;

        public interface OnExerciseClickListener {
            void onExerciseClick(Exercise exercise);
        }

        public ExerciseAdapter(List<Exercise> exercises, OnExerciseClickListener listener) {
            this.exercises = exercises;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_exercise_simple, parent, false);
            return new ExerciseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
            Exercise exercise = exercises.get(position);
            holder.bind(exercise, listener);
        }

        @Override
        public int getItemCount() {
            return exercises.size();
        }

        static class ExerciseViewHolder extends RecyclerView.ViewHolder {
            private final TextView nameText;
            private final TextView categoryText;

            public ExerciseViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.exercise_name);
                categoryText = itemView.findViewById(R.id.exercise_category);
            }

            public void bind(Exercise exercise, OnExerciseClickListener listener) {
                nameText.setText(exercise.getName());


                List<String> categories = exercise.getCategories();
                if (categories != null && !categories.isEmpty()) {
                    categoryText.setText(categories.get(0));
                    categoryText.setVisibility(View.VISIBLE);
                } else {
                    categoryText.setVisibility(View.GONE);
                }


                itemView.setOnClickListener(v -> listener.onExerciseClick(exercise));
            }
        }
    }
} 