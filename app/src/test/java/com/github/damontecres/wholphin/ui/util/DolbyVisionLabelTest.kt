package com.github.damontecres.wholphin.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DolbyVisionLabelTest {
    @Test fun distinguishesFelAndMel() {
        for (layer in listOf("FEL", "MEL")) {
            assertEquals("Dolby Vision P7 $layer", DolbyVisionLabel.format("Dolby Vision", 7, "original\n[hdrprobe-dv:v1;p=7;el=$layer]"))
        }
    }

    @Test fun missingProbeDoesNotGuessLayer() {
        assertEquals("Dolby Vision P7", DolbyVisionLabel.format("Dolby Vision", 7, null))
        assertEquals("Dolby Vision", DolbyVisionLabel.format("Dolby Vision", null, null))
    }

    @Test fun mismatchedOrAmbiguousAnnotationsDoNotLabelLayer() {
        assertEquals("Dolby Vision P8", DolbyVisionLabel.format("Dolby Vision", 8, "[hdrprobe-dv:v1;p=7;el=FEL]"))
        assertEquals(
            "Dolby Vision P7",
            DolbyVisionLabel.format("Dolby Vision", 7, "[hdrprobe-dv:v1;p=7;el=FEL][hdrprobe-dv:v1;p=7;el=MEL]"),
        )
    }

    @Test fun supportsProbeWithoutServerProfile() {
        assertEquals("Dolby Vision P7 MEL", DolbyVisionLabel.format("Dolby Vision", null, "[hdrprobe-dv:v1;p=7;el=MEL]"))
    }
}
