package com.example.calendar;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class SessionStore {
    private static final String PREFS = "calendar_session";
    private static final String USER_TOKEN = "user_token";
    private static final String PROJECT_TOKEN = "project_token";
    private static final String USER_JSON = "user_json";
    private static final String PROJECT_JSON = "project_json";

    private final SharedPreferences preferences;

    public SessionStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getUserToken() {
        return preferences.getString(USER_TOKEN, "");
    }

    public String getProjectToken() {
        return preferences.getString(PROJECT_TOKEN, "");
    }

    public JSONObject getUser() {
        return readJson(USER_JSON);
    }

    public JSONObject getProject() {
        return readJson(PROJECT_JSON);
    }

    public void saveUser(String token, JSONObject user) {
        preferences.edit()
                .putString(USER_TOKEN, token)
                .putString(USER_JSON, user.toString())
                .apply();
    }

    public void saveProject(String token, JSONObject project) {
        preferences.edit()
                .putString(PROJECT_TOKEN, token)
                .putString(PROJECT_JSON, project.toString())
                .apply();
    }

    public void clearProject() {
        preferences.edit()
                .remove(PROJECT_TOKEN)
                .remove(PROJECT_JSON)
                .apply();
    }

    public void clearAll() {
        preferences.edit().clear().apply();
    }

    private JSONObject readJson(String key) {
        String value = preferences.getString(key, "");
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
