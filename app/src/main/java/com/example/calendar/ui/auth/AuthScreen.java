package com.example.calendar.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.calendar.ApiConfig;
import com.example.calendar.ui.ScreenHost;
import com.example.calendar.ui.UiKit;

import org.json.JSONObject;

import java.io.InputStream;

public class AuthScreen {
    private final ScreenHost host;
    private static final String APP_LOGO = "icons/LogoApp.png";
    private static final String ICON_GOOGLE = "media/projectimages/google.png";
    private static final String ICON_GITHUB = "media/projectimages/github.png";
    private static final String ICON_APPLE = "media/projectimages/apple.png";
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String VK_AUTH_URL = "https://id.vk.ru/authorize";

    public AuthScreen(ScreenHost host) {
        this.host = host;
    }

    public void show(boolean registration) {
        host.setScreen("", false);
        host.root().setGravity(Gravity.CENTER_VERTICAL);
        host.root().setPadding(host.dp(24), host.dp(26), host.dp(24), host.dp(26));

        ImageView appLogo = new ImageView(host.context());
        appLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loadAssetIcon(appLogo, APP_LOGO);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(host.dp(88), host.dp(88));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.setMargins(0, 0, 0, host.dp(14));
        host.root().addView(appLogo, logoParams);

        TextView step = UiKit.text(host.context(), registration ? "04. Register" : "03. Login", 12, UiKit.TEXT_SECONDARY);
        step.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        host.root().addView(step);

        LinearLayout card = host.card();
        card.setPadding(host.dp(20), host.dp(22), host.dp(20), host.dp(20));
        TextView hero = UiKit.text(
                host.context(),
                registration ? "Create account" : "Welcome back!",
                18,
                UiKit.WHITE
        );
        hero.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        card.addView(hero);
        card.addView(UiKit.text(
                host.context(),
                registration ? "Let's get you all set up" : "Please login to your account",
                12,
                UiKit.TEXT_SECONDARY
        ));
        addInnerSpace(card, 10);

        EditText nickname = host.input(registration ? "Full name" : "Email", false);
        EditText password = host.input("Password", true);
        EditText firstName = host.input("First name", false);
        EditText lastName = host.input("Last name", false);
        firstName.setVisibility(registration ? View.VISIBLE : View.GONE);
        lastName.setVisibility(registration ? View.VISIBLE : View.GONE);

        Button submit = UiKit.primaryButton(host.context(), registration ? "Sign up" : "Log in");
        submit.setOnClickListener(v -> submit(registration, nickname, password, firstName, lastName));

        card.addView(nickname);
        if (registration) {
            card.addView(firstName);
            card.addView(lastName);
        }
        card.addView(password);
        if (!registration) {
            TextView forgotPassword = UiKit.text(host.context(), "Forgot password?", 11, UiKit.BLUE);
            forgotPassword.setGravity(Gravity.END);
            forgotPassword.setOnClickListener(v -> host.toast("Восстановление пароля будет добавлено после API endpoint."));
            card.addView(forgotPassword);
        }
        card.addView(submit);
        addSocialAuthBlock(card, registration);
        addBottomSwitch(card, registration);
        host.root().addView(card);
        host.addProgress();
    }

    private void addSocialAuthBlock(LinearLayout card, boolean registration) {
        addInnerSpace(card, 8);
        TextView divider = UiKit.text(host.context(), "or continue with", 11, UiKit.TEXT_SECONDARY);
        divider.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(divider);

        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.addView(socialButton(ICON_GOOGLE, "Google", () -> openGoogleAuth(registration)), socialParams(0, 0, 6, 0));
        row.addView(socialButton(ICON_GITHUB, "GitHub", () -> openGithubAuth(registration)), socialParams(6, 0, 6, 0));
        row.addView(socialButton(ICON_APPLE, "Apple", () -> host.toast("Apple Sign In будет подключён после Apple client id.")), socialParams(6, 0, 0, 0));
        card.addView(row);
    }

