package com.martist.vitamove.measurement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


public class MeasurementHistoryAdapter extends RecyclerView.Adapter<MeasurementHistoryAdapter.ViewHolder> {

    private List<MeasurementRecord> measurements;
    private SimpleDateFormat dateFormat;


    public MeasurementHistoryAdapter(List<MeasurementRecord> measurements) {
        this.measurements = measurements;

        this.dateFormat = new SimpleDateFormat("dd MMMM", new Locale("ru", "RU"));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_measurement_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeasurementRecord measurement = measurements.get(position);


        holder.measurementDate.setText(dateFormat.format(measurement.getDate()));


        if (measurement.hasNote()) {
            holder.measurementNote.setText(measurement.getNote());
            holder.measurementNote.setVisibility(View.VISIBLE);
        } else {
            holder.measurementNote.setVisibility(View.GONE);
        }


        holder.measurementValue.setText(String.format(Locale.getDefault(), "%.1f", measurement.getValue()));


        if (position < measurements.size() - 1) {
            MeasurementRecord previousMeasurement = measurements.get(position + 1);
            float change = measurement.getValue() - previousMeasurement.getValue();

            if (Math.abs(change) > 0.01f) {
                holder.changeIndicator.setVisibility(View.VISIBLE);

                boolean isPositive = change > 0;


                holder.changeIcon.setImageResource(isPositive ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
                int color = holder.itemView.getContext().getResources().getColor(
                        isPositive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark);
                holder.changeIcon.setColorFilter(color);


                String changeText = String.format(Locale.getDefault(), "%+.1f", change);
                holder.changeText.setText(changeText);
                holder.changeText.setTextColor(color);
            } else {
                holder.changeIndicator.setVisibility(View.GONE);
            }
        } else {

            holder.changeIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return measurements != null ? measurements.size() : 0;
    }


    public void updateMeasurements(List<MeasurementRecord> newMeasurements) {
        this.measurements = newMeasurements;
        notifyDataSetChanged();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView measurementDate;
        TextView measurementNote;
        TextView measurementValue;
        LinearLayout changeIndicator;
        ImageView changeIcon;
        TextView changeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            measurementDate = itemView.findViewById(R.id.measurementDate);
            measurementNote = itemView.findViewById(R.id.measurementNote);
            measurementValue = itemView.findViewById(R.id.measurementValue);
            changeIndicator = itemView.findViewById(R.id.changeIndicator);
            changeIcon = itemView.findViewById(R.id.changeIcon);
            changeText = itemView.findViewById(R.id.changeText);
        }
    }
}
