package com.martist.vitamove.workout.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.workout.data.managers.EquipmentFilterManager;
import com.martist.vitamove.workout.ui.adapters.EquipmentFilterAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EquipmentFilterFragment extends Fragment {

    private static final String TAG = "EquipmentFilterFragment";
    private static final String ARG_EXERCISES = "exercises";

    public interface OnFilterAppliedListener {
        void onFilterApplied(Set<String> selectedEquipment);
    }

    private Toolbar toolbar;
    private EditText equipmentSearchEditText;
    private RecyclerView equipmentRecyclerView;
    private CheckBox selectAllCheckbox;
    private TextView clearAllButton;
    private MaterialButton saveFilterButton;
    private LinearLayout selectAllContainer;

    private EquipmentFilterAdapter adapter;
    private EquipmentFilterManager filterManager;
    private OnFilterAppliedListener listener;
    private List<Exercise> exercises;
    private Set<String> selectedEquipment;


    private List<EquipmentFilterAdapter.EquipmentItem> allEquipmentItems;
    private List<EquipmentFilterAdapter.EquipmentItem> filteredEquipmentItems;

    public static EquipmentFilterFragment newInstance(ArrayList<Exercise> exercises) {
        EquipmentFilterFragment fragment = new EquipmentFilterFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_EXERCISES, exercises);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            exercises = getArguments().getParcelableArrayList(ARG_EXERCISES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_equipment_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupToolbar();
        setupFilterManager();
        setupAdapter();
        setupListeners();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        equipmentSearchEditText = view.findViewById(R.id.equipment_search);
        equipmentRecyclerView = view.findViewById(R.id.equipment_recycler_view);
        selectAllCheckbox = view.findViewById(R.id.select_all_checkbox);
        clearAllButton = view.findViewById(R.id.clear_all_button);
        saveFilterButton = view.findViewById(R.id.save_filter_button);
        selectAllContainer = view.findViewById(R.id.select_all_container);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupFilterManager() {
        filterManager = new EquipmentFilterManager(requireContext());
        selectedEquipment = new HashSet<>(filterManager.getSelectedEquipment());
    }

    private void setupAdapter() {
        allEquipmentItems = filterManager.getAvailableEquipment(exercises);
        filteredEquipmentItems = new ArrayList<>(allEquipmentItems);


        if (filterManager.isFirstLaunch()) {
            selectedEquipment = filterManager.selectAllEquipment(allEquipmentItems);
        }

        adapter = new EquipmentFilterAdapter(filteredEquipmentItems, selectedEquipment);

        adapter.setOnEquipmentSelectionChangedListener(newSelectedEquipment -> {

            updateSelectAllCheckbox();
        });

        equipmentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        equipmentRecyclerView.setAdapter(adapter);

        updateSelectAllCheckbox();
    }

    private void setupListeners() {

        equipmentSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEquipmentList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        selectAllContainer.setOnClickListener(v -> {
            boolean newState = !selectAllCheckbox.isChecked();
            selectAllCheckbox.setChecked(newState);

            if (newState) {
                adapter.selectAll();
            } else {
                adapter.clearAll();
            }
        });

        selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearAll();
            }
        });

        clearAllButton.setOnClickListener(v -> {
            adapter.clearAll();
            updateSelectAllCheckbox();
        });

        saveFilterButton.setOnClickListener(v -> {
            Log.d(TAG, "Применение фильтра. Выбрано оборудования: " + selectedEquipment.size());
            for (String equipment : selectedEquipment) {
                Log.d(TAG, "Выбранное оборудование: " + equipment);
            }


            filterManager.saveSelectedEquipment(selectedEquipment);


            if (listener != null) {
                listener.onFilterApplied(selectedEquipment);
                Log.d(TAG, "Слушатель уведомлен о применении фильтра");
            } else {
                Log.w(TAG, "Слушатель не установлен!");
            }


            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }


    private void filterEquipmentList(String query) {
        if (query == null || query.trim().isEmpty()) {

            filteredEquipmentItems.clear();
            filteredEquipmentItems.addAll(allEquipmentItems);
        } else {

            String queryLower = query.toLowerCase().trim();
            filteredEquipmentItems.clear();

            for (EquipmentFilterAdapter.EquipmentItem item : allEquipmentItems) {
                if (item.getName().toLowerCase().contains(queryLower)) {
                    filteredEquipmentItems.add(item);
                }
            }
        }


        adapter.updateEquipmentList(filteredEquipmentItems);
        updateSelectAllCheckbox();

        Log.d(TAG, "Поиск: '" + query + "', найдено: " + filteredEquipmentItems.size() + " элементов");
    }

    private void updateSelectAllCheckbox() {
        selectAllCheckbox.setOnCheckedChangeListener(null);
        selectAllCheckbox.setChecked(adapter.isAllSelected());
        selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearAll();
            }
        });
    }
}
