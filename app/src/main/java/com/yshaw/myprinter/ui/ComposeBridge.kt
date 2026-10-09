package com.yshaw.myprinter.ui

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat

/**
 * The only place where the Java side needs Kotlin: Compose state and Compose content can only be
 * created from Kotlin, so these small helpers are called from the Java activities and the Java
 * [PrintViewModel].
 */

/** Java-friendly `mutableStateOf`. */
fun <T> composeState(value: T): MutableState<T> = mutableStateOf(value)

fun ComposeView.showPrintScreen(viewModel: PrintViewModel, onOpenSettings: Runnable) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        MyPrinterTheme {
            PrintScreen(viewModel = viewModel, onOpenSettings = { onOpenSettings.run() })
        }
    }
}

fun ComposeView.showSettingsScreen(onBack: Runnable, onSave: SettingsSaveListener) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        MyPrinterTheme {
            SettingsScreen(
                onBack = { onBack.run() },
                onSave = { apiUrl, printer, paperSize, code, color ->
                    onSave.onSave(apiUrl, printer, paperSize, code, color)
                },
            )
        }
    }
}

/**
 * Edge-to-edge system bars whose icons follow the system night mode, just like the UI palette does.
 */
fun configureSystemBars(activity: ComponentActivity) {
    activity.enableEdgeToEdge()
    val dark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    controller.isAppearanceLightStatusBars = !dark
    controller.isAppearanceLightNavigationBars = !dark
}
