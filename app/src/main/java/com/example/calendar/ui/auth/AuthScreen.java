package com.example.calendar.ui.auth;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.calendar.ui.ScreenHost;
import com.example.calendar.ui.UiKit;

import org.json.JSONObject;

public class AuthScreen {
    private final ScreenHost host;

    public AuthScreen(ScreenHost host) {
        this.host = host;
    }

    public void show(boolean registration) {
        host.setScreen("", false);
        host.addSpace(56);
        TextView hero = UiKit.title(
                host.context(),
                registration ? "Create\nAccount" : "Hey,\nWelcome Back"
        );
        host.root().addView(hero);
        host.root().addView(UiKit.text(
                host.context(),
                registration ? "Создайте аккаунт, а проект выберите после входа." : "Войдите, чтобы продолжить работу с календарём.",
                14,
                UiKit.TEXT_SECONDARY
        ));
        host.addSpace(24);

        EditText nickname = host.input("Никнейм", false);
        EditText password = host.input("Пароль", true);
        EditText firstName = host.input("Имя", false);
        EditText lastName = host.input("Фамилия", false);
        firstName.setVisibility(registration ? View.VISIBLE : View.GONE);
        lastName.setVisibility(registration ? View.VISIBLE : View.GONE);

        Button submit = UiKit.primaryButton(host.context(), registration ? "Создать аккаунт" : "Войти");
        submit.setOnClickListener(v -> submit(registration, nickname, password, firstName, lastName));

        Button switchMode = UiKit.secondaryButton(
                host.context(),
                registration ? "Уже есть аккаунт" : "Зарегистрироваться"
        );
        switchMode.setOnClickListener(v -> host.showAuthScreen(!registration));

        host.root().addView(nickname);
        host.root().addView(password);
        host.root().addView(firstName);
        host.root().addView(lastName);
        host.root().addView(submit);
        host.root().addView(switchMode);
        host.addProgress();
    }

    private void submit(boolean registration, EditText nickname, EditText password, EditText firstName, EditText lastName) {
        try {
            JSONObject body = new JSONObject()
                    .put("nickname", host.text(nickname))
                    .put("password", host.text(password));
            if (registration) {
                body.put("first_name", host.text(firstName));
                body.put("last_name", host.text(lastName));
            }
            host.runApi(() -> (JSONObject) host.apiClient().post(
                    registration ? "/api/v1/auth/register/" : "/api/v1/auth/login/",
                    body,
                    null,
                    null
            ), response -> {
                host.sessionStore().saveUser(response.getString("token"), response.getJSONObject("user"));
                host.sessionStore().clearProject();
                host.showHomeScreen();
            });
        } catch (Exception e) {
            host.showError(e);
        }
    }
}
