package com.yshaw.myprinter;

import androidx.annotation.NonNull;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Uploads a file to the OnePrinter print service.
 *
 * <p>The request is a POST to the print service API configured in the settings page, sent as
 * multipart/form-data with the fields {@code file}, {@code code}, {@code printer}, {@code copies},
 * {@code pageRange}, {@code paperSize}, {@code duplex}, {@code layout} and {@code color}. The
 * service answers {@code {"code":1,"info":"..."}} on success, and {@code info} is the text shown
 * to the user.
 */
public final class PrintApi {
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("bmp", "image/bmp");
        MIME_TYPES.put("webp", "image/webp");
        MIME_TYPES.put("doc", "application/msword");
        MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPES.put("xls", "application/vnd.ms-excel");
        MIME_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private PrintApi() {
    }

    /**
     * Uploads {@code content} as the multipart file part and asks the service to print it.
     * Blocking call – always invoke it from a background thread.
     */
    public static PrintReply print(
            PrinterConfig config,
            String fileName,
            byte[] content,
            long copies,
            String pageRange,
            boolean duplex,
            boolean layout
    ) throws IOException, JSONException {
        MediaType mediaType = MediaType.parse(mediaTypeName(fileName));
        RequestBody fileBody = mediaType == null
                ? RequestBody.create(content, null)
                : RequestBody.create(content, mediaType);
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("code", config.getCode())
                .addFormDataPart("printer", config.getPrinter())
                .addFormDataPart("copies", Long.toString(copies))
                .addFormDataPart("pageRange", pageRange == null ? "" : pageRange)
                .addFormDataPart("paperSize", config.getPaperSize())
                .addFormDataPart("duplex", flag(duplex))
                .addFormDataPart("layout", flag(layout))
                .addFormDataPart("color", flag(config.getColor()))
                .build();

        long startedAt = System.currentTimeMillis();
        AppLog.event("发送打印请求",
                "URL=" + config.getApiUrl()
                        + ", 文件名=" + fileName
                        + ", 大小=" + (content.length / 1024) + "KB"
                        + ", 份数=" + copies
                        + ", 页码=" + (pageRange == null || pageRange.trim().isEmpty() ? "全部" : pageRange)
                        + ", 打印机=" + config.getPrinter()
                        + ", 纸张=" + config.getPaperSize()
                        + ", 双面=" + flag(duplex)
                        + ", 横版=" + flag(layout)
                        + ", 彩色=" + flag(config.getColor()));

        Request request = new Request.Builder()
                .url(config.getApiUrl())
                .header("Accept", "application/json")
                .post(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            AppLog.event("收到打印服务响应",
                    "HTTP=" + response.code()
                            + ", 耗时=" + (System.currentTimeMillis() - startedAt) + "ms"
                            + ", 响应长度=" + text.length() + "字符");
            if (!response.isSuccessful()) {
                AppLog.warn("打印服务返回错误状态", "HTTP=" + response.code() + ", 响应=" + snippet(text));
                throw new IOException("打印服务返回 HTTP " + response.code() + describe(text));
            }
            return parseReply(text);
        }
    }

    private static PrintReply parseReply(String response) throws JSONException {
        if (response == null || response.trim().isEmpty()) {
            AppLog.warn("打印服务返回空响应");
            return new PrintReply(false, "打印服务未返回结果。");
        }
        JSONObject root;
        try {
            root = new JSONObject(response);
        } catch (JSONException error) {
            AppLog.failure("解析打印服务响应失败", error, "响应=" + snippet(response));
            throw error;
        }
        int code = root.optInt("code", 0);
        String message = findMessage(root);
        if (code == 1) {
            return new PrintReply(true, message.isEmpty() ? "打印服务已接收请求。" : message);
        }
        if (!message.isEmpty()) {
            AppLog.warn("打印服务返回失败代码", "code=" + code + ", 提示=" + message);
            return new PrintReply(false, message);
        }
        AppLog.warn("打印服务返回失败代码", "code=" + code + ", 响应=" + snippet(response));
        return new PrintReply(false, "打印服务拒绝了请求（错误代码 " + code + "）。");
    }

    /** The user-facing text of the response: {@code info} first, then the older field names. */
    private static String findMessage(JSONObject root) {
        String direct = firstNotEmpty(
                root.optString("info", ""),
                root.optString("message", ""),
                root.optString("msg", ""));
        if (!direct.isEmpty()) {
            return direct;
        }
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            return "";
        }
        return firstNotEmpty(
                data.optString("info", ""),
                data.optString("message", ""),
                data.optString("msg", ""));
    }

    private static String firstNotEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String mediaTypeName(String fileName) {
        String extension = "";
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            extension = fileName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        }
        String mimeType = MIME_TYPES.get(extension);
        return mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
    }

    private static String flag(boolean value) {
        return value ? "1" : "0";
    }

    private static String snippet(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }
        String compact = response.trim().replaceAll("\\s+", " ");
        return compact.length() > 160 ? compact.substring(0, 160) + "…" : compact;
    }

    private static String describe(String response) {
        String compact = snippet(response);
        return compact.isEmpty() ? "" : "：" + compact;
    }

    /** Result of one print request: {@code message} is the text coming from the service. */
    public static final class PrintReply {
        private final boolean success;
        private final String message;

        public PrintReply(boolean success, @NonNull String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
