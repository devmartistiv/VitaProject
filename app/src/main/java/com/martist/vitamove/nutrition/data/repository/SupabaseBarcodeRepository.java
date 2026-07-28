package com.martist.vitamove.nutrition.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.martist.vitamove.core.data.remote.SupabaseClient;
import com.martist.vitamove.nutrition.data.dto.FoodDto;
import com.martist.vitamove.nutrition.ui.model.Food;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;


public class SupabaseBarcodeRepository {
    private static final String TAG = "SupabaseBarcodeRepo";
    private final SupabaseClient supabaseClient;
    private final Context context;

    public SupabaseBarcodeRepository(SupabaseClient supabaseClient, Context context) {
        this.supabaseClient = supabaseClient;
        this.context = context;
    }


    public Food findFoodByBarcode(String barcode) {
        try {
            Log.d(TAG, "Поиск продукта по штрихкоду: " + barcode);


            JSONArray results = null;
            int maxRetries = 3;
            int currentRetry = 0;


            while (currentRetry < maxRetries && results == null) {
                try {
                    results = supabaseClient.rpc("get_food_by_barcode")
                            .param("barcode_param", barcode)
                            .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {

                    Log.d(TAG, "Токен обновлен, повторяем запрос поиска по штрихкоду. Попытка " + (currentRetry + 1));
                    currentRetry++;

                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос по штрихкоду после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }

            if (results == null) {
                Log.e(TAG, "Не удалось получить результаты поиска по штрихкоду после обновления токена");
                return null;
            }


            if (results.length() > 0) {
                JSONObject foodJson = results.getJSONObject(0);
                Food food = parseFoodFromJson(foodJson);
                Log.d(TAG, "Найден продукт по штрихкоду: " + food.getName());
                return food;
            } else {
                Log.d(TAG, "Продукт с штрихкодом " + barcode + " не найден в базе данных");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при поиске продукта по штрихкоду: " + e.getMessage(), e);
            return null;
        }
    }


    public boolean addBarcode(String barcode, Food food) {
        try {
            if (barcode == null || barcode.trim().isEmpty()) {
                Log.e(TAG, "Ошибка: пустой штрихкод");
                return false;
            }

            if (food == null) {
                Log.e(TAG, "Ошибка: продукт равен null");
                return false;
            }

            String foodId = food.getId();
            if (foodId == null || foodId.trim().isEmpty()) {
                Log.e(TAG, "Ошибка: ID продукта пустой или равен null. Название продукта: " + food.getName());
                return false;
            }

            Log.d(TAG, "Добавление штрихкода: " + barcode + " для продукта: " + food.getName() + " (UUID: " + foodId + ")");


            if (existsBarcode(barcode)) {
                Log.d(TAG, "Штрихкод " + barcode + " уже существует в базе данных");
                return false;
            }


            JSONObject data = new JSONObject();
            data.put("barcode", barcode);
            data.put("food_id", foodId);

            JSONArray result = supabaseClient.from("barcodes")
                    .insert(data)
                    .executeAndGetArray();

            boolean success = result != null && result.length() > 0;
            if (success) {
                Log.d(TAG, "Штрихкод успешно добавлен: " + barcode + " -> " + foodId);
            } else {
                Log.e(TAG, "Не удалось добавить штрихкод: " + barcode);
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при добавлении штрихкода: " + e.getMessage(), e);
            return false;
        }
    }


    private boolean isUUID(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private String generateUuidFromFoodId(String foodId) {
        try {
            Log.d(TAG, "Генерация UUID из foodId: " + foodId);


            if (isUUID(foodId)) {
                Log.d(TAG, "foodId уже является UUID: " + foodId);
                return foodId;
            }


            try {
                long numericId = Long.parseLong(foodId);


                String seedBaseName = "vitamove.food.";
                String seedName = seedBaseName + numericId;


                UUID uuid = UUID.nameUUIDFromBytes(seedName.getBytes());
                String uuidString = uuid.toString();

                Log.d(TAG, "Сгенерирован UUID из числового ID " + numericId + ": " + uuidString);
                return uuidString;

            } catch (NumberFormatException e) {

                Log.d(TAG, "foodId не является числом, используем как строку для генерации UUID");


                String seedName = "vitamove.food.string." + foodId;


                UUID uuid = UUID.nameUUIDFromBytes(seedName.getBytes());
                String uuidString = uuid.toString();

                Log.d(TAG, "Сгенерирован UUID из строки: " + uuidString);
                return uuidString;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при генерации UUID из foodId: " + e.getMessage(), e);

            String randomUuid = UUID.randomUUID().toString();
            Log.d(TAG, "Возвращаем случайный UUID: " + randomUuid);
            return randomUuid;
        }
    }


    private boolean checkFoodExists(String uuid) {
        try {
            Log.d(TAG, "Проверка существования продукта с UUID: " + uuid);

            JSONArray foodsArray = null;
            int maxRetries = 3;
            int currentRetry = 0;


            while (currentRetry < maxRetries && foodsArray == null) {
                try {
                    foodsArray = supabaseClient.from("foods")
                            .select("id")
                            .eq("id", uuid)
                            .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {
                    Log.d(TAG, "Токен обновлен, повторяем запрос проверки продукта. Попытка " + (currentRetry + 1));
                    currentRetry++;

                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос проверки продукта после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }

            if (foodsArray == null) {
                Log.e(TAG, "Не удалось получить результаты проверки продукта после обновления токена");
                return false;
            }

            boolean exists = foodsArray.length() > 0;
            Log.d(TAG, "Продукт с UUID " + uuid + (exists ? " найден" : " не найден") + " в базе данных");
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке существования продукта: " + e.getMessage(), e);
            return false;
        }
    }


    private Food parseFoodFromJson(JSONObject json) {
        try {

            Gson gson = new Gson();


            FoodDto foodDto = gson.fromJson(json.toString(), FoodDto.class);

            Log.d(TAG, "Создаем Food из JSON: " + foodDto.getName());


            Food food = new Food.Builder()
                    .fromDto(foodDto)
                    .build();

            Log.d(TAG, "Food создан: " + food.getName() +
                    ", id=" + food.getId() +
                    ", calories=" + food.getCalories() +
                    ", proteins=" + food.getProteins() +
                    ", fats=" + food.getFats() +
                    ", carbs=" + food.getCarbs());

            return food;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании Food из JSON: " + e.getMessage(), e);
            return null;
        }
    }


    public boolean existsBarcode(String barcode) {
        try {
            Log.d(TAG, "Проверка наличия штрихкода " + barcode + " в базе данных");

            if (barcode == null || barcode.trim().isEmpty()) {
                Log.e(TAG, "Штрихкод пустой");
                return false;
            }


            JSONArray barcodeArray = null;
            int maxRetries = 3;
            int currentRetry = 0;


            while (currentRetry < maxRetries && barcodeArray == null) {
                try {
                    barcodeArray = supabaseClient.from("barcodes")
                            .select("barcode")
                            .eq("barcode", barcode)
                            .executeAndGetArray();
                } catch (SupabaseClient.TokenRefreshedException e) {
                    Log.d(TAG, "Токен обновлен, повторяем запрос существования штрихкода. Попытка " + (currentRetry + 1));
                    currentRetry++;

                    if (currentRetry >= maxRetries) {
                        Log.e(TAG, "Не удалось выполнить запрос проверки штрихкода после " + maxRetries + " попыток");
                        throw e;
                    }
                }
            }

            if (barcodeArray == null) {
                Log.e(TAG, "Не удалось получить результаты проверки штрихкода после обновления токена");
                return false;
            }


            boolean exists = barcodeArray.length() > 0;
            Log.d(TAG, "Штрихкод " + barcode + (exists ? " найден" : " не найден") + " в базе данных");
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке существования штрихкода: " + e.getMessage(), e);
            return false;
        }
    }
} 