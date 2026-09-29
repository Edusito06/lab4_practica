package com.example.lab4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lab4.databinding.FragmentRecipeBinding;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeFragment extends Fragment {

    private FragmentRecipeBinding binding;
    private Call<RecipeResponse> call;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecipeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBuscarReceta.setOnClickListener(v -> {
            String id = binding.etIdPlato.getText().toString().trim();
            if (id.isEmpty()) {
                binding.etIdPlato.setError(getString(R.string.error_id_vacio));
                return;
            }
            buscarReceta(id);
        });

        // Si llegó desde Meals (o del plato sorpresa) recibe el id por Safe Args y lo carga solo
        String idRecibido = getArguments() != null
                ? RecipeFragmentArgs.fromBundle(getArguments()).getMealId() : null;
        if (idRecibido != null) {
            binding.etIdPlato.setText(idRecibido);
            buscarReceta(idRecibido);
        }
    }

    // GET 3: detalle del plato
    private void buscarReceta(String id) {
        if (call != null) call.cancel();
        binding.progress.setVisibility(View.VISIBLE);
        binding.scrollReceta.setVisibility(View.GONE);
        call = ApiClient.get().lookup(id);
        call.enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecipeResponse> c, @NonNull Response<RecipeResponse> response) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                List<Map<String, String>> meals = response.body() != null ? response.body().getMeals() : null;
                if (!response.isSuccessful() || meals == null || meals.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.error_sin_resultados, Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrarReceta(RecipeDetail.from(meals.get(0)));
            }

            @Override
            public void onFailure(@NonNull Call<RecipeResponse> c, @NonNull Throwable t) {
                if (binding == null || c.isCanceled()) return;
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarReceta(RecipeDetail receta) {
        binding.tvNombre.setText(receta.getNombre());
        binding.tvCategoria.setText(getString(R.string.lbl_categoria, receta.getCategoria()));
        binding.tvArea.setText(getString(R.string.lbl_area, receta.getArea()));
        binding.tvIngredientes.setText(receta.getIngredientes());
        binding.tvInstrucciones.setText(receta.getInstrucciones());
        ImageLoader.load(receta.getImagen(), binding.ivReceta);
        binding.scrollReceta.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (call != null) call.cancel();
        binding = null;
    }
}
