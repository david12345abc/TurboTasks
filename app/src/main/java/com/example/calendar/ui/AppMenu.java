package com.example.calendar.ui;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AppMenu {
    private final ScreenHost host;
    private FrameLayout menuOverlay;

    public AppMenu(ScreenHost host) {
        this.host = host;
    }

    public void show() {
        if (menuOverlay != null) {
            return;
        }
        FrameLayout decor = (FrameLayout) host.activity().getWindow().getDecorView();
        menuOverlay = new FrameLayout(host.context());
        menuOverlay.setBackgroundColor(0x99000000);
        decor.addView(menuOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout menu = new LinearLayout(host.context());
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(host.dp(24), host.dp(28), host.dp(24), host.dp(24));
        menu.setBackground(UiKit.round(UiKit.SURFACE, 0));

        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                host.activity().getResources().getDisplayMetrics().heightPixels / 2,
                Gravity.TOP
        );
        menuOverlay.addView(menu, menuParams);

        menuOverlay.setOnClickListener(v -> hide(menu));
        menu.setOnClickListener(v -> {
        });

        addMenuItem(menu, "Главная", () -> {
            hide(menu);
            host.showHomeScreen();
        });
        addMenuItem(menu, "Проект", () -> {
            hide(menu);
            if (host.sessionStore().getProject() == null) {
                host.showProjectGateScreen();
            } else {
                host.showHomeScreen();
            }
        });
        if (host.isAppAdmin()) {
            addMenuItem(menu, "Страница администратора", () -> {
                hide(menu);
                host.showAdminScreen();
            });
        }

        TranslateAnimation animation = new TranslateAnimation(0, 0, -menuParams.height, 0);
        animation.setDuration(260);
        animation.setInterpolator(new DecelerateInterpolator());
        menu.startAnimation(animation);
    }

    private void hide(View menu) {
        if (menuOverlay == null) {
            return;
        }
        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, -menu.getHeight());
        animation.setDuration(200);
        animation.setInterpolator(new AccelerateInterpolator());
        animation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                FrameLayout decor = (FrameLayout) host.activity().getWindow().getDecorView();
                decor.removeView(menuOverlay);
                menuOverlay = null;
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {
            }
        });
        menu.startAnimation(animation);
    }

    private void addMenuItem(LinearLayout menu, String text, Runnable action) {
        TextView item = UiKit.text(host.context(), text, 24, UiKit.WHITE);
        item.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        item.setPadding(0, 0, 0, 0);
        item.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                host.dp(58)
        );
        params.setMargins(0, 0, 0, host.dp(8));
        menu.addView(item, params);
    }
}
