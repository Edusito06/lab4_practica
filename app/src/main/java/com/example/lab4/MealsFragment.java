package com.example.lab4;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lab4.databinding.FragmentMealsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MealsFragment extends Fragment {

    private FragmentMealsBinding binding;
    private MealAdapter adapter;
    private Call<MealsResponse> call;
    private Call<RecipeResponse> callRandom;

    private String categoria;          // null => modo búsqueda por ingrediente
    private SensorManager sensorManager;
    private Sensor acelerometro;
    private final SensorAccListener listener = new SensorAccListener(this::pedirPlatoSorpresa);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMealsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Argumento recibido con Safe Args (null si se entró por el menú inferior)
        categoria = getArguments() != null
                ? MealsFragmentArgs.fromBundle(getArguments()).getCategory() : null;

        adapter = new MealAdapter(meal -> irARecipe(meal.getIdMeal()));
        binding.rvMeals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMeals.setAdapter(adapter);

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        acelerometro = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;

        if (categoria != null) {
            // Llegó desde Categories: GET 2A
            binding.layoutBusqueda.setVisibility(View.GONE);
            binding.tvTituloCategoria.setVisibility(View.VISIBLE);
            binding.tvTituloCategoria.setText(getString(R.string.lbl_meals_categoria, categoria));
            cargarPlatos(ApiClient.get().filterByCategory(categoria));
        } else {
            // Modo búsqueda: GET 2B
            binding.btnBuscar.setOnClickListener(v -> {
                String ingrediente = binding.etIngrediente.getText().toString().trim();
                if (ingrediente.isEmpty()) {
                    binding.etIngrediente.setError(getString(R.string.error_ingrediente_vacio));
                    return;
                }
                cargarPlatos(ApiClient.get().filterByIngredient(ingrediente));
            });
        }
    }

    private void irARecipe(String idMeal) {
        NavHelper.navigateTo(NavHostFragment.findNavController(this),
                MealsFragmentDirections.actionMealsFragmentToRecipeFragment().setMealId(idMeal));
    }

    private void cargarPlatos(Call<MealsResponse> nuevaCall) {
        if (call != null) call.cancel();
        binding.progress.setVisibility(View.VISIBLE);
        call = nuevaCall;
        call.enqueue(new Callback<MealsResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealsResponse> c, @NonNull Response<MealsResponse> response) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), R.string.error_red, Toast.LENGTH_SHORT).show();
                    return;
                }
                List<MealSummary> platos = response.body().getMeals();
                if (platos == null || platos.isEmpty()) {
                    adapter.setListaPlatos(new ArrayList<>());
                    Toast.makeText(requireContext(), R.string.error_sin_resultados, Toast.LENGTH_SHORT).show();
                } else {
                    adapter.setListaPlatos(platos);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealsResponse> c, @NonNull Throwable t) {
                if (binding == null || c.isCanceled()) return;
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------- Acelerómetro: solo activo en modo búsqueda ----------

    @Override
    public void onResume() {
        super.onResume();
        if (categoria != null) return;
        if (acelerometro == null) {
            Toast.makeText(requireContext(), R.string.error_sin_acelerometro, Toast.LENGTH_SHORT).show();
            return;
        }
        listener.reiniciar();
        sensorManager.registerListener(listener, acelerometro, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(listener);
    }

    // GET 4: receta aleatoria (se ejecuta al agitar el celular)
    private void pedirPlatoSorpresa() {
        if (callRandom != null) callRandom.cancel();
        callRandom = ApiClient.get().random();
        callRandom.enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecipeResponse> c, @NonNull Response<RecipeResponse> response) {
                if (!isAdded() || binding == null) return;
                List<Map<String, String>> meals = response.body() != null ? response.body().getMeals() : null;
                if (!response.isSuccessful() || meals == null || meals.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.error_red, Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrarDialogSorpresa(RecipeDetail.from(meals.get(0)));
            }

            @Override
            public void onFailure(@NonNull Call<RecipeResponse> c, @NonNull Throwable t) {
                if (!isAdded() || binding == null || c.isCanceled()) return;
                Toast.makeText(requireContext(), R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogSorpresa(RecipeDetail receta) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_sorpresa_titulo)
                .setMessage(getString(R.string.dialog_sorpresa_mensaje,
                        receta.getNombre(), receta.getCategoria(), receta.getArea()))
                .setPositiveButton(R.string.dialog_ver_receta, (d, w) -> irARecipe(receta.getId()))
                .setNegativeButton(R.string.dialog_cerrar, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (call != null) call.cancel();
        if (callRandom != null) callRandom.cancel();
        binding = null;
    }
}
