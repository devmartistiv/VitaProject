package com.martist.vitamove.weight.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.martist.vitamove.core.data.local.converters.DateConverter;

import java.util.Date;
import java.util.UUID;


@Entity(tableName = "user_weight_history",
        indices = {@Index(value = {"date"}), @Index(value = {"user_id"})})
@TypeConverters(DateConverter.class)
public class UserWeightEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "weight")
    private float weight;

    @ColumnInfo(name = "date")
    private long date;

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    private boolean isSynced;


    public UserWeightEntity() {
    }


    public UserWeightEntity(String userId, float weight, Date date, String notes) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.weight = weight;
        this.date = date.getTime();
        this.notes = notes;
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.isSynced = false;
    }


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
        this.updatedAt = System.currentTimeMillis();
    }


    public long getDate() {
        return date;
    }


    public void setDate(long date) {
        this.date = date;
        this.updatedAt = System.currentTimeMillis();
    }


    public Date getDateAsDate() {
        return new Date(date);
    }

    public void setDateFromDate(Date date) {
        this.date = date.getTime();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
} 