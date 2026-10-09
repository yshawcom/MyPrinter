package com.yshaw.myprinter;

/** Immutable snapshot of the printer connection settings. */
public final class PrinterConfig {
    private final String apiUrl;
    private final String printer;
    private final String paperSize;
    private final String code;
    private final boolean color;

    public PrinterConfig(String apiUrl, String printer, String paperSize, String code, boolean color) {
        this.apiUrl = apiUrl == null ? "" : apiUrl;
        this.printer = printer == null ? "" : printer;
        this.paperSize = paperSize == null ? "" : paperSize;
        this.code = code == null ? "" : code;
        this.color = color;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getPrinter() {
        return printer;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public String getCode() {
        return code;
    }

    public boolean getColor() {
        return color;
    }

    public boolean isComplete() {
        return !apiUrl.trim().isEmpty() && !printer.trim().isEmpty() && !paperSize.trim().isEmpty();
    }
}
