package com.martist.vitamove.programs.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.programs.model.CreateProgramDay;

import java.util.ArrayList;
import java.util.List;


public class CreateWeekDayAdapter extends RecyclerView.Adapter<CreateWeekDayAdapter.DayViewHolder> {


    private List<CreateProgramDay> daysData = new ArrayList<>();
    private final OnDayClickListener dayClickListener;
    private final ExerciseActionCallback exerciseActionCallback;
    private final OnDayTitleEditListener dayTitleEditListener;


    public interface OnDayClickListener {
        void onDayClick(int dayNumber);
    }


    public interface OnDayTitleEditListener {
        void onEditDayTitle(int dayNumber, String currentTitle);
    }


    public interface ExerciseActionCallback {
        void onReplaceExerciseRequest(int dayNumber, int exercisePosition, String exerciseId);
    }


    public CreateWeekDayAdapter(OnDayClickListener dayClickListener,
                                ExerciseActionCallback exerciseActionCallback,
                                OnDayTitleEditListener dayTitleEditListener) {
        this.dayClickListener = dayClickListener;
        this.exerciseActionCallback = exerciseActionCallback;
        this.dayTitleEditListener = dayTitleEditListener;
    }


    public void setDays(List<CreateProgramDay> newDays) {

        this.daysData = newDays != null ? newDays : new ArrayList<>();
        notifyDataSetChanged();
    }


    public List<CreateProgramDay> getDays() {
        return daysData;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_create_program_day, parent, false);
        return new DayViewHolder(view, dayClickListener, exerciseActionCallback, dayTitleEditListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CreateProgramDay day = daysData.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return daysData.size();
    }


    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayTitle;
        TextView dayDescription;
        ImageView editDayTitle;
        RecyclerView rvDayExercises;
        DayExerciseListAdapter exerciseAdapter;
        private final OnDayClickListener dayClickListener;
        private final ExerciseActionCallback exerciseActionCallback;
        private final OnDayTitleEditListener dayTitleEditListener;

        public DayViewHolder(@NonNull View itemView,
                             OnDayClickListener dayClickListener,
                             ExerciseActionCallback exerciseActionCallback,
                             OnDayTitleEditListener dayTitleEditListener) {
            super(itemView);
            this.dayClickListener = dayClickListener;
            this.exerciseActionCallback = exerciseActionCallback;
            this.dayTitleEditListener = dayTitleEditListener;
            dayTitle = itemView.findViewById(R.id.tv_day_title);
            dayDescription = itemView.findViewById(R.id.tv_day_description);
            editDayTitle = itemView.findViewById(R.id.iv_edit_day_title);
            rvDayExercises = itemView.findViewById(R.id.rv_day_exercises);
        }

        public void bind(CreateProgramDay day) {
            dayTitle.setText(day.getTitle());


            editDayTitle.setOnClickListener(v -> {
                if (dayTitleEditListener != null) {
                    dayTitleEditListener.onEditDayTitle(day.getDayNumber(), day.getTitle());
                }
            });

            if (rvDayExercises != null) {


                DayExerciseListAdapter.OnExerciseActionListener innerListener =
                        (position, exerciseId) -> {
                            if (exerciseActionCallback != null) {

                                exerciseActionCallback.onReplaceExerciseRequest(day.getDayNumber(), position, exerciseId);
                            }
                        };


                if (rvDayExercises.getLayoutManager() == null) {
                    rvDayExercises.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                    rvDayExercises.setNestedScrollingEnabled(false);
                }
                exerciseAdapter = new DayExerciseListAdapter(innerListener);
                rvDayExercises.setAdapter(exerciseAdapter);


                List<Exercise> exercises = day.getSelectedExercises();
                if (exercises != null && !exercises.isEmpty()) {
                    dayDescription.setText(String.format("Упражнений: %d", exercises.size()));
                    rvDayExercises.setVisibility(View.VISIBLE);
                    exerciseAdapter.setExercises(exercises);
                } else {
                    dayDescription.setText("Нажмите, чтобы настроить день");
                    rvDayExercises.setVisibility(View.GONE);
                    exerciseAdapter.setExercises(new ArrayList<>());
                }
            }


            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (dayClickListener != null && position != RecyclerView.NO_POSITION) {
                    dayClickListener.onDayClick(day.getDayNumber());
                }
            });
        }
    }
} 