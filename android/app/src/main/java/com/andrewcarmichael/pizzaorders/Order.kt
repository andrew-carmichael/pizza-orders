package com.andrewcarmichael.pizzaorders

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val pizza: String,
    val customerName: String,
    val address: String,
    val phoneNumber: String,
    val status: String,
    val timestamp: String
)
