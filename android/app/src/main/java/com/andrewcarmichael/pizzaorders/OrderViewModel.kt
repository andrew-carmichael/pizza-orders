package com.andrewcarmichael.pizzaorders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class OrderViewModel : ViewModel() {

    private val client by lazy {
        HttpClient(CIO) {
            install(WebSockets) {
                pingInterval = 20.seconds
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    private val _ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    val ordersFlow: StateFlow<List<Order>> = _ordersFlow

    init {
        observePizzaOrdersWebsocket()
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }

    private var socketSession: DefaultClientWebSocketSession? = null

    private fun observePizzaOrdersWebsocket() {
        val orderMap = mutableMapOf<String, Order>()
        viewModelScope.launch {
            client.webSocket("wss://goldfish-app-y7pye.ondigitalocean.app/ws") {
                socketSession = this
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val json = frame.readText()
                        val order = Json.decodeFromString<Order>(json)
                        orderMap[order.id] = order
                        _ordersFlow.value = orderMap.values.sortedBy { it.timestamp }
                        if (order.status == "done") {
                            launch {
                                orderMap.remove(order.id)
                                _ordersFlow.value = orderMap.values.sortedBy { it.timestamp }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onCompleteOrder(order: Order) {
        viewModelScope.launch {
            val session = socketSession
            if (session != null && session.isActive) {
                val completeMessage = buildJsonObject {
                    put("action", JsonPrimitive("complete"))
                    put("orderId", JsonPrimitive(order.id))
                }
                session.send(Frame.Text(completeMessage.toString()))
            } else {
                Log.w("OrderViewModel", "WebSocket session is not active")
            }

            _ordersFlow.update { list ->
                list.map {
                    if (it.id == order.id) it.copy(status = "done") else it
                }
            }
        }
    }
}
