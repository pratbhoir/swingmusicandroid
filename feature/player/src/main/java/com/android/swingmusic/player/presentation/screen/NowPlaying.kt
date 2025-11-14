package com.android.swingmusic.player.presentation.screen

import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.swingmusic.common.presentation.navigator.CommonNavigator
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.model.TrackArtist
import com.android.swingmusic.core.domain.util.PlaybackState
import com.android.swingmusic.core.domain.util.RepeatMode
import com.android.swingmusic.core.domain.util.ShuffleMode
import com.android.swingmusic.player.presentation.event.PlayerUiEvent
import com.android.swingmusic.player.presentation.event.QueueEvent
import com.android.swingmusic.player.presentation.util.calculateCurrentOffsetForPage
import com.android.swingmusic.player.presentation.viewmodel.MediaControllerViewModel
import com.android.swingmusic.uicomponent.R
import com.android.swingmusic.uicomponent.presentation.component.slider.WaveAnimationSpecs
import com.android.swingmusic.uicomponent.presentation.component.slider.WaveDirection
import com.android.swingmusic.uicomponent.presentation.component.slider.WavySlider
import com.android.swingmusic.uicomponent.presentation.theme.SwingMusicTheme_Preview
import com.android.swingmusic.uicomponent.presentation.util.BlurTransformation
import com.android.swingmusic.uicomponent.presentation.util.formatDuration
import com.ramcosta.composedestinations.annotation.Destination
import java.util.Locale

