package com.yshaw.myprinter;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

import com.yshaw.myprinter.databinding.ActivitySettingsBinding;
import com.yshaw.myprinter.ui.ComposeBridgeKt;

/** Screen that edits the print service API, the verification code and the print defaults. */
public final class SettingsActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComposeBridgeKt.configureSystemBars(this);
        AppLog.user("打开设置页");

        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ComposeBridgeKt.showSettingsScreen(
                binding.settingsComposeView,
                () -> {
                    AppLog.user("点击返回，关闭设置页");
                    finish();
                },
                (apiUrl, printer, paperSize, code, color) -> {
                    ConfigStore.save(this, apiUrl, printer, paperSize, code, color);
                    AppLog.user("保存打印配置",
                            "API=" + apiUrl
                                    + ", 打印机=" + printer
                                    + ", 纸张=" + paperSize
                                    + ", 彩色=" + (color ? 1 : 0)
                                    + ", 口令长度=" + (code == null ? 0 : code.length()));
                    Toast.makeText(this, "打印配置已保存", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
    }
}
