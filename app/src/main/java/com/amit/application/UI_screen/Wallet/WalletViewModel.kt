package com.amit.application.UI_screen.Wallet

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// 1. The State Object (Immutable)
data class WalletUiState(
    val balance: Double = 0.00
)

// 2. The ViewModel
@HiltViewModel
class WalletViewModel @Inject constructor() : ViewModel() {

    // Internal mutable state
    private val _uiState = MutableStateFlow(WalletUiState(balance = 100.00)) // Starting balance

    // Public immutable state exposed to Compose
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    // 3. The Event (Action triggered by the UI)
    fun addFunds(amount: Double) {
        _uiState.update { currentState ->
            currentState.copy(
                balance = currentState.balance + amount
            )
        }
    }
}