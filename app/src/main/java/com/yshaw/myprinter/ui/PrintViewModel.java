package com.yshaw.myprinter.ui;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.compose.runtime.MutableState;
import androidx.lifecycle.AndroidViewModel;

import com.yshaw.myprinter.AppLog;
import com.yshaw.myprinter.ConfigStore;
import com.yshaw.myprinter.FileIntake;
import com.yshaw.myprinter.IncomingFile;
import com.yshaw.myprinter.PrinterConfig;
import com.yshaw.myprinter.PrintApi;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Holds the print screen state and performs the OkHttp upload. */
public final class PrintViewModel extends AndroidViewModel {

    public final MutableState<PrinterConfig> config;
    public final MutableState<PrintSelection> selection;
    public final MutableState<String> copiesText;
    /** Landscape printing; sent to the print service as layout=1/0. */
    public final MutableState<Boolean> layout;
    /** Duplex printing; sent to the print service as duplex=1/0. */
    public final MutableState<Boolean> duplex;
    /** Page range such as "1-5,8,10-12"; empty means every page. */
    public final MutableState<String> pageRange;
    public final MutableState<Boolean> printing;
    public final MutableState<PrintDialog> dialog;
    /** True while the selected file is the one handed over by another app. */
    public final MutableState<Boolean> openedFromAnotherApp;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PrintViewModel(Application application) {
        super(application);
        config = ComposeBridgeKt.composeState(ConfigStore.load(application));
        selection = ComposeBridgeKt.composeState((PrintSelection) null);
        copiesText = ComposeBridgeKt.composeState("1");
        layout = ComposeBridgeKt.composeState(false);
        duplex = ComposeBridgeKt.composeState(false);
        pageRange = ComposeBridgeKt.composeState("");
        printing = ComposeBridgeKt.composeState(false);
        dialog = ComposeBridgeKt.composeState((PrintDialog) null);
        openedFromAnotherApp = ComposeBridgeKt.composeState(false);
    }

    public void refreshConfig() {
        config.setValue(ConfigStore.load(getApplication()));
    }

    public void selectFile(Uri uri, String name, boolean isImage) {
        selection.setValue(new PrintSelection(uri, name, isImage));
        // A file picked inside the app is no longer "from another app".
        openedFromAnotherApp.setValue(false);
        dialog.setValue(null);
        AppLog.user("选择文件完成",
                "文件名=" + name + ", 类型=" + (isImage ? "图片" : "文档") + ", uri=" + uri);
    }

    /**
     * Entry point for files coming from WeChat, the share sheet or "open with".
     * The file is only staged; the user decides when to send it to the printer.
     */
    public void openIncomingFile(IncomingFile file) {
        openedFromAnotherApp.setValue(true);
        selection.setValue(new PrintSelection(file.getUri(), file.getName(), file.isImage()));
        dialog.setValue(null);
        AppLog.user("外部文件已载入，等待点击打印",
                "文件名=" + file.getName() + ", 类型=" + (file.isImage() ? "图片" : "文档"));
    }

    public void updateCopies(String value) {
        String sanitized = digitsOnly(value, 3);
        if (!sanitized.equals(copiesText.getValue())) {
            AppLog.event("修改打印份数", "份数=" + (sanitized.isEmpty() ? "1" : sanitized));
            copiesText.setValue(sanitized);
        }
    }

    public void updateLayout(boolean value) {
        layout.setValue(value);
        AppLog.user("修改横版打印", "横版=" + (value ? 1 : 0));
    }

    public void updateDuplex(boolean value) {
        duplex.setValue(value);
        AppLog.user("修改双面打印", "双面=" + (value ? 1 : 0));
    }

    public void updatePageRange(String value) {
        pageRange.setValue(value == null ? "" : value);
        AppLog.user("修改指定页码",
                "页码=" + (value == null || value.trim().isEmpty() ? "全部" : value));
    }

    public void dismissDialog() {
        dialog.setValue(null);
    }

    public long copies() {
        try {
            return Math.max(1L, Long.parseLong(copiesText.getValue()));
        } catch (NumberFormatException error) {
            return 1L;
        }
    }

