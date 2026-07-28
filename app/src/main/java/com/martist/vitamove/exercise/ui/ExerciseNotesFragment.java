package com.martist.vitamove.exercise.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.exercise.ui.adapters.ExerciseNotesHistoryAdapter;
import com.martist.vitamove.set.OneRepMax;
import com.martist.vitamove.workout.data.model.WorkoutExercise;


public class ExerciseNotesFragment extends Fragment {
    private static final String TAG = "ExerciseNotesFragment";
    private static final String ARG_EXERCISE = "exercise";
    private static final String ARG_POSITION = "position";

    private WorkoutExercise exercise;
    private int position;
    private ExerciseNotesViewModel viewModel;


    private MaterialToolbar toolbar;
    private TextInputEditText noteEditText;
    private MaterialButton saveButton;
    private RecyclerView historyRecyclerView;
    private ExerciseNotesHistoryAdapter historyAdapter;
    private View emptyStateLayout;


    private TextView oneRmValueText;
    private TextView oneRmBasedOnText;
    private View oneRmContentLayout;
    private View oneRmNoDataLayout;
    private ImageView oneRmInfoButton;

    public static ExerciseNotesFragment newInstance(WorkoutExercise exercise, int position) {
        ExerciseNotesFragment fragment = new ExerciseNotesFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_EXERCISE, exercise);
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            exercise = getArguments().getParcelable(ARG_EXERCISE);
            position = getArguments().getInt(ARG_POSITION);


