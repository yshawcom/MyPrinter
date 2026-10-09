package com.yshaw.myprinter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * A deliberately small provider used only to hand a private cache file to the system camera.
 * It lets the camera produce a full-resolution image without asking for storage permission.
 */
public final class CameraFileProvider extends ContentProvider {
    private File cameraDirectory;

    @Override
    public boolean onCreate() {
        if (getContext() == null) {
            return false;
        }
        cameraDirectory = new File(getContext().getCacheDir(), "camera");
        return cameraDirectory.exists() || cameraDirectory.mkdirs();
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode) && !"rw".equals(mode) && !"w".equals(mode)) {
            throw new FileNotFoundException("Unsupported access mode");
        }
        File image = resolveCameraFile(uri);
        int flags = "r".equals(mode)
                ? ParcelFileDescriptor.MODE_READ_ONLY
                : ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE;
        return ParcelFileDescriptor.open(image, flags);
    }

    private File resolveCameraFile(Uri uri) throws FileNotFoundException {
        List<String> segments = uri.getPathSegments();
        if (segments.size() != 2 || !"camera".equals(segments.get(0))) {
            throw new FileNotFoundException("Unknown camera file");
        }
        String name = segments.get(1);
        if (!name.endsWith(".jpg") || !name.equals(new File(name).getName())) {
            throw new FileNotFoundException("Invalid camera file name");
        }
        try {
            File root = cameraDirectory.getCanonicalFile();
            File image = new File(root, name).getCanonicalFile();
            if (!image.getPath().startsWith(root.getPath() + File.separator)) {
                throw new FileNotFoundException("Camera file is outside of cache");
            }
            return image;
        } catch (IOException exception) {
            throw new FileNotFoundException(exception.getMessage());
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
