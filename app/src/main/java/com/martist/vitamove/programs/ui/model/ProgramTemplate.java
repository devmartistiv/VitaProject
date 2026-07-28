package com.martist.vitamove.programs.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ProgramTemplate implements Parcelable {
    private String id;
    private String name;
    private String description;
    private String authorId;
    private String authorName;
    private boolean isPublic;
    private String category;
    private int durationWeeks;
    private int daysPerWeek;
    private String difficulty;
    private String imageUrl;
    private int likes;
    private Date createdAt;
    private Date updatedAt;


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);


    public ProgramTemplate() {
    }


    public ProgramTemplate(String name, String description, String authorId, boolean isPublic,
                           String category, int durationWeeks, int daysPerWeek, String difficulty) {
        this.name = name;
        this.description = description;
        this.authorId = authorId;
        this.isPublic = isPublic;
        this.category = category;
        this.durationWeeks = durationWeeks;
        this.daysPerWeek = daysPerWeek;
        this.difficulty = difficulty;
        this.likes = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }


    public static ProgramTemplate fromJson(JSONObject json) throws JSONException, ParseException {
        ProgramTemplate template = new ProgramTemplate();
        template.id = json.getString("id");
        template.name = json.getString("name");
        template.description = json.optString("description", "");
        template.authorId = json.getString("author_id");
        template.authorName = json.optString("author_name", "");
        template.isPublic = json.getBoolean("is_public");
        template.category = json.optString("category", "");
        template.durationWeeks = json.optInt("duration_weeks", 0);
        template.daysPerWeek = json.optInt("days_per_week", 0);
        template.difficulty = json.optString("difficulty", "");
        template.imageUrl = json.optString("image_url", "");
        template.likes = json.optInt("likes", 0);

        if (json.has("created_at") && !json.isNull("created_at")) {
            template.createdAt = dateFormat.parse(json.getString("created_at"));
        }

        if (json.has("updated_at") && !json.isNull("updated_at")) {
            template.updatedAt = dateFormat.parse(json.getString("updated_at"));
        }

        return template;
    }


    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (id != null) {
            json.put("id", id);
        }
        json.put("name", name);
        json.put("description", description);
        json.put("author_id", authorId);
        json.put("is_public", isPublic);
        json.put("category", category);
        json.put("duration_weeks", durationWeeks);
        json.put("days_per_week", daysPerWeek);
        json.put("difficulty", difficulty);
        json.put("image_url", imageUrl);
        json.put("likes", likes);

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

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getDurationWeeks() {
        return durationWeeks;
    }

    public void setDurationWeeks(int durationWeeks) {
        this.durationWeeks = durationWeeks;
    }

    public int getDaysPerWeek() {
        return daysPerWeek;
    }

    public void setDaysPerWeek(int daysPerWeek) {
        this.daysPerWeek = daysPerWeek;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
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


    protected ProgramTemplate(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        authorId = in.readString();
        authorName = in.readString();
        isPublic = in.readByte() != 0;
        category = in.readString();
        durationWeeks = in.readInt();
        daysPerWeek = in.readInt();
        difficulty = in.readString();
        imageUrl = in.readString();
        likes = in.readInt();
        long createdAtTime = in.readLong();
        createdAt = createdAtTime == -1 ? null : new Date(createdAtTime);
        long updatedAtTime = in.readLong();
        updatedAt = updatedAtTime == -1 ? null : new Date(updatedAtTime);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(authorId);
        dest.writeString(authorName);
        dest.writeByte((byte) (isPublic ? 1 : 0));
        dest.writeString(category);
        dest.writeInt(durationWeeks);
        dest.writeInt(daysPerWeek);
        dest.writeString(difficulty);
        dest.writeString(imageUrl);
        dest.writeInt(likes);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(updatedAt != null ? updatedAt.getTime() : -1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProgramTemplate> CREATOR = new Creator<ProgramTemplate>() {
        @Override
        public ProgramTemplate createFromParcel(Parcel in) {
            return new ProgramTemplate(in);
        }

        @Override
        public ProgramTemplate[] newArray(int size) {
            return new ProgramTemplate[size];
        }
    };
} 