package com.martist.vitamove.programs.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.martist.vitamove.exercise.ui.model.Exercise;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ProgramTemplateExercise implements Parcelable {
    private String id;
    private String templateDayId;
    private String exerciseId;
    private String exerciseName;
    private String notes;
    private int orderIndex;
    private int sets;
    private String repsRange;
    private String weightRange;
    private String restTime;
    private boolean isSuperset;
    private String supersetGroupId;
    private int supersetOrder;
    private Date createdAt;
    private Date updatedAt;


    private transient Exercise exercise;


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);


    public ProgramTemplateExercise() {
    }


    public ProgramTemplateExercise(String templateDayId, String exerciseId, String exerciseName,
                                   String notes, int orderIndex, int sets, String repsRange,
                                   String weightRange, String restTime) {
        this.templateDayId = templateDayId;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.notes = notes;
        this.orderIndex = orderIndex;
        this.sets = sets;
        this.repsRange = repsRange;
        this.weightRange = weightRange;
        this.restTime = restTime;
        this.isSuperset = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }


    public static ProgramTemplateExercise fromJson(JSONObject json) throws JSONException, ParseException {
        ProgramTemplateExercise exercise = new ProgramTemplateExercise();
        exercise.id = json.getString("id");
        exercise.templateDayId = json.getString("template_day_id");
        exercise.exerciseId = json.getString("exercise_id");
        exercise.exerciseName = json.optString("exercise_name", "");
        exercise.notes = json.optString("notes", "");
        exercise.orderIndex = json.getInt("order_index");
        exercise.sets = json.getInt("sets");
        exercise.repsRange = json.optString("reps_range", "");
        exercise.weightRange = json.optString("weight_range", "");
        exercise.restTime = json.optString("rest_time", "");
        exercise.isSuperset = json.optBoolean("is_superset", false);
        exercise.supersetGroupId = json.optString("superset_group_id", null);
        exercise.supersetOrder = json.optInt("superset_order", 0);

        if (json.has("created_at") && !json.isNull("created_at")) {
            exercise.createdAt = dateFormat.parse(json.getString("created_at"));
        }

        if (json.has("updated_at") && !json.isNull("updated_at")) {
            exercise.updatedAt = dateFormat.parse(json.getString("updated_at"));
        }

        return exercise;
    }


    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (id != null) {
            json.put("id", id);
        }
        json.put("template_day_id", templateDayId);
        json.put("exercise_id", exerciseId);
        json.put("exercise_name", exerciseName);
        json.put("notes", notes);
        json.put("order_index", orderIndex);
        json.put("sets", sets);
        json.put("reps_range", repsRange);
        json.put("weight_range", weightRange);
        json.put("rest_time", restTime);
        json.put("is_superset", isSuperset);

        if (supersetGroupId != null) {
            json.put("superset_group_id", supersetGroupId);
            json.put("superset_order", supersetOrder);
        }

        if (createdAt != null) {
            json.put("created_at", dateFormat.format(createdAt));
        }

        if (updatedAt != null) {
            json.put("updated_at", dateFormat.format(updatedAt));
        }

        return json;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTemplateDayId() {
        return templateDayId;
    }

    public void setTemplateDayId(String templateDayId) {
        this.templateDayId = templateDayId;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public String getRepsRange() {
        return repsRange;
    }

    public void setRepsRange(String repsRange) {
        this.repsRange = repsRange;
    }

    public String getWeightRange() {
        return weightRange;
    }

    public void setWeightRange(String weightRange) {
        this.weightRange = weightRange;
    }

    public String getRestTime() {
        return restTime;
    }

    public void setRestTime(String restTime) {
        this.restTime = restTime;
    }

    public boolean isSuperset() {
        return isSuperset;
    }

    public void setSuperset(boolean superset) {
        isSuperset = superset;
    }

    public String getSupersetGroupId() {
        return supersetGroupId;
    }

    public void setSupersetGroupId(String supersetGroupId) {
        this.supersetGroupId = supersetGroupId;
    }

    public int getSupersetOrder() {
        return supersetOrder;
    }

    public void setSupersetOrder(int supersetOrder) {
        this.supersetOrder = supersetOrder;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
        if (exercise != null) {
            this.exerciseName = exercise.getName();
        }
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }


    protected ProgramTemplateExercise(Parcel in) {
        id = in.readString();
        templateDayId = in.readString();
        exerciseId = in.readString();
        exerciseName = in.readString();
        notes = in.readString();
        orderIndex = in.readInt();
        sets = in.readInt();
        repsRange = in.readString();
        weightRange = in.readString();
        restTime = in.readString();
        isSuperset = in.readByte() != 0;
        supersetGroupId = in.readString();
        supersetOrder = in.readInt();
        long createdAtTime = in.readLong();
        createdAt = createdAtTime == -1 ? null : new Date(createdAtTime);
        long updatedAtTime = in.readLong();
        updatedAt = updatedAtTime == -1 ? null : new Date(updatedAtTime);
        exercise = in.readParcelable(Exercise.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(templateDayId);
        dest.writeString(exerciseId);
        dest.writeString(exerciseName);
        dest.writeString(notes);
        dest.writeInt(orderIndex);
        dest.writeInt(sets);
        dest.writeString(repsRange);
        dest.writeString(weightRange);
        dest.writeString(restTime);
        dest.writeByte((byte) (isSuperset ? 1 : 0));
        dest.writeString(supersetGroupId);
        dest.writeInt(supersetOrder);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(updatedAt != null ? updatedAt.getTime() : -1);
        dest.writeParcelable(exercise, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProgramTemplateExercise> CREATOR = new Creator<ProgramTemplateExercise>() {
        @Override
        public ProgramTemplateExercise createFromParcel(Parcel in) {
            return new ProgramTemplateExercise(in);
        }

        @Override
        public ProgramTemplateExercise[] newArray(int size) {
            return new ProgramTemplateExercise[size];
        }
    };
} 