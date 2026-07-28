package com.martist.vitamove.openFoodFactsApi;

import android.util.Log;

import com.martist.vitamove.nutrition.ui.model.Food;
import com.martist.vitamove.openFoodFactsApi.model.Nutriments;
import com.martist.vitamove.openFoodFactsApi.model.Product;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OpenFoodFactsService {
    private static final String TAG = "OpenFoodFactsService";
    private static final String BASE_URL = "https://world.openfoodfacts.org/";

    private final OpenFoodFactsApi api;

    public interface ProductListener {
        void onProductFound(Food food);

        void onProductNotFound();

        void onError(String message);

        void onProductFoundWithoutNutrients(String productName);
    }

    public OpenFoodFactsService() {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(OpenFoodFactsApi.class);
    }

    public void getProductByBarcode(String barcode, final ProductListener listener) {
        Call<OpenFoodFactsResponse> call = api.getProductByBarcode(barcode);

        call.enqueue(new Callback<OpenFoodFactsResponse>() {
            @Override
            public void onResponse(Call<OpenFoodFactsResponse> call, Response<OpenFoodFactsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OpenFoodFactsResponse openFoodFactsResponse = response.body();

                    if (openFoodFactsResponse.isSuccess()) {
                        Product product = openFoodFactsResponse.getProduct();


                        if (product != null && product.getNutriments() != null) {
                            Nutriments nutriments = product.getNutriments();
                            boolean hasNutrients =
                                    nutriments.getEnergyKcal() > 0 &&
                                            nutriments.getProteins() > 0 &&
                                            nutriments.getFat() > 0 &&
                                            nutriments.getCarbohydrates() > 0;

                            if (hasNutrients) {

                                Food food = convertToFoodModel(product);
                                listener.onProductFound(food);
                            } else if (product.getProductName() != null && !product.getProductName().isEmpty()) {

                                Log.d(TAG, "Продукт найден, но нет данных о нутриентах: " + product.getProductName());
                                listener.onProductFoundWithoutNutrients(product.getProductName());
                            } else {
                                listener.onProductNotFound();
                            }
                        } else if (product != null && product.getProductName() != null && !product.getProductName().isEmpty()) {

                            Log.d(TAG, "Продукт найден, но нет данных о нутриентах: " + product.getProductName());
                            listener.onProductFoundWithoutNutrients(product.getProductName());
                        } else {
                            listener.onProductNotFound();
                        }
                    } else {
                        listener.onProductNotFound();
                    }
                } else {
                    listener.onError("Ошибка получения данных: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<OpenFoodFactsResponse> call, Throwable t) {
                Log.e(TAG, "Ошибка запроса: " + t.getMessage(), t);
                listener.onError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private Food convertToFoodModel(Product product) {
        if (product == null) {
            Log.e(TAG, "Product is null");
            return null;
        }

        Nutriments nutriments = product.getNutriments();
        if (nutriments == null) {
            Log.e(TAG, "Nutriments is null");
            nutriments = new Nutriments();
        }


        String category = "Продукты";
        String subcategory = "Другое";

        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            String[] categories = product.getCategories().split(",");
            if (categories.length > 0) {
                category = categories[0].trim();
                if (categories.length > 1) {
                    subcategory = categories[1].trim();
                }
            }
        }

        Log.d(TAG, "Создаем Food из продукта: " + product.getProductName());


        int usefulnessIndex = calculateUsefulnessIndex(
                nutriments.getProteins(),
                nutriments.getFat(),
                nutriments.getCarbohydrates(),
                nutriments.getFiber(),
                nutriments.getSugars(),
                nutriments.getSaturatedFat(),
                nutriments.getTransFat(),
                nutriments.getSodium()
        );


        float fiber = nutriments.getFiber();
        float sugar = nutriments.getSugars();
        float saturatedFat = nutriments.getSaturatedFat();
        float transFat = nutriments.getTransFat();
        float sodium = nutriments.getSodium();
        float calcium = nutriments.getCalcium();
        float iron = nutriments.getIron();
        float magnesium = nutriments.getMagnesium();
        float phosphorus = nutriments.getPhosphorus();
        float potassium = nutriments.getPotassium();
        float zinc = nutriments.getZinc();
        float vitaminA = nutriments.getVitaminA();
        float vitaminC = nutriments.getVitaminC();
        float vitaminD = nutriments.getVitaminD();
        float vitaminE = nutriments.getVitaminE();
        float vitaminK = nutriments.getVitaminK();
        float vitaminB1 = nutriments.getVitaminB1();
        float vitaminB2 = nutriments.getVitaminB2();
        float vitaminB3 = nutriments.getVitaminB3();
        float vitaminB5 = nutriments.getVitaminB5();
        float vitaminB6 = nutriments.getVitaminB6();
        float vitaminB9 = nutriments.getVitaminB9();
        float vitaminB12 = nutriments.getVitaminB12();


        if (fiber == 0) fiber = Float.NaN;
        if (sugar == 0) sugar = Float.NaN;
        if (saturatedFat == 0) saturatedFat = Float.NaN;
        if (transFat == 0) transFat = Float.NaN;
        if (sodium == 0) sodium = Float.NaN;
        if (calcium == 0) calcium = Float.NaN;
        if (iron == 0) iron = Float.NaN;
        if (magnesium == 0) magnesium = Float.NaN;
        if (phosphorus == 0) phosphorus = Float.NaN;
        if (potassium == 0) potassium = Float.NaN;
        if (zinc == 0) zinc = Float.NaN;
        if (vitaminA == 0) vitaminA = Float.NaN;
        if (vitaminC == 0) vitaminC = Float.NaN;
        if (vitaminD == 0) vitaminD = Float.NaN;
        if (vitaminE == 0) vitaminE = Float.NaN;
        if (vitaminK == 0) vitaminK = Float.NaN;
        if (vitaminB1 == 0) vitaminB1 = Float.NaN;
        if (vitaminB2 == 0) vitaminB2 = Float.NaN;
        if (vitaminB3 == 0) vitaminB3 = Float.NaN;
        if (vitaminB5 == 0) vitaminB5 = Float.NaN;
        if (vitaminB6 == 0) vitaminB6 = Float.NaN;
        if (vitaminB9 == 0) vitaminB9 = Float.NaN;
        if (vitaminB12 == 0) vitaminB12 = Float.NaN;

        Food food = new Food.Builder()
                .id("")
                .name(product.getProductName())
                .category(category)
                .subcategory(subcategory)
                .calories((int) nutriments.getEnergyKcal())
                .proteins(nutriments.getProteins())
                .fats(nutriments.getFat())
                .carbs(nutriments.getCarbohydrates())
                .fiber(fiber)
                .sugar(sugar)
                .saturatedFats(saturatedFat)
                .transFats(transFat)
                .sodium(sodium)
                .calcium(calcium)
                .iron(iron)
                .magnesium(magnesium)
                .phosphorus(phosphorus)
                .potassium(potassium)
                .zinc(zinc)
                .vitaminA(vitaminA)
                .vitaminC(vitaminC)
                .vitaminD(vitaminD)
                .vitaminE(vitaminE)
                .vitaminK(vitaminK)
                .vitaminB1(vitaminB1)
                .vitaminB2(vitaminB2)
                .vitaminB3(vitaminB3)
                .vitaminB5(vitaminB5)
                .vitaminB6(vitaminB6)
                .vitaminB9(vitaminB9)
                .vitaminB12(vitaminB12)
                .popularity(0)
                .usefulness_index(usefulnessIndex)
                .build();

        Log.d(TAG, "Food создан: " + food.getName() +
                ", calories=" + food.getCalories() +
                ", proteins=" + food.getProteins() +
                ", fats=" + food.getFats() +
                ", carbs=" + food.getCarbs() +
                ", usefulness_index=" + usefulnessIndex);

        return food;
    }

    private int calculateUsefulnessIndex(
            float proteins, float fat, float carbs,
            float fiber, float sugars, float saturatedFat,
            float transFat, float sodium) {


        float index = 5.0f;


        if (proteins > 15.0f) {
            index += 2.0f;
        } else if (proteins > 10.0f) {
            index += 1.0f;
        }


        if (fiber > 5.0f) {
            index += 1.0f;
        }


        if (sugars > 15.0f) {
            index -= 1.0f;
        }


        if (saturatedFat > 5.0f || transFat > 1.0f) {
            index -= 1.0f;
        }


        if (sodium > 500.0f) {
            index -= 1.0f;
        }


        return Math.max(1, Math.min(10, Math.round(index)));
    }
} 