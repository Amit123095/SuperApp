package com.amit.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.amit.application.Routes.AppRoutes
import com.amit.application.UI_screen.Chat.ChatScreen.ChatMessage
import com.amit.application.UI_screen.Chat.ChatScreen.ChatScreen
import com.amit.application.UI_screen.Chat.ChatScreen.MessageStatus
import com.amit.application.UI_screen.Chat.ContactList.ContactsScreen
import com.amit.application.UI_screen.DashboardScreen
import com.amit.application.UI_screen.E_Commerce.EcommerceScreen
import com.amit.application.UI_screen.E_Commerce.ProductDetailRoute
import com.amit.application.UI_screen.E_Commerce.ProductDetailScreen
import com.amit.application.UI_screen.Login.LoginScreen
import com.amit.application.UI_screen.Profile.ProfileScreen
import com.amit.application.UI_screen.VideoPlayer.AdvancedVideoListScreen
import com.amit.application.UI_screen.VideoPlayer.AdvancedVideoPlayerScreen
import com.amit.application.UI_screen.Wallet.WalletScreen
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Apply your Compose Theme here (e.g., SuperAppTheme { })
            SuperAppNavigation()
        }
    }
}

@Composable
fun SuperAppNavigation() {
    val navController = rememberNavController()

    // 1. Determine the start route BEFORE initializing the NavHost.
    val startRoute: Any = if (FirebaseAuth.getInstance().currentUser != null) {
        AppRoutes.DashboardRoute
    } else {
        AppRoutes.LoginRoute
    }

    // 2. Create EXACTLY ONE NavHost
    NavHost(navController = navController, startDestination = startRoute) {

        composable<AppRoutes.LoginRoute> {
            LoginScreen(
                onNavigateToDashboard = {
                    navController.navigate(AppRoutes.DashboardRoute) {
                        popUpTo<AppRoutes.LoginRoute> { inclusive = true }
                    }
                }
            )
        }

// on click back form chat screen to chat_contact_list screen
        composable<AppRoutes.ChatRoomRoute> { backStackEntry ->
            val chatArgs: AppRoutes.ChatRoomRoute = backStackEntry.toRoute()
            val messages = remember { androidx.compose.runtime.mutableStateListOf<ChatMessage>() }

            ChatScreen(
                messages = messages,
                onSendMessage = { text, attachmentType, attachmentUri, repliedId, repliedText ->
                    messages.add(
                        0,
                        ChatMessage(
                            id = java.util.UUID.randomUUID().toString(),
                            text = text,
                            timestamp = System.currentTimeMillis(),
                            isFromMe = true,
                            status = MessageStatus.PENDING,
                            attachmentType = attachmentType,
                            attachmentUri = attachmentUri,
                            repliedToId = repliedId,     // Maps metadata
                            repliedToText = repliedText  // Maps string text
                        )
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.DashboardRoute> {
            DashboardScreen(
                onNavigateToProfile = { navController.navigate(AppRoutes.ProfileRoute) },
                onNavigateToEcommerce = { navController.navigate(AppRoutes.EcommerceRoute) },
                onNavigateToChat = { navController.navigate(AppRoutes.ContactListRoute) },
                onNavigateToVideo = { navController.navigate(AppRoutes.LocalVideoRoute) },
                onNavigateToWallet = { navController.navigate(AppRoutes.WalletRoute) }
            )
        }


//      1. The Local Video Grid/List Screen
        composable<AppRoutes.LocalVideoRoute> {
            AdvancedVideoListScreen(
                onVideoClick = { rawUri ->
                    // Crucial Step: We MUST encode the URI string (which contains slashes)
                    // so it doesn't break the navigation route path.
                    val encoded = URLEncoder.encode(rawUri, StandardCharsets.UTF_8.toString())
                    navController.navigate(AppRoutes.PlayerRoute(encodedUri = encoded))
                },
                // ADD THIS LINE HERE:
                onBackClick = { navController.popBackStack() }
            )
        }

        // 2. The Advanced Video Player Screen
        composable<AppRoutes.PlayerRoute> { backStackEntry ->
            // Extract the encoded argument
            val routeData: AppRoutes.PlayerRoute = backStackEntry.toRoute()

            // Decodes the URI back to normal before passing it to the Media3 player
            val decodedUri =
                URLDecoder.decode(routeData.encodedUri, StandardCharsets.UTF_8.toString())

            AdvancedVideoPlayerScreen(
                videoUriString = decodedUri,
                // ADD THIS LINE HERE TOO:
                onBackClick = { navController.popBackStack() }
            )
        }

//  To navigate to the profile screen and on back click it will navigate to the dashboard screen and on logout click it will navigate to the login screen
        composable<AppRoutes.ProfileRoute> {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    // Sign out of Firebase
                    FirebaseAuth.getInstance().signOut()

                    // Navigate back to Login and safely clear the ENTIRE navigation history
                    navController.navigate(AppRoutes.LoginRoute) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                }
            )
        }

//  Sync the contact list screen with the contacts
        composable<AppRoutes.ContactListRoute> {
            ContactsScreen(
                onBackClick = { navController.popBackStack() },
                onUserClick = { friendNumber, friendName ->
                    navController.navigate(
                        AppRoutes.ChatRoomRoute(
                            friendNumber = friendNumber,
                            friendName = friendName
                        )
                    )
                }
            )
        }


//  To navigate to the ecommerce screen
        composable<AppRoutes.EcommerceRoute> {
            EcommerceScreen(
                onProductClick = { productId ->
                    navController.navigate(ProductDetailRoute(productId))
                }
            )
        }
//  To navigate to the product detail screen and on back click it will navigate to the ecommerce screen product list
        composable<ProductDetailRoute> { backStackEntry ->
            val detailRoute: ProductDetailRoute = backStackEntry.toRoute()

            ProductDetailScreen(
                productId = detailRoute.productId,
                onBackClick = { navController.popBackStack() }
            )
        }

//  To navigate to the wallet screen
        composable<AppRoutes.WalletRoute> {
            WalletScreen()
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title)
    }
}