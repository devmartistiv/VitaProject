package com.martist.vitamove.workout.data.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.workout.ui.adapters.EquipmentFilterAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EquipmentFilterManager {
    private static final String TAG = "EquipmentFilterManager";
    private static final String PREFS_NAME = "EquipmentFilterPrefs";
    private static final String KEY_SELECTED_EQUIPMENT = "selected_equipment";
    private static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";

    private final SharedPreferences preferences;
    private final Context context;

    public EquipmentFilterManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public Set<String> getSelectedEquipment() {
        Set<String> savedEquipment = preferences.getStringSet(KEY_SELECTED_EQUIPMENT, new HashSet<>());
        Log.d(TAG, "Загружено сохраненное оборудование: " + savedEquipment.size() + " элементов");
        for (String equipment : savedEquipment) {
            Log.d(TAG, "Сохраненное оборудование: '" + equipment + "'");
        }
        return savedEquipment;
    }


    public void saveSelectedEquipment(Set<String> selectedEquipment) {
        Log.d(TAG, "Сохраняем выбранное оборудование: " + selectedEquipment.size() + " элементов");
        for (String equipment : selectedEquipment) {
            Log.d(TAG, "Сохраняем оборудование: '" + equipment + "'");
        }

        preferences.edit()
                .putStringSet(KEY_SELECTED_EQUIPMENT, selectedEquipment)
                .putBoolean(KEY_IS_FIRST_LAUNCH, false)
                .apply();

        Log.d(TAG, "Оборудование успешно сохранено");
    }


    public boolean isFirstLaunch() {
        boolean isFirst = preferences.getBoolean(KEY_IS_FIRST_LAUNCH, true);
        Log.d(TAG, "Проверка первого запуска: " + (isFirst ? "первый запуск" : "не первый запуск"));
        return isFirst;
    }


    public List<EquipmentFilterAdapter.EquipmentItem> getAvailableEquipment(List<Exercise> exercises) {
        Map<String, Integer> equipmentCount = new HashMap<>();


        for (Exercise exercise : exercises) {
            List<String> equipmentRequired = exercise.getEquipmentRequired();
            if (equipmentRequired != null && !equipmentRequired.isEmpty()) {
                for (String equipment : equipmentRequired) {
                    if (equipment != null && !equipment.trim().isEmpty()) {
                        String normalizedEquipment = equipment.trim();
                        equipmentCount.put(normalizedEquipment,
                                equipmentCount.getOrDefault(normalizedEquipment, 0) + 1);
                    }
                }
            } else {

                String noEquipment = "Без оборудования";
                equipmentCount.put(noEquipment,
                        equipmentCount.getOrDefault(noEquipment, 0) + 1);
            }
        }


        List<EquipmentFilterAdapter.EquipmentItem> equipmentList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : equipmentCount.entrySet()) {
            equipmentList.add(new EquipmentFilterAdapter.EquipmentItem(
                    entry.getKey(), entry.getValue()));
        }


        equipmentList.sort((a, b) -> {

            if (a.getName().equals("Без оборудования")) return -1;
            if (b.getName().equals("Без оборудования")) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        return equipmentList;
    }


    public List<Exercise> filterExercisesByEquipment(List<Exercise> exercises, Set<String> selectedEquipment) {
        Log.d(TAG, "Фильтрация упражнений. Всего упражнений: " + exercises.size() +
                ", выбрано оборудования: " + selectedEquipment.size());


        if (selectedEquipment.isEmpty()) {
            Log.d(TAG, "Оборудование не выбрано - возвращаем пустой список");
            return new ArrayList<>();
        }


        for (String equipment : selectedEquipment) {
            Log.d(TAG, "Выбрано оборудование: '" + equipment + "'");
        }

        List<Exercise> filteredExercises = new ArrayList<>();

        for (Exercise exercise : exercises) {
            List<String> exerciseEquipment = exercise.getEquipmentRequired();

            if (exerciseEquipment == null || exerciseEquipment.isEmpty()) {

                if (selectedEquipment.contains("Без оборудования")) {
                    filteredExercises.add(exercise);
                    Log.d(TAG, "Добавлено упражнение без оборудования: " + exercise.getName());
                }
            } else {

                boolean hasMatchingEquipment = false;
                for (String equipment : exerciseEquipment) {
                    if (equipment != null && selectedEquipment.contains(equipment.trim())) {
                        hasMatchingEquipment = true;
                        break;
                    }
                }

                if (hasMatchingEquipment) {
                    filteredExercises.add(exercise);
                    Log.d(TAG, "Добавлено упражнение с подходящим оборудованием: " + exercise.getName());
                }
            }
        }

        Log.d(TAG, "Результат фильтрации: " + filteredExercises.size() + " упражнений");
        return filteredExercises;
    }


    public Set<String> selectAllEquipment(List<EquipmentFilterAdapter.EquipmentItem> availableEquipment) {
        Log.d(TAG, "Выбираем все доступное оборудование. Доступно: " + availableEquipment.size() + " типов");

        Set<String> allEquipment = new HashSet<>();
        for (EquipmentFilterAdapter.EquipmentItem item : availableEquipment) {
            allEquipment.add(item.getName());
            Log.d(TAG, "Выбираем оборудование: '" + item.getName() + "' (" + item.getExerciseCount() + " упражнений)");
        }

        Log.d(TAG, "Всего выбрано оборудования: " + allEquipment.size() + " типов");
        return allEquipment;
    }


    public void clearAllFilters() {
        preferences.edit().clear().apply();
    }
}
