package com.martist.vitamove.exercise.ui


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martist.vitamove.exercise.data.repo.ExerciseRepository
import com.martist.vitamove.exercise.ui.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExercisesViewModel @Inject constructor(val exerciseRepository: ExerciseRepository) :
    ViewModel() {
    val exercisesLiveData = MutableLiveData<List<Exercise>>()
    val exercisesByIdsLiveData = MutableLiveData<List<Exercise>>()
    val exerciseLiveData = MutableLiveData<Exercise>()
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>(null)

    fun getAllExercises() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                exercisesLiveData.value = withContext(Dispatchers.IO) {
                    exerciseRepository.getAllExercises()
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun getExercisesByIds(exerciseIds: List<String>) {
        viewModelScope.launch() {
            try {
                isLoading.value = true
                exercisesByIdsLiveData.value = withContext(Dispatchers.IO) {
                    exerciseRepository.getExercisesByIds(exerciseIds)
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun getExercisesByIds2(
        exerciseIds: List<String>,
        onResult: (List<Exercise>) -> Unit
    ) {
        viewModelScope.launch {
            val exercises = withContext(Dispatchers.IO) {
                exerciseRepository.getExercisesByIds(exerciseIds)
            }

            onResult(exercises)
        }
    }

    fun getExerciseById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {

            exerciseLiveData.value = exerciseRepository.getExerciseById(id)
        }
    }


    fun isExercisesInRoomRelevance(): Boolean {
        return exerciseRepository.isExercisesInRoomRelevance()
    }

    fun getAllExercisesFromRemote() {
        viewModelScope.launch(Dispatchers.IO) {
            exerciseRepository.getAllExercisesFromRemote()
        }
    }
}