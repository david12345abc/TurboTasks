package com.example.calendar.ui.home;

import android.app.Dialog;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.calendar.ApiConfig;
import com.example.calendar.ui.CachedImageLoader;
import com.example.calendar.ui.ScreenHost;
import com.example.calendar.ui.UiKit;
import com.example.calendar.ui.project.ProjectScreen;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HomeScreen {
    private final ScreenHost host;
    private final ProjectScreen projectScreen;
    private final List<JSONObject> projects = new ArrayList<>();
    private LinearLayout projectsContainer;
    private LinearLayout dashboardContainer;
    private EditText searchInput;
    private FrameLayout profileOverlay;
    private View profilePanel;
    private String activeBottomItem = "home";
    private final List<FrameLayout> bottomSlots = new ArrayList<>();
    private static final String ICON_HOME = "icons/home-agreement.png";
    private static final String ICON_TASKS = "icons/checklist.png";
    private static final String ICON_CHAT = "icons/message.png";
    private static final String ICON_PROFILE = "icons/user.png";
    private static final String ICON_LOGO = "icons/Logo.png";
    private static final String ICON_BELL = "icons/bell.png";
    private static final String ICON_LOCK = "media/sequre.png";
    private static final String[] PROJECT_FALLBACK_IMAGES = {
            "media/fon/photo1.png",
            "media/fon/photo2.png",
            "media/fon/photo3.png",
            "media/fon/photo4.png"
    };

    public HomeScreen(ScreenHost host, ProjectScreen projectScreen) {
        this.host = host;
        this.projectScreen = projectScreen;
    }

    public void show() {
        host.setScreen("", false);
        host.root().setPadding(host.dp(18), host.dp(22), host.dp(18), host.dp(106));

        JSONObject activeProject = host.sessionStore().getProject();
        if (activeProject == null) {
            showProjectCatalog();
        } else {
            showProjectDashboard(activeProject);
        }
    }

    private void showProjectCatalog() {
        addTopBar();
        addSearchRow();
        projectsContainer = new LinearLayout(host.context());
        projectsContainer.setOrientation(LinearLayout.VERTICAL);
        host.root().addView(projectsContainer);
        host.addProgress();
        addBottomNavigation();
        loadProjects();
    }

    private void showProjectDashboard(JSONObject project) {
        addProjectTopBar(project);
        addLeaveProjectLink();
        dashboardContainer = new LinearLayout(host.context());
        dashboardContainer.setOrientation(LinearLayout.VERTICAL);
        dashboardContainer.addView(UiKit.text(host.context(), "Загрузка проекта...", 14, UiKit.TEXT_SECONDARY));
        host.root().addView(dashboardContainer);
        host.addProgress();
        addBottomNavigation();
        loadProjectDashboard();
    }

    private void addTopBar() {
        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(appLogo(), new LinearLayout.LayoutParams(host.dp(44), host.dp(44)));

        LinearLayout titles = new LinearLayout(host.context());
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(host.dp(10), 0, 0, 0);
        TextView title = UiKit.text(host.context(), "Good evening, " + userDisplayName() + " 👋", 20, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        titles.addView(title);
        titles.addView(UiKit.text(host.context(), "Here are your projects", 12, UiKit.TEXT_SECONDARY));
        row.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        row.addView(topIcon("⌕"));
        row.addView(topAssetIcon(ICON_BELL, () -> showNotificationsPopup()));
        host.root().addView(row);
        host.addSpace(12);
    }

    private void addProjectTopBar(JSONObject project) {
        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(appLogo(), new LinearLayout.LayoutParams(host.dp(44), host.dp(44)));

        LinearLayout titles = new LinearLayout(host.context());
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(host.dp(10), 0, 0, 0);
        TextView title = UiKit.text(host.context(), project.optString("title", "Project"), 22, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        titles.addView(title);
        titles.addView(UiKit.text(host.context(), "Project dashboard", 12, UiKit.TEXT_SECONDARY));
        row.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        row.addView(topIcon("⌕"));
        row.addView(topAssetIcon(ICON_BELL, () -> showNotificationsPopup()));
        host.root().addView(row);
        host.addSpace(14);
    }

    private void addLeaveProjectLink() {
        TextView leave = UiKit.text(host.context(), "Выйти из проекта", 14, 0xFFFF5A5F);
        leave.setGravity(Gravity.RIGHT);
        leave.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        leave.setOnClickListener(v -> leaveProject());
        host.root().addView(leave);
        host.addSpace(10);
    }

    private void leaveProject() {
        host.executor().execute(() -> {
            try {
                host.apiClient().delete(
                        "/api/calendar/projects/current/leave/",
                        host.sessionStore().getUserToken(),
                        host.sessionStore().getProjectToken()
                );
                host.activity().runOnUiThread(() -> {
                    host.sessionStore().clearProject();
                    host.toast("Вы вышли из проекта");
                    host.showHomeScreen();
                });
            } catch (Exception e) {
                host.activity().runOnUiThread(() -> host.showError(e));
            }
        });
    }

    private void loadProjectDashboard() {
        host.runApi(() -> (JSONObject) host.apiClient().get(
                "/api/calendar/projects/current/dashboard/",
                host.sessionStore().getUserToken(),
                host.sessionStore().getProjectToken()
        ), this::renderProjectDashboard);
    }

    private void renderProjectDashboard(JSONObject dashboard) {
        if (dashboardContainer == null) {
            return;
        }
        dashboardContainer.removeAllViews();
        dashboardContainer.addView(platformHeader(dashboard));
        dashboardContainer.addView(todayTasksCard(dashboard.optJSONArray("today_tasks")));
        dashboardContainer.addView(statisticsCard(dashboard.optJSONObject("statistics")));
        dashboardContainer.addView(activityCard(dashboard.optJSONArray("activity")));
    }

    private View platformHeader(JSONObject dashboard) {
        LinearLayout card = host.card();
        card.setPadding(host.dp(14), host.dp(14), host.dp(14), host.dp(12));

        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = appLogo();
        row.addView(logo, new LinearLayout.LayoutParams(host.dp(38), host.dp(38)));

        LinearLayout text = new LinearLayout(host.context());
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(host.dp(10), 0, 0, 0);
        JSONObject project = dashboard.optJSONObject("project");
        TextView title = UiKit.text(host.context(), project == null ? "TurboTasks Platform" : project.optString("title", "TurboTasks Platform"), 15, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        text.addView(title);

        LinearLayout users = new LinearLayout(host.context());
        users.setGravity(Gravity.CENTER_VERTICAL);
        JSONArray members = dashboard.optJSONArray("recent_members");
        if (members != null) {
            for (int i = 0; i < members.length(); i++) {
                users.addView(userAvatar(members.optJSONObject(i), 24));
            }
        }
        TextView count = UiKit.text(host.context(), dashboard.optInt("members_count", 0) + " members", 11, UiKit.TEXT_SECONDARY);
        count.setPadding(host.dp(6), 0, 0, 0);
        users.addView(count);
        text.addView(users);

        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(row);
        return card;
    }

    private View todayTasksCard(JSONArray tasks) {
        LinearLayout card = host.card();
        LinearLayout header = new LinearLayout(host.context());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = UiKit.text(host.context(), "Today's Tasks", 16, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(UiKit.text(host.context(), "View all", 12, UiKit.BLUE));
        card.addView(header);

        ScrollView scrollView = new ScrollView(host.context());
        LinearLayout list = new LinearLayout(host.context());
        list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(list);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                host.dp(210)
        );
        scrollParams.setMargins(0, host.dp(8), 0, 0);
        card.addView(scrollView, scrollParams);

        if (tasks == null || tasks.length() == 0) {
            list.addView(UiKit.text(host.context(), "На сегодня задач нет", 13, UiKit.TEXT_SECONDARY));
            return card;
        }
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject task = tasks.optJSONObject(i);
            if (task != null) {
                list.addView(taskRow(task));
            }
        }
        return card;
    }

    private View taskRow(JSONObject task) {
        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(host.dp(10), host.dp(9), host.dp(10), host.dp(9));
        row.setBackground(UiKit.round(Color.rgb(12, 28, 56), host.dp(14)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, host.dp(5), 0, host.dp(5));
        row.setLayoutParams(params);

        LinearLayout text = new LinearLayout(host.context());
        text.setOrientation(LinearLayout.VERTICAL);
        TextView taskTitle = UiKit.text(host.context(), task.optString("title", "Task"), 13, UiKit.WHITE);
        taskTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        text.addView(taskTitle);
        text.addView(UiKit.text(host.context(), taskMeta(task), 11, UiKit.TEXT_SECONDARY));
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout avatars = new LinearLayout(host.context());
        avatars.setGravity(Gravity.CENTER_VERTICAL);
        avatars.addView(userAvatar(task.optJSONObject("author"), 26));
        TextView arrow = UiKit.text(host.context(), "›", 18, UiKit.TEXT_SECONDARY);
        arrow.setGravity(Gravity.CENTER);
        avatars.addView(arrow, new LinearLayout.LayoutParams(host.dp(16), host.dp(26)));
        avatars.addView(userAvatar(task.optJSONObject("assignee"), 26));
        row.addView(avatars);
        return row;
    }

    private String taskMeta(JSONObject task) {
        String importance = task.optString("importance", "normal");
        String deadline = task.optString("deadline", "");
        return importance + (deadline.isEmpty() || "null".equals(deadline) ? "" : " • Due " + deadline.substring(0, Math.min(10, deadline.length())));
    }

    private View statisticsCard(JSONObject stats) {
        LinearLayout wrapper = new LinearLayout(host.context());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        TextView title = UiKit.text(host.context(), "Statistics", 16, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        wrapper.addView(title);

        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(statBox("Completed", String.valueOf(stats == null ? 0 : stats.optInt("completed")), 0xFF45D483), statParams(0, 4));
        row.addView(statBox("In Progress", String.valueOf(stats == null ? 0 : stats.optInt("in_progress")), UiKit.BLUE), statParams(4, 4));
        row.addView(statBox("Overdue", String.valueOf(stats == null ? 0 : stats.optInt("overdue")), 0xFFFF5A5F), statParams(4, 4));
        row.addView(statBox("Productivity", (stats == null ? 0 : stats.optInt("productivity")) + "%", 0xFF8E7CFF), statParams(4, 0));
        wrapper.addView(row);
        return wrapper;
    }

    private View statBox(String label, String value, int color) {
        LinearLayout box = new LinearLayout(host.context());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
        box.setBackground(UiKit.round(Color.rgb(12, 28, 56), host.dp(12)));
        box.addView(UiKit.text(host.context(), label, 10, UiKit.TEXT_SECONDARY));
        TextView valueView = UiKit.text(host.context(), value, 18, color);
        valueView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        box.addView(valueView);
        return box;
    }

    private LinearLayout.LayoutParams statParams(int leftDp, int rightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, host.dp(70), 1);
        params.setMargins(host.dp(leftDp), host.dp(8), host.dp(rightDp), host.dp(8));
        return params;
    }

    private View activityCard(JSONArray activities) {
        LinearLayout card = host.card();
        TextView title = UiKit.text(host.context(), "Activity", 16, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        card.addView(title);
        if (activities == null || activities.length() == 0) {
            card.addView(UiKit.text(host.context(), "Активности пока нет", 13, UiKit.TEXT_SECONDARY));
            return card;
        }
        int limit = Math.min(activities.length(), 5);
        for (int i = 0; i < limit; i++) {
            JSONObject item = activities.optJSONObject(i);
            if (item != null) {
                card.addView(activityRow(item));
            }
        }
        return card;
    }

    private View activityRow(JSONObject activity) {
        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, host.dp(6), 0, host.dp(6));
        row.addView(userAvatar(activity.optJSONObject("actor"), 32));
        LinearLayout text = new LinearLayout(host.context());
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(host.dp(10), 0, 0, 0);
        text.addView(UiKit.text(host.context(), activity.optString("message", "Project activity"), 13, UiKit.WHITE));
        text.addView(UiKit.text(host.context(), activity.optString("created_at", ""), 10, UiKit.TEXT_SECONDARY));
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private FrameLayout userAvatar(JSONObject user, int sizeDp) {
        FrameLayout avatar = new FrameLayout(host.context());
        avatar.setBackground(UiKit.round(Color.rgb(28, 50, 92), host.dp(sizeDp / 2)));
        String photoUrl = user == null ? "" : user.optString("photo_url", "");
        if (!photoUrl.isEmpty() && !"null".equals(photoUrl)) {
            ImageView image = new ImageView(host.context());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            loadRemoteImage(image, photoUrl, true);
        } else {
            TextView initial = UiKit.text(host.context(), userInitial(user), Math.max(10, sizeDp / 3), UiKit.WHITE);
            initial.setGravity(Gravity.CENTER);
            initial.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            avatar.addView(initial, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(host.dp(sizeDp), host.dp(sizeDp));
        params.setMargins(0, 0, -host.dp(5), 0);
        avatar.setLayoutParams(params);
        return avatar;
    }

    private String userInitial(JSONObject user) {
        String name = user == null ? "" : user.optString("full_name", user.optString("nickname", ""));
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private TextView topIcon(String text) {
        TextView icon = UiKit.text(host.context(), text, 18, UiKit.TEXT_SECONDARY);
        icon.setGravity(Gravity.CENTER);
        icon.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        icon.setBackground(UiKit.round(UiKit.SURFACE_LIGHT, host.dp(18)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(host.dp(36), host.dp(36));
        params.setMargins(host.dp(8), 0, 0, 0);
        icon.setLayoutParams(params);
        return icon;
    }

    private ImageView appLogo() {
        ImageView logo = new ImageView(host.context());
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loadAssetImage(logo, ICON_LOGO);
        return logo;
    }

    private FrameLayout topAssetIcon(String assetName, Runnable action) {
        FrameLayout button = new FrameLayout(host.context());
        button.setBackground(UiKit.round(UiKit.SURFACE_LIGHT, host.dp(18)));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(host.dp(36), host.dp(36));
        params.setMargins(host.dp(8), 0, 0, 0);
        button.setLayoutParams(params);

        ImageView icon = new ImageView(host.context());
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loadAssetImage(icon, assetName);
        button.addView(icon, new FrameLayout.LayoutParams(host.dp(20), host.dp(20), Gravity.CENTER));
        return button;
    }

    private void showNotificationsPopup() {
        Dialog dialog = new Dialog(host.context());
        LinearLayout card = new LinearLayout(host.context());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(host.dp(18), host.dp(18), host.dp(18), host.dp(16));
        card.setBackground(UiKit.stroke(UiKit.SURFACE, UiKit.BORDER_GRAY, host.dp(1), host.dp(22)));

        TextView title = UiKit.text(host.context(), "Notifications", 20, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        card.addView(title);

        LinearLayout list = new LinearLayout(host.context());
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(UiKit.text(host.context(), "Загрузка уведомлений...", 13, UiKit.TEXT_SECONDARY));
        card.addView(list);

        Button clear = UiKit.primaryButton(host.context(), "Clear notifications");
        clear.setOnClickListener(v -> clearNotifications(dialog));
        card.addView(clear);

        dialog.setContentView(card);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            shownWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shownWindow.setLayout(
                    (int) (host.activity().getResources().getDisplayMetrics().widthPixels * 0.9f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
        loadNotifications(list);
    }

    private void loadNotifications(LinearLayout list) {
        host.executor().execute(() -> {
            try {
                Object response = host.apiClient().get(
                        "/api/calendar/notifications/",
                        host.sessionStore().getUserToken(),
                        null
                );
                JSONArray notifications = response instanceof JSONArray ? (JSONArray) response : new JSONArray();
                host.activity().runOnUiThread(() -> renderNotifications(list, notifications));
            } catch (Exception e) {
                host.activity().runOnUiThread(() -> {
                    list.removeAllViews();
                    list.addView(UiKit.text(host.context(), friendlyNotificationError(e), 13, 0xFFFF5A5F));
                });
            }
        });
    }

    private String friendlyNotificationError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Не удалось загрузить уведомления.";
        }
        if (message.contains("миграции") || message.contains("backend")) {
            return message;
        }
        return "Не удалось загрузить уведомления. Проверьте backend и миграции базы данных.";
    }

    private void renderNotifications(LinearLayout list, JSONArray notifications) {
        list.removeAllViews();
        if (notifications.length() == 0) {
            list.addView(UiKit.text(host.context(), "Уведомлений нет", 13, UiKit.TEXT_SECONDARY));
            return;
        }
        for (int i = 0; i < notifications.length(); i++) {
            JSONObject item = notifications.optJSONObject(i);
            if (item != null) {
                list.addView(notificationRow(item));
            }
        }
    }

    private View notificationRow(JSONObject item) {
        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(host.dp(12), host.dp(10), host.dp(12), host.dp(10));
        row.setBackground(UiKit.round(Color.rgb(12, 28, 56), host.dp(16)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, host.dp(8), 0, host.dp(8));
        row.setLayoutParams(params);

        TextView title = UiKit.text(host.context(), item.optString("title", "Notification"), 14, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        row.addView(title);
        row.addView(UiKit.text(host.context(), item.optString("message", ""), 12, UiKit.TEXT_SECONDARY));
        return row;
    }

    private void clearNotifications(Dialog dialog) {
        host.executor().execute(() -> {
            try {
                host.apiClient().delete(
                        "/api/calendar/notifications/",
                        host.sessionStore().getUserToken(),
                        null
                );
                host.activity().runOnUiThread(() -> {
                    dialog.dismiss();
                    host.toast("Уведомления очищены");
                });
            } catch (Exception e) {
                host.activity().runOnUiThread(() -> host.showError(e));
            }
        });
    }

    private void addSearchRow() {
        searchInput = host.input("Search projects", false);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderProjects();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        host.root().addView(searchInput);
        host.addSpace(10);
    }

    private void loadProjects() {
        host.runApi(() -> {
            Object response = host.apiClient().get(
                    "/api/calendar/projects/all/",
                    host.sessionStore().getUserToken(),
                    null
            );
            JSONObject wrapper = new JSONObject();
            wrapper.put("results", response);
            return wrapper;
        }, response -> {
            projects.clear();
            JSONArray items = response.getJSONArray("results");
            for (int i = 0; i < items.length(); i++) {
                projects.add(items.getJSONObject(i));
            }
            renderProjects();
        });
    }

    private void addBottomNavigation() {
        FrameLayout content = host.activity().findViewById(android.R.id.content);
        LinearLayout nav = new LinearLayout(host.context());
        bottomSlots.clear();
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(host.dp(18), host.dp(8), host.dp(18), host.dp(8));
        nav.setBackground(UiKit.round(Color.rgb(7, 18, 39), host.dp(24)));

        nav.addView(bottomItem("home", ICON_HOME, () -> {
            activeBottomItem = "home";
            host.showHomeScreen();
        }), itemParams());
        nav.addView(bottomItem("tasks", ICON_TASKS, () -> {
            activeBottomItem = "tasks";
            host.toast("Tasks: список, Kanban, Calendar, фильтры и поиск.");
            host.showHomeScreen();
        }), itemParams());
        nav.addView(centerPlus(), itemParams());
        nav.addView(bottomItem("chat", ICON_CHAT, () -> {
            activeBottomItem = "chat";
            host.toast("Chat: каналы, личные сообщения и обсуждения задач.");
            host.showHomeScreen();
        }), itemParams());
        nav.addView(bottomItem("profile", ICON_PROFILE, () -> {
            activeBottomItem = "profile";
            toggleProfileMenu();
        }), itemParams());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                host.dp(76),
                Gravity.BOTTOM
        );
        params.setMargins(host.dp(12), 0, host.dp(12), host.dp(10));
        content.addView(nav, params);
    }

    private LinearLayout.LayoutParams itemParams() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
    }

    private View bottomItem(String id, String iconAsset, Runnable action) {
        FrameLayout slot = new FrameLayout(host.context());
        slot.setTag(id);
        ImageView icon = new ImageView(host.context());
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loadAssetImage(icon, iconAsset);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(host.dp(26), host.dp(26), Gravity.CENTER);
        slot.addView(icon, params);
        slot.setOnClickListener(v -> {
            activeBottomItem = id;
            refreshBottomSelection();
            action.run();
        });
        bottomSlots.add(slot);
        styleBottomSlot(slot);
        return slot;
    }

    private void refreshBottomSelection() {
        for (FrameLayout slot : bottomSlots) {
            styleBottomSlot(slot);
        }
    }

    private void styleBottomSlot(FrameLayout slot) {
        boolean active = activeBottomItem.equals(String.valueOf(slot.getTag()));
        slot.setBackground(active ? UiKit.round(Color.rgb(18, 48, 118), host.dp(18)) : null);
        if (slot.getChildCount() > 0 && slot.getChildAt(0) instanceof ImageView) {
            ImageView icon = (ImageView) slot.getChildAt(0);
            icon.setAlpha(active ? 1f : 0.56f);
            icon.setColorFilter(active ? UiKit.BLUE : UiKit.TEXT_SECONDARY);
        }
    }

    private View centerPlus() {
        FrameLayout slot = new FrameLayout(host.context());
        TextView plus = UiKit.text(host.context(), "+", 30, UiKit.WHITE);
        plus.setGravity(Gravity.CENTER);
        plus.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        plus.setBackground(UiKit.round(UiKit.BLUE, host.dp(28)));
        plus.setOnClickListener(v -> host.showProjectGateScreen());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(host.dp(56), host.dp(56), Gravity.CENTER);
        params.gravity = Gravity.CENTER;
        slot.addView(plus, params);
        return slot;
    }

    private void showCreateSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(host.context());
        LinearLayout sheet = new LinearLayout(host.context());
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(host.dp(22), host.dp(18), host.dp(22), host.dp(24));
        sheet.setBackground(UiKit.round(UiKit.SURFACE, host.dp(24)));

        TextView title = UiKit.text(host.context(), "Create", 22, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        sheet.addView(title);
        sheet.addView(createAction("Create Task", () -> {
            dialog.dismiss();
            host.toast("Создание задачи будет добавлено на экране Tasks.");
        }));
        sheet.addView(createAction("Create Project", () -> {
            dialog.dismiss();
            host.showProjectGateScreen();
        }));
        sheet.addView(createAction("Create Channel", () -> {
            dialog.dismiss();
            host.toast("Создание канала будет добавлено в Chat.");
        }));
        sheet.addView(createAction("Invite Member", () -> {
            dialog.dismiss();
            host.toast("Приглашение участника будет добавлено в проект.");
        }));
        sheet.addView(createAction("Schedule Meeting", () -> {
            dialog.dismiss();
            host.toast("Планирование встречи будет добавлено в календарь.");
        }));
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void toggleProfileMenu() {
        if (profileOverlay != null) {
            hideProfileMenu();
        } else {
            showProfileMenu();
        }
    }

    private void showProfileMenu() {
        FrameLayout content = host.activity().findViewById(android.R.id.content);
        profileOverlay = new FrameLayout(host.context());
        profileOverlay.setBackgroundColor(0x99000000);
        profileOverlay.setOnClickListener(v -> hideProfileMenu());

        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        overlayParams.setMargins(0, 0, 0, host.dp(92));
        content.addView(profileOverlay, overlayParams);

        LinearLayout panel = createProfilePanel();
        panel.setOnClickListener(v -> {
        });
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                (int) (host.activity().getResources().getDisplayMetrics().widthPixels * 0.62f),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.RIGHT
        );
        profileOverlay.addView(panel, panelParams);
        profilePanel = panel;

        TranslateAnimation animation = new TranslateAnimation(panelParams.width, 0, 0, 0);
        animation.setDuration(240);
        animation.setInterpolator(new DecelerateInterpolator());
        panel.startAnimation(animation);
    }

    private LinearLayout createProfilePanel() {
        LinearLayout panel = new LinearLayout(host.context());
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(host.dp(14), host.dp(16), host.dp(14), host.dp(16));
        panel.setBackground(UiKit.round(Color.rgb(8, 23, 48), host.dp(24)));

        TextView close = UiKit.text(host.context(), "×", 20, UiKit.TEXT_SECONDARY);
        close.setGravity(Gravity.RIGHT);
        close.setOnClickListener(v -> hideProfileMenu());
        panel.addView(close);

        panel.addView(profileHeader());
        panel.addView(profileStats());

        ScrollView scrollView = new ScrollView(host.context());
        LinearLayout menu = new LinearLayout(host.context());
        menu.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(menu);
        menu.addView(profileMenuItem("My Profile", "›", () -> host.toast("Переход на страницу профиля."), true));
        menu.addView(profileMenuItem("Settings", "›", () -> host.toast("Переход на страницу настроек."), false));
        menu.addView(profileMenuItem("Notifications", "›", () -> host.toast("Уведомления будут добавлены позже."), false));
        menu.addView(profileMenuItem("Theme", "Dark", () -> host.toast("Смена темы будет добавлена позже."), false));
        menu.addView(profileMenuItem("Time Zone", "GMT+3", () -> host.toast("Настройки часового пояса."), false));
        menu.addView(profileMenuItem("Language", "English", () -> host.toast("Смена языка будет добавлена позже."), false));
        menu.addView(profileMenuItem("Help & Support", "›", () -> host.toast("Помощь и поддержка."), false));
        menu.addView(profileMenuItem("What's New", "•", () -> host.toast("Что нового в TurboTasks."), false));
        menu.addView(profileMenuItem("Invite Members", "›", () -> host.toast("Пригласить участников."), false));
        menu.addView(profileMenuItem("Referral Program", "›", () -> host.toast("Реферальная программа."), false));
        panel.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        panel.addView(profileMenuItem("Log out", "", () -> {
            host.sessionStore().clearAll();
            hideProfileMenu();
            host.showAuthScreen(false);
        }, false, 0xFFFF5A5F));
        return panel;
    }

    private LinearLayout profileHeader() {
        LinearLayout header = new LinearLayout(host.context());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_HORIZONTAL);

        JSONObject user = host.sessionStore().getUser();
        FrameLayout avatar = new FrameLayout(host.context());
        avatar.setBackground(UiKit.stroke(Color.rgb(34, 69, 140), UiKit.BLUE, host.dp(2), host.dp(36)));
        avatar.setOnClickListener(v -> host.openProfilePhotoPicker());
        String photoUrl = user == null ? "" : user.optString("photo_url", "");
        if (!photoUrl.isEmpty() && !"null".equals(photoUrl)) {
            ImageView photo = new ImageView(host.context());
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.addView(photo, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            loadRemoteImage(photo, photoUrl, true);
        } else {
            TextView initials = UiKit.text(host.context(), initials(), 24, UiKit.WHITE);
            initials.setGravity(Gravity.CENTER);
            initials.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            avatar.addView(initials, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
        }
        header.addView(avatar, new LinearLayout.LayoutParams(host.dp(70), host.dp(70)));

        String name = user == null ? "TurboTasks User" : user.optString("nickname", "TurboTasks User");
        TextView nameView = UiKit.text(host.context(), name, 16, UiKit.WHITE);
        nameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        nameView.setGravity(Gravity.CENTER_HORIZONTAL);
        header.addView(nameView);
        TextView emailView = UiKit.text(host.context(), user == null ? "profile@turbotasks.com" : user.optString("email", "profile@turbotasks.com"), 11, UiKit.TEXT_SECONDARY);
        emailView.setGravity(Gravity.CENTER_HORIZONTAL);
        header.addView(emailView);
        return header;
    }

    private LinearLayout profileStats() {
        LinearLayout stats = new LinearLayout(host.context());
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(0, host.dp(10), 0, host.dp(10));
        stats.addView(statItem("Tasks Completed", "128"), itemParams());
        stats.addView(statItem("Projects", String.valueOf(projects.size())), itemParams());
        stats.addView(statItem("Team Members", "24"), itemParams());
        return stats;
    }

    private LinearLayout statItem(String label, String value) {
        LinearLayout item = new LinearLayout(host.context());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        TextView valueView = UiKit.text(host.context(), value, 15, UiKit.WHITE);
        valueView.setGravity(Gravity.CENTER);
        valueView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        item.addView(valueView);
        TextView labelView = UiKit.text(host.context(), label, 9, UiKit.TEXT_SECONDARY);
        labelView.setGravity(Gravity.CENTER);
        item.addView(labelView);
        return item;
    }

    private View profileMenuItem(String title, String right, Runnable action, boolean active) {
        return profileMenuItem(title, right, action, active, active ? UiKit.WHITE : UiKit.TEXT_PRIMARY);
    }

    private View profileMenuItem(String title, String right, Runnable action, boolean active, int color) {
        LinearLayout item = new LinearLayout(host.context());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(host.dp(10), host.dp(10), host.dp(10), host.dp(10));
        item.setBackground(UiKit.round(active ? Color.rgb(30, 61, 145) : UiKit.SURFACE, host.dp(12)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, host.dp(4), 0, host.dp(4));
        item.setLayoutParams(params);

        TextView titleView = UiKit.text(host.context(), title, 13, color);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        item.addView(titleView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView rightView = UiKit.text(host.context(), right, 12, UiKit.TEXT_SECONDARY);
        rightView.setGravity(Gravity.RIGHT);
        item.addView(rightView);
        item.setOnClickListener(v -> action.run());
        return item;
    }

    private String initials() {
        JSONObject user = host.sessionStore().getUser();
        if (user == null) {
            return "TT";
        }
        String nickname = user.optString("nickname", "TT").trim();
        if (nickname.length() >= 2) {
            return nickname.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return nickname.isEmpty() ? "TT" : nickname.toUpperCase(Locale.ROOT);
    }

    private String userDisplayName() {
        JSONObject user = host.sessionStore().getUser();
        if (user == null) {
            return "User";
        }
        String firstName = user.optString("first_name", "").trim();
        if (!firstName.isEmpty()) {
            return firstName;
        }
        String nickname = user.optString("nickname", "").trim();
        return nickname.isEmpty() ? "User" : nickname;
    }

    private void hideProfileMenu() {
        if (profileOverlay == null || profilePanel == null) {
            return;
        }
        View panel = profilePanel;
        TranslateAnimation animation = new TranslateAnimation(0, panel.getWidth(), 0, 0);
        animation.setDuration(180);
        animation.setInterpolator(new AccelerateInterpolator());
        animation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                FrameLayout content = host.activity().findViewById(android.R.id.content);
                content.removeView(profileOverlay);
                profileOverlay = null;
                profilePanel = null;
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {
            }
        });
        panel.startAnimation(animation);
    }

    private TextView createAction(String text, Runnable action) {
        TextView item = UiKit.text(host.context(), text, 18, UiKit.WHITE);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        item.setPadding(0, host.dp(14), 0, host.dp(14));
        item.setOnClickListener(v -> action.run());
        return item;
    }

    private void renderProjects() {
        if (projectsContainer == null) {
            return;
        }
        projectsContainer.removeAllViews();
        String query = searchInput == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        int rendered = 0;
        for (JSONObject project : projects) {
            String title = project.optString("title", project.optString("login"));
            if (!query.isEmpty() && !title.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            projectsContainer.addView(projectCard(project));
            rendered++;
        }
        if (rendered == 0) {
            TextView empty = UiKit.text(host.context(), "Проекты не найдены", 15, UiKit.TEXT_SECONDARY);
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            projectsContainer.addView(empty);
        }
    }

    private View projectCard(JSONObject project) {
        LinearLayout card = host.card();
        card.setPadding(host.dp(14), host.dp(14), host.dp(14), host.dp(12));
        card.setOnClickListener(v -> askProjectPassword(project));

        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView image = new ImageView(host.context());
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(UiKit.round(colorForProject(project.optInt("id")), host.dp(22)));
        String imageUrl = project.optString("image_url", "");
        if (!imageUrl.isEmpty() && !"null".equals(imageUrl)) {
            loadProjectImage(image, imageUrl);
        } else {
            loadAssetImage(image, fallbackImageForProject(project), true);
        }
        row.addView(image, new LinearLayout.LayoutParams(host.dp(44), host.dp(44)));

        LinearLayout info = new LinearLayout(host.context());
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        infoParams.setMargins(host.dp(12), 0, 0, 0);

        TextView title = UiKit.text(host.context(), project.optString("title", "Project"), 15, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        info.addView(title);
        String subtitle = project.optString("name", "Project")
                + " • " + generatedMembers(project) + " members"
                + " • " + generatedTasks(project) + " tasks";
        info.addView(UiKit.text(host.context(), subtitle, 11, UiKit.TEXT_SECONDARY));
        row.addView(info, infoParams);
        card.addView(row);

        int progress = generatedProgress(project);
        ProgressBar progressBar = new ProgressBar(host.context(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(progress);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                host.dp(8)
        );
        progressParams.setMargins(0, host.dp(12), 0, 0);
        card.addView(progressBar, progressParams);

        TextView footer = UiKit.text(host.context(), "Progress  " + progress + "%", 11, UiKit.TEXT_SECONDARY);
        footer.setGravity(Gravity.END);
        card.addView(footer);
        return card;
    }

    private void askProjectPassword(JSONObject project) {
        Dialog dialog = new Dialog(host.context());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = new LinearLayout(host.context());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(host.dp(18), host.dp(18), host.dp(18), host.dp(20));
        card.setBackground(UiKit.stroke(UiKit.SURFACE, UiKit.BORDER_GRAY, host.dp(1), host.dp(18)));

        TextView close = UiKit.text(host.context(), "×", 20, UiKit.TEXT_SECONDARY);
        close.setGravity(Gravity.RIGHT);
        close.setOnClickListener(v -> dialog.dismiss());
        card.addView(close);

        ImageView lock = new ImageView(host.context());
        lock.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loadAssetImage(lock, ICON_LOCK);
        LinearLayout.LayoutParams lockParams = new LinearLayout.LayoutParams(host.dp(96), host.dp(96));
        lockParams.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(lock, lockParams);

        TextView title = UiKit.text(host.context(), "Enter project password", 18, UiKit.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        card.addView(title);

        TextView projectName = UiKit.text(host.context(), project.optString("title", "Project"), 15, UiKit.BLUE);
        projectName.setGravity(Gravity.CENTER_HORIZONTAL);
        projectName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        card.addView(projectName);

        TextView message = UiKit.text(host.context(), "This project is protected.\nPlease enter the password to continue.", 12, UiKit.TEXT_SECONDARY);
        message.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, host.dp(12), 0, host.dp(8));
        card.addView(message, messageParams);

        EditText password = host.input("Project password", true);
        password.setHint("Password");
        card.addView(password);

        TextView forgot = UiKit.text(host.context(), "Forgot password?", 12, UiKit.BLUE);
        forgot.setGravity(Gravity.CENTER_HORIZONTAL);
        forgot.setOnClickListener(v -> host.toast("Восстановление пароля проекта будет добавлено позже."));
        card.addView(forgot);

        Button unlock = UiKit.primaryButton(host.context(), "Unlock project");
        unlock.setOnClickListener(v -> {
            dialog.dismiss();
            joinProject(project, password);
        });
        card.addView(unlock);

        dialog.setContentView(card);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (host.activity().getResources().getDisplayMetrics().widthPixels * 0.9f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            shownWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shownWindow.setLayout(
                    (int) (host.activity().getResources().getDisplayMetrics().widthPixels * 0.9f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void joinProject(JSONObject project, EditText password) {
        try {
            JSONObject body = new JSONObject()
                    .put("login", project.optString("login"))
                    .put("password", host.text(password));
            host.runApi(() -> (JSONObject) host.apiClient().post(
                    "/api/calendar/projects/join/",
                    body,
                    host.sessionStore().getUserToken(),
                    null
            ), host::saveProjectAndOpenHome);
        } catch (Exception e) {
            host.showError(e);
        }
    }

    private int generatedProgress(JSONObject project) {
        return 20 + Math.abs(project.optInt("id", 1) * 37) % 76;
    }

    private int generatedMembers(JSONObject project) {
        return 3 + Math.abs(project.optInt("id", 1) * 5) % 10;
    }

    private int generatedTasks(JSONObject project) {
        return 5 + Math.abs(project.optInt("id", 1) * 7) % 25;
    }

    private int colorForProject(int id) {
        int[] colors = {
                Color.rgb(116, 82, 255),
                Color.rgb(50, 122, 255),
                Color.rgb(33, 170, 130),
                Color.rgb(214, 145, 50)
        };
        return colors[Math.abs(id) % colors.length];
    }

    private String fallbackImageForProject(JSONObject project) {
        int projectId = project.optInt("id", 0);
        String savedImage = host.sessionStore().getProjectFallbackImage(projectId);
        if (!savedImage.isEmpty()) {
            return savedImage;
        }

        String imageAsset = PROJECT_FALLBACK_IMAGES[new Random().nextInt(PROJECT_FALLBACK_IMAGES.length)];
        host.sessionStore().saveProjectFallbackImage(projectId, imageAsset);
        return imageAsset;
    }

    private void loadProjectImage(ImageView target, String imageUrl) {
        loadRemoteImage(target, imageUrl, true);
    }

    private void loadRemoteImage(ImageView target, String imageUrl, boolean circular) {
        host.executor().execute(() -> {
            try {
                Bitmap bitmap = CachedImageLoader.load(host.context(), normalizeUrl(imageUrl));
                if (circular && bitmap != null) {
                    bitmap = circleBitmap(bitmap);
                }
                Bitmap finalBitmap = bitmap;
                target.post(() -> target.setImageBitmap(finalBitmap));
            } catch (Exception ignored) {
            }
        });
    }

    private String normalizeUrl(String imageUrl) {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        if (imageUrl.startsWith("/")) {
            return ApiConfig.BASE_URL + imageUrl;
        }
        return ApiConfig.BASE_URL + "/" + imageUrl;
    }

    private Bitmap circleBitmap(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);
        if (squared != source) {
            source.recycle();
        }

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        squared.recycle();
        return output;
    }

    private void loadAssetImage(ImageView target, String assetName) {
        loadAssetImage(target, assetName, false);
    }

    private void loadAssetImage(ImageView target, String assetName, boolean circular) {
        host.executor().execute(() -> {
            try (InputStream inputStream = host.context().getAssets().open(assetName)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (circular && bitmap != null) {
                    bitmap = circleBitmap(bitmap);
                }
                Bitmap finalBitmap = bitmap;
                target.post(() -> target.setImageBitmap(finalBitmap));
            } catch (Exception ignored) {
                target.post(() -> target.setImageDrawable(null));
            }
        });
    }
}
