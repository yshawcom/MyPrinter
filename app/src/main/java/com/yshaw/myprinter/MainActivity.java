package com.yshaw.myprinter;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.yshaw.myprinter.databinding.ActivityMainBinding;
import com.yshaw.myprinter.ui.ComposeBridgeKt;
import com.yshaw.myprinter.ui.PrintViewModel;

/**
 * Main screen for choosing a source file and submitting it to OnePrinter's print API.
 *
 * <p>Files opened from other apps (WeChat, DingTalk, file managers ...) land here as well; the
 * user then starts the print with the print button.
 */
public final class MainActivity extends ComponentActivity {

    private ActivityMainBinding binding;
    private PrintViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComposeBridgeKt.configureSystemBars(this);
        AppLog.user("打开主页面",
                "启动方式=" + (getIntent() == null ? "未知" : String.valueOf(getIntent().getAction())));

        // ViewBinding inflates the host layout, the Compose UI is attached to its ComposeView.
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(PrintViewModel.class);
        ComposeBridgeKt.showPrintScreen(binding.printComposeView, viewModel, () -> {
            AppLog.user("点击设置按钮");
            startActivity(new Intent(this, SettingsActivity.class));
        });

        if (savedInstanceState == null) {
            consumeIncomingFile(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        consumeIncomingFile(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshConfig();
        PrinterConfig config = viewModel.config.getValue();
        AppLog.event("刷新打印配置",
                "打印机=" + config.getPrinter() + ", 纸张=" + config.getPaperSize());
    }

    /** Stages a file handed over by another app; the user starts the print with the print button. */
    private void consumeIncomingFile(Intent intent) {
        if (intent == null) {
            return;
        }
        boolean fromHistory = (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
        if (fromHistory) {
            return;
        }
        IncomingFile incoming = FileIntake.resolve(this, intent);
        if (incoming == null) {
            if (!Intent.ACTION_MAIN.equals(intent.getAction())) {
                AppLog.event("Intent 中没有可打印的文件",
                        "action=" + intent.getAction()
                                + ", type=" + intent.getType()
                                + ", data=" + intent.getData());
            }
            return;
        }
        AppLog.user("接收到其他应用的文件",
                "action=" + intent.getAction()
                        + ", 文件名=" + incoming.getName()
                        + ", 类型=" + (incoming.isImage() ? "图片" : "文档")
                        + ", uri=" + incoming.getUri());
        FileIntake.persistReadPermission(this, intent, incoming.getUri());
        viewModel.openIncomingFile(incoming);
    }
}
