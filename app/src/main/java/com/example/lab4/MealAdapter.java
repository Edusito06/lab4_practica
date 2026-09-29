package com.example.lab4;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab4.databinding.ItemMealBinding;

import java.util.ArrayList;
import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    public interface OnMealClickListener {
        void onMealClick(MealSummary meal);
    }

    private List<MealSummary> listaPlatos = new ArrayList<>();
    private final OnMealClickListener listener;

    public MealAdapter(OnMealClickListener listener) {
        this.listener = listener;
    }

    public void setListaPlatos(List<MealSummary> listaPlatos) {
        this.listaPlatos = listaPlatos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMealBinding binding = ItemMealBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MealViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MealSummary meal = listaPlatos.get(position);
        holder.binding.tvMeal.setText(meal.getStrMeal());
        holder.binding.tvMealId.setText(holder.binding.getRoot().getContext()
                .getString(R.string.lbl_id_plato, meal.getIdMeal()));
        ImageLoader.load(meal.getStrMealThumb(), holder.binding.ivMeal);
        holder.binding.getRoot().setOnClickListener(v -> listener.onMealClick(meal));
    }

    @Override
    public int getItemCount() {
        return listaPlatos.size();
    }

    public static class MealViewHolder extends RecyclerView.ViewHolder {
        ItemMealBinding binding;

        public MealViewHolder(ItemMealBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
