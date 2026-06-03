package com.amit.application.Routes
import kotlinx.serialization.Serializable

class AppRoutes {

    @Serializable
    object DashboardRoute

    @Serializable
    object EcommerceRoute

    /*@Serializable
    object ChatRoute*/
    @Serializable
    data class ChatRoomRoute(
        val friendNumber: String,
        val friendName: String
    )

    @Serializable
    object VideoPlayerRoute

    @Serializable
    object WalletRoute

    @Serializable
    object LoginRoute

    @Serializable
    object ProfileRoute

    @Serializable
    object ContactListRoute

    @Serializable
    object LocalVideoRoute

    @Serializable
    data class PlayerRoute(val encodedUri: String) // Passing the URI here

}