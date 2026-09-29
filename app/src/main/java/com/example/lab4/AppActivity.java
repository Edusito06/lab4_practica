package com.example.lab4;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lab4.databinding.ActivityAppBinding;

public class AppActivity extends AppCompatActivity {

    private ActivityAppBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        navController = navHost.getNavController();

        // Clic en el menú inferior: navega sin acumular fragments en el BackStack
        binding.bottomNav.setOnItemSelectedListener((MenuItem item) -> {
            NavHelper.navigateTo(navController, item.getItemId());
            return true;
        });

        // Si se navega por código (ej. clic en una categoría), resalta el botón correcto
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            MenuItem item = binding.bottomNav.getMenu().findItem(destination.getId());
            if (item != null) item.setChecked(true);
        });

        // Atrás siempre regresa al MainActivity, sin importar cuánto se navegó
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }
}
