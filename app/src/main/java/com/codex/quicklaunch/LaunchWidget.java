package com.codex.quicklaunch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import com.codex.quicklaunch.Entries.Entry;
import java.util.List;

public final class LaunchWidget extends AppWidgetProvider {
    private static final int[] ROWS = {R.id.widget_first, R.id.widget_second, R.id.widget_third};

    static ComponentName component(Context context) {
        return new ComponentName(context, LaunchWidget.class);
    }

    static void refresh(Context context, List<Entry> entries) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        update(context, manager, manager.getAppWidgetIds(component(context)), entries);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        update(context, manager, widgetIds, new Entries(context).read());
    }

    private static void update(Context context, AppWidgetManager manager, int[] widgetIds,
            List<Entry> entries) {
        if (widgetIds.length == 0) {
            return;
        }
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.launch_widget);
        Intent openApp = new Intent(context, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent appClick = PendingIntent.getActivity(context, 0, openApp,
            PendingIntent.FLAG_IMMUTABLE);
        widget.setOnClickPendingIntent(R.id.widget_title, appClick);
        widget.setOnClickPendingIntent(R.id.widget_empty, appClick);
        widget.setViewVisibility(R.id.widget_empty, entries.isEmpty() ? View.VISIBLE : View.GONE);

        for (int index = 0; index < ROWS.length; index++) {
            int row = ROWS[index];
            if (index >= entries.size()) {
                widget.setViewVisibility(row, View.GONE);
                continue;
            }
            Entry entry = entries.get(index);
            widget.setViewVisibility(row, View.VISIBLE);
            widget.setTextViewText(row, entry.name);
            Intent launch = ShortcutActivity.intent(context, entry.id)
                .setData(Uri.fromParts("quicklaunch", entry.id, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            widget.setOnClickPendingIntent(row, PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_IMMUTABLE));
        }
        manager.updateAppWidget(widgetIds, widget);
    }
}
