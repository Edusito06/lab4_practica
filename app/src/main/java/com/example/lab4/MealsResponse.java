package com.example.lab4;

import java.util.List;

// {"meals":[ ... ]}  (meals viene null si no hay resultados)
public class MealsResponse {
    private List<MealSummary> meals;

    public List<MealSummary> getMeals() { return meals; }
}
