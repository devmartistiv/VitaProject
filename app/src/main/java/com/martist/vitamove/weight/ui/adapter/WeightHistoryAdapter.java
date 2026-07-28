package com.martist.vitamove.weight.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.weight.data.local.entities.UserWeightEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class WeightHistoryAdapter extends RecyclerView.Adapter<WeightHistoryAdapter.WeightViewHolder> {

    private List<UserWeightEntity> weightRecords = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private OnWeightRecordDeleteListener deleteListener;


    public interface OnWeightRecordDeleteListener {
        void onWeightRecordDelete(UserWeightEntity weightRecord);
    }


    public void setOnWeightRecordDeleteListener(OnWeightRecordDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weight_history, parent, false);
        return new WeightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        UserWeightEntity record = weightRecords.get(position);


        holder.weightTextView.setText(String.format(Locale.getDefault(), "%.1f кг", record.getWeight()));


        holder.dateTextView.setText(dateFormat.format(record.getDateAsDate()));
        holder.timeTextView.setText(timeFormat.format(record.getDateAsDate()));


        String notes = record.getNotes();
        if (notes != null && !notes.isEmpty()) {
            holder.notesTextView.setVisibility(View.VISIBLE);
            holder.notesTextView.setText(notes);
        } else {
            holder.notesTextView.setVisibility(View.GONE);
        }


        if (position < weightRecords.size() - 1) {
            float previousWeight = weightRecords.get(position + 1).getWeight();
            float difference = record.getWeight() - previousWeight;

            if (Math.abs(difference) < 0.01f) {

                holder.differenceTextView.setText("±0.0 кг");
                holder.differenceTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorGray));
            } else if (difference > 0) {

                holder.differenceTextView.setText(String.format(Locale.getDefault(), "+%.1f кг", difference));
                holder.differenceTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorRed));
            } else {

                holder.differenceTextView.setText(String.format(Locale.getDefault(), "%.1f кг", difference));
                holder.differenceTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorGreen));
            }

            holder.differenceTextView.setVisibility(View.VISIBLE);
        } else {

            holder.differenceTextView.setVisibility(View.GONE);
        }


        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onWeightRecordDelete(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return weightRecords.size();
    }


    public void setWeightRecords(List<UserWeightEntity> weightRecords) {
        this.weightRecords = weightRecords;
        notifyDataSetChanged();
    }


    public void removeWeightRecord(UserWeightEntity weightRecord) {
        int position = weightRecords.indexOf(weightRecord);
        if (position != -1) {
            weightRecords.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, weightRecords.size() - position);
        }
    }


    static class WeightViewHolder extends RecyclerView.ViewHolder {
        TextView weightTextView;
        TextView dateTextView;
        TextView timeTextView;
        TextView notesTextView;
        TextView differenceTextView;
        ImageButton deleteButton;

        public WeightViewHolder(@NonNull View itemView) {
            super(itemView);
            weightTextView = itemView.findViewById(R.id.weight_value);
            dateTextView = itemView.findViewById(R.id.weight_date);
            timeTextView = itemView.findViewById(R.id.weight_time);
            notesTextView = itemView.findViewById(R.id.weight_notes);
            differenceTextView = itemView.findViewById(R.id.weight_difference);
            deleteButton = itemView.findViewById(R.id.btn_delete_weight);
        }
    }
} 