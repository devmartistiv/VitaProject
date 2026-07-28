package com.martist.vitamove.exercise.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Exercise implements Parcelable {
    private static final String TAG = "Exercise";
    private final String id;
    private final String name;
    private final String description;
    @SerializedName("primary_muscles")
    private final List<String> muscleGroups;
    @SerializedName("secondary_muscles")
    private final List<String> secondaryMuscles;
    @SerializedName("stabilizer_muscles")
    private final List<String> stabilizerMuscles;
    @SerializedName("equipment_required")
    private final List<String> equipmentRequired;
    private final String difficulty;
    private final String exerciseType;
    private final ExerciseMedia media;
    private final int defaultSets;
    private final String defaultReps;
    private final int defaultRestSeconds;
    private final float met;
    private int durationSeconds;
    private final String instructions;
    @SerializedName("common_mistakes")
    private final List<String> commonMistakes;
    private final List<String> contraindications;
    private final List<String> categories;

    protected Exercise(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        muscleGroups = new ArrayList<>();
        in.readStringList(muscleGroups);
        secondaryMuscles = new ArrayList<>();
        in.readStringList(secondaryMuscles);
        stabilizerMuscles = new ArrayList<>();
        in.readStringList(stabilizerMuscles);
        equipmentRequired = new ArrayList<>();
        in.readStringList(equipmentRequired);
        difficulty = in.readString();
        exerciseType = in.readString();
        media = in.readParcelable(ExerciseMedia.class.getClassLoader());
        defaultSets = in.readInt();
        defaultReps = in.readString();
        defaultRestSeconds = in.readInt();
        met = in.readFloat();
        durationSeconds = in.readInt();
        instructions = in.readString();
        commonMistakes = new ArrayList<>();
        in.readStringList(commonMistakes);
        contraindications = new ArrayList<>();
        in.readStringList(contraindications);
        categories = new ArrayList<>();
        in.readStringList(categories);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeStringList(muscleGroups);
        dest.writeStringList(secondaryMuscles);
        dest.writeStringList(stabilizerMuscles);
        dest.writeStringList(equipmentRequired);
        dest.writeString(difficulty);
        dest.writeString(exerciseType);
        dest.writeParcelable(media, flags);
        dest.writeInt(defaultSets);
        dest.writeString(defaultReps);
        dest.writeInt(defaultRestSeconds);
        dest.writeFloat(met);
        dest.writeInt(durationSeconds);
        dest.writeString(instructions);
        dest.writeStringList(commonMistakes);
        dest.writeStringList(contraindications);
        dest.writeStringList(categories);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Exercise> CREATOR = new Creator<Exercise>() {
        @Override
        public Exercise createFromParcel(Parcel in) {
            return new Exercise(in);
        }

        @Override
        public Exercise[] newArray(int size) {
            return new Exercise[size];
        }
    };


    public Exercise(String id, String name, String description, List<String> muscleGroups,
                    List<String> secondaryMuscles, List<String> stabilizerMuscles,
                    List<String> equipmentRequired, String difficulty, String exerciseType,
                    ExerciseMedia media, int defaultSets, String defaultReps, int defaultRestSeconds,
                    float met, String instructions, List<String> commonMistakes,
                    List<String> contraindications, List<String> categories) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.muscleGroups = muscleGroups != null ? muscleGroups : new ArrayList<>();
        this.secondaryMuscles = secondaryMuscles != null ? secondaryMuscles : new ArrayList<>();
        this.stabilizerMuscles = stabilizerMuscles != null ? stabilizerMuscles : new ArrayList<>();
        this.equipmentRequired = equipmentRequired != null ? equipmentRequired : new ArrayList<>();
        this.difficulty = difficulty;
        this.exerciseType = exerciseType;
        this.media = media;
        this.defaultSets = defaultSets;
        this.defaultReps = defaultReps;
        this.defaultRestSeconds = defaultRestSeconds;
        this.met = met;
        this.instructions = instructions;
        this.commonMistakes = commonMistakes != null ? commonMistakes : new ArrayList<>();
        this.contraindications = contraindications;
        this.categories = categories != null ? categories : new ArrayList<>();


    }


    public Exercise(String id, String name, String description, List<String> muscleGroups,
                    List<String> secondaryMuscles, List<String> stabilizerMuscles,
                    List<String> equipmentRequired, String difficulty, String exerciseType,
                    ExerciseMedia media, int defaultSets, String defaultReps, int defaultRestSeconds,
                    float met, String instructions, List<String> commonMistakes, String category) {
        this(id, name, description, muscleGroups, secondaryMuscles, stabilizerMuscles,
                equipmentRequired, difficulty, exerciseType, media, defaultSets, defaultReps,
                defaultRestSeconds, met, instructions, commonMistakes, null, new ArrayList<>());
    }


    public Exercise(String id, String name, String description, List<String> muscleGroups,
                    List<String> equipmentRequired, String difficulty, String exerciseType,
                    ExerciseMedia media, int defaultSets, String defaultReps, int defaultRestSeconds,
                    float met) {
        this(id, name, description, muscleGroups, new ArrayList<>(), new ArrayList<>(),
                equipmentRequired, difficulty, exerciseType, media, defaultSets, defaultReps,
                defaultRestSeconds, met, null, new ArrayList<>(), null, new ArrayList<>());
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getMuscleGroups() {
        return Objects.requireNonNullElseGet(muscleGroups, ArrayList::new);
    }

    public List<String> getSecondaryMuscles() {
        return secondaryMuscles;
    }

    public List<String> getStabilizerMuscles() {
        return stabilizerMuscles;
    }

    public List<String> getEquipmentRequired() {
        if (equipmentRequired != null)
            return equipmentRequired;
        else return new ArrayList<String>();
    }

    public String getInstructions() {
        return instructions;
    }

    public List<String> getContraindications() {
        return contraindications;
    }

    public List<String> getCategories() {
        return categories;
    }


    public String getCategory() {
        if (categories != null && !categories.isEmpty()) {
            return categories.get(0);
        }
        return "";
    }


    public String getDifficulty() {
        return difficulty;
    }


    public String getDifficultyRussianName() {

        return difficulty;
    }

    public String getExerciseType() {
        return exerciseType;
    }


    public String getExerciseTypeRussianName() {

        return exerciseType;
    }

    public ExerciseMedia getMedia() {
        return media;
    }

    public int getDefaultSets() {
        return defaultSets;
    }

    public String getDefaultReps() {
        return defaultReps;
    }

    public int getDefaultRestSeconds() {
        return defaultRestSeconds;
    }

    public float getMet() {
        return met;
    }


    public List<String> getMuscleGroupRussianNames() {
        if (muscleGroups != null)
            return new ArrayList<>(muscleGroups);
        else
            return new ArrayList<String>();
    }


    public List<String> getSecondaryMuscleRussianNames() {

        return new ArrayList<>(secondaryMuscles);
    }


    public List<String> getStabilizerMuscleRussianNames() {

        return new ArrayList<>(stabilizerMuscles);
    }


    public List<String> getEquipmentRussianNames() {

        return new ArrayList<>(equipmentRequired);
    }


    public String getInstructionsText() {
        return instructions != null ? instructions : "";
    }


    public String getCommonMistakesText() {
        if (commonMistakes == null || commonMistakes.isEmpty()) {
            return "";
        }
        return String.join("\n", commonMistakes);
    }


    public int getDurationSeconds() {
        return durationSeconds;
    }


    public boolean isCardioExercise() {
        if (exerciseType == null) {
            return false;
        }

        String exerciseTypeLower = exerciseType.toLowerCase();
        return exerciseTypeLower.contains("кардио") ||
                exerciseTypeLower.contains("cardio");
    }


    public boolean isStaticExercise() {
        if (exerciseType == null) {
            return false;
        }

        String exerciseTypeLower = exerciseType.toLowerCase();
        return exerciseTypeLower.contains("статич") ||
                exerciseTypeLower.contains("static") ||
                exerciseTypeLower.contains("удержание") ||
                exerciseTypeLower.contains("планка");
    }


    public boolean isFunctionalExercise() {
        if (exerciseType == null) {
            return false;
        }

        String exerciseTypeLower = exerciseType.toLowerCase();
        return exerciseTypeLower.contains("функциональн") ||
                exerciseTypeLower.equals("functional");
    }


    public boolean isBodyweightExercise() {
        if (exerciseType == null) {
            return false;
        }

        String exerciseTypeLower = exerciseType.toLowerCase();
        return exerciseTypeLower.equals("с собственным весом") ||
                exerciseTypeLower.contains("собствен") ||
                exerciseTypeLower.equals("bodyweight");
    }


    public boolean usesTimer() {
        return isCardioExercise() || isStaticExercise();

    }


    public boolean isWarmupOrStretching() {
        if (exerciseType == null) {
            return false;
        }

        String exerciseTypeLower = exerciseType.toLowerCase();
        return exerciseTypeLower.contains("разминк") ||
                exerciseTypeLower.contains("warm") ||
                exerciseTypeLower.contains("растяжк") ||
                exerciseTypeLower.contains("stretch");
    }


    public boolean isRecoveryOrRehabilitation() {
        if (exerciseType == null) {
            return false;
        }

        String exerciseTypeLower = exerciseType.toLowerCase();
        return exerciseTypeLower.contains("восстановительн") ||
                exerciseTypeLower.contains("реабилитационн") ||
                exerciseTypeLower.contains("recovery") ||
                exerciseTypeLower.contains("rehabilitation");
    }


    public boolean usesRepsOnly() {
        return isFunctionalExercise() ||
                isBodyweightExercise() ||
                isRecoveryOrRehabilitation();
    }


    public String getFormattedContraindicationsText() {
        if (contraindications == null || contraindications.isEmpty()) {
            return "";
        }


        StringBuilder formattedText = new StringBuilder();
        for (String contraindication : contraindications) {
            formattedText.append(contraindication).append("\n");

        }


        return formattedText.toString();
    }


    public static class Builder {
        private String id = "";
        private String name = "";
        private String description = "";
        private List<String> muscleGroups = new ArrayList<>();
        private List<String> secondaryMuscles = new ArrayList<>();
        private List<String> stabilizerMuscles = new ArrayList<>();
        private List<String> equipmentRequired = new ArrayList<>();
        private String difficulty = "Легкое";
        private String exerciseType = "Силовое";
        private ExerciseMedia media = new ExerciseMedia();
        private int defaultSets = 3;
        private String defaultReps = "12";
        private int defaultRestSeconds = 60;
        private float met = 0f;
        private String instructions = "";
        private List<String> commonMistakes = new ArrayList<>();
        private List<String> contraindications = new ArrayList<>();
        private List<String> categories = new ArrayList<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }


        public Builder muscleGroups(List<String> muscleGroups) {
            this.muscleGroups = muscleGroups;
            return this;
        }


        public Builder secondaryMuscles(List<String> secondaryMuscles) {
            this.secondaryMuscles = secondaryMuscles;
            return this;
        }


        public Builder stabilizerMuscles(List<String> stabilizerMuscles) {
            this.stabilizerMuscles = stabilizerMuscles;
            return this;
        }


        public Builder equipmentRequired(List<String> equipmentRequired) {
            this.equipmentRequired = equipmentRequired;
            return this;
        }


        public Builder difficulty(String difficulty) {
            this.difficulty = difficulty;
            return this;
        }


        public Builder exerciseType(String exerciseType) {
            this.exerciseType = exerciseType;
            return this;
        }


        public Builder media(ExerciseMedia media) {
            this.media = media;
            return this;
        }

        public Builder defaultSets(int defaultSets) {
            this.defaultSets = defaultSets;
            return this;
        }

        public Builder defaultReps(String defaultReps) {
            this.defaultReps = defaultReps;
            return this;
        }

        public Builder defaultRestSeconds(int defaultRestSeconds) {
            this.defaultRestSeconds = defaultRestSeconds;
            return this;
        }

        public Builder met(float met) {
            this.met = met;
            return this;
        }

        public Builder caloriesPerMinute(float caloriesPerMinute) {

            this.met = (caloriesPerMinute * 200) / (70 * 3.5f);
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder commonMistakes(List<String> commonMistakes) {
            this.commonMistakes = commonMistakes;
            return this;
        }

        public Builder contraindications(List<String> contraindications) {
            this.contraindications = contraindications;
            return this;
        }

        public Builder category(String category) {
            if (category != null && !category.isEmpty()) {
                if (this.categories == null) {
                    this.categories = new ArrayList<>();
                }
                if (!this.categories.contains(category)) {
                    this.categories.add(category);
                }
            }
            return this;
        }

        public Builder categories(List<String> categories) {
            this.categories = categories;
            return this;
        }


        public Exercise build() {
            return new Exercise(
                    id,
                    name,
                    description,
                    muscleGroups,
                    secondaryMuscles,
                    stabilizerMuscles,
                    equipmentRequired,
                    difficulty,
                    exerciseType,
                    media,
                    defaultSets,
                    defaultReps,
                    defaultRestSeconds,
                    met,
                    instructions,
                    commonMistakes,
                    contraindications,
                    categories
            );
        }
    }
} 