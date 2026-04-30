package com.example.calendar.ui;

import org.json.JSONObject;

public interface ApiCall {
    JSONObject run() throws Exception;
}
