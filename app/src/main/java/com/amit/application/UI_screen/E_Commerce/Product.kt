package com.amit.application.UI_screen.E_Commerce

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val description: String
)

val dummyProducts = listOf(
    Product(1, "Wireless Headphones", 99.99, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e", "High-quality noise-canceling headphones."),
    Product(2, "Smart Watch", 199.99, "https://images.unsplash.com/photo-1523275335684-37898b6baf30", "Track your fitness and notifications."),
    Product(3, "Mechanical Keyboard", 129.50, "https://images.unsplash.com/photo-1595225476474-87563907a212", "Clicky switches for the best typing experience."),
    Product(4, "Coffee Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cup every morning."),
    Product(5, "tea Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect mug every morning."),
    Product(6, "soup Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect jug every morning."),
    Product(7, "greavy Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect ketel every morning."),
    Product(8, "Capichino Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect tea every morning."),
    Product(9, "Goat Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect hat every morning."),
    Product(11, "Chicken Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cap every morning."),
    Product(12, "Cabbage Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect goat every morning."),
    Product(13, "rice Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect chicken every morning."),
    Product(14, "chapati Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cup every morning."),
    Product(15, "veggi Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cup every morning."),
    Product(16, "egg Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cup every morning."),
    Product(17, "water Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cup every morning."),
    Product(18, "pork Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cup every morning."),
    Product(19, "mutton Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cup every morning."),
    Product(20, "juice Maker", 85.00, "https://images.unsplash.com/photo-1517686469429-8bdb88b9f907", "Brew the perfect cup every morning.")
)
