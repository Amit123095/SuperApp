package com.amit.application.UI_screen.VideoPlayer

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri

data class LocalVideo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val duration: Long
)

class VideoPagingSource(private val context: Context) : PagingSource<Int, LocalVideo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LocalVideo> = withContext(Dispatchers.IO) {
        try {
            val pageNumber = params.key ?: 0
            val limit = params.loadSize
            val offset = pageNumber * limit

            val videoList = mutableListOf<LocalVideo>()
            val resolver: ContentResolver = context.contentResolver

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            )

            // Safe pagination across all Android versions
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Video.Media.DATE_ADDED))
                    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                }
                resolver.query(collection, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
                resolver.query(collection, projection, null, null, sortOrder)
            }

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    videoList.add(
                        LocalVideo(
                            id = id,
                            uri = contentUri,
                            name = it.getString(nameColumn) ?: "Unknown",
                            duration = it.getLong(durationColumn)
                        )
                    )
                }
            }

            // Tell Paging3 if we have reached the end of the 10k list
            val nextKey = if (videoList.isEmpty()) null else pageNumber + 1
            val prevKey = if (pageNumber == 0) null else pageNumber - 1

            LoadResult.Page(
                data = videoList,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, LocalVideo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}