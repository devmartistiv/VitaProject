package com.martist.vitamove.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.BMICalculator;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.core.ui.MainActivity;
import com.martist.vitamove.weight.ui.UserWeightViewModel;
import com.martist.vitamove.weight.ui.adapter.WeightRulerAdapter;


public class SurveyActivity extends BaseActivity {

    private static final int TOTAL_STEPS = 8;
    private static final String PREFS_USER_DATA = "user_data";

    private int currentStep = 1;
    private String name = "";
    private int age = 0;
    private String gender = "";
    private String fitnessGoal = "";
    private float height = 0f;
    private float currentWeight = 0f;
    private float targetWeight = 0f;
    private String fitnessLevel = "";
    private final boolean isMetric = true;

    private LinearProgressIndicator progressIndicator;
    private TextView tvStepCounter;
    private FrameLayout contentContainer;
    private Button btnNext;
    private ImageView btnBack;


    private SupabaseClient supabaseClient;

    private UserWeightViewModel userWeightViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);


        supabaseClient = SupabaseClient.getInstance(
                Constants.SUPABASE_CLIENT_ID,
                Constants.SUPABASE_CLIENT_SECRET
        );

        progressIndicator = findViewById(R.id.progressIndicator);
        tvStepCounter = findViewById(R.id.tvStepCounter);
        contentContainer = findViewById(R.id.contentContainer);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);


        progressIndicator.setMax(TOTAL_STEPS);
        updateStepCounter();


        btnNext.setOnClickListener(v -> nextStep());
        btnBack.setOnClickListener(v -> previousStep());


        userWeightViewModel = new ViewModelProvider(this).get(UserWeightViewModel.class);


        showStep(currentStep);


    }

    private void showStep(int step) {

        progressIndicator.setProgress(step);
        tvStepCounter.setText(getString(R.string.step_counter, step, TOTAL_STEPS));


        contentContainer.removeAllViews();


        btnBack.setVisibility(View.VISIBLE);


        btnNext.setText(step == TOTAL_STEPS ? R.string.finish : R.string.continue_text);


        btnNext.setEnabled(true);
        btnNext.setAlpha(1.0f);

        View view;
        LayoutInflater inflater = getLayoutInflater();

        switch (step) {
            case 1:
                view = inflater.inflate(R.layout.survey_name, contentContainer, false);
                setupNameStep(view);
                break;
            case 2:
                view = inflater.inflate(R.layout.survey_age, contentContainer, false);
                setupAgeStep(view);
                break;
            case 3:
                view = inflater.inflate(R.layout.survey_gender, contentContainer, false);
                setupGenderStep(view);
                break;
            case 4:
                view = inflater.inflate(R.layout.survey_fitness_goal, contentContainer, false);
                setupFitnessGoalStep(view);
                break;
            case 5:
                view = inflater.inflate(R.layout.survey_height_weight, contentContainer, false);
                setupHeightStep(view);
                break;
            case 6:
                view = inflater.inflate(R.layout.survey_current_weight, contentContainer, false);
                setupCurrentWeightStep(view);
                break;
            case 7:
                view = inflater.inflate(R.layout.survey_target_weight, contentContainer, false);
                setupTargetWeightStep(view);
                break;
            case 8:
                view = inflater.inflate(R.layout.survey_fitness_level, contentContainer, false);
                setupFitnessLevelStep(view);
                break;
            default:
                view = new View(this);
                break;
        }


        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(300).start();

        contentContainer.addView(view);
    }

    private void setupNameStep(View view) {
        TextInputEditText etName = view.findViewById(R.id.etName);


        if (!name.isEmpty()) etName.setText(name);


        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                name = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupAgeStep(View view) {
        TextInputEditText etAge = view.findViewById(R.id.etAge);


        if (age > 0) etAge.setText(String.valueOf(age));


        etAge.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    age = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    age = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupGenderStep(View view) {
        RadioGroup radioGroup = view.findViewById(R.id.rgGender);
        RadioButton rbMale = view.findViewById(R.id.rbMale);
        RadioButton rbFemale = view.findViewById(R.id.rbFemale);

        MaterialCardView cardMale = view.findViewById(R.id.cardMale);
        MaterialCardView cardFemale = view.findViewById(R.id.cardFemale);

        ImageView ivMaleSelected = view.findViewById(R.id.ivMaleSelected);
        ImageView ivFemaleSelected = view.findViewById(R.id.ivFemaleSelected);


        cardMale.setStrokeColor(getResources().getColorStateList(R.color.card_stroke_color, null));
        cardFemale.setStrokeColor(getResources().getColorStateList(R.color.card_stroke_color, null));


        if (!gender.isEmpty()) {
            if (gender.equals("Мужчина")) {
                rbMale.setChecked(true);
                cardMale.setChecked(true);
                ivMaleSelected.setVisibility(View.VISIBLE);
            } else if (gender.equals("Женщина")) {
                rbFemale.setChecked(true);
                cardFemale.setChecked(true);
                ivFemaleSelected.setVisibility(View.VISIBLE);
            }
        }


        cardMale.setOnClickListener(v -> {

            rbMale.setChecked(true);
            rbFemale.setChecked(false);


            gender = "Мужчина";


            cardMale.setChecked(true);
            cardFemale.setChecked(false);

            ivMaleSelected.setVisibility(View.VISIBLE);
            ivFemaleSelected.setVisibility(View.GONE);
        });


        cardFemale.setOnClickListener(v -> {

            rbFemale.setChecked(true);
            rbMale.setChecked(false);


            gender = "Женщина";


            cardFemale.setChecked(true);
            cardMale.setChecked(false);

            ivFemaleSelected.setVisibility(View.VISIBLE);
            ivMaleSelected.setVisibility(View.GONE);
        });


        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMale) {
                gender = "Мужчина";
                cardMale.setChecked(true);
                cardFemale.setChecked(false);
                ivMaleSelected.setVisibility(View.VISIBLE);
                ivFemaleSelected.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbFemale) {
                gender = "Женщина";
                cardFemale.setChecked(true);
                cardMale.setChecked(false);
                ivFemaleSelected.setVisibility(View.VISIBLE);
                ivMaleSelected.setVisibility(View.GONE);
            }
        });
    }

    private void setupHeightStep(View view) {
        TextView tvHeightValue = view.findViewById(R.id.tvHeightValue);
        TextView tvHeightUnit = view.findViewById(R.id.tvHeightUnit);
        RecyclerView heightRulerRecyclerView = view.findViewById(R.id.heightRulerRecyclerView);

        if (height <= 0) {
            height = 175f;
        }

        tvHeightValue.setText(String.valueOf((int) height));
        tvHeightUnit.setText("см");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        heightRulerRecyclerView.setLayoutManager(layoutManager);
        heightRulerRecyclerView.setItemAnimator(null);


        heightRulerRecyclerView.setClipToPadding(false);
        heightRulerRecyclerView.post(() -> {
            int padding = heightRulerRecyclerView.getWidth() / 2;
            heightRulerRecyclerView.setPadding(padding, 0, padding, 0);


            int initialPosition = (int) height - 120;
            layoutManager.scrollToPositionWithOffset(initialPosition, 0);
        });

        HeightRulerAdapter adapter = new HeightRulerAdapter(this, true, (heightValue, metric) -> {

        });
        heightRulerRecyclerView.setAdapter(adapter);

        LinearSnapHelper heightSnapHelper = new LinearSnapHelper();
        heightSnapHelper.attachToRecyclerView(heightRulerRecyclerView);


        heightRulerRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastValue = -1;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int centerX = recyclerView.getWidth() / 2;
                int minDistance = Integer.MAX_VALUE;
                int closestPosition = -1;


                for (int i = 0; i < layoutManager.getChildCount(); i++) {
                    View child = layoutManager.getChildAt(i);
                    if (child == null) continue;

                    int childCenterX = (child.getLeft() + child.getRight()) / 2;
                    int distance = Math.abs(centerX - childCenterX);

                    if (distance < minDistance) {
                        minDistance = distance;
                        closestPosition = layoutManager.getPosition(child);
                    }
                }

                if (closestPosition != -1) {
                    int h = 120 + closestPosition;
                    if (h != lastValue) {
                        tvHeightValue.setText(String.valueOf(h));
                        height = (float) h;


                        recyclerView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);

                        lastValue = h;
                    }
                }
            }
        });
    }


    private void setupCurrentWeightStep(View view) {
        TextView tvWeightValue = view.findViewById(R.id.tvWeightValue);
        TextView tvWeightUnit = view.findViewById(R.id.tvWeightUnit);
        RecyclerView weightRulerRecyclerView = view.findViewById(R.id.weightRulerRecyclerView);

        if (currentWeight <= 0) {
            currentWeight = 70f;
        }

        tvWeightValue.setText(String.valueOf((int) currentWeight));
        tvWeightUnit.setText("кг");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        weightRulerRecyclerView.setLayoutManager(layoutManager);
        weightRulerRecyclerView.setItemAnimator(null);


        weightRulerRecyclerView.setClipToPadding(false);

        WeightRulerAdapter adapter = new WeightRulerAdapter(this, true, (weightValue, metric) -> {

        });
        weightRulerRecyclerView.setAdapter(adapter);


        LinearSnapHelper weightSnapHelper = new LinearSnapHelper();
        weightSnapHelper.attachToRecyclerView(weightRulerRecyclerView);


        weightRulerRecyclerView.post(() -> {
            int padding = weightRulerRecyclerView.getWidth() / 2;
            weightRulerRecyclerView.setPadding(padding, 0, padding, 0);


            int initialPosition = (int) currentWeight - 30;
            layoutManager.scrollToPositionWithOffset(initialPosition, 0);
        });


        weightRulerRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastValue = -1;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int scrollOffset = recyclerView.computeHorizontalScrollOffset();

                View firstView = layoutManager.findViewByPosition(layoutManager.findFirstVisibleItemPosition());
                if (firstView != null) {
                    int itemWidth = firstView.getWidth();
                    if (itemWidth > 0) {

                        int position = Math.round((float) scrollOffset / itemWidth);
                        int kg = 30 + position;

                        if (kg != lastValue) {
                            tvWeightValue.setText(String.valueOf(kg));
                            currentWeight = (float) kg;

                            firstView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);


                            lastValue = kg;
                        }
                    }
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (height > 0) {
                        updateBMI(view);
                    }
                }
            }
        });
    }


    private void updateBMI(View view) {
        if (height <= 0 || currentWeight <= 0) return;


        TextView tvBmiValue = view.findViewById(R.id.tvBmiValue);
        TextView tvBmiDescription = view.findViewById(R.id.tvBmiDescription);
        View bmiIndicator = view.findViewById(R.id.bmiIndicator);
        MaterialCardView bmiValueCard = view.findViewById(R.id.bmiValueCard);
        View bmiValuePointer = view.findViewById(R.id.bmiValuePointer);


        ViewGroup indicatorContainer = (ViewGroup) bmiIndicator.getParent();
        ViewGroup cardContainer = (ViewGroup) bmiValueCard.getParent();


        float bmi = BMICalculator.calculateBMI(currentWeight, height);
        BMICalculator.BMICategory category = BMICalculator.getBMICategory(bmi);


        tvBmiValue.setText(String.format("вы - %.1f", bmi));


        String description = String.format("Ваш ИМТ %.1f - %s",
                bmi, category.getDescription().toLowerCase());
        tvBmiDescription.setText(description);


        indicatorContainer.post(() -> {
            float containerWidth = indicatorContainer.getWidth();
            float indicatorWidth = bmiIndicator.getWidth();
            float cardWidth = bmiValueCard.getWidth();
            float cardContainerWidth = cardContainer.getWidth();

            float relativePosition;


            if (bmi < BMICalculator.UNDERWEIGHT_THRESHOLD) {

                relativePosition = bmi / BMICalculator.UNDERWEIGHT_THRESHOLD / 3.0f;
            } else if (bmi < BMICalculator.NORMAL_THRESHOLD) {

                relativePosition = 1.0f / 3.0f + (bmi - BMICalculator.UNDERWEIGHT_THRESHOLD) /
                        (BMICalculator.NORMAL_THRESHOLD - BMICalculator.UNDERWEIGHT_THRESHOLD) / 3.0f;
            } else if (bmi < BMICalculator.OVERWEIGHT_THRESHOLD) {

                relativePosition = 2.0f / 3.0f + (bmi - BMICalculator.NORMAL_THRESHOLD) /
                        (BMICalculator.OVERWEIGHT_THRESHOLD - BMICalculator.NORMAL_THRESHOLD) / 3.0f;
            } else {

                relativePosition = 1.0f;
            }


            relativePosition = Math.max(0, Math.min(1, relativePosition));


            float absolutePosition = containerWidth * relativePosition;


            float leftMargin = absolutePosition - (indicatorWidth / 2);


            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bmiIndicator.getLayoutParams();
            params.leftMargin = (int) leftMargin;
            params.gravity = Gravity.TOP | Gravity.START;
            bmiIndicator.setLayoutParams(params);


            float cardLeftMargin = (cardContainerWidth * relativePosition) - (cardWidth / 2);

            cardLeftMargin = Math.max(0, Math.min(cardContainerWidth - cardWidth, cardLeftMargin));

            FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams) bmiValueCard.getLayoutParams();
            cardParams.leftMargin = (int) cardLeftMargin;
            cardParams.gravity = Gravity.TOP | Gravity.START;
            bmiValueCard.setLayoutParams(cardParams);


            FrameLayout.LayoutParams pointerParams = (FrameLayout.LayoutParams) bmiValuePointer.getLayoutParams();
            pointerParams.leftMargin = (int) (cardLeftMargin + (cardWidth / 2) - (bmiValuePointer.getWidth() / 2));
            pointerParams.gravity = Gravity.TOP | Gravity.START;
            bmiValuePointer.setLayoutParams(pointerParams);
        });
    }

    private void setupTargetWeightStep(View view) {

        TextView tvTargetWeightValue = view.findViewById(R.id.tvTargetWeightValue);
        TextView tvTargetWeightUnit = view.findViewById(R.id.tvTargetWeightUnit);
        TextView tvWeightDifference = view.findViewById(R.id.tvWeightDifference);
        TextView tvWeightGoalError = view.findViewById(R.id.tvWeightGoalError);
        RecyclerView targetWeightRulerRecyclerView = view.findViewById(R.id.targetWeightRulerRecyclerView);

        if (targetWeight <= 0) {
            targetWeight = 65f;
        }

        tvTargetWeightValue.setText(String.valueOf((int) targetWeight));
        tvTargetWeightUnit.setText("кг");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        targetWeightRulerRecyclerView.setLayoutManager(layoutManager);
        targetWeightRulerRecyclerView.setItemAnimator(null);


        targetWeightRulerRecyclerView.setClipToPadding(false);
        targetWeightRulerRecyclerView.post(() -> {
            int padding = targetWeightRulerRecyclerView.getWidth() / 2;
            targetWeightRulerRecyclerView.setPadding(padding, 0, padding, 0);

            int initialPosition = (int) targetWeight - 30;
            layoutManager.scrollToPositionWithOffset(initialPosition, 0);
        });

        WeightRulerAdapter adapter = new WeightRulerAdapter(this, true, (weightValue, metric) -> {

        });
        targetWeightRulerRecyclerView.setAdapter(adapter);

        LinearSnapHelper targetWeightSnapHelper = new LinearSnapHelper();
        targetWeightSnapHelper.attachToRecyclerView(targetWeightRulerRecyclerView);

        targetWeightRulerRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastValue = -1;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);


                int scrollOffset = recyclerView.computeHorizontalScrollOffset();
                View firstView = layoutManager.findViewByPosition(layoutManager.findFirstVisibleItemPosition());

                if (firstView != null) {
                    int itemWidth = firstView.getWidth();
                    if (itemWidth > 0) {
                        int position = Math.round((float) scrollOffset / itemWidth);
                        int kg = 30 + position;

                        if (kg != lastValue) {
                            tvTargetWeightValue.setText(String.valueOf(kg));
                            targetWeight = (float) kg;


                            firstView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                            lastValue = kg;
                        }
                    }
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    updateWeightDifference(tvWeightDifference);
                    updateWeightGoalValidation(tvWeightGoalError);
                }
            }
        });


        updateWeightDifference(tvWeightDifference);
        updateWeightGoalValidation(tvWeightGoalError);
    }

    private void updateWeightDifference(TextView tvWeightDifference) {
        float weightDiff = targetWeight - currentWeight;

        if (Math.abs(weightDiff) < 0.1) {
            tvWeightDifference.setText("Целевой вес совпадает с текущим");
        } else if (weightDiff < 0) {
            tvWeightDifference.setText("Вам нужно сбросить " + Math.abs(Math.round(weightDiff)) + " кг");
        } else {
            tvWeightDifference.setText("Вам нужно набрать " + Math.round(weightDiff) + " кг");
        }
    }

    private void updateWeightGoalValidation(TextView tvWeightGoalError) {
        if (tvWeightGoalError == null) {
            Log.e("SurveyActivity", "tvWeightGoalError is null!");
            return;
        }

        boolean isValid = true;
        String errorMessage = "";

        Log.d("SurveyActivity", "Валидация: цель=" + fitnessGoal + ", текущий=" + currentWeight + ", целевой=" + targetWeight);


        if (fitnessGoal.equals("weight_loss") && targetWeight >= currentWeight) {
            isValid = false;
            errorMessage = "⚠️ Для похудения целевой вес должен быть меньше текущего (" + (int) currentWeight + " кг)";
        } else if (fitnessGoal.equals("muscle_gain") && targetWeight <= currentWeight) {
            isValid = false;
            errorMessage = "⚠️ Для набора массы целевой вес должен быть больше текущего (" + (int) currentWeight + " кг)";
        }


        if (isValid) {
            tvWeightGoalError.setVisibility(View.GONE);
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
            Log.d("SurveyActivity", "Валидация пройдена, кнопка активна");
        } else {
            tvWeightGoalError.setText(errorMessage);
            tvWeightGoalError.setVisibility(View.VISIBLE);
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
            Log.d("SurveyActivity", "Валидация не пройдена: " + errorMessage + ", кнопка заблокирована");
        }
    }

    private void setupFitnessGoalStep(View view) {
        RadioGroup radioGroup = view.findViewById(R.id.rgFitnessGoals);
        RadioButton rbWeightLoss = view.findViewById(R.id.rbWeightLoss);
        RadioButton rbMuscleGain = view.findViewById(R.id.rbMuscleGain);
        RadioButton rbEndurance = view.findViewById(R.id.rbEndurance);
        RadioButton rbGeneralFitness = view.findViewById(R.id.rbGeneralFitness);

        MaterialCardView cardWeightLoss = view.findViewById(R.id.cardWeightLoss);
        MaterialCardView cardMuscleGain = view.findViewById(R.id.cardMuscleGain);
        MaterialCardView cardEndurance = view.findViewById(R.id.cardEndurance);
        MaterialCardView cardGeneralFitness = view.findViewById(R.id.cardGeneralFitness);

        ImageView ivWeightLossSelected = view.findViewById(R.id.ivWeightLossSelected);
        ImageView ivMuscleGainSelected = view.findViewById(R.id.ivMuscleGainSelected);
        ImageView ivEnduranceSelected = view.findViewById(R.id.ivEnduranceSelected);
        ImageView ivGeneralFitnessSelected = view.findViewById(R.id.ivGeneralFitnessSelected);


        cardWeightLoss.setStrokeColor(getResources().getColorStateList(R.color.card_stroke_color, null));
        cardMuscleGain.setStrokeColor(getResources().getColorStateList(R.color.card_stroke_color, null));
        cardEndurance.setStrokeColor(getResources().getColorStateList(R.color.card_stroke_color, null));
        cardGeneralFitness.setStrokeColor(getResources().getColorStateList(R.color.card_stroke_color, null));


        if (!fitnessGoal.isEmpty()) {
            switch (fitnessGoal) {
                case "weight_loss":
                    rbWeightLoss.setChecked(true);
                    cardWeightLoss.setChecked(true);
                    ivWeightLossSelected.setVisibility(View.VISIBLE);
                    break;
                case "muscle_gain":
                    rbMuscleGain.setChecked(true);
                    cardMuscleGain.setChecked(true);
                    ivMuscleGainSelected.setVisibility(View.VISIBLE);
                    break;
                case "endurance":
                    rbEndurance.setChecked(true);
                    cardEndurance.setChecked(true);
                    ivEnduranceSelected.setVisibility(View.VISIBLE);
                    break;
                case "general_fitness":
                    rbGeneralFitness.setChecked(true);
                    cardGeneralFitness.setChecked(true);
                    ivGeneralFitnessSelected.setVisibility(View.VISIBLE);
                    break;
            }
        }


        Runnable clearSelections = () -> {

            rbWeightLoss.setChecked(false);
            rbMuscleGain.setChecked(false);
            rbEndurance.setChecked(false);
            rbGeneralFitness.setChecked(false);


            ivWeightLossSelected.setVisibility(View.GONE);
            ivMuscleGainSelected.setVisibility(View.GONE);
            ivEnduranceSelected.setVisibility(View.GONE);
            ivGeneralFitnessSelected.setVisibility(View.GONE);


            cardWeightLoss.setChecked(false);
            cardMuscleGain.setChecked(false);
            cardEndurance.setChecked(false);
            cardGeneralFitness.setChecked(false);
        };


        cardWeightLoss.setOnClickListener(v -> {
            clearSelections.run();
            rbWeightLoss.setChecked(true);
            fitnessGoal = "weight_loss";
            ivWeightLossSelected.setVisibility(View.VISIBLE);
            cardWeightLoss.setChecked(true);
        });


        cardMuscleGain.setOnClickListener(v -> {
            clearSelections.run();
            rbMuscleGain.setChecked(true);
            fitnessGoal = "muscle_gain";
            ivMuscleGainSelected.setVisibility(View.VISIBLE);
            cardMuscleGain.setChecked(true);
        });


        cardEndurance.setOnClickListener(v -> {
            clearSelections.run();
            rbEndurance.setChecked(true);
            fitnessGoal = "endurance";
            ivEnduranceSelected.setVisibility(View.VISIBLE);
            cardEndurance.setChecked(true);
        });


        cardGeneralFitness.setOnClickListener(v -> {
            clearSelections.run();
            rbGeneralFitness.setChecked(true);
            fitnessGoal = "general_fitness";
            ivGeneralFitnessSelected.setVisibility(View.VISIBLE);
            cardGeneralFitness.setChecked(true);
        });


        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbWeightLoss) {
                fitnessGoal = "weight_loss";
            } else if (checkedId == R.id.rbMuscleGain) {
                fitnessGoal = "muscle_gain";
            } else if (checkedId == R.id.rbEndurance) {
                fitnessGoal = "endurance";
            } else if (checkedId == R.id.rbGeneralFitness) {
                fitnessGoal = "general_fitness";
            }
        });
    }

    private void setupFitnessLevelStep(View view) {
        RadioGroup radioGroup = view.findViewById(R.id.rgFitnessLevel);
        RadioButton rbBeginner = view.findViewById(R.id.rbBeginner);
        RadioButton rbIntermediate = view.findViewById(R.id.rbIntermediate);
        RadioButton rbAdvanced = view.findViewById(R.id.rbAdvanced);
        RadioButton rbExpert = view.findViewById(R.id.rbExpert);

        MaterialCardView cardBeginner = view.findViewById(R.id.cardBeginner);
        MaterialCardView cardIntermediate = view.findViewById(R.id.cardIntermediate);
        MaterialCardView cardAdvanced = view.findViewById(R.id.cardAdvanced);
        MaterialCardView cardExpert = view.findViewById(R.id.cardExpert);

        ImageView ivBeginnerSelected = view.findViewById(R.id.ivBeginnerSelected);
        ImageView ivIntermediateSelected = view.findViewById(R.id.ivIntermediateSelected);
        ImageView ivAdvancedSelected = view.findViewById(R.id.ivAdvancedSelected);
        ImageView ivExpertSelected = view.findViewById(R.id.ivExpertSelected);


        if (!fitnessLevel.isEmpty()) {
            switch (fitnessLevel) {
                case "beginner":
                    rbBeginner.setChecked(true);
                    cardBeginner.setChecked(true);
                    ivBeginnerSelected.setVisibility(View.VISIBLE);
                    break;
                case "intermediate":
                    rbIntermediate.setChecked(true);
                    cardIntermediate.setChecked(true);
                    ivIntermediateSelected.setVisibility(View.VISIBLE);
                    break;
                case "advanced":
                    rbAdvanced.setChecked(true);
                    cardAdvanced.setChecked(true);
                    ivAdvancedSelected.setVisibility(View.VISIBLE);
                    break;
                case "expert":
                    rbExpert.setChecked(true);
                    cardExpert.setChecked(true);
                    ivExpertSelected.setVisibility(View.VISIBLE);
                    break;
            }
        }


        cardBeginner.setOnClickListener(v -> {

            rbBeginner.setChecked(true);
            rbIntermediate.setChecked(false);
            rbAdvanced.setChecked(false);
            rbExpert.setChecked(false);


            fitnessLevel = "beginner";


            ivBeginnerSelected.setVisibility(View.VISIBLE);
            ivIntermediateSelected.setVisibility(View.GONE);
            ivAdvancedSelected.setVisibility(View.GONE);
            ivExpertSelected.setVisibility(View.GONE);


            cardBeginner.setChecked(true);
            cardIntermediate.setChecked(false);
            cardAdvanced.setChecked(false);
            cardExpert.setChecked(false);
        });


        cardIntermediate.setOnClickListener(v -> {

            rbIntermediate.setChecked(true);
            rbBeginner.setChecked(false);
            rbAdvanced.setChecked(false);
            rbExpert.setChecked(false);


            fitnessLevel = "intermediate";


            ivIntermediateSelected.setVisibility(View.VISIBLE);
            ivBeginnerSelected.setVisibility(View.GONE);
            ivAdvancedSelected.setVisibility(View.GONE);
            ivExpertSelected.setVisibility(View.GONE);


            cardIntermediate.setChecked(true);
            cardBeginner.setChecked(false);
            cardAdvanced.setChecked(false);
            cardExpert.setChecked(false);
        });


        cardAdvanced.setOnClickListener(v -> {

            rbAdvanced.setChecked(true);
            rbBeginner.setChecked(false);
            rbIntermediate.setChecked(false);
            rbExpert.setChecked(false);


            fitnessLevel = "advanced";


            ivAdvancedSelected.setVisibility(View.VISIBLE);
            ivBeginnerSelected.setVisibility(View.GONE);
            ivIntermediateSelected.setVisibility(View.GONE);
            ivExpertSelected.setVisibility(View.GONE);


            cardAdvanced.setChecked(true);
            cardBeginner.setChecked(false);
            cardIntermediate.setChecked(false);
            cardExpert.setChecked(false);
        });


        cardExpert.setOnClickListener(v -> {

            rbExpert.setChecked(true);
            rbBeginner.setChecked(false);
            rbIntermediate.setChecked(false);
            rbAdvanced.setChecked(false);


            fitnessLevel = "expert";


            ivExpertSelected.setVisibility(View.VISIBLE);
            ivBeginnerSelected.setVisibility(View.GONE);
            ivIntermediateSelected.setVisibility(View.GONE);
            ivAdvancedSelected.setVisibility(View.GONE);


            cardExpert.setChecked(true);
            cardBeginner.setChecked(false);
            cardIntermediate.setChecked(false);
            cardAdvanced.setChecked(false);
        });


        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbBeginner) {
                fitnessLevel = "beginner";
            } else if (checkedId == R.id.rbIntermediate) {
                fitnessLevel = "intermediate";
            } else if (checkedId == R.id.rbAdvanced) {
                fitnessLevel = "advanced";
            } else if (checkedId == R.id.rbExpert) {
                fitnessLevel = "expert";
            }
        });
    }

    private void nextStep() {
        if (validateCurrentStep()) {
            if (currentStep < TOTAL_STEPS) {

                contentContainer.animate()
                        .alpha(0f)
                        .translationX(-contentContainer.getWidth())
                        .setDuration(250)
                        .withEndAction(() -> {

                            currentStep++;
                            showStep(currentStep);
                            if (currentStep == 3) {
                                setupCardsBeforeAnimation(new int[]{R.id.cardMale, R.id.cardFemale});
                            }
                            if (currentStep == 4) {
                                setupCardsBeforeAnimation(new int[]{R.id.cardWeightLoss, R.id.cardMuscleGain, R.id.cardEndurance, R.id.cardGeneralFitness});
                            }
                            if (currentStep == 8) {
                                setupCardsBeforeAnimation(new int[]{R.id.cardBeginner, R.id.cardIntermediate, R.id.cardAdvanced, R.id.cardExpert});
                            }

                            contentContainer.setTranslationX(contentContainer.getWidth());
                            contentContainer.animate()
                                    .alpha(1f)
                                    .translationX(0)
                                    .setDuration(250)
                                    .withEndAction(() -> {
                                        if (currentStep == 3) {
                                            animateActivityCards(new int[]{R.id.cardMale, R.id.cardFemale});
                                        }
                                        if (currentStep == 4) {
                                            animateActivityCards(new int[]{R.id.cardWeightLoss, R.id.cardMuscleGain, R.id.cardEndurance, R.id.cardGeneralFitness});
                                        }
                                        if (currentStep == 8) {
                                            animateActivityCards(new int[]{R.id.cardBeginner, R.id.cardIntermediate, R.id.cardAdvanced, R.id.cardExpert});
                                        }
                                    })
                                    .start();
                        })
                        .start();
            } else {

                saveUserData();


                boolean fromGoogleAuth = getIntent().getBooleanExtra("fromGoogleAuth", false);

                if (fromGoogleAuth) {

                    Log.d("SurveyActivity", "Завершение Google регистрации - переход в MainActivity");


                    SharedPreferences prefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("isFirstRun", false).apply();

                    Toast.makeText(this, "Профиль настроен! Добро пожаловать в VitaMove!", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {

                    startActivity(new Intent(this, RegisterActivity.class));

                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                }
            }
        } else {

            String errorMessage;
            switch (currentStep) {
                case 1:
                    errorMessage = "Пожалуйста, введите ваше имя";
                    break;
                case 2:
                    errorMessage = "Введите корректный возраст (от 1 до 120 лет)";
                    break;
                case 3:
                    errorMessage = "Пожалуйста, выберите ваш пол";
                    break;
                case 4:
                    errorMessage = "Пожалуйста, выберите вашу фитнес-цель";
                    break;
                case 5:
                    errorMessage = "Введите корректный рост (в см)";
                    break;
                case 6:
                    errorMessage = "Введите корректный текущий вес (в кг)";
                    break;
                case 7:
                    if (fitnessGoal.equals("weight_loss") && targetWeight >= currentWeight) {
                        errorMessage = "Для похудения целевой вес должен быть меньше текущего";
                    } else if (fitnessGoal.equals("muscle_gain") && targetWeight <= currentWeight) {
                        errorMessage = "Для набора массы целевой вес должен быть больше текущего";
                    } else {
                        errorMessage = "Введите корректный целевой вес (в кг)";
                    }
                    break;
                case 8:
                    errorMessage = "Пожалуйста, выберите ваш уровень физической активности";
                    break;
                default:
                    errorMessage = "Заполните все поля корректно";
                    break;
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1:
                return validateName();
            case 2:
                return validateAge();
            case 3:
                return validateGender();
            case 4:
                return validateFitnessGoal();
            case 5:
                return validateHeight();
            case 6:
                return validateCurrentWeight();
            case 7:
                return validateTargetWeight();
            case 8:
                return validateFitnessLevel();
            default:
                return true;
        }
    }

    private boolean validateName() {
        return !name.trim().isEmpty();
    }

    private boolean validateAge() {
        return age > 0 && age <= 120;
    }

    private boolean validateGender() {
        return !gender.isEmpty();
    }

    private boolean validateFitnessGoal() {
        return !fitnessGoal.isEmpty();
    }

    private boolean validateHeight() {
        return height > 0 && height < 250;
    }

    private boolean validateCurrentWeight() {
        return currentWeight > 0 && currentWeight < 500;
    }

    private boolean validateTargetWeight() {

        if (targetWeight <= 0 || targetWeight >= 500) {
            return false;
        }


        if (fitnessGoal.equals("weight_loss") && targetWeight >= currentWeight) {
            return false;
        }

        if (fitnessGoal.equals("muscle_gain") && targetWeight <= currentWeight) {
            return false;
        }

        return true;
    }

    private boolean validateFitnessLevel() {
        return !fitnessLevel.isEmpty();
    }

    private void previousStep() {
        if (currentStep > 1) {

            contentContainer.animate()
                    .alpha(0f)
                    .translationX(contentContainer.getWidth())
                    .setDuration(250)
                    .withEndAction(() -> {

                        currentStep--;
                        showStep(currentStep);
                        if (currentStep == 3) {
                            setupCardsBeforeAnimation(new int[]{R.id.cardMale, R.id.cardFemale});
                        }
                        if (currentStep == 4) {
                            setupCardsBeforeAnimation(new int[]{R.id.cardWeightLoss, R.id.cardMuscleGain, R.id.cardEndurance, R.id.cardGeneralFitness});
                        }
                        if (currentStep == 8) {
                            setupCardsBeforeAnimation(new int[]{R.id.cardBeginner, R.id.cardIntermediate, R.id.cardAdvanced, R.id.cardExpert});
                        }

                        contentContainer.setTranslationX(-contentContainer.getWidth());
                        contentContainer.animate()
                                .alpha(1f)
                                .translationX(0)
                                .setDuration(250)
                                .withEndAction(() -> {
                                    if (currentStep == 3) {
                                        animateActivityCards(new int[]{R.id.cardMale, R.id.cardFemale});
                                    }
                                    if (currentStep == 4) {
                                        animateActivityCards(new int[]{R.id.cardWeightLoss, R.id.cardMuscleGain, R.id.cardEndurance, R.id.cardGeneralFitness});
                                    }
                                    if (currentStep == 8) {
                                        animateActivityCards(new int[]{R.id.cardBeginner, R.id.cardIntermediate, R.id.cardAdvanced, R.id.cardExpert});
                                    }
                                })
                                .start();
                    })
                    .start();
        } else {

            Intent intent = new Intent(this, OnboardingActivity.class);
            startActivity(intent);

            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        }
    }

    private void saveUserData() {

        SharedPreferences prefs = getSharedPreferences(PREFS_USER_DATA, MODE_PRIVATE);
        SharedPreferences.Editor userDataEditor = prefs.edit();


        userDataEditor.putString("name", name);
        userDataEditor.putInt("age", age);
        userDataEditor.putString("gender", gender);
        userDataEditor.putString("fitness_goal", fitnessGoal);
        userDataEditor.putFloat("height", height);
        userDataEditor.putFloat("current_weight", currentWeight);
        userDataEditor.putFloat("target_weight", targetWeight);


        userDataEditor.putString("user_fitness_level", fitnessLevel);


        float bmi = 0;
        if (height > 0 && currentWeight > 0) {
            bmi = currentWeight / ((height / 100) * (height / 100));
        }

        userDataEditor.putFloat("bmi", bmi);
        userDataEditor.putBoolean("is_metric", isMetric);

        userDataEditor.apply();


        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor appPrefsEditor = appPrefs.edit();
        appPrefsEditor.putString("fitness_goal", fitnessGoal);
        appPrefsEditor.putString("user_fitness_level", fitnessLevel);
        appPrefsEditor.apply();


        Log.d("SurveyActivity", "Сохранены данные пользователя, фитнес-цель: " + fitnessGoal + ", уровень активности: " + fitnessLevel);


        SharedPreferences vitaPrefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        String userId = vitaPrefs.getString("userId", null);
        if (userId != null) {

            new Thread(() -> {
                try {

                    String accessToken = vitaPrefs.getString("accessToken", null);
                    String refreshToken = vitaPrefs.getString("refreshToken", null);

                    if (accessToken != null && refreshToken != null) {
                        Log.d("SurveyActivity", "Токены найдены, начинаем обновление профиля в Supabase");

                        supabaseClient.setUserToken(accessToken);
                        supabaseClient.setRefreshToken(refreshToken);

                        Log.d("SurveyActivity", "Вызов updateUserProfile для пользователя: " + userId);
                        Log.d("SurveyActivity", "Данные: name=" + name + ", age=" + age + ", gender=" + gender +
                                ", fitnessGoal=" + fitnessGoal + ", height=" + height + ", currentWeight=" + currentWeight);

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


                        runOnUiThread(() -> {
                            if (success) {
                                Log.d("SurveyActivity", "Профиль пользователя успешно обновлен в Supabase");

                                SharedPreferences.Editor syncEditor = prefs.edit();
                                syncEditor.putBoolean("user_data_synced", true);
                                syncEditor.apply();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("SurveyActivity", "Ошибка при обновлении профиля пользователя: " + e.getMessage(), e);
                }
            }).start();
        }
    }


    private void updateStepCounter() {
        progressIndicator.setProgress(currentStep);
        tvStepCounter.setText("Шаг " + currentStep + " из " + TOTAL_STEPS);
    }

    private void animateActivityCards(int[] cardIds) {
        long delay = 50;

        for (int id : cardIds) {
            View card = findViewById(id);
            if (card != null) {
                card.setAlpha(0f);
                card.setTranslationY(50f);
                card.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setStartDelay(delay)
                        .start();
                delay += 50;
            }
        }
    }

    private void setupCardsBeforeAnimation(int[] cardIds) {
        for (int id : cardIds) {
            View card = findViewById(id);
            if (card != null) {
                card.setAlpha(0f);
                card.setTranslationY(50f);
            }
        }
    }
} 