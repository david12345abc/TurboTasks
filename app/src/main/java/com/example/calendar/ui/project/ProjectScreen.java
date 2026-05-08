package com.example.calendar.ui.project;

import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.calendar.ApiConfig;
import com.example.calendar.ui.CachedImageLoader;
import com.example.calendar.ui.ScreenHost;
import com.example.calendar.ui.UiKit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class ProjectScreen {
    private final ScreenHost host;
    private Uri selectedProjectImage;
    private final Set<Integer> invitedUserIds = new HashSet<>();
    private LinearLayout userResults;

    public ProjectScreen(ScreenHost host) {
        this.host = host;
    }

    public void showProjectGate() {
        selectedProjectImage = null;
        invitedUserIds.clear();
        host.setScreen("", false);
        host.root().setPadding(host.dp(18), host.dp(22), host.dp(18), host.dp(28));

        TextView back = UiKit.text(host.context(), "← Back to projects", 15, UiKit.TEXT_SECONDARY);
        back.setOnClickListener(v -> host.showHomeScreen());
        host.root().addView(back);

        LinearLayout createCard = (LinearLayout) UiKit.card(host.context());
        createCard.setPadding(host.dp(18), host.dp(22), host.dp(18), host.dp(18));
        TextView title = UiKit.text(host.context(), "New Project", 24, UiKit.WHITE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        createCard.addView(title);
        createCard.addView(UiKit.text(host.context(), "Let's create something great", 13, UiKit.TEXT_SECONDARY));

        FrameLayout uploadBox = new FrameLayout(host.context());
        uploadBox.setBackground(UiKit.round(Color.rgb(14, 30, 62), host.dp(36)));
        TextView uploadText = UiKit.text(host.context(), "▧\nUpload icon", 14, UiKit.BLUE);
        uploadText.setGravity(Gravity.CENTER);
        uploadBox.addView(uploadText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        LinearLayout.LayoutParams uploadParams = new LinearLayout.LayoutParams(host.dp(78), host.dp(78));
        uploadParams.gravity = Gravity.CENTER_HORIZONTAL;
        uploadParams.setMargins(0, host.dp(18), 0, host.dp(8));
        createCard.addView(uploadBox, uploadParams);
        uploadBox.setOnClickListener(v -> host.openProjectImagePicker(imageUri -> {
            selectedProjectImage = imageUri;
            uploadBox.removeAllViews();
            ImageView preview = new ImageView(host.context());
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            uploadBox.addView(preview, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            loadLocalCircularPreview(preview, imageUri);
        }));

        EditText projectLogin = host.input("Логин проекта", false);
        EditText projectPassword = host.input("Пароль проекта", true);
        EditText projectTitle = host.input("Project name", false);
        EditText description = host.input("Description", false);
        description.setSingleLine(false);
        description.setMinLines(3);
        description.setGravity(Gravity.TOP | Gravity.START);
        createCard.addView(projectLogin);
        createCard.addView(projectPassword);
        createCard.addView(projectTitle);
        createCard.addView(description);

        createCard.addView(UiKit.text(host.context(), "Invite members", 15, UiKit.WHITE));
        EditText userSearch = searchInput();
        createCard.addView(searchBox(userSearch));
        userResults = new LinearLayout(host.context());
        userResults.setOrientation(LinearLayout.VERTICAL);
        createCard.addView(userResults);
        userSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        Button create = UiKit.primaryButton(host.context(), "Create project");
        create.setOnClickListener(v -> createProject(projectLogin, projectPassword, projectTitle, description));
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
            EditText description
    ) {
        try {
            String login = host.text(projectLogin);
            String password = host.text(projectPassword);
            String title = host.text(projectTitle);
            JSONObject body = new JSONObject()
                    .put("login", login)
                    .put("password", password)
                    .put("title", title)
                    .put("name", title)
                    .put("description", host.text(description));
            host.runApi(() -> {
                JSONObject response = host.apiClient().createProject(
                        host.context(),
                        body,
                        selectedProjectImage,
                        host.sessionStore().getUserToken()
                );
                String projectToken = response.optString("project_token");
                for (Integer userId : invitedUserIds) {
                    host.apiClient().post(
                            "/api/calendar/projects/current/invite/",
                            new JSONObject()
                                    .put("user_id", userId)
                                    .put("project_login", login)
                                    .put("project_password", password),
                            host.sessionStore().getUserToken(),
                            projectToken
                    );
                }
                return response;
            }, host::saveProjectAndOpenHome);
        } catch (Exception e) {
            host.showError(e);
        }
    }

    private LinearLayout searchBox(EditText input) {
        LinearLayout box = new LinearLayout(host.context());
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(host.dp(14), 0, host.dp(14), 0);
        box.setBackground(UiKit.stroke(Color.rgb(12, 28, 56), UiKit.BORDER_GRAY, host.dp(1), host.dp(18)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                host.dp(52)
        );
        params.setMargins(0, host.dp(8), 0, host.dp(10));
        box.setLayoutParams(params);

        TextView icon = UiKit.text(host.context(), "⌕", 20, UiKit.TEXT_SECONDARY);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon, new LinearLayout.LayoutParams(host.dp(28), LinearLayout.LayoutParams.MATCH_PARENT));
        box.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return box;
    }

    private EditText searchInput() {
        EditText input = new EditText(host.context());
        input.setHint("Search by name or nickname");
        input.setHintTextColor(UiKit.TEXT_SECONDARY);
        input.setTextColor(UiKit.WHITE);
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setPadding(host.dp(8), 0, 0, 0);
        input.setBackgroundColor(Color.TRANSPARENT);
        return input;
    }

    private void searchUsers(String query) {
        if (userResults == null) {
            return;
        }
        String trimmed = query.trim();
        if (trimmed.length() < 2) {
            userResults.removeAllViews();
            return;
        }
        host.runApi(() -> (JSONObject) host.apiClient().get(
                "/api/v1/users/search/?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8"),
                host.sessionStore().getUserToken(),
                null
        ), response -> {
            userResults.removeAllViews();
            JSONArray users = response.optJSONArray("results");
            if (users == null || users.length() == 0) {
                userResults.addView(UiKit.text(host.context(), "Пользователи не найдены", 13, UiKit.TEXT_SECONDARY));
                return;
            }
            int limit = Math.min(users.length(), 5);
            for (int i = 0; i < limit; i++) {
                userResults.addView(userRow(users.getJSONObject(i)));
            }
        });
    }

    private LinearLayout userRow(JSONObject user) {
        LinearLayout row = new LinearLayout(host.context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
        row.setBackground(UiKit.round(Color.rgb(12, 28, 56), host.dp(18)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, host.dp(6), 0, host.dp(6));
        row.setLayoutParams(rowParams);

        FrameLayout avatar = new FrameLayout(host.context());
        avatar.setBackground(UiKit.round(Color.rgb(28, 50, 92), host.dp(19)));
        TextView initials = UiKit.text(host.context(), initials(user), 13, UiKit.WHITE);
        initials.setGravity(Gravity.CENTER);
        avatar.addView(initials, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        row.addView(avatar, new LinearLayout.LayoutParams(host.dp(38), host.dp(38)));
        loadUserPhoto(avatar, user.optString("photo_url", ""));

        LinearLayout textColumn = new LinearLayout(host.context());
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setPadding(host.dp(10), 0, host.dp(8), 0);
        String name = displayName(user);
        TextView title = UiKit.text(host.context(), name, 14, UiKit.WHITE);
        TextView nickname = UiKit.text(host.context(), "@" + user.optString("nickname", "user"), 12, UiKit.TEXT_SECONDARY);
        textColumn.addView(title);
        textColumn.addView(nickname);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        int userId = user.optInt("id");
        Button invite = UiKit.secondaryButton(host.context(), invitedUserIds.contains(userId) ? "Invited" : "Invite");
        invite.setOnClickListener(v -> {
            invitedUserIds.add(userId);
            invite.setText("Invited");
            host.toast("Пользователь будет приглашён после создания проекта.");
        });
        row.addView(invite, new LinearLayout.LayoutParams(host.dp(96), host.dp(42)));
        return row;
    }

    private String displayName(JSONObject user) {
        String firstName = user.optString("first_name", "").trim();
        String lastName = user.optString("last_name", "").trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.optString("nickname", "User") : fullName;
    }

    private String initials(JSONObject user) {
        String name = displayName(user);
        if (name.isEmpty()) {
            return "?";
        }
        return name.substring(0, 1).toUpperCase();
    }

    private void loadLocalCircularPreview(ImageView target, Uri imageUri) {
        host.executor().execute(() -> {
            try (InputStream inputStream = host.context().getContentResolver().openInputStream(imageUri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Bitmap circular = circularBitmap(bitmap);
                target.post(() -> target.setImageBitmap(circular));
            } catch (Exception ignored) {
            }
        });
    }

    private void loadUserPhoto(FrameLayout avatar, String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            return;
        }
        ImageView photo = new ImageView(host.context());
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.addView(photo, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        host.executor().execute(() -> {
            try {
                Bitmap bitmap = CachedImageLoader.load(host.context(), normalizeUrl(photoUrl));
                Bitmap circular = circularBitmap(bitmap);
                photo.post(() -> photo.setImageBitmap(circular));
            } catch (Exception ignored) {
            }
        });
    }

    private String normalizeUrl(String imageUrl) {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        return ApiConfig.BASE_URL + imageUrl;
    }

    private Bitmap circularBitmap(Bitmap source) {
        if (source == null) {
            return null;
        }
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        if (squared != source) {
            squared.recycle();
        }
        return output;
    }
}
