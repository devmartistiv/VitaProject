package com.martist.vitamove.nutrition.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.nutrition.ui.model.NutrientType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public class NutrientSelectionAdapter extends RecyclerView.Adapter<NutrientSelectionAdapter.ViewHolder> {

    private final NutrientType[] nutrientTypes;

    private final Set<NutrientType> selected;

    public NutrientSelectionAdapter(NutrientType.Group group, Set<NutrientType> sharedSelectedSet) {
        this.selected = sharedSelectedSet;

        this.nutrientTypes = Arrays.stream(NutrientType.values())
                .filter(type -> type.getGroup() == group)
                .toArray(NutrientType[]::new);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.nutrient_selection_check_box);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nutrient_selection_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NutrientType type = nutrientTypes[position];

        holder.checkBox.setText(type.getLocalizedName());


        holder.checkBox.setOnCheckedChangeListener(null);


        holder.checkBox.setChecked(selected.contains(type));


        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selected.add(type);
            else selected.remove(type);
        });


        holder.itemView.setOnClickListener(v -> holder.checkBox.performClick());
    }

    @Override
    public int getItemCount() {
        return nutrientTypes.length;
    }
}
