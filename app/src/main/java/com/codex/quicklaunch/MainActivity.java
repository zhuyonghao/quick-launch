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
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.codex.quicklaunch.Entries.Entry;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int PAPER = 0xFFF4F6FC;
    private static final int INK = 0xFF17213B;
    private static final int MUTED = 0xFF535D75;
    private static final int ACCENT = 0xFF5146D9;
    private static final int PALE = 0xFFEAE9FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BORDER = 0xFFE0E5F2;
    private Entries store;
    private LinearLayout rows;
    private TextView entryCount;

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
        page.setPadding(dp(18), dp(12), dp(18), dp(26));
        scroll.addView(page);
        setContentView(scroll);

        page.addView(hero());

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
        addWithTopMargin(page, actions, 14);

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        TextView sectionTitle = label("快捷入口", 23, INK, true);
        section.addView(sectionTitle, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        entryCount = label("", 12, ACCENT, true);
        entryCount.setPadding(dp(10), dp(4), dp(10), dp(4));
        entryCount.setBackground(rounded(PALE, 18));
        section.addView(entryCount);
        if (Build.VERSION.SDK_INT >= 28) {
            sectionTitle.setAccessibilityHeading(true);
        }
        addWithTopMargin(page, section, 24);

        rows = column();
        addWithTopMargin(page, rows, 6);
        TextView note = label("点入口打开，点图钉固定到桌面。", 13, MUTED, false);
        addWithTopMargin(page, note, 14);
        render();
    }

    private LinearLayout hero() {
        LinearLayout hero = column();
        hero.setPadding(dp(20), dp(16), dp(20), dp(19));
        hero.setBackgroundResource(R.drawable.hero_surface);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_launch);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        brand.addView(mark, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView eyebrow = label("安卓快捷启动器", 12, 0xFFE4E3FF, true);
        LinearLayout.LayoutParams eyebrowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        eyebrowParams.setMarginStart(dp(8));
        brand.addView(eyebrow, eyebrowParams);
        hero.addView(brand);

        TextView title = label(getString(R.string.app_name), 37, WHITE, true);
        title.setLetterSpacing(-0.025f);
        if (Build.VERSION.SDK_INT >= 28) {
            title.setAccessibilityHeading(true);
        }
        addWithTopMargin(hero, title, 7);
        addWithTopMargin(hero, label("常用页面，一点即达", 15, 0xFFE4E3FF, false), 2);
        return hero;
    }

    private void render() {
        List<Entry> entries = store.read();
        entryCount.setText(entries.size() + " 个");
        rows.removeAllViews();
        if (entries.isEmpty()) {
            TextView empty = label("还没有快捷入口。添加一个常用链接试试。", 15, MUTED, false);
            empty.setPadding(dp(16), dp(18), dp(16), dp(18));
            empty.setBackground(rounded(WHITE, 18));
            rows.addView(empty);
        }
        for (Entry entry : entries) {
            addWithTopMargin(rows, card(entry), 8);
        }
    }

    private LinearLayout card(Entry entry) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(5), dp(4), dp(5), dp(4));
        GradientDrawable shape = rounded(WHITE, 18);
        shape.setStroke(dp(1), BORDER);
        card.setBackground(shape);

        LinearLayout launch = new LinearLayout(this);
        launch.setGravity(Gravity.CENTER_VERTICAL);
        launch.setMinimumHeight(dp(66));
        launch.setPadding(dp(7), dp(6), dp(3), dp(6));
        launch.setBackground(touchBackground(0x00FFFFFF, 14));
        launch.setContentDescription("打开 " + entry.name);
        launch.setFocusable(true);
        launch.setOnClickListener(view -> Entries.open(this, entry.url));

        String destination = Entries.destinationLabel(entry.url);
        ImageView marker = new ImageView(this);
        marker.setImageResource(R.drawable.ic_open);
        marker.setPadding(dp(10), dp(10), dp(10), dp(10));
        marker.setColorFilter(ACCENT);
        marker.setBackground(rounded(PALE, 12));
        if (destination.startsWith("哔哩哔哩")) {
            marker.setImageResource(R.drawable.ic_play);
            marker.setColorFilter(0xFF176182);
            marker.setBackground(rounded(0xFFE1F4FF, 12));
        } else if (entry.url.startsWith("amapuri:") || entry.url.startsWith("androidamap:")) {
            marker.setImageResource(R.drawable.ic_route);
            marker.setColorFilter(0xFF11685D);
            marker.setBackground(rounded(0xFFDCF4EC, 12));
        }
        marker.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        launch.addView(marker, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout text = column();
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart(dp(11));
        launch.addView(text, textParams);
        TextView name = label(entry.name, 17, INK, true);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(name);
        addWithTopMargin(text, label(destination, 12, MUTED, false), 2);
        text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        card.addView(launch, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(iconAction(R.drawable.ic_pin, "将 " + entry.name + " 添加到桌面",
            view -> pin(entry)), new LinearLayout.LayoutParams(dp(48), dp(48)));
        card.addView(iconAction(R.drawable.ic_more, "管理 " + entry.name,
            view -> manage(entry)), new LinearLayout.LayoutParams(dp(48), dp(48)));
        return card;
    }

    private void manage(Entry entry) {
        styleDialog(new AlertDialog.Builder(this).setTitle(entry.name)
            .setItems(new String[]{"编辑", "删除"}, (dialog, index) -> {
                if (index == 0) {
                    edit(entry);
                } else {
                    confirmDelete(entry);
                }
            })
            .setNegativeButton("取消", null)
            .show());
    }

    private void edit(Entry existing) {
        LinearLayout fields = column();
        fields.setPadding(dp(22), dp(4), dp(22), dp(5));
        EditText name = field("名称", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(48)});
        TextView nameLabel = label("入口名称", 13, INK, true);
        name.setId(View.generateViewId());
        nameLabel.setLabelFor(name.getId());
        fields.addView(nameLabel);
        addWithTopMargin(fields, name, 6);
        EditText url = field("链接，例如 https:// 或 bilibili://", InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_VARIATION_URI);
        url.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4096)});
        TextView urlLabel = label("目标链接", 13, INK, true);
        url.setId(View.generateViewId());
        urlLabel.setLabelFor(url.getId());
        addWithTopMargin(fields, urlLabel, 14);
        addWithTopMargin(fields, url, 6);
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
        styleDialog(dialog);
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
        styleDialog(new AlertDialog.Builder(this).setTitle("删除「" + entry.name + "」？")
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
            }).show());
    }

    private void styleDialog(AlertDialog dialog) {
        dialog.getWindow().setBackgroundDrawable(new InsetDrawable(rounded(WHITE, 24), dp(8)));
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
        field.setTextSize(15);
        field.setTextColor(INK);
        field.setHintTextColor(MUTED);
        field.setMinimumHeight(dp(48));
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        StateListDrawable background = new StateListDrawable();
        GradientDrawable focused = rounded(PAPER, 12);
        focused.setStroke(dp(2), ACCENT);
        GradientDrawable resting = rounded(PAPER, 12);
        resting.setStroke(dp(1), BORDER);
        background.addState(new int[]{android.R.attr.state_focused}, focused);
        background.addState(new int[]{}, resting);
        field.setBackground(background);
        return field;
    }

    private Button action(String text, boolean primary) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(15);
        button.setTextColor(primary ? WHITE : ACCENT);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinimumHeight(dp(52));
        button.setMinHeight(dp(52));
        button.setMinimumWidth(dp(60));
        button.setMinWidth(dp(60));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setStateListAnimator(null);
        button.setBackground(touchBackground(primary ? ACCENT : PALE, 15));
        return button;
    }

    private ImageView iconAction(int resource, String description, View.OnClickListener click) {
        ImageView button = new ImageView(this);
        button.setImageResource(resource);
        button.setColorFilter(MUTED);
        button.setPadding(dp(13), dp(13), dp(13), dp(13));
        button.setBackground(touchBackground(0x00FFFFFF, 14));
        button.setContentDescription(description);
        button.setTooltipText(description);
        button.setFocusable(true);
        button.setOnClickListener(click);
        return button;
    }

    private RippleDrawable touchBackground(int color, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(color == ACCENT ? 0x32FFFFFF : 0x245146D9),
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
