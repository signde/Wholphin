package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization
import com.github.damontecres.wholphin.ui.util.MediaFlags
import com.github.damontecres.wholphin.ui.util.mediaFlagResources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class LibraryMediaDetailsViewModel
    @Inject
    constructor(
        private val api: ApiClient,
    ) : ViewModel() {
        suspend fun details(item: BaseItem): BaseItemDto? =
            try {
                api.libraryApi.getItem(itemId = item.id).content
            } catch (ex: CancellationException) {
                throw ex
            } catch (_: Exception) {
                // Missing metadata must not interrupt library browsing.
                null
            }
    }

/** Format icons describe the focused file, never an entire series or collection. */
@Composable
fun LibraryMediaDetails(
    item: BaseItem,
    timeRemaining: Duration?,
    modifier: Modifier = Modifier,
) {
    val customization = LocalInterfaceCustomization.current
    val enabled = customization.showMediaIcons
    val isVideo = item.type in listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.VIDEO, BaseItemKind.MUSIC_VIDEO)
    val viewModel: LibraryMediaDetailsViewModel = hiltViewModel()
    // Browse requests include streams, so changing focus can render the new badges immediately.
    val inlineData = item.data.takeIf { !it.mediaStreams.isNullOrEmpty() || !it.mediaSources.isNullOrEmpty() }
    val fetchedData by produceState<BaseItemDto?>(null, item, enabled) {
        value = null
        if (enabled && isVideo && inlineData == null) {
            // Fallback for items from endpoints that omit stream metadata.
            delay(200)
            value = viewModel.details(item)
        }
    }
    val data = if (enabled && isVideo) inlineData ?: fetchedData?.takeIf { it.id == item.id } else null
    val flags =
        remember(data, item.id, customization.mediaIconStyle) {
            val current = data?.takeIf { it.id == item.id }
            val source = current?.mediaSources?.firstOrNull()
            val streams = source?.mediaStreams ?: current?.mediaStreams.orEmpty()
            val video = streams.firstOrNull { it.type == MediaStreamType.VIDEO }
            val audioStreams = streams.filter { it.type == MediaStreamType.AUDIO }
            val audio = audioStreams.firstOrNull { it.isDefault } ?: audioStreams.firstOrNull()
            (listOfNotNull(MediaFlags.source(source, video, current?.path)) + MediaFlags.video(video) + MediaFlags.audio(audio))
                .mapNotNull { flag -> mediaFlagResources[flag.artwork]?.resource(customization.mediaIconStyle)?.let { it to flag.label } }
        }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier) {
        QuickDetails(
            item.ui.quickDetails,
            timeRemaining,
            endsAt = item.data.endDate,
        )
        if (enabled) {
            // Keep the synopsis stationary even when a title has no format metadata.
            BoxWithConstraints(Modifier.fillMaxWidth().height(30.dp)) {
                if (flags.isEmpty()) return@BoxWithConstraints
                // Reserve room for the full set: four video, one source and two audio icons.
                // Size depends on the layout's available width, not the title or badge count.
                val slots = 7
                val gap = minOf(4.dp, maxWidth / (slots * 4))
                val iconWidth = minOf(44.dp, (maxWidth - gap * (slots - 1)) / slots)
                Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                    flags.forEach { (resource, label) ->
                        Image(
                            painter = painterResource(resource),
                            contentDescription = label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.width(iconWidth).height(iconWidth * 43 / 64),
                        )
                    }
                }
            }
        }
    }
}
