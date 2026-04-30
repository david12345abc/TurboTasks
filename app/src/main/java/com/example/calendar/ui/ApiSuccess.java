package com.example.calendar.ui;

import org.json.JSONObject;

public interface ApiSuccess {
    void onSuccess(JSONObject response) throws Exception;
}
