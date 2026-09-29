package com.example.lab4;

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

import com.example.lab4.databinding.FragmentCategoriesBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriesFragment extends Fragment {

    private FragmentCategoriesBinding binding;
    private Call<CategoriesResponse> call;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Al tocar una categoría, se navega a Meals enviando el nombre de la categoría
        CategoryAdapter adapter = new CategoryAdapter(category -> {
            NavHelper.navigateTo(NavHostFragment.findNavController(this),
                    CategoriesFragmentDirections.actionCategoriesFragmentToMealsFragment()
                            .setCategory(category.getStrCategory()));
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(adapter);

        cargarCategorias(adapter);
    }

    private void cargarCategorias(CategoryAdapter adapter) {
        binding.progress.setVisibility(View.VISIBLE);
        call = ApiClient.get().getCategories();
        call.enqueue(new Callback<CategoriesResponse>() {
            @Override
            public void onResponse(@NonNull Call<CategoriesResponse> c,
                                   @NonNull Response<CategoriesResponse> response) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                List<Category> lista = response.body() != null ? response.body().getCategories() : null;
                if (response.isSuccessful() && lista != null) {
                    adapter.setListaCategorias(lista);
                } else {
                    Toast.makeText(requireContext(), R.string.error_red, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CategoriesResponse> c, @NonNull Throwable t) {
                if (binding == null || c.isCanceled()) return;
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (call != null) call.cancel();
        binding = null;
    }
}
