package com.android.swingmusic.database.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_track")
data class DownloadTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val trackHash: String,
    val album: String,
    val albumHash: String,
    val bitrate: Int,
    val duration: Int,
    val filepath: String,
    val folder: String,
    val image: String,
    val isFavorite: Boolean,
    val title: String,
    val albumTrackArtists: List<TrackArtistEntity>,
    val trackArtists: List<TrackArtistEntity>,
    val disc: Int,
    val trackNumber: Int
)
