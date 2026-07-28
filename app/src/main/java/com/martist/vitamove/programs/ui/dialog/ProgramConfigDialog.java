package com.martist.vitamove.programs.ui.dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.AsyncCallback;
import com.martist.vitamove.programs.data.ProgramManager;
import com.martist.vitamove.workout.data.model.WorkoutProgram;

import java.util.ArrayList;
import java.util.List;


public class ProgramConfigDialog extends BottomSheetDialogFragment {
    private static final String ARG_PROGRAM_ID = "program_id";
    private static final String TAG = "ProgramConfigDialog";

    private String programId;
    private ProgramManager programManager;
    private WorkoutProgram program;

    private final CheckBox[] dayCheckboxes = new CheckBox[7];
    private final List<Integer> selectedDays = new ArrayList<>();

    public static ProgramConfigDialog newInstance(String programId) {
        ProgramConfigDialog fragment = new ProgramConfigDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_ID, programId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            programId = getArguments().getString(ARG_PROGRAM_ID);
        }

        programManager = new ProgramManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_program_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        dayCheckboxes[0] = view.findViewById(R.id.checkbox_monday);
        dayCheckboxes[1] = view.findViewById(R.id.checkbox_tuesday);
        dayCheckboxes[2] = view.findViewById(R.id.checkbox_wednesday);
        dayCheckboxes[3] = view.findViewById(R.id.checkbox_thursday);
        dayCheckboxes[4] = view.findViewById(R.id.checkbox_friday);
        dayCheckboxes[5] = view.findViewById(R.id.checkbox_saturday);
        dayCheckboxes[6] = view.findViewById(R.id.checkbox_sunday);


        MaterialButton saveButton = view.findViewById(R.id.button_save);
        MaterialButton cancelButton = view.findViewById(R.id.button_cancel);

        saveButton.setOnClickListener(v -> saveConfig());
        cancelButton.setOnClickListener(v -> dismiss());


        loadProgram();
    }

    private void loadProgram() {

        if (getView() != null) {
            View loadingView = getView().findViewById(R.id.loading_view);
            if (loadingView != null) {
                loadingView.setVisibility(View.VISIBLE);
            }
        }


        programManager.getProgramByIdAsync(programId, new AsyncCallback<WorkoutProgram>() {
            @Override
            public void onSuccess(WorkoutProgram result) {
                program = result;
                if (program != null) {

                    setupDays(program.getWorkoutDays());
                }


                if (getView() != null) {
                    View loadingView = getView().findViewById(R.id.loading_view);
                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Ошибка загрузки программы: " + e.getMessage(), e);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Ошибка загрузки программы", Toast.LENGTH_SHORT).show();
                    dismiss();
                }


                if (getView() != null) {
                    View loadingView = getView().findViewById(R.id.loading_view);
                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void setupDays(List<Integer> workoutDays) {
        if (workoutDays != null && !workoutDays.isEmpty()) {
            for (Integer day : workoutDays) {
                if (day >= 0 && day < 7) {
                    dayCheckboxes[day].setChecked(true);
                }
            }
        } else {

            dayCheckboxes[0].setChecked(true);
            dayCheckboxes[2].setChecked(true);
            dayCheckboxes[4].setChecked(true);
        }
    }

    private void saveConfig() {

        selectedDays.clear();
        for (int i = 0; i < dayCheckboxes.length; i++) {
            if (dayCheckboxes[i].isChecked()) {
                selectedDays.add(i);
            }
        }


        if (selectedDays.isEmpty()) {
            Toast.makeText(requireContext(), "Выберите хотя бы один день тренировки", Toast.LENGTH_SHORT).show();
            return;
        }


        try {
            programManager.updateProgramWorkoutDaysAsync(programId, selectedDays, new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show();
                            dismiss();
                        });
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Ошибка при сохранении настроек", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Ошибка при сохранении настроек: " + e.getMessage(), e);
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении настроек: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Ошибка при сохранении настроек", Toast.LENGTH_SHORT).show();
        }
    }
} 