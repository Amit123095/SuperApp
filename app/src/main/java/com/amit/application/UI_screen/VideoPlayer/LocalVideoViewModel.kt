package com.amit.application.UI_screen.VideoPlayer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class LocalVideoViewModel @Inject constructor() : ViewModel() {

    // Loads 50 items initially, then 50 more each time the user hits the bottom
    fun getPagedVideos(context: Context): Flow<PagingData<LocalVideo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                initialLoadSize = 50
            ),
            pagingSourceFactory = { VideoPagingSource(context) }
        ).flow.cachedIn(viewModelScope) // Caches data so it survives device rotation
    }
}