@Composable
private fun NowPlaying(
    track: Track?,
    playingTrackIndex: Int,
    queue: List<Track>,
    seekPosition: Float = 0F,
    playbackDuration: String,
    trackDuration: String,
    playbackState: PlaybackState,
    isBuffering: Boolean,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    baseUrl: String,
    onPageSelect: (page: Int) -> Unit,
    onClickArtist: (artistHash: String) -> Unit,
    onToggleRepeatMode: (RepeatMode) -> Unit,
    onClickPrev: () -> Unit,
    onTogglePlayerState: (PlaybackState) -> Unit,
    onResumePlayBackFromError: () -> Unit,
    onClickNext: () -> Unit,
    onToggleShuffleMode: (ShuffleMode) -> Unit,
    onSeekPlayBack: (Float) -> Unit,
    onClickMore: () -> Unit,
    onClickLyricsIcon: () -> Unit,
    onToggleFavorite: (Boolean, String) -> Unit,
    onClickQueueIcon: () -> Unit
) {
    if (track == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Playing track will appear here!",
                style = MaterialTheme.typography.titleSmall
            )
        }

        return
    }

    val fileType by remember {
        derivedStateOf {
            track.filepath.substringAfterLast(".").uppercase(Locale.ROOT)
        }
    }

    val isDarkTheme = isSystemInDarkTheme()
    val inverseOnSurface = MaterialTheme.colorScheme.inverseOnSurface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val fileTypeBadgeColor = when (track.bitrate) {
        in 321..1023 -> if (isDarkTheme) Color(0xFF172B2E) else Color(0xFFAEFAF4)
        in 1024..Int.MAX_VALUE -> if (isDarkTheme) Color(0XFF443E30) else Color(0xFFFFFBCC)
        else -> inverseOnSurface
    }
    val fileTypeTextColor = when (track.bitrate) {
        in 321..1023 -> if (isDarkTheme) Color(0XFF33FFEE) else Color(0xFF172B2E)
        in 1024..Int.MAX_VALUE -> if (isDarkTheme) Color(0XFFEFE143) else Color(0xFF221700)
        else -> onSurface
    }

    val animateWave = playbackState == PlaybackState.PLAYING && isBuffering.not()
    val repeatModeIcon = when (repeatMode) {
        RepeatMode.REPEAT_ONE -> R.drawable.repeat_one
        else -> R.drawable.repeat_all
    }
    val playbackStateIcon = when (playbackState) {
        PlaybackState.PLAYING -> R.drawable.pause_icon
        PlaybackState.PAUSED -> R.drawable.play_arrow_fill
        PlaybackState.ERROR -> R.drawable.error
    }

    val pagerState = rememberPagerState(
        initialPage = playingTrackIndex,
        pageCount = { if (queue.isEmpty()) 1 else queue.size }
    )

    var isInitialComposition by remember { mutableStateOf(true) }

    LaunchedEffect(
        key1 = playingTrackIndex,
        key2 = pagerState
    ) {
        if (playingTrackIndex in queue.indices) {
            if (playingTrackIndex != pagerState.currentPage) {
                pagerState.animateScrollToPage(playingTrackIndex)
            }
        }

        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (isInitialComposition) {
                isInitialComposition = false // Skip the first run
            } else {
                if (playingTrackIndex != page) {
                    onPageSelect(page)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.inverseOnSurface)
            )
        }
    ) { paddingValues ->
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1F),
            model = ImageRequest.Builder(LocalContext.current)
                .data("${baseUrl}img/thumbnail/${track.image}")
                .crossfade(true)
                .transformations(
                    listOf(
                        BlurTransformation(
                            scale = 0.25f,
                            radius = 25
                        )
                    )
                )
                .build(),
            contentDescription = "Track Image",
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1F)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = .75F),
                            MaterialTheme.colorScheme.surface.copy(alpha = 1F),
                            MaterialTheme.colorScheme.surface.copy(alpha = 1F)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            //PlayerTopBar: Back, Title, More
            PlayerTopBar()

            // Artwork, SeekBar...
            Column(
                modifier = Modifier
                    //.fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {

                HorizontalPager(
                    modifier = Modifier.fillMaxWidth(),
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    val imageData = if (page == playingTrackIndex) {
                        "${baseUrl}img/thumbnail/${queue.getOrNull(playingTrackIndex)?.image ?: track.image}"
                    } else {
                        "${baseUrl}img/thumbnail/${queue.getOrNull(page)?.image ?: track.image}"
                    }
                    val pageOffset = pagerState.calculateCurrentOffsetForPage(page)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Artwork
                        AsyncImage(
                            modifier = Modifier
                                .size(356.dp)
                                .clip(RoundedCornerShape(5))
                                .graphicsLayer {
                                    val scale = lerp(1f, 1.25f, pageOffset)
                                    scaleX = scale
                                    scaleY = scale
                                    clip = true
                                    shape = RoundedCornerShape(5)
                                },
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageData)
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(R.drawable.audio_fallback),
                            fallback = painterResource(R.drawable.audio_fallback),
                            error = painterResource(R.drawable.audio_fallback),
                            contentDescription = "Track Image",
                            contentScale = ContentScale.Crop
                        )
                    }
                }

            }

            Spacer(modifier = Modifier.height(24.dp))

            // Player Track Info: Title, Artist, Favorite
            PlayerTrackInfo(
                track,
                onClickArtist,
                onToggleFavorite,
                onSeekPlayBack,
                playbackState,
                seekPosition,
                playbackDuration,
                trackDuration,
                animateWave
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Player Controls: Shuffle, Next, Play/Pause, Forward, Repeat
            PlayerControls(
                playbackState,
                playbackStateIcon,
                onToggleShuffleMode,
                onClickPrev,
                onClickNext,
                shuffleMode,
                onResumePlayBackFromError,
                onToggleRepeatMode,
                repeatMode,
                repeatModeIcon,
                onTogglePlayerState,
                isBuffering,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bitrate, Track format
            PlayerExtraDetails(track,isDarkTheme)

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation and Control Icons
            PlayerBottomTabs(onClickQueueIcon,onClickLyricsIcon)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class CastDevice(
    val name: String,
    val iconRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var showCastScreen by remember { mutableStateOf(false) }
        val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

        IconButton(onClick = { backDispatcher?.onBackPressed() }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
        }

        Spacer(modifier = Modifier.weight(1f))

//        // Segmented Control for Song/Video
//        Row(
//            modifier = Modifier
//                .clip(CircleShape)
//                .background(Color.DarkGray)
//        ) {
//            TextButton(
//                onClick = { /* TODO */ },
//                colors = ButtonDefaults.textButtonColors(contentColor = Color.Black),
//                modifier = Modifier
//                    .clip(CircleShape)
//                    .background(Color.White)
//            ) {
//                Text("Song", fontWeight = FontWeight.Bold)
//            }
//            TextButton(
//                onClick = { /* TODO */ },
//                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
//            ) {
//                Text("Video")
//            }
//        }

        Spacer(modifier = Modifier.weight(1f))

        // TODO: Return this when cast is ready
        IconButton(onClick = { showCastScreen = true }) {
            Icon(painter = painterResource(id = R.drawable.cast), contentDescription = "Cast", tint = Color.White)
        }
        // TODO: Return this when contextual menu is ready
        IconButton(onClick = {  }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
        }

        //Casting screen

        var devices = listOf(
            CastDevice("Bedroom Speaker", R.drawable.cast),
            CastDevice("LivingRoom TV", R.drawable.cast)
        )

        fun onDeviceSelected(device: CastDevice){

        }

        if (showCastScreen) {
            ModalBottomSheet(
                onDismissRequest = {
                    showCastScreen = false
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                tonalElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connect to a device",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding( bottom = 12.dp, start = 12.dp)
                    )

                    devices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) }
                                .padding(vertical = 12.dp, horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = device.iconRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding( end = 5.dp)
                            )
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun PlayerTrackInfo(
    track: Track,
    onClickArtist: (String) -> Unit,
    onToggleFavorite: (Boolean, String) -> Unit,
    onSeekPlayBack: (Float) -> Unit,
    playbackState: PlaybackState,
    seekPosition: Float,
    playbackDuration: String,
    trackDuration: String,
    animateWave: Boolean,

    ) {

    Column(
        modifier = Modifier
        //.fillMaxSize()
        //.padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.fillMaxWidth(.78F)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(modifier = Modifier.fillMaxWidth()) {
                    track.trackArtists.forEachIndexed { index, trackArtist ->
                        item {
                            Text(
                                modifier = Modifier
                                    .clickable(
                                        onClick = { onClickArtist(trackArtist.artistHash) },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ),
                                text = trackArtist.name,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .84F),
                                overflow = TextOverflow.Ellipsis
                            )
                            if (index != track.trackArtists.lastIndex) {
                                Text(
                                    text = ", ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = .84F
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                modifier = Modifier
                    .clip(CircleShape),
                onClick = {
                    onToggleFavorite(track.isFavorite, track.trackHash)
                }) {
                val icon =
                    if (track.isFavorite) R.drawable.fav_filled
                    else R.drawable.fav_not_filled
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = "Favorite"
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            WavySlider(
                modifier = Modifier.height(12.dp),
                value = seekPosition,
                onValueChange = { value ->
                    onSeekPlayBack(value)
                },
                waveLength = 32.dp,
                waveHeight = 6.dp,
                waveVelocity = (if (animateWave) 16.dp else 0.dp) to WaveDirection.HEAD,
                waveThickness = 2.dp,
                trackThickness = 2.dp,
                incremental = false,
                animationSpecs = WaveAnimationSpecs(
                    waveHeightAnimationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    ),
                    waveVelocityAnimationSpec = tween(
                        durationMillis = 2000,
                        easing = LinearOutSlowInEasing
                    ),
                    waveStartSpreadAnimationSpec = tween(
                        durationMillis = 0,
                        easing = EaseOutQuad
                    )
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = playbackDuration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .84F)
                )
                Text(
                    text = if (playbackState == PlaybackState.ERROR)
                        track.duration.formatDuration() else trackDuration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .84F)
                )
            }
        }

    }

}

@Composable
private fun PlayerControls(
    playbackState: PlaybackState,
    playbackStateIcon: Int,
    onToggleShuffleMode: (ShuffleMode) -> Unit,
    onClickPrev: () -> Unit,
    onClickNext:() -> Unit,
    shuffleMode: ShuffleMode,
    onResumePlayBackFromError: () -> Unit,
    onToggleRepeatMode: (RepeatMode) -> Unit,
    repeatMode: RepeatMode,
    repeatModeIcon: Int,
    onTogglePlayerState: (PlaybackState) -> Unit,
    isBuffering: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val activeColor = Color.White
        val inactiveColor = Color.Gray

        IconButton(onClick = { onToggleShuffleMode(shuffleMode)}) {
            Icon(
                painter = painterResource(id = R.drawable.shuffle),
                //imageVector = Icons.Default.Clear,
                contentDescription = "Shuffle",
                tint = if (shuffleMode == ShuffleMode.SHUFFLE_ON) activeColor else inactiveColor
            )
        }

        IconButton(onClick = {  onClickPrev() }) {
            Icon(
                painter = painterResource(id = R.drawable.prev),
                //imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Previous",
                modifier = Modifier.size(40.dp),
                tint = activeColor
            )
        }

        // Large Play/Pause button
        Box(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    if (playbackState != PlaybackState.ERROR) {
                        onTogglePlayerState(playbackState)
                    } else {
                        onResumePlayBackFromError()
                    }
                }
            )
        ) {
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                if (playbackState == PlaybackState.ERROR) {
                    Icon(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .size(70.dp),
                        painter = painterResource(id = playbackStateIcon),
                        tint = if (isBuffering)
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = .25F) else
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = .75F),
                        contentDescription = "Error state"
                    )
                } else {
                    Box(
                        modifier = Modifier
                            //
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        //.background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(44.dp),
                            tint = Color.Black,
                            painter = painterResource(id = playbackStateIcon),
                            contentDescription = "Play/Pause"
                        )
                    }
                }

                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                        strokeCap = StrokeCap.Round,
                        strokeWidth = 1.dp,
                        //color = MaterialTheme.colorScheme.onSecondaryContainer
                        color = Color.Black
                    )
                }
            }
        }

        IconButton(onClick = {  onClickNext() }) {
            Icon(
                painter = painterResource(id = R.drawable.next),
                //imageVector = Icons.Default.PlayArrow,
                contentDescription = "Next",
                modifier = Modifier.size(40.dp),
                tint = activeColor
            )
        }

        IconButton(onClick = { onToggleRepeatMode(repeatMode) }) {
            Icon(
                //painter = painterResource(id = R.drawable.repeat_all),
                //imageVector = Icons.Default.PlayArrow,
                contentDescription = "Repeat",
                painter = painterResource(id = repeatModeIcon),
                tint = if (repeatMode == RepeatMode.REPEAT_OFF)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = .3F)
                else MaterialTheme.colorScheme.onSurface,
                //tint = if (repeatMode == RepeatMode.REPEAT_ALL) activeColor else inactiveColor
            )
        }
    }
}

