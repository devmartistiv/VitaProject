package com.martist.vitamove.programs.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.martist.vitamove.R;

import java.util.ArrayList;
import java.util.List;


public class ProgramDaysConfigDialog extends BottomSheetDialogFragment {
    private static final String TAG = "ProgramDaysConfigDialog";
    private static final String ARG_PROGRAM_ID = "program_id";
    private static final String ARG_CURRENT_DAYS = "current_days";
    private static final String ARG_DAYS_PER_WEEK = "days_per_week";

    private String programId;
    private ArrayList<Integer> currentDays;
    private int daysPerWeek;

    private ChipGroup daysOfWeekChipGroup;
    private TextView daysSelectionHintText;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;

    private OnDaysUpdatedListener listener;


    public interface OnDaysUpdatedListener {
        void onDaysUpdated(List<Integer> newDays);
    }


    public static ProgramDaysConfigDialog newInstance(String programId, List<Integer> currentDays, int daysPerWeek) {
        ProgramDaysConfigDialog fragment = new ProgramDaysConfigDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_ID, programId);
        args.putIntegerArrayList(ARG_CURRENT_DAYS, currentDays != null ? new ArrayList<>(currentDays) : new ArrayList<>());
        args.putInt(ARG_DAYS_PER_WEEK, daysPerWeek);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            programId = getArguments().getString(ARG_PROGRAM_ID);
            currentDays = getArguments().getIntegerArrayList(ARG_CURRENT_DAYS);
            daysPerWeek = getArguments().getInt(ARG_DAYS_PER_WEEK);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), getTheme());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_program_days_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupCurrentSelection();
        setupClickListeners();
        updateDaysSelectionHint();
    }


    public void setOnDaysUpdatedListener(OnDaysUpdatedListener listener) {
        this.listener = listener;
    }


    private void initViews(View view) {
        daysOfWeekChipGroup = view.findViewById(R.id.days_of_week_chip_group);
        daysSelectionHintText = view.findViewById(R.id.days_selection_hint);
        saveButton = view.findViewById(R.id.btn_save);
        cancelButton = view.findViewById(R.id.btn_cancel);
    }


    private void setupCurrentSelection() {
        if (currentDays == null || currentDays.isEmpty()) {
            return;
        }


        for (Integer dayIndex : currentDays) {
            Chip dayChip = getDayChip(dayIndex);
            if (dayChip != null) {
                dayChip.setChecked(true);
            }
        }
    }


    private void setupClickListeners() {

        daysOfWeekChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateDaysSelectionHint();
        });


        saveButton.setOnClickListener(v -> saveChanges());


        cancelButton.setOnClickListener(v -> dismiss());
    }


    private void saveChanges() {

        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < daysOfWeekChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) daysOfWeekChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                int dayIndex = getDayIndex(chip.getId());
                selectedDays.add(dayIndex);
            }
        }


        if (selectedDays.size() != daysPerWeek) {
            showDaysSelectionErrorDialog(daysPerWeek);
            return;
        }


        if (listener != null) {
            listener.onDaysUpdated(selectedDays);
        }
        dismiss();
    }


    private void updateDaysSelectionHint() {
        if (daysSelectionHintText == null) {
            return;
        }


        int selectedCount = 0;
        for (int i = 0; i < daysOfWeekChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) daysOfWeekChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedCount++;
            }
        }

        String declension = getDeclensionForDays(daysPerWeek);
        String hint = String.format("Выберите %d %s для тренировок", daysPerWeek, declension);


        if (selectedCount == daysPerWeek) {
            daysSelectionHintText.setVisibility(View.GONE);
        } else {
            daysSelectionHintText.setVisibility(View.VISIBLE);
            daysSelectionHintText.setTextColor(requireContext().getResources().getColor(R.color.gray_600));
            daysSelectionHintText.setText(hint);
        }
    }


    private void showDaysSelectionErrorDialog(int requiredDaysCount) {
        String declension = getDeclensionForDays(requiredDaysCount);
        String message = String.format("Для этой программы необходимо выбрать %d %s тренировок.\n\nПожалуйста, измените выбор дней недели.",
                requiredDaysCount, declension);


        highlightDaysChips(true);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Неверное количество дней")
                .setMessage(message)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("Понятно", (dialogInterface, i) -> {

                    highlightDaysChips(false);
                })
                .create();

        dialog.setOnDismissListener(dialogInterface -> {

            highlightDaysChips(false);
        });

        dialog.show();
    }


    private void highlightDaysChips(boolean highlight) {
        for (int i = 0; i < daysOfWeekChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) daysOfWeekChipGroup.getChildAt(i);
            if (highlight) {

                chip.setTag(R.id.tag_stroke_color, chip.getChipStrokeColor());
                chip.setChipStrokeColor(getColorStateList(R.color.error_color));
                chip.setChipStrokeWidth(2f);
            } else {

                if (chip.getTag(R.id.tag_stroke_color) != null) {
                    chip.setChipStrokeColor((android.content.res.ColorStateList) chip.getTag(R.id.tag_stroke_color));
                } else {
                    chip.setChipStrokeColor(getColorStateList(R.color.colorPrimary));
                }
                chip.setChipStrokeWidth(1f);
            }
        }
    }


    private String getDeclensionForDays(int number) {
        int remainder10 = number % 10;
        int remainder100 = number % 100;

        if (remainder10 == 1 && remainder100 != 11) {
            return "день";
        } else if (remainder10 >= 2 && remainder10 <= 4 && (remainder100 < 10 || remainder100 >= 20)) {
            return "дня";
        } else {
            return "дней";
        }
    }


    private int getDayIndex(int chipId) {
        if (chipId == R.id.chip_monday) return 0;
        if (chipId == R.id.chip_tuesday) return 1;
        if (chipId == R.id.chip_wednesday) return 2;
        if (chipId == R.id.chip_thursday) return 3;
        if (chipId == R.id.chip_friday) return 4;
        if (chipId == R.id.chip_saturday) return 5;
        if (chipId == R.id.chip_sunday) return 6;
        return 0;
    }


    private Chip getDayChip(int dayIndex) {
        View view = getView();
        if (view == null) return null;

        switch (dayIndex) {
            case 0:
                return view.findViewById(R.id.chip_monday);
            case 1:
                return view.findViewById(R.id.chip_tuesday);
            case 2:
                return view.findViewById(R.id.chip_wednesday);
            case 3:
                return view.findViewById(R.id.chip_thursday);
            case 4:
                return view.findViewById(R.id.chip_friday);
            case 5:
                return view.findViewById(R.id.chip_saturday);
            case 6:
                return view.findViewById(R.id.chip_sunday);
            default:
                return null;
        }
    }

    private android.content.res.ColorStateList getColorStateList(int colorRes) {
        return android.content.res.ColorStateList.valueOf(requireContext().getResources().getColor(colorRes));
    }
}

