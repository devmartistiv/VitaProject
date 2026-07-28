package com.martist.vitamove.workout.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EquipmentFilterAdapter extends RecyclerView.Adapter<EquipmentFilterAdapter.EquipmentViewHolder> {

    private List<EquipmentItem> equipmentList;
    private Set<String> selectedEquipment;
    private OnEquipmentSelectionChangedListener listener;

    public interface OnEquipmentSelectionChangedListener {
        void onSelectionChanged(Set<String> selectedEquipment);
    }

    public static class EquipmentItem {
        private final String name;
        private final int exerciseCount;

        public EquipmentItem(String name, int exerciseCount) {
            this.name = name;
            this.exerciseCount = exerciseCount;
        }

        public String getName() {
            return name;
        }

        public int getExerciseCount() {
            return exerciseCount;
        }
    }

    public EquipmentFilterAdapter(List<EquipmentItem> equipmentList, Set<String> selectedEquipment) {
        this.equipmentList = equipmentList != null ? equipmentList : new ArrayList<>();
        this.selectedEquipment = selectedEquipment;
    }

    public void setOnEquipmentSelectionChangedListener(OnEquipmentSelectionChangedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment_filter, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        EquipmentItem item = equipmentList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    public void selectAll() {
        selectedEquipment.clear();
        for (EquipmentItem item : equipmentList) {
            selectedEquipment.add(item.getName());
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void clearAll() {
        selectedEquipment.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public boolean isAllSelected() {
        return selectedEquipment.size() == equipmentList.size();
    }


    public void updateEquipmentList(List<EquipmentItem> newEquipmentList) {
        this.equipmentList = newEquipmentList != null ? newEquipmentList : new ArrayList<>();
        notifyDataSetChanged();
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(selectedEquipment);
        }
    }

    class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkbox;
        private final TextView equipmentName;
        private final TextView exerciseCount;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.equipment_checkbox);
            equipmentName = itemView.findViewById(R.id.equipment_name);
            exerciseCount = itemView.findViewById(R.id.exercise_count);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    EquipmentItem item = equipmentList.get(position);
                    if (selectedEquipment.contains(item.getName())) {
                        selectedEquipment.remove(item.getName());
                    } else {
                        selectedEquipment.add(item.getName());
                    }
                    checkbox.setChecked(selectedEquipment.contains(item.getName()));
                    notifySelectionChanged();
                }
            });
        }

        public void bind(EquipmentItem item) {
            equipmentName.setText(item.getName());
            exerciseCount.setText(String.valueOf(item.getExerciseCount()));
            checkbox.setChecked(selectedEquipment.contains(item.getName()));
        }
    }
}
