package com.martist.vitamove.gigachat;

import android.util.Log;

import com.martist.vitamove.core.domain.utils.Constants;
import com.martist.vitamove.nutrition.ui.model.Food;

import org.json.JSONObject;

public class GigaChatProductService {
    private static final String TAG = "GigaChatProductService";
    private final GigaChatService gigaChatService;

    public interface ProductListener {
        void onProductFound(Food food);

        void onProductNotFound();

        void onError(String message);
    }

    public GigaChatProductService() {
        gigaChatService = GigaChatService.getInstance(
                Constants.GIGACHAT_CLIENT_ID,
                Constants.GIGACHAT_CLIENT_SECRET
        );
    }

    private int calculateUsefulnessIndex(float proteins, float fats, float carbs) {

        float index = 5.0f;


        float total = proteins + fats + carbs;
        if (total > 0) {
            float proteinRatio = proteins / total;
            float fatRatio = fats / total;
            float carbRatio = carbs / total;


            if (proteinRatio > 0.3f) {
                index += 2.0f;
            } else if (proteinRatio > 0.2f) {
                index += 1.0f;
            }


            if (fatRatio > 0.4f) {
                index -= 2.0f;
            } else if (fatRatio > 0.3f) {
                index -= 1.0f;
            }


            if (carbRatio > 0.5f) {
                index += 1.0f;
            }
        }


        return Math.max(1, Math.min(10, Math.round(index)));
    }

    public void searchProduct(String productName, ProductListener listener) {
        String prompt = String.format(
                "Предоставь информацию о продукте питания,если затрудняешься, то предоставь приблизительную ифнормацию(брэнд можно отбросить) '%s'. " +
                        "Ответ должен быть в формате JSON со следующими полями:\n" +
                        "{\n" +
                        "  \"name\": \"название продукта\",\n" +
                        "  \"calories\": число (калории на 100г),\n" +
                        "  \"proteins\": число (белки на 100г),\n" +
                        "  \"fats\": число (жиры на 100г),\n" +
                        "  \"carbs\": число (углеводы на 100г),\n" +
                        "  \"category\": \"категория продукта (например: Мясо, Молочные продукты, Крупы, Овощи, Фрукты, Напитки, Сладости и т.д.)\",\n" +
                        "  \"subcategory\": \"подкатегория продукта (например: для мяса - Говядина, Курица, Свинина; для молочных - Сыр, Творог, Молоко и т.д.)\"\n" +
                        "}\n" +
                        "Если информацию предоставить не можешь, верни null.",
                productName
        );

        gigaChatService.sendMessage(prompt, new GigaChatService.ChatCallback() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.d(TAG, "Получен ответ от GigaChat: [" + response + "]");


                    if (response == null || response.trim().isEmpty()) {
                        Log.e(TAG, "Получен пустой ответ от GigaChat API");
                        listener.onError("Получен пустой ответ от сервера");
                        return;
                    }


                    response = response.replaceAll("^[^{]*", "");
                    response = response.replaceAll("[^}]*$", "");

                    Log.d(TAG, "Обработанный ответ: [" + response + "]");


                    if (response.isEmpty() || response.equals("null")) {
                        Log.d(TAG, "Ответ GigaChat указывает на отсутствие продукта");
                        listener.onProductNotFound();
                        return;
                    }


                    if (!response.startsWith("{") || !response.endsWith("}")) {
                        Log.e(TAG, "Ответ не является корректным JSON: " + response);
                        listener.onError("Некорректный формат ответа");
                        return;
                    }

                    JSONObject json = new JSONObject(response);


                    if (!json.has("name") || !json.has("calories") ||
                            !json.has("proteins") || !json.has("fats") || !json.has("carbs")) {
                        Log.e(TAG, "В ответе отсутствуют необходимые поля: " + response);
                        listener.onError("Отсутствуют необходимые данные о продукте");
                        return;
                    }

                    float proteins = (float) json.getDouble("proteins");
                    float fats = (float) json.getDouble("fats");
                    float carbs = (float) json.getDouble("carbs");


                    int usefulnessIndex = calculateUsefulnessIndex(proteins, fats, carbs);


                    String category = "Другое";
                    String subcategory = "Другое";

                    if (json.has("category") && !json.isNull("category")) {
                        category = json.getString("category");
                    }

                    if (json.has("subcategory") && !json.isNull("subcategory")) {
                        subcategory = json.getString("subcategory");
                    }

                    Log.d(TAG, "Получены категория: " + category + ", подкатегория: " + subcategory);


                    Food food = new Food.Builder()
                            .id("")
                            .name(json.getString("name"))
                            .calories(json.getInt("calories"))
                            .proteins(proteins)
                            .fats(fats)
                            .carbs(carbs)
                            .category(category)
                            .subcategory(subcategory)
                            .popularity(0)
                            .calcium(Float.NaN)
                            .iron(Float.NaN)
                            .magnesium(Float.NaN)
                            .phosphorus(Float.NaN)
                            .potassium(Float.NaN)
                            .sodium(Float.NaN)
                            .zinc(Float.NaN)
                            .vitaminA(Float.NaN)
                            .vitaminB1(Float.NaN)
                            .vitaminB2(Float.NaN)
                            .vitaminB3(Float.NaN)
                            .vitaminB5(Float.NaN)
                            .vitaminB6(Float.NaN)
                            .vitaminB9(Float.NaN)
                            .vitaminB12(Float.NaN)
                            .vitaminC(Float.NaN)
                            .vitaminD(Float.NaN)
                            .vitaminE(Float.NaN)
                            .vitaminK(Float.NaN)
                            .cholesterol(Float.NaN)
                            .saturatedFats(Float.NaN)
                            .transFats(Float.NaN)
                            .fiber(Float.NaN)
                            .sugar(Float.NaN)
                            .usefulness_index(usefulnessIndex)
                            .build();

                    Log.d(TAG, "Food создан из GigaChat: " + food.getName() +
                            ", calories=" + food.getCalories() +
                            ", proteins=" + food.getProteins() +
                            ", fats=" + food.getFats() +
                            ", carbs=" + food.getCarbs() +
                            ", category=" + food.getCategory() +
                            ", subcategory=" + food.getSubcategory() +
                            ", usefulness_index=" + usefulnessIndex);

                    listener.onProductFound(food);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка парсинга ответа: " + e.getMessage() + ", ответ: [" + response + "]", e);
                    listener.onError("Ошибка обработки ответа");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Ошибка запроса к GigaChat: " + error);
                listener.onError("Ошибка получения информации о продукте");
            }
        });
    }
} 