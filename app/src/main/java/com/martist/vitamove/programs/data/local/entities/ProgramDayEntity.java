package com.martist.vitamove.programs.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(tableName = "program_days",
        foreignKeys = @ForeignKey(
                entity = ProgramEntity.class,
                parentColumns = "id",
                childColumns = "program_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("program_id")})
public class ProgramDayEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @NonNull
    @ColumnInfo(name = "program_id")
    private String programId;

    @ColumnInfo(name = "day_number")
    private int dayNumber;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "template_day_id")
    private String templateDayId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;


    public ProgramDayEntity() {
    }


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getProgramId() {
        return programId;
    }

    public void setProgramId(@NonNull String programId) {
        this.programId = programId;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
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

    public String getTemplateDayId() {
        return templateDayId;
    }

    public void setTemplateDayId(String templateDayId) {
        this.templateDayId = templateDayId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
} 