package com.codex.quicklaunch;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.codex.quicklaunch.Entries.Entry;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int PAPER = 0xFFFBF7F0;
    private static final int INK = 0xFF28231F;
    private static final int MUTED = 0xFF6D5B51;
    private static final int ACCENT = 0xFFA83E2D;
    private static final int PALE = 0xFFF5E6DC;
    private static final int WHITE = 0xFFFFFDFA;
    private Entries store;
    private LinearLayout rows;
    private TextView sectionTitle;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Entries(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAPER);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });

        LinearLayout page = column();
        page.setPadding(dp(20), dp(22), dp(20), dp(24));
        scroll.addView(page);
        setContentView(scroll);

        TextView title = label(getString(R.string.app_name), 40, INK, true);
        page.addView(title);
        TextView subtitle = label("把常用的应用页面放在手边。", 16, MUTED, false);
        addWithTopMargin(page, subtitle, 4);

        Button add = action("＋  添加链接", true);
        add.setOnClickListener(view -> edit(null));
        LinearLayout actions = new LinearLayout(this);
        actions.addView(add, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button widget = action("添加桌面小组件", false);
        widget.setOnClickListener(view -> pinWidget());
        LinearLayout.LayoutParams widgetParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        widgetParams.setMarginStart(dp(8));
        actions.addView(widget, widgetParams);
        addWithTopMargin(page, actions, 22);

        sectionTitle = label("", 16, INK, true);
        if (Build.VERSION.SDK_INT >= 28) {
            title.setAccessibilityHeading(true);
            sectionTitle.setAccessibilityHeading(true);
        }
        addWithTopMargin(page, sectionTitle, 25);

        rows = column();
        addWithTopMargin(page, rows, 8);
        TextView note = label("点名称直接打开。点「桌面」可固定快捷方式。", 14, MUTED, false);
        addWithTopMargin(page, note, 14);
        render();
    }

    private void render() {
        List<Entry> entries = store.read();
        sectionTitle.setText("快捷入口 · " + entries.size());
        rows.removeAllViews();
        if (entries.isEmpty()) {
            TextView empty = label("添加一个链接，就可以在这里快速打开。", 15, MUTED, false);
            empty.setPadding(dp(12), dp(12), dp(12), dp(12));
            rows.addView(empty);
        }
        for (Entry entry : entries) {
            addWithTopMargin(rows, card(entry), 8);
        }
    }

    private LinearLayout card(Entry entry) {
        LinearLayout card = column();
        card.setPadding(dp(12), dp(8), dp(12), dp(6));
        GradientDrawable shape = rounded(WHITE, 19);
        shape.setStroke(dp(1), 0xFFEADFD6);
        card.setBackground(shape);

        LinearLayout launch = new LinearLayout(this);
        launch.setGravity(Gravity.CENTER_VERTICAL);
        launch.setMinimumHeight(dp(58));
        launch.setPadding(dp(3), dp(3), dp(3), dp(3));
        launch.setBackground(touchBackground(WHITE, 12));
        launch.setContentDescription("打开 " + entry.name);
        launch.setFocusable(true);
        launch.setOnClickListener(view -> Entries.open(this, entry.url));

        TextView marker = label("▶", 17, ACCENT, true);
        marker.setGravity(Gravity.CENTER);
        marker.setBackground(rounded(PALE, 13));
        marker.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        launch.addView(marker, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout text = column();
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart(dp(11));
        launch.addView(text, textParams);
        text.addView(label(entry.name, 20, INK, true));
        text.addView(label(Entries.destinationLabel(entry.url), 13, MUTED, false));
        text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        card.addView(launch);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.END);
        Button manage = action("管理", false);
        manage.setContentDescription("管理 " + entry.name);
        manage.setOnClickListener(view -> manage(entry));
        controls.addView(manage);
        Button pin = action("桌面", false);
        pin.setContentDescription("将 " + entry.name + " 添加到桌面");
        pin.setOnClickListener(view -> pin(entry));
        controls.addView(pin);
        card.addView(controls);
        return card;
    }

    private void manage(Entry entry) {
        new AlertDialog.Builder(this).setTitle(entry.name)
            .setItems(new String[]{"编辑", "删除"}, (dialog, index) -> {
                if (index == 0) {
                    edit(entry);
                } else {
                    confirmDelete(entry);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void edit(Entry existing) {
        LinearLayout fields = column();
        fields.setPadding(dp(22), dp(8), dp(22), 0);
        EditText name = field("名称", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(48)});
        fields.addView(name);
        EditText url = field("链接，例如 https:// 或 bilibili://", InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_VARIATION_URI);
        url.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4096)});
        addWithTopMargin(fields, url, 10);
        if (existing != null) {
            name.setText(existing.name);
            url.setText(existing.url);
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(existing == null ? "添加链接" : "编辑入口")
            .setView(fields)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String title = name.getText().toString().trim();
            String link = url.getText().toString().trim();
            if (title.isEmpty()) {
                name.setError("请输入名称");
                name.requestFocus();
                return;
            }
            if (Entries.parse(link) == null) {
                url.setError("请输入完整的网页链接或应用链接");
                url.requestFocus();
                return;
            }
            List<Entry> entries = store.read();
            Entry updated = new Entry(existing == null ? UUID.randomUUID().toString() : existing.id,
                title, link);
            if (existing == null) {
                entries.add(updated);
            } else {
                for (int index = 0; index < entries.size(); index++) {
                    if (entries.get(index).id.equals(updated.id)) {
                        entries.set(index, updated);
                        break;
                    }
                }
            }
            store.save(entries);
            LaunchWidget.refresh(this, entries);
            if (existing != null) {
                updatePinnedShortcut(updated);
            }
            dialog.dismiss();
            render();
        });
    }

    private void confirmDelete(Entry entry) {
        new AlertDialog.Builder(this).setTitle("删除「" + entry.name + "」？")
            .setMessage("如果已添加到桌面，该图标将无法再打开。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除", (dialog, which) -> {
                List<Entry> entries = store.read();
                entries.removeIf(item -> item.id.equals(entry.id));
                store.save(entries);
                LaunchWidget.refresh(this, entries);
                getSystemService(ShortcutManager.class).disableShortcuts(
                    Collections.singletonList(entry.id), "这条快捷入口已删除。");
                render();
            }).show();
    }

    private void pin(Entry entry) {
        ShortcutManager shortcuts = getSystemService(ShortcutManager.class);
        if (!shortcuts.isRequestPinShortcutSupported()
                || !shortcuts.requestPinShortcut(shortcut(entry), null)) {
            Toast.makeText(this, "当前桌面无法固定快捷方式。", Toast.LENGTH_LONG).show();
        }
    }

    private void pinWidget() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (!manager.isRequestPinAppWidgetSupported()
                || !manager.requestPinAppWidget(LaunchWidget.component(this), null, null)) {
            Toast.makeText(this, "请长按桌面空白处，从小组件列表添加 Quick Launch。",
                Toast.LENGTH_LONG).show();
        }
    }

    private void updatePinnedShortcut(Entry entry) {
        ShortcutManager shortcuts = getSystemService(ShortcutManager.class);
        for (ShortcutInfo pinned : shortcuts.getPinnedShortcuts()) {
            if (pinned.getId().equals(entry.id)) {
                shortcuts.updateShortcuts(Collections.singletonList(shortcut(entry)));
                return;
            }
        }
    }

    private ShortcutInfo shortcut(Entry entry) {
        return new ShortcutInfo.Builder(this, entry.id)
            .setShortLabel(entry.name)
            .setIcon(Icon.createWithResource(this, R.drawable.ic_launch))
            .setIntent(ShortcutActivity.intent(this, entry.id))
            .build();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private EditText field(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(inputType);
        field.setSingleLine();
        return field;
    }

    private Button action(String text, boolean primary) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(15);
        button.setTextColor(primary ? WHITE : ACCENT);
        button.setMinimumHeight(dp(48));
        button.setMinHeight(dp(48));
        button.setMinimumWidth(dp(60));
        button.setMinWidth(dp(60));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setStateListAnimator(null);
        button.setBackground(touchBackground(primary ? ACCENT : WHITE, 14));
        return button;
    }

    private RippleDrawable touchBackground(int color, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x246D4235),
            rounded(color, radius), rounded(0xFFFFFFFF, radius));
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private void addWithTopMargin(LinearLayout parent, View child, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
        parent.addView(child, params);
    }

    private int dp(int amount) {
        return Math.round(amount * getResources().getDisplayMetrics().density);
    }
}
