package com.varunrajesh.clipsync;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import static com.varunrajesh.clipsync.Constants.DIRECTION;
import static com.varunrajesh.clipsync.Constants.IMAGE_SHARE;
import static com.varunrajesh.clipsync.Constants.TEXT_SHARE;

public class MainActivity extends Activity {

    private ClipboardManager clipboardManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startNotificationPopup();
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        AsyncHandler handler = new AsyncHandler(this);
        if (getIntent().getAction() != null && getIntent().getType() != null) {
            if (getIntent().getType().equals(TEXT_SHARE)) {
                handler.execute(TEXT_SHARE, getIntent().getStringExtra(Intent.EXTRA_TEXT));
            } else if (getIntent().getType().startsWith(IMAGE_SHARE)) {
                handler.execute(IMAGE_SHARE, getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
            } else {
                finish();
            }
        } else if (getIntent().getStringExtra(DIRECTION) != null) {
            handler.execute(getIntent().getStringExtra(DIRECTION));
        } else {
            finish();
        }
    }

    private void startNotificationPopup() {
        startService(new Intent(this, DownloadPopup.class));
        startService(new Intent(this, UploadPopup.class));
    }
}