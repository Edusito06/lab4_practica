package com.example.lab4;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;

// Listener del acelerómetro (misma idea que SensorAccListener de la Clase5),
// que además detecta una agitación mayor al umbral.
public class SensorAccListener implements SensorEventListener {

    public interface OnShakeListener {
        void onShake();
    }

    private static final float UMBRAL_AGITACION = 4f;          // m/s²
    private static final long ESPERA_ENTRE_AGITACIONES_MS = 2000;
    private static final float ALPHA = 0.8f;

    private final OnShakeListener listener;
    private final float[] gravedad = new float[3];
    private long ultimaAgitacion;

    public SensorAccListener(OnShakeListener listener) {
        this.listener = listener;
        reiniciar();
    }

    // Llamar al registrar el sensor: evita un falso disparo con los primeros valores
    public void reiniciar() {
        gravedad[0] = gravedad[1] = gravedad[2] = 0f;
        ultimaAgitacion = SystemClock.elapsedRealtime();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        // Filtro paso-alto: se resta la gravedad para quedarse solo con el movimiento
        float[] lineal = new float[3];
        for (int i = 0; i < 3; i++) {
            gravedad[i] = ALPHA * gravedad[i] + (1 - ALPHA) * sensorEvent.values[i];
            lineal[i] = sensorEvent.values[i] - gravedad[i];
        }
        double magnitud = Math.sqrt(lineal[0] * lineal[0] + lineal[1] * lineal[1] + lineal[2] * lineal[2]);

        long ahora = SystemClock.elapsedRealtime();
        if (magnitud > UMBRAL_AGITACION && ahora - ultimaAgitacion > ESPERA_ENTRE_AGITACIONES_MS) {
            ultimaAgitacion = ahora;
            listener.onShake();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
