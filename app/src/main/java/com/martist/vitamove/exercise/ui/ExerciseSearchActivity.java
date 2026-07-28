package com.martist.vitamove.exercise.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.exercise.ui.adapters.ExerciseAdapter;
import com.martist.vitamove.exercise.ui.adapters.ExerciseCategoryAdapter;
import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseCategory;
import com.martist.vitamove.workout.data.managers.EquipmentFilterManager;
import com.martist.vitamove.workout.ui.fragments.EquipmentFilterFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ExerciseSearchActivity extends BaseActivity {
    private static final String TAG = "ExerciseSearchActivity";
    private SearchView searchView;
    private ImageView filterIcon;
    private RecyclerView exerciseList;
    private ExerciseAdapter exerciseAdapter;
    private ExerciseCategoryAdapter categoryAdapter;

    private ProgressBar progressBar;
    private List<Exercise> allExercises = new ArrayList<>();
    private static final int REQUEST_CODE_EXERCISE_DETAILS = 101;


    private boolean showingCategories = true;
    private String currentCategory = null;


    private EquipmentFilterManager equipmentFilterManager;
    private Set<String> selectedEquipment;

    ExercisesViewModel exercisesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_search);


        exercisesViewModel = new ViewModelProvider(this).get(ExercisesViewModel.class);


        equipmentFilterManager = new EquipmentFilterManager(this);

        setupToolbar();
        setupViews();
        setupObservers();
        exercisesViewModel.getAllExercises();

    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Поиск упражнений");
    }

    private void setupViews() {
        searchView = findViewById(R.id.search_view);
        filterIcon = findViewById(R.id.filter_icon);
        exerciseList = findViewById(R.id.exercise_list);
        progressBar = findViewById(R.id.progressBar);


        exerciseList.setLayoutManager(new LinearLayoutManager(this));


        exerciseAdapter = new ExerciseAdapter(this, new ExerciseAdapter.OnExerciseClickListener() {
            @Override
            public void onExerciseClick(Exercise exercise) {

                Intent intent = new Intent(ExerciseSearchActivity.this, ExerciseDetailsActivity.class);
                intent.putExtra(ExerciseDetailsActivity.EXTRA_EXERCISE_ID, exercise.getId());


                boolean fromAnalytics = getIntent().getBooleanExtra("from_analytics", false);
                if (fromAnalytics) {

                    intent.putExtra("from_analytics", true);
                }

                startActivityForResult(intent, REQUEST_CODE_EXERCISE_DETAILS);
            }

            @Override
            public void onAddExerciseClick(Exercise exercise) {

                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_exercise", exercise);
                resultIntent.putExtra("action", "add");
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });


        categoryAdapter = new ExerciseCategoryAdapter(this, categoryName -> {

            showExercisesForCategory(categoryName);
        });


        showCategories();


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performLocalSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performLocalSearch(newText);
                return true;
            }
        });


        filterIcon.setOnClickListener(v -> showEquipmentFilterDialog());
    }

    private void setupObservers() {

        exercisesViewModel.getExercisesLiveData().observe(this, exercises -> {
            allExercises = exercises;


            if (selectedEquipment == null && !exercises.isEmpty()) {
                initializeEquipmentFilter();
            }


            applyFiltersAndUpdateDisplay();

            Log.d(TAG, "Получено " + exercises.size() + " упражнений из ViewModel");
        });


        exercisesViewModel.isLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });


        exercisesViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLocalSearch(String query) {
        if (query == null || query.isEmpty()) {

            applyFiltersAndUpdateDisplay();
            return;
        }


        if (showingCategories) {
            showExercisesMode();
        }


        String queryLowerCase = query.toLowerCase();
        String[] queryWords = queryLowerCase.split("\\s+");


        List<Exercise> filteredByEquipment = selectedEquipment != null ?
                equipmentFilterManager.filterExercisesByEquipment(allExercises, selectedEquipment) : allExercises;


        List<Exercise> searchBase = currentCategory != null ?
                filterExercisesByCategory(filteredByEquipment, currentCategory) : filteredByEquipment;

        List<Exercise> filteredExercises = searchBase.stream()
                .filter(exercise -> {

                    for (String word : queryWords) {

                        if (!containsWordOrPrefix(exercise, word)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());


        sortExercisesByRelevance(filteredExercises, queryLowerCase, queryWords);

        exerciseAdapter.updateExercises(filteredExercises);
        Log.d(TAG, "Найдено " + filteredExercises.size() + " упражнений по запросу '" + query + "'");
    }


    private boolean containsWordOrPrefix(Exercise exercise, String word) {

        if (exercise.getName() != null) {
            String nameLower = exercise.getName().toLowerCase();

            if (containsWordOrPrefix(nameLower, word)) {
                return true;
            }
        }


        if (exercise.getDescription() != null) {
            String descLower = exercise.getDescription().toLowerCase();
            if (containsWordOrPrefix(descLower, word)) {
                return true;
            }
        }


        if (exercise.getMuscleGroupRussianNames() != null) {
            for (String muscle : exercise.getMuscleGroupRussianNames()) {
                String muscleLower = muscle.toLowerCase();
                if (containsWordOrPrefix(muscleLower, word)) {
                    return true;
                }
            }
        }


        if (exercise.getSecondaryMuscles() != null) {
            for (String muscle : exercise.getSecondaryMuscles()) {
                String muscleLower = muscle.toLowerCase();
                if (containsWordOrPrefix(muscleLower, word)) {
                    return true;
                }
            }
        }


        if (exercise.getStabilizerMuscles() != null) {
            for (String muscle : exercise.getStabilizerMuscles()) {
                String muscleLower = muscle.toLowerCase();
                if (containsWordOrPrefix(muscleLower, word)) {
                    return true;
                }
            }
        }


        if (exercise.getInstructions() != null) {
            String instructionsLower = exercise.getInstructions().toLowerCase();
            return containsWordOrPrefix(instructionsLower, word);
        }

        return false;
    }


    private boolean containsWordOrPrefix(String text, String wordOrPrefix) {

        final int MIN_PREFIX_LENGTH = 3;


        if (text.contains(wordOrPrefix)) {
            return true;
        }


        if (wordOrPrefix.length() < MIN_PREFIX_LENGTH) {
            return false;
        }


        String[] words = text.split("\\s+");

        for (String w : words) {

            if (w.startsWith(wordOrPrefix)) {
                return true;
            }


            if (wordOrPrefix.length() >= MIN_PREFIX_LENGTH && w.length() > wordOrPrefix.length()) {

                String wPrefix = w.substring(0, Math.min(w.length(), wordOrPrefix.length() + 3));


                int distance = calculateLevenshteinDistance(wPrefix, wordOrPrefix);


                int maxAllowedDistance = Math.max(1, wordOrPrefix.length() / 3);

                if (distance <= maxAllowedDistance) {
                    Log.d(TAG, "Найден префикс с опечаткой: '" + wPrefix + "' для '" + wordOrPrefix + "' с расстоянием " + distance);
                    return true;
                }
            }


            if (wordOrPrefix.length() >= MIN_PREFIX_LENGTH && w.length() >= MIN_PREFIX_LENGTH) {


                if (w.length() <= wordOrPrefix.length() * 1.5) {
                    int distance = calculateLevenshteinDistance(w, wordOrPrefix);

                    int maxAllowedDistance = Math.max(1, Math.min(wordOrPrefix.length() / 3, 3));

                    if (distance <= maxAllowedDistance) {
                        Log.d(TAG, "Найдено с опечаткой: '" + w + "' для '" + wordOrPrefix + "' с расстоянием " + distance);
                        return true;
                    }
                } else {
                    String wStart = w.substring(0, Math.min(w.length(), wordOrPrefix.length() + 2));
                    int distance = calculateLevenshteinDistance(wStart, wordOrPrefix);
                    int maxAllowedDistance = Math.max(1, Math.min(wordOrPrefix.length() / 3, 2));

                    if (distance <= maxAllowedDistance) {
                        Log.d(TAG, "Найдено начало слова с опечаткой: '" + wStart + "' для '" + wordOrPrefix + "' с расстоянием " + distance);
                        return true;
                    }
                }
            }
        }

        return false;
    }


    private int calculateLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();


        int[][] dp = new int[len1 + 1][len2 + 1];


        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }


        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {

                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;


                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }


        return dp[len1][len2];
    }


    private void sortExercisesByRelevance(List<Exercise> exercises, String query, String[] queryWords) {
        Collections.sort(exercises, (a, b) -> {
            int scoreA = calculateRelevanceScore(a, query, queryWords);
            int scoreB = calculateRelevanceScore(b, query, queryWords);

            return Integer.compare(scoreB, scoreA);
        });
    }


    private int calculateRelevanceScore(Exercise exercise, String query, String[] queryWords) {
        int score = 0;


        if (exercise.getName() != null) {
            String name = exercise.getName().toLowerCase();


            if (name.equals(query)) {
                score += 100;
            } else if (name.startsWith(query)) {
                score += 50;
            } else if (name.contains(query)) {
                score += 30;
            }


            for (String word : queryWords) {
                if (name.contains(word)) {
                    score += 10;
                }
            }


            String[] nameWords = name.split("\\s+");
            int excessWords = Math.max(0, nameWords.length - queryWords.length);
            score -= excessWords * 3;
        }


        if (exercise.getDescription() != null) {
            String desc = exercise.getDescription().toLowerCase();
            if (desc.contains(query)) {
                score += 15;
            }
            for (String word : queryWords) {
                if (desc.contains(word)) {
                    score += 5;
                }
            }
        }


        boolean queryHasNegativeIncline = false;
        for (String word : queryWords) {
            if (word.contains("отрицат") || word.contains("наклон")) {
                queryHasNegativeIncline = true;
                break;
            }
        }

        if (!queryHasNegativeIncline) {

            if (exercise.getName() != null) {
                String name = exercise.getName().toLowerCase();
                if (name.contains("отрицательн") && name.contains("наклон")) {
                    score -= 40;
                }
            }
        }


        if (exercise.getMuscleGroupRussianNames() != null) {
            for (String muscle : exercise.getMuscleGroupRussianNames()) {
                String muscleLower = muscle.toLowerCase();
                if (muscleLower.equals(query)) {
                    score += 40;
                } else if (muscleLower.contains(query)) {
                    score += 20;
                }
                for (String word : queryWords) {
                    if (muscleLower.contains(word)) {
                        score += 8;
                    }
                }
            }
        }


        if (exercise.getSecondaryMuscles() != null) {
            for (String muscle : exercise.getSecondaryMuscles()) {
                String muscleLower = muscle.toLowerCase();
                if (muscleLower.contains(query)) {
                    score += 10;
                }
                for (String word : queryWords) {
                    if (muscleLower.contains(word)) {
                        score += 3;
                    }
                }
            }
        }


        if (exercise.getStabilizerMuscles() != null) {
            for (String muscle : exercise.getStabilizerMuscles()) {
                String muscleLower = muscle.toLowerCase();
                if (muscleLower.contains(query)) {
                    score += 5;
                }
                for (String word : queryWords) {
                    if (muscleLower.contains(word)) {
                        score += 2;
                    }
                }
            }
        }


        if (exercise.getInstructions() != null) {
            String instructions = exercise.getInstructions().toLowerCase();
            if (instructions.contains(query)) {
                score += 5;
            }
            for (String word : queryWords) {
                if (instructions.contains(word)) {
                    score += 1;
                }
            }
        }


        boolean queryHasHorizontal = false;
        for (String word : queryWords) {
            if (word.contains("горизонт")) {
                queryHasHorizontal = true;
                break;
            }
        }

        if (queryHasHorizontal && exercise.getName() != null) {
            String name = exercise.getName().toLowerCase();
            if (name.contains("горизонт")) {
                score += 35;
            } else if (name.contains("наклон")) {
                score -= 30;
            }
        }

        return score;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_EXERCISE_DETAILS && resultCode == RESULT_OK && data != null) {

            boolean fromAnalytics = getIntent().getBooleanExtra("from_analytics", false);


            if (data.getBooleanExtra("exercise_added", false) || data.getBooleanExtra("exercise_added_via_details", false)) {
                String exerciseId = data.getStringExtra("exercise_id");
                Log.d(TAG, "Упражнение выбрано из экрана деталей, ID: " + exerciseId);


                Intent resultIntent = new Intent();
                resultIntent.putExtra("exercise_added_via_details", true);
                resultIntent.putExtra("exercise_id", exerciseId);
                setResult(RESULT_OK, resultIntent);


                finish();
            }
        }
    }


    private void showCategories() {
        showingCategories = true;
        currentCategory = null;

        exerciseList.setAdapter(categoryAdapter);
        getSupportActionBar().setTitle("Категории упражнений");


        applyFiltersAndUpdateDisplay();

        Log.d(TAG, "Показываем категории упражнений с учетом фильтров");
    }


    private void showExercisesForCategory(String categoryName) {
        showingCategories = false;


        String originalCategoryName = ExerciseCategory.getOriginalNameFromDisplayName(categoryName);
        currentCategory = originalCategoryName;

        exerciseList.setAdapter(exerciseAdapter);
        getSupportActionBar().setTitle(categoryName);


        applyFiltersAndUpdateDisplay();

        Log.d(TAG, "Показываем упражнения категории: " + categoryName + " (ориг: " + originalCategoryName + ") с учетом фильтров");
    }


    private void showExercisesMode() {
        showingCategories = false;
        currentCategory = null;

        exerciseList.setAdapter(exerciseAdapter);
        getSupportActionBar().setTitle("Поиск упражнений");


        applyFiltersAndUpdateDisplay();

        Log.d(TAG, "Переключение в режим отображения всех упражнений с учетом фильтров");
    }


    private void updateCategoriesWithCounts(List<Exercise> exercises) {
        List<ExerciseCategory> categories = new ArrayList<>();


        for (String categoryName : ExerciseCategory.getEquipmentCategories()) {
            int exerciseCount = countExercisesInCategory(exercises, categoryName, ExerciseCategory.CategoryType.EQUIPMENT);


            if (exerciseCount > 0) {
                ExerciseCategory category = new ExerciseCategory(
                        categoryName,
                        ExerciseCategory.getIconForCategory(categoryName),
                        exerciseCount,
                        ExerciseCategory.CategoryType.EQUIPMENT
                );

                categories.add(category);
            }
        }


        for (String categoryName : ExerciseCategory.getMuscleGroupCategories()) {
            int exerciseCount = countExercisesInCategory(exercises, categoryName, ExerciseCategory.CategoryType.MUSCLE_GROUP);


            if (exerciseCount > 0) {
                ExerciseCategory category = new ExerciseCategory(
                        categoryName,
                        ExerciseCategory.getIconForCategory(categoryName),
                        exerciseCount,
                        ExerciseCategory.CategoryType.MUSCLE_GROUP
                );

                categories.add(category);
            }
        }


        String[] originalTypeNames = ExerciseCategory.getExerciseTypeCategories();
        String[] displayTypeNames = ExerciseCategory.getExerciseTypeDisplayNames();

        for (int i = 0; i < originalTypeNames.length; i++) {
            String originalName = originalTypeNames[i];
            String displayName = displayTypeNames[i];

            int exerciseCount = countExercisesInCategory(exercises, originalName, ExerciseCategory.CategoryType.EXERCISE_TYPE);


            if (exerciseCount > 0) {
                ExerciseCategory category = new ExerciseCategory(
                        displayName,
                        ExerciseCategory.getIconForCategory(originalName),
                        exerciseCount,
                        ExerciseCategory.CategoryType.EXERCISE_TYPE
                );

                categories.add(category);
            }
        }

        categoryAdapter.setCategories(categories);

        Log.d(TAG, "Обновлен список категорий. Показано категорий: " + categories.size() + " (с упражнениями)");
    }


    private List<Exercise> filterExercisesByCategory(List<Exercise> exercises, String categoryName) {
        ExerciseCategory.CategoryType categoryType = ExerciseCategory.getCategoryType(categoryName);

        return exercises.stream()
                .filter(exercise -> {
                    if (categoryType == ExerciseCategory.CategoryType.EQUIPMENT) {

                        if ("Без оборудования".equalsIgnoreCase(categoryName)) {

                            List<String> equipment = exercise.getEquipmentRequired();
                            return equipment == null || equipment.isEmpty();
                        }
                        return false;
                    } else if (categoryType == ExerciseCategory.CategoryType.MUSCLE_GROUP) {

                        List<String> exerciseCategories = exercise.getCategories();
                        if (exerciseCategories != null) {
                            for (String category : exerciseCategories) {
                                if (categoryName.equalsIgnoreCase(category)) {
                                    return true;
                                }
                            }
                        }


                        String primaryCategory = exercise.getCategory();
                        return categoryName.equalsIgnoreCase(primaryCategory);
                    } else {

                        String exerciseType = exercise.getExerciseType();
                        return categoryName.equalsIgnoreCase(exerciseType);
                    }
                })
                .collect(Collectors.toList());
    }


    private int countExercisesInCategory(List<Exercise> exercises, String categoryName, ExerciseCategory.CategoryType categoryType) {
        return (int) exercises.stream()
                .filter(exercise -> {
                    if (categoryType == ExerciseCategory.CategoryType.EQUIPMENT) {

                        if ("Без оборудования".equalsIgnoreCase(categoryName)) {

                            List<String> equipment = exercise.getEquipmentRequired();
                            return equipment == null || equipment.isEmpty();
                        }
                        return false;
                    } else if (categoryType == ExerciseCategory.CategoryType.MUSCLE_GROUP) {

                        List<String> exerciseCategories = exercise.getCategories();
                        if (exerciseCategories != null) {
                            for (String category : exerciseCategories) {
                                if (categoryName.equalsIgnoreCase(category)) {
                                    return true;
                                }
                            }
                        }


                        String primaryCategory = exercise.getCategory();
                        return categoryName.equalsIgnoreCase(primaryCategory);
                    } else {

                        String exerciseType = exercise.getExerciseType();
                        return categoryName.equalsIgnoreCase(exerciseType);
                    }
                })
                .count();
    }


    private void initializeEquipmentFilter() {
        selectedEquipment = equipmentFilterManager.getSelectedEquipment();


        if (equipmentFilterManager.isFirstLaunch() || selectedEquipment.isEmpty()) {
            selectedEquipment = equipmentFilterManager.selectAllEquipment(
                    equipmentFilterManager.getAvailableEquipment(allExercises));
            equipmentFilterManager.saveSelectedEquipment(selectedEquipment);
        }
    }


    private void applyFiltersAndUpdateDisplay() {
        if (selectedEquipment == null) {
            Log.d(TAG, "selectedEquipment is null, не применяем фильтры");
            return;
        }

        Log.d(TAG, "Применяем фильтры. Всего упражнений: " + allExercises.size() +
                ", выбрано оборудования: " + selectedEquipment.size());


        List<Exercise> filteredByEquipment = equipmentFilterManager.filterExercisesByEquipment(allExercises, selectedEquipment);
        Log.d(TAG, "После фильтрации по оборудованию осталось: " + filteredByEquipment.size() + " упражнений");


        if (showingCategories) {
            Log.d(TAG, "Обновляем категории с учетом фильтра");
            updateCategoriesWithCounts(filteredByEquipment);
        } else {

            List<Exercise> finalFilteredExercises;
            if (currentCategory != null) {
                finalFilteredExercises = filterExercisesByCategory(filteredByEquipment, currentCategory);
                Log.d(TAG, "Фильтрация по категории '" + currentCategory + "': " + finalFilteredExercises.size() + " упражнений");
            } else {
                finalFilteredExercises = filteredByEquipment;
                Log.d(TAG, "Показываем все отфильтрованные упражнения: " + finalFilteredExercises.size());
            }
            exerciseAdapter.updateExercises(finalFilteredExercises);
        }
    }


    private void showEquipmentFilterDialog() {
        if (allExercises.isEmpty()) {
            Toast.makeText(this, "Загрузка упражнений...", Toast.LENGTH_SHORT).show();
            return;
        }

        EquipmentFilterFragment fragment = EquipmentFilterFragment.newInstance(new ArrayList<>(allExercises));
        fragment.setOnFilterAppliedListener(newSelectedEquipment -> {
            Log.d(TAG, "Получено уведомление о применении фильтра. Выбрано: " + newSelectedEquipment.size() + " элементов");
            selectedEquipment = newSelectedEquipment;
            applyFiltersAndUpdateDisplay();


            String currentQuery = searchView.getQuery().toString();
            if (!currentQuery.isEmpty()) {
                Log.d(TAG, "Повторяем поиск с новым фильтром: " + currentQuery);
                performLocalSearch(currentQuery);
            }

            Log.d(TAG, "Фильтр успешно применен");
        });

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack("EquipmentFilter")
                .commit();
    }


    @Override
    public void onBackPressed() {
        if (!showingCategories) {

            searchView.setQuery("", false);
            showCategories();
        } else {

            super.onBackPressed();
        }
    }
} 