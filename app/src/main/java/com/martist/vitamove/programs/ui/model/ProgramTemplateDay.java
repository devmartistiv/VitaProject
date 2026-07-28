package com.martist.vitamove.programs.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ProgramTemplateDay implements Parcelable {
    private String id;
    private String templateId;
    private String name;
    private String description;
    private int dayNumber;
    private int weekNumber;
    private String muscleGroups;
    private String focusArea;
    private int estimatedDuration;
    private Date createdAt;
    private Date updatedAt;


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);


    public ProgramTemplateDay() {
    }


    public ProgramTemplateDay(String templateId, String name, String description, int dayNumber,
                              int weekNumber, String muscleGroups, String focusArea, int estimatedDuration) {
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.dayNumber = dayNumber;
        this.weekNumber = weekNumber;
        this.muscleGroups = muscleGroups;
        this.focusArea = focusArea;
        this.estimatedDuration = estimatedDuration;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }


    public static ProgramTemplateDay fromJson(JSONObject json) throws JSONException, ParseException {
        ProgramTemplateDay day = new ProgramTemplateDay();
        day.id = json.getString("id");
        day.templateId = json.getString("template_id");
        day.name = json.getString("name");
        day.description = json.optString("description", "");
        day.dayNumber = json.getInt("day_number");
        day.weekNumber = json.getInt("week_number");
        day.muscleGroups = json.optString("muscle_groups", "");
        day.focusArea = json.optString("focus_area", "");
        day.estimatedDuration = json.optInt("estimated_duration", 0);

        if (json.has("created_at") && !json.isNull("created_at")) {
            day.createdAt = dateFormat.parse(json.getString("created_at"));
        }

        if (json.has("updated_at") && !json.isNull("updated_at")) {
            day.updatedAt = dateFormat.parse(json.getString("updated_at"));
        }

        return day;
    }


    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (id != null) {
            json.put("id", id);
        }
        json.put("template_id", templateId);
        json.put("name", name);
        json.put("description", description);
        json.put("day_number", dayNumber);
        json.put("week_number", weekNumber);
        json.put("muscle_groups", muscleGroups);
        json.put("focus_area", focusArea);
        json.put("estimated_duration", estimatedDuration);

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

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getMuscleGroups() {
        return muscleGroups;
    }

    public void setMuscleGroups(String muscleGroups) {
        this.muscleGroups = muscleGroups;
    }

    public String getFocusArea() {
        return focusArea;
    }

    public void setFocusArea(String focusArea) {
        this.focusArea = focusArea;
    }

    public int getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(int estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
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


    protected ProgramTemplateDay(Parcel in) {
        id = in.readString();
        templateId = in.readString();
        name = in.readString();
        description = in.readString();
        dayNumber = in.readInt();
        weekNumber = in.readInt();
        muscleGroups = in.readString();
        focusArea = in.readString();
        estimatedDuration = in.readInt();
        long createdAtTime = in.readLong();
        createdAt = createdAtTime == -1 ? null : new Date(createdAtTime);
        long updatedAtTime = in.readLong();
        updatedAt = updatedAtTime == -1 ? null : new Date(updatedAtTime);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(templateId);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeInt(dayNumber);
        dest.writeInt(weekNumber);
        dest.writeString(muscleGroups);
        dest.writeString(focusArea);
        dest.writeInt(estimatedDuration);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(updatedAt != null ? updatedAt.getTime() : -1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProgramTemplateDay> CREATOR = new Creator<ProgramTemplateDay>() {
        @Override
        public ProgramTemplateDay createFromParcel(Parcel in) {
            return new ProgramTemplateDay(in);
        }

        @Override
        public ProgramTemplateDay[] newArray(int size) {
            return new ProgramTemplateDay[size];
        }
    };
} 