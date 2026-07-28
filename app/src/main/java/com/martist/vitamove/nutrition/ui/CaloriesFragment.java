package com.martist.vitamove.nutrition.ui;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.views.CustomCalendarDialog;
import com.martist.vitamove.databinding.FragmentCaloriesBinding;
import com.martist.vitamove.nutrition.data.managers.CaloriesManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.nutrition.ui.adapter.MealFoodsAdapter;
import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.nutrition.ui.model.Meal;
import com.martist.vitamove.nutrition.ui.model.NutrientType;
import com.martist.vitamove.nutrition.ui.model.SelectedFood;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import org.greenrobot.eventbus.EventBus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CaloriesFragment extends Fragment {
    private static final String TAG = "CaloriesFragment";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());


    private final Map<String, Meal> mealMap = new HashMap<>();
    private ActivityResultLauncher<Intent> foodSelectionLauncher;
    private FoodManager foodManager;

    private final Map<String, MealCard> mealCards = new HashMap<>();

    View view;
    private final View[] dayViews = new View[7];
    private String selectedDate;
    private Calendar selectedWeekStart;
    private TextView selectedDateLabel;
    private float targetCalories = 2000f;
    private float targetProteins = 90f;
    private float targetFats = 70f;
    private float targetCarbs = 250f;

    private CaloriesManager caloriesManager;
    private List<NutrientType> trackedNutrients;
    private FragmentCaloriesBinding binding;


    private final BroadcastReceiver caloriesUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshNutrientsNorms();
            updateCaloriesCard();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().setBackgroundDrawable(null);
        foodManager = new FoodManager(requireContext());
        caloriesManager = CaloriesManager.getInstance(requireContext());
        foodSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String mealType = data.getStringExtra("MEAL_TYPE");
                        SelectedFood selectedFood = data.getParcelableExtra("SELECTED_FOOD");

                        if (selectedFood != null && mealType != null) {
                            Meal meal = mealMap.get(mealType);
                            if (meal != null) {
                                meal.addFood(selectedFood);
                                updateMealCard(mealType);
                                updateCaloriesCard();
                            }
                        }
                    }
                });

        selectedDate = dateFormat.format(new Date());


        trackedNutrients = getTrackedNutrients();
    }

    List<NutrientType> getTrackedNutrients() {
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("app_prefs", MODE_PRIVATE);
            return prefs.getStringSet("track_nutrients", Collections.emptySet()).stream().map(NutrientType::valueOf).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCaloriesBinding.inflate(inflater, container, false);
        view = binding.getRoot();
        view.setBackground(null);

        foodManager = FoodManager.getInstance(requireContext());
        caloriesManager = CaloriesManager.getInstance(requireContext());


        targetCalories = foodManager.getDailyNorm("calories");
        targetProteins = foodManager.getDailyNorm("proteins");
        targetFats = foodManager.getDailyNorm("fats");
        targetCarbs = foodManager.getDailyNorm("carbs");


        for (int i = 0; i < 6; i++) {
            String day = "day_" + i;
            int resourceId = getResources().getIdentifier(day, "id", Objects.requireNonNull(getActivity()).getPackageName());
            if (resourceId != 0)
                dayViews[i] = view.findViewById(resourceId);
        }

        binding.calendarButton.setOnClickListener(v -> showDatePicker());

        binding.voiceInputButton.setOnClickListener(v -> showVoiceInputDialog());

        binding.quickAddButton.setOnClickListener(v -> showQuickAddDialog());

        binding.copyDayButton.setOnClickListener(v -> showCopyDayDialog());


        selectedDateLabel = view.findViewById(R.id.selected_date_label);


        initializeCalendar();


        initializeMealCards(view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.setBackground(null);

        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusbar_color));


            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        updateCaloriesCard();

        caloriesManager.getConsumedCaloriesLiveData().observe(getViewLifecycleOwner(), calories -> {

            updateCaloriesCard();
        });

        caloriesManager.getBurnedCaloriesLiveData().observe(getViewLifecycleOwner(), calories -> {

            updateCaloriesCard();
        });

        foodManager.getCaloriesNormLiveData().observe(getViewLifecycleOwner(), norm -> {
            if (norm != null) {
                this.targetCalories = norm;
                updateCaloriesCard();
            }
        });
        foodManager.getProteinsNormLiveData().observe(getViewLifecycleOwner(), norm -> {
            if (norm != null) this.targetProteins = norm;
        });
        foodManager.getFatsNormLiveData().observe(getViewLifecycleOwner(), norm -> {
            if (norm != null) this.targetFats = norm;
        });
        foodManager.getCarbsNormLiveData().observe(getViewLifecycleOwner(), norm -> {
            if (norm != null) this.targetCarbs = norm;
        });
    }

    @Override
    public void onResume() {
        super.onResume();


        updateDayIndicators();

        foodManager.refreshNutrientNorms();

        updateAllMealCards();
        syncCaloriesWithManager();


        IntentFilter filter = new IntentFilter("com.martist.vitamove.UPDATE_DASHBOARD");
        requireContext().registerReceiver(caloriesUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

    }

    @Override
    public void onDestroyView() {

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();


        try {
            requireContext().unregisterReceiver(caloriesUpdateReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    private void updateCaloriesCard() {
        binding.caloriesPotreb.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NutritionAnalyticsActivity.class);
            startActivity(intent);
        });


        Meal breakfast = foodManager.getMealForDate("breakfast", selectedDate);
        Meal lunch = foodManager.getMealForDate("lunch", selectedDate);
        Meal dinner = foodManager.getMealForDate("dinner", selectedDate);
        Meal snack = foodManager.getMealForDate("snack", selectedDate);


        double totalProt = 0;
        double totalFat = 0;
        double totalCarb = 0;
        double consumedCalories = 0;


        if (breakfast != null) {
            totalProt += breakfast.getTotalProteins();
            totalFat += breakfast.getTotalFats();
            totalCarb += breakfast.getTotalCarbs();
            consumedCalories += breakfast.getCalories();
        }
        if (lunch != null) {
            totalProt += lunch.getTotalProteins();
            totalFat += lunch.getTotalFats();
            totalCarb += lunch.getTotalCarbs();
            consumedCalories += lunch.getCalories();
        }
        if (dinner != null) {
            totalProt += dinner.getTotalProteins();
            totalFat += dinner.getTotalFats();
            totalCarb += dinner.getTotalCarbs();
            consumedCalories += dinner.getCalories();
        }
        if (snack != null) {
            totalProt += snack.getTotalProteins();
            totalFat += snack.getTotalFats();
            totalCarb += snack.getTotalCarbs();
            consumedCalories += snack.getCalories();
        }


        int burnedCalories = 0;


        String currentDateStr = dateFormat.format(new Date());
        boolean isCurrentDay = selectedDate.equals(currentDateStr);


        if (isCurrentDay) {
            burnedCalories = caloriesManager.getTotalBurnedCalories();
        } else {

            burnedCalories = 0;
        }


        int totalAvailableCalories = (int) (targetCalories + burnedCalories);


        binding.totalCalories.setText(String.format("%d/%d ккал", (int) consumedCalories, totalAvailableCalories));
        binding.proteinsValue.setText(String.format("%.1f/%.0fг", totalProt, targetProteins));
        binding.fatsValue.setText(String.format("%.1f/%.0fг", totalFat, targetFats));
        binding.carbsValue.setText(String.format("%.1f/%.0fг", totalCarb, targetCarbs));


        int progress = totalAvailableCalories > 0 ? (int) ((consumedCalories / (float) totalAvailableCalories) * 100) : 0;
        progress = Math.min(progress, 100);
        binding.caloriesProgress.setProgress(progress);

        updateTrackedNutrients();
    }

    private void updateMealCard(String mealType) {
        MealCard card = mealCards.get(mealType);
        if (card != null) {

            Meal meal = foodManager.getMealForDate(mealType, selectedDate);
            card.update(meal);
        }
    }

    private void initializeMealCards(View view) {
        binding.breakfastCard.setTag(Constants.MEAL_TYPE_BREAKFAST);

        binding.lunchCard.setTag(Constants.MEAL_TYPE_LUNCH);

        binding.dinnerCard.setTag(Constants.MEAL_TYPE_DINNER);

        binding.snackCard.setTag(Constants.MEAL_TYPE_SNACK);

        mealCards.put(Constants.MEAL_TYPE_BREAKFAST,
                new MealCard(binding.breakfastCard, "Завтрак", R.drawable.ic_breakfast, Constants.MEAL_TYPE_BREAKFAST));
        mealCards.put(Constants.MEAL_TYPE_LUNCH,
                new MealCard(binding.lunchCard, "Обед", R.drawable.ic_lunch, Constants.MEAL_TYPE_LUNCH));
        mealCards.put(Constants.MEAL_TYPE_DINNER,
                new MealCard(binding.dinnerCard, "Ужин", R.drawable.ic_dinner, Constants.MEAL_TYPE_DINNER));
        mealCards.put(Constants.MEAL_TYPE_SNACK,
                new MealCard(binding.snackCard, "Перекус", R.drawable.ic_snack, Constants.MEAL_TYPE_SNACK));


    }

    private void openFoodSelection(String mealType) {
        Intent intent = new Intent(requireContext(), FoodSelectionActivity.class);
        intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
        foodSelectionLauncher.launch(intent);
    }


    private class MealCard {
        private final View cardView;
        private final RecyclerView foodList;
        private final TextView totalCalories;
        private final TextView proteinsValue;
        private final TextView fatsValue;
        private final TextView carbsValue;
        private final View divider;
        private final ImageView expandIcon;
        private final MealFoodsAdapter adapter;
        private boolean isExpanded = false;
        private final String mealType;
        private static final String PREFS_NAME = "meal_card_states";
        private static final String KEY_PREFIX_EXPANDED = "expanded_";

        MealCard(View cardView, String title, int iconResId, String mealType) {
            this.cardView = cardView;
            this.mealType = mealType;


            TextView titleView = cardView.findViewById(R.id.meal_title);
            titleView.setText(title);


            ImageView iconView = cardView.findViewById(R.id.meal_icon);
            iconView.setImageResource(iconResId);


            expandIcon = cardView.findViewById(R.id.expand_icon);


            divider = cardView.findViewById(R.id.divider);


            foodList = cardView.findViewById(R.id.food_list);
            foodList.setLayoutManager(new LinearLayoutManager(requireContext()));


            totalCalories = cardView.findViewById(R.id.total_calories);
            proteinsValue = cardView.findViewById(R.id.proteins_value);
            fatsValue = cardView.findViewById(R.id.fats_value);
            carbsValue = cardView.findViewById(R.id.carbs_value);


            int dailyCaloriesNorm = (int) foodManager.getDailyNorm("calories");
            adapter = new MealFoodsAdapter(new ArrayList<>(), mealType, dailyCaloriesNorm);
            foodList.setAdapter(adapter);


            adapter.setOnFoodClickListener((foodPortion, mealTypeInner) -> {
                openPortionSizeActivity(foodPortion, mealTypeInner);
            });


            setupSwipeToDelete();


            Button addFoodButton = cardView.findViewById(R.id.add_button);
            if (addFoodButton != null) {
                addFoodButton.setOnClickListener(v -> openFoodSelection(mealType));
            }


            cardView.setOnClickListener(v -> toggleExpand());
        }

        private void toggleExpand() {

            Meal meal = foodManager.getMealForDate(mealType, selectedDate);


            if (meal != null && !meal.getFoods().isEmpty()) {
                isExpanded = !isExpanded;
                foodList.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                if (divider != null) {
                    divider.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                }
                if (expandIcon != null) {
                    expandIcon.setRotation(isExpanded ? 180 : 0);
                }


                saveExpandedState(isExpanded);
            }
        }

        private void saveExpandedState(boolean expanded) {
            String key = KEY_PREFIX_EXPANDED + mealType + "_" + selectedDate;
            requireContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(key, expanded)
                    .apply();

        }


        private Boolean getSavedExpandedState() {
            String key = KEY_PREFIX_EXPANDED + mealType + "_" + selectedDate;
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);


            if (prefs.contains(key)) {
                return prefs.getBoolean(key, false);
            }

            return null;
        }

        private void setupSwipeToDelete() {

            adapter.setupSwipeToDelete(foodList);

            adapter.setOnFoodRemovedListener((position, removedPortion) -> {

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    foodList.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                }


                Meal updatedMeal = foodManager.getMeal(mealType);
                if (updatedMeal == null) {
                    return;
                }


                if (position >= 0 && position < updatedMeal.getFoods().size()) {


                    if (updatedMeal.removeFood(position)) {

                        foodManager.updateMeal(mealType, updatedMeal);


                        adapter.notifyItemRemoved(position);
                        if (position < updatedMeal.getFoods().size()) {
                            adapter.notifyItemRangeChanged(position, updatedMeal.getFoods().size() - position);
                        }


                        totalCalories.setText(String.format("%.0f ккал", updatedMeal.getCalories()));


                        View expandIconContainer = cardView.findViewById(R.id.expand_icon_container);

                        if (!updatedMeal.getFoods().isEmpty()) {

                            proteinsValue.setText(String.format("%.1f г", updatedMeal.getTotalProteins()));
                            fatsValue.setText(String.format("%.1f г", updatedMeal.getTotalFats()));
                            carbsValue.setText(String.format("%.1f г", updatedMeal.getTotalCarbs()));
                            proteinsValue.setVisibility(View.VISIBLE);
                            fatsValue.setVisibility(View.VISIBLE);
                            carbsValue.setVisibility(View.VISIBLE);


                            if (expandIconContainer != null) {
                                expandIconContainer.setVisibility(View.VISIBLE);
                            }
                        } else {

                            proteinsValue.setText("0 г");
                            fatsValue.setText("0 г");
                            carbsValue.setText("0 г");
                            proteinsValue.setVisibility(View.VISIBLE);
                            fatsValue.setVisibility(View.VISIBLE);
                            carbsValue.setVisibility(View.VISIBLE);


                            if (expandIconContainer != null) {
                                expandIconContainer.setVisibility(View.GONE);
                            }

                            divider.setVisibility(View.GONE);
                            foodList.setVisibility(View.GONE);
                            isExpanded = false;


                            if (expandIcon != null) {
                                expandIcon.setRotation(0);
                            }
                        }


                        calculateAndDisplayTotalNutrients();
                    }
                }
            });
        }


        private void calculateAndDisplayTotalNutrients() {

            updateCaloriesCard();
        }

        void update(Meal meal) {

            View expandIconContainer = cardView.findViewById(R.id.expand_icon_container);

            if (meal == null || meal.getFoods().isEmpty()) {
                totalCalories.setText("0 ккал");
                if (proteinsValue != null) proteinsValue.setText("0 г");
                if (fatsValue != null) fatsValue.setText("0 г");
                if (carbsValue != null) carbsValue.setText("0 г");


                if (expandIconContainer != null) {
                    expandIconContainer.setVisibility(View.GONE);
                }


                foodList.setVisibility(View.GONE);
                if (divider != null) {
                    divider.setVisibility(View.GONE);
                }


                isExpanded = false;
                if (expandIcon != null) {
                    expandIcon.setRotation(0);
                }

                return;
            }


            if (expandIconContainer != null) {
                expandIconContainer.setVisibility(View.VISIBLE);
            }
            Boolean savedState = getSavedExpandedState();

            if (savedState != null) {

                isExpanded = savedState;
            } else {
                if (!meal.getFoods().isEmpty()) {
                    isExpanded = true;
                }
            }


            foodList.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            if (divider != null) {
                divider.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            }
            if (expandIcon != null) {
                expandIcon.setRotation(isExpanded ? 180 : 0);
            }


            float calories = meal.getCalories();
            float percentage = (targetCalories > 0) ? (calories / targetCalories) * 100 : 0;
            totalCalories.setText(String.format("%.0f ккал (%.0f%%)", calories, percentage));

            float proteins = meal.getTotalProteins();
            float fats = meal.getTotalFats();
            float carbs = meal.getTotalCarbs();

            if (proteinsValue != null) {
                proteinsValue.setText(proteins > 0 ? String.format("%.1f г", proteins) : "0 г");
            }

            if (fatsValue != null) {
                fatsValue.setText(fats > 0 ? String.format("%.1f г", fats) : "0 г");
            }

            if (carbsValue != null) {
                carbsValue.setText(carbs > 0 ? String.format("%.1f г", carbs) : "0 г");
            }


            adapter.updateFoods(meal.getFoods());
        }
    }

    private void initializeCalendar() {

        for (int i = 1; i <= 7; i++) {
            int viewId = getResources().getIdentifier("day_" + i, "id", requireContext().getPackageName());
            dayViews[i - 1] = view.findViewById(viewId);

            final int dayIndex = i;
            dayViews[i - 1].setOnClickListener(v -> selectDay(dayIndex));
        }


        Calendar now = Calendar.getInstance();
        now.setFirstDayOfWeek(Calendar.MONDAY);


        int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        currentDayOfWeek = currentDayOfWeek == 1 ? 7 : currentDayOfWeek - 1;


        selectedWeekStart = (Calendar) now.clone();
        selectedWeekStart.add(Calendar.DAY_OF_MONTH, -(currentDayOfWeek - 1));


        selectDay(currentDayOfWeek);


        updateCopyButtonVisibility();
    }

    private void selectDay(int dayIndex) {

        Calendar selectedDay = (Calendar) selectedWeekStart.clone();
        selectedDay.add(Calendar.DAY_OF_MONTH, dayIndex - 1);


        selectedDate = dateFormat.format(selectedDay.getTime());


        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {

                foodManager.setSelectedDateForView(date);
                updateDayIndicators();

                updateCaloriesCard();
                updateAllMealCards();

                updateCopyButtonVisibility();
            }
        } catch (ParseException e) {
            Log.e(TAG, "Ошибка при установке даты: " + e.getMessage());
            Toast.makeText(requireContext(), "Ошибка при выборе даты", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDayIndicators() {
        if (selectedWeekStart == null) return;

        Calendar calendar = (Calendar) selectedWeekStart.clone();


        for (int i = 0; i < 7; i++) {
            String date = dateFormat.format(calendar.getTime());


            if (date.equals(selectedDate)) {
                dayViews[i].setBackgroundResource(R.drawable.selected_day_background);
            } else if (foodManager.hasFoodForDate(date)) {
                dayViews[i].setBackgroundResource(R.drawable.day_circle_with_check);

            } else {
                dayViews[i].setBackgroundResource(R.drawable.day_circle_background);
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }


        updateSelectedDateLabel();
    }

    private void updateSelectedDateLabel() {
        if (selectedDateLabel == null || selectedDate == null) return;

        try {

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);


            Date selected = dateFormat.parse(selectedDate);
            if (selected == null) return;

            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTime(selected);
            selectedCal.set(Calendar.HOUR_OF_DAY, 0);
            selectedCal.set(Calendar.MINUTE, 0);
            selectedCal.set(Calendar.SECOND, 0);
            selectedCal.set(Calendar.MILLISECOND, 0);


            long diffInMillis = selectedCal.getTimeInMillis() - today.getTimeInMillis();
            int daysDiff = (int) (diffInMillis / (1000 * 60 * 60 * 24));


            SimpleDateFormat displayFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
            String formattedDate = displayFormat.format(selected);

            String displayText;
            switch (daysDiff) {
                case -2:
                    displayText = "Позавчера, " + formattedDate;
                    break;
                case -1:
                    displayText = "Вчера, " + formattedDate;
                    break;
                case 0:
                    displayText = "Сегодня, " + formattedDate;
                    break;
                case 1:
                    displayText = "Завтра, " + formattedDate;
                    break;
                case 2:
                    displayText = "Послезавтра, " + formattedDate;
                    break;
                default:

                    displayText = formattedDate;
                    break;
            }

            selectedDateLabel.setText(displayText);

        } catch (ParseException e) {
            Log.e(TAG, "Ошибка при обновлении метки даты: " + e.getMessage());
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        try {
            Date currentDate = dateFormat.parse(selectedDate);
            if (currentDate != null) {
                cal.setTime(currentDate);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Ошибка парсинга даты: " + e.getMessage());
        }
        CalendarDay initialDay = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));


        Set<CalendarDay> datesWithEntries = new HashSet<>();
        List<String> datesWithFood = foodManager.getAllDatesWithFood();

        for (String dateStr : datesWithFood) {
            try {
                Date date = dateFormat.parse(dateStr);
                if (date != null) {
                    Calendar dateCal = Calendar.getInstance();
                    dateCal.setTime(date);
                    CalendarDay calendarDay = CalendarDay.from(
                            dateCal.get(Calendar.YEAR),
                            dateCal.get(Calendar.MONTH) + 1,
                            dateCal.get(Calendar.DAY_OF_MONTH)
                    );
                    datesWithEntries.add(calendarDay);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Ошибка парсинга даты с записями: " + e.getMessage());
            }
        }

        CustomCalendarDialog dialog = new CustomCalendarDialog(initialDay, date -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(date.getYear(), date.getMonth() - 1, date.getDay());
            Date selectedDateObj = selectedCal.getTime();
            foodManager.setSelectedDateForView(selectedDateObj);

            int dayOfWeek = selectedCal.get(Calendar.DAY_OF_WEEK);
            dayOfWeek = dayOfWeek == 1 ? 7 : dayOfWeek - 1;
            selectedWeekStart = (Calendar) selectedCal.clone();
            selectedWeekStart.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1));
            selectDay(dayOfWeek);
            updateCopyButtonVisibility();
            String formattedDate = new java.text.SimpleDateFormat("d MMMM yyyy", new java.util.Locale("ru"))
                    .format(selectedCal.getTime());
            Toast.makeText(requireContext(), formattedDate, Toast.LENGTH_SHORT).show();
        }, datesWithEntries);
        dialog.show(getParentFragmentManager(), "CustomCalendarDialog");
    }

    private void updateAllMealCards() {
        updateMealCard("breakfast");
        updateMealCard("lunch");
        updateMealCard("dinner");
        updateMealCard("snack");
        syncCaloriesWithManager();
    }


    private void refreshNutrientsNorms() {
        if (foodManager != null) {
            targetCalories = foodManager.getDailyNorm("calories");
            targetProteins = foodManager.getDailyNorm("proteins");
            targetFats = foodManager.getDailyNorm("fats");
            targetCarbs = foodManager.getDailyNorm("carbs");


            Log.d(TAG, "Обновлены нормы нутриентов: calories=" + targetCalories +
                    ", proteins=" + targetProteins +
                    ", fats=" + targetFats +
                    ", carbs=" + targetCarbs);


            updateCaloriesCard();
        }
    }

    @Override
    public void onStart() {
        super.onStart();


        new Handler().postDelayed(this::refreshNutrientsNorms, 300);
    }


    private void openPortionSizeActivity(Meal.FoodPortion foodPortion, String mealType) {
        Food food = foodPortion.getFood();


        if (isDish(food)) {

            openDishConstructor(food);
        } else {

            Intent intent = new Intent(requireContext(), PortionSizeActivity.class);
            intent.putExtra(Constants.EXTRA_FOOD, food);
            intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
            intent.putExtra("portion_quantity", foodPortion.getQuantity());
            intent.putExtra("portion_name", foodPortion.getPortionName());
            intent.putExtra(Constants.EXTRA_SELECTED_DATE, selectedDate);
            startActivity(intent);
        }
    }


    private boolean isDish(Food food) {


        return "Блюда".equals(food.getCategory()) && "Собственные".equals(food.getSubcategory());
    }


    private void openDishConstructor(Food food) {

        String originalDishId = food.getDishId();

        Log.d(TAG, "Открываем конструктор блюда для редактирования. Food ID: " + food.getId() +
                ", Original Dish ID: " + originalDishId + ", Название: " + food.getName());


        ensureFoodManagerReady(() -> {
            Intent intent = new Intent(requireContext(), DishConstructorActivity.class);
            intent.putExtra("dish_id", originalDishId);
            startActivity(intent);
        });
    }


    private void ensureFoodManagerReady(Runnable onReady) {
        Log.d(TAG, "Обеспечиваем готовность FoodManager для загрузки блюда");


        foodManager.loadFoodsAsync(() -> {
            Log.d(TAG, "FoodManager готов, открываем конструктор блюда");
            onReady.run();
        });
    }


    private void syncCaloriesWithManager() {

        String currentDateStr = dateFormat.format(new Date());
        String selectedDateStr = foodManager.getSelectedDateFormatted();


        int totalConsumedCalories = (int) foodManager.getTotalCaloriesForSelectedDate();


        if (currentDateStr.equals(selectedDateStr)) {
            caloriesManager.setConsumedCalories(totalConsumedCalories);
            Log.d(TAG, "Синхронизация калорий с CaloriesManager (текущая дата): " + totalConsumedCalories + " кал.");
        }
    }


    private void showQuickAddDialog() {
        QuickAddBottomSheet bottomSheet = QuickAddBottomSheet.newInstance();

        bottomSheet.setListener(mealType -> {

            updateMealCard(mealType);

            updateCaloriesCard();

            syncCaloriesWithManager();
        });
        bottomSheet.show(getChildFragmentManager(), "QuickAddBottomSheet");
    }


    private void updateTrackedNutrients() {
        Log.d("aaa", "[eq");


        View nutrientsContainer = view.findViewById(R.id.tracked_nutrients_container);
        View micronutrientsCard = view.findViewById(R.id.micronutrients_card);


        trackedNutrients = getTrackedNutrients();


        if (trackedNutrients.isEmpty()) {
            nutrientsContainer.setVisibility(View.GONE);
            micronutrientsCard.setVisibility(View.GONE);
            return;
        }


        nutrientsContainer.setVisibility(View.VISIBLE);
        micronutrientsCard.setVisibility(View.VISIBLE);


        ((ViewGroup) nutrientsContainer).removeAllViews();


        Map<NutrientType, Float> consumedNutrients = foodManager.getConsumedNutrients(selectedDate);

        Log.d("aaa", "[eq");


        for (NutrientType nutrient : trackedNutrients) {
            Log.d("aaa", nutrient.name());
            float consumed = consumedNutrients.getOrDefault(nutrient, 0f);


            View nutrientView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_tracked_nutrient, (ViewGroup) nutrientsContainer, false);


            TextView nameView = nutrientView.findViewById(R.id.nutrient_name);
            TextView valueView = nutrientView.findViewById(R.id.nutrient_value);
            TextView percentView = nutrientView.findViewById(R.id.nutrient_percent);
            ProgressBar progressBar = nutrientView.findViewById(R.id.nutrient_progress);

            nameView.setText(nutrient.getLocalizedName());
            valueView.setText(String.format(Locale.getDefault(), "%.1f %s", consumed, nutrient.getUnit()));


            int percent = (int) (consumed * 100 / nutrient.getPersonalizedNorm());
            percentView.setText(String.format(Locale.getDefault(), "%d%%", percent));


            if (percent < 30) {
                percentView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorDanger));
            } else if (percent < 70) {
                percentView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorWarning));
            } else {
                percentView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSuccess));
            }


            progressBar.setProgress(Math.min(percent, 100));


            ((ViewGroup) nutrientsContainer).addView(nutrientView);
        }

    }

    private void showVoiceInputDialog() {
        VoiceInputBottomSheet voiceInputBottomSheet = VoiceInputBottomSheet.newInstance();
        voiceInputBottomSheet.setOnFoodsAddedListener((foods, mealType) -> {
            updateMealCard(mealType);
            updateCaloriesCard();
        });

        voiceInputBottomSheet.show(getParentFragmentManager(), "VoiceInputBottomSheet");
    }

    private String getMealTypeName(String mealType) {
        switch (mealType) {
            case Constants.MEAL_TYPE_BREAKFAST:
                return "Завтрак";
            case Constants.MEAL_TYPE_LUNCH:
                return "Обед";
            case Constants.MEAL_TYPE_DINNER:
                return "Ужин";
            case Constants.MEAL_TYPE_SNACK:
                return "Перекус";
            default:
                return mealType;
        }
    }


    private void updateCopyButtonVisibility() {
        String currentDateStr = dateFormat.format(new Date());
        boolean isCurrentDay = selectedDate.equals(currentDateStr);
        binding.copyDayButton.setVisibility(isCurrentDay ? View.GONE : View.VISIBLE);
    }


    private void showCopyDayDialog() {
        String currentDateStr = dateFormat.format(new Date());


        String formattedDate;
        try {
            Date date = dateFormat.parse(selectedDate);
            formattedDate = new SimpleDateFormat("d MMMM yyyy", new Locale("ru")).format(date);
        } catch (ParseException e) {
            formattedDate = selectedDate;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Копирование дня")
                .setMessage("Все продукты из " + formattedDate + " будут скопированы в сегодняшний день.\n\n" +
                        "Существующие записи за сегодня сохранятся, к ним добавятся новые продукты.\n\n" +
                        "Продолжить?")
                .setPositiveButton("Скопировать", (dialog, which) -> {
                    copyDayToToday();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void copyDayToToday() {
        String currentDateStr = dateFormat.format(new Date());


        if (selectedDate.equals(currentDateStr)) {
            Toast.makeText(requireContext(), "Невозможно скопировать текущий день", Toast.LENGTH_SHORT).show();
            return;
        }


        String sourceDateStr = selectedDate;


        Calendar now = Calendar.getInstance();
        now.setFirstDayOfWeek(Calendar.MONDAY);
        int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        currentDayOfWeek = currentDayOfWeek == 1 ? 7 : currentDayOfWeek - 1;


        selectedWeekStart = (Calendar) now.clone();
        selectedWeekStart.add(Calendar.DAY_OF_MONTH, -(currentDayOfWeek - 1));


        selectedDate = currentDateStr;
        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                foodManager.setSelectedDateForView(date);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Ошибка при установке даты: " + e.getMessage());
        }

        int totalCopiedItems = 0;


        String[] mealTypes = {Constants.MEAL_TYPE_BREAKFAST, Constants.MEAL_TYPE_LUNCH,
                Constants.MEAL_TYPE_DINNER, Constants.MEAL_TYPE_SNACK};


        for (String mealType : mealTypes) {

            Meal sourceMeal = foodManager.getMealForDate(mealType, sourceDateStr);

            if (sourceMeal != null && !sourceMeal.getFoods().isEmpty()) {

                Meal targetMeal = foodManager.getMealForDate(mealType, currentDateStr);


                if (targetMeal == null) {
                    targetMeal = new Meal(getMealTypeName(mealType), getMealIconResId(mealType));
                }


                for (Meal.FoodPortion portion : sourceMeal.getFoods()) {
                    Food food = portion.getFood();
                    float quantity = portion.getQuantity();
                    String portionName = portion.getPortionName();


                    targetMeal.addFood(food, quantity, portionName);
                    totalCopiedItems++;

                }


                foodManager.updateMeal(mealType, targetMeal);
            }
        }


        if (totalCopiedItems > 0) {
            Toast.makeText(requireContext(),
                    String.format("Скопировано %d продукт(ов) в сегодня", totalCopiedItems),
                    Toast.LENGTH_SHORT).show();


            updateDayIndicators();
            updateCaloriesCard();
            updateAllMealCards();
            updateCopyButtonVisibility();
        } else {
            Toast.makeText(requireContext(), "Нет продуктов для копирования", Toast.LENGTH_SHORT).show();
        }
    }

    private int getMealIconResId(String mealType) {
        switch (mealType) {
            case Constants.MEAL_TYPE_BREAKFAST:
                return R.drawable.ic_breakfast;
            case Constants.MEAL_TYPE_LUNCH:
                return R.drawable.ic_lunch;
            case Constants.MEAL_TYPE_DINNER:
                return R.drawable.ic_dinner;
            case Constants.MEAL_TYPE_SNACK:
                return R.drawable.ic_snack;
            default:
                return R.drawable.ic_breakfast;
        }
    }
} 