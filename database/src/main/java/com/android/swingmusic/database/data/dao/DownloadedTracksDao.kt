package com.android.swingmusic.database.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.android.swingmusic.database.data.entity.DownloadTrackEntity

@Dao
interface DownloadedTracksDao {


    @Transaction
    suspend fun insertQueueInTransaction(track: DownloadTrackEntity) {
        // Remove any existing track with the same hash
        clearDownloadByHash(track.trackHash)
        // Insert the new track
        insertDownload(track)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(track: DownloadTrackEntity)

    @Query("SELECT * FROM downloaded_track")
    suspend fun getDownloads(): List<DownloadTrackEntity>

    @Query("DELETE FROM downloaded_track")
    suspend fun clearAllDownloads()

    @Transaction
    suspend fun clearDownload(track: DownloadTrackEntity) {
        clearDownloadByHash(track.trackHash)
    }

    @Query("DELETE FROM downloaded_track WHERE trackHash = :trackHash")
    suspend fun clearDownloadByHash(trackHash: String)


}