@Composable
private fun PlayerExtraDetails(
    track: Track,
    isDarkTheme: Boolean
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val inverseOnSurface = MaterialTheme.colorScheme.inverseOnSurface
    val fileTypeTextColor = when (track.bitrate) {
        in 321..1023 -> if (isDarkTheme) Color(0XFF33FFEE) else Color(0xFF172B2E)
        in 1024..Int.MAX_VALUE -> if (isDarkTheme) Color(0XFFEFE143) else Color(0xFF221700)
        else -> onSurface
    }
    val fileTypeBadgeColor = when (track.bitrate) {
        in 321..1023 -> if (isDarkTheme) Color(0xFF172B2E) else Color(0xFFAEFAF4)
        in 1024..Int.MAX_VALUE -> if (isDarkTheme) Color(0XFF443E30) else Color(0xFFFFFBCC)
        else -> inverseOnSurface
    }
    val fileType by remember {
        derivedStateOf {
            track.filepath.substringAfterLast(".").uppercase(Locale.ROOT)
        }
    }

    // Bitrate, Track format
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24))
                .background(
                    if (isDarkTheme) fileTypeTextColor.copy(alpha = .075F) else fileTypeBadgeColor
                )
                .wrapContentSize()
                .padding(8.dp)

        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fileType,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = fileTypeTextColor
                )

                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = fileTypeTextColor
                )

                Text(
                    text = "${track.bitrate} Kbps",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = fileTypeTextColor
                )
            }
        }
    }
}


