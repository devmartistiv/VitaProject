package com.martist.vitamove.programs.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.AsyncCallback;
import com.martist.vitamove.programs.data.ProgramManager;
import com.martist.vitamove.programs.ui.adapter.ProgramExerciseAdapter;
import com.martist.vitamove.programs.ui.model.ProgramDay;
import com.martist.vitamove.programs.ui.model.ProgramExercise;
import com.martist.vitamove.workout.utils.WorkoutExerciseHelper;

import java.util.ArrayList;
import java.util.List;

public class ProgramDayDetailsFragment extends Fragment implements
        ProgramExerciseAdapter.OnExerciseClickListener {

    private static final String ARG_DAY_ID = "day_id";
    private static final String TAG = "ProgramDayDetailsFragment";

    private String dayId;
    private ProgramManager programManager;
    private ProgramDay programDay;
    private RecyclerView exercisesRecyclerView;
    private ProgramExerciseAdapter adapter;
    private TextView dayNumberText;
    private TextView dayNameText;
    private TextView dayDescriptionText;
    private WorkoutExerciseHelper exerciseHelper;

    public static ProgramDayDetailsFragment newInstance(String dayId) {
        ProgramDayDetailsFragment fragment = new ProgramDayDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DAY_ID, dayId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            dayId = getArguments().getString(ARG_DAY_ID);
        }
        programManager = new ProgramManager(requireContext());
        exerciseHelper = new WorkoutExerciseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_program_day_details, container, false);
        initViews(view);


        View backButton = view.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> navigateBackToProgramsTab());
        }

        setupRecyclerView();
        setupClickListeners();
        loadDayDetails();
        return view;
    }

    private void initViews(View view) {
        dayNumberText = view.findViewById(R.id.day_number);
        dayNameText = view.findViewById(R.id.day_name);
        dayDescriptionText = view.findViewById(R.id.day_description);
        exercisesRecyclerView = view.findViewById(R.id.exercises_recycler_view);
    }

    private void setupRecyclerView() {
        adapter = new ProgramExerciseAdapter(new ArrayList<>(), this);
        exercisesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        exercisesRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {

    }

    private void loadDayDetails() {
        if (dayId == null || dayId.isEmpty()) {
            showError("ID дня программы отсутствует");
            return;
        }


        View loadingView = getView() != null ? getView().findViewById(R.id.loading_view) : null;
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        }


        programManager.getProgramDayByIdAsync(dayId, new AsyncCallback<ProgramDay>() {
            @Override
            public void onSuccess(ProgramDay day) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        programDay = day;
                        updateUI(day);
                        loadExercises();
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showError("Ошибка при загрузке информации о дне: " + e.getMessage());
                        if (loadingView != null) {
                            loadingView.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    public void loadExercises() {
        if (programDay == null || programDay.getId() == null) {
            showError("День программы не инициализирован");
            return;
        }

        String dayId = programDay.getId();
        Log.d(TAG, "Загрузка упражнений для дня программы с ID: " + dayId +
                ", день №" + programDay.getDayNumber() +
                ", название: '" + programDay.getName() + "'");


        View loadingView = getView() != null ? getView().findViewById(R.id.loading_view) : null;
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        }


        new Thread(() -> {
            try {

                List<ProgramExercise> exercises = programManager.getProgramDayExercises(dayId);


                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getActivity() == null || !isAdded()) return;

                    Log.d(TAG, "Упражнения получены успешно. Количество: " + exercises.size());


                    for (int i = 0; i < exercises.size(); i++) {
                        ProgramExercise exercise = exercises.get(i);
                        Log.d(TAG, "Упражнение [" + i + "]: " +
                                "id=" + exercise.getId() +
                                ", name=" + (exercise.getExercise() != null ? exercise.getExercise().getName() : "null") +
                                ", exerciseId=" + exercise.getExerciseId() +
                                ", day_id=" + exercise.getProgramDayId());
                    }


                    adapter.updateExercises(exercises);


                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }


                    View emptyView = getView() != null ? getView().findViewById(R.id.empty_view) : null;
                    if (emptyView != null) {
                        emptyView.setVisibility(exercises.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getActivity() == null || !isAdded()) return;

                    Log.e(TAG, "Ошибка при загрузке упражнений: " + e.getMessage() + " для дня: " + dayId, e);


                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }


                    showError("Не удалось загрузить упражнения: " + e.getMessage());


                    View emptyView = getView() != null ? getView().findViewById(R.id.empty_view) : null;
                    if (emptyView != null) {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    private void updateUI(ProgramDay day) {
        dayNumberText.setText(String.format("День %d", day.getDayNumber()));
        dayNameText.setText(day.getName());
        dayDescriptionText.setText(day.getDescription());
    }

    @Override
    public void onExerciseClick(ProgramExercise exercise) {

        android.util.Log.d(TAG, "onExerciseClick: Клик по упражнению ID: " + exercise.getId());


        exerciseHelper.showExerciseInfoDialog(exercise);
    }


    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }


    private void navigateBackToProgramsTab() {

        requireActivity().getSharedPreferences("VitaMovePrefs", 0)
                .edit()
                .putInt("workout_tab_index", 2)
                .apply();


        requireActivity().getSupportFragmentManager().popBackStack();
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