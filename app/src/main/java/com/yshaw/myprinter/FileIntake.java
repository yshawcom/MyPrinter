package com.yshaw.myprinter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Size;
import android.webkit.MimeTypeMap;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Bridges the outside world into the app: resolves the file behind "open with" / "share to"
 * intents (WeChat, DingTalk, file managers, browsers ...) and offers the small set of URI
 * helpers the print flow needs.
 */
public final class FileIntake {
    private static final int PREVIEW_MAX_SIZE = 1080;

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "bmp", "webp", "gif", "heic", "heif", "tif", "tiff"
    ));

    private FileIntake() {
    }

    /** Returns the file carried by the intent, or null when the intent is not a file hand-off. */
    public static IncomingFile resolve(Context context, Intent intent) {
        if (intent == null) {
            return null;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_VIEW.equals(action)
                && !Intent.ACTION_SEND.equals(action)
                && !Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            return null;
        }
        Uri uri = firstUri(intent);
        if (uri == null) {
            return null;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"content".equals(scheme) && !"file".equals(scheme)) {
            // Links (http/https) are not printable files and are ignored on purpose.
            AppLog.warn("忽略非文件链接", "scheme=" + scheme + ", uri=" + uri);
            return null;
        }
        String name = displayName(context, uri);
        return new IncomingFile(uri, name, isImage(context, intent, uri, name));
    }

    /** Persists the read grant when the sender allows it; silently ignored otherwise. */
    public static void persistReadPermission(Context context, Intent intent, Uri uri) {
        int flags = intent == null
                ? Intent.FLAG_GRANT_READ_URI_PERMISSION
                : intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            context.getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception ignored) {
            // Many senders hand out a one-shot grant only; the active grant still works.
        }
    }

    public static String displayName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to the last path segment below.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        String segment = uri.getLastPathSegment();
        return segment == null || segment.isEmpty() ? "已选择的文件" : segment;
    }

    /**
     * The name used for the multipart {@code file} part.
     *
     * The print service picks its document converter from the file extension, so when a provider
     * hands out a name without one, fall back to the URI or to the content type.
     */
    public static String uploadFileName(Context context, Uri uri, String displayName) {
        if (!extensionOf(displayName).isEmpty()) {
            return displayName;
        }
        String segment = uri.getLastPathSegment();
        String fromUri = segment == null ? "" : extensionOf(segment);
        if (!fromUri.isEmpty()) {
            return displayName + "." + fromUri;
        }
        String mimeType = null;
        try {
            mimeType = context.getContentResolver().getType(uri);
        } catch (Exception ignored) {
            // Leave the plain display name below.
        }
        String fromMimeType = mimeType == null
                ? null
                : MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return fromMimeType == null || fromMimeType.isEmpty()
                ? displayName
                : displayName + "." + fromMimeType;
    }

    public static byte[] readAllBytes(Context context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("无法读取所选文件");
        }
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            byte[] bytes = output.toByteArray();
            AppLog.event("读取文件完成", "大小=" + bytes.length + " 字节");
            return bytes;
        }
    }

    /**
     * Decodes a down-scaled preview so large photos stay cheap to draw. Several providers (and
     * several image formats) behave differently, so the decoders are tried from the most capable
     * one to the most compatible one.
     */
    public static Bitmap decodePreview(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "content".equals(uri.getScheme())) {
            Bitmap thumbnail = thumbnailOrNull(context, uri);
            if (thumbnail != null) {
                AppLog.event("图片预览解码完成",
                        "方式=系统缩略图, " + thumbnail.getWidth() + "x" + thumbnail.getHeight());
                return thumbnail;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Bitmap decoded = decoderBitmapOrNull(context, uri);
            if (decoded != null) {
                AppLog.event("图片预览解码完成",
                        "方式=ImageDecoder, " + decoded.getWidth() + "x" + decoded.getHeight());
                return decoded;
            }
        }
        Bitmap bitmap = factoryBitmapOrNull(context, uri);
        if (bitmap == null) {
            AppLog.warn("图片预览解码失败", "uri=" + uri);
        } else {
            AppLog.event("图片预览解码完成",
                    "方式=BitmapFactory, " + bitmap.getWidth() + "x" + bitmap.getHeight());
        }
        return bitmap;
    }

    /** Cache file plus content URI the system camera can write a full-resolution photo into. */
    public static CameraTarget newCameraTarget(Context context) {
        File directory = new File(context.getCacheDir(), "camera");
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
        File file = new File(directory, "myprinter_" + System.currentTimeMillis() + ".jpg");
        Uri uri = new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".camera-fileprovider")
                .appendPath("camera")
                .appendPath(file.getName())
                .build();
        AppLog.event("创建拍照输出文件", "文件=" + file.getName());
        return new CameraTarget(file, uri);
    }

    private static Uri firstUri(Intent intent) {
        if (intent.getData() != null) {
            return intent.getData();
        }
        if (intent.getClipData() != null) {
            for (int index = 0; index < intent.getClipData().getItemCount(); index++) {
                Uri uri = intent.getClipData().getItemAt(index).getUri();
                if (uri != null) {
                    return uri;
                }
            }
        }
        Object extra = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        return extra instanceof Uri ? (Uri) extra : null;
    }

    private static boolean isImage(Context context, Intent intent, Uri uri, String name) {
        String type = intent.getType();
        if (type != null && type.startsWith("image/")) {
            return true;
        }
        String resolved = null;
        try {
            resolved = context.getContentResolver().getType(uri);
        } catch (Exception ignored) {
            // Fall back to the file name below.
        }
        if (resolved != null && resolved.startsWith("image/")) {
            return true;
        }
        String extension = extensionOf(name);
        return !extension.isEmpty() && IMAGE_EXTENSIONS.contains(extension);
    }

    private static String extensionOf(String value) {
        if (value == null) {
            return "";
        }
        int dot = value.lastIndexOf('.');
        if (dot < 0 || dot == value.length() - 1) {
            return "";
        }
        return value.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static Bitmap thumbnailOrNull(Context context, Uri uri) {
        try {
            return context.getContentResolver().loadThumbnail(
                    uri, new Size(PREVIEW_MAX_SIZE, PREVIEW_MAX_SIZE), null);
        } catch (Exception error) {
            return null;
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private static Bitmap decoderBitmapOrNull(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, ignored) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                int width = info.getSize().getWidth();
                int height = info.getSize().getHeight();
                int longestSide = Math.max(width, height);
                if (longestSide > PREVIEW_MAX_SIZE) {
                    float scale = (float) PREVIEW_MAX_SIZE / longestSide;
                    decoder.setTargetSize(
                            Math.max(1, (int) (width * scale)),
                            Math.max(1, (int) (height * scale)));
                }
            });
        } catch (Exception error) {
            return null;
        }
    }

    private static Bitmap factoryBitmapOrNull(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream boundsStream = context.getContentResolver().openInputStream(uri);
            if (boundsStream != null) {
                try (InputStream stream = boundsStream) {
                    // Fills the bounds only; the returned bitmap is always null for this pass.
                    BitmapFactory.decodeStream(stream, null, bounds);
                }
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }
            int sampleSize = 1;
            while (bounds.outWidth / sampleSize > PREVIEW_MAX_SIZE
                    || bounds.outHeight / sampleSize > PREVIEW_MAX_SIZE) {
                sampleSize *= 2;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            InputStream stream = context.getContentResolver().openInputStream(uri);
            if (stream == null) {
                return null;
            }
            try (InputStream input = stream) {
                return BitmapFactory.decodeStream(input, null, options);
            }
        } catch (Exception error) {
            return null;
        }
    }
}
