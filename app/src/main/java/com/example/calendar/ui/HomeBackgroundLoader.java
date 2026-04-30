package com.example.calendar.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

public final class HomeBackgroundLoader {
    private static final String[] HOME_BACKGROUND_ASSETS = {
            "главная.jpg",
            "главная.png",
            "главная.jpeg",
            "главная.webp"
    };

    private HomeBackgroundLoader() {
    }

    public static void loadFromMedia(ExecutorService executor, ImageView target, Context context) {
        executor.execute(() -> {
            try {
                AssetManager assets = context.getAssets();
                try (InputStream inputStream = openHomeBackground(assets)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    target.post(() -> target.setImageBitmap(bitmap));
                }
            } catch (Exception ignored) {
                target.post(() -> target.setImageDrawable(null));
            }
        });
    }

    private static InputStream openHomeBackground(AssetManager assets) throws Exception {
        Exception lastException = null;
        for (String assetName : HOME_BACKGROUND_ASSETS) {
            try {
                return assets.open(assetName);
            } catch (Exception exception) {
                lastException = exception;
            }
        }
        throw lastException == null ? new IllegalStateException("Home background not found") : lastException;
    }
}
