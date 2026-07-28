package com.martist.vitamove.report;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.local.MealsDatabase;
import com.martist.vitamove.core.data.local.entities.DayMeal;
import com.martist.vitamove.core.data.local.entities.MealDao;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.exercise.data.local.dao.ExerciseDao;
import com.martist.vitamove.exercise.data.local.entities.ExerciseEntity;
import com.martist.vitamove.gigachat.GigaChatService;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Meal;
import com.martist.vitamove.set.ExerciseSetEntity;
import com.martist.vitamove.workout.data.dao.WorkoutDao;
import com.martist.vitamove.workout.data.entities.UserWorkoutEntity;
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WeeklyReportWorker extends Worker {
    private static final String TAG = "WeeklyReportWorker";
    private static final String PREFS_NAME = "AssistantFragmentPrefs";
    private static final String KEY_REPORT_HISTORY = "report_history";
    private final Gson gson = new Gson();

    public WeeklyReportWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Начинаем генерацию недельного отчета");

        try {
            GigaChatService gigaChatService = GigaChatService.getInstance(
                    Constants.GIGACHAT_CLIENT_ID,
                    Constants.GIGACHAT_CLIENT_SECRET
            );

            String stats = collectUserStatsForReport();
            String prompt = "Сформируй краткий недельный отчет по пользователю в формате: итоги, ключевые метрики, проблемы, рекомендации на неделю. " +
                    "Отвечай на русском и используй маркдаун для структурирования списка рекомендаций. Исходные данные:\n" + stats;

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};
            final String[] reportContent = {null};

            gigaChatService.sendMessage(prompt, new GigaChatService.ChatCallback() {
                @Override
                public void onResponse(String response) {
                    reportContent[0] = response;
                    success[0] = true;
                    latch.countDown();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Ошибка при генерации отчета: " + error);
                    latch.countDown();
                }
            });


            boolean completed = latch.await(60, TimeUnit.SECONDS);

            if (completed && success[0] && reportContent[0] != null) {
                saveReportToHistory(reportContent[0]);
                Log.d(TAG, "Недельный отчет успешно сгенерирован и сохранен");
                return Result.success();
            } else {
                Log.e(TAG, "Не удалось получить отчет в течение таймаута");
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при генерации отчета", e);
            return Result.failure();
        }
    }

    private void saveReportToHistory(String content) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_REPORT_HISTORY, null);

        List<ReportSummary> reportHistory = new ArrayList<>();
        if (json != null) {
            Type type = new TypeToken<List<ReportSummary>>() {
            }.getType();
            List<ReportSummary> restored = gson.fromJson(json, type);
            if (restored != null) {
                reportHistory.addAll(restored);
            }
        }

        Date now = new Date();
        String title = "Отчет за неделю";
        String subtitle = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(now);
        ReportSummary report = new ReportSummary(
                UUID.randomUUID().toString(),
                title,
                subtitle,
                content,
                now.getTime()
        );

        reportHistory.add(0, report);

        String updatedJson = gson.toJson(reportHistory);
        prefs.edit().putString(KEY_REPORT_HISTORY, updatedJson).apply();
    }

    private String collectUserStatsForReport() {
        Context context = getApplicationContext();
        SharedPreferences userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        SharedPreferences userPrefs2 = context.getSharedPreferences("VitaMovePrefs", Context.MODE_PRIVATE);
        String userId = userPrefs2.getString("userId", "default_user");
        String name = userPrefs.getString("name", "пользователь");
        String fitnessGoal = userPrefs.getString("fitness_goal", "не указана");
        int targetCalories = userPrefs.getInt("target_calories", 0);
        float targetWater = userPrefs.getFloat("target_water", 0f);

        StringBuilder builder = new StringBuilder();
        builder.append("Имя: ").append(name).append("\n");
        builder.append("ID пользователя: ").append(userId != null ? userId : "не найден").append("\n");
        builder.append("Цель: ").append(fitnessGoal).append("\n");
        builder.append("Целевые калории: ").append(targetCalories > 0 ? targetCalories + " ккал" : "не указаны").append("\n");
        builder.append("Целевой объем воды: ").append(targetWater > 0 ? targetWater + " л" : "не указан").append("\n\n");

        builder.append("Данные за последние 7 дней:\n");
        builder.append(buildWeeklyWorkoutsSection(userId));
        builder.append("\n");
        builder.append(buildWeeklyMealsSection(userId));
        builder.append("\n");
        builder.append("Если каких-то данных нет, явно укажи, чего не хватает.\n");
        return builder.toString();
    }

    private String buildWeeklyWorkoutsSection(String userId) {
        StringBuilder builder = new StringBuilder();
        builder.append("Тренировки:\n");
        if (userId == null) {
            builder.append("- user_id не найден, тренировки недоступны\n");
            return builder.toString();
        }

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        WorkoutDao workoutDao = db.workoutDao();
        ExerciseDao exerciseDao = db.exerciseDao();

        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        long startTime = calendar.getTimeInMillis();

        List<UserWorkoutEntity> workouts = workoutDao.getAllWorkoutsByTimeRange(userId, startTime, now);
        if (workouts == null || workouts.isEmpty()) {
            builder.append("- нет тренировок за последние 7 дней\n");
        } else {
            Set<String> exerciseIds = new HashSet<>();
            for (UserWorkoutEntity workout : workouts) {
                List<WorkoutExerciseEntity> exercises = workoutDao.getExercisesForWorkout(workout.getId());
                for (WorkoutExerciseEntity exercise : exercises) {
                    exerciseIds.add(exercise.getBaseExerciseId());
                }
            }

            Map<String, String> exerciseNames = new HashMap<>();
            if (!exerciseIds.isEmpty()) {
                List<ExerciseEntity> entities = exerciseDao.getExercisesByIds(new ArrayList<>(exerciseIds));
                if (entities != null) {
                    for (ExerciseEntity entity : entities) {
                        exerciseNames.put(entity.getId(), entity.getName());
                    }
                }
            }

            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            for (UserWorkoutEntity workout : workouts) {
                builder.append("- Тренировка: ").append(workout.getName() != null ? workout.getName() : "Без названия").append("\n");
                builder.append("  Старт: ").append(dateTimeFormat.format(new Date(workout.getStartTime()))).append("\n");
                if (workout.getEndTime() != null) {
                    builder.append("  Финиш: ").append(dateTimeFormat.format(new Date(workout.getEndTime()))).append("\n");
                } else {
                    builder.append("  Финиш: не завершена\n");
                }
                builder.append("  Калории тренировки: ").append(workout.getTotalCalories()).append(" ккал\n");
                if (workout.getNotes() != null && !workout.getNotes().trim().isEmpty()) {
                    builder.append("  Заметки: ").append(workout.getNotes().trim()).append("\n");
                }

                List<WorkoutExerciseEntity> exercises = workoutDao.getExercisesForWorkout(workout.getId());
                if (exercises == null || exercises.isEmpty()) {
                    builder.append("  Упражнения: нет данных\n");
                    continue;
                }
                for (WorkoutExerciseEntity exercise : exercises) {
                    String exerciseName = exerciseNames.get(exercise.getBaseExerciseId());
                    builder.append("  Упражнение: ").append(exerciseName != null ? exerciseName : exercise.getBaseExerciseId()).append("\n");
                    if (exercise.getNotes() != null && !exercise.getNotes().trim().isEmpty()) {
                        builder.append("    Заметки: ").append(exercise.getNotes().trim()).append("\n");
                    }

                    List<ExerciseSetEntity> sets = workoutDao.getSetsForExercise(exercise.getId());
                    if (sets == null || sets.isEmpty()) {
                        builder.append("    Подходы: нет данных\n");
                        continue;
                    }
                    for (ExerciseSetEntity set : sets) {
                        builder.append("    Подход ").append(set.getSetNumber()).append(": ");
                        if (set.getReps() != null) {
                            builder.append(set.getReps()).append(" повторений");
                        } else {
                            builder.append("повторы не указаны");
                        }
                        if (set.getWeight() != null) {
                            builder.append(", ").append(set.getWeight()).append(" кг");
                        }
                        if (set.getDurationSeconds() != null) {
                            builder.append(", ").append(set.getDurationSeconds()).append(" сек");
                        }
                        builder.append(set.isCompleted() ? " (выполнен)" : " (не выполнен)");
                        builder.append("\n");
                    }
                }
            }
        }
        return builder.toString();
    }

    private String buildWeeklyMealsSection(String userId) {
        StringBuilder builder = new StringBuilder();
        builder.append("Питание:\n");
        if (userId == null) {
            builder.append("- user_id не найден, питание недоступно\n");
            return builder.toString();
        }

        MealsDatabase mealsDatabase = MealsDatabase.getInstance(getApplicationContext());
        MealDao mealDao = mealsDatabase.mealDao();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        boolean hasMeals = false;
        for (int i = 0; i < 7; i++) {
            String dateKey = dateFormat.format(calendar.getTime());
            List<DayMeal> dayMeals = mealDao.getMealsForDate(dateKey, userId);
            if (dayMeals != null && !dayMeals.isEmpty()) {
                hasMeals = true;
                builder.append("- Дата: ").append(displayFormat.format(calendar.getTime())).append("\n");
                for (DayMeal dayMeal : dayMeals) {
                    Meal meal = null;
                    try {
                        meal = gson.fromJson(dayMeal.mealData, Meal.class);
                    } catch (Exception e) {
                        meal = null;
                    }
                    String mealTitle = meal != null && meal.getTitle() != null ? meal.getTitle()
                            : (dayMeal.mealType != null ? dayMeal.mealType : "прием пищи");
                    builder.append("  Прием пищи: ").append(mealTitle).append("\n");
                    if (meal == null || meal.getFoods() == null || meal.getFoods().isEmpty()) {
                        builder.append("    Нет данных о продуктах\n");
                        continue;
                    }
                    builder.append("    Калории: ").append(String.format(Locale.getDefault(), "%.1f", meal.getCalories())).append(" ккал\n");
                    builder.append("    Белки: ").append(String.format(Locale.getDefault(), "%.1f", meal.getTotalProteins())).append(" г\n");
                    builder.append("    Жиры: ").append(String.format(Locale.getDefault(), "%.1f", meal.getTotalFats())).append(" г\n");
                    builder.append("    Углеводы: ").append(String.format(Locale.getDefault(), "%.1f", meal.getTotalCarbs())).append(" г\n");
                    for (Meal.FoodPortion portion : meal.getFoods()) {
                        Food food = portion.getFood();
                        String foodName = food != null ? food.getName() : "продукт";
                        builder.append("    - ").append(foodName)
                                .append(": ").append(portion.getQuantity()).append(" ")
                                .append(portion.getPortionName() != null ? portion.getPortionName() : "г")
                                .append(" (").append(portion.getTotalWeightInGrams()).append(" г)")
                                .append("\n");
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        if (!hasMeals) {
            builder.append("- нет данных о питании за последние 7 дней\n");
        }
        return builder.toString();
    }
}
