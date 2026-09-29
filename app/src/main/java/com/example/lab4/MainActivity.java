package com.example.lab4;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lab4.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnIngresar.setOnClickListener(v -> {
            if (hayInternet()) {
                startActivity(new Intent(this, AppActivity.class));
            } else {
                mostrarDialogSinInternet();
            }
        });
    }

    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void mostrarDialogSinInternet() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_sin_internet_titulo)
                .setMessage(R.string.dialog_sin_internet_mensaje)
                .setPositiveButton(R.string.dialog_configuracion, (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_SETTINGS)))
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }
}
