package com.martist.vitamove.core.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.martist.vitamove.core.data.local.converters.DateConverter;
import com.martist.vitamove.core.data.local.converters.ListConverter;
import com.martist.vitamove.exercise.data.local.dao.ExerciseDao;
import com.martist.vitamove.exercise.data.local.entities.ExerciseEntity;
import com.martist.vitamove.nutrition.data.local.dao.DishDao;
import com.martist.vitamove.nutrition.data.local.dao.FavoriteFoodDao;
import com.martist.vitamove.nutrition.data.local.dao.FoodCacheDao;
import com.martist.vitamove.nutrition.data.local.dao.RecentFoodDao;
import com.martist.vitamove.nutrition.data.local.entities.DishEntity;
import com.martist.vitamove.nutrition.data.local.entities.DishIngredientEntity;
import com.martist.vitamove.nutrition.data.local.entities.FavoriteFoodEntity;
import com.martist.vitamove.nutrition.data.local.entities.FoodCacheEntity;
import com.martist.vitamove.nutrition.data.local.entities.RecentFoodEntity;
import com.martist.vitamove.set.ExerciseSetEntity;
import com.martist.vitamove.steps.data.local.dao.StepHistoryDao;
import com.martist.vitamove.steps.data.local.entities.StepHistoryEntity;
import com.martist.vitamove.weight.data.local.dao.UserWeightDao;
import com.martist.vitamove.weight.data.local.entities.UserWeightEntity;
import com.martist.vitamove.workout.data.dao.WorkoutDao;
import com.martist.vitamove.workout.data.entities.UserWorkoutEntity;
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity;

@Database(entities = {
        UserWorkoutEntity.class,
        WorkoutExerciseEntity.class,
        ExerciseSetEntity.class,
        ExerciseEntity.class,
        StepHistoryEntity.class,
        UserWeightEntity.class,
        FavoriteFoodEntity.class,
        DishEntity.class,
        DishIngredientEntity.class,
        RecentFoodEntity.class,
        FoodCacheEntity.class
},
        version = 25,
        exportSchema = false)
