package com.example.lab4;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;

// Endpoints de TheMealDB (base: https://www.themealdb.com/api/json/v1/1/)
public interface MealApi {

    // GET 1: todas las categorías
    @GET("categories.php")
    Call<CategoriesResponse> getCategories();

    // GET 2A: platos por categoría (c=Seafood)
    @GET("filter.php")
    Call<MealsResponse> filterByCategory(@Query("c") String category);

    // GET 2B: platos por ingrediente (i=chicken_breast)
    @GET("filter.php")
    Call<MealsResponse> filterByIngredient(@Query("i") String ingredient);

    // GET 3: detalle de un plato por id
    @GET("lookup.php")
    Call<RecipeResponse> lookup(@Query("i") String idMeal);

    // GET 4: plato aleatorio
    @GET("random.php")
    Call<RecipeResponse> random();

    // Descarga de imágenes (url absoluta) con el mismo Retrofit
    @GET
    Call<ResponseBody> downloadImage(@Url String url);
}
