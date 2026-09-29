package com.example.lab4;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Carga imágenes desde una URL usando solo Retrofit (no se usa Glide/Picasso,
// que no se vieron en clase). Incluye caché en memoria.
public class ImageLoader {

    private static final int CACHE_BYTES = (int) (Runtime.getRuntime().maxMemory() / 8);

    private static final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(CACHE_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private ImageLoader() {}

    public static void load(String url, ImageView imageView) {
        // El tag evita que una vista reciclada por el RecyclerView muestre una imagen que no le corresponde
        imageView.setTag(url);
        imageView.setImageDrawable(null);
        if (url == null || url.isEmpty()) return;

        Bitmap enCache = cache.get(url);
        if (enCache != null) {
            imageView.setImageBitmap(enCache);
            return;
        }

        ApiClient.get().downloadImage(url).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    byte[] bytes = response.body().bytes();   // ya viene en memoria (no es streaming)
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;                 // reduce el consumo de memoria
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    if (bitmap == null) return;
                    cache.put(url, bitmap);
                    if (url.equals(imageView.getTag())) {
                        imageView.setImageBitmap(bitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
