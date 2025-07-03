package com.android.swingmusic.album.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.android.swingmusic.core.data.mapper.Map.toAlbum
import com.android.swingmusic.core.domain.model.Album
import com.android.swingmusic.core.domain.util.CustomPagingException
import com.android.swingmusic.network.data.api.service.NetworkApiService
import retrofit2.HttpException
import java.io.IOException

class AlbumsPagingSource(
    private val api: NetworkApiService,
    private val sortBy: String,
    private val sortOrder: Int,
    private val baseUrl: String,
    private val accessToken: String
) : PagingSource<Int, Album>() {

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
        return try {
            val nextPageNumber = params.key ?: 0
            val startIndex = nextPageNumber * params.loadSize

            val allAlbumsDto = api.getAllAlbums(
                url = baseUrl,
                bearerToken = accessToken,
                startIndex = startIndex,
                sortBy = sortBy,
                sortOrder = sortOrder
            )
            LoadResult.Page(
                data = allAlbumsDto.albumDto?.map { it.toAlbum() } ?: emptyList(),
                prevKey = if (nextPageNumber == 0) null else nextPageNumber - 1,
                nextKey = if (allAlbumsDto.albumDto?.isEmpty() == true) null else nextPageNumber + 1
            )
        } catch (e: IOException) {
            LoadResult.Error(CustomPagingException("Network connection failed. Please check your internet connection.", e))
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                404 -> "Albums not found."
                401 -> "Authentication failed. Please log in again."
                403 -> "Access denied to albums."
                500 -> "Server error. Please try again later."
                else -> "Failed to load albums. Please try again."
            }
            LoadResult.Error(CustomPagingException(errorMessage, e))
        } catch (e: Exception) {
            LoadResult.Error(CustomPagingException("An unexpected error occurred while loading albums.", e))
        }
    }
}
