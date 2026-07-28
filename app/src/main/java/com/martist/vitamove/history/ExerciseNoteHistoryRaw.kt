package com.martist.vitamove.history

data class ExerciseNoteHistoryRaw(
    var notes: String,
    var start_time: Long = 0,
    var workout_name: String,
    var exercise_id: String,
    var workout_id: String,
)