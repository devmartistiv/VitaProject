package com.martist.vitamove.programs.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.AsyncCallback;
import com.martist.vitamove.core.domain.utils.TextUtils;
import com.martist.vitamove.programs.data.ProgramManager;
import com.martist.vitamove.programs.ui.adapter.ProgramDayAdapter;
import com.martist.vitamove.programs.ui.model.ProgramDay;
import com.martist.vitamove.workout.data.model.WorkoutProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProgramDetailsFragment extends Fragment {
    private static final String ARG_PROGRAM_ID = "program_id";
    private static final String TAG = "ProgramDetailsFragment";

    private String programId;
    private ProgramManager programManager;
    private WorkoutProgram program;
    private RecyclerView daysRecyclerView;
    private ProgramDayAdapter dayAdapter;
    private TextView programNameText;
    private TextView programDescriptionText;
    private TextView programTypeText;
    private TextView programDurationText;
    private FloatingActionButton favoriteButton;

    public static ProgramDetailsFragment newInstance(String programId) {
        ProgramDetailsFragment fragment = new ProgramDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_ID, programId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            programId = getArguments().getString(ARG_PROGRAM_ID);
        }
        programManager = new ProgramManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_program_details, container, false);

        programNameText = view.findViewById(R.id.program_name);
        programDescriptionText = view.findViewById(R.id.program_description);
        programTypeText = view.findViewById(R.id.program_level);
        programDurationText = view.findViewById(R.id.program_duration);
        daysRecyclerView = view.findViewById(R.id.days_recycler_view);
        favoriteButton = view.findViewById(R.id.favorite_fab);


        View backButton = view.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> navigateBackToProgramsTab());
        }

        setupRecyclerView();
        setupClickListeners();
        loadProgramDetails();

        return view;
    }

    private void setupRecyclerView() {
        dayAdapter = new ProgramDayAdapter(new ArrayList<>(), day -> {

            showDayDetails(day);
        });

        daysRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        daysRecyclerView.setAdapter(dayAdapter);
    }

    private void setupClickListeners() {


    }

    private void loadProgramDetails() {
        if (programId == null || programId.isEmpty()) {
            showError("ID программы отсутствует");
            return;
        }

        showLoading(true);

        programManager.getProgramByIdAsync(programId, new AsyncCallback<WorkoutProgram>() {
            @Override
            public void onSuccess(WorkoutProgram result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (result != null) {
                            program = result;
                            updateUI();
                            loadProgramDays();
                        } else {
                            showError("Программа не найдена");
                            showLoading(false);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showError("Ошибка при загрузке программы: " + e.getMessage());
                        showLoading(false);
                    });
                }
            }
        });
    }


    public void loadProgramDays() {
        if (program == null || program.getId() == null) {
            showError("Программа не найдена");
            return;
        }

        Log.d(TAG, "Загрузка дней программы: " + program.getId());


        ProgressBar dayProgressBar = requireView().findViewById(R.id.days_progress_bar);
        if (dayProgressBar != null) {
            dayProgressBar.setVisibility(View.VISIBLE);
        }


        programManager.getProgramDaysAsync(program.getId(), new AsyncCallback<List<ProgramDay>>() {
            @Override
            public void onSuccess(List<ProgramDay> days) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "Дни программы загружены. Количество: " + days.size());


                        for (ProgramDay day : days) {
                            Log.d(TAG, "День программы: id=" + day.getId() +
                                    ", номер=" + day.getDayNumber() +
                                    ", название='" + day.getName() + "'" +
                                    ", программа=" + day.getProgramId());
                        }


                        Collections.sort(days, Comparator.comparingInt(ProgramDay::getDayNumber));


                        dayAdapter.updateDays(days);


                        if (dayProgressBar != null) {
                            dayProgressBar.setVisibility(View.GONE);
                        }


                        if (days.isEmpty()) {

                            TextView emptyDaysTextView = requireView().findViewById(R.id.empty_days_text);
                            if (emptyDaysTextView != null) {
                                emptyDaysTextView.setVisibility(View.VISIBLE);
                            }
                        }


                        showLoading(false);
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Ошибка при загрузке дней программы: " + e.getMessage(), e);


                        showError("Ошибка при загрузке дней программы: " + e.getMessage());


                        if (dayProgressBar != null) {
                            dayProgressBar.setVisibility(View.GONE);
                        }


                        showLoading(false);
                    });
                }
            }
        });
    }

    private void updateUI() {
        if (program == null) {
            showError("Программа не инициализирована");
            showLoading(false);
            return;
        }

        try {

            programNameText.setText(program.getName());


            if (program.getDescription() != null && !program.getDescription().isEmpty()) {
                programDescriptionText.setText(program.getDescription());
                programDescriptionText.setVisibility(View.VISIBLE);
            } else {
                programDescriptionText.setVisibility(View.GONE);
            }


            String levelText = "Уровень: " + (program.getLevel() != null ? program.getLevel() : "не указан");
            programTypeText.setText(levelText);


            String durationText = TextUtils.formatProgramDuration(
                    program.getDurationWeeks() > 0 ? program.getDurationWeeks() : 4,
                    program.getDaysPerWeek() > 0 ? program.getDaysPerWeek() : 3);
            programDurationText.setText(durationText);


            View appBarLayout = getView().findViewById(R.id.app_bar_layout);
            if (appBarLayout != null) {
                appBarLayout.setVisibility(View.VISIBLE);
            }


            if (program.getImageUrl() != null && !program.getImageUrl().isEmpty()) {
                Log.d(TAG, "Загрузка изображения программы: " + program.getImageUrl());


            }

            showLoading(false);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении интерфейса: " + e.getMessage(), e);
            showError("Ошибка при обновлении интерфейса: " + e.getMessage());
            showLoading(false);
        }
    }

    private void showDayDetails(ProgramDay day) {

        Log.d(TAG, "Переход к деталям дня программы: " +
                "id=" + day.getId() +
                ", номер=" + day.getDayNumber() +
                ", название='" + day.getName() + "'" +
                ", программа=" + day.getProgramId() +
                ", templateDayId=" + day.getTemplateDayId());


        ProgramDayDetailsFragment detailsFragment = ProgramDayDetailsFragment.newInstance(day.getId());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }


    private void navigateBackToProgramsTab() {

        requireActivity().getSharedPreferences("VitaMovePrefs", 0)
                .edit()
                .putInt("workout_tab_index", 2)
                .apply();


        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void showError(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        if (getView() != null) {
            View loadingView = getView().findViewById(R.id.loading_view);
            View contentView = getView().findViewById(R.id.content_container);

            if (loadingView != null && contentView != null) {
                loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
                contentView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        navigateBackToProgramsTab();
                    }
                });
    }
} 