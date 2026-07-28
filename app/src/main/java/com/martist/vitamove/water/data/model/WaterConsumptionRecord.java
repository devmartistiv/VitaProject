package com.martist.vitamove.water.data.model;

import java.util.Date;


public class WaterConsumptionRecord {

    private Date timestamp;
    private float amount;
    private String description;


    public WaterConsumptionRecord(Date timestamp, float amount, String description) {
        this.timestamp = timestamp;
        this.amount = amount;
        this.description = description;
    }


    public Date getTimestamp() {
        return timestamp;
    }


    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    public float getAmount() {
        return amount;
    }


    public void setAmount(float amount) {
        this.amount = amount;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }
} 