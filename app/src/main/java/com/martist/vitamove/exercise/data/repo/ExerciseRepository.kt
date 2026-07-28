package com.martist.vitamove.exercise.data.repo

import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.martist.vitamove.VitaMoveApplication
import com.martist.vitamove.core.data.local.AppDatabase
import com.martist.vitamove.core.data.remote.SupabaseClient
import com.martist.vitamove.core.domain.utils.Constants
import com.martist.vitamove.exercise.data.local.mappers.ExerciseDBToExercise
import com.martist.vitamove.exercise.data.local.mappers.ExerciseToExerciseDB
import com.martist.vitamove.exercise.ui.model.Exercise
import org.json.JSONArray
import javax.inject.Inject


class ExerciseRepository @Inject constructor() {
    private val PREFS_NAME: String = "ExerciseListCache"
    val sharedPreferences = VitaMoveApplication.context.getSharedPreferences(
        PREFS_NAME,
        android.content.Context.MODE_PRIVATE
    )

    val CACHE_EXPIRATION_TIME: Long = (24 * 60 * 60 * 1000).toLong()
    val exerciseDao = AppDatabase.getInstance(VitaMoveApplication.context).exerciseDao();
    val exerciseDbToExercise = ExerciseDBToExercise()
    val exerciseToExerciseDB = ExerciseToExerciseDB()


    val supabaseClient: SupabaseClient = SupabaseClient.getInstance(
        Constants.SUPABASE_CLIENT_ID,
        Constants.SUPABASE_CLIENT_SECRET
    )

    suspend fun getAllExercises(): List<Exercise> {
        val exercises = exerciseDao.getAllExercises()

        if (!exercises.isEmpty() && isExercisesInRoomRelevance()) {
            val cachedExercises = mutableListOf<Exercise>()

            for (entity in exercises) {
                val exercise = exerciseDbToExercise(entity)
                cachedExercises.add(exercise)
            }
            return cachedExercises
        }
        return getAllExercisesFromRemote()


    }

    fun getExercisesByIds(exerciseIds: List<String>) =
        exerciseDao.getExercisesByIds(exerciseIds).map { exerciseDbToExercise(it) }

    fun getExerciseById(id: String) =
        exerciseDbToExercise(exerciseDao.getExerciseById(id))

    suspend fun getAllExercisesFromRemote(): List<Exercise> {
        var exercises: List<Exercise> = emptyList()
        try {
            val exercisesArray: JSONArray = supabaseClient.from("exercises")
                .select("*")
                .executeAndGetArray()
            val gson = Gson()

            val type = object : TypeToken<List<Exercise>>() {}.type

            exercises = gson.fromJson(
                exercisesArray.toString(),
                type
            )

            saveExercisesToRoom(exercises)
        } catch (e: Exception) {

        }

        sharedPreferences.edit { putLong("last_update_time", System.currentTimeMillis()) }
        return exercises

    }

    suspend fun saveExercisesToRoom(exercises: List<Exercise>) =
        exerciseDao.insertExercises(exercises.map { exercise -> exerciseToExerciseDB(exercise) })


    fun isExercisesInRoomRelevance(): Boolean {

        val lastUpdateTime: Long =
            sharedPreferences.getLong("last_update_time", 0)
        if (lastUpdateTime == 0L)
            return false
        val currentTime = System.currentTimeMillis()

        return currentTime - lastUpdateTime < CACHE_EXPIRATION_TIME

    }
}