    public void printNow() {
        PrintSelection selected = selection.getValue();
        if (selected == null) {
            AppLog.warn("点击打印按钮，但尚未选择文件");
            return;
        }
        Application application = getApplication();
        PrinterConfig currentConfig = ConfigStore.load(application);
        config.setValue(currentConfig);
        if (!currentConfig.isComplete()) {
            AppLog.warn("打印被拦截：打印配置不完整",
                    "API=" + currentConfig.getApiUrl()
                            + ", 打印机=" + currentConfig.getPrinter()
                            + ", 纸张=" + currentConfig.getPaperSize());
            dialog.setValue(PrintDialog.needConfig(
                    "请先完成打印设置", "打印机 API、打印机名称和纸张大小均不能为空。"));
            return;
        }

        long copies = copies();
        copiesText.setValue(Long.toString(copies));
        // Duplex and page ranges only apply to documents; images always print single sided.
        boolean imageFile = selected.isImage();
        String requestPageRange = imageFile ? "" : pageRange.getValue();
        boolean requestDuplex = !imageFile && Boolean.TRUE.equals(duplex.getValue());
        boolean requestLayout = Boolean.TRUE.equals(layout.getValue());
        boolean fromAnotherApp = Boolean.TRUE.equals(openedFromAnotherApp.getValue());
        long startedAt = System.currentTimeMillis();
        AppLog.user("点击打印按钮，开始打印",
                "文件名=" + selected.getName()
                        + ", 份数=" + copies
                        + ", 打印机=" + currentConfig.getPrinter()
                        + ", 纸张=" + currentConfig.getPaperSize()
                        + ", 页码=" + (requestPageRange == null || requestPageRange.trim().isEmpty()
                        ? "全部" : requestPageRange)
                        + ", 双面=" + (requestDuplex ? 1 : 0)
                        + ", 横版=" + (requestLayout ? 1 : 0)
                        + ", 彩色=" + (currentConfig.getColor() ? 1 : 0)
                        + ", 来源=" + (fromAnotherApp ? "其他应用" : "应用内"));

        printing.setValue(true);
        executor.execute(() -> {
            try {
                byte[] content = FileIntake.readAllBytes(application, selected.getUri());
                if (content.length == 0) {
                    throw new IOException("所选文件为空");
                }
                PrintApi.PrintReply reply = PrintApi.print(
                        currentConfig,
                        FileIntake.uploadFileName(application, selected.getUri(), selected.getName()),
                        content,
                        copies,
                        requestPageRange,
                        requestDuplex,
                        requestLayout);
                long elapsed = System.currentTimeMillis() - startedAt;
                mainHandler.post(() -> {
                    printing.setValue(false);
                    if (reply.isSuccess()) {
                        AppLog.user("打印请求成功",
                                "耗时=" + elapsed + "ms, 服务端消息=" + reply.getMessage());
                        dialog.setValue(PrintDialog.info("打印请求已发送", reply.getMessage()));
                    } else {
                        AppLog.warn("打印请求被服务端拒绝",
                                "耗时=" + elapsed + "ms, 服务端消息=" + reply.getMessage());
                        dialog.setValue(PrintDialog.info("打印失败", reply.getMessage()));
                    }
                });
            } catch (Exception error) {
                AppLog.failure("打印请求异常", error,
                        "文件名=" + selected.getName()
                                + ", 耗时=" + (System.currentTimeMillis() - startedAt) + "ms");
                String message = error.getMessage();
                String text = message == null || message.trim().isEmpty()
                        ? "无法连接到打印服务，请检查配置和网络。"
                        : message;
                mainHandler.post(() -> {
                    printing.setValue(false);
                    dialog.setValue(PrintDialog.info("打印请求失败", text));
                });
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
        super.onCleared();
    }

    private static String digitsOnly(String value, int limit) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length() && builder.length() < limit; index++) {
            char character = value.charAt(index);
            if (Character.isDigit(character)) {
                builder.append(character);
            }
        }
        return builder.toString();
    }
}
