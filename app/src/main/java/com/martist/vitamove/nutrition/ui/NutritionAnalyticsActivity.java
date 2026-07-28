package com.martist.vitamove.nutrition.ui;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.martist.vitamove.R;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.core.ui.views.CustomCalendarDialog;
import com.martist.vitamove.databinding.ActivityNutritionAnalyticsBinding;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.domain.events.MealsLoadedEvent;
import com.martist.vitamove.nutrition.domain.events.TrackedNutrientsChangedEvent;
import com.martist.vitamove.nutrition.ui.model.Meal;
import com.martist.vitamove.nutrition.ui.model.NutrientType;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class NutritionAnalyticsActivity extends BaseActivity {
    private FoodManager foodManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
    private ActivityNutritionAnalyticsBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNutritionAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        foodManager = FoodManager.getInstance(this);
        setupToolbar();
        updateNutritionData();
    }

    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    private void setupToolbar() {
        binding.backButton.setOnClickListener(v -> onBackPressed());
        binding.settingsButton.setOnClickListener(v -> showNutrientSelectionBottomSheet());
        binding.datePrevButton.setOnClickListener(v -> changeDate(-1));
        binding.dateNextButton.setOnClickListener(v -> changeDate(1));
        binding.dateText.setClickable(true);
        binding.dateText.setFocusable(true);
        binding.dateText.setBackground(getResources().getDrawable(R.drawable.date_text_background));
        binding.dateText.setCompoundDrawablesWithIntrinsicBounds(
                getResources().getDrawable(R.drawable.ic_calendar), null, null, null);
        binding.dateText.setCompoundDrawablePadding(8);
        binding.dateText.setOnClickListener(v -> showDatePickerDialog());
    }


    private void updateNutritionData() {

        Date selectedDate = foodManager.getSelectedDateForView();
        binding.dateText.setText(dateFormat.format(selectedDate));


        updateMacronutrients();
        updateNutrients();
    }

    private void updateMacronutrients() {

        float totalCalories = (float) foodManager.getTotalCaloriesForSelectedDate();
        float targetCalories = foodManager.getDailyNorm("calories");
        float totalProteins = 0;
        float totalFats = 0;
        float totalCarbs = 0;


        Meal breakfast = foodManager.getMeal("breakfast");
        Meal lunch = foodManager.getMeal("lunch");
        Meal dinner = foodManager.getMeal("dinner");
        Meal snack = foodManager.getMeal("snack");


        if (breakfast != null) {
            totalProteins += breakfast.getTotalProteins();
            totalFats += breakfast.getTotalFats();
            totalCarbs += breakfast.getTotalCarbs();
        }
        if (lunch != null) {
            totalProteins += lunch.getTotalProteins();
            totalFats += lunch.getTotalFats();
            totalCarbs += lunch.getTotalCarbs();
        }
        if (dinner != null) {
            totalProteins += dinner.getTotalProteins();
            totalFats += dinner.getTotalFats();
            totalCarbs += dinner.getTotalCarbs();
        }
        if (snack != null) {
            totalProteins += snack.getTotalProteins();
            totalFats += snack.getTotalFats();
            totalCarbs += snack.getTotalCarbs();
        }


        float targetProteins = foodManager.getDailyNorm("proteins");
        float targetFats = foodManager.getDailyNorm("fats");
        float targetCarbs = foodManager.getDailyNorm("carbs");


        int caloriesPercent = (int) ((totalCalories / targetCalories) * 100);


        binding.caloriesText.setText(String.format(Locale.getDefault(), "%.0f", totalCalories));


        binding.caloriesTarget.setText(String.format(Locale.getDefault(), "из %.0f ккал (%d%%)", targetCalories, caloriesPercent));


        int caloriesProgress = Math.min((int) ((totalCalories / targetCalories) * 100), 150);

        int currentPercentCall = binding.circularCaloriesProgress.getProgress();

        ValueAnimator valueAnimator = ValueAnimator.ofInt(currentPercentCall, caloriesProgress);
        valueAnimator.setDuration(1000);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            int animationValue = (int) animation.getAnimatedValue();
            binding.circularCaloriesProgress.setProgress(animationValue);
        });
        valueAnimator.start();


        int proteinsPercent = (int) ((totalProteins / targetProteins) * 100);
        int fatsPercent = (int) ((totalFats / targetFats) * 100);
        int carbsPercent = (int) ((totalCarbs / targetCarbs) * 100);


        binding.proteinsValue.setText(String.format(Locale.getDefault(), "%.1f/%.0fг", totalProteins, targetProteins));
        int finalProgressProtein = Math.min(proteinsPercent, 100);
        int currentPercentProtein = binding.proteinsProgress.getProgress();

        ValueAnimator valueAnimatorProtein = ValueAnimator.ofInt(currentPercentProtein, finalProgressProtein);
        valueAnimatorProtein.setDuration(1000);
        valueAnimatorProtein.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimatorProtein.addUpdateListener(animation -> {
            int animationValue = (int) animation.getAnimatedValue();
            binding.proteinsProgress.setProgress(animationValue);
        });
        valueAnimatorProtein.start();


        binding.fatsValue.setText(String.format(Locale.getDefault(), "%.1f/%.0fг", totalFats, targetFats));
        int finalProgressFats = Math.min(fatsPercent, 100);
        int currentPercentFats = binding.fatsProgress.getProgress();

        ValueAnimator valueAnimatorFats = ValueAnimator.ofInt(currentPercentFats, finalProgressFats);
        valueAnimatorFats.setDuration(1000);
        valueAnimatorFats.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimatorFats.addUpdateListener(animation -> {
            int animationValue = (int) animation.getAnimatedValue();
            binding.fatsProgress.setProgress(animationValue);
        });
        valueAnimatorFats.start();

        binding.carbsValue.setText(String.format(Locale.getDefault(), "%.1f/%.0fг", totalCarbs, targetCarbs));
        int finalProgressCarbs = Math.min(carbsPercent, 100);
        int currentPercentCarbs = binding.fatsProgress.getProgress();

        ValueAnimator valueAnimatorCarbs = ValueAnimator.ofInt(currentPercentCarbs, finalProgressCarbs);
        valueAnimatorCarbs.setDuration(1000);
        valueAnimatorCarbs.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimatorCarbs.addUpdateListener(animation -> {
            int animationValue = (int) animation.getAnimatedValue();
            binding.carbsProgress.setProgress(animationValue);
        });
        valueAnimatorCarbs.start();
    }

    private void updateNutrients() {

        binding.vitaminsContainer.removeAllViews();


        Date selectedDate = foodManager.getSelectedDateForView();
        String selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate);
        Map<NutrientType, Float> consumedNutrients = foodManager.getConsumedNutrients(selectedDateStr);
        for (NutrientType type : NutrientType.values()) {
            addVitaminItem(binding.vitaminsContainer, type.getLocalizedName(), consumedNutrients.getOrDefault(type, 0f), type.getPersonalizedNorm(), type.getUnit());
        }
    }


    private void addVitaminItem(LinearLayout container, String name, float value, float norm, String unit) {
        if (value <= 0) return;

        View itemView = LayoutInflater.from(this).inflate(R.layout.item_nutrient, container, false);

        TextView nameText = itemView.findViewById(R.id.nutrient_name);
        TextView valueText = itemView.findViewById(R.id.nutrient_value);
        LinearProgressIndicator progressBar = itemView.findViewById(R.id.nutrient_progress);
        TextView percentText = itemView.findViewById(R.id.nutrient_percent);

        nameText.setText(name);
        valueText.setText(String.format(Locale.getDefault(), "%.1f", value));

        int percentOfNorm = (int) ((value / norm) * 100);

        progressBar.setMax(100);
        progressBar.setProgress(Math.min(percentOfNorm, 100));

        percentText.setText(String.format(Locale.getDefault(), "/%d%s", Math.round(norm), unit));


        int colorId;
        if (percentOfNorm < 30) {
            colorId = R.color.colorDanger;
        } else if (percentOfNorm < 70) {
            colorId = R.color.colorWarning;
        } else {
            colorId = R.color.colorSuccess;
        }

        int color = ContextCompat.getColor(this, colorId);

        progressBar.setIndicatorColor(color);

        container.addView(itemView);
    }

    private void showNutrientSelectionBottomSheet() {
        NutrientSelectionBottomSheet bottomSheet = new NutrientSelectionBottomSheet();
        bottomSheet.show(getSupportFragmentManager(), "NutrientSelectionBottomSheet");
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTrackedNutrientsChanged(TrackedNutrientsChangedEvent event) {

        updateNutritionData();
    }


    private void changeDate(int daysDelta) {
        Date currentDate = foodManager.getSelectedDateForView();
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(java.util.Calendar.DAY_OF_MONTH, daysDelta);

        Date newDate = calendar.getTime();
        foodManager.setSelectedDateForView(newDate);
        updateNutritionData();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMealsLoaded(MealsLoadedEvent event) {

        updateNutritionData();
    }


    private void showDatePickerDialog() {
        Date currentSelectedDate = foodManager.getSelectedDateForView();
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentSelectedDate);
        CalendarDay initialDay = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        CustomCalendarDialog dialog = new CustomCalendarDialog(initialDay, date -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(date.getYear(), date.getMonth() - 1, date.getDay());
            Date selectedDate = selectedCal.getTime();
            foodManager.setSelectedDateForView(selectedDate);
            updateNutritionData();
        });
        dialog.show(getSupportFragmentManager(), "CustomCalendarDialog");
    }
} 