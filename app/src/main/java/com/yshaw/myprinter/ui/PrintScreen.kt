package com.yshaw.myprinter.ui

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yshaw.myprinter.FileIntake
import com.yshaw.myprinter.AppLog
import com.yshaw.myprinter.CameraTarget
import com.yshaw.myprinter.PrinterConfig
import com.yshaw.myprinter.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DOCUMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
)

/** Main screen: pick a file (or receive one from another app) and send it to the printer. */
@Composable
fun PrintScreen(
    viewModel: PrintViewModel,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<CameraTarget?>(null) }

    // The system photo picker (gallery) is used for images; it falls back to a document picker
    // only on devices that do not ship one.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            FileIntake.persistReadPermission(context, null, uri)
            viewModel.selectFile(uri, FileIntake.displayName(context, uri), true)
        } else {
            AppLog.event("相册未返回图片，用户可能取消了选择")
        }
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            FileIntake.persistReadPermission(context, null, uri)
            viewModel.selectFile(uri, FileIntake.displayName(context, uri), false)
        } else {
            AppLog.event("文件选择器未返回文件，用户可能取消了选择")
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val target = cameraTarget
        if (saved && target != null && target.file.exists() && target.file.length() > 0) {
            AppLog.event("拍照完成", "文件=${target.file.name}, 大小=${target.file.length()} 字节")
            viewModel.selectFile(target.uri, target.file.name, true)
        } else {
            AppLog.warn("相机没有返回可用的照片", "saved=$saved")
            Toast.makeText(context, "未获取到照片，请重试。", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            // Tapping outside the inputs closes the soft keyboard.
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        HeaderBar(onOpenSettings = onOpenSettings)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        ) {
            ConfigSummary(viewModel.config.value)
            Spacer(Modifier.height(12.dp))
            SectionTitle("选择文件")
            Spacer(Modifier.height(8.dp))
            SourceButtons(
                enabled = !viewModel.printing.value,
                onPickImage = {
                    AppLog.user("点击「选择图片」按钮")
                    showImageSourceDialog = true
                },
                onPickDocument = {
                    AppLog.user("点击「选择文档」按钮", "类型=PDF/Word/PPT")
                    documentPicker.launch(DOCUMENT_MIME_TYPES)
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "支持文件格式：pdf、jpg、jpeg、png、gif、webp、bmp、cad、dwg、" +
                    "doc、docx、ppt、pptx、xls、xlsx",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TextSecondary,
            )
            viewModel.selection.value?.let { selection ->
                Spacer(Modifier.height(12.dp))
                SelectionCard(
                    selection = selection,
                    copiesText = viewModel.copiesText.value,
                    onCopiesChange = viewModel::updateCopies,
                    layout = viewModel.layout.value,
                    onLayoutChange = viewModel::updateLayout,
                    duplex = viewModel.duplex.value,
                    onDuplexChange = viewModel::updateDuplex,
                    pageRange = viewModel.pageRange.value,
                    onPageRangeChange = viewModel::updatePageRange,
                    openedFromAnotherApp = viewModel.openedFromAnotherApp.value,
                )
            }
            Spacer(Modifier.height(12.dp))
            PrintButton(
                printing = viewModel.printing.value,
                enabled = viewModel.selection.value != null && !viewModel.printing.value,
                onClick = viewModel::printNow,
            )
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = {
                Column {
                    Text("选择图片", fontSize = 18.sp, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("请选择图片的来源", fontSize = 13.sp, color = TextSecondary)
                }
            },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(18.dp),
            text = {
                Column(Modifier.fillMaxWidth()) {
                    ImageSourceOption(
                        iconRes = R.drawable.ic_select_image,
                        title = "从相册选择",
                        subtitle = "挑选相册中已有的图片",
                        onClick = {
                            showImageSourceDialog = false
                            AppLog.user("选择图片来源：系统相册")
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    ImageSourceOption(
                        iconRes = R.drawable.ic_camera,
                        title = "打开相机拍摄",
                        subtitle = "拍一张新的照片来打印",
                        onClick = {
                            showImageSourceDialog = false
                            if (hasCameraApp(context)) {
                                AppLog.user("选择图片来源：相机拍照")
                                val target = FileIntake.newCameraTarget(context)
                                cameraTarget = target
                                cameraLauncher.launch(target.uri)
                            } else {
                                AppLog.warn("设备上没有可用的相机应用")
                                Toast.makeText(context, "设备上没有可用的相机应用。", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("取消", fontSize = 14.sp, color = TextSecondary)
                }
            },
        )
    }

    viewModel.dialog.value?.let { dialog ->
        when (dialog.kind) {
            PrintDialog.Kind.NEED_CONFIG -> AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(dialog.title) },
                text = { Text(dialog.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            AppLog.user("打印配置未完成，前往设置页")
                            viewModel.dismissDialog()
                            onOpenSettings()
                        },
                    ) {
                        Text("去设置")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            AppLog.user("取消前往设置")
                            viewModel.dismissDialog()
                        },
                    ) {
                        Text("取消")
                    }
                },
            )

            else -> AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(dialog.title) },
                text = { Text(dialog.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            AppLog.event("关闭提示弹窗", "标题=${dialog.title}")
                            viewModel.dismissDialog()
                        },
                    ) {
                        Text("确定")
                    }
                },
            )
        }
    }
}

@Composable
private fun ImageSourceOption(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PreviewBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun HeaderBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(SurfaceColor)
            .padding(start = 16.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.oneprinter_logo),
            contentDescription = "OnePrinter 图标",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Text(
            text = "我的打印机",
            fontSize = 19.sp,
            color = TextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "打开打印设置",
                tint = Primary,
            )
        }
    }
}

