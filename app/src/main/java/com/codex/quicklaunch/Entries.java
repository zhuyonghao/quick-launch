package com.codex.quicklaunch;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Entries {
    static final String FAVORITES = "bilibili://user_center/favourite";
    private static final String PLAYLIST_PREFIX = "bilibili://music/playlist/playpage/";
    static final String EXTRA_ID = "com.codex.quicklaunch.ENTRY_ID";
    private static final Pattern SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*");
    private static final Pattern FAVORITES_PATH = Pattern.compile("/[0-9]+/favlist/?");
    private static final Pattern FOLDER_ID = Pattern.compile("[0-9]+");

    static final class Entry {
        final String id;
        final String name;
        final String url;

        Entry(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }

    private final SharedPreferences preferences;

    Entries(Context context) {
        preferences = context.getSharedPreferences("shortcuts", Context.MODE_PRIVATE);
    }

    List<Entry> read() {
        List<Entry> entries = new ArrayList<>();
        String stored = preferences.getString("items", null);
        if (stored == null) {
            entries.add(new Entry("favorites", "我的收藏", FAVORITES));
            return entries;
        }
        try {
            JSONArray json = new JSONArray(stored);
            for (int index = 0; index < json.length(); index++) {
                JSONObject item = json.getJSONObject(index);
                entries.add(new Entry(item.getString("id"), item.getString("name"),
                    item.getString("url")));
            }
        } catch (JSONException error) {
            android.util.Log.e("QuickLaunch", "Could not read saved shortcuts", error);
        }
        return entries;
    }

    Entry find(String id) {
        for (Entry entry : read()) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }

    void save(List<Entry> entries) {
        JSONArray json = new JSONArray();
        try {
            for (Entry entry : entries) {
                JSONObject item = new JSONObject();
                item.put("id", entry.id);
                item.put("name", entry.name);
                item.put("url", entry.url);
                json.put(item);
            }
        } catch (JSONException error) {
            throw new IllegalStateException(error);
        }
        preferences.edit().putString("items", json.toString()).apply();
    }

    static Uri parse(String url) {
        for (int index = 0; index < url.length(); index++) {
            char character = url.charAt(index);
            if (Character.isWhitespace(character) || Character.isSpaceChar(character)
                    || Character.isISOControl(character)) {
                return null;
            }
        }
        Uri uri = Uri.parse(url).normalizeScheme();
        String scheme = uri.getScheme();
        if (scheme == null || !SCHEME.matcher(scheme).matches()
                || uri.getSchemeSpecificPart().isEmpty()) {
            return null;
        }
        switch (scheme) {
            case "http":
            case "https":
                return uri.getHost() == null || uri.getHost().isEmpty() ? null : uri;
            case "file":
            case "content":
            case "android.resource":
            case "javascript":
            case "data":
            case "intent":
                return null;
            default:
                return uri;
        }
    }

    private static Uri launchUri(String url) {
        Uri uri = parse(url);
        if (uri == null || !"https".equals(uri.getScheme())
                || !"space.bilibili.com".equalsIgnoreCase(uri.getEncodedAuthority())) {
            return uri;
        }
        String path = uri.getEncodedPath();
        if (path == null || !FAVORITES_PATH.matcher(path).matches()) {
            return uri;
        }
        String folderId = uri.getQueryParameter("fid");
        String folderType = uri.getQueryParameter("ftype");
        if (folderId == null || !FOLDER_ID.matcher(folderId).matches()
                || (folderType != null && !"create".equals(folderType))) {
            return uri;
        }
        return Uri.parse(PLAYLIST_PREFIX + folderId);
    }

    static String destinationLabel(String url) {
        Uri destination = launchUri(url);
        if (destination == null) {
            return "";
        }
        if (destination.toString().startsWith(PLAYLIST_PREFIX)) {
            return "哔哩哔哩 · 收藏夹播放";
        }
        if (FAVORITES.equals(destination.toString())) {
            return "哔哩哔哩 · 收藏总览";
        }
        return destination.getScheme();
    }

    static void open(Context context, String url) {
        Uri uri = launchUri(url);
        if (uri == null) {
            Toast.makeText(context, "这个链接格式无法打开。", Toast.LENGTH_LONG).show();
            return;
        }
        Intent view = new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(view);
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(context, "手机上没有应用能打开这个链接。请检查目标应用是否已安装。",
                Toast.LENGTH_LONG).show();
        }
    }
}
