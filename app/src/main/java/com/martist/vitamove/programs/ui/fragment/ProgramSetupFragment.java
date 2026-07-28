package com.martist.vitamove.programs.ui.fragment;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.AsyncCallback;
import com.martist.vitamove.core.domain.utils.TextUtils;
import com.martist.vitamove.programs.data.ProgramManager;
import com.martist.vitamove.programs.data.local.ProgramRoomCache;
import com.martist.vitamove.programs.ui.model.ProgramSetupConfig;
import com.martist.vitamove.workout.data.model.WorkoutProgram;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ProgramSetupFragment extends Fragment {
    private static final String TAG = "ProgramSetupFragment";
    private static final String ARG_PROGRAM_ID = "program_id";

    private String programId;
    private WorkoutProgram program;
    private ProgramManager programManager;
    private ProgramSetupConfig setupConfig;


    private TextView programNameText;
    private TextView programDurationText;
    private TextView programLevelText;
    private TextView startDateText;
    private TextView daysSelectionHintText;
    private ChipGroup daysOfWeekChipGroup;
    private MaterialButton startProgramButton;
    private MaterialButton cancelButton;
    private View loadingView;
    private ImageButton programInfoButton;
    private View programInfoCard;

    private Calendar selectedStartDate = Calendar.getInstance();
    private final Calendar selectedReminderTime = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("ru"));


    public static ProgramSetupFragment newInstance(String programId) {
        ProgramSetupFragment fragment = new ProgramSetupFragment();
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
        setupConfig = new ProgramSetupConfig();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_program_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();
        loadProgramDetails();


        setupDefaultValues();


        requestNotificationPermission();
    }

    @Override
    public void onResume() {
        super.onResume();

        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
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


    private void initViews(View view) {
        programNameText = view.findViewById(R.id.program_name);
        programDurationText = view.findViewById(R.id.program_duration);
        programLevelText = view.findViewById(R.id.program_level);
        startDateText = view.findViewById(R.id.start_date_text);
        daysSelectionHintText = view.findViewById(R.id.days_selection_hint);
        daysOfWeekChipGroup = view.findViewById(R.id.days_of_week_chip_group);
        startProgramButton = view.findViewById(R.id.btn_start_program);
        cancelButton = view.findViewById(R.id.btn_cancel);
        loadingView = view.findViewById(R.id.loading_view);
        programInfoButton = view.findViewById(R.id.btn_program_info);
        programInfoCard = view.findViewById(R.id.program_info_card);
    }


    private void setupClickListeners() {

        MaterialButton changeDateButton = requireView().findViewById(R.id.btn_change_date);
        changeDateButton.setOnClickListener(v -> showDatePickerDialog());


        startProgramButton.setOnClickListener(v -> startProgram());


        cancelButton.setOnClickListener(v -> {

            requireActivity().getSharedPreferences("VitaMovePrefs", 0)
                    .edit()
                    .putInt("workout_tab_index", 2)
                    .apply();

            requireActivity().getSupportFragmentManager().popBackStack();
        });


        daysOfWeekChipGroup.setOnCheckedChangeListener((group, checkedId) -> {

            List<Integer> selectedDays = new ArrayList<>();
            for (int i = 0; i < daysOfWeekChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) daysOfWeekChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    int dayIndex = getDayIndex(chip.getId());
                    selectedDays.add(dayIndex);
                    Log.d(TAG, "!!! VITAMOVE_DEBUG: [DAYS] Выбран день: " + dayIndex);
                }
            }


            setupConfig.setWorkoutDays(selectedDays);
            Log.d(TAG, "!!! VITAMOVE_DEBUG: [DAYS] Обновлен список дней недели: " + selectedDays);


            updateDaysSelectionHint();
        });


        programInfoButton.setOnClickListener(v -> showProgramInfoBottomSheet());


        programInfoCard.setOnClickListener(v -> showProgramInfoBottomSheet());
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
                            setupConfig.setProgramId(programId);
                            updateUI();
                            setupDefaultDaysOfWeek(result.getDaysPerWeek());
                            showLoading(false);
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


    private void updateUI() {
        try {
            programNameText.setText(program.getName());

            String durationText = TextUtils.formatProgramDuration(
                    program.getDurationWeeks() > 0 ? program.getDurationWeeks() : 4,
                    program.getDaysPerWeek() > 0 ? program.getDaysPerWeek() : 3);
            programDurationText.setText(durationText);

            String levelText = "Уровень: " + (program.getLevel() != null ? program.getLevel() : "не указан");
            programLevelText.setText(levelText);


            updateDaysSelectionHint();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении интерфейса: " + e.getMessage(), e);
            showError("Ошибка при обновлении интерфейса");
        }
    }


    private void setupDefaultValues() {

        Calendar today = Calendar.getInstance();


        int currentYear = today.get(Calendar.YEAR);
        Log.d(TAG, "!!! VITAMOVE_DEBUG: Устанавливаем текущий год: " + currentYear);


        selectedStartDate = Calendar.getInstance();
        selectedStartDate.set(Calendar.YEAR, currentYear);

        startDateText.setText(dateFormat.format(selectedStartDate.getTime()));
        setupConfig.setStartDate(selectedStartDate.getTimeInMillis());

        Log.d(TAG, "!!! VITAMOVE_DEBUG: Дата начала программы: " + new Date(selectedStartDate.getTimeInMillis()) +
                ", timestamp: " + selectedStartDate.getTimeInMillis());


    }


    private void setupDefaultDaysOfWeek(int daysPerWeek) {

        for (int i = 0; i < daysOfWeekChipGroup.getChildCount(); i++) {
            ((Chip) daysOfWeekChipGroup.getChildAt(i)).setChecked(false);
        }

        List<Integer> selectedDays = new ArrayList<>();


        if (daysPerWeek == 3) {

            selectedDays.add(0);
            selectedDays.add(2);
            selectedDays.add(4);
        } else if (daysPerWeek < 3) {

            for (int i = 0; i < daysPerWeek; i++) {
                selectedDays.add(i * 2);
            }
        } else if (daysPerWeek > 3) {

            selectedDays.add(0);
            selectedDays.add(2);
            selectedDays.add(4);


            int daysToAdd = daysPerWeek - 3;
            int[] additionalDays = {1, 3, 5, 6};
            for (int i = 0; i < daysToAdd && i < additionalDays.length; i++) {
                selectedDays.add(additionalDays[i]);
            }
        }


        for (Integer dayIndex : selectedDays) {
            Chip dayChip = getDayChip(dayIndex);
            if (dayChip != null) {
                dayChip.setChecked(true);
            }
        }

        setupConfig.setWorkoutDays(selectedDays);


        daysOfWeekChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            List<Integer> newSelectedDays = new ArrayList<>();
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip chip = (Chip) group.getChildAt(i);
                if (chip.isChecked()) {
                    newSelectedDays.add(getDayIndex(chip.getId()));
                }
            }
            setupConfig.setWorkoutDays(newSelectedDays);


            updateDaysSelectionHint();
        });


        updateDaysSelectionHint();
    }


    private int getDayIndex(int chipId) {
        if (chipId == R.id.chip_monday) return 0;
        if (chipId == R.id.chip_tuesday) return 1;
        if (chipId == R.id.chip_wednesday) return 2;
        if (chipId == R.id.chip_thursday) return 3;
        if (chipId == R.id.chip_friday) return 4;
        if (chipId == R.id.chip_saturday) return 5;
        if (chipId == R.id.chip_sunday) return 6;
        return 0;
    }


    private Chip getDayChip(int dayIndex) {
        switch (dayIndex) {
            case 0:
                return requireView().findViewById(R.id.chip_monday);
            case 1:
                return requireView().findViewById(R.id.chip_tuesday);
            case 2:
                return requireView().findViewById(R.id.chip_wednesday);
            case 3:
                return requireView().findViewById(R.id.chip_thursday);
            case 4:
                return requireView().findViewById(R.id.chip_friday);
            case 5:
                return requireView().findViewById(R.id.chip_saturday);
            case 6:
                return requireView().findViewById(R.id.chip_sunday);
            default:
                return null;
        }
    }


    private void showDatePickerDialog() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedStartDate.set(year, month, dayOfMonth);
                    startDateText.setText(dateFormat.format(selectedStartDate.getTime()));
                    setupConfig.setStartDate(selectedStartDate.getTimeInMillis());
                },
                selectedStartDate.get(Calendar.YEAR),
                selectedStartDate.get(Calendar.MONTH),
                selectedStartDate.get(Calendar.DAY_OF_MONTH)
        );


        Calendar minDate = Calendar.getInstance();
        dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());


        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 1);
        dialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        dialog.show();
    }


    private void startProgram() {
        if (program == null || setupConfig.getProgramId() == null) {
            showError("Программа не инициализирована");
            return;
        }


        if (setupConfig.getWorkoutDays().isEmpty()) {
            showError("Выберите хотя бы один день недели для тренировок");
            return;
        }


        int daysPerWeek = program.getDaysPerWeek();
        int selectedDaysCount = setupConfig.getWorkoutDays().size();

        if (selectedDaysCount != daysPerWeek) {

            showDaysSelectionErrorDialog(daysPerWeek);
            return;
        }

        showLoading(true);


        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < daysOfWeekChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) daysOfWeekChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                int dayIndex = getDayIndex(chip.getId());
                selectedDays.add(dayIndex);
            }
        }

        Log.d(TAG, "!!! VITAMOVE_DEBUG: [START PROGRAM] Выбранные дни недели: " + selectedDays);


        setupConfig.setWorkoutDays(selectedDays);


        program.setWorkoutDays(selectedDays);

        Log.d(TAG, "!!! VITAMOVE_DEBUG: [START PROGRAM] Обновленные дни в программе: " + program.getWorkoutDays());


        programManager.saveProgramConfigAsync(setupConfig, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.d(TAG, "!!! VITAMOVE_DEBUG: [START PROGRAM] Сохранена конфигурация программы");


                programManager.updateProgramWorkoutDaysAsync(program.getId(), selectedDays, new AsyncCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean updated) {
                        Log.d(TAG, "!!! VITAMOVE_DEBUG: [START PROGRAM] Обновлены дни тренировок в программе: " + updated);


                        ProgramSetupConfig configBeforeStart = programManager.getProgramConfig(setupConfig.getProgramId());
                        Log.d(TAG, "!!! VITAMOVE_DEBUG: [START PROGRAM] Дни тренировок в конфигурации перед запуском: " +
                                (configBeforeStart != null ? configBeforeStart.getWorkoutDays() : "конфигурация не найдена"));


                        programManager.startProgramAsync(
                                setupConfig.getProgramId(),
                                setupConfig.getStartDate(),
                                new AsyncCallback<String>() {
                                    @Override
                                    public void onSuccess(String userProgramId) {

                                        cacheEntireProgram(userProgramId);


                                        showLoading(false);
                                        navigateToProgramsScreen();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        showLoading(false);
                                        Log.e(TAG, "Ошибка при активации программы: " + e.getMessage(), e);
                                        showError("Не удалось запустить программу");
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(Exception e) {
                        showLoading(false);
                        Log.e(TAG, "Ошибка при обновлении дней тренировок: " + e.getMessage(), e);
                        showError("Не удалось обновить дни тренировок");
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Log.e(TAG, "Ошибка при сохранении конфигурации: " + e.getMessage(), e);
                showError("Не удалось сохранить настройки программы");
            }
        });
    }


    private void navigateToProgramsScreen() {

        requireActivity().getSharedPreferences("VitaMovePrefs", 0)
                .edit()
                .putInt("workout_tab_index", 2)
                .apply();


        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }


    private void showError(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }


    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }


    private void showProgramInfoBottomSheet() {
        if (program != null) {
            ProgramInfoBottomSheet bottomSheet = ProgramInfoBottomSheet.newInstance(program);
            bottomSheet.show(getChildFragmentManager(), "ProgramInfoBottomSheet");
        }
    }


    private void cacheEntireProgram(String userProgramId) {
        try {
            Log.d(TAG, "Начало кэширования полной программы и планов с ID: " + userProgramId + " через RPC");


            programManager.getFullProgramAsync(userProgramId, new AsyncCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject programJson) {
                    if (programJson != null) {
                        try {

                            Log.i(TAG, "Начало сохранения программы в нормализованную структуру Room для ID: " + userProgramId);
                            ProgramRoomCache.saveProgram(userProgramId, programJson);
                            Log.i(TAG, "Программа успешно сохранена в нормализованную структуру Room для ID: " + userProgramId);


                            android.os.Handler handler = new android.os.Handler();
                            handler.postDelayed(() -> {
                                Log.d(TAG, "Запускаем отладочную проверку кэширования упражнений программы");
                                ProgramRoomCache.debugExerciseSaving(userProgramId);
                            }, 2000);


                            Log.d("VITAMOVE_PLAN_CACHE", "cacheEntireProgram: Вызов programManager.fetchAndCacheWorkoutPlansAsync для userProgramId: " + userProgramId);
                            programManager.fetchAndCacheWorkoutPlansAsync(userProgramId, new AsyncCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Log.i("VITAMOVE_PLAN_CACHE", "cacheEntireProgram: fetchAndCacheWorkoutPlansAsync onSuccess (инициировано) для userProgramId: " + userProgramId);
                                }

                                @Override
                                public void onFailure(Exception error) {
                                    Log.e("VITAMOVE_PLAN_CACHE", "cacheEntireProgram: fetchAndCacheWorkoutPlansAsync onFailure для userProgramId: " + userProgramId, error);

                                }
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка сохранения программы (RPC) в кэш для ID: " + userProgramId, e);
                        }
                    } else {
                        Log.w(TAG, "Получен пустой JSON для программы (RPC) с ID: " + userProgramId + ", кэширование не выполнено.");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Ошибка получения полной программы (RPC) для кэширования, ID: " + userProgramId, e);


                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Общая ошибка при запуске кэширования программы и планов (RPC): " + e.getMessage(), e);
        }
    }


    private String getDeclensionForDays(int number) {
        int remainder10 = number % 10;
        int remainder100 = number % 100;

        if (remainder10 == 1 && remainder100 != 11) {
            return "день";
        } else if (remainder10 >= 2 && remainder10 <= 4 && (remainder100 < 10 || remainder100 >= 20)) {
            return "дня";
        } else {
            return "дней";
        }
    }


    private void updateDaysSelectionHint() {
        if (program != null && daysSelectionHintText != null) {
            int daysPerWeek = program.getDaysPerWeek();
            int selectedDaysCount = setupConfig.getWorkoutDays().size();

            String declension = getDeclensionForDays(daysPerWeek);
            String hint = String.format("Выберите %d %s для тренировок", daysPerWeek, declension);


            if (selectedDaysCount == daysPerWeek) {
                daysSelectionHintText.setVisibility(View.GONE);
            } else {
                daysSelectionHintText.setVisibility(View.VISIBLE);
                daysSelectionHintText.setTextColor(requireContext().getResources().getColor(R.color.gray_600));
                daysSelectionHintText.setText(hint);
            }
        }
    }


    private void showDaysSelectionErrorDialog(int requiredDaysCount) {
        String declension = getDeclensionForDays(requiredDaysCount);
        String message = String.format("Для этой программы необходимо выбрать %d %s тренировок.\n\nПожалуйста, измените выбор дней недели.",
                requiredDaysCount, declension);


        highlightDaysChips(true);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Неверное количество дней")
                .setMessage(message)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("Понятно", (dialogInterface, i) -> {

                    highlightDaysChips(false);
                })
                .create();

        dialog.setOnDismissListener(dialogInterface -> {

            highlightDaysChips(false);
        });

        dialog.show();
    }


    private void highlightDaysChips(boolean highlight) {
        for (int i = 0; i < daysOfWeekChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) daysOfWeekChipGroup.getChildAt(i);
            if (highlight) {

                chip.setTag(R.id.tag_stroke_color, chip.getChipStrokeColor());
                chip.setChipStrokeColor(getColorStateList(R.color.error_color));
                chip.setChipStrokeWidth(2f);
            } else {

                if (chip.getTag(R.id.tag_stroke_color) != null) {
                    chip.setChipStrokeColor((android.content.res.ColorStateList) chip.getTag(R.id.tag_stroke_color));
                } else {
                    chip.setChipStrokeColor(getColorStateList(R.color.colorPrimary));
                }
                chip.setChipStrokeWidth(1f);
            }
        }
    }

    private android.content.res.ColorStateList getColorStateList(int colorRes) {
        return android.content.res.ColorStateList.valueOf(requireContext().getResources().getColor(colorRes));
    }


    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {


                ActivityResultLauncher<String> requestPermissionLauncher =
                        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                            if (isGranted) {
                                Log.d(TAG, "Разрешение на отправку уведомлений получено");
                            } else {
                                Log.w(TAG, "Разрешение на отправку уведомлений не получено");


                            }
                        });


                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
} 