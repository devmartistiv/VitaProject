package com.martist.vitamove.programs.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.martist.vitamove.programs.data.local.dao.ProgramDao;
import com.martist.vitamove.programs.data.local.dao.ProgramDayDao;
import com.martist.vitamove.programs.data.local.dao.ProgramExerciseDao;
import com.martist.vitamove.programs.data.local.entities.ProgramDayEntity;
import com.martist.vitamove.programs.data.local.entities.ProgramEntity;
import com.martist.vitamove.programs.data.local.entities.ProgramExerciseEntity;
import com.martist.vitamove.workout.data.cache.WorkoutPlanDao;
import com.martist.vitamove.workout.data.entities.WorkoutPlanEntity;


@Database(
        entities = {
                ProgramEntity.class,
                ProgramDayEntity.class,
                ProgramExerciseEntity.class,
                WorkoutPlanEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class ProgramRoomDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "program_database";
    private static volatile ProgramRoomDatabase INSTANCE;


    public abstract ProgramDao programDao();

    public abstract ProgramDayDao programDayDao();

    public abstract ProgramExerciseDao programExerciseDao();

    public abstract WorkoutPlanDao workoutPlanDao();


    public static ProgramRoomDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ProgramRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ProgramRoomDatabase.class,
                                    DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
} 