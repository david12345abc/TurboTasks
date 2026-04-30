package com.example.calendar.ui.project;

import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.calendar.ui.ScreenHost;
import com.example.calendar.ui.UiKit;

import org.json.JSONObject;

public class ProjectScreen {
    private final ScreenHost host;

    public ProjectScreen(ScreenHost host) {
        this.host = host;
    }

    public void showProjectGate() {
        host.setScreen("Проект", true);
        TextView hint = host.textView("Войдите в проект или создайте новый. После этого он станет активным для календаря.");
        host.root().addView(hint);
        host.addSpace(14);

        addProjectLoginBlock(host.root(), false);

        LinearLayout createCard = host.card();
        createCard.addView(host.section("Создать проект"));
        EditText projectLogin = host.input("Логин проекта", false);
        EditText projectPassword = host.input("Пароль проекта", true);
        EditText projectTitle = host.input("Название проекта", false);
        EditText roomName = host.input("Название комнаты", false);
        EditText description = host.input("Описание", false);
        Button create = UiKit.primaryButton(host.context(), "Создать проект");
        create.setOnClickListener(v -> createProject(projectLogin, projectPassword, projectTitle, roomName, description));
        createCard.addView(projectLogin);
        createCard.addView(projectPassword);
        createCard.addView(projectTitle);
        createCard.addView(roomName);
        createCard.addView(description);
        createCard.addView(create);
        host.root().addView(createCard);
        host.addProgress();
    }

    public void addProjectLoginBlock(LinearLayout parent, boolean showCreateLink) {
        LinearLayout joinCard = projectLoginCard(showCreateLink);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, host.dp(18));
        parent.addView(joinCard, params);
    }

    public LinearLayout projectLoginCard(boolean showCreateLink) {
        LinearLayout joinCard = showCreateLink
                ? (LinearLayout) UiKit.transparentCard(host.context())
                : host.card();
        TextView title = UiKit.text(host.context(), "Войти\nв проект", 32, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setGravity(Gravity.START);
        title.setLineSpacing(0, 0.95f);
        joinCard.addView(title);
        EditText joinLogin = host.input("Название проекта", false);
        EditText joinPassword = host.input("Пароль проекта", true);
        Button join = UiKit.primaryButton(host.context(), "Войти");
        join.setOnClickListener(v -> joinProject(joinLogin, joinPassword));
        joinCard.addView(joinLogin);
        joinCard.addView(joinPassword);
        joinCard.addView(join);
        if (showCreateLink) {
            TextView createLink = UiKit.text(host.context(), "Зарегистрировать проект", 15, UiKit.TEXT_PRIMARY);
            createLink.setGravity(Gravity.CENTER_HORIZONTAL);
            createLink.setPadding(0, host.dp(10), 0, 0);
            createLink.setOnClickListener(v -> host.showProjectGateScreen());
            joinCard.addView(createLink);
        }
        return joinCard;
    }

    private void joinProject(EditText joinLogin, EditText joinPassword) {
        try {
            JSONObject body = new JSONObject()
                    .put("login", host.text(joinLogin))
                    .put("password", host.text(joinPassword));
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

    private void createProject(
            EditText projectLogin,
            EditText projectPassword,
            EditText projectTitle,
            EditText roomName,
            EditText description
    ) {
        try {
            JSONObject body = new JSONObject()
                    .put("login", host.text(projectLogin))
                    .put("password", host.text(projectPassword))
                    .put("title", host.text(projectTitle))
                    .put("name", host.text(roomName))
                    .put("description", host.text(description));
            host.runApi(() -> (JSONObject) host.apiClient().post(
                    "/api/calendar/projects/",
                    body,
                    host.sessionStore().getUserToken(),
                    null
            ), host::saveProjectAndOpenHome);
        } catch (Exception e) {
            host.showError(e);
        }
    }
}
