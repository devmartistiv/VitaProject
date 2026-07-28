package com.martist.vitamove.measurement;

import java.util.Date;


public class MeasurementRecord {
    private String id;
    private float value;
    private Date date;
    private String note;
    private String bodyPart;


    public MeasurementRecord(String id, float value, Date date, String note) {
        this.id = id;
        this.value = value;
        this.date = date;
        this.note = note;
    }


    public MeasurementRecord(float value, Date date, String note) {
        this(null, value, date, note);
    }


    public MeasurementRecord(float value, Date date) {
        this(null, value, date, "");
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getBodyPart() {
        return bodyPart;
    }

    public void setBodyPart(String bodyPart) {
        this.bodyPart = bodyPart;
    }


    public boolean hasNote() {
        return note != null &&
                !note.trim().isEmpty() &&
                !note.trim().equalsIgnoreCase("null");
    }

    @Override
    public String toString() {
        return "MeasurementRecord{" +
                "value=" + value +
                ", date=" + date +
                ", note='" + note + '\'' +
                '}';
    }
}
