package com.martist.vitamove.workout.data.model;

public enum ProgressionType {
    NONE("Нет прогрессии"),
    LINEAR("Линейная"),
    DOUBLE_PROGRESSION("Двойная прогрессия"),
    WAVE_LOADING("Волновая нагрузка"),
    PERCENTAGE_BASED("Процентная");

    private final String displayName;

    ProgressionType(String displayName) {
        this.displayName = displayName;
    }

}