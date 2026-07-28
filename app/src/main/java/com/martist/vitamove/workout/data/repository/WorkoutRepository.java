package com.martist.vitamove.workout.data.repository;

import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.data.model.WorkoutExercise;
import com.martist.vitamove.workout.data.model.WorkoutPlan;

import java.util.List;


public interface WorkoutRepository {

    String createWorkout(String userId) throws Exception;

    void deleteWorkout(String workoutId) throws Exception;

    void deleteUnfinishedWorkouts(String userId) throws Exception;


    void updateWorkoutStartTime(String workoutId, long startTime) throws Exception;

    String createSuperset(List<WorkoutExercise> selectedExercises) throws Exception;


    String addExerciseToWorkout(String workoutId, String exerciseId, int orderNumber) throws Exception;


    void updateExerciseOrderNumber(String exerciseId, int orderNumber) throws Exception;


    void updateExerciseNote(String exerciseId, String notes) throws Exception;


    String addSet(String workoutExerciseId, ExerciseSet set) throws Exception;

    void updateSet(String setId, ExerciseSet set) throws Exception;

    void deleteSet(String setId) throws Exception;


    List<ExerciseSet> getExerciseSets(String workoutExerciseId) throws Exception;


    Float getLastWeightForExercise(String exerciseId);


    Integer getLastRepsForExercise(String exerciseId);


    List<UserWorkout> getWorkoutHistory(String userId, long startTime, long endTime, int offset, int limit) throws Exception;


    Exercise getExerciseById(String exerciseId) throws Exception;


    List<Exercise> getAllExercises() throws Exception;


    List<ExerciseSet> getExerciseSetsHistoryById(String exerciseId) throws Exception;


    void updateWorkoutPlanStatus(String planId, String newStatus) throws Exception;


    void updateWorkoutPlan(WorkoutPlan plan) throws Exception;

    List<WorkoutPlan> getWorkoutPlansByDateRange(String userId, long startDate, long endDate) throws Exception;


    WorkoutPlan getTodayWorkoutPlan(String userId) throws Exception;

    List<UserWorkout> getRecentWorkouts(String userId, int limit) throws Exception;


    UserWorkout getWorkoutById(String workoutId) throws Exception;


    String createWorkoutFromPlan(WorkoutPlan plan) throws Exception;


    void loadWorkoutExercises(UserWorkout workout) throws Exception;


    WorkoutPlan getWorkoutPlanById(String id) throws Exception;


    void saveCompletedWorkout(UserWorkout workout) throws Exception;
}