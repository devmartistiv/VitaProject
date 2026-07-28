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

public class ProgramInfoBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PROGRAM = "program";

    private WorkoutProgram mProgram;

    public static ProgramInfoBottomSheet newInstance(WorkoutProgram program) {
        ProgramInfoBottomSheet fragment = new ProgramInfoBottomSheet();
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
        return inflater.inflate(R.layout.bottom_sheet_program_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mProgram == null) {
            dismiss();
            return;
        }


        TextView titleTextView = view.findViewById(R.id.bottom_sheet_title);
        TextView typeTextView = view.findViewById(R.id.program_type);
        TextView durationTextView = view.findViewById(R.id.bottom_sheet_duration);
        TextView levelTextView = view.findViewById(R.id.bottom_sheet_level);
        TextView categoryTextView = view.findViewById(R.id.program_category);
        TextView descriptionTextView = view.findViewById(R.id.program_description);

        titleTextView.setText(mProgram.getName());


        String type = "Тип: " + (mProgram.getProgramType() != null && !mProgram.getProgramType().isEmpty() ?
                mProgram.getProgramType() : "Не указан");
        typeTextView.setText(type);

        String duration = "Продолжительность: " + TextUtils.formatProgramDuration(
                mProgram.getDurationWeeks(), mProgram.getDaysPerWeek());
        durationTextView.setText(duration);

        String level = "Уровень: " + mProgram.getLevel();
        levelTextView.setText(level);

        String category = "Категория: " + mProgram.getGoal();
        categoryTextView.setText(category);

        descriptionTextView.setText(mProgram.getDescription());
    }
} 