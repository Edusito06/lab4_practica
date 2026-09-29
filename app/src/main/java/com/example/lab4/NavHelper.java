package com.example.lab4;

import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;

// Navega sin acumular fragments en el BackStack:
// siempre limpia hasta el destino inicial, así el stack nunca crece.
public class NavHelper {

    private NavHelper() {}

    private static NavOptions opciones(NavController nav) {
        return new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(nav.getGraph().getStartDestinationId(), true)
                .build();
    }

    // Desde el menú inferior (sin argumentos)
    public static void navigateTo(NavController nav, int destinationId) {
        nav.navigate(destinationId, null, opciones(nav));
    }

    // Desde un fragment, usando las clases Directions de Safe Args
    public static void navigateTo(NavController nav, NavDirections directions) {
        nav.navigate(directions, opciones(nav));
    }
}
