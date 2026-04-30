package com.example.calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    public Object get(String path, String userToken, String projectToken) throws IOException, JSONException {
        return request("GET", path, null, userToken, projectToken);
    }

    public Object post(String path, JSONObject body, String userToken, String projectToken) throws IOException, JSONException {
        return request("POST", path, body, userToken, projectToken);
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

    private String extractError(String text, int code) {
        if (text == null || text.trim().isEmpty()) {
            return "Ошибка API: " + code;
        }
        try {
            JSONObject json = new JSONObject(text);
            if (json.has("detail")) {
                return json.getString("detail");
            }
            return json.toString();
        } catch (JSONException ignored) {
            return text;
        }
    }
}
