package com.martist.vitamove.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.credentials.CredentialManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.martist.vitamove.R;
import com.martist.vitamove.auth.OnboardingActivity;
import com.martist.vitamove.core.data.local.AppDatabase;
import com.martist.vitamove.core.data.local.MealsDatabase;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.core.ui.BaseActivity;
import com.martist.vitamove.nutrition.data.managers.CaloriesManager;
import com.martist.vitamove.nutrition.data.managers.DashboardManager;
import com.martist.vitamove.nutrition.data.managers.FoodManager;
import com.martist.vitamove.water.data.WaterHistoryManager;
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository;
import com.martist.vitamove.workout.data.repository.WorkoutRepository;

import java.io.File;
import java.lang.reflect.Field;


public class SettingsActivity extends BaseActivity {


    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        credentialManager = CredentialManager.create(this);


        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Настройки");

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }


        MaterialButton logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logout();
                }
            });
        }
    }


    private void logout() {
        Log.d("SettingsActivity", "Начало процесса выхода из аккаунта");


        androidx.appcompat.app.AlertDialog loadingDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_logout)
                .setCancelable(false)
                .create();
        loadingDialog.show();


        TextView messageTextView = null;
        try {
            messageTextView = loadingDialog.findViewById(R.id.logout_message);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Не удалось найти TextView в диалоге: " + e.getMessage(), e);
        }


        final TextView finalMessageTextView = messageTextView;


        SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings_container);
        if (fragment != null) {
            fragment.unregisterListener();
            Log.d("SettingsActivity", "Слушатель в SettingsFragment успешно отписан перед выходом.");
        }


        SharedPreferences appPrefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
        String userId = appPrefs.getString("userId", null);


        if (userId != null) {
            updateLoadingMessage(finalMessageTextView, "Удаление незавершенных тренировок...");
            new Thread(() -> {
                try {
                    WorkoutRepository workoutRepository = new SupabaseWorkoutRepository(SupabaseClient.getInstance(SupabaseClient.SUPABASE_URL, Constants.SUPABASE_CLIENT_SECRET));
                    workoutRepository.deleteUnfinishedWorkouts(userId);
                    Log.d("SettingsActivity", "Незавершенные тренировки успешно удалены.");
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при удалении незавершенных тренировок: " + e.getMessage(), e);

                }
            }).start();
        }


        updateLoadingMessage(finalMessageTextView, "Закрытие активных соединений с базами данных...");


        updateLoadingMessage(finalMessageTextView, "Удаление служебных файлов...");
        removeMealsDatabaseSaveTags();


        updateLoadingMessage(finalMessageTextView, "Очистка настроек приложения...");
        clearSharedPreferences();


        updateLoadingMessage(finalMessageTextView, "Сброс менеджеров данных...");
        resetManagers();


        updateLoadingMessage(finalMessageTextView, "Очистка баз данных...");
        clearRoomDatabases(userId);


        updateLoadingMessage(finalMessageTextView, "Сброс авторизационных токенов...");
        clearSupabaseTokens();


        updateLoadingMessage(finalMessageTextView, "Очистка кэшей приложения...");
        try {

            clearApplicationData();
            Log.d("SettingsActivity", "Кэш приложения очищен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке кэша приложения: " + e.getMessage(), e);
        }


        updateLoadingMessage(finalMessageTextView, "Финальная очистка данных...");
        try {
            clearApplicationDataViaContentProvider();
            Log.d("SettingsActivity", "Данные ContentProvider очищены");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке через ContentProvider: " + e.getMessage(), e);
        }


        updateLoadingMessage(finalMessageTextView, "Выход завершен. Возврат на экран входа...");
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        Log.d("SettingsActivity", "Успешный выход из аккаунта, все данные очищены");


        try {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при закрытии диалога: " + e.getMessage(), e);
        }


        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void clearApplicationDataViaContentProvider() {
        try {

            getContentResolver().call(
                    android.net.Uri.parse("content://" + getPackageName() + ".provider"),
                    "clearAllTables",
                    null,
                    null
            );
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке через ContentProvider: " + e.getMessage(), e);
        }
    }


    private void clearSharedPreferences() {
        try {

            String[] prefsFiles = {
                    "user_data",
                    "VitaMovePrefs",
                    "workout_history_cache",
                    "calories_data",
                    "water_history_prefs",
                    "dashboard_prefs",
                    "FoodManagerPrefs",
                    "food_meal_history"
            };

            Log.d("SettingsActivity", "Очистка SharedPreferences");


            for (String prefName : prefsFiles) {
                SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                try {
                    editor.apply();
                    Log.d("SettingsActivity", "Очищены настройки: " + prefName);
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при очистке настроек " + prefName + ": " + e.getMessage(), e);

                    editor = prefs.edit();
                    editor.clear();
                    editor.apply();
                }
            }


            safelyClearDefaultPreferences();


            File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
            if (prefsDir.exists() && prefsDir.isDirectory()) {
                File[] preferenceFiles = prefsDir.listFiles();
                if (preferenceFiles != null) {
                    for (File file : preferenceFiles) {
                        try {
                            String filename = file.getName();
                            if (filename.endsWith(".xml")) {

                                if (file.delete()) {
                                    Log.d("SettingsActivity", "Удален файл настроек: " + filename);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("SettingsActivity", "Ошибка при удалении файла настроек: " + e.getMessage(), e);
                        }
                    }
                }
            }


            try {

                SharedPreferences vmPrefs = getSharedPreferences("VitaMovePrefs", MODE_PRIVATE);
                SharedPreferences.Editor vmEditor = vmPrefs.edit();
                vmEditor.clear();
                vmEditor.putBoolean("isLogged", false);
                vmEditor.apply();
            } catch (Exception e) {
                Log.e("SettingsActivity", "Ошибка при сбросе VitaMovePrefs: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке SharedPreferences: " + e.getMessage(), e);
        }
    }


    private void safelyClearDefaultPreferences() {
        try {
            SharedPreferences defaultPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = defaultPrefs.edit();
            editor.clear();
            editor.apply();
            Log.d("SettingsActivity", "Стандартные настройки успешно очищены.");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при безопасной очистке стандартных настроек: " + e.getMessage(), e);
        }
    }


    private void resetManagers() {
        Log.d("SettingsActivity", "Начало сброса менеджеров данных и баз данных");


        try {
            AppDatabase.resetInstance();
            Log.d("SettingsActivity", "AppDatabase (vitamove.db) успешно сброшен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе AppDatabase: " + e.getMessage(), e);
        }


        try {
            FoodManager.resetInstance();
            Log.d("SettingsActivity", "FoodManager успешно сброшен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе FoodManager: " + e.getMessage(), e);
        }


        try {
            CaloriesManager.resetInstance();
            Log.d("SettingsActivity", "CaloriesManager успешно сброшен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе CaloriesManager: " + e.getMessage(), e);
        }


        try {
            FoodManager.resetInstance();
            Log.d("SettingsActivity", "FoodManager повторно сброшен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при повторном сбросе FoodManager: " + e.getMessage(), e);
        }


        try {
            DashboardManager.resetInstance();
            Log.d("SettingsActivity", "DashboardManager успешно сброшен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе DashboardManager: " + e.getMessage(), e);
        }


        try {
            WaterHistoryManager.resetInstance();
            Log.d("SettingsActivity", "WaterHistoryManager успешно сброшен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе WaterHistoryManager: " + e.getMessage(), e);
        }


        try {
            resetMealsDatabase();
            Log.d("SettingsActivity", "MealsDatabase успешно сброшен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе MealsDatabase: " + e.getMessage(), e);
        }


        try {

            Runtime.getRuntime().gc();
            Log.d("SettingsActivity", "Запущен сборщик мусора для освобождения памяти");


            System.runFinalization();
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке кэшей: " + e.getMessage(), e);
        }

        Log.d("SettingsActivity", "Все доступные менеджеры данных сброшены");
    }


    private void resetMealsDatabase() {
        try {

            Class<?> mealsDatabaseClass = Class.forName("com.martist.vitamove.core.data.local.MealsDatabase");


            Field instanceField = mealsDatabaseClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);


            instanceField.set(null, null);
            Log.d("SettingsActivity", "Синглтон MealsDatabase сброшен через рефлексию");


            try {
                java.lang.reflect.Method resetMethod = mealsDatabaseClass.getDeclaredMethod("resetInstance");
                resetMethod.setAccessible(true);
                resetMethod.invoke(null);
                Log.d("SettingsActivity", "Метод resetInstance() в MealsDatabase успешно вызван");
            } catch (NoSuchMethodException e) {

                Log.d("SettingsActivity", "Метод resetInstance() в MealsDatabase не найден");
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе MealsDatabase: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }


    private void clearRoomDatabases(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w("SettingsActivity", "ID пользователя не определен, пропускаем очистку баз данных");
            return;
        }

        try {


            try {
                Log.d("SettingsActivity", "Принудительная очистка meals_database...");


                try {
                    MealsDatabase mealsDatabase =
                            MealsDatabase.getInstance(getApplicationContext());
                    mealsDatabase.mealDao().deleteAllMealsForUser(userId);


                    MealsDatabase.resetInstance();

                    Log.d("SettingsActivity", "Таблица meals очищена через DAO для пользователя: " + userId);
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при очистке meals через DAO: " + e.getMessage(), e);
                }


                android.database.sqlite.SQLiteDatabase mealsDb = getApplication().openOrCreateDatabase("meals_database", MODE_PRIVATE, null);
                try {

                    mealsDb.execSQL("DELETE FROM meals");


                    mealsDb.execSQL("DELETE FROM meals WHERE user_id=?", new String[]{userId});


                    mealsDb.execSQL("VACUUM");
                    Log.d("SettingsActivity", "Таблица meals успешно очищена через SQL");
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при очистке таблицы meals: " + e.getMessage(), e);
                } finally {
                    mealsDb.close();
                }


                File mealsDbFile = getDatabasePath("meals_database");
                if (mealsDbFile.exists()) {
                    boolean deleted = mealsDbFile.delete();
                    if (deleted) {
                        Log.d("SettingsActivity", "Файл meals_database успешно удален");
                    } else {
                        Log.e("SettingsActivity", "Не удалось удалить файл meals_database");


                        boolean deletedAlt = getApplicationContext().deleteDatabase("meals_database");
                        Log.d("SettingsActivity", "Удаление через context.deleteDatabase: " + (deletedAlt ? "успешно" : "неудачно"));
                    }
                }


                File mealsDbJournal = new File(mealsDbFile.getPath() + "-journal");
                if (mealsDbJournal.exists()) {
                    mealsDbJournal.delete();
                }

                File mealsDbShm = new File(mealsDbFile.getPath() + "-shm");
                if (mealsDbShm.exists()) {
                    mealsDbShm.delete();
                }

                File mealsDbWal = new File(mealsDbFile.getPath() + "-wal");
                if (mealsDbWal.exists()) {
                    mealsDbWal.delete();
                }

                Log.d("SettingsActivity", "Принудительная очистка meals_database завершена");
            } catch (Exception e) {
                Log.e("SettingsActivity", "Ошибка при принудительной очистке meals_database: " + e.getMessage(), e);
            }


            String[] databases = {
                    "meals.db", "food_cache.db", "exercise_cache.db",
                    "workout_database.db", "user_cache.db", "program_cache.db",
                    "workout_cache.db", "calories.db", "vitamove.db",
                    "WorkName", "WorkProgress", "WorkSpec", "WorkTag",
                    "WorkManager", "room_master_table", "program_database.db",
                    "meals_database.db"
            };

            for (String dbName : databases) {
                try {
                    Log.d("SettingsActivity", "Очищаем таблицы в базе данных: " + dbName);
                    android.database.sqlite.SQLiteDatabase db = getApplication().openOrCreateDatabase(dbName, MODE_PRIVATE, null);


                    android.database.Cursor cursor = db.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' AND name!='android_metadata' AND name!='sqlite_sequence'",
                            null
                    );

                    while (cursor.moveToNext()) {
                        String tableName = cursor.getString(0);
                        try {
                            db.execSQL("DELETE FROM " + tableName);
                            Log.d("SettingsActivity", "Таблица " + tableName + " в базе " + dbName + " очищена");
                        } catch (Exception e) {
                            Log.e("SettingsActivity", "Ошибка при очистке таблицы " + tableName + ": " + e.getMessage());
                        }
                    }

                    cursor.close();


                    try {
                        db.execSQL("VACUUM");
                    } catch (Exception e) {
                        Log.e("SettingsActivity", "Ошибка при сжатии базы данных " + dbName + ": " + e.getMessage());
                    }

                    db.close();
                    Log.d("SettingsActivity", "База данных " + dbName + " обработана");
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при работе с базой данных " + dbName + ": " + e.getMessage(), e);
                }
            }


            try {
                clearSpecificSharedPreferences("calories_data");
            } catch (Exception e) {
                Log.e("SettingsActivity", "Ошибка при очистке SharedPreferences calories_data: " + e.getMessage(), e);
            }


            for (String dbName : databases) {
                try {

                    boolean deleted = getApplication().getDatabasePath(dbName).delete();
                    if (deleted) {
                        Log.d("SettingsActivity", "База данных " + dbName + " удалена");
                    } else {
                        Log.d("SettingsActivity", "База данных " + dbName + " не существует или не может быть удалена");


                        boolean deletedAlt = getApplicationContext().deleteDatabase(dbName);
                        if (deletedAlt) {
                            Log.d("SettingsActivity", "База данных " + dbName + " удалена альтернативным методом");
                        }
                    }


                    getApplication().getDatabasePath(dbName + "-journal").delete();
                    getApplication().getDatabasePath(dbName + "-shm").delete();
                    getApplication().getDatabasePath(dbName + "-wal").delete();
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при удалении базы данных " + dbName + ": " + e.getMessage(), e);
                }
            }


            try {

                File databaseDir = getApplication().getDatabasePath("dummy").getParentFile();
                if (databaseDir != null && databaseDir.exists() && databaseDir.isDirectory()) {

                    File[] databaseFiles = databaseDir.listFiles();
                    if (databaseFiles != null) {
                        for (File file : databaseFiles) {
                            String fileName = file.getName();

                            if (file.isFile() &&
                                    (fileName.endsWith(".db") ||
                                            fileName.contains("-journal") ||
                                            fileName.contains("-shm") ||
                                            fileName.contains("-wal") ||
                                            fileName.contains(".sqlite"))) {
                                try {
                                    boolean success = file.delete();
                                    Log.d("SettingsActivity", "Файл " + fileName + " " + (success ? "удален" : "не удален"));
                                } catch (Exception e) {
                                    Log.e("SettingsActivity", "Ошибка при удалении файла " + fileName + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SettingsActivity", "Ошибка при очистке директории баз данных: " + e.getMessage());
            }

            Log.d("SettingsActivity", "Все базы данных удалены");

        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке баз данных Room: " + e.getMessage(), e);
        }
    }


    private void clearSpecificSharedPreferences(String prefsName) {
        try {
            SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.commit();
            Log.d("SettingsActivity", "SharedPreferences " + prefsName + " очищены");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке SharedPreferences " + prefsName + ": " + e.getMessage(), e);
        }
    }


    private void clearSupabaseTokens() {
        try {

            SupabaseClient supabaseClient =
                    SupabaseClient.getInstance(
                            Constants.SUPABASE_CLIENT_ID,
                            Constants.SUPABASE_CLIENT_SECRET
                    );


            supabaseClient.clearCredentialState(credentialManager);

            Log.d("SettingsActivity", "Токены Supabase сброшены и состояние учетных данных очищено");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при сбросе токенов и очистке состояния учетных данных: " + e.getMessage(), e);
        }
    }


    private void clearApplicationData() {
        try {

            File cache = getCacheDir();
            deleteDir(cache);


            if (getExternalCacheDir() != null) {
                deleteDir(getExternalCacheDir());
            }

            Log.d("SettingsActivity", "Кэш приложения очищен");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при очистке кэша: " + e.getMessage(), e);
        }
    }


    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }


    private void removeMealsDatabaseSaveTags() {
        try {
            Log.d("SettingsActivity", "Удаление тегов сохранения данных meals_database...");


            File roomSharedPrefs = new File(getApplicationInfo().dataDir + "/shared_prefs/androidx.room.RoomSharedPreferences.xml");
            if (roomSharedPrefs.exists() && roomSharedPrefs.delete()) {
                Log.d("SettingsActivity", "Удален файл настроек Room: " + roomSharedPrefs.getName());
            }


            File cacheDir = getCacheDir();
            File[] cacheFiles = cacheDir.listFiles((dir, name) ->
                    name.contains("meals") || name.contains("room") || name.contains("food"));

            if (cacheFiles != null) {
                for (File file : cacheFiles) {
                    if (file.delete()) {
                        Log.d("SettingsActivity", "Удален кэш-файл: " + file.getName());
                    }
                }
            }


            Log.d("SettingsActivity", "Удаление тегов сохранения данных meals_database завершено");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при удалении тегов сохранения: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateLoadingMessage(final TextView messageTextView, final String message) {
        if (messageTextView == null) {
            Log.d("SettingsActivity", "Пропуск обновления сообщения (TextView = null): " + message);
            return;
        }

        try {

            runOnUiThread(() -> {
                try {
                    messageTextView.setText(message);
                    Log.d("SettingsActivity", "Обновлено сообщение в диалоге: " + message);
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Ошибка при обновлении сообщения в диалоге: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e("SettingsActivity", "Ошибка при вызове runOnUiThread: " + e.getMessage(), e);
        }
    }

} 