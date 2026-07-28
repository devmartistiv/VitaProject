package com.martist.vitamove.programs.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.AsyncCallback;
import com.martist.vitamove.programs.data.local.ProgramRoomCache;
import com.martist.vitamove.programs.data.local.ProgramRoomDatabase;
import com.martist.vitamove.programs.data.local.entities.ProgramDayEntity;
import com.martist.vitamove.programs.data.local.entities.ProgramExerciseEntity;
import com.martist.vitamove.programs.ui.model.ProgramDay;
import com.martist.vitamove.programs.ui.model.ProgramExercise;
import com.martist.vitamove.programs.ui.model.ProgramSetupConfig;
import com.martist.vitamove.workout.data.entities.WorkoutPlanEntity;
import com.martist.vitamove.workout.data.model.WorkoutPlan;
import com.martist.vitamove.workout.data.model.WorkoutProgram;
import com.martist.vitamove.workout.data.repository.SupabaseProgramRepository;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ProgramManager {
    private static final String TAG = "ProgramManager";
    private static ProgramManager instance;
    private final SupabaseProgramRepository supabaseProgramRepository;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final String userId;
    private Context context;


    private ProgramManager() {

        SupabaseClient supabaseClient = SupabaseClient.getInstance(SupabaseClient.SUPABASE_URL, "public-anon-key");
        this.supabaseProgramRepository = new SupabaseProgramRepository(supabaseClient);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newCachedThreadPool();
        this.userId = "default_user";
    }


    public ProgramManager(Context context) {
        this();
        init(context);
    }


    public void init(Context context) {
        this.context = context;


        if (supabaseProgramRepository instanceof SupabaseProgramRepository) {
            supabaseProgramRepository.setContext(context);
        }
    }


    public static synchronized ProgramManager getInstance() {
        if (instance == null) {
            instance = new ProgramManager();
        }
        return instance;
    }


    public List<WorkoutProgram> getAllPrograms() {
        try {
            Log.d(TAG, "Получение всех программ из Supabase");


            List<WorkoutProgram> programs = supabaseProgramRepository.getAllPrograms();


            Log.d(TAG, "Получено программ: " + programs.size());
            return programs;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении программ тренировок: " + e.getMessage(), e);

        }
        return Collections.emptyList();
    }


    public void getAllProgramsAsync(AsyncCallback<List<WorkoutProgram>> callback) {

        if (supabaseProgramRepository instanceof SupabaseProgramRepository) {

            supabaseProgramRepository.getAllProgramsAsync(callback);
        } else {

            executor.execute(() -> {
                try {
                    List<WorkoutProgram> programs = getAllPrograms();
                    mainHandler.post(() -> callback.onSuccess(programs));
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при получении программ тренировок", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            });
        }
    }


    public List<WorkoutProgram> filterPrograms(String goal, String level, int maxDuration) {
        try {
            return supabaseProgramRepository.filterPrograms(goal, level, maxDuration);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при фильтрации программ тренировок", e);
            return new ArrayList<>();
        }
    }


    public void getProgramByIdAsync(String programId, AsyncCallback<WorkoutProgram> callback) {
        executor.execute(() -> {
            try {
                WorkoutProgram program = supabaseProgramRepository.getProgramById(programId);
                mainHandler.post(() -> callback.onSuccess(program));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public WorkoutProgram getActiveProgram() {
        try {
            return supabaseProgramRepository.getActiveProgram(userId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении активной программы пользователя: " + userId, e);
            return null;
        }
    }


    public WorkoutProgram getActiveProgram(String userId) {
        try {
            return supabaseProgramRepository.getActiveProgram(userId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении активной программы пользователя: " + userId, e);
            return null;
        }
    }


    public void getActiveProgramAsync(AsyncCallback<WorkoutProgram> callback) {
        executor.execute(() -> {
            try {
                WorkoutProgram program = getActiveProgram();
                mainHandler.post(() -> callback.onSuccess(program));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении активной программы", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public String startProgram(String programId, long startDate) {
        try {
            return supabaseProgramRepository.startProgram(userId, programId, startDate);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при запуске программы: " + programId, e);
            return null;
        }
    }


    public void startProgramAsync(String programId, long startDate, AsyncCallback<String> callback) {
        executor.execute(() -> {
            try {
                String result = startProgram(programId, startDate);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public List<ProgramDay> getProgramDays(String programId) {
        try {
            Log.d(TAG, "getProgramDays: Попытка получить дни из кэша Room для программы ID: " + programId);
            if (context != null) {
                ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                List<ProgramDayEntity> dayEntities = db.programDayDao().getAllByProgramId(programId);

                if (dayEntities != null && !dayEntities.isEmpty()) {
                    Log.d(TAG, "getProgramDays: Найдено " + dayEntities.size() + " дней в кэше Room.");
                    List<ProgramDay> days = new ArrayList<>();
                    for (ProgramDayEntity entity : dayEntities) {

                        days.add(ProgramRoomCache.entityToProgramDay(entity));
                    }

                    Collections.sort(days, Comparator.comparingInt(ProgramDay::getDayNumber));
                    return days;
                } else {
                    Log.d(TAG, "getProgramDays: Дни не найдены в кэше Room.");
                }
            } else {
                Log.w(TAG, "getProgramDays: Контекст равен null, не могу получить доступ к кэшу Room.");
            }


            Log.d(TAG, "getProgramDays: Загружаю дни из репозитория для программы ID: " + programId);
            List<ProgramDay> daysFromRepo = supabaseProgramRepository.getProgramDays(programId);

            if (daysFromRepo != null) {
                Collections.sort(daysFromRepo, Comparator.comparingInt(ProgramDay::getDayNumber));
            }
            return daysFromRepo;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении дней программы: " + programId, e);
            return new ArrayList<>();
        }
    }


    public void getProgramDaysAsync(String programId, AsyncCallback<List<ProgramDay>> callback) {
        executor.execute(() -> {
            try {

                List<ProgramDay> days = getProgramDays(programId);
                mainHandler.post(() -> callback.onSuccess(days));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public List<ProgramExercise> getProgramDayExercises(String dayId) {
        try {

            Log.d(TAG, "getProgramDayExercises: Пытаюсь получить упражнения из кэша Room для дня ID: " + dayId);
            if (context != null) {
                ProgramRoomDatabase db =
                        ProgramRoomDatabase.getInstance(context);
                List<ProgramExerciseEntity> exerciseEntities =
                        db.programExerciseDao().getAllByDayId(dayId);

                if (exerciseEntities != null && !exerciseEntities.isEmpty()) {
                    Log.d(TAG, "getProgramDayExercises: Найдено " + exerciseEntities.size() + " упражнений в кэше Room для дня ID: " + dayId);
                    List<ProgramExercise> exercises = new ArrayList<>();
                    for (ProgramExerciseEntity entity : exerciseEntities) {


                        exercises.add(ProgramRoomCache.entityToProgramExercise(entity));
                    }

                    Collections.sort(exercises, Comparator.comparingInt(ProgramExercise::getOrderNumber));
                    return exercises;

                } else {
                    Log.d(TAG, "getProgramDayExercises: Упражнения не найдены в кэше Room.");
                }
            } else {
                Log.w(TAG, "getProgramDayExercises: Контекст равен null, не могу получить доступ к кэшу Room.");
            }


            Log.d(TAG, "getProgramDayExercises: Загружаю упражнения из репозитория для дня ID: " + dayId);
            return supabaseProgramRepository.getProgramDayExercises(dayId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении упражнений для дня программы: " + dayId, e);
            return new ArrayList<>();
        }
    }


    public ProgramDay getFirstProgramDay(String programId) {
        try {
            List<ProgramDay> days = getProgramDays(programId);
            if (days.isEmpty()) {
                return null;
            }


            Collections.sort(days, Comparator.comparingInt(ProgramDay::getDayNumber));
            return days.get(0);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении первого дня программы", e);
            return null;
        }
    }


    public ProgramDay getProgramDayById(String dayId) {
        try {
            Log.d(TAG, "getProgramDayById: Попытка получить день из кэша Room по ID: " + dayId);
            if (context != null) {
                ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
                ProgramDayEntity dayEntity = db.programDayDao().getById(dayId);

                if (dayEntity != null) {
                    Log.d(TAG, "getProgramDayById: День найден в кэше Room.");
                    return ProgramRoomCache.entityToProgramDay(dayEntity);
                } else {
                    Log.d(TAG, "getProgramDayById: День не найден в кэше Room.");
                }
            } else {
                Log.w(TAG, "getProgramDayById: Контекст равен null, не могу получить доступ к кэшу Room.");
            }


            Log.d(TAG, "getProgramDayById: Загружаю день из репозитория по ID: " + dayId);
            return supabaseProgramRepository.getProgramDayById(dayId);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении дня программы по ID: " + dayId, e);
            return null;
        }
    }


    public void getProgramDayByIdAsync(String dayId, AsyncCallback<ProgramDay> callback) {
        executor.execute(() -> {
            try {
                ProgramDay day = getProgramDayById(dayId);
                mainHandler.post(() -> callback.onSuccess(day));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public boolean saveProgramConfig(ProgramSetupConfig config) {
        if (config == null || !config.isValid()) {
            Log.e(TAG, "Невозможно сохранить недействительную конфигурацию программы: " + config);
            return false;
        }

        try {
            SharedPreferences sharedPrefs = context.getSharedPreferences("program_config_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefs.edit();


            editor.putString("config_program_id_" + config.getProgramId(), config.getProgramId());
            editor.putLong("config_start_date_" + config.getProgramId(), config.getStartDate());
            editor.putBoolean("config_reminders_enabled_" + config.getProgramId(), config.isRemindersEnabled());
            editor.putLong("config_reminder_time_" + config.getProgramId(), config.getReminderTime());
            editor.putBoolean("config_auto_progression_" + config.getProgramId(), config.isAutoProgression());


            StringBuilder daysBuilder = new StringBuilder();
            for (int i = 0; i < config.getWorkoutDays().size(); i++) {
                daysBuilder.append(config.getWorkoutDays().get(i));
                if (i < config.getWorkoutDays().size() - 1) {
                    daysBuilder.append(",");
                }
            }
            editor.putString("config_workout_days_" + config.getProgramId(), daysBuilder.toString());


            return editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении конфигурации программы: " + e.getMessage(), e);
            return false;
        }
    }


    public void saveProgramConfigAsync(ProgramSetupConfig config, AsyncCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean result = saveProgramConfig(config);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при асинхронном сохранении конфигурации программы: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public ProgramSetupConfig getProgramConfig(String programId) {
        if (programId == null || programId.isEmpty()) {
            Log.e(TAG, "Невозможно получить конфигурацию для null или пустого programId");
            return null;
        }

        try {
            SharedPreferences sharedPrefs = context.getSharedPreferences("program_config_prefs", Context.MODE_PRIVATE);


            if (!sharedPrefs.contains("config_program_id_" + programId)) {
                Log.d(TAG, "Конфигурация не найдена для программы: " + programId);
                return null;
            }

            ProgramSetupConfig config = new ProgramSetupConfig();
            config.setProgramId(programId);
            config.setStartDate(sharedPrefs.getLong("config_start_date_" + programId, 0));
            config.setRemindersEnabled(sharedPrefs.getBoolean("config_reminders_enabled_" + programId, false));
            config.setReminderTime(sharedPrefs.getLong("config_reminder_time_" + programId, 0));
            config.setAutoProgression(sharedPrefs.getBoolean("config_auto_progression_" + programId, true));


            String daysStr = sharedPrefs.getString("config_workout_days_" + programId, "");
            if (!daysStr.isEmpty()) {
                List<Integer> workoutDays = new ArrayList<>();
                String[] dayArray = daysStr.split(",");
                for (String day : dayArray) {
                    try {
                        workoutDays.add(Integer.parseInt(day));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Ошибка при парсинге дня тренировки: " + day, e);
                    }
                }
                config.setWorkoutDays(workoutDays);
            }

            return config;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении конфигурации программы: " + e.getMessage(), e);
            return null;
        }
    }


    public void updateProgramWorkoutDaysAsync(String programId, List<Integer> workoutDays, AsyncCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "!!! VITAMOVE_DEBUG: [UPDATE WORKOUT DAYS] Начало обновления дней тренировок для программы " + programId + ": " + workoutDays);

                WorkoutProgram program = supabaseProgramRepository.getProgramById(programId);
                if (program != null) {
                    Log.d(TAG, "!!! VITAMOVE_DEBUG: [UPDATE WORKOUT DAYS] Программа найдена, текущие дни: " + program.getWorkoutDays());

                    program.setWorkoutDays(workoutDays);
                    boolean updated = supabaseProgramRepository.updateProgram(program);
                    Log.d(TAG, "!!! VITAMOVE_DEBUG: [UPDATE WORKOUT DAYS] Программа обновлена в БД: " + updated);


                    if (supabaseProgramRepository instanceof SupabaseProgramRepository) {
                        supabaseProgramRepository.saveWorkoutDaysToPrefs(programId, workoutDays);
                        Log.d(TAG, "!!! VITAMOVE_DEBUG: [UPDATE WORKOUT DAYS] Дни тренировок сохранены в SharedPreferences: " + workoutDays);
                    }


                    if (supabaseProgramRepository instanceof SupabaseProgramRepository) {
                        List<Integer> savedDays = supabaseProgramRepository.loadWorkoutDaysFromPrefs(programId);
                        Log.d(TAG, "!!! VITAMOVE_DEBUG: [UPDATE WORKOUT DAYS] Проверка сохраненных дней тренировок: " + savedDays);
                    }

                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    Log.e(TAG, "!!! VITAMOVE_DEBUG: [UPDATE WORKOUT DAYS] Программа не найдена: " + programId);
                    throw new Exception("Программа не найдена");
                }
            } catch (Exception e) {
                Log.e(TAG, "!!! VITAMOVE_DEBUG: [UPDATE WORKOUT DAYS] Ошибка при обновлении дней тренировок программы: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public void updateProgramWorkoutDaysAndReschedulePlansAsync(String programId, List<Integer> newWorkoutDays, AsyncCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Начало обновления дней тренировок и пересчета планов для программы: " + programId);


                String userId = ((com.martist.vitamove.VitaMoveApplication) context.getApplicationContext()).getCurrentUserId();

                if (userId == null || userId.isEmpty()) {
                    throw new Exception("Не удалось получить ID пользователя");
                }


                org.json.JSONArray daysArray = new org.json.JSONArray();
                for (Integer day : newWorkoutDays) {
                    daysArray.put(day);
                }

                Log.d(TAG, "Вызов RPC функции update_program_workout_days с параметрами: programId=" + programId + ", userId=" + userId + ", days=" + daysArray.toString());


                SupabaseClient client = supabaseProgramRepository.getSupabaseClient();


                JSONObject result = client.rpc("update_program_workout_days")
                        .param("p_program_id", programId)
                        .param("p_user_id", userId)
                        .param("p_workout_days", daysArray)
                        .executeAndGetSingle();

                boolean success = result.getBoolean("success");
                int updatedCount = result.getInt("updated_count");

                Log.d(TAG, "RPC функция выполнена. Успех: " + success + ", обновлено планов: " + updatedCount);

                if (success) {

                    Log.d(TAG, "Сохранение новых дней в SharedPreferences для программы: " + programId);
                    supabaseProgramRepository.saveWorkoutDaysToPrefs(programId, newWorkoutDays);


                    fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
                        @Override
                        public void onSuccess(Void ignored) {
                            Log.d(TAG, "Кэш планов успешно обновлен после изменения дней");
                            mainHandler.post(() -> callback.onSuccess(true));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Ошибка обновления кэша планов: " + e.getMessage(), e);

                            mainHandler.post(() -> callback.onSuccess(true));
                        }
                    });
                } else {
                    throw new Exception("RPC функция вернула success=false");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении дней и планов: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public boolean deactivateProgram(String programId) {
        Log.d(TAG, "Деактивация программы: " + programId);
        try {
            supabaseProgramRepository.deactivateProgram(programId);


            SharedPreferences prefs = context.getSharedPreferences("program_activation_prefs", Context.MODE_PRIVATE);
            String currentActiveId = prefs.getString("active_program_id", null);


            if (programId.equals(currentActiveId)) {
                prefs.edit().remove("active_program_id").apply();
                Log.d(TAG, "Запись об активной программе удалена из SharedPreferences");
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при деактивации программы: " + e.getMessage(), e);
            return false;
        }
    }


    public ProgramExercise getProgramExerciseById(String exerciseId) {
        Log.d(TAG, "Получение упражнения программы по ID: " + exerciseId);
        try {


            return supabaseProgramRepository.getProgramExerciseById(exerciseId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении упражнения по ID: " + e.getMessage(), e);
            return null;
        }
    }


    public void clearProgramCache() {
        try {

            ProgramRoomCache.clearCache();
            Log.d(TAG, "clearProgramCache: Кэш программ очищен (ProgramRoomCache)");
        } catch (Exception e) {
            Log.e(TAG, "clearProgramCache: Ошибка при очистке кэша программ", e);
        }
    }


    public void getFullProgramAsync(String programId, AsyncCallback<JSONObject> callback) {
        Log.d(TAG, "Запрос полной программы (RPC) для ID: " + programId);

        if (supabaseProgramRepository instanceof SupabaseProgramRepository) {
            supabaseProgramRepository.getFullProgramAsync(programId, new AsyncCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {

                    Log.d(TAG, "getFullProgramAsync: Успешно получен JSON для программы ID: " + programId);
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Ошибка при получении полной программы (RPC) для ID: " + programId, e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            });
        } else {
            Log.e(TAG, "getFullProgramAsync не поддерживается текущим репозиторием");
            mainHandler.post(() -> callback.onFailure(new UnsupportedOperationException("Метод getFullProgramAsync не поддерживается")));
        }
    }


    public void fetchAndCacheWorkoutPlansAsync(String programId, AsyncCallback<Void> callback) {
        if (programId == null || programId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Program ID не может быть пустым"));
            return;
        }
        Log.d(TAG, "fetchAndCacheWorkoutPlansAsync: Начало загрузки и кэширования планов для программы ID: " + programId);
        final long startTime = System.currentTimeMillis();
        executor.execute(() -> {
            try {

                List<WorkoutPlan> plans = supabaseProgramRepository.getWorkoutPlansForProgram(programId);
                int plansCount = (plans != null) ? plans.size() : 0;
                Log.d(TAG, "fetchAndCacheWorkoutPlansAsync: Получено " + plansCount + " планов из репозитория за " + (System.currentTimeMillis() - startTime) + " мс.");

                if (plans != null) {

                    Log.d(TAG, "fetchAndCacheWorkoutPlansAsync: Вызов ProgramRoomCache.saveWorkoutPlans для " + plansCount + " планов.");
                    ProgramRoomCache.saveWorkoutPlans(programId, plans);


                    mainHandler.post(() -> callback.onSuccess(null));
                } else {

                    Log.w(TAG, "fetchAndCacheWorkoutPlansAsync: Репозиторий вернул null для планов программы " + programId);
                    throw new Exception("Не удалось получить планы из репозитория (null)");
                }

            } catch (Exception e) {
                Log.e(TAG, "fetchAndCacheWorkoutPlansAsync: Ошибка при загрузке или кэшировании планов для programId " + programId, e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public void getCachedWorkoutPlansAsync(String programId, AsyncCallback<List<WorkoutPlan>> callback) {
        if (programId == null || programId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Program ID не может быть пустым"));
            return;
        }
        Log.d(TAG, "getCachedWorkoutPlansAsync: Запрос кэшированных планов из Room для программы ID: " + programId);

        ProgramRoomCache.getWorkoutPlansByProgramId(programId, callback);
    }


    public List<WorkoutPlan> getWorkoutPlansByProgramId(String programId) {
        if (programId == null || programId.isEmpty()) {
            Log.e(TAG, "getWorkoutPlansByProgramId: programId не может быть null или пустым");
            return new ArrayList<>();
        }

        Log.d(TAG, "getWorkoutPlansByProgramId: Получение планов тренировок для программы ID: " + programId);

        try {

            ProgramRoomDatabase db = ProgramRoomDatabase.getInstance(context);
            if (db == null) {
                Log.e(TAG, "getWorkoutPlansByProgramId: Ошибка доступа к базе данных Room");
                return new ArrayList<>();
            }

            List<WorkoutPlanEntity> entities = db.workoutPlanDao().getPlansByProgramId(programId);
            if (entities == null) {
                Log.e(TAG, "getWorkoutPlansByProgramId: Получен null из базы данных");
                return new ArrayList<>();
            }


            List<WorkoutPlan> plans = new ArrayList<>();
            for (WorkoutPlanEntity entity : entities) {
                plans.add(ProgramRoomCache.mapEntityToWorkoutPlan(entity));
            }

            Log.d(TAG, "getWorkoutPlansByProgramId: Загружено " + plans.size() + " планов для программы");
            return plans;

        } catch (Exception e) {
            Log.e(TAG, "getWorkoutPlansByProgramId: Ошибка при получении планов", e);
            return new ArrayList<>();
        }
    }


    public void fetchAndCacheFullProgramAsync(String programId, AsyncCallback<Void> callback) {
        if (programId == null || programId.isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(new IllegalArgumentException("programId не может быть пустым")));
            }
            return;
        }

        Log.d(TAG, "Начало загрузки полной программы с сервера для ID: " + programId);


        getFullProgramAsync(programId, new AsyncCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject programJson) {
                try {

                    if (programJson != null) {
                        Log.d(TAG, "JSON программы успешно получен с сервера, размер: " + programJson.length());


                        ProgramRoomCache.saveProgramAsync(programJson, new AsyncCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                Log.d(TAG, "Программа успешно сохранена в Room кэш: " + result);


                                fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        Log.d(TAG, "Планы тренировок успешно загружены и кэшированы");
                                        if (callback != null) {
                                            callback.onSuccess(null);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Ошибка при загрузке планов тренировок: " + e.getMessage(), e);

                                        if (callback != null) {
                                            callback.onSuccess(null);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Ошибка при сохранении программы в Room кэш: " + e.getMessage(), e);
                                if (callback != null) {
                                    callback.onFailure(e);
                                }
                            }
                        });
                    } else {
                        Log.e(TAG, "Получен пустой JSON программы");
                        if (callback != null) {
                            callback.onFailure(new Exception("Получен пустой JSON программы"));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при обработке JSON программы: " + e.getMessage(), e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Ошибка при получении JSON программы: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }
} 