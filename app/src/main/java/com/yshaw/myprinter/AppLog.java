package com.yshaw.myprinter;

import android.util.Log;

/**
 * Single entry point for the operation log, so every user action can be followed on logcat with
 * one filter:
 *
 * <pre>adb logcat -s MyPrinter</pre>
 *
 * Never log file contents or credentials – only names, sizes, configuration and results.
 */
public final class AppLog {
    public static final String TAG = "MyPrinter";

    private static final boolean ENABLED = true;

    private AppLog() {
    }

    /** Something the user did. */
    public static void user(String action) {
        user(action, null);
    }

    public static void user(String action, String detail) {
        write(Log.INFO, action, detail);
    }

    /** Supporting detail of an operation. */
    public static void event(String action) {
        event(action, null);
    }

    public static void event(String action, String detail) {
        write(Log.DEBUG, action, detail);
    }

    /** A problem that the app could recover from. */
    public static void warn(String action) {
        warn(action, null);
    }

    public static void warn(String action, String detail) {
        write(Log.WARN, action, detail);
    }

    /** A failure with a stack trace. */
    public static void failure(String action, Throwable error, String detail) {
        if (ENABLED) {
            Log.e(TAG, line(action, detail), error);
        }
    }

    private static void write(int priority, String action, String detail) {
        if (ENABLED) {
            Log.println(priority, TAG, line(action, detail));
        }
    }

    private static String line(String action, String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            return action;
        }
        return action + " | " + detail;
    }
}
