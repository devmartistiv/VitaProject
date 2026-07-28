package com.martist.vitamove.measurement;

import java.util.Date;


public class BodyMeasurement {
    private String bodyPart;
    private float currentValue;
    private float previousValue;
    private Date measurementDate;
    private Date previousMeasurementDate;
    private int iconResourceId;
    private boolean isHidden;


    public BodyMeasurement(String bodyPart, float currentValue, int iconResourceId) {
        this.bodyPart = bodyPart;
        this.currentValue = currentValue;
        this.previousValue = 0;
        this.measurementDate = new Date();
        this.iconResourceId = iconResourceId;
        this.isHidden = false;
    }


    public BodyMeasurement(String bodyPart, float currentValue, float previousValue,
                           Date measurementDate, Date previousMeasurementDate, int iconResourceId) {
        this.bodyPart = bodyPart;
        this.currentValue = currentValue;
        this.previousValue = previousValue;
        this.measurementDate = measurementDate;
        this.previousMeasurementDate = previousMeasurementDate;
        this.iconResourceId = iconResourceId;
        this.isHidden = false;
    }


    public String getBodyPart() {
        return bodyPart;
    }

    public void setBodyPart(String bodyPart) {
        this.bodyPart = bodyPart;
    }

    public float getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
    }

    public float getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(float previousValue) {
        this.previousValue = previousValue;
    }

    public Date getMeasurementDate() {
        return measurementDate;
    }

    public void setMeasurementDate(Date measurementDate) {
        this.measurementDate = measurementDate;
    }

    public Date getPreviousMeasurementDate() {
        return previousMeasurementDate;
    }

    public void setPreviousMeasurementDate(Date previousMeasurementDate) {
        this.previousMeasurementDate = previousMeasurementDate;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public void setIconResourceId(int iconResourceId) {
        this.iconResourceId = iconResourceId;
    }


    public float getChange() {
        if (previousValue == 0) {
            return 0;
        }
        return currentValue - previousValue;
    }


    public boolean isPositiveTrend() {
        return getChange() > 0;
    }


    public boolean hasChange() {
        return Math.abs(getChange()) > 0.01f;
    }


    public boolean isHidden() {
        return isHidden;
    }


    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }
}
