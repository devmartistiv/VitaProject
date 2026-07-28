package com.martist.vitamove.programs.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.programs.ui.model.ProgramTemplate;
import com.martist.vitamove.programs.ui.model.ProgramTemplateDay;
import com.martist.vitamove.programs.ui.model.ProgramTemplateExercise;
import com.martist.vitamove.workout.data.repository.ProgramTemplateRepository;
import com.martist.vitamove.workout.data.repository.SupabaseProgramTemplateRepository;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ProgramTemplateManager {
    private static final String TAG = "ProgramTemplateManager";
    private final ProgramTemplateRepository repository;
    private final Context context;
    private final Executor executor;
    private final Handler mainHandler;

    public ProgramTemplateManager(Context context) {
        this.context = context;
        this.repository = new SupabaseProgramTemplateRepository(
                SupabaseClient.getInstance(
                        Constants.SUPABASE_CLIENT_ID,
                        Constants.SUPABASE_CLIENT_SECRET
                )
        );
        this.executor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }


    public interface AsyncCallback<T> {
        void onSuccess(T result);

        void onFailure(Exception e);
    }


    public ProgramTemplate createTemplate(
            String authorId,
            String authorName,
            String name,
            String description,
            String category,
            int durationWeeks,
            int daysPerWeek,
            String difficulty,
            boolean isPublic) throws Exception {

        Log.d(TAG, "createTemplate: Создание шаблона программы: " + name);

        ProgramTemplate template = new ProgramTemplate();
        template.setAuthorId(authorId);
        template.setAuthorName(authorName);
        template.setName(name);
        template.setDescription(description);
        template.setCategory(category);
        template.setDurationWeeks(durationWeeks);
        template.setDaysPerWeek(daysPerWeek);
        template.setDifficulty(difficulty);
        template.setPublic(isPublic);
        template.setLikes(0);
        template.setCreatedAt(new Date());
        template.setUpdatedAt(new Date());

        String templateId = repository.createTemplate(template);
        template.setId(templateId);

        return template;
    }


    public void createTemplateAsync(
            String authorId,
            String authorName,
            String name,
            String description,
            String category,
            int durationWeeks,
            int daysPerWeek,
            String difficulty,
            boolean isPublic,
            AsyncCallback<ProgramTemplate> callback) {

        executor.execute(() -> {
            try {
                ProgramTemplate template = createTemplate(
                        authorId, authorName, name, description, category,
                        durationWeeks, daysPerWeek, difficulty, isPublic
                );
                mainHandler.post(() -> callback.onSuccess(template));
            } catch (Exception e) {
                Log.e(TAG, "createTemplateAsync: Ошибка", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public ProgramTemplateDay addTemplateDay(
            String templateId,
            String name,
            String description,
            int dayNumber,
            int weekNumber,
            String muscleGroups,
            String focusArea,
            int estimatedDuration) throws Exception {

        Log.d(TAG, "addTemplateDay: Добавление дня в шаблон: " + templateId);

        ProgramTemplateDay day = new ProgramTemplateDay();
        day.setTemplateId(templateId);
        day.setName(name);
        day.setDescription(description);
        day.setDayNumber(dayNumber);
        day.setWeekNumber(weekNumber);
        day.setMuscleGroups(muscleGroups);
        day.setFocusArea(focusArea);
        day.setEstimatedDuration(estimatedDuration);
        day.setCreatedAt(new Date());
        day.setUpdatedAt(new Date());

        String dayId = repository.createTemplateDay(day);
        day.setId(dayId);

        return day;
    }


    public void addTemplateDayAsync(
            String templateId,
            String name,
            String description,
            int dayNumber,
            int weekNumber,
            String muscleGroups,
            String focusArea,
            int estimatedDuration,
            AsyncCallback<ProgramTemplateDay> callback) {

        executor.execute(() -> {
            try {
                ProgramTemplateDay day = addTemplateDay(
                        templateId, name, description, dayNumber, weekNumber,
                        muscleGroups, focusArea, estimatedDuration
                );
                mainHandler.post(() -> callback.onSuccess(day));
            } catch (Exception e) {
                Log.e(TAG, "addTemplateDayAsync: Ошибка", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


    public ProgramTemplateExercise addTemplateExercise(
            String dayId,
            String exerciseId,
            String exerciseName,
            int orderIndex,
            int sets,
            String repsRange,
            String weightRange,
            String restTime,
            String notes) throws Exception {

        Log.d(TAG, "addTemplateExercise: Добавление упражнения в день: " + dayId);

        ProgramTemplateExercise exercise = new ProgramTemplateExercise();
        exercise.setTemplateDayId(dayId);
        exercise.setExerciseId(exerciseId);
        exercise.setExerciseName(exerciseName);
        exercise.setOrderIndex(orderIndex);
        exercise.setSets(sets);
        exercise.setRepsRange(repsRange);
        exercise.setWeightRange(weightRange);
        exercise.setRestTime(restTime);
        exercise.setNotes(notes);
        exercise.setSuperset(false);
        exercise.setCreatedAt(new Date());
        exercise.setUpdatedAt(new Date());

        String exerciseId2 = repository.createTemplateExercise(exercise);
        exercise.setId(exerciseId2);

        return exercise;
    }


    public void addTemplateExerciseAsync(
            String dayId,
            String exerciseId,
            String exerciseName,
            int orderIndex,
            int sets,
            String repsRange,
            String weightRange,
            String restTime,
            String notes,
            AsyncCallback<ProgramTemplateExercise> callback) {

        executor.execute(() -> {
            try {
                ProgramTemplateExercise exercise = addTemplateExercise(
                        dayId, exerciseId, exerciseName, orderIndex, sets,
                        repsRange, weightRange, restTime, notes
                );
                mainHandler.post(() -> callback.onSuccess(exercise));
            } catch (Exception e) {
                Log.e(TAG, "addTemplateExerciseAsync: Ошибка", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }


}