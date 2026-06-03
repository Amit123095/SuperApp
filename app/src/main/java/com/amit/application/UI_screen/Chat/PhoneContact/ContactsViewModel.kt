package com.amit.application.UI_screen.Chat.PhoneContact

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ContactsUiState {
    object Initial : ContactsUiState()
    object Loading : ContactsUiState()
    data class Success(val friends: List<AppUser>) : ContactsUiState()
}

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactSyncRepository: ContactSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Initial)
    val uiState = _uiState.asStateFlow()

    fun syncContacts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ContactsUiState.Loading
            val friends = contactSyncRepository.findFriendsOnApp(context)
            _uiState.value = ContactsUiState.Success(friends)
        }
    }
}