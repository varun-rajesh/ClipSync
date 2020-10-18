package com.varunrajesh.clipsync;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import static android.content.ClipDescription.MIMETYPE_TEXT_HTML;
import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;
import static com.varunrajesh.clipsync.Constants.CLIP_DOWNLOAD;
import static com.varunrajesh.clipsync.Constants.CLIP_UPLOAD;
import static com.varunrajesh.clipsync.Constants.DATABASE_LAST_USED_CHILD;
import static com.varunrajesh.clipsync.Constants.DATABASE_LAST_USED_IMAGE;
import static com.varunrajesh.clipsync.Constants.DATABASE_LAST_USED_TEXT;
import static com.varunrajesh.clipsync.Constants.DATABASE_TEXT_CHILD;
import static com.varunrajesh.clipsync.Constants.ERROR;
import static com.varunrajesh.clipsync.Constants.IMAGE_SHARE;
import static com.varunrajesh.clipsync.Constants.STORAGE_IMAGE_CHILD;
import static com.varunrajesh.clipsync.Constants.TEXT_SHARE;
import static java.lang.Thread.sleep;

public class AsyncHandler extends AsyncTask {

    private WeakReference<Activity> activityWeakReference;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReferenceText;
    private DatabaseReference databaseReferenceLastUsed;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReferenceImage;

    private ClipboardManager clipboardManager;

    private boolean requestedUpdate;

    private String returnMessage = "";

    public AsyncHandler(Activity activity) {
        activityWeakReference = new WeakReference<>(activity);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReferenceText = firebaseDatabase.getReference().child(DATABASE_TEXT_CHILD);
        databaseReferenceLastUsed = firebaseDatabase.getReference().child(DATABASE_LAST_USED_CHILD);

        firebaseStorage = FirebaseStorage.getInstance();
        storageReferenceImage = firebaseStorage.getReference().child(STORAGE_IMAGE_CHILD);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        requestedUpdate = true;
        returnMessage = "";
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        Activity activity = activityWeakReference.get();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        Toast.makeText(activity, (String) o, Toast.LENGTH_SHORT).show();
        activity.finish();
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        Activity activity = activityWeakReference.get();
        if (activity == null || activity.isFinishing()) {
            return ERROR;
        }

        switch ((String) objects[0]) {
            case TEXT_SHARE:
                databaseReferenceLastUsed.setValue(DATABASE_LAST_USED_TEXT);
                databaseReferenceText.setValue(objects[1]);
                return "Uploaded: " + objects[1];

            case IMAGE_SHARE:
                Uri imageUri = (Uri) objects[1];
                try {
                    InputStream inputStream = activity.getContentResolver().openInputStream(imageUri);
                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];

                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteBuffer.write(buffer, 0, len);
                    }

                    storageReferenceImage.putBytes(byteBuffer.toByteArray())
                            .addOnFailureListener(exception -> {})
                            .addOnSuccessListener(takeSnapshot -> {});
                    databaseReferenceLastUsed.setValue(DATABASE_LAST_USED_IMAGE);

                    return "Uploaded Image";

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return ERROR;
                } catch (IOException e) {
                    e.printStackTrace();
                    return ERROR;
                }

            case CLIP_UPLOAD:
                clipboardManager = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);

                while (clipboardManager.getPrimaryClipDescription() == null) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return ERROR;
                    }
                }

                if (clipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)
                        || clipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_HTML)) {
                    ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
                    databaseReferenceLastUsed.setValue(DATABASE_LAST_USED_TEXT);
                    databaseReferenceText.setValue(item.getText().toString());

                    return "Uploaded: " + item.getText().toString();
                } else {
                    return ERROR;
                }

            case CLIP_DOWNLOAD:

                clipboardManager = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);

                while (clipboardManager.getPrimaryClipDescription() == null) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return ERROR;
                    }
                }

                databaseReferenceLastUsed.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String type = snapshot.getValue().toString();
                        type = type == null ? "" : type;

                        if (type.equals(DATABASE_LAST_USED_TEXT)) {
                            databaseReferenceText.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (requestedUpdate) {
                                        ClipData clip = ClipData.newPlainText(DATABASE_LAST_USED_TEXT,
                                                snapshot.getValue().toString());
                                        clipboardManager.setPrimaryClip(clip);
                                        requestedUpdate = false;
                                        returnMessage = "Copied: " + clip.getItemAt(0).getText();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    returnMessage = ERROR;
                                    return;
                                }
                            });
                        }
                        else if (type.equals(DATABASE_LAST_USED_IMAGE)) {
                            final long MAX_FILE_SIZE = 1024 * 1024 * 100;
                            if (requestedUpdate) {
                                storageReferenceImage.getBytes(MAX_FILE_SIZE)
                                        .addOnSuccessListener(bytes -> {
                                            try {
                                                File tempFile = File.createTempFile("Image",
                                                        ".jpg", activity.getExternalFilesDir(Environment.DIRECTORY_DCIM));
                                                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));
                                                bos.write(bytes);
                                                bos.flush();

                                                MediaStore.Images.Media.insertImage(activity.getContentResolver(),
                                                        tempFile.getAbsolutePath(), tempFile.getName(), null);
                                                activity.sendBroadcast(
                                                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(tempFile)));
                                                returnMessage = "Downloaded Image";
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                returnMessage = ERROR;
                                            }
                                        }).addOnFailureListener(exception -> {});
                                requestedUpdate = false;
                            }
                        }
                        return;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        returnMessage = ERROR;
                        return;
                    }
                });

                while(returnMessage.equals("")) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                return returnMessage;

            default:
                return ERROR;
        }

    }

}
