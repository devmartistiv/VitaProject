package com.martist.vitamove.core.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.martist.vitamove.R;
import com.martist.vitamove.assistant.AssistantFragment;
import com.martist.vitamove.auth.OnboardingActivity;
import com.martist.vitamove.core.data.managers.UpdateManager;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.BMICalculator;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.dashboard.HomeFragment;
import com.martist.vitamove.exercise.ui.ExercisesViewModel;
import com.martist.vitamove.nutrition.data.FoodSyncService;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.data.repository.SupabaseFoodRepository;
import com.martist.vitamove.nutrition.ui.CaloriesFragment;
import com.martist.vitamove.programs.ui.CreateProgramWeekActivity;
import com.martist.vitamove.user.ProfileFragment;
import com.martist.vitamove.water.ui.WaterBalanceFragment;
import com.martist.vitamove.workout.ui.fragments.WorkoutFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends BaseActivity implements View.OnClickListener {
    private ImageView btnAssistant, btnWorkouts, btnHome, btnNutrition, btnProfile;

    private Fragment currentFragment;


    private LinearLayout bottomNavigation;
    private boolean isKeyboardVisible = false;
    private boolean isNavigationHidden = false;

    ExercisesViewModel exercisesViewModel;
    private static final String TAG = "MainActivity";
    private static final int ACTIVITY_RECOGNITION_PERMISSION_CODE = 100;
    private static final int KEYBOARD_ANIMATION_DURATION = 250;


    private UpdateManager updateManager;
    private View navIndicator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_VitaMove);
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(null);
        setContentView(R.layout.activity_main);
        setupKeyboardHandling();
        exercisesViewModel = new ViewModelProvider(this).get(ExercisesViewModel.class);

        updateManager = UpdateManager.getInstance(this);
        updateManager.setCurrentActivity(this);


        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);
        boolean isLogged = prefs.getBoolean("isLogged", false);
        boolean exercisesCached = prefs.getBoolean("exercises_cached", false);

        if (isFirstRun || !isLogged) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkActivityRecognitionPermission();
        }


        if (!exercisesViewModel.isExercisesInRoomRelevance()) {

            exercisesViewModel.getAllExercisesFromRemote();

        }

        syncFitnessGoals();
        FoodManager foodManager = FoodManager.getInstance(this);
        foodManager.refreshNutrientNorms();


        initializeFoodSync();


        initNavigationButtons();


        String openTab = getIntent().getStringExtra("open_tab");
        String openFragment = getIntent().getStringExtra("open_fragment");
        boolean navigateToPrograms = getIntent().getBooleanExtra(CreateProgramWeekActivity.EXTRA_NAVIGATE_TO_PROGRAMS, false);

        if (navigateToPrograms) {

            loadFragment(new WorkoutFragment());
            btnWorkouts.setSelected(true);


            prefs.edit().putInt("workout_tab_index", 2).apply();
        } else if (openFragment != null) {

            handleNotificationNavigation(openFragment, prefs);
        } else if (openTab != null && openTab.equals("workout")) {

            loadFragment(new WorkoutFragment());
            btnWorkouts.setSelected(true);


            int workoutTabIndex = getIntent().getIntExtra("workout_tab_index", -1);
            String activeWorkoutId = getIntent().getStringExtra("active_workout_id");
            if (workoutTabIndex != -1) {

                SharedPreferences.Editor editor = prefs.edit().putInt("workout_tab_index", workoutTabIndex);
                if (activeWorkoutId != null) {

                    editor.putString("active_workout_id_for_fragment", activeWorkoutId);
                }
                editor.apply();
            }
        } else if (savedInstanceState == null) {

            loadFragment(new HomeFragment());
            btnHome.setSelected(true);

        }


    }


    private void checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        ACTIVITY_RECOGNITION_PERMISSION_CODE);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    private void initNavigationButtons() {
        bottomNavigation = findViewById(R.id.bottom_nav);
        View navAssistant = findViewById(R.id.nav_assistant);
        View navWorkouts = findViewById(R.id.nav_workouts);
        View navHome = findViewById(R.id.nav_home);
        View navNutrition = findViewById(R.id.nav_nutrition);
        View navProfile = findViewById(R.id.nav_profile);
        navIndicator = findViewById(R.id.nav_indicator);

        navHome.post(() -> {
            updateWidths(navHome);
            navIndicator.setX(navHome.getX());
        });


        btnAssistant = findViewById(R.id.btn_assistant);
        btnWorkouts = findViewById(R.id.btn_workouts);
        btnHome = findViewById(R.id.btn_home);
        btnNutrition = findViewById(R.id.btn_nutrition);
        btnProfile = findViewById(R.id.btn_profile);

        navAssistant.setOnClickListener(this);
        navWorkouts.setOnClickListener(this);
        navHome.setOnClickListener(this);
        navNutrition.setOnClickListener(this);
        navProfile.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {


        resetButtonSelection();

        animateIndicator(view);


        int id = view.getId();
        if (id != R.id.nav_assistant) {
            ensureBottomNavigationVisible();
        }

        switch (id) {
            case R.id.nav_assistant:
                loadFragment(new AssistantFragment());
                btnAssistant.setSelected(true);

                break;
            case R.id.nav_workouts:
                loadFragment(new WorkoutFragment());
                btnWorkouts.setSelected(true);

                break;
            case R.id.nav_home:
                loadFragment(new HomeFragment());
                btnHome.setSelected(true);

                break;
            case R.id.nav_nutrition:
                loadFragment(new CaloriesFragment());
                btnNutrition.setSelected(true);

                break;
            case R.id.nav_profile:
                loadFragment(new ProfileFragment());
                btnProfile.setSelected(true);

                break;
        }
    }


    private void resetButtonSelection() {
        btnAssistant.setSelected(false);

        btnWorkouts.setSelected(false);

        btnHome.setSelected(false);

        btnNutrition.setSelected(false);

        btnProfile.setSelected(false);

    }


    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            currentFragment = fragment;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    private void animateIndicator(View targetView) {

        navIndicator.animate()
                .x(targetView.getX())
                .setDuration(300)
                .start();
        updateWidths(targetView);
    }

    private void updateWidths(View targetView) {
        int width = targetView.getWidth();
        ViewGroup.LayoutParams p1 = navIndicator.getLayoutParams();
        p1.width = width;
        navIndicator.setLayoutParams(p1);
    }


    private void handleNotificationNavigation(String fragmentType, SharedPreferences prefs) {
        resetButtonSelection();

        switch (fragmentType) {
            case "calories":

                loadFragment(new CaloriesFragment());
                btnNutrition.setSelected(true);

                Log.d(TAG, "Opened CaloriesFragment from dinner notification");
                break;

            case "water":

                loadFragment(new WaterBalanceFragment());
                btnHome.setSelected(true);

                Log.d(TAG, "Opened WaterBalanceFragment from water notification");
                break;

            case "programs":

                loadFragment(new WorkoutFragment());
                btnWorkouts.setSelected(true);


                int workoutTabIndex = getIntent().getIntExtra("workout_tab_index", 2);
                prefs.edit().putInt("workout_tab_index", workoutTabIndex).apply();
                Log.d(TAG, "Opened WorkoutFragment (Programs tab) from workout notification");
                break;

            default:

                loadFragment(new HomeFragment());
                btnHome.setSelected(true);

                Log.d(TAG, "Unknown fragment type: " + fragmentType + ", opened HomeFragment");
                break;
        }
    }


    private void syncFitnessGoals() {

        SharedPreferences userDataPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String userDataGoal = userDataPrefs.getString("fitness_goal", "");


        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String appPrefsGoal = appPrefs.getString("fitness_goal", "weight_loss");


        if (!userDataGoal.isEmpty() && !userDataGoal.equals(appPrefsGoal)) {

            appPrefs.edit().putString("fitness_goal", userDataGoal).apply();
            Log.d(TAG, "Синхронизирована фитнес-цель из user_data: " + userDataGoal);
        } else if (userDataGoal.isEmpty() && !appPrefsGoal.isEmpty()) {
            userDataPrefs.edit().putString("fitness_goal", appPrefsGoal).apply();
            Log.d(TAG, "Синхронизирована фитнес-цель из основных настроек: " + appPrefsGoal);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (updateManager != null) {
            updateManager.setCurrentActivity(this);
            updateManager.checkForUpdates();
        }
        checkAndSyncUserData();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();


        if (updateManager != null) {
            updateManager.cleanup();
        }


        DashboardManager dashboardManager = DashboardManager.getInstance(this);
        dashboardManager.stopTracking();
    }


    private void syncUserDataWithSupabase() {


        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId == null) {
            Log.w(TAG, "ID пользователя не найден, синхронизация невозможна");
            return;
        }


        String name = prefs.getString("user_name", "");
        int age = prefs.getInt("user_age", 0);
        String gender = prefs.getString("user_gender", "");
        String fitnessGoal = prefs.getString("user_fitness_goal", "weight_loss");
        float height = prefs.getFloat("user_height", 0);
        float currentWeight = prefs.getFloat("user_current_weight", 0);
        float targetWeight = prefs.getFloat("user_target_weight", 0);
        String fitnessLevel = prefs.getString("user_fitness_level", "beginner");
        boolean isMetric = prefs.getBoolean("use_metric", true);


        if (name.isEmpty() || age == 0 || gender.isEmpty() || height == 0 || currentWeight == 0 || targetWeight == 0) {
            return;
        }


        String accessToken = prefs.getString("accessToken", null);
        String refreshToken = prefs.getString("refreshToken", null);

        if (accessToken == null || refreshToken == null) {
            Log.e(TAG, "Отсутствуют токены авторизации, синхронизация невозможна");
            return;
        }


        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );


        supabaseClient.setUserToken(accessToken);
        supabaseClient.setRefreshToken(refreshToken);

        Log.d(TAG, "Отправка данных в Supabase: userId=" + userId + ", name=" + name);


        new Thread(() -> {
            try {
                boolean success = supabaseClient.updateUserProfile(
                        userId,
                        name,
                        age,
                        gender,
                        fitnessGoal,
                        height,
                        currentWeight,
                        targetWeight,
                        fitnessLevel,
                        isMetric
                );

                if (success) {
                    Log.d(TAG, "Данные пользователя успешно синхронизированы с Supabase");


                    SharedPreferences.Editor syncEditor = prefs.edit();
                    syncEditor.putBoolean("user_data_synced", true);
                    syncEditor.apply();


                    SharedPreferences userDataPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
                    SharedPreferences.Editor userDataEditor = userDataPrefs.edit();

                    userDataEditor.putString("name", name);
                    userDataEditor.putInt("age", age);
                    userDataEditor.putString("gender", gender);
                    userDataEditor.putString("fitness_goal", fitnessGoal);
                    userDataEditor.putFloat("height", height);
                    userDataEditor.putFloat("current_weight", currentWeight);
                    userDataEditor.putFloat("target_weight", targetWeight);
                    userDataEditor.putString("fitness_level", fitnessLevel);
                    userDataEditor.putFloat("bmi", BMICalculator.calculateBMI(currentWeight, height));
                    userDataEditor.putBoolean("is_metric", isMetric);

                    userDataEditor.apply();

                    Log.d(TAG, "Данные пользователя также сохранены в user_data для профиля");
                } else {
                    Log.e(TAG, "Не удалось синхронизировать данные пользователя с Supabase");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при синхронизации данных пользователя: " + e.getMessage(), e);
            }
        }).start();
    }


    private void checkAndSyncUserData() {
        SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        boolean dataSynced = prefs.getBoolean("user_data_synced", false);

        if (!dataSynced) {
            syncUserDataWithSupabase();

            prefs.edit().putBoolean("user_data_synced", true).apply();
        }
    }


    public void updateNavigationHeader() {

        DashboardManager dashboardManager = DashboardManager.getInstance(this);
        dashboardManager.updateWaterGoalFromProfile();
        dashboardManager.updateCaloriesGoalFromProfile();


        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {

            ((HomeFragment) currentFragment).updateDashboardData();

        }

    }


    private void setupKeyboardHandling() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                boolean keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());


                if (keyboardVisible != isKeyboardVisible) {
                    isKeyboardVisible = keyboardVisible;
                    handleKeyboardVisibilityChange(keyboardVisible);
                }

                return insets;
            });
        }
    }


    private void handleKeyboardVisibilityChange(boolean isVisible) {


        if (currentFragment instanceof AssistantFragment) {
            if (isVisible && !isNavigationHidden) {
                hideBottomNavigation();
            } else if (!isVisible && isNavigationHidden) {
                showBottomNavigation();
            }
        }
    }


    private void hideBottomNavigation() {
        if (bottomNavigation == null || isNavigationHidden) return;


        TranslateAnimation slideDown = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f
        );
        slideDown.setDuration(KEYBOARD_ANIMATION_DURATION);
        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                bottomNavigation.setVisibility(View.GONE);

                isNavigationHidden = true;


                updateFragmentContainerConstraints(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        bottomNavigation.startAnimation(slideDown);

    }


    private void showBottomNavigation() {
        if (bottomNavigation == null || !isNavigationHidden) return;


        bottomNavigation.setVisibility(View.VISIBLE);


        updateFragmentContainerConstraints(false);


        TranslateAnimation slideUp = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f
        );
        slideUp.setDuration(KEYBOARD_ANIMATION_DURATION);
        slideUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isNavigationHidden = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        bottomNavigation.startAnimation(slideUp);

    }


    private void updateFragmentContainerConstraints(boolean navigationHidden) {
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            ViewGroup.LayoutParams params = fragmentContainer.getLayoutParams();
            if (params instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams constraintParams =
                        (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) params;

                if (navigationHidden) {

                    constraintParams.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    constraintParams.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                } else {

                    constraintParams.bottomToTop = R.id.bottom_nav;
                    constraintParams.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                }

                fragmentContainer.setLayoutParams(constraintParams);
            }
        }
    }


    public void ensureBottomNavigationVisible() {
        if (isNavigationHidden) {
            showBottomNavigation();
        }
    }


    private void initializeFoodSync() {
        new Thread(() -> {
            try {


                SupabaseClient supabaseClient = SupabaseClient.getInstance(
                        Constants.SUPABASE_CLIENT_ID,
                        Constants.SUPABASE_CLIENT_SECRET
                );

                SupabaseFoodRepository foodRepository = new SupabaseFoodRepository(supabaseClient, this);


                FoodSyncService foodSyncService = new FoodSyncService(this, foodRepository);


                if (foodSyncService.shouldSync()) {
                    Log.d(TAG, "Запуск синхронизации продуктов");


                    foodSyncService.syncFoods(false, new FoodSyncService.SyncCallback() {
                        @Override
                        public void onSyncStarted() {
                            Log.d(TAG, "Синхронизация продуктов начата");
                        }

                        @Override
                        public void onSyncProgress(int current, int total) {
                            Log.d(TAG, "Прогресс синхронизации: " + current + "/" + total);
                        }

                        @Override
                        public void onSyncCompleted(int syncedCount) {
                            Log.d(TAG, "Синхронизация продуктов завершена. Синхронизировано: " + syncedCount);

                        }

                        @Override
                        public void onSyncError(String error) {
                            Log.e(TAG, "Ошибка синхронизации продуктов: " + error);

                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при инициализации синхронизации продуктов: " + e.getMessage(), e);
            }
        }).start();
    }
}