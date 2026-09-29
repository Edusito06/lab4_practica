package com.example.lab4;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Instancia única de Retrofit (API key pública de desarrollo = 1)
public class ApiClient {
    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1/";
    private static MealApi api;

    private ApiClient() {}

    public static MealApi get() {
        if (api == null) {
            api = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(MealApi.class);
        }
        return api;
    }
}
