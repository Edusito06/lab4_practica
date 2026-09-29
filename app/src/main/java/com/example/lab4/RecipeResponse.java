package com.example.lab4;

import java.util.List;
import java.util.Map;

// Respuesta de lookup.php y random.php. Cada plato trae strIngredient1..20 y
// strMeasure1..20, por eso se recibe como mapa clave->valor (los valores pueden ser null).
public class RecipeResponse {
    private List<Map<String, String>> meals;

    public List<Map<String, String>> getMeals() { return meals; }
}
