package com.martist.vitamove.programs.ui.fragment;

import static com.martist.vitamove.VitaMoveApplication.context;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.AsyncCallback;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.domain.utils.SupabaseCallback;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.databinding.FragmentProgramsBinding;
import com.martist.vitamove.programs.data.ProgramManager;
import com.martist.vitamove.programs.data.local.ProgramRoomCache;
import com.martist.vitamove.programs.ui.adapter.ProgramAdapter;
import com.martist.vitamove.programs.ui.adapter.ProgramDayAdapter;
import com.martist.vitamove.programs.ui.dialog.ProgramDaysConfigDialog;
import com.martist.vitamove.programs.ui.model.Program;
import com.martist.vitamove.programs.ui.model.ProgramDay;
import com.martist.vitamove.programs.ui.model.ProgramType;
import com.martist.vitamove.workout.data.model.WorkoutPlan;
import com.martist.vitamove.workout.data.model.WorkoutProgram;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;
import com.martist.vitamove.workout.domain.WorkoutStartedEvent;
import com.martist.vitamove.workout.ui.fragments.CreateWorkoutFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class ProgramsFragment extends Fragment implements ProgramAdapter.OnProgramClickListener {
    private static final String TAG = "ProgramsFragment";
    private ProgramAdapter programAdapter;
    private ProgramManager programManager;
    private ProgramAdapter userProgramAdapter;
    private WorkoutProgram activeProgram;
    private List<WorkoutProgram> allPrograms = new ArrayList<>();
    private List<WorkoutProgram> userPrograms = new ArrayList<>();
    private List<WorkoutProgram> recommendedPrograms = new ArrayList<>();
    private ProgramDayAdapter programDayAdapter;
    private ExecutorService executorService;
    private WorkoutRepository workoutRepository;
    private FragmentProgramsBinding binding;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Context context = requireContext();
        workoutRepository =
                ((VitaMoveApplication) requireActivity().getApplication()).getWorkoutRepository();
        programManager = new ProgramManager(context);
        executorService = Executors.newSingleThreadExecutor();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProgramsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        setupSearchField(view);
        setupClickListeners();
        setupRecyclerView();
        setupActiveProgramDaysRecyclerView();
    }

    void setupClickListeners() {
        binding.fabCreateProgram.setOnClickListener(v -> createNewProgram());
        binding.activeProgramContinueButton.setOnClickListener(v -> continueProgramWorkout(activeProgram));
        binding.activeProgramChangeButton.setOnClickListener(v -> changeProgram(activeProgram));
        binding.activeProgramConfigButton.setOnClickListener(v -> showProgramDaysConfigDialog(activeProgram));
        binding.activeProgramInfo.setOnClickListener(v -> showActiveProgramInfo(activeProgram));
        binding.activeProgramName.setOnClickListener(v -> showActiveProgramInfo(activeProgram));

    }


    private void setupSearchField(View view) {
        TextInputEditText searchEditText = view.findViewById(R.id.search_edit_text);
        if (searchEditText != null) {

            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    searchPrograms(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });


            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchPrograms(v.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }


    private void searchPrograms(String query) {
        if (allPrograms == null || allPrograms.isEmpty()) {
            return;
        }

        if (query == null || query.trim().isEmpty()) {

            userProgramAdapter.updatePrograms(userPrograms);
            programAdapter.updatePrograms(recommendedPrograms);


            updateUserProgramsVisibility();
            return;
        }

        String searchQuery = query.toLowerCase().trim();


        List<WorkoutProgram> filteredUserPrograms = userPrograms.stream()
                .filter(program -> {
                    return program.getName().toLowerCase().contains(searchQuery) ||
                            (program.getDescription() != null && program.getDescription().toLowerCase().contains(searchQuery)) ||
                            (program.getGoal() != null && program.getGoal().toLowerCase().contains(searchQuery));
                })
                .collect(Collectors.toList());


        List<WorkoutProgram> filteredRecommendedPrograms = recommendedPrograms.stream()
                .filter(program -> {

                    boolean matchesSearch = program.getName().toLowerCase().contains(searchQuery) ||
                            (program.getDescription() != null && program.getDescription().toLowerCase().contains(searchQuery)) ||
                            (program.getGoal() != null && program.getGoal().toLowerCase().contains(searchQuery));

                    return matchesSearch;
                })
                .collect(Collectors.toList());


        userProgramAdapter.updatePrograms(filteredUserPrograms);
        programAdapter.updatePrograms(filteredRecommendedPrograms);


        if (filteredUserPrograms.isEmpty()) {
            binding.userProgramsTitle.setVisibility(View.GONE);
            binding.userProgramsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.userProgramsTitle.setVisibility(View.VISIBLE);
            binding.userProgramsRecyclerView.setVisibility(View.VISIBLE);
        }
    }


    private void updateUserProgramsVisibility() {

        if (activeProgram != null) {
            binding.userProgramsTitle.setVisibility(View.GONE);
            binding.userProgramsRecyclerView.setVisibility(View.GONE);
            return;
        }


        if (userPrograms.isEmpty()) {
            binding.userProgramsTitle.setVisibility(View.GONE);
            binding.userProgramsRecyclerView.setVisibility(View.GONE);

        } else {
            binding.userProgramsTitle.setVisibility(View.VISIBLE);
            binding.userProgramsRecyclerView.setVisibility(View.VISIBLE);

        }
    }


    private void setupRecyclerView() {
        allPrograms = new ArrayList<>();
        userPrograms = new ArrayList<>();
        recommendedPrograms = new ArrayList<>();


        programAdapter = new ProgramAdapter(recommendedPrograms, this, requireContext());
        binding.programsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.programsRecyclerView.setAdapter(programAdapter);


        userProgramAdapter = new ProgramAdapter(userPrograms, this, requireContext());
        binding.userProgramsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.userProgramsRecyclerView.setAdapter(userProgramAdapter);
    }


    public void loadPrograms(boolean loadActiveProgram) {
        showLoading(true);
        allPrograms.clear();
        userPrograms.clear();
        recommendedPrograms.clear();
        programAdapter.updatePrograms(new ArrayList<>());
        userProgramAdapter.updatePrograms(new ArrayList<>());

        programManager.getAllProgramsAsync(new AsyncCallback<List<WorkoutProgram>>() {
            @Override
            public void onSuccess(List<WorkoutProgram> result) {
                Log.d(TAG, "Программы успешно загружены из Supabase. Количество: " + result.size());


                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allPrograms = result;
                        separatePrograms(allPrograms);
                        showLoading(false);

                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Ошибка при загрузке программ: " + e.getMessage(), e);


                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {

                        Toast.makeText(requireContext(),
                                "Ошибка при загрузке программ: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();


                        showLoading(false);

                    });
                }
            }
        });


        if (loadActiveProgram) {
            loadActiveProgram();
        }
    }


    private void separatePrograms(List<WorkoutProgram> programs) {
        userPrograms.clear();
        recommendedPrograms.clear();


        String userId = null;
        if (context != null) {
            userId = ((VitaMoveApplication) context.getApplicationContext()).getCurrentUserId();
        }
        final String currentUserId = userId;


        Set<String> addedProgramIds = new HashSet<>();

        for (WorkoutProgram program : programs) {

            if (program.getId() != null && !addedProgramIds.add(program.getId())) {
                continue;
            }

            if (program.getType() == ProgramType.USER_CREATED ||
                    (currentUserId != null && currentUserId.equals(program.getUserId()))) {
                userPrograms.add(program);
            } else {
                recommendedPrograms.add(program);
            }
        }


        userProgramAdapter.updatePrograms(userPrograms);
        programAdapter.updatePrograms(recommendedPrograms);


        updateUserProgramsVisibility();
    }


    public void loadPrograms() {
        loadPrograms(true);
    }


    private void loadActiveProgram() {
        Log.d(TAG, "Загрузка активной программы");
        showLoading(true);
        programManager.getActiveProgramAsync(new AsyncCallback<WorkoutProgram>() {
            @Override
            public void onSuccess(WorkoutProgram program) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    activeProgram = program;
                    if (activeProgram != null) {
                        Log.d(TAG, "Активная программа найдена: " + activeProgram.getName());
                        updateActiveWorkoutCard();


                        SharedPreferences prefs = requireContext().getSharedPreferences("vitamove_user_prefs", Context.MODE_PRIVATE);
                        String programDataKey = "program_data_loaded_" + activeProgram.getId();
                        boolean isProgramDataLoaded = prefs.getBoolean(programDataKey, false);

                        if (!isProgramDataLoaded) {


                            Log.d(TAG, "Первый вход для программы " + activeProgram.getId() + ", принудительно обновляем данные с сервера");
                            programManager.fetchAndCacheFullProgramAsync(activeProgram.getId(), new AsyncCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Log.d(TAG, "Данные программы принудительно обновлены с сервера");

                                    prefs.edit().putBoolean(programDataKey, true).apply();
                                    loadAndDisplayActiveProgramDetails();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Ошибка при обновлении данных программы с сервера: " + e.getMessage());

                                    loadAndDisplayActiveProgramDetails();
                                }
                            });
                        } else {

                            Log.d(TAG, "Повторный вход для программы " + activeProgram.getId() + ", загружаем из кэша");
                            loadAndDisplayActiveProgramDetails();
                        }
                    } else {
                        Log.d(TAG, "Активная программа не найдена.");
                        binding.activeProgramCard.setVisibility(View.GONE);
                        hideActiveProgramDays();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Log.e(TAG, "Ошибка при загрузке активной программы: " + e.getMessage(), e);
                    binding.activeProgramCard.setVisibility(View.GONE);
                    hideActiveProgramDays();
                    showError("Не удалось загрузить активную программу");
                });
            }
        });
    }


    private void loadAndDisplayActiveProgramDetails() {
        if (activeProgram == null || activeProgram.getId() == null) {
            hideActiveProgramDays();
            Log.e(TAG, "loadAndDisplayActiveProgramDetails: Не удалось загрузить детали - activeProgram пустой или без ID");
            return;
        }

        final String programId = activeProgram.getId();
        Log.d(TAG, "loadAndDisplayActiveProgramDetails: Начало загрузки деталей для программы ID: " + programId);
        showLoading(true);


        ProgramRoomCache.getProgramAsync(programId, new AsyncCallback<Program>() {
            @Override
            public void onSuccess(Program cachedProgram) {
                Log.d(TAG, "loadAndDisplayActiveProgramDetails: Кэш Room успешно получен для " + programId);
                List<ProgramDay> programDays = cachedProgram.getDays();

                if (programDays != null && !programDays.isEmpty()) {
                    Log.d(TAG, "loadAndDisplayActiveProgramDetails: Получено " + programDays.size() + " дней программы");

                    fetchAndCombineWorkoutPlans(programId, programDays);


                    if (activeProgram != null) {
                        fetchAndUpdateProgramProgress(activeProgram);
                    }
                } else {

                    Log.w(TAG, "loadAndDisplayActiveProgramDetails: Список дней пуст в кэше Room для " + programId);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            hideActiveProgramDays();
                            showLoading(false);

                            Log.w(TAG, "loadAndDisplayActiveProgramDetails: Дни не получены, принудительно запрашиваем с сервера");

                            programManager.fetchAndCacheFullProgramAsync(programId, new AsyncCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Log.d(TAG, "Программа успешно загружена с сервера, обновляем UI");
                                    loadAndDisplayActiveProgramDetails();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Не удалось загрузить программу с сервера: " + e.getMessage());
                                }
                            });


                            if (activeProgram != null) {
                                fetchAndUpdateProgramProgress(activeProgram);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "loadAndDisplayActiveProgramDetails: Ошибка получения программы из кэша Room для " + programId, e);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "loadAndDisplayActiveProgramDetails: Принудительно запрашиваем программу с сервера после ошибки кэша");

                        programManager.fetchAndCacheFullProgramAsync(programId, new AsyncCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "Программа успешно загружена с сервера после ошибки кэша, обновляем UI");
                                loadAndDisplayActiveProgramDetails();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Не удалось загрузить программу с сервера после ошибки кэша: " + e.getMessage());
                                hideActiveProgramDays();
                                showLoading(false);
                            }
                        });


                        if (activeProgram != null) {
                            fetchAndUpdateProgramProgress(activeProgram);
                        }
                    });
                }
            }
        });
    }


    private void fetchAndCombineWorkoutPlans(String programId, List<ProgramDay> programDays) {
        Log.d(TAG, "fetchAndCombineWorkoutPlans: Запрос кэшированных планов для программы ID: " + programId);
        programManager.getCachedWorkoutPlansAsync(programId, new AsyncCallback<List<WorkoutPlan>>() {
            @Override
            public void onSuccess(List<WorkoutPlan> workoutPlans) {
                Log.d(TAG, "fetchAndCombineWorkoutPlans: Получено " + (workoutPlans != null ? workoutPlans.size() : 0) + " планов из кэша.");
                combineAndDisplayDaysAndPlans(programDays, workoutPlans);
            }

            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "fetchAndCombineWorkoutPlans: Ошибка получения планов из кэша", error);

                combineAndDisplayDaysAndPlans(programDays, null);
            }
        });
    }


    private void combineAndDisplayDaysAndPlans(List<ProgramDay> programDays, @Nullable List<WorkoutPlan> workoutPlans) {
        Log.d(TAG, "combineAndDisplayDaysAndPlans: Объединение данных и обновление UI.");

        Map<String, WorkoutPlan> planMap = new HashMap<>();


        if (workoutPlans != null) {
            Log.d(TAG, "ОТЛАДКА ПЛАНОВ: Получено " + workoutPlans.size() + " планов");
            for (WorkoutPlan plan : workoutPlans) {
                Log.d(TAG, "ОТЛАДКА ПЛАНОВ: План ID=" + plan.getId()
                        + ", program_day_id=" + plan.getProgramDayId()
                        + ", status=" + plan.getStatus()
                        + ", дата=" + new Date(plan.getPlannedDate()));
                if (plan.getProgramDayId() != null) {
                    planMap.put(plan.getProgramDayId(), plan);
                }
            }
        } else {
            Log.d(TAG, "ОТЛАДКА ПЛАНОВ: workoutPlans равен null");
        }


        for (ProgramDay day : programDays) {
            WorkoutPlan correspondingPlan = planMap.get(day.getId());
            if (correspondingPlan != null) {
                day.setPlannedTimestamp(correspondingPlan.getPlannedDate());


                Log.d(TAG, "ОТЛАДКА ДНЕЙ: День " + day.getDayNumber()
                        + " (ID: " + day.getId()
                        + ") найден соответствующий план со статусом: "
                        + correspondingPlan.getStatus());


                if ("completed".equals(correspondingPlan.getStatus())) {
                    day.setCompleted(true);
                    day.setStatus("completed");
                    Log.d(TAG, "ОТЛАДКА ДНЕЙ: Установлен статус completed для дня " + day.getDayNumber());
                } else if ("skipped".equals(correspondingPlan.getStatus())) {
                    day.setStatus("skipped");
                    Log.d(TAG, "ОТЛАДКА ДНЕЙ: Установлен статус skipped для дня " + day.getDayNumber());
                } else {
                    day.setStatus(correspondingPlan.getStatus());
                    Log.d(TAG, "ОТЛАДКА ДНЕЙ: Установлен статус " + correspondingPlan.getStatus() + " для дня " + day.getDayNumber());
                }

                Log.d("VITAMOVE_PLAN_CACHE", "День " + day.getDayNumber() + " (ID: " + day.getId() + ") -> Дата: " + new Date(day.getPlannedTimestamp()) + ", Статус: " + day.getStatus());
            } else {
                day.setPlannedTimestamp(0);
                day.setStatus("planned");
                Log.d(TAG, "ОТЛАДКА ДНЕЙ: Не найден план для дня " + day.getDayNumber() + " (ID: " + day.getId() + "), устанавливаем статус planned");
            }
        }


        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!programDays.isEmpty()) {
                    Log.d(TAG, "combineAndDisplayDaysAndPlans: Обновление адаптера с " + programDays.size() + " днями.");

                    for (ProgramDay day : programDays) {
                        Log.d(TAG, "ФИНАЛЬНЫЕ ДАННЫЕ: День " + day.getDayNumber()
                                + " (ID: " + day.getId()
                                + "), статус: " + day.getStatus()
                                + ", isCompleted: " + day.isCompleted());
                    }
                    programDayAdapter.updateDays(programDays);
                    binding.activeProgramDaysTitle.setVisibility(View.VISIBLE);
                    binding.activeProgramDaysRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    Log.w(TAG, "combineAndDisplayDaysAndPlans: Список дней пуст после обработки.");
                    hideActiveProgramDays();
                }
                showLoading(false);
            });
        }
    }


    private void hideActiveProgramDays() {
        binding.activeProgramDaysTitle.setVisibility(View.GONE);

        binding.activeProgramDaysRecyclerView.setVisibility(View.GONE);
        if (programDayAdapter != null)
            programDayAdapter.updateDays(new ArrayList<>());
    }


    private List<ProgramDay> parseProgramDaysFromJson(JSONObject programJson) {
        List<ProgramDay> daysList = new ArrayList<>();
        try {
            if (programJson.has("days")) {
                JSONArray daysArray = programJson.getJSONArray("days");
                for (int i = 0; i < daysArray.length(); i++) {
                    JSONObject dayJson = daysArray.getJSONObject(i);
                    ProgramDay day = new ProgramDay();

                    day.setId(dayJson.optString("id"));
                    day.setProgramId(dayJson.optString("program_id", activeProgram.getId()));
                    day.setDayNumber(dayJson.optInt("day_number"));
                    day.setName(dayJson.optString("name", "День " + day.getDayNumber()));
                    day.setDescription(dayJson.optString("description"));


                    if (dayJson.has("exercises")) {
                        JSONArray exercisesArray = dayJson.getJSONArray("exercises");


                    } else {

                    }

                    daysList.add(day);
                }

                daysList.sort(Comparator.comparingInt(ProgramDay::getDayNumber));
                return daysList;
            } else {
                Log.w(TAG, "Ключ 'days' отсутствует в JSON программы.");
                return null;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка парсинга дней программы из JSON", e);
            return null;
        }
    }


    private void updateActiveWorkoutCard() {
        Log.d(TAG, "Обновление карточки активной программы. activeProgram: " + (activeProgram != null ? activeProgram.getId() : "null"));
        try {

            RecyclerView programsRecyclerView = null;
            com.google.android.material.textfield.TextInputLayout searchLayoutRef = null;

            try {
                programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                searchLayoutRef = requireView().findViewById(R.id.search_layout);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении ссылок на UI компоненты: " + e.getMessage(), e);
            }

            if (activeProgram != null) {

                if (searchLayoutRef != null) {
                    searchLayoutRef.setVisibility(View.GONE);
                    Log.d(TAG, "Скрываем строку поиска");
                }
                binding.userProgramsTitle.setVisibility(View.GONE);


                binding.userProgramsRecyclerView.setVisibility(View.GONE);

                binding.fabCreateProgram.setVisibility(View.GONE);


                binding.activeProgramName.setText(activeProgram.getName());


                binding.activeProgramLevel.setText(activeProgram.getLevel());


                fetchAndUpdateProgramProgress(activeProgram);

                binding.activeProgramCard.setOnClickListener(v -> showActiveProgramInfo(activeProgram));


                binding.activeProgramCard.setVisibility(View.VISIBLE);
                if (programsRecyclerView != null) {
                    programsRecyclerView.setVisibility(View.GONE);
                    Log.d(TAG, "Скрываем список программ");
                }

            } else {

                binding.activeProgramCard.setVisibility(View.GONE);
                Log.d(TAG, "Скрываем карточку активной программы");


                if (searchLayoutRef != null) {
                    searchLayoutRef.setVisibility(View.VISIBLE);

                }


                if (!userPrograms.isEmpty()) {
                    binding.userProgramsTitle.setVisibility(View.VISIBLE);

                }

                if (!userPrograms.isEmpty()) {
                    binding.userProgramsRecyclerView.setVisibility(View.VISIBLE);

                }


                if (programsRecyclerView != null) {
                    programsRecyclerView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Показываем список программ");
                }

                binding.fabCreateProgram.setVisibility(View.VISIBLE);
                Log.d(TAG, "Показываем кнопку добавления программы");

            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении карточки активной программы: " + e.getMessage(), e);
        }
    }


    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void createNewProgram() {
        CreateWorkoutFragment createFragment = CreateWorkoutFragment.newInstance();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, createFragment)
                .addToBackStack(null)
                .commit();
    }


    private void continueProgramWorkout(WorkoutProgram program) {
        if (program == null || program.getId() == null) {
            Log.e(TAG, "Невозможно продолжить тренировку: программа или её ID равны null");
            showError("Ошибка: программа не найдена");
            return;
        }


        showLoading(true);


        String programId = program.getId();

        executorService.execute(() -> {
            try {

                programManager.fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {

                        findAndStartNextWorkout(programId);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Ошибка обновления кэша планов: " + e.getMessage(), e);

                        findAndStartNextWorkout(programId);
                    }
                });
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Ошибка при поиске планов тренировок: " + e.getMessage());
                    });
                }
            }
        });
    }


    private void findAndStartNextWorkout(String programId) {
        executorService.execute(() -> {
            try {
                List<WorkoutPlan> workoutPlans = programManager.getWorkoutPlansByProgramId(programId);

                if (workoutPlans == null || workoutPlans.isEmpty()) {
                    Log.e(TAG, "Планы тренировок не найдены для программы: " + programId);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            showError("Не найдены тренировки в программе");
                        });
                    }
                    return;
                }

                Log.d(TAG, "Получено " + workoutPlans.size() + " планов тренировок для программы: " + programId);


                for (WorkoutPlan plan : workoutPlans) {
                    if ("in_progress".equals(plan.getStatus())) {
                        Log.d(TAG, "Найден активный план тренировки (in_progress): " + plan.getName() + " для программы: " + programId);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                navigateToActiveWorkout();
                            });
                        }
                        return;
                    }
                }


                Log.d(TAG, "Активная тренировка (in_progress) не найдена для программы: " + programId + ". Ищем следующий план.");


                Collections.sort(workoutPlans, (p1, p2) -> Long.compare(p1.getPlannedDate(), p2.getPlannedDate()));

                WorkoutPlan planToPropose = null;
                boolean allPlansAreTrulyCompleted = true;

                if (workoutPlans.isEmpty()) {
                    Log.d(TAG, "Список планов пуст для программы: " + programId + " после сортировки.");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            showError("В программе нет тренировок.");
                        });
                    }
                    return;
                }

                for (WorkoutPlan plan : workoutPlans) {
                    if (!"completed".equals(plan.getStatus())) {
                        allPlansAreTrulyCompleted = false;
                        if (planToPropose == null && !"skipped".equals(plan.getStatus())) {
                            planToPropose = plan;
                        }
                    }
                }

                if (allPlansAreTrulyCompleted) {
                    Log.d(TAG, "Все дни программы (" + programId + ") фактически завершены. 'Продолжить' неактивно.");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Программа полностью завершена.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                if (planToPropose == null) {
                    Log.d(TAG, "Не найден подходящий следующий план для запуска (возможно, все оставшиеся пропущены или нет незавершенных) для программы: " + programId);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Нет доступных тренировок для запуска.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }


                Log.d(TAG, "Найден следующий план для возможного запуска: " + planToPropose.getName());

                final WorkoutPlan planToStartDialog = planToPropose;


                if (getActivity() != null) {
                    final WorkoutPlan finalNextPlan = planToStartDialog;
                    getActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Подтверждение")
                                .setMessage("Вы действительно хотите запустить тренировку '" + finalNextPlan.getName() + "' прямо сейчас?")
                                .setPositiveButton("Запустить", (dialog, which) -> {

                                    executorService.execute(() -> createAndStartWorkout(finalNextPlan, programId));
                                })
                                .setNegativeButton("Отмена", (dialog, which) -> {

                                    showLoading(false);
                                    Log.d(TAG, "Запуск тренировки отменен пользователем.");
                                })
                                .setOnCancelListener(dialog -> {

                                    showLoading(false);
                                    Log.d(TAG, "Диалог запуска тренировки отменен.");
                                })
                                .show();
                    });
                } else {

                    showLoading(false);
                    Log.e(TAG, "Activity is null, не могу показать диалог подтверждения.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при поиске следующего плана: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Ошибка при поиске следующего дня программы: " + e.getMessage());
                    });
                }
            }
        });
    }


    private void navigateToActiveWorkout() {
        try {


            WorkoutStartedEvent event =
                    new WorkoutStartedEvent(System.currentTimeMillis());
            org.greenrobot.eventbus.EventBus.getDefault().post(event);


            View workoutsTab = requireActivity().findViewById(R.id.nav_workouts);
            if (workoutsTab != null) {
                workoutsTab.performClick();
                Log.d(TAG, "Переключение на вкладку тренировок для отображения активной тренировки");
            } else {
                Log.e(TAG, "Не удалось найти кнопку навигации тренировок");

                if (requireActivity() instanceof MainActivity) {
                    View view = requireActivity().findViewById(R.id.nav_workouts);
                    if (view != null) {
                        view.performClick();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при переходе к экрану активной тренировки: " + e.getMessage(), e);
            showError("Не удалось открыть экран тренировки");
        }
    }


    @Override
    public void onProgramClick(WorkoutProgram program) {

        navigateToProgramDetails(program.getId());
    }

    @Override
    public void onStartClick(WorkoutProgram program) {

        startProgram(program);
    }

    @Override
    public void onSetupClick(WorkoutProgram program) {

        navigateToProgramSetup(program.getId());
    }

    @Override
    public void onDetailsClick(WorkoutProgram program) {

        navigateToProgramDetails(program.getId());
    }


    private void navigateToProgramDetails(String programId) {

        requireActivity().getSharedPreferences("VitaMovePrefs", 0)
                .edit()
                .putInt("workout_tab_index", 2)
                .apply();

        ProgramDetailsFragment detailsFragment = ProgramDetailsFragment.newInstance(programId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }


    private void navigateToProgramSetup(String programId) {
        ProgramSetupFragment setupFragment = ProgramSetupFragment.newInstance(programId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, setupFragment)
                .addToBackStack(null)
                .commit();
    }


    private void startProgram(WorkoutProgram program) {
        showLoading(true);


        long startDate = System.currentTimeMillis();

        programManager.startProgramAsync(program.getId(), startDate, new AsyncCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (result != null && !result.isEmpty()) {
                            Toast.makeText(requireContext(),
                                    getString(R.string.program_started_success),
                                    Toast.LENGTH_SHORT).show();


                            programManager.fetchAndCacheWorkoutPlansAsync(program.getId(), new AsyncCallback<Void>() {
                                @Override
                                public void onSuccess(Void ignored) {
                                    Log.d(TAG, "Кэш планов обновлен после старта программы");


                                    programManager.getActiveProgramAsync(new AsyncCallback<WorkoutProgram>() {
                                        @Override
                                        public void onSuccess(WorkoutProgram activatedProgram) {
                                            if (activatedProgram != null) {
                                                Log.d(TAG, "Активная программа успешно получена после запуска: " + activatedProgram.getName());
                                                activeProgram = activatedProgram;


                                                updateActiveWorkoutCard();


                                                loadAndDisplayActiveProgramDetails();


                                                RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                                                if (programsRecyclerView != null) {
                                                    programsRecyclerView.setVisibility(View.GONE);
                                                }

                                                showLoading(false);
                                            } else {

                                                showLoading(false);
                                                navigateToProgramDetails(program.getId());
                                            }
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e(TAG, "Не удалось загрузить активированную программу", e);
                                            showLoading(false);

                                            navigateToProgramDetails(program.getId());
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Ошибка обновления кэша планов после старта программы", e);

                                    programManager.getActiveProgramAsync(new AsyncCallback<WorkoutProgram>() {
                                        @Override
                                        public void onSuccess(WorkoutProgram activatedProgram) {
                                            if (activatedProgram != null) {
                                                activeProgram = activatedProgram;
                                                updateActiveWorkoutCard();
                                                loadAndDisplayActiveProgramDetails();
                                                binding.programsRecyclerView.setVisibility(View.GONE);
                                                showLoading(false);
                                            } else {
                                                showLoading(false);
                                                navigateToProgramDetails(program.getId());
                                            }
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e(TAG, "Не удалось загрузить активированную программу", e);
                                            showLoading(false);
                                            navigateToProgramDetails(program.getId());
                                        }
                                    });
                                }
                            });
                        } else {
                            showLoading(false);
                            showError("Не удалось начать программу");
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Ошибка при запуске программы: " + e.getMessage());
                    });
                }
            }
        });
    }


    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Ошибка: " + message);
        }
    }


    private void changeProgram(WorkoutProgram program) {
        if (program == null) {
            Log.e(TAG, "Попытка деактивировать null программу");
            Toast.makeText(requireContext(), "Ошибка: программа не найдена", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Запрос на смену программы: " + program.getId());


        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.program_change_title)
                .setMessage(R.string.program_change_message)
                .setPositiveButton(R.string.program_change_confirm, (dialog, which) -> {

                    showLoading(true);


                    String programId = program.getId();
                    Log.d(TAG, "Смена программы с ID: " + programId);


                    String userId = ((VitaMoveApplication) requireActivity().getApplication()).getCurrentUserId();
                    if (userId == null || userId.isEmpty()) {
                        Log.e(TAG, "Не удалось получить ID пользователя");
                        showError("Ошибка: не удалось определить пользователя");
                        showLoading(false);
                        return;
                    }

                    try {

                        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                                Constants.SUPABASE_CLIENT_ID,
                                Constants.SUPABASE_CLIENT_SECRET
                        );


                        showLoading(true);


                        supabaseClient.rpc("clean_program_data")
                                .param("p_program_id", programId)
                                .param("p_user_id", userId)
                                .executeAsync(new SupabaseCallback<String>() {
                                    @Override
                                    public void onSuccess(String responseBody) {
                                        Log.d(TAG, "Результат выполнения clean_program_data: " + responseBody);


                                        Activity activity = getActivity();
                                        if (activity != null) {
                                            activity.runOnUiThread(() -> {
                                                Log.d(TAG, "Программа успешно деактивирована через RPC: " + programId);


                                                programManager.clearProgramCache();

                                                Toast.makeText(requireContext(),
                                                        getString(R.string.program_deactivated),
                                                        Toast.LENGTH_SHORT).show();


                                                activeProgram = null;


                                                updateActiveWorkoutCard();


                                                hideActiveProgramDays();


                                                try {

                                                    RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);


                                                    if (programsRecyclerView != null) {
                                                        programsRecyclerView.setVisibility(View.VISIBLE);
                                                        Log.d(TAG, "Показываем список программ после смены программы");
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Ошибка при отображении компонентов после смены программы: " + e.getMessage(), e);
                                                }


                                                loadPrograms(false);


                                                showLoading(false);
                                            });
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Ошибка при вызове RPC-функции clean_program_data: " + e.getMessage(), e);
                                        Activity activity = getActivity();
                                        if (activity != null) {
                                            activity.runOnUiThread(() -> {
                                                Log.e(TAG, "Ошибка при смене программы: " + e.getMessage(), e);


                                                activeProgram = null;
                                                updateActiveWorkoutCard();


                                                hideActiveProgramDays();


                                                try {

                                                    RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);


                                                    if (programsRecyclerView != null) {
                                                        programsRecyclerView.setVisibility(View.VISIBLE);
                                                    }
                                                } catch (Exception ex) {
                                                    Log.e(TAG, "Ошибка при отображении компонентов после ошибки: " + ex.getMessage(), ex);
                                                }


                                                loadPrograms(false);


                                                showError(getString(R.string.program_deactivation_error) + ": " + e.getMessage());


                                                showLoading(false);
                                            });
                                        }
                                    }
                                });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при запуске асинхронной задачи очистки программы: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), getString(R.string.program_deactivation_error), Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                })
                .setNegativeButton(R.string.program_change_cancel, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ProgramsFragment resumed");


        Log.d(TAG, "onResume: Выполняется отложенная загрузка программ");
        loadPrograms();


        loadActiveProgram();


        if (activeProgram != null) {
            updateActiveWorkoutCard();
        }
    }


    private void refreshWorkoutPlansCache(String programId) {
        if (programId == null || programId.isEmpty()) {
            Log.e(TAG, "refreshWorkoutPlansCache: Program ID не может быть пустым");
            return;
        }

        Log.d(TAG, "Запуск обновления кэша планов для программы ID: " + programId);


        programManager.fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "refreshWorkoutPlansCache: Кэш планов успешно обновлен для программы ID: " + programId);


                loadAndDisplayActiveProgramDetails();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "refreshWorkoutPlansCache: Ошибка обновления кэша планов", e);


                loadAndDisplayActiveProgramDetails();
            }
        });
    }


    private void navigateToActiveProgramDetails() {
        if (activeProgram != null) {
            Log.d(TAG, "Начинаем навигацию к деталям активной программы: " + activeProgram.getId());


            try {

                RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                com.google.android.material.textfield.TextInputLayout searchLayoutRef = requireView().findViewById(R.id.search_layout);

                if (searchLayoutRef != null) {
                    searchLayoutRef.setVisibility(View.GONE);
                    Log.d(TAG, "(Навигация) Скрываем строку поиска");
                }


                if (programsRecyclerView != null) {
                    programsRecyclerView.setVisibility(View.GONE);
                    Log.d(TAG, "(Навигация) Скрываем список программ");
                }
            } catch (Exception e) {
                Log.e(TAG, "(Навигация) Ошибка при скрытии элементов перед переходом: " + e.getMessage(), e);
            }


        } else {
            Log.e(TAG, "Попытка перехода к активной программе, но программа не загружена");
            showError("Активная программа не загружена");
        }
    }


    private void setupActiveProgramDaysRecyclerView() {
        programDayAdapter = new ProgramDayAdapter(new ArrayList<>(), programDay -> {

            Log.d(TAG, "Клик на день программы: " + programDay.getName());
            navigateToProgramDayDetails(programDay.getId());
        });
        binding.activeProgramDaysRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.activeProgramDaysRecyclerView.setAdapter(programDayAdapter);
        binding.activeProgramDaysRecyclerView.setNestedScrollingEnabled(false);
    }


    private void navigateToProgramDayDetails(String dayId) {
        if (dayId == null || dayId.isEmpty()) {
            showError("ID дня программы не найден");
            return;
        }

        try {

            ProgramDayDetailsFragment detailsFragment = ProgramDayDetailsFragment.newInstance(dayId);


            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailsFragment)
                    .addToBackStack(null)
                    .commit();

            Log.d(TAG, "Переход к деталям дня программы: " + dayId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при переходе к деталям дня программы: " + e.getMessage(), e);
            showError("Не удалось открыть детали дня программы");
        }
    }


    private void showActiveProgramInfo(WorkoutProgram program) {
        if (program != null) {
            Log.d(TAG, "Показываем информацию о программе: " + program.getName());
            ActiveProgramInfoBottomSheet bottomSheet = ActiveProgramInfoBottomSheet.newInstance(program);
            bottomSheet.show(getChildFragmentManager(), "ActiveProgramInfoBottomSheet");
        } else {
            Log.e(TAG, "Невозможно показать информацию: программа равна null");
        }
    }


    @Override
    public void onDeleteClick(WorkoutProgram program) {
        if (program == null || program.getId() == null) {
            showError("Невозможно удалить программу: отсутствует ID программы");
            return;
        }


        String userId = ((VitaMoveApplication) requireContext().getApplicationContext()).getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            showError("Невозможно удалить программу: не удалось определить ID пользователя");
            return;
        }


        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление программы")
                .setMessage("Вы действительно хотите удалить программу \"" + program.getName() + "\"? Это действие невозможно отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> {

                    deleteUserProgram(program.getId(), userId);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void deleteUserProgram(String programTemplateId, String authorId) {

        showLoading(true);


        executorService.execute(() -> {
            try {

                SupabaseClient supabaseClient = SupabaseClient.getInstance(
                        Constants.SUPABASE_CLIENT_ID,
                        Constants.SUPABASE_CLIENT_SECRET
                );


                Log.d(TAG, "Удаление программы: ID=" + programTemplateId + ", authorId=" + authorId);


                supabaseClient.from("program_templates")
                        .eq("id", programTemplateId)
                        .eq("author_id", authorId)
                        .delete()
                        .executeDelete();


                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    showLoading(false);

                    Toast.makeText(requireContext(), "Программа успешно удалена", Toast.LENGTH_SHORT).show();


                    loadPrograms();
                });
            } catch (Exception e) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Log.e(TAG, "Ошибка при удалении программы: " + e.getMessage(), e);
                    showError("Не удалось удалить программу: " + e.getMessage());
                });
            }
        });
    }


    private void fetchAndUpdateProgramProgress(WorkoutProgram program) {
        if (program == null || program.getId() == null) {
            Log.e(TAG, "Невозможно обновить прогресс: программа или её ID равны null");
            return;
        }

        String programId = program.getId();
        Log.d(TAG, "Загрузка прогресса программы с ID: " + programId);


        programManager.fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {

                loadWorkoutPlansAndUpdateProgress(programId, binding.activeProgramProgressText, binding.progressBar, binding.activeProgramProgressWeek);
            }

            @Override
            public void onFailure(Exception e) {

                loadWorkoutPlansAndUpdateProgress(programId, binding.activeProgramProgressText, binding.progressBar, binding.activeProgramProgressWeek);
            }
        });
    }


    private void loadWorkoutPlansAndUpdateProgress(String programId, TextView programProgressTextView,
                                                   ProgressBar progressBar, TextView nextDayTextView) {

        executorService.execute(() -> {
            try {
                List<WorkoutPlan> workoutPlans = programManager.getWorkoutPlansByProgramId(programId);

                if (workoutPlans == null || workoutPlans.isEmpty()) {
                    Log.d(TAG, "Планы тренировок не найдены для программы: " + programId);

                    updateProgressUI(0, programProgressTextView, progressBar, "Программа загружается...", nextDayTextView);
                    return;
                }

                Log.d(TAG, "Получено " + workoutPlans.size() + " планов тренировок для программы");


                int totalPlans = workoutPlans.size();
                int completedPlans = 0;


                WorkoutPlan nextDay = null;


                Collections.sort(workoutPlans, (p1, p2) -> Long.compare(p1.getPlannedDate(), p2.getPlannedDate()));

                for (WorkoutPlan plan : workoutPlans) {
                    if ("completed".equals(plan.getStatus())) {
                        completedPlans++;
                    } else if (nextDay == null && !"completed".equals(plan.getStatus()) && !"skipped".equals(plan.getStatus())) {

                        nextDay = plan;
                    }
                }

                Log.d(TAG, "Выполнено " + completedPlans + " из " + totalPlans + " тренировок");


                final float progressPercentage = totalPlans > 0 ?
                        (float) completedPlans / totalPlans * 100 : 0;


                final String nextDayName;
                if (nextDay != null) {
                    nextDayName = nextDay.getName();
                    Log.d(TAG, "Следующий день: " + nextDayName);
                } else if (completedPlans == totalPlans && totalPlans > 0) {
                    nextDayName = null;
                    Log.d(TAG, "Все дни программы выполнены, программа завершена");
                } else {
                    nextDayName = "Следующая тренировка";
                    Log.d(TAG, "Не найден следующий день тренировки");
                }


                updateProgressUI(progressPercentage, programProgressTextView, progressBar, nextDayName, nextDayTextView);

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении планов тренировок: " + e.getMessage(), e);

                updateProgressUI(0, programProgressTextView, progressBar, "Проверка прогресса...", nextDayTextView);
            }
        });
    }

    private void updateProgressUI(float progressPercentage, TextView progressTextView, ProgressBar progressBar,
                                  String nextDayName, TextView nextDayTextView) {

        if (Looper.myLooper() != Looper.getMainLooper()) {

            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        updateProgressUI(progressPercentage, progressTextView, progressBar, nextDayName, nextDayTextView));
            }
            return;
        }


        if (progressTextView != null) {
            String progressText = String.format("%.0f%% завершено", progressPercentage);
            progressTextView.setText(progressText);
        }

        if (progressBar != null) {
            progressBar.setProgress((int) progressPercentage);
        }

        if (nextDayTextView != null) {
            if (nextDayName != null) {
                nextDayTextView.setText(nextDayName);
            } else {
                nextDayTextView.setText("Программа завершена");
            }
        }
    }

    private void createAndStartWorkout(WorkoutPlan planToStart, String programId) {
        try {

            String workoutId = workoutRepository.createWorkoutFromPlan(planToStart);

            if (workoutId != null && !workoutId.isEmpty()) {
                WorkoutStartedEvent event = new WorkoutStartedEvent(System.currentTimeMillis());
                org.greenrobot.eventbus.EventBus.getDefault().post(event);


                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        navigateToActiveWorkout();
                    });
                }
            } else {
                Log.e(TAG, "Создание тренировки из плана вернуло null ID для программы: " + programId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Не удалось создать тренировку из плана");
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании тренировки из плана: " + planToStart.getName() + " для программы: " + programId + ": " + e.getMessage(), e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showError("Ошибка при создании тренировки: " + e.getMessage());
                });
            }
        }
    }

    private void showProgramDaysConfigDialog(WorkoutProgram program) {
        if (program == null) {
            Log.e(TAG, "Невозможно показать диалог настройки: программа равна null");
            return;
        }

        ProgramDaysConfigDialog dialog = ProgramDaysConfigDialog.newInstance(
                program.getId(),
                program.getWorkoutDays(),
                program.getDaysPerWeek()
        );

        dialog.setOnDaysUpdatedListener(newDays -> updateProgramDays(program.getId(), newDays));

        dialog.show(getChildFragmentManager(), "ProgramDaysConfigDialog");
    }


    private void updateProgramDays(String programId, List<Integer> newWorkoutDays) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Изменить расписание?")
                .setMessage("Это обновит даты всех будущих тренировок. Уже выполненные тренировки останутся без изменений.\n\nПродолжить?")
                .setPositiveButton("Да", (dialog, which) -> {
                    showLoading(true);

                    programManager.updateProgramWorkoutDaysAndReschedulePlansAsync(programId, newWorkoutDays, new AsyncCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(requireContext(), "Расписание обновлено", Toast.LENGTH_SHORT).show();


                                    Log.d(TAG, "Перезагрузка активной программы после обновления дней");
                                    loadActiveProgram();
                                });
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showLoading(false);
                                    showError("Не удалось обновить расписание: " + e.getMessage());
                                });
                            }
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
} 