    private void addBottomSwitch(LinearLayout card, boolean registration) {
        TextView switchMode = UiKit.text(
                host.context(),
                registration ? "Already have an account?  Login" : "Don't have an account?  Sign up",
                12,
                UiKit.TEXT_SECONDARY
        );
        switchMode.setGravity(Gravity.CENTER_HORIZONTAL);
        switchMode.setOnClickListener(v -> host.showAuthScreen(!registration));
        addInnerSpace(card, 4);
        card.addView(switchMode);
    }

    private LinearLayout socialButton(String iconAsset, String label, Runnable action) {
        LinearLayout button = new LinearLayout(host.context());
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
        button.setBackground(UiKit.stroke(UiKit.SURFACE_LIGHT, UiKit.BORDER_GRAY, host.dp(1), host.dp(14)));
        button.setOnClickListener(v -> action.run());

        ImageView iconView = new ImageView(host.context());
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loadAssetIcon(iconView, iconAsset);
        button.addView(iconView, new LinearLayout.LayoutParams(host.dp(24), host.dp(24)));

        TextView labelView = UiKit.text(host.context(), label, 10, UiKit.TEXT_SECONDARY);
        labelView.setGravity(Gravity.CENTER);
        button.addView(labelView);
        return button;
    }

    private void loadAssetIcon(ImageView target, String assetName) {
        host.executor().execute(() -> {
            try (InputStream inputStream = host.context().getAssets().open(assetName)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                target.post(() -> target.setImageBitmap(bitmap));
            } catch (Exception ignored) {
                target.post(() -> target.setImageDrawable(null));
            }
        });
    }

    private LinearLayout.LayoutParams socialParams(int leftDp, int topDp, int rightDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, host.dp(68), 1);
        params.setMargins(host.dp(leftDp), host.dp(topDp), host.dp(rightDp), host.dp(bottomDp));
        return params;
    }

    private void addInnerSpace(LinearLayout parent, int heightDp) {
        View view = new View(host.context());
        parent.addView(view, new LinearLayout.LayoutParams(1, host.dp(heightDp)));
    }

    private void openGoogleAuth(boolean registration) {
        if (isBlank(ApiConfig.GOOGLE_CLIENT_ID) || isBlank(ApiConfig.GOOGLE_REDIRECT_URI)) {
            host.toast("Укажите GOOGLE_CLIENT_ID и GOOGLE_REDIRECT_URI в ApiConfig.");
            return;
        }
        Uri uri = Uri.parse(GOOGLE_AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", ApiConfig.GOOGLE_CLIENT_ID)
                .appendQueryParameter("redirect_uri", ApiConfig.GOOGLE_REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "openid email profile")
                .appendQueryParameter("prompt", "select_account")
                .appendQueryParameter("state", registration ? "register_google" : "login_google")
                .build();
        openAuthUrl(uri);
    }

    private void openGithubAuth(boolean registration) {
        if (isBlank(ApiConfig.GITHUB_CLIENT_ID) || isBlank(ApiConfig.GITHUB_REDIRECT_URI)) {
            host.toast("Укажите GITHUB_CLIENT_ID и GITHUB_REDIRECT_URI в ApiConfig.");
            return;
        }
        Uri uri = Uri.parse(GITHUB_AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", ApiConfig.GITHUB_CLIENT_ID)
                .appendQueryParameter("redirect_uri", ApiConfig.GITHUB_REDIRECT_URI)
                .appendQueryParameter("scope", "read:user user:email")
                .appendQueryParameter("state", registration ? "register_github" : "login_github")
                .build();
        openAuthUrl(uri);
    }

    private void openVkAuth(boolean registration) {
        if (isBlank(ApiConfig.VK_CLIENT_ID) || isBlank(ApiConfig.VK_REDIRECT_URI)) {
            host.toast("Укажите VK_CLIENT_ID и VK_REDIRECT_URI в ApiConfig.");
            return;
        }
        Uri uri = Uri.parse(VK_AUTH_URL).buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", ApiConfig.VK_CLIENT_ID)
                .appendQueryParameter("redirect_uri", ApiConfig.VK_REDIRECT_URI)
                .appendQueryParameter("scope", "email")
                .appendQueryParameter("state", registration ? "register_vk" : "login_vk")
                .appendQueryParameter("scheme", "dark")
                .build();
        openAuthUrl(uri);
    }

    private void openAuthUrl(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        host.activity().startActivity(intent);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
