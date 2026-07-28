package com.martist.vitamove.set;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.martist.vitamove.R;
import com.martist.vitamove.workout.data.model.WorkoutExercise;

import java.util.List;

public class AddSupersetDialog extends DialogFragment {
    private RecyclerView recyclerView;
    private List<WorkoutExercise> workoutExercises;
    private SupersetAdapter supersetAdapter;
    private OnSupersetCreatedListener listener;
    private Button addButton;

    public AddSupersetDialog(List<WorkoutExercise> workoutExercises, OnSupersetCreatedListener listener) {
        this.workoutExercises = workoutExercises;
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public interface OnSupersetCreatedListener {
        void onSupersetCreated(List<WorkoutExercise> selectedExercises);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_superset, null);
        recyclerView = view.findViewById(R.id.superset_list);
        addButton = view.findViewById(R.id.add_superset_btn);
        Button cancelButton = view.findViewById(R.id.reject_superset_btn);
        setupRecyclerView();
        updateAddButtonState();


        addButton.setOnClickListener(v -> {
            List<WorkoutExercise> selected = supersetAdapter.getSelectedExercises();
            if (!selected.isEmpty()) {
                if (listener != null) {
                    listener.onSupersetCreated(selected);
                }
                dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
        return new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                .setView(view)
                .create();
    }

    private void setupRecyclerView() {

        supersetAdapter = new SupersetAdapter(exercise -> {

            updateAddButtonState();
        });
        supersetAdapter.setExercises(workoutExercises);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(supersetAdapter);
    }

    private void updateAddButtonState() {

        int selectedCount = supersetAdapter.getSelectedExercises().size();


        addButton.setEnabled(selectedCount >= 2);


        if (selectedCount >= 2) {
            addButton.setText("Создать суперсет (" + selectedCount + ")");
        } else if (selectedCount == 1) {
            addButton.setText("Выберите ещё упражнения");
        } else {
            addButton.setText("Выберите упражнения");
        }
    }
}
