package com.martist.vitamove.dashboard;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.martist.vitamove.R;
import com.martist.vitamove.analytics.AnalyticsFragment;
import com.martist.vitamove.databinding.FragmentHomeBinding;
import com.martist.vitamove.nutrition.data.managers.CaloriesManager;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.nutrition.ui.CaloriesExplanationActivity;
import com.martist.vitamove.nutrition.ui.NutritionAnalyticsActivity;
import com.martist.vitamove.steps.StepsStatsFragment;
import com.martist.vitamove.user.UserProfile;
import com.martist.vitamove.water.data.WaterHistoryManager;
import com.martist.vitamove.water.ui.WaterBalanceFragment;
import com.martist.vitamove.weight.ui.UserWeightViewModel;
import com.martist.vitamove.weight.ui.WeightHistoryActivity;
import com.martist.vitamove.workout.domain.WorkoutCompletedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class HomeFragment extends Fragment {
    private UserWeightViewModel weightViewModel;


    private DashboardManager dashboardManager;
    private DashboardData dashboardData;
    private CaloriesManager caloriesManager;
    private WaterHistoryManager waterHistoryManager;


    private Handler updateHandler;
    private static final int UPDATE_INTERVAL_MS = 5000;
    private boolean isUpdateActive = false;
    private FragmentHomeBinding binding;
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUpdateActive && dashboardManager != null) {

                dashboardManager.syncStepsData();
                dashboardData = dashboardManager.getDashboardData();


                if (isAdded() && !isDetached() && getActivity() != null) {
                    binding.stepsCount.setText(String.format(Locale.getDefault(), "%,d", dashboardData.getStepsToday()));

                    Log.d("HomeFragment", "Периодическое обновление шагов: " + dashboardData.getStepsToday());
                }


                updateHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        }
    };


    private final BroadcastReceiver dashboardUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("HomeFragment", "Получено сообщение об обновлении данных: " +
                    intent.getStringExtra("update_source"));


            SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
            int targetCalories = prefs.getInt("target_calories", 0);
            if (targetCalories > 0 && caloriesManager != null) {
                caloriesManager.setTargetCalories(targetCalories);
                Log.d("HomeFragment", "Принудительно обновлены целевые калории в CaloriesManager: " + targetCalories);
            }


            initializeCorrectCaloriesGoal();


            caloriesManager.forceReloadFromDatabase();


            dashboardData = dashboardManager.getDashboardData();


            int burnedCalories = caloriesManager.getTotalBurnedCalories();
            int consumedCalories = caloriesManager.getConsumedCalories();


            dashboardData.setCaloriesBurned(burnedCalories);
            dashboardData.setCaloriesConsumed(consumedCalories);


            updateCaloriesUI(burnedCalories, consumedCalories);


            setupStepsCard();
            setupWaterCard();
            setupWeightCard();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackground(null);


        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusbar_color));


            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }


        dashboardManager = DashboardManager.getInstance(requireContext());
        caloriesManager = CaloriesManager.getInstance(requireContext());
        waterHistoryManager = WaterHistoryManager.getInstance(requireContext());


        updateHandler = new Handler(Looper.getMainLooper());


        weightViewModel = new ViewModelProvider(this).get(UserWeightViewModel.class);


        initializeCorrectCaloriesGoal();


        dashboardData = dashboardManager.getDashboardData();
        updateGreetingAndDate();
        setupStepsCard();
        setupCaloriesCard();
        setupWaterCard();
        setupWeightCard();


        setupButtonListeners();


        observeCaloriesChanges();
    }


    private void updateGreetingAndDate() {

        SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String userName = prefs.getString("name", "Пользователь");


        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (timeOfDay < 12) {
            greeting = "Доброе утро";
        } else if (timeOfDay < 18) {
            greeting = "Добрый день";
        } else {
            greeting = "Добрый вечер";
        }

        binding.dashboardGreeting.setText(greeting + ", " + userName + "!");


        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM", new Locale("ru"));
        String currentDate = "Сегодня, " + dateFormat.format(new Date());
        binding.dashboardDate.setText(currentDate);
    }


    private void setupStepsCard() {

        binding.stepsCount.setText(String.format(Locale.getDefault(), "%,d", dashboardData.getStepsToday()));
    }


    private void setupCaloriesCard() {

        if (caloriesManager == null) {
            return;
        }


        if (dashboardData == null) {
            Log.e("HomeFragment", "setupCaloriesCard: dashboardData не инициализирован");


            if (dashboardManager != null) {
                dashboardData = dashboardManager.getDashboardData();
            }


            if (dashboardData == null) {
                return;
            }
        }


        int caloriesBurned = caloriesManager.getTotalBurnedCalories();
        int caloriesConsumed = caloriesManager.getConsumedCalories();


        dashboardData.setCaloriesBurned(caloriesBurned);
        dashboardData.setCaloriesConsumed(caloriesConsumed);


        updateCaloriesUI(caloriesBurned, caloriesConsumed);
    }


    private void observeCaloriesChanges() {

        caloriesManager.getBurnedCaloriesLiveData().observe(getViewLifecycleOwner(), burnedCalories -> {

            updateCaloriesUI(burnedCalories, dashboardData.getCaloriesConsumed());
            Log.d("HomeFragment", "Получены обновленные данные о сожженных калориях: " + burnedCalories + " кал.");
        });


        caloriesManager.getConsumedCaloriesLiveData().observe(getViewLifecycleOwner(), consumedCalories -> {

            dashboardData.setCaloriesConsumed(consumedCalories);


            updateCaloriesUI(dashboardData.getCaloriesBurned(), consumedCalories);
            Log.d("HomeFragment", "Получены обновленные данные о потребленных калориях: " + consumedCalories + " кал.");
        });
    }


    private void updateCaloriesUI(int burnedCalories, int consumedCalories) {

        if (dashboardData == null) {
            Log.e("HomeFragment", "updateCaloriesUI: dashboardData не инициализирован");
            return;
        }


        int caloriesGoalValue = dashboardData.getCaloriesGoal();


        if (caloriesGoalValue <= 0) {

            initializeCorrectCaloriesGoal();

            caloriesGoalValue = dashboardData.getCaloriesGoal();


            if (caloriesGoalValue <= 0) {
                caloriesGoalValue = 2000;
                Log.w("HomeFragment", "Не удалось получить корректное значение целевых калорий, используем значение по умолчанию: " + caloriesGoalValue);
            }
        }


        int totalAvailableCalories = caloriesGoalValue + burnedCalories;
        int remainingCalories = totalAvailableCalories - consumedCalories;


        if (remainingCalories < 0) {
            remainingCalories = 0;
        }


        binding.caloriesRemaining.setText(String.format(Locale.getDefault(), "%,d", remainingCalories));
        binding.caloriesFood.setText(String.format(Locale.getDefault(), "%,d", consumedCalories));
        binding.caloriesExercise.setText(String.format(Locale.getDefault(), "%,d", burnedCalories));


        int consumedPercent = totalAvailableCalories > 0 ?
                (int) (((float) consumedCalories / totalAvailableCalories) * 100) : 0;
        consumedPercent = Math.min(consumedPercent, 100);

        int currentPercent = binding.caloriesCircularProgress.getProgress();

        ValueAnimator animator = ValueAnimator.ofInt(currentPercent, consumedPercent);
        animator.setDuration(800);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int animationValue = (int) animation.getAnimatedValue();
            binding.caloriesCircularProgress.setProgress(animationValue);
        });
        animator.start();
    }


    private void setupButtonListeners() {


        binding.homeCaloriesPotreb.setOnClickListener(v -> startActivity(new Intent(getContext(), NutritionAnalyticsActivity.class)));
        binding.homeCaloriesBurned.setOnClickListener(v -> getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new AnalyticsFragment())
                .addToBackStack(null)
                .commit()

        );
        binding.cardCalories.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CaloriesExplanationActivity.class));
        });

        float[] waterPortions = loadWaterPortions();
        binding.btnAddWater200.setOnClickListener(v -> addWater(waterPortions[0], "Быстрое добавление"));
        binding.btnAddWater500.setOnClickListener(v -> addWater(waterPortions[1], "Быстрое добавление"));


        updateWaterButtonsText(waterPortions);


        View waterCard = getView().findViewById(R.id.card_water);
        waterCard.setOnClickListener(v -> {

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new WaterBalanceFragment())
                    .addToBackStack(null)
                    .commit();
        });


        View stepsCard = getView().findViewById(R.id.card_steps);
        stepsCard.setOnClickListener(v -> {

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StepsStatsFragment())
                    .addToBackStack(null)
                    .commit();

            Log.d("HomeFragment", "Открываем экран статистики шагов");
        });


        binding.cardWeight.setOnClickListener(v -> {

            Intent intent = new Intent(getActivity(), WeightHistoryActivity.class);
            startActivity(intent);
            Log.d("HomeFragment", "Открываем экран истории веса");
        });
    }


    private float[] loadWaterPortions() {
        SharedPreferences prefs = requireContext().getSharedPreferences("water_portions_prefs", Context.MODE_PRIVATE);
        float portion1 = prefs.getFloat("portion_1", 0.2f);
        float portion2 = prefs.getFloat("portion_2", 0.5f);
        return new float[]{portion1, portion2};
    }


    private void updateWaterButtonsText(float[] portions) {
        binding.tvWaterAmount200.setText(String.format("%.0f мл", portions[0] * 1000));
        binding.tvWaterAmount500.setText(String.format("%.0f мл", portions[1] * 1000));
    }


    private void setupWaterCard() {

        float waterConsumed = waterHistoryManager.getTotalWaterConsumption();


        dashboardData.setWaterConsumed(waterConsumed);

        float waterGoal = dashboardData.getWaterGoal();


        binding.waterAmount.setText(String.format(Locale.getDefault(), "%.1f / %.1f л", waterConsumed, waterGoal));


        int waterPercent = (int) ((waterConsumed / waterGoal) * 100);
        binding.waterProgressText.setText(waterPercent + "%");

        int currentProgress = binding.waterCircularProgress.getProgress();
        int finalProgress = Math.min(waterPercent, 100);

        ValueAnimator animator = ValueAnimator.ofInt(currentProgress, finalProgress);
        animator.setDuration(800);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            binding.waterCircularProgress.setProgress(animatedValue);
        });
        animator.start();

        Log.d("HomeFragment", "Обновлена карточка водного баланса: " +
                waterConsumed + "/" + waterGoal + " л (" + waterPercent + "%)");
    }


    private void addWater(float amount, String description) {

        dashboardManager.addWaterConsumption(amount);


        waterHistoryManager.addWaterRecord(amount, description);


        dashboardData = dashboardManager.getDashboardData();


        setupWaterCard();
    }


    private void setupWeightCard() {

        SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        final float targetWeightPref = prefs.getFloat("target_weight", 0f);
        final float initialWeightPref = prefs.getFloat("initial_weight", 0f);
        final float currentWeightPref = prefs.getFloat("current_weight", 0f);


        final float initialWeight;
        if (initialWeightPref <= 0) {
            initialWeight = currentWeightPref;

            if (initialWeight > 0) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("initial_weight", initialWeight);
                editor.apply();
            }
        } else {
            initialWeight = initialWeightPref;
        }

        final float targetWeight = targetWeightPref;

        if (currentWeightPref > 0) {
            binding.currentWeightCard.setText(String.format(Locale.getDefault(), "%.1f кг", currentWeightPref));
            Log.d("HomeFragment", "Установлен текущий вес из SharedPreferences: " + currentWeightPref + " кг");
        } else {
            binding.currentWeightCard.setText("–");
        }


        weightViewModel.getLatestWeightRecord().observe(getViewLifecycleOwner(), weightRecord -> {
            if (weightRecord != null) {
                float weightFromDb = weightRecord.getWeight();


                if (currentWeightPref > 0 && Math.abs(currentWeightPref - weightFromDb) > 5) {

                    Log.w("HomeFragment", "Значительное расхождение между весом в БД (" + weightFromDb +
                            " кг) и SharedPreferences (" + currentWeightPref + " кг). Используем вес из SharedPreferences.");


                    binding.currentWeightCard.setText(String.format(Locale.getDefault(), "%.1f кг", currentWeightPref));
                } else {

                    binding.currentWeightCard.setText(String.format(Locale.getDefault(), "%.1f кг", weightFromDb));


                    if (Math.abs(currentWeightPref - weightFromDb) > 0.1) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putFloat("current_weight", weightFromDb);
                        editor.apply();
                        Log.d("HomeFragment", "Обновлен текущий вес в SharedPreferences: " + weightFromDb + " кг");
                    }
                }

                Log.d("HomeFragment", "Обновлена карточка веса: текущий=" + weightFromDb +
                        ", начальный=" + initialWeight + ", целевой=" + targetWeight);
            } else {

                if (currentWeightPref > 0) {
                    binding.currentWeightCard.setText(String.format(Locale.getDefault(), "%.1f кг", currentWeightPref));
                } else {
                    binding.currentWeightCard.setText("–");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();


        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
            Log.d("HomeFragment", "Зарегистрирован на EventBus в onResume");
        }


        IntentFilter filter = new IntentFilter("com.martist.vitamove.UPDATE_DASHBOARD");
        requireContext().registerReceiver(dashboardUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);


        if (dashboardManager != null) {

            dashboardManager.checkAndResetDailyDataIfNeeded();
            waterHistoryManager.checkAndResetDailyDataIfNeeded();


            dashboardManager.syncStepsData();


            dashboardData = dashboardManager.getDashboardData();


            dashboardData.setWaterConsumed(waterHistoryManager.getTotalWaterConsumption());


            float[] waterPortions = loadWaterPortions();
            updateWaterButtonsText(waterPortions);


            binding.btnAddWater200.setOnClickListener(v -> addWater(waterPortions[0], "Быстрое добавление"));
            binding.btnAddWater500.setOnClickListener(v -> addWater(waterPortions[1], "Быстрое добавление"));


            setupStepsCard();
            setupCaloriesCard();
            setupWaterCard();
            setupWeightCard();


            startPeriodicUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();


        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
            Log.d("HomeFragment", "Отписка от EventBus в onPause");
        }


        try {
            requireContext().unregisterReceiver(dashboardUpdateReceiver);
        } catch (IllegalArgumentException e) {

            Log.e("HomeFragment", "Ошибка при отмене регистрации приемника: " + e.getMessage());
        }


        stopPeriodicUpdates();
    }


    private void startPeriodicUpdates() {
        if (!isUpdateActive) {
            isUpdateActive = true;
            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
            Log.d("HomeFragment", "Запущено периодическое обновление шагов");
        }
    }


    private void stopPeriodicUpdates() {
        isUpdateActive = false;
        updateHandler.removeCallbacks(updateRunnable);
        Log.d("HomeFragment", "Остановлено периодическое обновление шагов");
    }


    public void updateDashboardData() {
        if (dashboardManager != null) {

            dashboardManager.syncStepsData();


            dashboardData = dashboardManager.getDashboardData();


            updateGreetingAndDate();
            setupStepsCard();
            setupCaloriesCard();
            setupWaterCard();
            setupWeightCard();


            Log.d("HomeFragment", "Данные дашборда обновлены асинхронно. Вода: " +
                    dashboardData.getWaterConsumed() + "/" + dashboardData.getWaterGoal() + " л, " +
                    "Калории: " + dashboardData.getCaloriesGoal() + " ккал.");
        }
    }


    private void initializeCorrectCaloriesGoal() {
        try {

            SharedPreferences prefs = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);


            int targetCalories = prefs.getInt("target_calories", 0);


            if (targetCalories <= 0 || targetCalories == 3178) {
                Log.d("HomeFragment", "Целевые калории не установлены или равны значению по умолчанию. Рассчитываем правильное значение.");


                String name = prefs.getString("name", "Пользователь");
                int age = prefs.getInt("age", 30);
                String gender = prefs.getString("gender", "Мужчина");
                float currentWeight = prefs.getFloat("current_weight", 70);
                float targetWeight = prefs.getFloat("target_weight", 70);
                float height = prefs.getFloat("height", 175);
                float bodyFat = prefs.getFloat("body_fat", 20);
                float waist = prefs.getFloat("waist", 80);


                UserProfile userProfile = new UserProfile(name, age, gender, currentWeight,
                        targetWeight, height, bodyFat, waist);
                userProfile.updateTargetCalories();


                targetCalories = userProfile.getTargetCalories();


                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("target_calories", targetCalories);
                editor.apply();


                dashboardManager.updateCaloriesGoalFromProfile();


                caloriesManager.setTargetCalories(targetCalories);

                Log.d("HomeFragment", "Целевые калории обновлены: " + targetCalories + " ккал");
            } else {
                Log.d("HomeFragment", "Целевые калории уже установлены: " + targetCalories + " ккал");
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Ошибка при инициализации целевых калорий", e);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWorkoutCompleted(WorkoutCompletedEvent event) {
        Log.d("HomeFragment", "Получено событие завершения тренировки. Калории: " + event.getCaloriesBurned());


        if (caloriesManager != null && dashboardData != null) {

            int burnedCalories = caloriesManager.getTotalBurnedCalories();
            int consumedCalories = caloriesManager.getConsumedCalories();


            dashboardData.setCaloriesBurned(burnedCalories);
            dashboardData.setCaloriesConsumed(consumedCalories);


            updateCaloriesUI(burnedCalories, consumedCalories);

            Log.d("HomeFragment", "UI калорий обновлен после завершения тренировки. Сожжено: " + burnedCalories + " кал.");
        }
    }
}