            if (exercise != null) {
                Log.d(TAG, "ОТЛАДКА НАВИГАЦИИ: Получено упражнение - ID: " + exercise.getId() +
                        ", name: " + (exercise.getExercise() != null ? exercise.getExercise().getName() : "null") +
                        ", base_exercise_id: " + (exercise.getExercise() != null ? exercise.getExercise().getId() : "null") +
                        ", notes: '" + exercise.getNotes() + "'");
            } else {
                Log.e(TAG, "ОТЛАДКА НАВИГАЦИИ: exercise is NULL!");
            }
        } else {
            Log.e(TAG, "ОТЛАДКА НАВИГАЦИИ: getArguments() is NULL!");
        }

        viewModel = new ViewModelProvider(this).get(ExerciseNotesViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupToolbar();
        setupNoteInput();
        setupHistoryRecyclerView();
        setupOneRepMaxInfo();
        observeViewModel();


        if (exercise != null && exercise.getExercise() != null) {
            viewModel.loadNotesHistory(exercise.getExercise().getId());
            viewModel.loadOneRepMax(exercise.getExercise().getId());
        }
        if (getActivity() != null && getActivity().getWindow() != null) {

            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusbar_color));


            boolean isNightMode = (getResources().getConfiguration().uiMode &
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES;

            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            if (isNightMode) {

                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {

                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        noteEditText = view.findViewById(R.id.note_edit_text);
        saveButton = view.findViewById(R.id.save_button);
        historyRecyclerView = view.findViewById(R.id.history_recycler_view);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);


        oneRmValueText = view.findViewById(R.id.one_rm_value);
        oneRmBasedOnText = view.findViewById(R.id.one_rm_based_on);
        oneRmContentLayout = view.findViewById(R.id.one_rm_content_layout);
        oneRmNoDataLayout = view.findViewById(R.id.one_rm_no_data_layout);
        oneRmInfoButton = view.findViewById(R.id.one_rm_info_button);
    }

    private void setupToolbar() {
        if (exercise != null && exercise.getExercise() != null) {
            toolbar.setTitle("Заметки");
            toolbar.setSubtitle(exercise.getExercise().getName());
        }

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "Возврат с экрана заметок, back stack entries: " + getParentFragmentManager().getBackStackEntryCount());
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().finish();
            }
        });

    }

    private void setupNoteInput() {

        if (exercise != null && exercise.getNotes() != null) {
            noteEditText.setText(exercise.getNotes());
        }


        noteEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        saveButton.setOnClickListener(v -> saveNote());

        updateSaveButtonState();
    }

    private void setupHistoryRecyclerView() {
        historyAdapter = new ExerciseNotesHistoryAdapter();
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void setupOneRepMaxInfo() {
        oneRmInfoButton.setOnClickListener(v -> showOneRepMaxInfoDialog());
    }

    private void observeViewModel() {

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            saveButton.setEnabled(!isLoading);
            if (isLoading) {
                saveButton.setText("Сохранение...");
            } else {
                saveButton.setText("Сохранить заметку");
            }
        });


        viewModel.getNotesHistory().observe(getViewLifecycleOwner(), notesHistory -> {
            Log.d(TAG, "Получена история заметок: " + notesHistory.size() + " записей");
            historyAdapter.updateNotes(notesHistory);


            if (historyAdapter.isEmpty()) {
                historyRecyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            } else {
                historyRecyclerView.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
            }
        });


        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });


        viewModel.getOneRepMax().observe(getViewLifecycleOwner(), oneRepMax -> {
            updateOneRepMaxUI(oneRepMax);
        });


        viewModel.getNoteSaved().observe(getViewLifecycleOwner(), saved -> {
            if (saved) {
                Toast.makeText(requireContext(), "Заметка сохранена", Toast.LENGTH_SHORT).show();


                String currentNote = noteEditText.getText() != null ?
                        noteEditText.getText().toString().trim() : "";
                if (exercise != null) {
                    exercise.setNotes(currentNote);
                }


                if (exercise != null && exercise.getExercise() != null) {
                    Log.d(TAG, "Заметка сохранена, перезагружаем историю для упражнения: " +
                            exercise.getExercise().getName() + " (ID: " + exercise.getExercise().getId() + ")");


                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        viewModel.loadNotesHistory(exercise.getExercise().getId());
                    }, 100);
                }
            }
        });
    }

    private void updateSaveButtonState() {
        String currentText = noteEditText.getText() != null ?
                noteEditText.getText().toString().trim() : "";
        String originalText = exercise != null && exercise.getNotes() != null ?
                exercise.getNotes().trim() : "";


        boolean hasChanges = !currentText.equals(originalText);
        saveButton.setEnabled(hasChanges && !viewModel.getIsLoading().getValue());
    }

    private void saveNote() {
        if (exercise == null) {
            Toast.makeText(requireContext(), "Ошибка: упражнение не найдено", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "ОТЛАДКА СОХРАНЕНИЯ: exercise is NULL при попытке сохранить!");
            return;
        }

        String noteText = noteEditText.getText() != null ?
                noteEditText.getText().toString().trim() : "";

        Log.d(TAG, "ОТЛАДКА СОХРАНЕНИЯ: Сохранение заметки для упражнения - " +
                "workout_exercise_id: " + exercise.getId() +
                ", exercise_name: " + (exercise.getExercise() != null ? exercise.getExercise().getName() : "null") +
                ", base_exercise_id: " + (exercise.getExercise() != null ? exercise.getExercise().getId() : "null") +
                ", noteText: '" + (noteText.isEmpty() ? "(пусто)" : noteText) + "'");

        if (exercise.getId() == null) {
            Log.e(TAG, "ОТЛАДКА СОХРАНЕНИЯ: workout_exercise_id is NULL! Не можем сохранить заметку.");
            Toast.makeText(requireContext(), "Ошибка: ID упражнения не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.saveNote(exercise.getId(), noteText);
    }


    private void updateOneRepMaxUI(OneRepMax oneRepMax) {
        if (oneRepMax == null) {

            oneRmContentLayout.setVisibility(View.GONE);
            oneRmNoDataLayout.setVisibility(View.VISIBLE);
            return;
        }

        if (oneRepMax.isHasData()) {

            oneRmContentLayout.setVisibility(View.VISIBLE);
            oneRmNoDataLayout.setVisibility(View.GONE);

            oneRmValueText.setText(oneRepMax.getFormattedValue());
            oneRmBasedOnText.setText(oneRepMax.getBasedOnDescription());

            Log.d(TAG, "Отображение 1ПМ: " + oneRepMax.getFormattedValue() + " кг, " + oneRepMax.getBasedOnDescription());
        } else {

            oneRmContentLayout.setVisibility(View.GONE);
            oneRmNoDataLayout.setVisibility(View.VISIBLE);

            Log.d(TAG, "Недостаточно данных для расчета 1ПМ");
        }
    }


    private void showOneRepMaxInfoDialog() {
        String title = "О расчете 1ПМ";
        String message = "📊 Одноповторный максимум (1ПМ) — это максимальный вес, который вы можете поднять за одно повторение.\n\n" +

                "📐 Формула расчета:\n" +
                "• Формула Бржицки: 1ПМ = вес × (36 / (37 - повторения))\n" +
                "• Для 1 повторения: это уже ваш 1ПМ\n" +
                "• Для >15 повторений: альтернативная формула\n\n" +

                "⚠️ Важное предупреждение:\n" +
                "• Это только ПРИБЛИЗИТЕЛЬНЫЙ расчет\n" +
                "• Результат может отличаться от реального 1ПМ\n" +
                "• Используйте как справочную информацию\n" +
                "• Наиболее точен для 2-10 повторений\n\n" +

                "💡 Для точного определения 1ПМ рекомендуется проводить специальное тестирование под наблюдением тренера.";

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Понятно", null)
                .create()
                .show();
    }
}
