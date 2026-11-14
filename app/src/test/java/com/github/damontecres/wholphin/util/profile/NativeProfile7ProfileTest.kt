package com.github.damontecres.wholphin.util.profile

import android.util.Size
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.junit.Assert.*
import org.junit.Test

class NativeProfile7ProfileTest {
    private fun profile(native: Boolean) =
        createDeviceProfile(
            mediaTest =
                mockk<MediaCodecCapabilitiesTest>(relaxed = true) {
                    every { supportsHevc() } returns true
                    every { supportsHevcMain10() } returns true
                    every { getHevcMainLevel() } returns 153
                    every { getHevcMain10Level() } returns 153
                    every { getMaxResolution(any()) } returns
                        mockk<Size> {
                            every { width } returns 3840
                            every { height } returns 2160
                        }
                },
            maxBitrate = 200000000,
            isAC3Enabled = true,
            downMixAudio = false,
            assDirectPlay = true,
            pgsDirectPlay = true,
            decodeAv1 = false,
            jellyfinTenEleven = true,
            preferAc3ForSurround = false,
            forceEnabledHdr = emptySet(),
            forceDisabledHdr = emptySet(),
            maxResolution = 2160,
            nativeProfile7 = native,
        )

    @Test fun nativeP7HasSeparateLevelLimitAndRetainsGenericLimit() {
        val levels =
            profile(true).codecProfiles.orEmpty().filter {
                it.codec == "hevc" &&
                    it.conditions.any { c -> c.property == ProfileConditionValue.VIDEO_LEVEL }
            }
        assertEquals(3, levels.size)
        val raised = levels.single { it.conditions.any { c -> c.value == "156" } }
        assertTrue(
            raised.applyConditions.any {
                it.property == ProfileConditionValue.VIDEO_RANGE_TYPE &&
                    it.value?.contains("DOVIWithEL") == true
            },
        )
        assertEquals(2, levels.count { it.conditions.any { c -> c.value == "153" } })
    }

    @Test fun otherDevicesKeepReportedLevel() {
        assertFalse(
            profile(false).codecProfiles.orEmpty().any {
                it.codec == "hevc" &&
                    it.conditions.any { c -> c.property == ProfileConditionValue.VIDEO_LEVEL && c.value == "156" }
            },
        )
    }
}
