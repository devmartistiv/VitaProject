package com.martist.vitamove.exercise.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.martist.vitamove.R;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.exercise.data.managers.ExerciseManager;
import com.martist.vitamove.exercise.domain.AddExerciseEvent;
import com.martist.vitamove.exercise.ui.model.Exercise;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ExerciseDetailsActivity extends BaseActivity {
    private static final String TAG = "ExerciseDetailsActivity";
    public static final String EXTRA_EXERCISE_ID = "exercise_id";

    private String exerciseId;
    private ExerciseManager exerciseManager;
    private Exercise exercise;


    private TextView textExerciseName;
    private TextView textDifficulty;
    private TextView textType;
    private TextView textDescription;
    private ChipGroup chipGroupPrimaryMuscles;
    private ChipGroup chipGroupSecondaryMuscles;
    private ChipGroup chipGroupStabilizerMuscles;
    private TextView textEquipment;
    private TextView textInstructions;
    private TextView textCommonMistakes;
    private TextView textContraindications;
    private Button buttonAddToWorkout;
    private ProgressBar loadingIndicator;
    private NestedScrollView contentContainer;


    private TextView labelSecondaryMuscles;
    private TextView labelStabilizerMuscles;
    private TextView labelInstructions;
    private TextView labelCommonMistakes;
    private TextView labelContraindications;

    private Executor executor;
    private Handler mainHandler;

    private boolean hideAddButton = false;


    private GestureDetectorCompat gestureDetector;
    private ConstraintLayout rootLayout;


    public static Intent newIntent(Context context, String exerciseId) {
        Intent intent = new Intent(context, ExerciseDetailsActivity.class);
        intent.putExtra(EXTRA_EXERCISE_ID, exerciseId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_details);

        exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);

        hideAddButton = getIntent().getBooleanExtra("hide_add_button", false);

        boolean showAddToProgramButton = getIntent().getBooleanExtra("show_add_to_program_button", false);

        boolean fromAnalytics = getIntent().getBooleanExtra("from_analytics", false);

        if (exerciseId == null) {
            Log.e(TAG, "No exercise ID provided in intent");
            finish();
            return;
        }

        exerciseManager = new ExerciseManager(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initializeViews();
        setupActionBar();
        setupGestureDetector();


        if (showAddToProgramButton && buttonAddToWorkout != null) {
            buttonAddToWorkout.setText("Добавить в программу");
            buttonAddToWorkout.setOnClickListener(v -> addExerciseToProgram());
        } else if (fromAnalytics && buttonAddToWorkout != null) {
            buttonAddToWorkout.setText("Отслеживать");
        }

        loadExerciseDetails();
    }

    private void initializeViews() {

        textExerciseName = findViewById(R.id.textExerciseName);
        textDifficulty = findViewById(R.id.textDifficulty);
        textType = findViewById(R.id.textType);
        textDescription = findViewById(R.id.textDescription);


        chipGroupPrimaryMuscles = findViewById(R.id.chipGroupPrimaryMuscles);
        chipGroupSecondaryMuscles = findViewById(R.id.chipGroupSecondaryMuscles);
        chipGroupStabilizerMuscles = findViewById(R.id.chipGroupStabilizerMuscles);


        textEquipment = findViewById(R.id.textEquipment);
        textInstructions = findViewById(R.id.textInstructions);
        textCommonMistakes = findViewById(R.id.textCommonMistakes);
        textContraindications = findViewById(R.id.textContraindications);


        labelSecondaryMuscles = findViewById(R.id.labelSecondaryMuscles);
        labelStabilizerMuscles = findViewById(R.id.labelStabilizerMuscles);
        labelInstructions = findViewById(R.id.labelInstructions);
        labelCommonMistakes = findViewById(R.id.labelCommonMistakes);
        labelContraindications = findViewById(R.id.labelContraindications);


        buttonAddToWorkout = findViewById(R.id.buttonAddToWorkout);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        contentContainer = findViewById(R.id.contentContainer);


        rootLayout = findViewById(R.id.content_container);


        MaterialButton backButton = findViewById(R.id.back_button);


        if (hideAddButton && buttonAddToWorkout != null) {
            buttonAddToWorkout.setVisibility(View.GONE);


            if (backButton != null) {
                backButton.setVisibility(View.VISIBLE);
                backButton.setOnClickListener(v -> {

                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                });
            }
        } else {

            if (backButton != null) {
                backButton.setVisibility(View.GONE);
            }
        }


        buttonAddToWorkout.setOnClickListener(v -> postAddExerciseEvent());
    }

    private void setupActionBar() {

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.exercise_details);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (hideAddButton) {


            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        } else {
            return super.onSupportNavigateUp();
        }
    }

    private void loadExerciseDetails() {
        showLoading(true);

        executor.execute(() -> {
            try {
                exercise = exerciseManager.getExerciseById(exerciseId);
                mainHandler.post(() -> {
                    if (exercise != null) {
                        populateUI();
                    } else {
                        showError(getString(R.string.error_exercise_not_found));
                    }
                    showLoading(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading exercise: " + e.getMessage());
                mainHandler.post(() -> {
                    showError(getString(R.string.error_loading_exercise) + e.getMessage());
                    showLoading(false);
                });
            }
        });
    }

    private void populateUI() {

        textExerciseName.setText(exercise.getName());


        textDifficulty.setText(exercise.getDifficultyRussianName());


        textType.setText(exercise.getExerciseTypeRussianName());


        if (exercise.getDescription() != null && !exercise.getDescription().isEmpty()) {
            textDescription.setText(exercise.getDescription());
            textDescription.setVisibility(View.VISIBLE);
        } else {
            textDescription.setVisibility(View.GONE);
        }


        setupMuscleGroupChips(chipGroupPrimaryMuscles, exercise.getMuscleGroupRussianNames());


        List<String> secondaryMuscles = exercise.getSecondaryMuscleRussianNames();
        if (secondaryMuscles != null && !secondaryMuscles.isEmpty()) {
            setupMuscleGroupChips(chipGroupSecondaryMuscles, secondaryMuscles);
            chipGroupSecondaryMuscles.setVisibility(View.VISIBLE);
            labelSecondaryMuscles.setVisibility(View.VISIBLE);
        } else {
            chipGroupSecondaryMuscles.setVisibility(View.GONE);
            labelSecondaryMuscles.setVisibility(View.GONE);
        }


        List<String> stabilizerMuscles = exercise.getStabilizerMuscleRussianNames();
        if (stabilizerMuscles != null && !stabilizerMuscles.isEmpty()) {
            setupMuscleGroupChips(chipGroupStabilizerMuscles, stabilizerMuscles);
            chipGroupStabilizerMuscles.setVisibility(View.VISIBLE);
            labelStabilizerMuscles.setVisibility(View.VISIBLE);
        } else {
            chipGroupStabilizerMuscles.setVisibility(View.GONE);
            labelStabilizerMuscles.setVisibility(View.GONE);
        }


        List<String> equipment = exercise.getEquipmentRussianNames();
        if (equipment != null && !equipment.isEmpty()) {
            textEquipment.setText(String.join(", ", equipment));
        } else {
            textEquipment.setText(R.string.no_equipment_required);
        }


        String instructions = exercise.getInstructionsText();
        if (instructions != null && !instructions.isEmpty()) {
            textInstructions.setText(instructions);
            textInstructions.setVisibility(View.VISIBLE);
            labelInstructions.setVisibility(View.VISIBLE);
        } else {
            textInstructions.setText(R.string.no_instructions);
            textInstructions.setVisibility(View.GONE);
            labelInstructions.setVisibility(View.GONE);
        }


        String commonMistakes = exercise.getCommonMistakesText();
        if (commonMistakes != null && !commonMistakes.isEmpty()) {
            textCommonMistakes.setText(commonMistakes);
            textCommonMistakes.setVisibility(View.VISIBLE);
            labelCommonMistakes.setVisibility(View.VISIBLE);
        } else {
            textCommonMistakes.setText(R.string.no_common_mistakes);
            textCommonMistakes.setVisibility(View.GONE);
            labelCommonMistakes.setVisibility(View.GONE);
        }


        String contraindications = exercise.getFormattedContraindicationsText();
        if (contraindications != null && !contraindications.isEmpty()) {
            textContraindications.setText(contraindications);
            textContraindications.setVisibility(View.VISIBLE);
            labelContraindications.setVisibility(View.VISIBLE);
        } else {
            textContraindications.setText(R.string.no_contraindications);
            textContraindications.setVisibility(View.GONE);
            labelContraindications.setVisibility(View.GONE);
        }
    }

    private void setupMuscleGroupChips(ChipGroup chipGroup, List<String> muscleGroups) {
        chipGroup.removeAllViews();

        if (muscleGroups == null || muscleGroups.isEmpty()) {
            Chip chip = new Chip(this);
            chip.setText(R.string.no_muscles_involved);
            chip.setChipBackgroundColorResource(R.color.gray_300);
            chip.setTextColor(getResources().getColor(R.color.gray_700, null));
            chip.setClickable(false);
            chipGroup.addView(chip);
            return;
        }

        for (String muscle : muscleGroups) {
            Chip chip = new Chip(this);
            chip.setText(muscle);
            chip.setChipBackgroundColorResource(R.color.colorAccent);
            chip.setTextColor(getResources().getColor(android.R.color.white, null));
            chip.setClickable(false);
            chipGroup.addView(chip);
        }
    }


    private void postAddExerciseEvent() {
        if (exercise == null || exerciseId == null) {
            Log.e(TAG, "Cannot post AddExerciseEvent, details not loaded");
            return;
        }


        boolean fromAnalytics = getIntent().getBooleanExtra("from_analytics", false);

        if (fromAnalytics) {
            Log.d(TAG, "Нажата кнопка 'Отслеживать', возвращаем результат с ID: " + exerciseId);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("exercise_added_via_details", true);
            resultIntent.putExtra("exercise_id", exerciseId);
            setResult(RESULT_OK, resultIntent);


            Toast.makeText(this, "Упражнение выбрано для отслеживания", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Нажата кнопка 'Добавить в тренировку', отправка AddExerciseEvent для ID: " + exerciseId);

            EventBus.getDefault().post(new AddExerciseEvent(exerciseId, exercise));


            Toast.makeText(this, R.string.exercise_added_to_workout, Toast.LENGTH_SHORT).show();


            Intent resultIntent = new Intent();
            resultIntent.putExtra("exercise_added", true);
            resultIntent.putExtra("exercise_id", exerciseId);
            setResult(RESULT_OK, resultIntent);
        }


        finish();
    }


    private void addExerciseToProgram() {
        if (exercise == null || exercise.getId() == null) {
            Toast.makeText(this, "Ошибка: данные упражнения недоступны", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_exercise_id", exercise.getId());

        resultIntent.putExtra("com.martist.vitamove.EXTRA_IS_REPLACEMENT_MODE",
                getIntent().getBooleanExtra("com.martist.vitamove.EXTRA_IS_REPLACEMENT_MODE", false));

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener());


        if (hideAddButton) {
            if (rootLayout != null) {
                rootLayout.setOnTouchListener((v, event) -> {
                    gestureDetector.onTouchEvent(event);
                    return false;
                });
            }
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (hideAddButton) {
            gestureDetector.onTouchEvent(event);
        }

        return super.dispatchTouchEvent(event);
    }


    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (e1 == null || e2 == null) return false;

                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();


                if (Math.abs(diffX) > Math.abs(diffY)) {

                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD && diffX > 0) {

                        finish();
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                        return true;
                    }
                }
            } catch (Exception exception) {
                Log.e(TAG, "onFling: Error processing gesture: " + exception.getMessage());
            }
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (hideAddButton) {


            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } else {
            super.onBackPressed();
        }
    }
} 