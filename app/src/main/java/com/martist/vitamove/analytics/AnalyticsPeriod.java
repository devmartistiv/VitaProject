package com.martist.vitamove.analytics;


public enum AnalyticsPeriod {
    DAY("День"),
    WEEK("Неделя"),
    MONTH("Месяц");

    private final String displayName;

    AnalyticsPeriod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}