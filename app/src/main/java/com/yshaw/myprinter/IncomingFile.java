package com.yshaw.myprinter;

import android.net.Uri;

/** A file handed over to MyPrinter by another app. */
public final class IncomingFile {
    private final Uri uri;
    private final String name;
    private final boolean image;

    public IncomingFile(Uri uri, String name, boolean image) {
        this.uri = uri;
        this.name = name;
        this.image = image;
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public boolean isImage() {
        return image;
    }
}
