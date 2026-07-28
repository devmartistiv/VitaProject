package com.martist.vitamove.workout.data.repository;

import com.martist.vitamove.programs.ui.model.ProgramDay;
import com.martist.vitamove.programs.ui.model.ProgramExercise;
import com.martist.vitamove.workout.data.model.WorkoutProgram;

import java.util.List;


public interface ProgramRepository {


    List<WorkoutProgram> getAllPrograms();


    List<WorkoutProgram> getUserPrograms(String userId) throws Exception;


    List<WorkoutProgram> filterPrograms(String goal, String level, int maxDuration);


    WorkoutProgram getProgramById(String programId);


    WorkoutProgram getActiveProgram(String userId);


    String startProgram(String userId, String programId, long startDate);


    List<ProgramDay> getProgramDays(String programId);


    List<ProgramExercise> getProgramDayExercises(String dayId);


    String createProgramExercise(String dayId, ProgramExercise exercise);


    boolean updateProgram(WorkoutProgram program);


    ProgramDay getProgramDayById(String dayId);


    void deactivateProgram(String programId) throws Exception;


    ProgramExercise getProgramExerciseById(String exerciseId);
} 