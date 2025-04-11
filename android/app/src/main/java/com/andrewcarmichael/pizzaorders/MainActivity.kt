package com.andrewcarmichael.pizzaorders

import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrewcarmichael.pizzaorders.ui.theme.PizzaOrdersTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val isTvDevice by lazy {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        val isTvByUiModeManager = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val isTvByConfiguration = (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
        val isTvByLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        isTvByUiModeManager || isTvByConfiguration || isTvByLeanback
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        if (isTvDevice) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
        }
        setContent {
            PizzaOrdersApplication(
                isTvDevice = isTvDevice,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PizzaOrdersApplication(
    isTvDevice: Boolean,
    modifier: Modifier = Modifier
) {
    RealtimeOrdersRoot(
        isTvDevice = isTvDevice,
        modifier = modifier
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RealtimeOrdersRoot(
    isTvDevice: Boolean,
    modifier: Modifier = Modifier,
    viewModel: OrderViewModel = viewModel(),
) {
    val ordersState by viewModel.ordersFlow.collectAsState()
    if (isTvDevice) {
        RealtimeOrdersForTv(
            orders = ordersState,
            onCompleteOrder = { order ->
                viewModel.onCompleteOrder(order)
            },
            modifier = modifier
        )
    } else {
        RealtimeOrdersForMobile(
            orders = ordersState,
            onCompleteOrder = { order ->
                viewModel.onCompleteOrder(order)
            },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RealtimeOrdersForTv(
    orders: List<Order>,
    onCompleteOrder: (Order) -> Unit,
    modifier: Modifier = Modifier,
) {
    PizzaOrdersTheme {
        Column(modifier = modifier.padding(16.dp)) {
            OrderRow(
                pizza = "Pizza",
                customerName = "Customer",
                address = "Address",
                phoneNumber = "Phone",
                status = "Status",
                timestamp = "Time",
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusable()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().focusable()
                ) {
                    items(
                        count = orders.size,
                        key = { orders[it].id },
                    ) { index ->
                        val order = orders[index]
                        OrderRow(
                            pizza = order.pizza,
                            customerName = order.customerName,
                            address = order.address,
                            phoneNumber = order.phoneNumber,
                            status = order.status,
                            timestamp = order.timestamp,
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .dpadFocusable(
                                    onClick = { onCompleteOrder(order) },
                                )
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OrderRow(
    pizza: String,
    customerName: String,
    address: String,
    phoneNumber: String,
    status: String,
    timestamp: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .drawBehind {
                if (status == "done") {
                    drawRect(color = Color.Green)
                }
            }
    ) {
        Text(
            text = pizza,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = customerName,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = address,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = phoneNumber,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = status,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = formatTimestamp(timestamp),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
    }
}

// I did not write this. The d-pad isn't easy to work with.
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
fun Modifier.dpadFocusable(
    onClick: () -> Unit,
    borderWidth: Dp = 4.dp,
    unfocusedBorderColor: Color = Color(0x00f39c12),
    focusedBorderColor: Color = Color(0xfff39c12),
    indication: Indication? = null,
    scrollPadding: Rect = Rect.Zero,
    isDefault: Boolean = false
) = composed {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val boxInteractionSource = remember { MutableInteractionSource() }
    val isItemFocused by boxInteractionSource.collectIsFocusedAsState()
    val animatedBorderColor by animateColorAsState(
        targetValue =
            if (isItemFocused) focusedBorderColor
            else unfocusedBorderColor
    )
    var previousFocus: FocusInteraction.Focus? by remember {
        mutableStateOf(null)
    }
    var previousPress: PressInteraction.Press? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    var boxSize by remember {
        mutableStateOf(IntSize(0, 0))
    }
    val inputMode = LocalInputModeManager.current

    LaunchedEffect(inputMode.inputMode) {
        when (inputMode.inputMode) {
            InputMode.Keyboard -> {
                Log.d("dpadFocusable", "keyboard")
                if (isDefault) {
                    focusRequester.requestFocus()
                }
            }
            InputMode.Touch -> {
                Log.d("dpadFocusable", "touch")
            }
        }
    }
    LaunchedEffect(isItemFocused) {
        previousPress?.let {
            if (!isItemFocused) {
                boxInteractionSource.emit(
                    PressInteraction.Release(
                        press = it
                    )
                )
            }
        }
    }

    if (inputMode.inputMode == InputMode.Touch)
        this.clickable(
            interactionSource = boxInteractionSource,
            indication = indication ?: ripple()
        ) {
            onClick()
        }
    else
        this
            .bringIntoViewRequester(bringIntoViewRequester)
            .onSizeChanged {
                boxSize = it
            }
            .indication(
                interactionSource = boxInteractionSource,
                indication = indication ?: ripple()
            )
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    val newFocusInteraction = FocusInteraction.Focus()
                    scope.launch {
                        boxInteractionSource.emit(newFocusInteraction)
                    }
                    scope.launch {
                        val visibilityBounds = Rect(
                            left = -1f * scrollPadding.left,
                            top = -1f * scrollPadding.top,
                            right = boxSize.width + scrollPadding.right,
                            bottom = boxSize.height + scrollPadding.bottom
                        )
                        bringIntoViewRequester.bringIntoView(visibilityBounds)
                    }
                    previousFocus = newFocusInteraction
                } else {
                    previousFocus?.let {
                        scope.launch {
                            boxInteractionSource.emit(FocusInteraction.Unfocus(it))
                        }
                    }
                }
            }
            .onKeyEvent {
                if (!listOf(Key.DirectionCenter, Key.Enter).contains(it.key)) {
                    return@onKeyEvent false
                }
                when (it.type) {
                    KeyEventType.KeyDown -> {
                        val press =
                            PressInteraction.Press(
                                pressPosition = Offset(x = boxSize.width / 2f, y = boxSize.height / 2f)
                            )
                        scope.launch {
                            boxInteractionSource.emit(press)
                        }
                        previousPress = press
                        true
                    }
                    KeyEventType.KeyUp -> {
                        previousPress?.let { previousPress ->
                            onClick()
                            scope.launch {
                                boxInteractionSource.emit(
                                    PressInteraction.Release(
                                        press = previousPress
                                    )
                                )
                            }
                        }
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
            .focusRequester(focusRequester)
            .focusTarget()
            .border(
                width = borderWidth,
                color = animatedBorderColor
            )
}

@Composable
private fun RealtimeOrdersForMobile(
    orders: List<Order>,
    onCompleteOrder: (Order) -> Unit,
    modifier: Modifier = Modifier,
) {
    PizzaOrdersTheme {
        LazyColumn(
            modifier = modifier.padding(16.dp)
        ) {
            items(
                count = orders.size,
                key = { orders[it].id },
            ) { index ->
                val order = orders[index]
                SwipeToCompleteOrderCard(
                    order = order,
                    onComplete = { onCompleteOrder(order) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToCompleteOrderCard(
    order: Order,
    onComplete: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart && order.status.lowercase() != "done") {
                onComplete(order)
                true
            } else false
        }
    )

    val shape = RoundedCornerShape(12.dp)

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp), // padding applied here
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete Order",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .size(24.dp)
                )
            }
        }
    ) {
        OrderCard(
            order = order,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Order #${order.id}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text("Pizza: ${order.pizza}", style = MaterialTheme.typography.bodyMedium)
            Text("Customer: ${order.customerName}", style = MaterialTheme.typography.bodyMedium)
            Text("Phone: ${order.phoneNumber}", style = MaterialTheme.typography.bodySmall)
            Text("Address: ${order.address}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (order.status.lowercase()) {
                    "completed" -> Color(0xFF388E3C) // Green
                    "in progress" -> Color(0xFFFFA000) // Amber
                    else -> Color.Gray
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.status,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = order.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        // In case of parsing issues, return the original timestamp
        timestamp
    }
}