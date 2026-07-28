package com.martist.vitamove.openFoodFactsApi;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    Call<OpenFoodFactsResponse> getProductByBarcode(@Path("barcode") String barcode);
} 