@Composable
private fun PlayerBottomTabs(
    onClickQueueIcon:() -> Unit,
    onClickLyricsIcon:() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Text("UP NEXT", modifier = Modifier.clickable { onClickQueueIcon() }, style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
        Text("LYRICS", modifier = Modifier.clickable { onClickLyricsIcon() }, style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray))
        // TODO: Return this when related concept is ready
        Text("RELATED", style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray))
    }
}


/**
 * Expose a public Composable tied to MediaControllerViewModel
 * **/

@Destination
@Composable
fun NowPlayingScreen(
    mediaControllerViewModel: MediaControllerViewModel,
    navigator: CommonNavigator
) {
    val playerUiState by mediaControllerViewModel.playerUiState.collectAsState()
    val baseUrl by mediaControllerViewModel.baseUrl.collectAsState()

    NowPlaying(
        track = playerUiState.nowPlayingTrack,
        playingTrackIndex = playerUiState.playingTrackIndex,
        queue = playerUiState.queue,
        seekPosition = playerUiState.seekPosition,
        playbackDuration = playerUiState.playbackDuration,
        trackDuration = playerUiState.trackDuration,
        playbackState = playerUiState.playbackState,
        repeatMode = playerUiState.repeatMode,
        shuffleMode = playerUiState.shuffleMode,
        isBuffering = playerUiState.isBuffering,
        baseUrl = baseUrl ?: "",
        onPageSelect = { page ->
            // treat this as clicking a track in queue
            mediaControllerViewModel.onQueueEvent(
                QueueEvent.SeekToQueueItem(page)
            )
        },
        onClickArtist = {
            navigator.gotoArtistInfo(it)
        },
        onToggleRepeatMode = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnToggleRepeatMode
            )
        },
        onClickPrev = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnPrev
            )
        },
        onTogglePlayerState = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnTogglePlayerState
            )
        },
        onResumePlayBackFromError = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnResumePlaybackFromError
            )
        },
        onClickNext = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnNext
            )
        },
        onToggleShuffleMode = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnToggleShuffleMode(
                    toggleShuffle = true
                )
            )
        },
        onSeekPlayBack = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnSeekPlayBack(it)
            )
        },
        onClickLyricsIcon = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnClickLyricsIcon
            )
        },
        onToggleFavorite = { isFavorite, trackHash ->
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnToggleFavorite(isFavorite, trackHash)
            )
        },
        onClickQueueIcon = {
            navigator.gotoQueueScreen()
        },
        onClickMore = {
            mediaControllerViewModel.onPlayerUiEvent(
                PlayerUiEvent.OnClickMore
            )
        }
    )
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    wallpaper = RED_DOMINATED_EXAMPLE,
    device = Devices.PIXEL_5
)
@Composable
fun FullPlayerPreview() {
    val lilPeep = TrackArtist(
        artistHash = "lilpeep123",
        image = "lilpeep.jpg",
        name = "Lil Peep"
    )

    val juice = TrackArtist(
        artistHash = "juice123",
        image = "juice.jpg",
        name = "Juice WRLD"
    )
    val young = TrackArtist(
        artistHash = "young123",
        image = "young.jpg",
        name = "Young Thug"
    )

    val albumArtists = listOf(lilPeep, juice)
    val artists = listOf(juice, young)

    val track = Track(
        album = "Sample Album",
        albumTrackArtists = albumArtists,
        albumHash = "albumHash123",
        trackArtists = artists,
        bitrate = 320,
        duration = 454, // Sample duration in seconds
        filepath = "/path/to/track.mp3",
        folder = "/path/to/folder",
        image = "/path/to/album/artwork.jpg",
        isFavorite = true,
        title = "Save Your Tears",
        trackHash = "trackHash123",
        disc = 1,
        trackNumber = 1
    )

    SwingMusicTheme_Preview {
        NowPlaying(
            track = track,
            playingTrackIndex = 0,
            queue = emptyList(),
            seekPosition = .22F,
            playbackDuration = "01:23",
            trackDuration = "02:59",
            playbackState = PlaybackState.PLAYING,
            isBuffering = false,
            repeatMode = RepeatMode.REPEAT_OFF,
            shuffleMode = ShuffleMode.SHUFFLE_OFF,
            baseUrl = "",
            onPageSelect = {},
            onClickArtist = {},
            onToggleRepeatMode = {},
            onResumePlayBackFromError = {},
            onClickPrev = {},
            onTogglePlayerState = {},
            onClickNext = {},
            onToggleShuffleMode = {},
            onSeekPlayBack = {},
            onClickLyricsIcon = {},
            onToggleFavorite = { _, _ -> },
            onClickQueueIcon = {},
            onClickMore = {}
        )
    }
}