@TypeConverters({DateConverter.class, ListConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "vitamove.db";
    private static AppDatabase instance;
    private static Context applicationContext;


    public abstract WorkoutDao workoutDao();

    public abstract ExerciseDao exerciseDao();

    public abstract StepHistoryDao stepHistoryDao();

    public abstract UserWeightDao userWeightDao();

    public abstract FavoriteFoodDao favoriteFoodDao();

    public abstract DishDao dishDao();

    public abstract RecentFoodDao recentFoodDao();

    public abstract FoodCacheDao foodCacheDao();


    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            applicationContext = context.getApplicationContext();
            instance = Room.databaseBuilder(
                            applicationContext,
                            AppDatabase.class,
                            DATABASE_NAME)
                    .addMigrations(
                            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
                            MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12,
                            MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
                            MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
                            MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }


    public static synchronized void resetInstance() {
        if (instance != null) {
            if (instance.isOpen()) {
                instance.close();
            }
            instance = null;
        }
    }


    public static Context getContext() {
        return applicationContext;
    }


    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `step_history` " +
                            "(`date` TEXT NOT NULL, " +
                            "`step_count` INTEGER NOT NULL, " +
                            "`created_at` INTEGER NOT NULL, " +
                            "`updated_at` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`date`))");
        }
    };


    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "ALTER TABLE exercise_sets ADD COLUMN exercise_id TEXT");


            database.execSQL(
                    "UPDATE exercise_sets SET exercise_id = (" +
                            "SELECT base_exercise_id FROM workout_exercises " +
                            "WHERE workout_exercises.id = exercise_sets.workout_exercise_id) " +
                            "WHERE exercise_id IS NULL");
        }
    };


    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "ALTER TABLE exercise_sets ADD COLUMN created_at INTEGER");


            database.execSQL(
                    "UPDATE exercise_sets SET created_at = " + System.currentTimeMillis() +
                            " WHERE created_at IS NULL");
        }
    };


    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_weight_history` (" +
                            "`id` TEXT NOT NULL, " +
                            "`user_id` TEXT, " +
                            "`weight` REAL NOT NULL, " +
                            "`date` INTEGER NOT NULL, " +
                            "`notes` TEXT, " +
                            "`created_at` INTEGER NOT NULL, " +
                            "`updated_at` INTEGER NOT NULL, " +
                            "`is_synced` INTEGER NOT NULL DEFAULT 0, " +
                            "PRIMARY KEY(`id`))");


            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_user_weight_history_user_id` " +
                            "ON `user_weight_history` (`user_id`)");


            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_user_weight_history_date` " +
                            "ON `user_weight_history` (`date`)");
        }
    };


    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

        }
    };


    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL("ALTER TABLE exercises ADD COLUMN categories TEXT");


            database.execSQL(
                    "UPDATE exercises SET categories = '[\"' || category || '\"]' " +
                            "WHERE category IS NOT NULL AND category != ''");


            database.execSQL(
                    "UPDATE exercises SET categories = '[]' " +
                            "WHERE (category IS NULL OR category = '') AND categories IS NULL");
        }
    };


    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {


            database.execSQL(
                    "CREATE TABLE `exercises_new` (" +
                            "`id` TEXT NOT NULL PRIMARY KEY, " +
                            "`name` TEXT, " +
                            "`description` TEXT, " +
                            "`difficulty` TEXT, " +
                            "`exerciseType` TEXT, " +
                            "`met` REAL NOT NULL, " +
                            "`muscleGroups` TEXT, " +
                            "`muscleGroupRussianNames` TEXT, " +
                            "`equipmentRequired` TEXT, " +
                            "`categories` TEXT)");


            database.execSQL(
                    "INSERT INTO `exercises_new` " +
                            "(`id`, `name`, `description`, `difficulty`, `exerciseType`, " +
                            "`met`, `muscleGroups`, `muscleGroupRussianNames`, " +
                            "`equipmentRequired`, `categories`) " +
                            "SELECT `id`, `name`, `description`, `difficulty`, `exerciseType`, " +
                            "`met`, `muscleGroups`, `muscleGroupRussianNames`, " +
                            "`equipmentRequired`, `categories` " +
                            "FROM `exercises`");


            database.execSQL("DROP TABLE `exercises`");


            database.execSQL("ALTER TABLE `exercises_new` RENAME TO `exercises`");
        }
    };


    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL("ALTER TABLE exercises ADD COLUMN instructions TEXT");
        }
    };


    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorite_foods` (" +
                            "`food_id` INTEGER NOT NULL, " +
                            "`food_name` TEXT, " +
                            "`food_category` TEXT, " +
                            "`food_subcategory` TEXT, " +
                            "`calories` REAL NOT NULL, " +
                            "`proteins` REAL NOT NULL, " +
                            "`fats` REAL NOT NULL, " +
                            "`carbs` REAL NOT NULL, " +
                            "`created_at` INTEGER, " +
                            "PRIMARY KEY(`food_id`))"
            );
        }
    };


    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {


            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorite_foods_new` (" +
                            "`food_id` INTEGER NOT NULL, " +
                            "`food_name` TEXT, " +
                            "`food_category` TEXT, " +
                            "`food_subcategory` TEXT, " +
                            "`calories` REAL NOT NULL, " +
                            "`proteins` REAL NOT NULL, " +
                            "`fats` REAL NOT NULL, " +
                            "`carbs` REAL NOT NULL, " +
                            "`created_at` INTEGER, " +
                            "PRIMARY KEY(`food_id`))"
            );


            try {
                database.execSQL(
                        "INSERT INTO `favorite_foods_new` " +
                                "SELECT `food_id`, `food_name`, `food_category`, `food_subcategory`, " +
                                "`calories`, `proteins`, `fats`, `carbs`, `created_at` " +
                                "FROM `favorite_foods`"
                );
            } catch (Exception e) {


            }


            database.execSQL("DROP TABLE IF EXISTS `favorite_foods`");


            database.execSQL("ALTER TABLE `favorite_foods_new` RENAME TO `favorite_foods`");
        }
    };


    static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dishes` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT, " +
                            "`description` TEXT, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`updatedAt` INTEGER NOT NULL)"
            );


            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dish_ingredients` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`dishId` INTEGER NOT NULL, " +
                            "`foodId` INTEGER NOT NULL, " +
                            "`foodName` TEXT, " +
                            "`quantity` REAL NOT NULL, " +
                            "`portionName` TEXT, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`dishId`) REFERENCES `dishes`(`id`) ON DELETE CASCADE)"
            );


            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_dish_ingredients_dishId` " +
                            "ON `dish_ingredients` (`dishId`)"
            );
        }
    };


    static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recent_foods` (" +
                            "`food_id` TEXT NOT NULL PRIMARY KEY, " +
                            "`food_name` TEXT, " +
                            "`food_category` TEXT, " +
                            "`food_subcategory` TEXT, " +
                            "`calories` REAL NOT NULL, " +
                            "`proteins` REAL NOT NULL, " +
                            "`fats` REAL NOT NULL, " +
                            "`carbs` REAL NOT NULL, " +
                            "`last_used_at` INTEGER NOT NULL)"
            );


            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recent_foods_last_used` " +
                            "ON `recent_foods` (`last_used_at` DESC)"
            );
        }
    };


    static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

        }
    };


    static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

        }
    };


    static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

        }
    };


    static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `food_cache` (" +
                            "`id` TEXT NOT NULL PRIMARY KEY, " +
                            "`name` TEXT, " +
                            "`category` TEXT, " +
                            "`subcategory` TEXT, " +
                            "`calories` INTEGER NOT NULL, " +
                            "`proteins` REAL NOT NULL, " +
                            "`fats` REAL NOT NULL, " +
                            "`carbs` REAL NOT NULL, " +
                            "`popularity` INTEGER NOT NULL, " +
                            "`calcium` REAL NOT NULL, " +
                            "`iron` REAL NOT NULL, " +
                            "`magnesium` REAL NOT NULL, " +
                            "`phosphorus` REAL NOT NULL, " +
                            "`potassium` REAL NOT NULL, " +
                            "`sodium` REAL NOT NULL, " +
                            "`zinc` REAL NOT NULL, " +
                            "`vitamin_a` REAL NOT NULL, " +
                            "`vitamin_b1` REAL NOT NULL, " +
                            "`vitamin_b2` REAL NOT NULL, " +
                            "`vitamin_b3` REAL NOT NULL, " +
                            "`vitamin_b5` REAL NOT NULL, " +
                            "`vitamin_b6` REAL NOT NULL, " +
                            "`vitamin_b9` REAL NOT NULL, " +
                            "`vitamin_b12` REAL NOT NULL, " +
                            "`vitamin_c` REAL NOT NULL, " +
                            "`vitamin_d` REAL NOT NULL, " +
                            "`vitamin_e` REAL NOT NULL, " +
                            "`vitamin_k` REAL NOT NULL, " +
                            "`cholesterol` REAL NOT NULL, " +
                            "`saturated_fats` REAL NOT NULL, " +
                            "`trans_fats` REAL NOT NULL, " +
                            "`fiber` REAL NOT NULL, " +
                            "`sugar` REAL NOT NULL, " +
                            "`usefulness_index` INTEGER NOT NULL, " +
                            "`is_liquid` INTEGER NOT NULL, " +
                            "`updated_at` TEXT)"
            );


            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_food_cache_name` " +
                            "ON `food_cache` (`name`)"
            );

            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_food_cache_category` " +
                            "ON `food_cache` (`category`)"
            );

            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_food_cache_popularity` " +
                            "ON `food_cache` (`popularity`)"
            );

            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_food_cache_updated_at` " +
                            "ON `food_cache` (`updated_at`)"
            );
        }
    };


    static final Migration MIGRATION_20_21 = new Migration(20, 21) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL("ALTER TABLE food_cache ADD COLUMN portions TEXT");
        }
    };


    static final Migration MIGRATION_21_22 = new Migration(21, 22) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL("ALTER TABLE food_cache ADD COLUMN is_moderated INTEGER NOT NULL DEFAULT 1");
        }
    };


    static final Migration MIGRATION_22_23 = new Migration(22, 23) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL("ALTER TABLE user_workouts ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 1");


            database.execSQL("ALTER TABLE user_workouts ADD COLUMN local_created_at INTEGER");
        }
    };
} 