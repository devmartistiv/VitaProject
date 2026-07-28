package com.martist.vitamove.assistant;

import static com.martist.vitamove.VitaMoveApplication.context;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.martist.vitamove.R;
import com.martist.vitamove.analytics.ReportDetailActivity;
import com.martist.vitamove.analytics.ReportHistoryAdapter;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.local.MealsDatabase;
import com.martist.vitamove.core.data.local.entities.DayMeal;
import com.martist.vitamove.core.data.local.entities.MealDao;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.data.services.SupabaseService;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.exercise.data.local.dao.ExerciseDao;
import com.martist.vitamove.exercise.data.local.entities.ExerciseEntity;
import com.martist.vitamove.gigachat.GigaChatService;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Meal;
import com.martist.vitamove.report.ReportSummary;
import com.martist.vitamove.report.WeeklyReportWorker;
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
import java.util.concurrent.TimeUnit;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;

public class AssistantFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private AppCompatImageButton sendButton;
    private MaterialButton menuButton;
    private ChatAdapter chatAdapter;
    private GigaChatService gigaChatService;
    private SupabaseService supabaseService;
    private MaterialButton resetButton;
    private DrawerLayout drawerLayout;
    private RecyclerView reportsRecyclerView;
    private ReportHistoryAdapter reportHistoryAdapter;
    private boolean isProcessingMessage = false;
    private static final String TAG = "AssistantFragment";


    private static final String PREFS_NAME = "AssistantFragmentPrefs";
    private static final String KEY_CHAT_HISTORY = "chat_history";
    private static final String KEY_REPORT_HISTORY = "report_history";
    private final Gson gson = new Gson();
    private final List<ReportSummary> reportHistory = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGigaChatService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusbar_color));


            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }
        initViews(view);
        setupRecyclerView();
        setupReportsDrawer();
        setupClickListeners();


        SupabaseClient supabaseClient = SupabaseClient.getInstance(Constants.SUPABASE_CLIENT_ID, Constants.SUPABASE_CLIENT_SECRET);
        supabaseService = new SupabaseService(supabaseClient);


        restoreChatHistory();
        restoreReportsHistory();


        scheduleWeeklyReports();


        if (chatAdapter.getItemCount() == 0) {
            addAssistantMessage("Здравствуйте! Я ваш фитнес-ассистент в VitaMove. Готов помочь вам с вопросами о тренировках, питании и здоровом образе жизни. Что вас интересует?");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (gigaChatService != null) {
            gigaChatService.updateUserContext();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        saveChatHistory();
        saveReportHistory();


        if (messageInput != null && messageInput.hasFocus()) {
            messageInput.clearFocus();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();


        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).ensureBottomNavigationVisible();
        }
    }

    private void initViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        resetButton = view.findViewById(R.id.resetButton);
        menuButton = view.findViewById(R.id.menuButton);
        drawerLayout = view.findViewById(R.id.assistantDrawerLayout);
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView);
    }

    private void initGigaChatService() {
        gigaChatService = GigaChatService.getInstance(
                Constants.GIGACHAT_CLIENT_ID,
                Constants.GIGACHAT_CLIENT_SECRET
        );


        gigaChatService.initializeUserContext();

    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        chatRecyclerView.setLayoutManager(layoutManager);


        Markwon markwon = Markwon.builder(requireContext())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .build();

        chatAdapter = new ChatAdapter(markwon);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupReportsDrawer() {
        if (reportsRecyclerView == null) return;
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reportHistoryAdapter = new ReportHistoryAdapter(this::openReport);
        reportsRecyclerView.setAdapter(reportHistoryAdapter);
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        resetButton.setOnClickListener(v -> resetConversation());
        menuButton.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                sendMessage();
                return true;
            }
            return false;
        });


        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {

                chatRecyclerView.postDelayed(() -> {
                    if (chatAdapter.getItemCount() > 0) {
                        chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }, 200);
            }
        });


        messageInput.setOnClickListener(v -> {

            chatRecyclerView.postDelayed(() -> {
                if (chatAdapter.getItemCount() > 0) {
                    chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                }
            }, 100);
        });
    }


    private void saveChatHistory() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();


        List<ChatMessage> messages = chatAdapter.getMessages();


        String json = gson.toJson(messages);


        editor.putString(KEY_CHAT_HISTORY, json);
        editor.apply();

        Log.d(TAG, "История сообщений сохранена: " + messages.size() + " сообщений");
    }


    private void restoreChatHistory() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CHAT_HISTORY, null);

        if (json != null) {

            Type type = new TypeToken<List<ChatMessage>>() {
            }.getType();
            List<ChatMessage> messages = gson.fromJson(json, type);


            if (messages != null && !messages.isEmpty()) {
                for (ChatMessage message : messages) {
                    chatAdapter.addMessage(message);
                }


                chatRecyclerView.post(() -> chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1));

                Log.d(TAG, "История сообщений восстановлена: " + messages.size() + " сообщений");


                restoreChatHistoryToGigaChat(messages);
            }
        }
    }


    private void restoreChatHistoryToGigaChat(List<ChatMessage> messages) {

        gigaChatService.resetConversation();


        for (ChatMessage message : messages) {
            if (message.isFromUser()) {
                gigaChatService.addMessageToHistory("user", message.getText());
            } else {
                gigaChatService.addMessageToHistory("assistant", message.getText());
            }
        }

        Log.d(TAG, "История сообщений восстановлена в GigaChatService");
    }

    private void saveReportHistory() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(reportHistory);
        editor.putString(KEY_REPORT_HISTORY, json);
        editor.apply();
    }

    private void restoreReportsHistory() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_REPORT_HISTORY, null);
        if (json != null) {
            Type type = new TypeToken<List<ReportSummary>>() {
            }.getType();
            List<ReportSummary> restored = gson.fromJson(json, type);
            if (restored != null && !restored.isEmpty()) {
                reportHistory.clear();
                reportHistory.addAll(restored);
                if (reportHistoryAdapter != null) {
                    reportHistoryAdapter.setReports(reportHistory);
                }
            }
        }
    }

    private void addReportToHistory(ReportSummary report) {
        reportHistory.add(0, report);
        if (reportHistoryAdapter != null) {
            reportHistoryAdapter.addReport(report);
        }
        saveReportHistory();
    }

    private void openReport(ReportSummary report) {
        if (getContext() == null) return;
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        Intent intent = new Intent(getContext(), ReportDetailActivity.class);
        intent.putExtra(ReportDetailActivity.EXTRA_TITLE, report.getTitle());
        intent.putExtra(ReportDetailActivity.EXTRA_CONTENT, report.getContent());
        startActivity(intent);
    }


    private void resetConversation() {
        if (gigaChatService != null) {
            gigaChatService.resetConversation();
            chatAdapter.clearMessages();

            addAssistantMessage("Разговор сброшен. Чем я могу помочь вам сегодня?");


            saveChatHistory();
        }
    }

    private String preprocessMessage(String message) {

        message = message.replaceAll("\\s+", " ").trim();


        message = message.replaceAll("\\?+", "?")
                .replaceAll("!+", "!")
                .replaceAll("\\.+", ".");


        String[] greetings = {"привет", "здравствуйте", "добрый день", "доброе утро", "добрый вечер"};
        for (String greeting : greetings) {
            message = message.toLowerCase().replaceAll("^" + greeting + "[,\\s]*", "");
        }


        message = message.toLowerCase()
                .replace("я хотел бы узнать", "")
                .replace("подскажите пожалуйста", "")
                .replace("не могли бы вы", "")
                .replace("скажите", "")
                .replace("расскажите", "");

        return message.trim();
    }


    private void scheduleWeeklyReports() {
        if (getContext() == null) return;


        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();


        Calendar now = Calendar.getInstance();
        Calendar nextSunday = Calendar.getInstance();


        nextSunday.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        nextSunday.set(Calendar.HOUR_OF_DAY, 20);
        nextSunday.set(Calendar.MINUTE, 0);
        nextSunday.set(Calendar.SECOND, 0);
        nextSunday.set(Calendar.MILLISECOND, 0);


        if (nextSunday.before(now) || nextSunday.equals(now)) {
            nextSunday.add(Calendar.WEEK_OF_YEAR, 1);
        }

        long initialDelay = nextSunday.getTimeInMillis() - now.getTimeInMillis();


        PeriodicWorkRequest weeklyReportWork = new PeriodicWorkRequest.Builder(
                WeeklyReportWorker.class,
                7, TimeUnit.DAYS,
                1, TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();


        WorkManager.getInstance(getContext()).enqueueUniquePeriodicWork(
                "weekly_report",
                ExistingPeriodicWorkPolicy.KEEP,
                weeklyReportWork
        );

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        Log.d(TAG, "Автоматическая генерация недельных отчетов настроена. Следующий отчет: " + sdf.format(nextSunday.getTime()));
    }

    private String collectUserStatsForReport() {
        if (getContext() == null) {
            return "Нет данных о пользователе.";
        }
        SharedPreferences userPrefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
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

        AppDatabase db = AppDatabase.getInstance(requireContext());
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

        MealsDatabase mealsDatabase = MealsDatabase.getInstance(requireContext());
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

    private void sendMessage() {
        if (isProcessingMessage) {
            Toast.makeText(getContext(), "Пожалуйста, подождите ответа...", Toast.LENGTH_SHORT).show();
            return;
        }

        final String originalMessage = messageInput.getText().toString().trim();
        if (originalMessage.isEmpty()) {
            return;
        }


        setUIState(false);


        final Handler timeoutHandler = new Handler(Looper.getMainLooper());
        final Runnable timeoutRunnable = () -> {
            if (isProcessingMessage && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Не удалось получить ответ. Пожалуйста, попробуйте еще раз.", Toast.LENGTH_LONG).show();
                    setUIState(true);
                });
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000);

        supabaseService.canSendMessage(canSend -> {
            if (getActivity() == null) {

                timeoutHandler.removeCallbacks(timeoutRunnable);
                setUIState(true);
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (canSend) {

                    String processedMessage = preprocessMessage(originalMessage);

                    if (processedMessage.isEmpty()) {
                        processedMessage = originalMessage;
                    }

                    addUserMessage(originalMessage);
                    messageInput.setText("");

                    gigaChatService.sendMessage(processedMessage, new GigaChatService.ChatCallback() {
                        @Override
                        public void onResponse(final String response) {

                            timeoutHandler.removeCallbacks(timeoutRunnable);

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    addAssistantMessage(response);
                                    saveChatHistory();
                                    setUIState(true);
                                });
                            } else {

                                isProcessingMessage = false;
                            }
                        }

                        @Override
                        public void onError(final String error) {

                            timeoutHandler.removeCallbacks(timeoutRunnable);

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_LONG).show();

                                    messageInput.setText(originalMessage);

                                    chatAdapter.removeLastMessage();
                                    setUIState(true);
                                });
                            } else {

                                isProcessingMessage = false;
                            }
                        }
                    });

                } else {

                    Toast.makeText(getContext(), "Вы достигли лимита отправки сообщений на сегодня. Попробуйте завтра.", Toast.LENGTH_LONG).show();

                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    setUIState(true);
                }
            });
        });
    }

    private void setUIState(boolean isEnabled) {
        isProcessingMessage = !isEnabled;


    }

    private void addUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true);
        chatAdapter.addMessage(chatMessage);
        chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void addAssistantMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, false);
        chatAdapter.addMessage(chatMessage);
        chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }


} 
