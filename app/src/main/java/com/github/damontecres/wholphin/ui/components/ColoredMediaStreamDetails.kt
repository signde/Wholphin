package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.playback.audioStreamCount
import com.github.damontecres.wholphin.ui.playback.embeddedSubtitleCount
import com.github.damontecres.wholphin.ui.playback.externalSubtitlesCount
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization
import com.github.damontecres.wholphin.ui.util.MediaFlags
import com.github.damontecres.wholphin.ui.util.StreamFormatting
import com.github.damontecres.wholphin.ui.util.mediaFlagResources
import com.github.damontecres.wholphin.util.languageName
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream

@Composable
fun ColoredMediaStreamDetails(
    source: MediaSourceInfo?,
    video: MediaStream?,
    audio: MediaStream?,
    subtitle: MediaStream?,
    numberOfVersions: Int,
    modifier: Modifier = Modifier,
) {
    val style = LocalInterfaceCustomization.current.mediaIconStyle
    val flags =
        remember(source, video, audio) {
            listOfNotNull(MediaFlags.source(source, video)) + MediaFlags.video(video) + MediaFlags.audio(audio)
        }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            flags.forEach { flag ->
                val resource = mediaFlagResources[flag.artwork]?.resource(style)
                if (resource == null) {
                    StreamLabel(flag.label)
                } else {
                    Image(
                        painter = painterResource(resource),
                        contentDescription = flag.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(44.dp).height(44.dp * 43 / 64),
                    )
                }
            }
        }
        // Preserve selection context; these are not format flags.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (numberOfVersions > 1) StreamLabel(stringResource(R.string.media_flag_versions, numberOfVersions))
            val audioCount = source?.audioStreamCount ?: 0
            if (audio != null) {
                val language = languageName(audio.language).takeIf { audio.language != null }
                if (language != null || audioCount > 1) {
                    StreamLabel(language ?: stringResource(R.string.audio), count = audioCount, icon = R.string.fa_volume_high)
                }
            } else if (source != null) {
                StreamLabel(stringResource(R.string.none), icon = R.string.fa_volume_high)
            }
            val subtitleCount = (source?.embeddedSubtitleCount ?: 0) + (source?.externalSubtitlesCount ?: 0)
            if (subtitle != null || subtitleCount > 0) {
                val text =
                    subtitle?.let {
                        listOfNotNull(
                            languageName(it.language),
                            "SDH".takeIf {
                                subtitle.isHearingImpaired
                            },
                            StreamFormatting.formatSubtitleCodec(it.codec),
                        ).joinToString(" ")
                    } ?: stringResource(R.string.none)
                StreamLabel(
                    text,
                    count = subtitleCount,
                    icon = R.string.fa_closed_captioning,
                    disabled = subtitle == null,
                    modifier = Modifier.widthIn(max = 200.dp),
                )
            }
        }
    }
}
