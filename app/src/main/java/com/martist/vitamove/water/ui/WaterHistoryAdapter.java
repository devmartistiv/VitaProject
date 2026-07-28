package com.martist.vitamove.water.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.water.data.model.WaterConsumptionRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class WaterHistoryAdapter extends RecyclerView.Adapter<WaterHistoryAdapter.ViewHolder> {

    private List<WaterConsumptionRecord> waterRecords = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private OnDeleteClickListener deleteListener;


    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }


    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }


    public void setWaterRecords(List<WaterConsumptionRecord> waterRecords) {
        this.waterRecords = waterRecords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_water_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaterConsumptionRecord record = waterRecords.get(position);


        holder.timeText.setText(timeFormat.format(record.getTimestamp()));


        holder.amountText.setText(String.format(Locale.getDefault(),
                "%.0f мл", record.getAmount() * 1000));


        if (record.getDescription() != null && !record.getDescription().isEmpty()) {
            holder.descriptionText.setText(record.getDescription());
            holder.descriptionText.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionText.setVisibility(View.GONE);
        }


        if (deleteListener != null) {
            holder.deleteButton.setOnClickListener(v -> {
                deleteListener.onDeleteClick(holder.getAdapterPosition());
            });
        }
    }

    @Override
    public int getItemCount() {
        return waterRecords.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        TextView amountText;
        TextView descriptionText;
        ImageView deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.text_time);
            amountText = itemView.findViewById(R.id.text_amount);
            descriptionText = itemView.findViewById(R.id.text_description);
            deleteButton = itemView.findViewById(R.id.btn_delete_record);
        }
    }
} 