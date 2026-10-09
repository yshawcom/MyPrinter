package com.yshaw.myprinter.ui;

/** Called when the user saves the settings screen. */
public interface SettingsSaveListener {
    void onSave(String apiUrl, String printer, String paperSize, String code, boolean color);
}
