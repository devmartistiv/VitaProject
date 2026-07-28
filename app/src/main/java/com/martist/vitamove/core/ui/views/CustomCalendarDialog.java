package com.martist.vitamove.core.ui.views;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.martist.vitamove.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter;

import org.threeten.bp.DayOfWeek;

import java.util.HashSet;
import java.util.Set;

public class CustomCalendarDialog extends DialogFragment {
    public interface OnDateSelectedListener {
        void onDateSelected(CalendarDay date);
    }

    private final OnDateSelectedListener listener;
    private final CalendarDay initialDate;
    private final Set<CalendarDay> datesWithEntries;

    public CustomCalendarDialog(CalendarDay initialDate, OnDateSelectedListener listener) {
        this(initialDate, listener, new HashSet<>());
    }

    public CustomCalendarDialog(CalendarDay initialDate, OnDateSelectedListener listener, Set<CalendarDay> datesWithEntries) {
        this.initialDate = initialDate;
        this.listener = listener;
        this.datesWithEntries = datesWithEntries != null ? datesWithEntries : new HashSet<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_custom_calendar, null);
        MaterialCalendarView calendarView = view.findViewById(R.id.calendarView);


        String[] monthsArray = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        String[] weekDaysArray = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        calendarView.setTitleFormatter(new MonthArrayTitleFormatter(monthsArray));
        calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(weekDaysArray));
        calendarView.state().edit().setFirstDayOfWeek(DayOfWeek.of(1)).commit();


        if (initialDate != null) {
            calendarView.setSelectedDate(initialDate);
            calendarView.setCurrentDate(initialDate);
        }


        if (!datesWithEntries.isEmpty()) {
            int orangeColor = Color.parseColor("#FF6D00");
            calendarView.addDecorator(new DatesWithEntriesDecorator(datesWithEntries, orangeColor));
        }

        Button btnOk = view.findViewById(R.id.btn_ok);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnOk.setOnClickListener(v -> {
            CalendarDay selected = calendarView.getSelectedDate();
            if (selected != null && listener != null) {
                listener.onDateSelected(selected);
            }
            dismiss();
        });
        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }
} 