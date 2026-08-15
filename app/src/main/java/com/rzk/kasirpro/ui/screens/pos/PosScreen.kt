package com.rzk.kasirpro.ui.screens.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.model.ProductWithCategory
import com.rzk.kasirpro.data.model.StockStatus
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.domain.PromoEngine
import com.rzk.kasirpro.ui.components.ChipFilterRow
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.SearchField
import com.rzk.kasirpro.ui.components.StatusPill
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun PosScreen(
    scannedBarcode: String?,
    onScanConsumed: () -> Unit,
    onOpenScanner: () -> Unit,
    onCheckoutComplete: (Long) -> Unit,
    onOpenProducts: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: PosViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbol = state.settings.currencySymbol

    var showCart by rememberSaveable { mutableStateOf(false) }
    var showPayment by rememberSaveable { mutableStateOf(false) }
    var showHeld by rememberSaveable { mutableStateOf(false) }
    var editingProductId by rememberSaveable { mutableStateOf<Long?>(null) }

    // A scan hands back a barcode through the nav back stack; consume it exactly once.
    LaunchedEffect(scannedBarcode) {
        val code = scannedBarcode
        if (!code.isNullOrBlank()) {
            viewModel.onBarcodeScanned(code)
            onScanConsumed()
        }
    }

    // Events are parked in state rather than formatted inside the collector, because
    // stringResource() can only be read during composition.
    var pendingMessage by remember { mutableStateOf<PosEvent?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is PosEvent.Completed -> {
                    showPayment = false
                    showCart = false
                    onCheckoutComplete(event.saleId)
                }
                PosEvent.OrderHeld -> {
                    showCart = false
                    pendingMessage = event
                }
                else -> pendingMessage = event
            }
        }
    }

    val genericError = stringResource(R.string.something_went_wrong)
    val message: String? = when (val event = pendingMessage) {
        is PosEvent.InsufficientStock ->
            stringResource(R.string.insufficient_stock, event.available, event.productName)
        is PosEvent.ProductNotFound -> stringResource(R.string.product_not_found, event.barcode)
        PosEvent.Underpaid -> stringResource(R.string.underpaid)
        PosEvent.OrderHeld -> stringResource(R.string.order_held)
        is PosEvent.Error -> event.message.ifBlank { genericError }
        else -> null
    }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            pendingMessage = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = stringResource(R.string.search_products),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = onOpenScanner,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = stringResource(R.string.scan_barcode)
                    )
                }
                BadgedBox(
                    badge = {
                        if (state.heldOrders.isNotEmpty()) {
                            Badge { Text(state.heldOrders.size.toString()) }
                        }
                    }
                ) {
                    FilledTonalIconButton(
                        onClick = { showHeld = true },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Inbox,
                            contentDescription = stringResource(R.string.held_orders)
                        )
                    }
                }
            }

            val categoryOptions = remember(state.categories) {
                listOf<Long?>(null) + state.categories.map { it.id }
            }
            ChipFilterRow(
                options = categoryOptions,
                selected = state.selectedCategoryId,
                onSelect = viewModel::setCategory,
                label = { id ->
                    if (id == null) stringResource(R.string.all_categories)
                    else state.categories.firstOrNull { it.id == id }?.name.orEmpty()
                }
            )

            Spacer(Modifier.height(8.dp))

            if (state.products.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = if (state.query.isBlank()) stringResource(R.string.no_products)
                    else stringResource(R.string.no_matching_products),
                    message = if (state.query.isBlank()) stringResource(R.string.no_products_hint) else null,
                    actionLabel = if (state.query.isBlank()) stringResource(R.string.add_product) else null,
                    onAction = if (state.query.isBlank()) onOpenProducts else null,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp,
                        bottom = if (state.isCartEmpty) 16.dp else 96.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.products, key = { it.product.id }) { item ->
                        val promoLabel = remember(item.product.id, state.livePromos) {
                            state.livePromos.firstOrNull {
                                PromoEngine.isLiveNow(it, System.currentTimeMillis()) &&
                                    PromoEngine.appliesTo(it, item.product)
                            }?.name
                        }
                        ProductTile(
                            item = item,
                            currencySymbol = symbol,
                            promoLabel = promoLabel,
                            inCartQty = state.lines.firstOrNull { it.product.id == item.product.id }?.qty ?: 0,
                            blockOutOfStock = state.settings.blockSaleWhenOutOfStock,
                            onClick = { viewModel.addProduct(item.product) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !state.isCartEmpty,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CartSummaryBar(
                itemCount = state.totals.itemCount,
                total = state.totals.total,
                currencySymbol = symbol,
                onOpenCart = { showCart = true },
                onCharge = { showPayment = true }
            )
        }
    }

    if (showCart) {
        CartSheet(
            state = state,
            onDismiss = { showCart = false },
            onQuantityChange = viewModel::setQuantity,
            onRemove = viewModel::removeLine,
            onEditLine = { editingProductId = it },
            onOrderDiscountChange = viewModel::setOrderDiscount,
            onCustomerNameChange = viewModel::setCustomerName,
            onNoteChange = viewModel::setOrderNote,
            onClearCart = viewModel::clearCart,
            onHold = viewModel::holdOrder,
            onCharge = {
                showCart = false
                showPayment = true
            }
        )
    }

    if (showPayment) {
        PaymentSheet(
            totals = state.totals,
            settings = state.settings,
            isProcessing = state.isProcessing,
            onDismiss = { showPayment = false },
            onConfirm = viewModel::checkout
        )
    }

    if (showHeld) {
        HeldOrdersSheet(
            heldOrders = state.heldOrders,
            currencySymbol = symbol,
            onDismiss = { showHeld = false },
            onResume = {
                viewModel.resumeHeld(it)
                showHeld = false
                showCart = true
            },
            onDiscard = viewModel::discardHeld
        )
    }

    // Resolved by lookup rather than held as state: if the line is removed while the dialog
    // is open the dialog simply stops composing, instead of writing state mid-composition.
    val editingLine = editingProductId?.let { id ->
        state.lines.firstOrNull { it.product.id == id }
    }
    if (editingLine != null) {
        LineEditDialog(
            line = editingLine,
            currencySymbol = symbol,
            onDismiss = { editingProductId = null },
            onApply = { discount, note ->
                viewModel.setLineDiscount(editingLine.product.id, discount)
                viewModel.setLineNote(editingLine.product.id, note)
                editingProductId = null
            }
        )
    }
}

