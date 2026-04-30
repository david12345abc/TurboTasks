package com.example.calendar.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class UiKit {
    public static final int BACKGROUND = Color.rgb(15, 15, 16);
    public static final int SURFACE = Color.rgb(27, 27, 30);
    public static final int SURFACE_LIGHT = Color.rgb(37, 37, 41);
    public static final int TEXT_PRIMARY = Color.rgb(244, 244, 245);
    public static final int TEXT_SECONDARY = Color.rgb(166, 166, 170);
    public static final int BORDER_GRAY = Color.rgb(150, 150, 150);
    public static final int WHITE = Color.WHITE;

    private UiKit() {
    }

    public static TextView title(android.content.Context context, String text) {
        TextView view = text(context, text, 30, TEXT_PRIMARY);
        view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        view.setGravity(Gravity.START);
        return view;
    }

    public static TextView section(android.content.Context context, String text) {
        TextView view = text(context, text, 24, TEXT_PRIMARY);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return view;
    }

    public static TextView text(android.content.Context context, String text, int sp, int color) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        view.setPadding(0, dp(context, 4), 0, dp(context, 4));
        return view;
    }

    public static EditText input(android.content.Context context, String hint, boolean password) {
        EditText editText = new EditText(context);
        editText.setHint(hint);
        editText.setHintTextColor(TEXT_SECONDARY);
        editText.setTextColor(WHITE);
        editText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        editText.setSingleLine(true);
        editText.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        editText.setBackground(stroke(Color.BLACK, BORDER_GRAY, dp(context, 1), dp(context, 10)));
        if (password) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 50)
        );
        params.setMargins(0, dp(context, 8), 0, dp(context, 8));
        editText.setLayoutParams(params);
        return editText;
    }

    public static Button primaryButton(android.content.Context context, String text) {
        Button button = baseButton(context, text);
        button.setTextColor(Color.BLACK);
        button.setBackground(round(WHITE, dp(context, 24)));
        return button;
    }

    public static Button secondaryButton(android.content.Context context, String text) {
        Button button = baseButton(context, text);
        button.setTextColor(TEXT_PRIMARY);
        button.setBackground(round(SURFACE_LIGHT, dp(context, 14)));
        return button;
    }

    public static Button iconButton(android.content.Context context, String text) {
        Button button = secondaryButton(context, text);
        button.setMinWidth(dp(context, 48));
        return button;
    }

    public static View card(android.content.Context context) {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        view.setBackground(round(SURFACE, dp(context, 18)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(context, 12), 0, dp(context, 12));
        view.setLayoutParams(params);
        return view;
    }

    public static View transparentCard(android.content.Context context) {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        view.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(context, 12), 0, dp(context, 12));
        view.setLayoutParams(params);
        return view;
    }

    public static GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    public static GradientDrawable stroke(int color, int strokeColor, int strokeWidth, int radius) {
        GradientDrawable drawable = round(color, radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    public static int dp(android.content.Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static Button baseButton(android.content.Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 52)
        );
        params.setMargins(0, dp(context, 8), 0, dp(context, 8));
        button.setLayoutParams(params);
        return button;
    }
}
