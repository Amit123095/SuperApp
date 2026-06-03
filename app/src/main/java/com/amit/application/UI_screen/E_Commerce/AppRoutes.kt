package com.amit.application.UI_screen.E_Commerce

import kotlinx.serialization.Serializable

// ... existing routes

@Serializable
data class ProductDetailRoute(val productId: Int) // Notice we pass the ID directly in the route!