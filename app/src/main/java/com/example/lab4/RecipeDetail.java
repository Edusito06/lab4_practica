package com.example.lab4;

import java.util.Map;

// Receta ya "limpia" lista para mostrar en pantalla
public class RecipeDetail {
    private static final int MAX_INGREDIENTES = 20;

    private final String id;
    private final String nombre;
    private final String categoria;
    private final String area;
    private final String instrucciones;
    private final String imagen;
    private final String ingredientes;

    private RecipeDetail(Map<String, String> m) {
        id = m.get("idMeal");
        nombre = m.get("strMeal");
        categoria = m.get("strCategory");
        area = m.get("strArea");
        instrucciones = m.get("strInstructions");
        imagen = m.get("strMealThumb");

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= MAX_INGREDIENTES; i++) {
            String ing = m.get("strIngredient" + i);
            String med = m.get("strMeasure" + i);
            if (ing == null || ing.trim().isEmpty()) continue;
            sb.append("• ").append(ing.trim());
            if (med != null && !med.trim().isEmpty()) {
                sb.append(" - ").append(med.trim());
            }
            sb.append("\n");
        }
        ingredientes = sb.toString().trim();
    }

    public static RecipeDetail from(Map<String, String> m) {
        return new RecipeDetail(m);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getArea() { return area; }
    public String getInstrucciones() { return instrucciones; }
    public String getImagen() { return imagen; }
    public String getIngredientes() { return ingredientes; }
}
