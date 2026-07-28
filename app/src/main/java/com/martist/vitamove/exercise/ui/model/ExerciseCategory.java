package com.martist.vitamove.exercise.ui.model;


public class ExerciseCategory {
    private final String name;
    private final String iconResource;
    private final int exerciseCount;
    private final CategoryType type;

    public enum CategoryType {
        MUSCLE_GROUP,
        EXERCISE_TYPE,
        EQUIPMENT
    }

    public ExerciseCategory(String name, String iconResource, int exerciseCount, CategoryType type) {
        this.name = name;
        this.iconResource = iconResource;
        this.exerciseCount = exerciseCount;
        this.type = type;
    }


    public ExerciseCategory(String name, String iconResource, int exerciseCount) {
        this(name, iconResource, exerciseCount, CategoryType.MUSCLE_GROUP);
    }

    public String getName() {
        return name;
    }

    public String getIconResource() {
        return iconResource;
    }

    public int getExerciseCount() {
        return exerciseCount;
    }

    public CategoryType getType() {
        return type;
    }


    public static String[] getMuscleGroupCategories() {
        return new String[]{
                "Бицепс",
                "Грудь",
                "Кор",
                "Ноги",
                "Плечи",
                "Предплечье",
                "Спина",
                "Трицепс",
                "Ягодицы"
        };
    }


    public static String[] getExerciseTypeCategories() {
        return new String[]{
                "кардио",
                "разминка",
                "растяжка",
                "реабилитационное",
                "силовое",
                "с собственным весом",
                "статическое",
                "функциональное"
        };
    }


    public static String[] getExerciseTypeDisplayNames() {
        return new String[]{
                "кардио упражнения",
                "разминка",
                "растяжка",
                "реабилитационные упражнения",
                "силовые упражнения",
                "упражнения с собственным весом",
                "статические упражнения",
                "функциональные упражнения"
        };
    }


    public static String[] getEquipmentCategories() {
        return new String[]{
                "Без оборудования"
        };
    }


    public static String getDisplayNameForExerciseType(String originalName) {
        String[] originalNames = getExerciseTypeCategories();
        String[] displayNames = getExerciseTypeDisplayNames();

        for (int i = 0; i < originalNames.length; i++) {
            if (originalNames[i].equals(originalName)) {
                return displayNames[i];
            }
        }

        return originalName;
    }


    public static String[] getAllCategories() {
        String[] equipmentCategories = getEquipmentCategories();
        String[] muscleGroups = getMuscleGroupCategories();
        String[] exerciseTypes = getExerciseTypeCategories();

        String[] allCategories = new String[equipmentCategories.length + muscleGroups.length + exerciseTypes.length];
        System.arraycopy(equipmentCategories, 0, allCategories, 0, equipmentCategories.length);
        System.arraycopy(muscleGroups, 0, allCategories, equipmentCategories.length, muscleGroups.length);
        System.arraycopy(exerciseTypes, 0, allCategories, equipmentCategories.length + muscleGroups.length, exerciseTypes.length);

        return allCategories;
    }


    public static String getIconForCategory(String categoryName) {

        switch (categoryName) {

            case "Без оборудования":
                return "ic_bodyweight";

            case "Бицепс":
                return "ic_biceps";
            case "Грудь":
                return "ic_chest";
            case "Кор":
                return "ic_core";
            case "Ноги":
                return "ic_legs";
            case "Плечи":
                return "ic_shoulders";
            case "Предплечье":
                return "ic_forearms";
            case "Спина":
                return "ic_back";
            case "Трицепс":
                return "ic_triceps";
            case "Ягодицы":
                return "ic_glutes";


            case "кардио":
                return "ic_cardio";
            case "разминка":
                return "ic_warmup";
            case "растяжка":
                return "ic_stretching";
            case "реабилитационное":
                return "ic_rehabilitation";
            case "силовое":
                return "ic_strength";
            case "с собственным весом":
                return "ic_bodyweight";
            case "статическое":
                return "ic_static";
            case "функциональное":
                return "ic_functional";

            default:
                return "ic_exercise_default";
        }
    }


    public static CategoryType getCategoryType(String categoryName) {
        for (String equipmentCategory : getEquipmentCategories()) {
            if (equipmentCategory.equals(categoryName)) {
                return CategoryType.EQUIPMENT;
            }
        }

        for (String muscleGroup : getMuscleGroupCategories()) {
            if (muscleGroup.equals(categoryName)) {
                return CategoryType.MUSCLE_GROUP;
            }
        }

        for (String exerciseType : getExerciseTypeCategories()) {
            if (exerciseType.equals(categoryName)) {
                return CategoryType.EXERCISE_TYPE;
            }
        }


        for (String displayName : getExerciseTypeDisplayNames()) {
            if (displayName.equals(categoryName)) {
                return CategoryType.EXERCISE_TYPE;
            }
        }

        return CategoryType.MUSCLE_GROUP;
    }


    public static String getOriginalNameFromDisplayName(String displayName) {
        String[] originalNames = getExerciseTypeCategories();
        String[] displayNames = getExerciseTypeDisplayNames();

        for (int i = 0; i < displayNames.length; i++) {
            if (displayNames[i].equals(displayName)) {
                return originalNames[i];
            }
        }

        return displayName;
    }
}