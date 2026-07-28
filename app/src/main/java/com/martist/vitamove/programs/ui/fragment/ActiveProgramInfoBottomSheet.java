package com.martist.vitamove.programs.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.TextUtils;
import com.martist.vitamove.workout.data.model.WorkoutProgram;


public class ActiveProgramInfoBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PROGRAM = "program";

    private WorkoutProgram mProgram;


    public static ActiveProgramInfoBottomSheet newInstance(WorkoutProgram program) {
        ActiveProgramInfoBottomSheet fragment = new ActiveProgramInfoBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PROGRAM, program);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProgram = getArguments().getParcelable(ARG_PROGRAM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_active_program_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mProgram == null) {
            dismiss();
            return;
        }


        TextView titleTextView = view.findViewById(R.id.program_title);
        TextView durationTextView = view.findViewById(R.id.program_duration);
        TextView levelTextView = view.findViewById(R.id.program_level);

        TextView frequencyTextView = view.findViewById(R.id.program_frequency);
        TextView descriptionTextView = view.findViewById(R.id.program_description);


        titleTextView.setText(mProgram.getName());


        String duration = mProgram.getDurationWeeks() + " недель";
        durationTextView.setText(duration);


        levelTextView.setText(mProgram.getLevel());


        String frequency = TextUtils.formatWorkoutFrequency(mProgram.getDaysPerWeek());
        frequencyTextView.setText(frequency);


        String description = mProgram.getDescription();
        if (description != null && !description.isEmpty()) {
            descriptionTextView.setText(description);
        } else {
            descriptionTextView.setText("Описание программы не указано.");
        }
    }

} 