package com.github.damontecres.wholphin.ui.util

/** Reads the HDR Metadata plugin's annotation without guessing FEL from a P7 container flag. */
object DolbyVisionLabel {
    private val annotation = Regex("""\[hdrprobe-dv:v1;p=(\d+);el=(FEL|MEL|unknown)\]""")

    fun format(
        name: String,
        profile: Int?,
        comment: String?,
    ): String {
        val matches = annotation.findAll(comment.orEmpty()).toList()
        val probe = matches.singleOrNull()
        val probedProfile =
            probe
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?.takeIf { it in 1..20 }
        val effectiveProfile = profile?.takeIf { it in 1..20 } ?: probedProfile
        val layer =
            probe?.groupValues?.get(2)?.takeIf {
                effectiveProfile in listOf(4, 7) && probedProfile == effectiveProfile && it in listOf("FEL", "MEL")
            }
        return listOfNotNull(name, effectiveProfile?.let { "P$it" }, layer).joinToString(" ")
    }
}
