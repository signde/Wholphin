package com.github.damontecres.wholphin.ui.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.DisplayToggle
import com.github.damontecres.wholphin.preferences.MediaIconStyle
import java.util.EnumSet

/**
 * Represents various UI preferences made available via [LocalInterfaceCustomization]
 */
data class InterfaceCustomization(
    val enabledDisplayToggles: EnumSet<DisplayToggle>,
    val mediaIconStyle: MediaIconStyle = MediaIconStyle.ICON_COLOR,
) {
    val showMediaIcons: Boolean get() = mediaIconStyle != MediaIconStyle.ICON_TEXT

    constructor(prefs: AppPreferences) : this(
        prefs.interfacePreferences.displayTogglesList.let {
            if (it.isEmpty()) {
                EnumSet.noneOf(DisplayToggle::class.java)
            } else {
                EnumSet.copyOf(it)
            }
        },
        AppPreference.MediaIcons.getter(prefs),
    )
}

val LocalInterfaceCustomization =
    staticCompositionLocalOf<InterfaceCustomization> {
        InterfaceCustomization(EnumSet.allOf(DisplayToggle::class.java))
    }
