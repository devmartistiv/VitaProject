package com.martist.vitamove.analytics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;

import java.util.List;


public class MuscleGroupLegendAdapter extends RecyclerView.Adapter<MuscleGroupLegendAdapter.LegendViewHolder> {

    private List<LegendItem> items;

    public MuscleGroupLegendAdapter(List<LegendItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_muscle_group_legend, parent, false);
        return new LegendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LegendViewHolder holder, int position) {
        LegendItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    public void setItems(List<LegendItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }


    static class LegendViewHolder extends RecyclerView.ViewHolder {
        private final View colorIndicator;
        private final TextView nameText;
        private final TextView percentText;
        private final TextView countText;

        public LegendViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
            nameText = itemView.findViewById(R.id.category_name);
            percentText = itemView.findViewById(R.id.percent_text);
            countText = itemView.findViewById(R.id.count_text);
        }

        public void bind(LegendItem item) {
            colorIndicator.setBackgroundColor(item.color);
            nameText.setText(item.name);
            percentText.setText(String.format("%.1f%%", item.percent));
            countText.setText(String.format("%d", item.count));
        }
    }


    public static class LegendItem {
        public String name;
        public int color;
        public float percent;
        public int count;

        public LegendItem(String name, int color, float percent, int count) {
            this.name = name;
            this.color = color;
            this.percent = percent;
            this.count = count;
        }
    }
} 