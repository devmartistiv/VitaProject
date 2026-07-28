package com.martist.vitamove.nutrition.ui;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.martist.vitamove.R;
import com.martist.vitamove.nutrition.domain.events.TrackedNutrientsChangedEvent;
import com.martist.vitamove.nutrition.ui.adapter.NutrientSelectionAdapter;
import com.martist.vitamove.nutrition.ui.model.NutrientType;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NutrientSelectionBottomSheet extends BottomSheetDialogFragment {

    private static final int MAX_SELECTIONS = 3;

    private NutrientSelectionAdapter vitaminsAdapter;
    private NutrientSelectionAdapter mineralsAdapter;
    private NutrientSelectionAdapter othersAdapter;

    private final Set<NutrientType> selectedNutrients = new HashSet<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_nutrient_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", MODE_PRIVATE);
        Set<String> savedStrings = prefs.getStringSet("track_nutrients", Collections.emptySet());

        selectedNutrients.clear();
        if (!savedStrings.isEmpty()) {
            selectedNutrients.addAll(savedStrings.stream()
                    .map(NutrientType::valueOf)
                    .collect(Collectors.toSet()));
        }

        vitaminsAdapter = new NutrientSelectionAdapter(NutrientType.Group.VITAMIN, selectedNutrients);
        mineralsAdapter = new NutrientSelectionAdapter(NutrientType.Group.MINERAL, selectedNutrients);
        othersAdapter = new NutrientSelectionAdapter(NutrientType.Group.OTHER, selectedNutrients);

        RecyclerView vitaminsList = view.findViewById(R.id.vitamins_selection_list);
        RecyclerView mineralsList = view.findViewById(R.id.mineral_selection_list);
        RecyclerView othersList = view.findViewById(R.id.additional_selection_list);

        vitaminsList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        vitaminsList.setAdapter(vitaminsAdapter);
        mineralsList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mineralsList.setAdapter(mineralsAdapter);
        othersList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        othersList.setAdapter(othersAdapter);


        Button saveButton = view.findViewById(R.id.save_button);
        Button cancelButton = view.findViewById(R.id.cancel_button);

        saveButton.setOnClickListener(v -> {
            saveSelectedNutrients();
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        }
    }


    private void saveSelectedNutrients() {
        List<String> selectedNames = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();

        for (NutrientType nutrient : selectedNutrients) {
            selectedIds.add(nutrient.name());
            selectedNames.add(nutrient.getLocalizedName());
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putStringSet("track_nutrients", selectedIds).apply();

        EventBus.getDefault().post(new TrackedNutrientsChangedEvent(new ArrayList<>(selectedNutrients)));

        String message;
        if (selectedNutrients.isEmpty()) {
            message = "Отслеживание нутриентов отключено";
        } else {
            message = "Отслеживаемые нутриенты: " + String.join(", ", selectedNames);
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
