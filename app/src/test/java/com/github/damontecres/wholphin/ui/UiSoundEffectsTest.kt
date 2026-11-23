package com.github.damontecres.wholphin.ui

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import com.github.damontecres.wholphin.R
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class UiSoundEffectsTest {
    private fun themedContext(): Context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_Wholphin)

    @Test fun appAndDialogViewHostsDisableAutomaticSounds() {
        val context = themedContext()
        assertFalse(context.uiSoundEffectsEnabled())
        // Compose creates views without XML attributes, so the theme flag alone is insufficient.
        for (viewContext in listOf(context, Dialog(context).context)) {
            val root = FrameLayout(viewContext)
            val host = ComposeView(viewContext)
            val inner = View(viewContext)
            root.addView(host)
            // Use a regular host to exercise the same ancestor chain as Compose's internal view.
            val container = FrameLayout(viewContext)
            root.addView(container)
            container.addView(inner)
            disableUiSoundEffects(host)
            disableUiSoundEffects(inner)
            assertFalse(host.isSoundEffectsEnabled)
            assertFalse(inner.isSoundEffectsEnabled)
            assertFalse(container.isSoundEffectsEnabled)
            assertFalse(root.isSoundEffectsEnabled)
        }
    }

    @Test fun explicitClicksDoNotRequestTheAudioService() {
        val context = object : ContextWrapper(themedContext()) {
            override fun getSystemService(name: String): Any? {
                check(name != Context.AUDIO_SERVICE) { "UI click requested the audio service" }
                return super.getSystemService(name)
            }
        }
        playOnClickSound(context)
    }
}