@Composable
private fun ProductTile(
    item: ProductWithCategory,
    currencySymbol: String,
    promoLabel: String?,
    inCartQty: Int,
    blockOutOfStock: Boolean,
    onClick: () -> Unit
) {
    val product = item.product
    val soldOut = blockOutOfStock && item.stockStatus == StockStatus.OUT_OF_STOCK
    val accent = item.categoryColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxWidth()
            .background(
                if (inCartQty > 0) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = !soldOut, onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 26.dp, height = 5.dp)
                        .background(accent, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.weight(1f))
                if (promoLabel != null) {
                    StatusPill(
                        text = stringResource(R.string.promo_badge),
                        containerColor = MaterialTheme.kasirColors.promoContainer,
                        contentColor = MaterialTheme.kasirColors.onPromoContainer
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Formatters.money(product.sellPrice, currencySymbol),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StockPill(item)
                Spacer(Modifier.weight(1f))
                if (inCartQty > 0) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            inCartQty.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockPill(item: ProductWithCategory) {
    val colors = MaterialTheme.kasirColors
    when (item.stockStatus) {
        StockStatus.OUT_OF_STOCK -> StatusPill(
            text = stringResource(R.string.out_of_stock),
            containerColor = colors.cashOutContainer,
            contentColor = colors.onCashOutContainer
        )
        StockStatus.LOW -> StatusPill(
            text = stringResource(R.string.stock_left, item.product.stock),
            containerColor = colors.warningContainer,
            contentColor = colors.onWarningContainer
        )
        StockStatus.HEALTHY -> Text(
            stringResource(R.string.stock_left, item.product.stock),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StockStatus.UNTRACKED -> Text(
            item.product.unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Sticky bottom bar: the running total plus the one button that matters. */
@Composable
private fun CartSummaryBar(
    itemCount: Int,
    total: Long,
    currencySymbol: String,
    onOpenCart: () -> Unit,
    onCharge: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenCart)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.ShoppingCart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.inverseOnSurface
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.cart_items, itemCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.75f)
            )
            Text(
                Formatters.money(total, currencySymbol),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
        Button(
            onClick = onCharge,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.charge), fontWeight = FontWeight.Bold)
        }
    }
}
