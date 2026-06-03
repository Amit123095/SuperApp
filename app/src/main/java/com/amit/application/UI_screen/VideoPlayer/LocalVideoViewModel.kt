package com.amit.application.UI_screen.VideoPlayer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalVideoViewModel @Inject constructor(
    private val repository: LocalVideoRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())
    val videos = _videos.asStateFlow()

    fun loadVideos(context: Context) {
        viewModelScope.launch {
            _videos.value = repository.getAllLocalVideos(context)
        }
    }
}