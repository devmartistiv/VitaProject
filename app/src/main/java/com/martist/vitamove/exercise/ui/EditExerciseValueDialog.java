package com.martist.vitamove.exercise.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.martist.vitamove.R;


public class EditExerciseValueDialog extends DialogFragment {

    public static final String ARG_TYPE = "arg_type";
    public static final String ARG_WEIGHT = "arg_weight";
    public static final String ARG_REPS = "arg_reps";
    public static final String ARG_REPS_ONLY = "arg_reps_only";

    public static final int TYPE_INITIAL = 0;
    public static final int TYPE_CURRENT = 1;
    public static final int TYPE_TARGET = 2;

    private OnValueSavedListener listener;
    private int type;
    private float weight;
    private int reps;
    private boolean isRepsOnly = false;


    public static EditExerciseValueDialog newInstance(int type, float weight, int reps) {
        return newInstance(type, weight, reps, false);
    }


    public static EditExerciseValueDialog newInstance(int type, float weight, int reps, boolean isRepsOnly) {
        EditExerciseValueDialog dialog = new EditExerciseValueDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        args.putFloat(ARG_WEIGHT, weight);
        args.putInt(ARG_REPS, reps);
        args.putBoolean(ARG_REPS_ONLY, isRepsOnly);
        dialog.setArguments(args);
        return dialog;
    }


    public interface OnValueSavedListener {
        void onValueSaved(int type, float weight, int reps);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (getParentFragment() instanceof OnValueSavedListener) {
            listener = (OnValueSavedListener) getParentFragment();
        } else {
            throw new RuntimeException(getParentFragment().toString()
                    + " должен реализовывать OnValueSavedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            type = getArguments().getInt(ARG_TYPE);
            weight = getArguments().getFloat(ARG_WEIGHT);
            reps = getArguments().getInt(ARG_REPS);
            isRepsOnly = getArguments().getBoolean(ARG_REPS_ONLY, false);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_exercise_value, null);


        TextView titleView = view.findViewById(R.id.dialog_title);
        String title;
        switch (type) {
            case TYPE_INITIAL:
                title = "Редактирование начального значения";
                break;
            case TYPE_CURRENT:
                title = "Редактирование текущего значения";
                break;
            case TYPE_TARGET:
                title = "Редактирование целевого значения";
                break;
            default:
                title = "Редактирование значения";
        }
        titleView.setText(title);


        TextView weightLabel = view.findViewById(R.id.weight_label);
        EditText weightEdit = view.findViewById(R.id.weight_edit);
        EditText repsEdit = view.findViewById(R.id.reps_edit);


        if (isRepsOnly) {
            if (weightLabel != null) weightLabel.setVisibility(View.GONE);
            weightEdit.setVisibility(View.GONE);
        }


        if (weight > 0) {
            weightEdit.setText(String.valueOf(weight));
        }
        if (reps > 0) {
            repsEdit.setText(String.valueOf(reps));
        }


        Button cancelButton = view.findViewById(R.id.cancel_button);
        Button saveButton = view.findViewById(R.id.save_button);

        cancelButton.setOnClickListener(v -> dismiss());

        saveButton.setOnClickListener(v -> {

            float newWeight = 0;
            int newReps = 0;


            if (!isRepsOnly) {
                String weightStr = weightEdit.getText().toString();
                if (!TextUtils.isEmpty(weightStr)) {
                    try {
                        newWeight = Float.parseFloat(weightStr);
                    } catch (NumberFormatException e) {
                        weightEdit.setError("Введите корректное значение");
                        return;
                    }
                }
            }

            String repsStr = repsEdit.getText().toString();
            if (!TextUtils.isEmpty(repsStr)) {
                try {
                    newReps = Integer.parseInt(repsStr);
                } catch (NumberFormatException e) {
                    repsEdit.setError("Введите корректное значение");
                    return;
                }
            }


            if (isRepsOnly) {

                if (newReps <= 0) {
                    repsEdit.setError("Введите количество повторений");
                    return;
                }
            } else {

                if (newWeight <= 0 && newReps <= 0) {
                    weightEdit.setError("Введите вес или повторения");
                    return;
                }
            }


            listener.onValueSaved(type, newWeight, newReps);
            dismiss();
        });

        builder.setView(view);
        return builder.create();
    }
} 