package com.example.calendar.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public final class CachedImageLoader {
    private CachedImageLoader() {
    }

    public static Bitmap load(Context context, String imageUrl) throws Exception {
        File cacheFile = cacheFileFor(context, imageUrl);
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setUseCaches(true);
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(cacheFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
        return BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
    }

    private static File cacheFileFor(Context context, String imageUrl) throws Exception {
        File dir = new File(context.getCacheDir(), "remote-images");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = fileNameFromUrl(imageUrl);
        return new File(dir, sha1(imageUrl) + "_" + fileName);
    }

    private static String fileNameFromUrl(String imageUrl) {
        String cleanUrl = imageUrl.split("\\?")[0];
        int slashIndex = cleanUrl.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? cleanUrl.substring(slashIndex + 1) : cleanUrl;
        return fileName.isEmpty() ? "image" : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String sha1(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(value.getBytes("UTF-8"));
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
