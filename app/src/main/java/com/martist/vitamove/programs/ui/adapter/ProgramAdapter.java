package com.martist.vitamove.programs.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.TextUtils;
import com.martist.vitamove.programs.ui.model.ProgramType;
import com.martist.vitamove.workout.data.model.WorkoutProgram;

import java.util.List;

public class ProgramAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "ProgramAdapter";
    private static final int VIEW_TYPE_ACTIVE = 1;
    private static final int VIEW_TYPE_INACTIVE = 2;

    private List<WorkoutProgram> programs;
    private final OnProgramClickListener listener;
    private final Context context;

    public interface OnProgramClickListener {
        void onProgramClick(WorkoutProgram program);

        void onStartClick(WorkoutProgram program);

        void onDetailsClick(WorkoutProgram program);

        void onSetupClick(WorkoutProgram program);

        void onDeleteClick(WorkoutProgram program);
    }

    public ProgramAdapter(List<WorkoutProgram> programs, OnProgramClickListener listener, Context context) {
        this.programs = programs;
        this.listener = listener;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return programs.get(position).isActive() ? VIEW_TYPE_ACTIVE : VIEW_TYPE_INACTIVE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_program_inactive, parent, false);
        return new InactiveProgramViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        WorkoutProgram program = programs.get(position);


        if (holder instanceof InactiveProgramViewHolder) {
            ((InactiveProgramViewHolder) holder).bind(program, listener);
        }
    }

    @Override
    public int getItemCount() {
        return programs != null ? programs.size() : 0;
    }

    public void updatePrograms(List<WorkoutProgram> newPrograms) {
        Log.d(TAG, "updatePrograms: Обновление списка программ, размер: " +
                (newPrograms != null ? newPrograms.size() : "null"));

        if (newPrograms != null) {
            for (int i = 0; i < newPrograms.size(); i++) {
                WorkoutProgram program = newPrograms.get(i);
                Log.d(TAG, "Программа #" + i + ": " + program.getName() +
                        ", id: " + program.getId() + ", активна: " + program.isActive());
            }
        }

        this.programs = newPrograms;
        notifyDataSetChanged();
    }


    abstract class BaseProgramViewHolder extends RecyclerView.ViewHolder {
        protected final TextView nameTextView;
        protected final TextView levelTextView;
        protected final TextView durationTextView;
        protected final TextView descriptionTextView;
        protected final ImageView programImageView;
        protected final ChipGroup goalChipGroup;
        protected TextView ownershipText;

        public BaseProgramViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.program_name);
            levelTextView = itemView.findViewById(R.id.program_level);
            durationTextView = itemView.findViewById(R.id.program_duration);
            descriptionTextView = itemView.findViewById(R.id.program_description);
            programImageView = itemView.findViewById(R.id.program_image);
            goalChipGroup = itemView.findViewById(R.id.goal_chip_group);
            ownershipText = itemView.findViewById(R.id.program_ownership_text);
        }


        protected void bindCommon(WorkoutProgram program, OnProgramClickListener listener) {
            nameTextView.setText(program.getName());
            levelTextView.setText(program.getLevel());
            durationTextView.setText(TextUtils.formatProgramDuration(
                    program.getDurationWeeks(), program.getDaysPerWeek()));
            descriptionTextView.setText(program.getDescription());


            if (program.getImageUrl() != null && !program.getImageUrl().isEmpty()) {

                Glide.with(itemView.getContext())
                        .load(program.getImageUrl())
                        .placeholder(getPlaceholderDrawable(program))
                        .error(getPlaceholderDrawable(program))
                        .centerCrop()
                        .into(programImageView);
            } else {

                programImageView.setImageResource(getPlaceholderDrawable(program));
            }


            setupGoalChips(program);


            if (ownershipText != null) {

                ownershipText.setVisibility(View.GONE);
            }


            itemView.setOnClickListener(v -> listener.onProgramClick(program));


        }


        private void setupGoalChips(WorkoutProgram program) {
            goalChipGroup.removeAllViews();
            if (program.getType() == ProgramType.USER_CREATED) {
                Chip userChip = new Chip(
                        itemView.getContext(),
                        null,
                        com.google.android.material.R.style.Widget_Material3_Chip_Assist
                );
                userChip.setText("Пользовательская");
                userChip.setChipBackgroundColorResource(R.color.colorPrimary);
                userChip.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                goalChipGroup.addView(userChip);
            }


            String programType = program.getProgramType();
            if (programType != null && !programType.isEmpty() && !programType.equalsIgnoreCase("null")) {
                if (!programType.equals("USER_CREATED")) {

                    Chip typeChip = new Chip(
                            itemView.getContext(),
                            null,
                            com.google.android.material.R.style.Widget_Material3_Chip_Assist
                    );

                    typeChip.setText(programType);
                    typeChip.setChipBackgroundColor(null);
                    typeChip.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));

                    goalChipGroup.addView(typeChip);
                }
            }

            String goal = program.getGoal();
            if (goal != null && !goal.isEmpty() && !goal.equalsIgnoreCase("null")) {

                Chip goalChip = new Chip(
                        itemView.getContext(),
                        null,
                        com.google.android.material.R.style.Widget_Material3_Chip_Assist
                );

                goalChip.setText(goal);
                goalChip.setChipBackgroundColor(null);
                goalChip.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));

                goalChipGroup.addView(goalChip);
            }


            if (goalChipGroup.getChildCount() == 0) {
                goalChipGroup.setVisibility(View.GONE);
            } else {
                goalChipGroup.setVisibility(View.VISIBLE);
            }
        }


        private int getPlaceholderDrawable(WorkoutProgram program) {

            if (program.getType() == ProgramType.USER_CREATED) {
                return R.drawable.workout_card_gradient_background_custom;
            }

            return R.drawable.workout_card_gradient_background_2;
        }
    }


    class InactiveProgramViewHolder extends BaseProgramViewHolder {
        private final MaterialButton configureButton;
        private final MaterialButton detailsButton;

        public InactiveProgramViewHolder(@NonNull View itemView) {
            super(itemView);
            configureButton = itemView.findViewById(R.id.configure_button);
            detailsButton = itemView.findViewById(R.id.details_button);
        }

        public void bind(WorkoutProgram program, OnProgramClickListener listener) {
            bindCommon(program, listener);


            configureButton.setOnClickListener(v -> listener.onSetupClick(program));


            if (program.getType() == ProgramType.USER_CREATED) {
                detailsButton.setText("Удалить");
                detailsButton.setOnClickListener(v -> listener.onDeleteClick(program));
            } else {
                detailsButton.setText("Подробнее");
                detailsButton.setOnClickListener(v -> listener.onDetailsClick(program));
            }
        }
    }
} 