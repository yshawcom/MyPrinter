package com.yshaw.myprinter.ui;

import android.net.Uri;

/** The file the user is about to print. */
public final class PrintSelection {
    private final Uri uri;
    private final String name;
    private final boolean image;

    public PrintSelection(Uri uri, String name, boolean image) {
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
