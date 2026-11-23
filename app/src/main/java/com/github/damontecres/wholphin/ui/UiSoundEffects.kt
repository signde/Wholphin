package com.github.damontecres.wholphin.ui

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Silence the Compose view and its window hosts, including focus-navigation sounds. */
@Composable
fun DisableUiSoundEffects() {
    val view = LocalView.current
    SideEffect { disableUiSoundEffects(view) }
}

internal fun disableUiSoundEffects(view: View) {
    var current: View? = view
    while (current != null) {
        current.isSoundEffectsEnabled = false
        current = current.parent as? View
    }
}

/** Dialogs have their own Compose view and do not inherit the activity view's sound flag. */
@Composable
fun SilentDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        DisableUiSoundEffects()
        content()
    }
}
