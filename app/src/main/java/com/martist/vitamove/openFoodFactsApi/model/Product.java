package com.martist.vitamove.openFoodFactsApi.model;

import com.google.gson.annotations.SerializedName;

public class Product {
    @SerializedName("code")
    private String code;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("product_name_ru")
    private String productNameRu;

    @SerializedName("brands")
    private String brands;

    @SerializedName("categories")
    private String categories;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("nutriments")
    private Nutriments nutriments;

    public String getCode() {
        return code;
    }

    public String getProductName() {

        if (productNameRu != null && !productNameRu.isEmpty()) {
            return productNameRu;
        }
        return productName;
    }

    public String getBrands() {
        return brands;
    }

    public String getCategories() {
        return categories;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Nutriments getNutriments() {
        return nutriments;
    }
} 