package com.martist.vitamove.workout.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.programs.ui.CreateProgramWeekActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dagger.hilt.processor.internal.definecomponent.codegen._dagger_hilt_android_components_ActivityComponent;


public class CreateWorkoutFragment extends Fragment {

    private static final String TAG = "CreateWorkoutFragment";

    private TextInputLayout nameLayout;
    private TextInputEditText nameEditText;
    private AutoCompleteTextView levelSpinner;
    private AutoCompleteTextView weeksSpinner;
    private AutoCompleteTextView daysPerWeekSpinner;
    private Button nextButton;
    private MaterialButton backButton;


    public static CreateWorkoutFragment newInstance() {
        return new CreateWorkoutFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_workout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        initViews(view);


        setupSpinners();


        setupNextButton();


        setupBackButton();
    }


    private void initViews(View view) {
        nameLayout = view.findViewById(R.id.name_input_layout);
        nameEditText = view.findViewById(R.id.name_edit_text);
        levelSpinner = view.findViewById(R.id.level_spinner);
        weeksSpinner = view.findViewById(R.id.weeks_spinner);
        daysPerWeekSpinner = view.findViewById(R.id.days_per_week_spinner);
        nextButton = view.findViewById(R.id.next_button);
        backButton = view.findViewById(R.id.back_button);
    }


    private void setupSpinners() {

        List<String> levels = Arrays.asList(
                getString(R.string.level_beginner),
                getString(R.string.level_intermediate),
                getString(R.string.level_advanced)
        );

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                0,
                levels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.item_portion_dropdown, parent, false);
                }

                TextView textView = view.findViewById(R.id.portion_text);
                View divider = view.findViewById(R.id.portion_divider);

                if (textView != null) {
                    textView.setText(getItem(position));
                }


                if (divider != null) {
                    if (position == getCount() - 1) {
                        divider.setVisibility(View.GONE);
                    } else {
                        divider.setVisibility(View.VISIBLE);
                    }
                }

                return view;
            }
        };
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(levelAdapter);


        List<String> weeks = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            weeks.add(String.valueOf(i));
        }

        ArrayAdapter<String> weeksAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                0,
                weeks
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.item_portion_dropdown, parent, false);
                }

                TextView textView = view.findViewById(R.id.portion_text);
                View divider = view.findViewById(R.id.portion_divider);

                if (textView != null) {
                    textView.setText(getItem(position));
                }


                if (divider != null) {
                    if (position == getCount() - 1) {
                        divider.setVisibility(View.GONE);
                    } else {
                        divider.setVisibility(View.VISIBLE);
                    }
                }

                return view;
            }
        };
        weeksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weeksSpinner.setAdapter(weeksAdapter);


        List<String> daysPerWeek = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            daysPerWeek.add(String.valueOf(i));
        }

        ArrayAdapter<String> daysAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                0,
                daysPerWeek
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.item_portion_dropdown, parent, false);
                }

                TextView textView = view.findViewById(R.id.portion_text);
                View divider = view.findViewById(R.id.portion_divider);

                if (textView != null) {
                    textView.setText(getItem(position));
                }


                if (divider != null) {
                    if (position == getCount() - 1) {
                        divider.setVisibility(View.GONE);
                    } else {
                        divider.setVisibility(View.VISIBLE);
                    }
                }

                return view;
            }
        };
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daysPerWeekSpinner.setAdapter(daysAdapter);
    }


    private void setupNextButton() {
        nextButton.setOnClickListener(v -> {
            if (validateInput()) {
                createWorkoutProgram();
            }
        });
    }


    private void setupBackButton() {
        backButton.setOnClickListener(v -> {

            requireActivity().getSharedPreferences("VitaMovePrefs", 0)
                    .edit()
                    .putInt("workout_tab_index", 2)
                    .apply();

            requireActivity().onBackPressed();
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        requireActivity().getSharedPreferences("VitaMovePrefs", 0)
                                .edit()
                                .putInt("workout_tab_index", 2)
                                .apply();


                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });
    }


    private boolean validateInput() {
        String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";

        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_empty_name));
            return false;
        } else {
            nameLayout.setError(null);
        }

        return true;
    }


    private void createWorkoutProgram() {
        String name = nameEditText.getText().toString().trim();
        String level = "";
        if (levelSpinner.getText().toString().equals("")) {
            Toast.makeText(requireContext(), "Уровень сложности не выбран", Toast.LENGTH_SHORT).show();
            return;
        } else {
            level = levelSpinner.getText().toString();
        }
        int weeks = 0;
        if (weeksSpinner.getText().toString().equals("")) {
            Toast.makeText(requireContext(), "Количество недель не выбрано", Toast.LENGTH_SHORT).show();
            return;
        } else {
            weeks = Integer.parseInt(weeksSpinner.getText().toString());
        }
        int daysPerWeek = 0;
        if (daysPerWeekSpinner.getText().toString().equals("")) {
            Toast.makeText(requireContext(), "Количество дней в неделю не выбрано", Toast.LENGTH_SHORT).show();
            return;
        } else {
            daysPerWeek = Integer.parseInt(daysPerWeekSpinner.getText().toString());
        }


        String userId = ((VitaMoveApplication) requireActivity().getApplication()).getCurrentUserId();


        Intent intent = new Intent(requireActivity(), CreateProgramWeekActivity.class);
        intent.putExtra("NUMBER_OF_DAYS", daysPerWeek);
        intent.putExtra(CreateProgramWeekActivity.EXTRA_TOTAL_WEEKS, weeks);
        intent.putExtra("PROGRAM_NAME", name);
        intent.putExtra("PROGRAM_LEVEL", level);
        intent.putExtra(CreateProgramWeekActivity.EXTRA_NAVIGATE_TO_PROGRAMS, true);
        intent.putExtra("workout_tab_index", 2);
        requireActivity().startActivity(intent);
    }


    private void navigateToProgramSetup(String programId) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
} 