/** One-line summary of the configured printer, e.g. "HP LaserJet 1020    A4". */
@Composable
private fun ConfigSummary(config: PrinterConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayValue(config.printer),
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = displayValue(config.paperSize),
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 15.sp, color = TextPrimary)
}

@Composable
private fun SourceButtons(
    enabled: Boolean,
    onPickImage: () -> Unit,
    onPickDocument: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionButton(
            label = "图片",
            iconRes = R.drawable.ic_select_image,
            enabled = enabled,
            onClick = onPickImage,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = "文档",
            iconRes = R.drawable.ic_select_document,
            enabled = enabled,
            onClick = onPickDocument,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 15.sp)
    }
}

@Composable
private fun SelectionCard(
    selection: PrintSelection,
    copiesText: String,
    onCopiesChange: (String) -> Unit,
    layout: Boolean,
    onLayoutChange: (Boolean) -> Unit,
    duplex: Boolean,
    onDuplexChange: (Boolean) -> Unit,
    pageRange: String,
    onPageRangeChange: (String) -> Unit,
    openedFromAnotherApp: Boolean,
) {
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(
        initialValue = null,
        selection.uri,
        selection.isImage,
    ) {
        value = if (selection.isImage) {
            withContext(Dispatchers.IO) {
                FileIntake.decodePreview(context, selection.uri)?.asImageBitmap()
            }
        } else {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("待打印文件", fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            if (selection.isImage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PreviewBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    val bitmap = preview
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "待打印图片预览",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text("无法预览，不影响打印", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = selection.name,
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (openedFromAnotherApp) {
                Spacer(Modifier.height(4.dp))
                Text("来自其他应用的文件", fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(10.dp))
            Text("打印份数", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            CompactTextField(
                value = copiesText,
                onValueChange = onCopiesChange,
                placeholder = "1",
                keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(8.dp))
            OptionSwitch(
                label = "横版打印",
                checked = layout,
                onCheckedChange = onLayoutChange,
            )
            // Duplex and page ranges only make sense for documents.
            if (!selection.isImage) {
                OptionSwitch(
                    label = "双面打印",
                    checked = duplex,
                    onCheckedChange = onDuplexChange,
                )
                Spacer(Modifier.height(6.dp))
                Text("指定页码", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                CompactTextField(
                    value = pageRange,
                    onValueChange = onPageRangeChange,
                    placeholder = "如：1-5,8,10-12（留空表示全部）",
                    keyboardType = KeyboardType.Text,
                )
            }
        }
    }
}

/** Label on the left, switch on the right; off = 0, on = 1. */
@Composable
private fun OptionSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PrintButton(printing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White,
        ),
    ) {
        Text(
            text = if (printing) "正在发送…" else "打印",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun displayValue(value: String): String = value.ifBlank { "未设置" }

private fun hasCameraApp(context: Context): Boolean =
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null
