package com.yshaw.myprinter;

import android.net.Uri;

import java.io.File;

/** Cache file plus content URI the system camera can write a full-resolution photo into. */
public final class CameraTarget {
    private final File file;
    private final Uri uri;

    public CameraTarget(File file, Uri uri) {
        this.file = file;
        this.uri = uri;
    }

    public File getFile() {
        return file;
    }

    public Uri getUri() {
        return uri;
    }
}
