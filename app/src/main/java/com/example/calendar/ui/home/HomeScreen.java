package com.example.calendar.ui.home;

import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.calendar.ui.HomeBackgroundLoader;
import com.example.calendar.ui.ScreenHost;
import com.example.calendar.ui.UiKit;
import com.example.calendar.ui.project.ProjectScreen;

import org.json.JSONObject;

public class HomeScreen {
    private final ScreenHost host;
    private final ProjectScreen projectScreen;

    public HomeScreen(ScreenHost host, ProjectScreen projectScreen) {
        this.host = host;
        this.projectScreen = projectScreen;
    }

    public void show() {
        JSONObject project = host.sessionStore().getProject();
        if (project == null) {
            showProjectLoginHome();
            return;
        }

        host.setScreen("Главная", true);
        Button refresh = UiKit.secondaryButton(host.context(), "Обновить проект");
        refresh.setOnClickListener(v -> host.runApi(() -> (JSONObject) host.apiClient().get(
                "/api/calendar/projects/current/",
                host.sessionStore().getUserToken(),
                host.sessionStore().getProjectToken()
        ), response -> {
            host.sessionStore().saveProject(host.sessionStore().getProjectToken(), response);
            host.showHomeScreen();
        }));

        host.root().addView(refresh);
        host.addSpace(12);
        host.root().addView(host.textView("Главная страница календаря готова к подключению списка задач."));
        host.addProgress();
    }

    private void showProjectLoginHome() {
        FrameLayout frame = new FrameLayout(host.context());
        host.activity().setContentView(frame);

        ImageView imageView = new ImageView(host.context());
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(imageView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        HomeBackgroundLoader.loadFromMedia(host.executor(), imageView, host.context());

        frame.addView(host.createHeaderBar(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                host.dp(86),
                Gravity.TOP
        ));

        LinearLayout overlay = new LinearLayout(host.context());
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER_VERTICAL);
        overlay.setPadding(host.dp(28), host.dp(96), host.dp(28), host.dp(40));
        frame.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        overlay.addView(projectScreen.projectLoginCard(true));
        ProgressBar progressBar = new ProgressBar(host.context());
        progressBar.setVisibility(android.view.View.GONE);
        overlay.addView(progressBar);
        host.setProgressBar(progressBar);
    }
}
