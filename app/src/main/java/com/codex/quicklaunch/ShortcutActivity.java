package com.codex.quicklaunch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public final class ShortcutActivity extends Activity {
    static Intent intent(Context context, String entryId) {
        return new Intent(context, ShortcutActivity.class)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(Entries.EXTRA_ID, entryId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Entries.Entry entry = new Entries(this).find(getIntent().getStringExtra(Entries.EXTRA_ID));
        if (entry == null) {
            Toast.makeText(this, "这条快捷入口已删除。", Toast.LENGTH_SHORT).show();
        } else {
            Entries.open(this, entry.url);
        }
        finish();
    }
}
