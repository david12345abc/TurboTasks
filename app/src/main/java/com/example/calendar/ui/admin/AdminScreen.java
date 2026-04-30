package com.example.calendar.ui.admin;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.calendar.ui.ScreenHost;
import com.example.calendar.ui.UiKit;

import org.json.JSONArray;
import org.json.JSONObject;

public class AdminScreen {
    private final ScreenHost host;
    private LinearLayout usersContainer;
    private LinearLayout projectsContainer;

    public AdminScreen(ScreenHost host) {
        this.host = host;
    }

    public void show() {
        host.setScreen("Админ-панель", true);
        host.root().addView(UiKit.title(host.context(), "Страница администратора"));
        Button back = UiKit.secondaryButton(host.context(), "Назад");
        back.setOnClickListener(v -> host.showHomeScreen());
        host.root().addView(back);

        LinearLayout createUserCard = host.card();
        createUserCard.addView(host.section("Создать пользователя"));
        EditText nickname = host.input("Никнейм", false);
        EditText password = host.input("Пароль", true);
        EditText appRole = host.input("Роль: admin/user/guest", false);
        Button create = UiKit.primaryButton(host.context(), "Создать пользователя");
        create.setOnClickListener(v -> createUser(nickname, password, appRole));
        createUserCard.addView(nickname);
        createUserCard.addView(password);
        createUserCard.addView(appRole);
        createUserCard.addView(create);
        host.root().addView(createUserCard);

        LinearLayout usersCard = host.card();
        usersCard.addView(host.section("Пользователи"));
        usersContainer = new LinearLayout(host.context());
        usersContainer.setOrientation(LinearLayout.VERTICAL);
        usersCard.addView(usersContainer);
        host.root().addView(usersCard);

        LinearLayout projectsCard = host.card();
        projectsCard.addView(host.section("Проекты"));
        projectsContainer = new LinearLayout(host.context());
        projectsContainer.setOrientation(LinearLayout.VERTICAL);
        projectsCard.addView(projectsContainer);
        host.root().addView(projectsCard);

        host.addProgress();
        loadUsers();
        loadProjects();
    }

    private void createUser(EditText nickname, EditText password, EditText appRole) {
        try {
            String role = host.text(appRole).isEmpty() ? "user" : host.text(appRole);
            JSONObject body = new JSONObject()
                    .put("nickname", host.text(nickname))
                    .put("password", host.text(password))
                    .put("app_role", role)
                    .put("is_active", true)
                    .put("is_staff", "admin".equals(role) || "superadmin".equals(role));
            host.runApi(() -> (JSONObject) host.apiClient().post(
                    "/api/admin/users/",
                    body,
                    host.sessionStore().getUserToken(),
                    null
            ), response -> {
                host.toast("Пользователь создан");
                loadUsers();
            });
        } catch (Exception e) {
            host.showError(e);
        }
    }

    private void loadUsers() {
        host.runApi(() -> (JSONObject) host.apiClient().get(
                "/api/admin/users/?page=1&page_size=50",
                host.sessionStore().getUserToken(),
                null
        ), response -> {
            usersContainer.removeAllViews();
            JSONArray users = response.getJSONArray("results");
            if (users.length() == 0) {
                usersContainer.addView(host.textView("Пользователей нет"));
                return;
            }
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                usersContainer.addView(host.textView(
                        user.optInt("id") + ". " + user.optString("nickname")
                                + "\nРоль: " + user.optString("app_role")
                                + "\nИмя: " + user.optString("first_name") + " " + user.optString("last_name")
                                + "\nОтдел: " + user.optString("department")
                                + "\nДолжность: " + user.optString("job_title")
                                + "\nАктивен: " + user.optBoolean("is_active")
                ));
                addDivider(usersContainer);
            }
        });
    }

    private void loadProjects() {
        host.runApi(() -> {
            Object response = host.apiClient().get(
                    "/api/calendar/admin/projects/",
                    host.sessionStore().getUserToken(),
                    null
            );
            JSONObject wrapper = new JSONObject();
            wrapper.put("results", response);
            return wrapper;
        }, response -> {
            projectsContainer.removeAllViews();
            JSONArray projects = response.getJSONArray("results");
            if (projects.length() == 0) {
                projectsContainer.addView(host.textView("Проектов нет"));
                return;
            }
            for (int i = 0; i < projects.length(); i++) {
                JSONObject project = projects.getJSONObject(i);
                projectsContainer.addView(host.textView(
                        project.optInt("id") + ". " + project.optString("title")
                                + "\nЛогин: " + project.optString("login")
                                + "\nКомната: " + project.optString("name")
                                + "\nСоздатель: " + project.optString("creator_nickname")
                                + "\nУчастников: " + project.optInt("members_count")
                                + "\nЗадач: " + project.optInt("tasks_count")
                                + "\nСоздан: " + project.optString("room_created_at")
                ));
                addDivider(projectsContainer);
            }
        });
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(host.context());
        divider.setBackgroundColor(UiKit.SURFACE_LIGHT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                host.dp(1)
        );
        params.setMargins(0, host.dp(10), 0, host.dp(10));
        parent.addView(divider, params);
    }
}
