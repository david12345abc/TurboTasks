package com.example.calendar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ApiClient {
    public Object get(String path, String userToken, String projectToken) throws IOException, JSONException {
        return request("GET", path, null, userToken, projectToken);
    }

    public Object post(String path, JSONObject body, String userToken, String projectToken) throws IOException, JSONException {
        return request("POST", path, body, userToken, projectToken);
    }

    public Object delete(String path, String userToken, String projectToken) throws IOException, JSONException {
        return request("DELETE", path, null, userToken, projectToken);
    }

    public JSONObject uploadProfilePhoto(Context context, Uri photoUri, String userToken) throws IOException, JSONException {
        String boundary = "TurboTasksBoundary" + UUID.randomUUID();
        URL url = new URL(ApiConfig.BASE_URL + "/api/v1/auth/me/photo/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Token " + userToken);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        byte[] photoBytes = prepareProfilePhotoBytes(context, photoUri);
        try (OutputStream outputStream = connection.getOutputStream()) {
            writeText(outputStream, "--" + boundary + "\r\n");
            writeText(outputStream, "Content-Disposition: form-data; name=\"photo\"; filename=\"profile.jpg\"\r\n");
            writeText(outputStream, "Content-Type: image/jpeg\r\n\r\n");
            outputStream.write(photoBytes);
            writeText(outputStream, "\r\n--" + boundary + "--\r\n");
        }

        int code = connection.getResponseCode();
        String text = readBody(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException(extractError(text, code));
        }
        return new JSONObject(text);
    }

    public JSONObject createProject(Context context, JSONObject fields, Uri imageUri, String userToken)
            throws IOException, JSONException {
        if (imageUri == null) {
            return (JSONObject) post("/api/calendar/projects/", fields, userToken, null);
        }

        String boundary = "TurboTasksBoundary" + UUID.randomUUID();
        URL url = new URL(ApiConfig.BASE_URL + "/api/calendar/projects/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Token " + userToken);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream outputStream = connection.getOutputStream()) {
            String[] keys = {"login", "password", "title", "name", "description"};
            for (String key : keys) {
                writeText(outputStream, "--" + boundary + "\r\n");
                writeText(outputStream, "Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n");
                writeText(outputStream, fields.optString(key));
                writeText(outputStream, "\r\n");
            }
            byte[] imageBytes = prepareImageBytes(context, imageUri, 720, 720, 55);
            writeText(outputStream, "--" + boundary + "\r\n");
            writeText(outputStream, "Content-Disposition: form-data; name=\"image\"; filename=\"project.jpg\"\r\n");
            writeText(outputStream, "Content-Type: image/jpeg\r\n\r\n");
            outputStream.write(imageBytes);
            writeText(outputStream, "\r\n--" + boundary + "--\r\n");
        }

        int code = connection.getResponseCode();
        String text = readBody(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException(extractError(text, code));
        }
        return new JSONObject(text);
    }

    private byte[] prepareProfilePhotoBytes(Context context, Uri photoUri) throws IOException {
        return prepareImageBytes(context, photoUri, 520, 520, 45);
    }

    private byte[] prepareImageBytes(Context context, Uri photoUri, int maxWidth, int maxHeight, int quality) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream inputStream = context.getContentResolver().openInputStream(photoUri)) {
            BitmapFactory.decodeStream(inputStream, null, bounds);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds, maxWidth, maxHeight);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (InputStream inputStream = context.getContentResolver().openInputStream(photoUri)) {
            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        }
        if (bitmap == null) {
            throw new IOException("Не удалось прочитать изображение.");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        bitmap.recycle();
        return outputStream.toByteArray();
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
        return Math.max(1, inSampleSize);
    }

    private Object request(String method, String path, JSONObject body, String userToken, String projectToken)
            throws IOException, JSONException {
        URL url = new URL(ApiConfig.BASE_URL + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        if (userToken != null && !userToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Token " + userToken);
        }
        if (projectToken != null && !projectToken.isEmpty()) {
            connection.setRequestProperty("X-Project-Token", projectToken);
        }
        if (body != null) {
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Content-Length", String.valueOf(payload.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload);
            }
        }

        int code = connection.getResponseCode();
        String text = readBody(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException(extractError(text, code));
        }
        if (text == null || text.trim().isEmpty()) {
            return new JSONObject();
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        return new JSONObject(trimmed);
    }

    private String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private void writeText(OutputStream outputStream, String text) throws IOException {
        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private String extractError(String text, int code) {
        if (text == null || text.trim().isEmpty()) {
            return "Ошибка API: " + code;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html") || trimmed.contains("Traceback")) {
            return "Ошибка сервера: " + code + ". Проверьте backend и миграции базы данных.";
        }
        try {
            JSONObject json = new JSONObject(trimmed);
            if (json.has("detail")) {
                return json.getString("detail");
            }
            return json.toString();
        } catch (JSONException ignored) {
            return text;
        }
    }
}
