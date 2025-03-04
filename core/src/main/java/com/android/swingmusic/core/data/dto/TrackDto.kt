package com.android.swingmusic.core.data.dto


import com.google.gson.annotations.SerializedName

data class TrackDto(
    @SerializedName("album")
    val album: String?,
    @SerializedName("albumartists")
    val albumTrackArtistDto: List<TrackArtistDto>?,
    @SerializedName("albumhash")
    val albumHash: String?,
    @SerializedName("artists")
    val artistsDto: List<TrackArtistDto>?,
    @SerializedName("bitrate")
    val bitrate: Int?,
    @SerializedName("duration")
    val duration: Int?,
    @SerializedName("filepath")
    val filepath: String?,
    @SerializedName("folder")
    val folder: String?,
    @SerializedName("image")
    val image: String?,
    @SerializedName("is_favorite")
    val isFavorite: Boolean?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("trackhash")
    val trackHash: String?,
    @SerializedName("disc")
    val disc: Int?,
    @SerializedName("track")
    val trackNumber: Int?
)
