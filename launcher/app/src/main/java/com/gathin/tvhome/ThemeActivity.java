package com.gathin.tvhome;

import android.app.Activity;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

/** Shared window styling. Home keeps its own background player and focus lifecycle. */
abstract class ThemeActivity extends Activity {
    private final ContentObserver changes = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) { onThemeChanged(); }
    };
    @Override protected void onStart() {
        super.onStart();
        getContentResolver().registerContentObserver(Uri.parse("content://" + getPackageName() + ".theme"), true, changes);
    }
    @Override protected void onStop() {
        getContentResolver().unregisterContentObserver(changes);
        super.onStop();
    }
    protected void onThemeChanged() { recreate(); }
    @Override public void onContentChanged() {
        super.onContentChanged();
        ThemeStyle.get(this).watch(getWindow().getDecorView());
    }
}
