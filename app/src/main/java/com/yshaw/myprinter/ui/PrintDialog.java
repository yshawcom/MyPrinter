package com.yshaw.myprinter.ui;

/** Dialog the print screen may show; keeps the strings out of the composable. */
public final class PrintDialog {
    public enum Kind {
        /** Plain information or a result message. */
        INFO,
        /** The print configuration is incomplete, offer to open the settings. */
        NEED_CONFIG,
    }

    private final Kind kind;
    private final String title;
    private final String message;

    private PrintDialog(Kind kind, String title, String message) {
        this.kind = kind;
        this.title = title;
        this.message = message;
    }

    public static PrintDialog info(String title, String message) {
        return new PrintDialog(Kind.INFO, title, message);
    }

    public static PrintDialog needConfig(String title, String message) {
        return new PrintDialog(Kind.NEED_CONFIG, title, message);
    }

    public Kind getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
