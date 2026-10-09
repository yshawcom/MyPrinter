package com.yshaw.myprinter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yshaw.myprinter.BuildConfig
import com.yshaw.myprinter.ConfigStore
import com.yshaw.myprinter.R

/** The only place where printer connection settings can be changed. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSave: (
        apiUrl: String,
        printer: String,
        paperSize: String,
        code: String,
        color: Boolean,
    ) -> Unit,
) {
    val context = LocalContext.current
    val stored = remember { ConfigStore.load(context) }
    var apiUrl by remember { mutableStateOf(stored.apiUrl) }
    var code by remember { mutableStateOf(stored.code) }
    var printer by remember { mutableStateOf(stored.printer) }
    var paperSize by remember { mutableStateOf(stored.paperSize) }
    var color by remember { mutableStateOf(stored.color) }
    val focusManager = LocalFocusManager.current

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SurfaceColor)
                .padding(start = 2.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "返回",
                    tint = Primary,
                )
            }
            Text(
                text = "设置",
                fontSize = 18.sp,
                color = TextPrimary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        ) {
            Text(
                text = "请勿随意修改该页面配置，否则打印可能失败！",
                fontSize = 14.sp,
                color = WarningText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarningBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(14.dp))

            SettingField(
                label = "打印服务API",
                hint = "例如：http://h.yshaw.com:10081/upload/single",
                value = apiUrl,
                onValueChange = { apiUrl = it },
                keyboardType = KeyboardType.Uri,
            )
            SecretField(
                label = "验证口令",
                hint = "请输入打印服务的验证口令",
                value = code,
                onValueChange = { code = it },
            )
            SettingField(
                label = "打印机名称",
                hint = "输入打印机名称",
                value = printer,
                onValueChange = { printer = it },
                keyboardType = KeyboardType.Text,
            )
            SettingField(
                label = "纸张大小",
                hint = "例如：A4",
                value = paperSize,
                onValueChange = { paperSize = it },
                keyboardType = KeyboardType.Text,
            )

            SwitchRow(
                label = "彩色打印",
                checked = color,
                onCheckedChange = { color = it },
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSave(apiUrl, printer, paperSize, code, color) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White,
                ),
            ) {
                Text("保存配置", fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))
            VersionInfo()
        }
    }
}

/** Shows the installed version name and version code, e.g. "版本名称 1.0 / 版本号 1". */
@Composable
private fun VersionInfo() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 12.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            fontSize = 12.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SecretField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Text(label, fontSize = 14.sp, color = TextPrimary)
    Spacer(Modifier.height(6.dp))
    // Masked by default; the text is only shown while the field is focused.
    CompactTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = hint,
        keyboardType = KeyboardType.Text,
        visualTransformation = if (focused) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun SettingField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
) {
    Text(label, fontSize = 14.sp, color = TextPrimary)
    Spacer(Modifier.height(6.dp))
    CompactTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = hint,
        keyboardType = keyboardType,
    )
    Spacer(Modifier.height(14.dp))
}
