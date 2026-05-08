package com.example.calendar.ui;

import android.app.Activity;
import android.content.Context;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.calendar.ApiClient;
import com.example.calendar.SessionStore;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;

public interface ScreenHost {
    Activity activity();

    Context context();

    ApiClient apiClient();

    ExecutorService executor();

    SessionStore sessionStore();

    LinearLayout root();

    void setScreen(String title, boolean withHeader);

    LinearLayout createHeaderBar();

    void showAuthScreen(boolean registration);

    void showHomeScreen();

    void showProjectGateScreen();

    void showAdminScreen();

    void saveProjectAndOpenHome(JSONObject response) throws Exception;

    void openProfilePhotoPicker();

    void openProjectImagePicker(ImageSelectionCallback callback);

    void runApi(ApiCall call, ApiSuccess success);

    void toast(String message);

    void showError(Exception exception);

    EditText input(String hint, boolean password);

    TextView textView(String text);

    TextView section(String text);

    LinearLayout card();

    void addSpace(int heightDp);

    void addProgress();

    void setProgressBar(ProgressBar progressBar);

    String text(EditText editText);

    String opt(JSONObject json, String key);

    int dp(int value);

    boolean isAppAdmin();
}
