package com.android.swingmusic.album.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.swingmusic.album.domain.AlbumRepository
import com.android.swingmusic.album.presentation.event.AlbumWithInfoUiEvent
import com.android.swingmusic.album.presentation.state.AlbumInfoWithGroupedTracks
import com.android.swingmusic.album.presentation.state.AlbumWithInfoState
import com.android.swingmusic.core.data.util.Resource
import com.android.swingmusic.core.domain.model.AlbumWithInfo
import com.android.swingmusic.player.domain.repository.PLayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumWithInfoViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val pLayerRepository: PLayerRepository
) : ViewModel() {

    private val _albumWithInfoState: MutableStateFlow<AlbumWithInfoState> =
        MutableStateFlow(AlbumWithInfoState())
    val albumWithInfoState: StateFlow<AlbumWithInfoState> get() = _albumWithInfoState

    private fun updateAlbumInfoState(resource: Resource<AlbumWithInfo>) {
        when (resource) {
            is Resource.Success -> {
                val groupedTracks = resource.data?.tracks
                    ?.sortedBy { track -> track.trackNumber }
                    ?.groupBy { track -> track.disc }
                    ?.toSortedMap()

                val orderedTracks = groupedTracks?.values?.flatten() ?: emptyList()

                _albumWithInfoState.value = _albumWithInfoState.value.copy(
                    reloadRequired = false,
                    orderedTracks = orderedTracks,
                    infoResource = Resource.Success(
                        data = AlbumInfoWithGroupedTracks(
                            albumInfo = resource.data?.albumInfo,
                            groupedTracks = groupedTracks ?: emptyMap(),
                            copyright = resource.data?.copyright ?: ""
                        )
                    )
                )
            }

            is Resource.Loading -> {
                _albumWithInfoState.value =
                    _albumWithInfoState.value.copy(infoResource = Resource.Loading())
            }

            else -> {
                _albumWithInfoState.value =
                    _albumWithInfoState.value.copy(
                        infoResource = Resource.Error(
                            message = resource.message!!
                        )
                    )
            }
        }
    }

    private fun toggleAlbumFavorite(albumHash: String, isFavorite: Boolean) {
        viewModelScope.launch {
            // Optimistically update the UI
            _albumWithInfoState.value = _albumWithInfoState.value.copy(
                infoResource = Resource.Success(
                    AlbumInfoWithGroupedTracks(
                        albumInfo = _albumWithInfoState.value.infoResource.data?.albumInfo?.copy(
                            isFavorite = !isFavorite
                        ),
                        groupedTracks = _albumWithInfoState.value.infoResource.data?.groupedTracks
                            ?: emptyMap(),
                        copyright = _albumWithInfoState.value.infoResource.data?.copyright
                    )
                )
            )

            val request = if (isFavorite) {
                albumRepository.removeAlbumFromFavorite(albumHash)
            } else {
                albumRepository.addAlbumToFavorite(albumHash)
            }

            request.collectLatest {
                when (it) {
                    is Resource.Loading -> {}

                    is Resource.Success -> {
                        _albumWithInfoState.value = _albumWithInfoState.value.copy(
                            infoResource = Resource.Success(
                                AlbumInfoWithGroupedTracks(
                                    albumInfo = _albumWithInfoState.value.infoResource.data?.albumInfo?.copy(
                                        isFavorite = it.data ?: false
                                    ),
                                    groupedTracks = _albumWithInfoState.value.infoResource.data?.groupedTracks
                                        ?: emptyMap(),
                                    copyright = _albumWithInfoState.value.infoResource.data?.copyright
                                )
                            )
                        )
                    }

                    is Resource.Error -> {
                        _albumWithInfoState.value = _albumWithInfoState.value.copy(
                            infoResource = Resource.Success(
                                AlbumInfoWithGroupedTracks(
                                    albumInfo = _albumWithInfoState.value.infoResource.data?.albumInfo?.copy(
                                        isFavorite = isFavorite
                                    ),
                                    groupedTracks = _albumWithInfoState.value.infoResource.data?.groupedTracks
                                        ?: emptyMap(),
                                    copyright = _albumWithInfoState.value.infoResource.data?.copyright
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun toggleAlbumTrackFavorite(trackHash: String, isFavorite: Boolean) {
        viewModelScope.launch {
            // Optimistically update the Ui
            _albumWithInfoState.value = _albumWithInfoState.value.copy(
                orderedTracks = _albumWithInfoState.value.orderedTracks.map { track ->
                    if (track.trackHash == trackHash) {
                        track.copy(isFavorite = !isFavorite)
                    } else {
                        track
                    }
                }
            )

            _albumWithInfoState.value = _albumWithInfoState.value.copy(
                infoResource = Resource.Success(
                    AlbumInfoWithGroupedTracks(
                        albumInfo = _albumWithInfoState.value.infoResource.data?.albumInfo,
                        groupedTracks = _albumWithInfoState.value.infoResource.data?.groupedTracks?.mapValues { entry ->
                            entry.value.map { track ->
                                if (track.trackHash == trackHash) {
                                    track.copy(isFavorite = !isFavorite)
                                } else {
                                    track
                                }
                            }
                        } ?: emptyMap(),
                        copyright = _albumWithInfoState.value.infoResource.data?.copyright
                    )
                )
            )


            val request = if (isFavorite) {
                pLayerRepository.removeTrackFromFavorite(trackHash)
            } else {
                pLayerRepository.addTrackToFavorite(trackHash)
            }

            request.collectLatest {
                when (it) {
                    is Resource.Loading -> {}

                    is Resource.Success -> {
                        _albumWithInfoState.value = _albumWithInfoState.value.copy(
                            orderedTracks = _albumWithInfoState.value.orderedTracks.map { track ->
                                if (track.trackHash == trackHash) {
                                    track.copy(isFavorite = it.data ?: false)
                                } else {
                                    track
                                }
                            }
                        )

                        _albumWithInfoState.value = _albumWithInfoState.value.copy(
                            infoResource = Resource.Success(
                                AlbumInfoWithGroupedTracks(
                                    albumInfo = _albumWithInfoState.value.infoResource.data?.albumInfo,
                                    groupedTracks = _albumWithInfoState.value.infoResource.data?.groupedTracks?.mapValues { entry ->
                                        entry.value.map { track ->
                                            if (track.trackHash == trackHash) {
                                                track.copy(isFavorite = it.data ?: false)
                                            } else {
                                                track
                                            }
                                        }
                                    } ?: emptyMap(),
                                    copyright = _albumWithInfoState.value.infoResource.data?.copyright
                                )
                            )
                        )
                    }

                    is Resource.Error -> {
                        // Revert the optimistic updates in case of an error
                        _albumWithInfoState.value = _albumWithInfoState.value.copy(
                            orderedTracks = _albumWithInfoState.value.orderedTracks.map { track ->
                                if (track.trackHash == trackHash) {
                                    track.copy(isFavorite = isFavorite)
                                } else {
                                    track
                                }
                            }
                        )

                        _albumWithInfoState.value = _albumWithInfoState.value.copy(
                            infoResource = Resource.Success(
                                AlbumInfoWithGroupedTracks(
                                    albumInfo = _albumWithInfoState.value.infoResource.data?.albumInfo,
                                    groupedTracks = _albumWithInfoState.value.infoResource.data?.groupedTracks?.mapValues { entry ->
                                        entry.value.map { track ->
                                            if (track.trackHash == trackHash) {
                                                track.copy(isFavorite = isFavorite)
                                            } else {
                                                track
                                            }
                                        }
                                    } ?: emptyMap(),
                                    copyright = _albumWithInfoState.value.infoResource.data?.copyright
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun onAlbumWithInfoUiEvent(event: AlbumWithInfoUiEvent) {
        when (event) {
            is AlbumWithInfoUiEvent.ResetState -> {
                _albumWithInfoState.value = AlbumWithInfoState()
            }

            is AlbumWithInfoUiEvent.OnUpdateAlbumHash -> {
                _albumWithInfoState.value =
                    _albumWithInfoState.value.copy(
                        albumHash = event.albumHash,
                        reloadRequired = true
                    )
            }

            is AlbumWithInfoUiEvent.OnLoadAlbumWithInfo -> {
                viewModelScope.launch {
                    _albumWithInfoState.value =
                        _albumWithInfoState.value.copy(albumHash = event.albumHash)

                    val result = albumRepository.getAlbumWithInfo(event.albumHash)
                    result.collectLatest {
                        updateAlbumInfoState(it)
                    }
                }
            }

            is AlbumWithInfoUiEvent.OnRefreshAlbumInfo -> {
                viewModelScope.launch {
                    val result =
                        albumRepository.getAlbumWithInfo(_albumWithInfoState.value.albumHash ?: "")
                    result.collectLatest {
                        updateAlbumInfoState(it)
                    }
                }
            }

            is AlbumWithInfoUiEvent.OnToggleAlbumFavorite -> {
                toggleAlbumFavorite(event.albumHash, event.isFavorite)
            }

            is AlbumWithInfoUiEvent.OnToggleAlbumTrackFavorite -> {
                toggleAlbumTrackFavorite(event.trackHash, event.favorite)
            }

            else -> {}
        }
    }
}
