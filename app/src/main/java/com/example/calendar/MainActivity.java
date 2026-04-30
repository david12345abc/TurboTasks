package com.example.calendar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.calendar.ui.ApiCall;
import com.example.calendar.ui.ApiSuccess;
import com.example.calendar.ui.AppMenu;
import com.example.calendar.ui.ScreenHost;
import com.example.calendar.ui.UiKit;
import com.example.calendar.ui.admin.AdminScreen;
import com.example.calendar.ui.auth.AuthScreen;
import com.example.calendar.ui.home.HomeScreen;
import com.example.calendar.ui.project.ProjectScreen;

public class MainActivity extends AppCompatActivity implements ScreenHost {
    private final ApiClient apiClient = new ApiClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SessionStore sessionStore;
    private LinearLayout root;
    private ProgressBar progressBar;
    private AuthScreen authScreen;
    private ProjectScreen projectScreen;
    private HomeScreen homeScreen;
    private AdminScreen adminScreen;
    private AppMenu appMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionStore = new SessionStore(this);
        projectScreen = new ProjectScreen(this);
        authScreen = new AuthScreen(this);
        homeScreen = new HomeScreen(this, projectScreen);
        adminScreen = new AdminScreen(this);
        appMenu = new AppMenu(this);
        showStartScreen();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void showStartScreen() {
        if (sessionStore.getUserToken().isEmpty()) {
            showAuthScreen(false);
        } else {
            showHomeScreen();
        }
    }

    @Override
    public void setScreen(String title, boolean withHeader) {
        progressBar = null;
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(UiKit.BACKGROUND);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(28));
        root.setBackgroundColor(UiKit.BACKGROUND);
        scrollView.addView(root);
        setContentView(scrollView);
        if (withHeader) {
            addHeader(title);
        } else {
            addTitle(title);
        }
    }

    private void addHeader(String title) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button menuButton = UiKit.iconButton(this, "☰");
        menuButton.setOnClickListener(v -> appMenu.show());
        header.addView(menuButton, new LinearLayout.LayoutParams(dp(52), dp(48)));

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView logoutButton = UiKit.text(this, "Выйти", 15, 0xFFFF5A5F);
        logoutButton.setGravity(Gravity.CENTER_VERTICAL);
        logoutButton.setPadding(dp(16), 0, 0, 0);
        logoutButton.setText("Выйти");
        logoutButton.setOnClickListener(v -> {
            sessionStore.clearAll();
            showAuthScreen(false);
        });
        header.addView(logoutButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)));

        root.addView(header);
        addSpace(24);
    }

    @Override
    public LinearLayout createHeaderBar() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(28), dp(28), dp(28), 0);

        Button menuButton = UiKit.iconButton(this, "☰");
        menuButton.setOnClickListener(v -> appMenu.show());
        header.addView(menuButton, new LinearLayout.LayoutParams(dp(52), dp(48)));

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView logoutButton = UiKit.text(this, "Выйти", 15, 0xFFFF5A5F);
        logoutButton.setGravity(Gravity.CENTER_VERTICAL);
        logoutButton.setOnClickListener(v -> {
            sessionStore.clearAll();
            showAuthScreen(false);
        });
        header.addView(logoutButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(48)
        ));
        return header;
    }

    @Override
    public void showAuthScreen(boolean registration) {
        authScreen.show(registration);
    }

    @Override
    public void showProjectGateScreen() {
        projectScreen.showProjectGate();
    }

    @Override
    public void saveProjectAndOpenHome(JSONObject response) throws Exception {
        sessionStore.saveProject(response.getString("project_token"), response.getJSONObject("project"));
        showHomeScreen();
    }

    @Override
    public void showHomeScreen() {
        progressBar = null;
        homeScreen.show();
    }

    @Override
    public void showAdminScreen() {
        adminScreen.show();
    }

    @Override
    public boolean isAppAdmin() {
        JSONObject user = sessionStore.getUser();
        if (user == null) {
            return false;
        }
        String role = user.optString("app_role");
        return "admin".equals(role) || "superadmin".equals(role) || user.optBoolean("is_superuser");
    }

    @Override
    public Activity activity() {
        return this;
    }

    @Override
    public Context context() {
        return this;
    }

    @Override
    public ApiClient apiClient() {
        return apiClient;
    }

    @Override
    public ExecutorService executor() {
        return executor;
    }

    @Override
    public SessionStore sessionStore() {
        return sessionStore;
    }

    @Override
    public LinearLayout root() {
        return root;
    }

    @Override
    public EditText input(String hint, boolean password) {
        return UiKit.input(this, hint, password);
    }

    @Override
    public TextView textView(String text) {
        return UiKit.text(this, text, 16, UiKit.TEXT_SECONDARY);
    }

    @Override
    public TextView section(String text) {
        return UiKit.section(this, text);
    }

    @Override
    public LinearLayout card() {
        return (LinearLayout) UiKit.card(this);
    }

    private void addTitle(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        root.addView(UiKit.title(this, text));
        addSpace(12);
    }

    @Override
    public void addSpace(int heightDp) {
        View view = new View(this);
        root.addView(view, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    @Override
    public void addProgress() {
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);
    }

    @Override
    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    @Override
    public String text(EditText editText) {
        return editText.getText().toString().trim();
    }

    @Override
    public String opt(JSONObject json, String key) {
        return json == null ? "" : json.optString(key);
    }

    @Override
    public int dp(int value) {
        return UiKit.dp(this, value);
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showError(Exception exception) {
        toast(exception.getMessage() == null ? "Ошибка" : exception.getMessage());
    }

    @Override
    public void runApi(ApiCall call, ApiSuccess success) {
        setLoading(true);
        executor.execute(() -> {
            try {
                JSONObject response = call.run();
                runOnUiThread(() -> {
                    setLoading(false);
                    try {
                        success.onSuccess(response);
                    } catch (Exception e) {
                        showError(e);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(e);
                });
            }
        });
    }
}
