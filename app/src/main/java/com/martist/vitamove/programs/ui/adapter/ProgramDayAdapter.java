package com.martist.vitamove.programs.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.DateUtils;
import com.martist.vitamove.programs.ui.model.ProgramDay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ProgramDayAdapter extends RecyclerView.Adapter<ProgramDayAdapter.DayViewHolder> {
    private final Context context;
    private List<ProgramDay> days;
    private final OnDayClickListener listener;


    public interface OnDayClickListener {
        void onDayClick(ProgramDay day);
    }


    public ProgramDayAdapter(Context context, OnDayClickListener listener) {
        this.context = context;
        this.days = new ArrayList<>();
        this.listener = listener;
    }


    public ProgramDayAdapter(Context context, List<ProgramDay> days, OnDayClickListener listener) {
        this.context = context;
        this.days = days != null ? days : new ArrayList<>();
        this.listener = listener;
    }


    public ProgramDayAdapter(List<ProgramDay> days, OnDayClickListener listener) {
        this.days = days != null ? days : new ArrayList<>();
        this.listener = listener;
        this.context = null;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Context contextToUse = context != null ? context : parent.getContext();
        View view = LayoutInflater.from(contextToUse).inflate(R.layout.item_program_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        ProgramDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }


    public void updateDays(List<ProgramDay> newDays) {
        this.days = newDays != null ? newDays : new ArrayList<>();
        notifyDataSetChanged();
    }


    class DayViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView dayNumber;
        private final TextView dayTitle;
        private final TextView focusAreaText;
        private final TextView exerciseCount;
        private final TextView duration;
        private final ImageView dayStatusIcon;
        private final View completedIndicator;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);


            if (itemView instanceof MaterialCardView) {
                cardView = (MaterialCardView) itemView;
            } else {


                cardView = null;
            }

            dayNumber = itemView.findViewById(R.id.day_number);
            dayTitle = itemView.findViewById(R.id.day_title);
            focusAreaText = itemView.findViewById(R.id.focus_area_text);
            exerciseCount = itemView.findViewById(R.id.exercise_count_text);
            duration = itemView.findViewById(R.id.duration_text);
            dayStatusIcon = itemView.findViewById(R.id.day_status_icon);
            completedIndicator = itemView.findViewById(R.id.completed_indicator);


            if (cardView != null) {
                cardView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onDayClick(days.get(position));
                    }
                });
            } else {
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onDayClick(days.get(position));
                    }
                });
            }
        }


        public void bind(ProgramDay day) {

            Context contextToUse = context != null ? context : itemView.getContext();

            Log.d("ProgramDayAdapter", "Отрисовываем день " + day.getDayNumber() + " со статусом: "
                    + day.getStatus() + ", isCompleted: " + day.isCompleted()
                    + ", ID: " + day.getId());


            if (dayNumber != null) {
                dayNumber.setText(String.valueOf(day.getDayNumber()));
            }


            if (dayTitle != null) {
                String title = day.getName() != null && !day.getName().isEmpty() ? day.getName() : "День " + day.getDayNumber();
                dayTitle.setText(title);
                dayTitle.setVisibility(View.VISIBLE);
            }


            if (focusAreaText != null) {
                long timestamp = day.getPlannedTimestamp();
                if (timestamp > 0) {

                    String formattedDate = DateUtils.formatDayMonthRussian(new Date(timestamp));
                    focusAreaText.setText(formattedDate);
                    focusAreaText.setVisibility(View.VISIBLE);
                } else {

                    String focus = day.getDescription();
                    if (focus != null && !focus.isEmpty()) {
                        focusAreaText.setText(focus);
                        focusAreaText.setVisibility(View.VISIBLE);
                    } else {

                        focusAreaText.setVisibility(View.GONE);
                    }
                }
            }


            if (exerciseCount != null) {
                int exercisesCount = day.getExercises() != null ? day.getExercises().size() : 0;
                if (exercisesCount > 0) {
                    exerciseCount.setText(contextToUse.getResources().getQuantityString(
                            R.plurals.exercise_count, exercisesCount, exercisesCount));
                    exerciseCount.setVisibility(View.VISIBLE);
                } else {
                    exerciseCount.setVisibility(View.GONE);
                }
            }


            if (duration != null) {
                int durationMinutes = day.getDurationMinutes();
                if (durationMinutes > 0) {
                    duration.setText(contextToUse.getString(R.string.duration_minutes, durationMinutes));
                    duration.setVisibility(View.VISIBLE);
                } else {
                    duration.setVisibility(View.GONE);
                }
            }


            if (dayStatusIcon != null && completedIndicator != null) {

                boolean isCompleted = day.isCompleted() || "completed".equals(day.getStatus());
                Log.d("ProgramDayAdapter", "Статус для дня " + day.getDayNumber() + ": "
                        + day.getStatus() + ", isCompleted: " + isCompleted);

                if (isCompleted) {

                    Log.d("ProgramDayAdapter", "Отображаем галочку для дня " + day.getDayNumber());
                    dayStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    dayStatusIcon.setVisibility(View.VISIBLE);
                    completedIndicator.setVisibility(View.VISIBLE);
                } else if ("skipped".equals(day.getStatus())) {

                    Log.d("ProgramDayAdapter", "Отображаем иконку пропуска для дня " + day.getDayNumber());
                    dayStatusIcon.setImageResource(R.drawable.ic_skip_next);
                    dayStatusIcon.setVisibility(View.VISIBLE);
                    completedIndicator.setVisibility(View.GONE);
                } else {

                    Log.d("ProgramDayAdapter", "Скрываем иконки статуса для дня " + day.getDayNumber());
                    dayStatusIcon.setVisibility(View.INVISIBLE);
                    completedIndicator.setVisibility(View.GONE);
                }
            } else {
                Log.d("ProgramDayAdapter", "ОШИБКА: dayStatusIcon или completedIndicator равны null для дня " + day.getDayNumber());
            }
        }
    }
} 