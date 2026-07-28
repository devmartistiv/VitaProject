package com.martist.vitamove.workout.data.repository;

import android.util.Log;

import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.AsyncCallback;
import com.martist.vitamove.programs.data.ProgramManager;
import com.martist.vitamove.programs.data.local.ProgramRoomCache;
import com.martist.vitamove.programs.ui.model.ProgramTemplate;
import com.martist.vitamove.programs.ui.model.ProgramTemplateDay;
import com.martist.vitamove.programs.ui.model.ProgramTemplateExercise;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class SupabaseProgramTemplateRepository implements ProgramTemplateRepository {
    private static final String TAG = "SupabaseProgramTemplateRepo";
    private final SupabaseClient supabaseClient;
    private static final SimpleDateFormat ISO_DATE_FORMAT;

    static {
        ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public SupabaseProgramTemplateRepository(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
    }

    @Override
    public List<ProgramTemplate> getAllPublicTemplates() throws Exception {
        Log.d(TAG, "getAllPublicTemplates: Получение всех публичных шаблонов");

        JSONArray result = supabaseClient.from("program_templates")
                .select("*")
                .eq("is_public", true)
                .executeAndGetArray();

        return parseTemplates(result);
    }

    @Override
    public List<ProgramTemplate> getTemplatesByAuthor(String authorId) throws Exception {
        Log.d(TAG, "getTemplatesByAuthor: Получение шаблонов автора: " + authorId);

        JSONArray result = supabaseClient.from("program_templates")
                .select("*")
                .eq("author_id", authorId)
                .executeAndGetArray();

        return parseTemplates(result);
    }

    @Override
    public ProgramTemplate getTemplateById(String templateId) throws Exception {
        Log.d(TAG, "getTemplateById: Получение шаблона по ID: " + templateId);

        JSONObject result = supabaseClient.from("program_templates")
                .select("*")
                .eq("id", templateId)
                .executeAndGetSingle();

        return parseTemplate(result);
    }

    @Override
    public String createTemplate(ProgramTemplate template) throws Exception {
        Log.d(TAG, "createTemplate: Создание нового шаблона: " + template.getName());

        JSONObject templateJson = new JSONObject();


        templateJson.put("name", template.getName());
        templateJson.put("duration_weeks", template.getDurationWeeks());
        templateJson.put("days_per_week", template.getDaysPerWeek());


        if (template.getDescription() != null && !template.getDescription().isEmpty()) {
            templateJson.put("description", template.getDescription());
        }
        if (template.getAuthorId() != null && !template.getAuthorId().isEmpty()) {
            templateJson.put("author_id", template.getAuthorId());
        }
        templateJson.put("is_public", template.isPublic());


        if (template.getDifficulty() != null && !template.getDifficulty().isEmpty()) {
            templateJson.put("difficulty", template.getDifficulty());
        }


        Log.d(TAG, "createTemplate JSON: " + templateJson);

        JSONObject result = supabaseClient.from("program_templates")
                .insert(templateJson)
                .executeAndGetSingle();

        return result.getString("id");
    }

    @Override
    public void updateTemplate(ProgramTemplate template) throws Exception {
        Log.d(TAG, "updateTemplate: Обновление шаблона: " + template.getId());

        JSONObject templateJson = new JSONObject();
        templateJson.put("name", template.getName());
        templateJson.put("description", template.getDescription());
        templateJson.put("is_public", template.isPublic());
        templateJson.put("category", template.getCategory());
        templateJson.put("duration_weeks", template.getDurationWeeks());
        templateJson.put("days_per_week", template.getDaysPerWeek());
        templateJson.put("difficulty", template.getDifficulty());
        templateJson.put("image_url", template.getImageUrl());
        templateJson.put("likes", template.getLikes());
        templateJson.put("updated_at", ISO_DATE_FORMAT.format(template.getUpdatedAt()));

        supabaseClient.from("program_templates")
                .update(templateJson)
                .eq("id", template.getId())
                .execute();
    }

    @Override
    public void deleteTemplate(String templateId) throws Exception {
        Log.d(TAG, "deleteTemplate: Удаление шаблона: " + templateId);


        supabaseClient.from("program_template_exercises")
                .delete()
                .eq("template_id", templateId)
                .execute();


        supabaseClient.from("program_template_days")
                .delete()
                .eq("template_id", templateId)
                .execute();


        supabaseClient.from("program_templates")
                .delete()
                .eq("id", templateId)
                .execute();
    }

    @Override
    public List<ProgramTemplateDay> getTemplateDays(String templateId) throws Exception {
        Log.d(TAG, "getTemplateDays: Получение дней шаблона: " + templateId);

        JSONArray result = supabaseClient.from("program_template_days")
                .select("*")
                .eq("template_id", templateId)
                .order("week_number", true)
                .order("day_number", true)
                .executeAndGetArray();

        return parseTemplateDays(result);
    }

    @Override
    public ProgramTemplateDay getTemplateDayById(String dayId) throws Exception {
        Log.d(TAG, "getTemplateDayById: Получение дня шаблона по ID: " + dayId);

        JSONObject result = supabaseClient.from("program_template_days")
                .select("*")
                .eq("id", dayId)
                .executeAndGetSingle();

        return parseTemplateDay(result);
    }

    @Override
    public String createTemplateDay(ProgramTemplateDay day) throws Exception {
        Log.d(TAG, "createTemplateDay: Создание нового дня шаблона");

        JSONObject dayJson = new JSONObject();
        dayJson.put("template_id", day.getTemplateId());
        dayJson.put("name", day.getName());
        dayJson.put("description", day.getDescription());
        dayJson.put("day_number", day.getDayNumber());

        JSONObject result = supabaseClient.from("program_template_days")
                .insert(dayJson)
                .executeAndGetSingle();

        return result.getString("id");
    }

    @Override
    public void updateTemplateDay(ProgramTemplateDay day) throws Exception {
        Log.d(TAG, "updateTemplateDay: Обновление дня шаблона: " + day.getId());

        JSONObject dayJson = new JSONObject();
        dayJson.put("name", day.getName());
        dayJson.put("description", day.getDescription());
        dayJson.put("day_number", day.getDayNumber());
        dayJson.put("updated_at", ISO_DATE_FORMAT.format(day.getUpdatedAt()));

        supabaseClient.from("program_template_days")
                .update(dayJson)
                .eq("id", day.getId())
                .execute();
    }

    @Override
    public void deleteTemplateDay(String dayId) throws Exception {
        Log.d(TAG, "deleteTemplateDay: Удаление дня шаблона: " + dayId);


        supabaseClient.from("program_template_exercises")
                .delete()
                .eq("template_day_id", dayId)
                .execute();


        supabaseClient.from("program_template_days")
                .delete()
                .eq("id", dayId)
                .execute();
    }

    @Override
    public List<ProgramTemplateExercise> getTemplateDayExercises(String dayId) throws Exception {
        Log.d(TAG, "getTemplateDayExercises: Получение упражнений дня шаблона: " + dayId);


        JSONArray result = supabaseClient.from("program_template_exercises")
                .select("*")
                .eq("template_day_id", dayId)
                .order("order_index", true)
                .executeAndGetArray();


        if (result == null || result.length() == 0) {
            Log.d(TAG, "getTemplateDayExercises: Запрос с template_day_id не дал результатов, пробуем с day_id");
            result = supabaseClient.from("program_template_exercises")
                    .select("*")
                    .eq("day_id", dayId)
                    .order("order_number", true)
                    .executeAndGetArray();
        }

        return parseTemplateExercises(result);
    }

    @Override
    public ProgramTemplateExercise getTemplateExerciseById(String exerciseId) throws Exception {
        Log.d(TAG, "getTemplateExerciseById: Получение упражнения шаблона по ID: " + exerciseId);

        JSONObject result = supabaseClient.from("program_template_exercises")
                .select("*")
                .eq("id", exerciseId)
                .executeAndGetSingle();

        return parseTemplateExercise(result);
    }

    @Override
    public String createTemplateExercise(ProgramTemplateExercise exercise) throws Exception {
        Log.d(TAG, "createTemplateExercise: Создание нового упражнения шаблона для day_id: " + exercise.getTemplateDayId());

        JSONObject exerciseJson = new JSONObject();


        exerciseJson.put("day_id", exercise.getTemplateDayId());
        exerciseJson.put("exercise_id", exercise.getExerciseId());
        exerciseJson.put("order_number", exercise.getOrderIndex());
        exerciseJson.put("target_sets", exercise.getSets());
        exerciseJson.put("target_reps", exercise.getRepsRange());
        exerciseJson.put("target_weight", exercise.getWeightRange());
        exerciseJson.put("rest_seconds", parseRestTimeToSeconds(exercise.getRestTime()));
        exerciseJson.put("notes", exercise.getNotes());


        Log.d(TAG, "createTemplateExercise JSON: " + exerciseJson);

        JSONObject result = supabaseClient.from("program_template_exercises")
                .insert(exerciseJson)
                .executeAndGetSingle();

        return result.getString("id");
    }

    @Override
    public void updateTemplateExercise(ProgramTemplateExercise exercise) throws Exception {
        Log.d(TAG, "updateTemplateExercise: Обновление упражнения шаблона: " + exercise.getId());

        JSONObject exerciseJson = new JSONObject();

        exerciseJson.put("order_number", exercise.getOrderIndex() + 1);
        exerciseJson.put("target_sets", exercise.getSets());
        exerciseJson.put("target_reps", exercise.getRepsRange());
        exerciseJson.put("target_weight", exercise.getWeightRange());
        exerciseJson.put("rest_seconds", parseRestTimeToSeconds(exercise.getRestTime()));
        exerciseJson.put("notes", exercise.getNotes());

        exerciseJson.put("updated_at", ISO_DATE_FORMAT.format(exercise.getUpdatedAt()));

        supabaseClient.from("program_template_exercises")
                .update(exerciseJson)
                .eq("id", exercise.getId())
                .execute();
    }

    @Override
    public void deleteTemplateExercise(String exerciseId) throws Exception {
        Log.d(TAG, "deleteTemplateExercise: Удаление упражнения шаблона: " + exerciseId);

        supabaseClient.from("program_template_exercises")
                .delete()
                .eq("id", exerciseId)
                .execute();
    }

    @Override
    public String createProgramFromTemplate(String templateId, String userId, String name) throws Exception {
        Log.d(TAG, "createProgramFromTemplate: Создание программы из шаблона: " + templateId);


        JSONObject result = supabaseClient.rpc("create_program_from_template")
                .param("p_template_id", templateId)
                .param("p_user_id", userId)
                .param("p_program_name", name)
                .executeAndGetSingle();

        String programId = result.getString("program_id");
        Log.i("VITAMOVE_PLAN_CACHE", "createProgramFromTemplate: RPC выполнено, получен programId: " + programId);


        Log.d("VITAMOVE_PLAN_CACHE", "createProgramFromTemplate: Вызов cacheFullProgramAsync для programId: " + programId);
        cacheFullProgramAsync(programId);

        return programId;
    }


    public String createProgramFromTemplate(String templateId, String userId, String name,
                                            List<Integer> workoutDays, Date startDate) throws Exception {
        Log.d(TAG, "createProgramFromTemplate: Создание программы из шаблона с выбором дней недели");


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);


        JSONArray daysArray = new JSONArray();
        for (Integer day : workoutDays) {
            daysArray.put(day);
        }

        JSONObject result = supabaseClient.rpc("create_program_from_template")
                .param("p_template_id", templateId)
                .param("p_user_id", userId)
                .param("p_program_name", name)
                .param("p_workout_days", daysArray)
                .param("p_start_date", dateFormat.format(startDate))
                .executeAndGetSingle();

        String programId = result.getString("program_id");
        Log.d(TAG, "createProgramFromTemplate: Программа успешно создана с планами, ID: " + programId);


        cacheFullProgramAsync(programId);

        return programId;
    }


    private void cacheFullProgramAsync(String programId) {
        Log.d("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: Начало для programId: " + programId);
        ProgramManager programManager = new ProgramManager(VitaMoveApplication.getContext());
        programManager.getFullProgramAsync(programId, new AsyncCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject fullProgramJson) {
                Log.d("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: getFullProgramAsync onSuccess для programId: " + programId);
                if (fullProgramJson != null) {
                    try {

                        ProgramRoomCache.saveProgram(programId, fullProgramJson);
                        Log.i("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: Основной JSON сохранен в Room (ProgramRoomCache) для programId: " + programId);


                        Log.d("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: Вызов programManager.fetchAndCacheWorkoutPlansAsync для programId: " + programId);
                        programManager.fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.i("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: fetchAndCacheWorkoutPlansAsync onSuccess (инициировано) для programId: " + programId);
                            }

                            @Override
                            public void onFailure(Exception error) {
                                Log.e("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: fetchAndCacheWorkoutPlansAsync onFailure для programId: " + programId, error);


                                Log.e(TAG, "Ошибка при загрузке/кэшировании планов для программы ID: " + programId, error);
                            }
                        });

                    } catch (Exception e) {
                        Log.e("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: Ошибка при сохранении основного JSON или вызове кэширования планов", e);
                        Log.e(TAG, "Ошибка сохранения полной программы в кэш Room для ID: " + programId, e);

                    }
                } else {
                    Log.w("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: Получен null JSON от getFullProgramAsync для programId: " + programId);
                }
            }

            @Override
            public void onFailure(Exception error) {
                Log.e("VITAMOVE_PLAN_CACHE", "cacheFullProgramAsync: getFullProgramAsync onFailure для programId: " + programId, error);
            }
        });
    }


    private Integer parseRestTimeToSeconds(String restTime) {
        try {
            if (restTime == null || restTime.isEmpty()) {
                return 60;
            }


            String cleanedTime = restTime.replaceAll("[^0-9,.]", "");


            cleanedTime = cleanedTime.replace(',', '.');


            float timeValue = Float.parseFloat(cleanedTime);


            if (timeValue < 10 && restTime.toLowerCase().contains("мин")) {
                return Math.round(timeValue * 60);
            }


            return Math.round(timeValue);
        } catch (Exception e) {
            Log.e(TAG, "parseRestTimeToSeconds: Ошибка парсинга времени отдыха: " + restTime, e);
            return 60;
        }
    }


    private List<ProgramTemplate> parseTemplates(JSONArray jsonArray) throws Exception {
        List<ProgramTemplate> templates = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            templates.add(parseTemplate(jsonArray.getJSONObject(i)));
        }
        return templates;
    }

    private ProgramTemplate parseTemplate(JSONObject json) throws Exception {
        ProgramTemplate template = new ProgramTemplate();
        template.setId(json.getString("id"));
        template.setName(json.getString("name"));
        template.setDescription(json.getString("description"));
        template.setAuthorId(json.getString("author_id"));
        template.setPublic(json.getBoolean("is_public"));
        template.setCategory(json.getString("category"));
        template.setDurationWeeks(json.getInt("duration_weeks"));
        template.setDaysPerWeek(json.getInt("days_per_week"));
        template.setDifficulty(json.getString("difficulty"));


        if (json.has("image_url") && !json.isNull("image_url")) {
            template.setImageUrl(json.getString("image_url"));
        } else {
            template.setImageUrl("");
        }

        if (json.has("likes") && !json.isNull("likes")) {
            template.setLikes(json.getInt("likes"));
        } else {
            template.setLikes(0);
        }

        template.setCreatedAt(ISO_DATE_FORMAT.parse(json.getString("created_at")));
        template.setUpdatedAt(ISO_DATE_FORMAT.parse(json.getString("updated_at")));
        return template;
    }

    private List<ProgramTemplateDay> parseTemplateDays(JSONArray jsonArray) throws Exception {
        List<ProgramTemplateDay> days = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            days.add(parseTemplateDay(jsonArray.getJSONObject(i)));
        }
        return days;
    }

    private ProgramTemplateDay parseTemplateDay(JSONObject json) throws Exception {
        ProgramTemplateDay day = new ProgramTemplateDay();
        day.setId(json.getString("id"));
        day.setTemplateId(json.getString("template_id"));
        day.setName(json.getString("name"));
        day.setDescription(json.getString("description"));
        day.setDayNumber(json.getInt("day_number"));


        day.setWeekNumber(json.optInt("week_number", 1));
        day.setMuscleGroups(json.optString("muscle_groups", ""));
        day.setFocusArea(json.optString("focus_area", ""));
        day.setEstimatedDuration(json.optInt("estimated_duration", 60));

        day.setCreatedAt(ISO_DATE_FORMAT.parse(json.getString("created_at")));
        day.setUpdatedAt(ISO_DATE_FORMAT.parse(json.getString("updated_at")));
        return day;
    }

    private List<ProgramTemplateExercise> parseTemplateExercises(JSONArray jsonArray) throws Exception {
        List<ProgramTemplateExercise> exercises = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            exercises.add(parseTemplateExercise(jsonArray.getJSONObject(i)));
        }
        return exercises;
    }

    private ProgramTemplateExercise parseTemplateExercise(JSONObject json) throws Exception {
        ProgramTemplateExercise exercise = new ProgramTemplateExercise();
        exercise.setId(json.getString("id"));


        exercise.setTemplateDayId(json.getString("day_id"));
        exercise.setExerciseId(json.getString("exercise_id"));


        exercise.setExerciseName("Упражнение " + json.getString("exercise_id"));

        exercise.setNotes(json.optString("notes"));
        exercise.setOrderIndex(json.getInt("order_number") - 1);
        exercise.setSets(json.getInt("target_sets"));
        exercise.setRepsRange(json.getString("target_reps"));
        exercise.setWeightRange(json.optString("target_weight"));


        if (json.has("rest_seconds") && !json.isNull("rest_seconds")) {
            int seconds = json.getInt("rest_seconds");
            exercise.setRestTime(seconds + " сек");
        } else {
            exercise.setRestTime("60 сек");
        }


        exercise.setSuperset(false);
        exercise.setSupersetGroupId(null);
        exercise.setSupersetOrder(0);

        if (json.has("created_at") && !json.isNull("created_at")) {
            exercise.setCreatedAt(ISO_DATE_FORMAT.parse(json.getString("created_at")));
        }

        if (json.has("updated_at") && !json.isNull("updated_at")) {
            exercise.setUpdatedAt(ISO_DATE_FORMAT.parse(json.getString("updated_at")));
        }

        return exercise;
    }
} 