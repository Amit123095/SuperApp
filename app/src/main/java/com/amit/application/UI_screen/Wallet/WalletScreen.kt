package com.amit.application.UI_screen.Wallet

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WalletScreen(
    // Hilt automatically injects the ViewModel here!
    viewModel: WalletViewModel = hiltViewModel()
) {
    // 1. Collect the state in a lifecycle-aware way
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Total Balance", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Animate the balance whenever it changes
        AnimatedContent(
            targetState = uiState.balance,
            transitionSpec = {
                // The new number slides up and fades in, the old one slides up and fades out
                (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
            },
            label = "balance_animation"
        ) { targetBalance ->
            Text(
                text = "$${"%.2f".format(targetBalance)}", // Format to 2 decimal places
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 3. Trigger events via the ViewModel
        Button(
            onClick = { viewModel.addFunds(15.50) },
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
        ) {
            Text("Claim Reward (+ $15.50)")
        }
    }
}