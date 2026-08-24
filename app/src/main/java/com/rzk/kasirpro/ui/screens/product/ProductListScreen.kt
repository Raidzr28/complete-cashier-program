package com.rzk.kasirpro.ui.screens.product

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.rzk.kasirpro.ui.components.ChipFilterRow
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.SearchField
import com.rzk.kasirpro.ui.components.StaggeredEntrance
import com.rzk.kasirpro.ui.components.StatCard
import com.rzk.kasirpro.ui.components.StatusPill
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenMovements: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ProductListViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbol = state.settings.currencySymbol
    var stockSheetProduct by remember { mutableStateOf<ProductWithCategory?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val updatedMessage = stringResource(R.string.stock_updated)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                StockEvent.StockUpdated -> snackbarHostState.showSnackbar(updatedMessage)
                is StockEvent.Error -> errorMessage = event.message
            }
        }
    }
    val genericError = stringResource(R.string.something_went_wrong)
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it.ifBlank { genericError })
            errorMessage = null
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_product)) }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.products_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenCategories) {
                    Icon(
                        Icons.Filled.Category,
                        contentDescription = stringResource(R.string.categories_title)
                    )
                }
                IconButton(onClick = onOpenMovements) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = stringResource(R.string.movement_history)
                    )
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = stringResource(R.string.stock_value),
                            value = Formatters.compactMoney(state.valuation.costValue, symbol),
                            supporting = stringResource(
                                R.string.items_count,
                                state.valuation.productCount
                            ),
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.potential_profit),
                            value = Formatters.compactMoney(
                                state.valuation.potentialProfit,
                                symbol
                            ),
                            accent = MaterialTheme.kasirColors.cashIn,
                            supporting = if (state.lowStockCount > 0) {
                                stringResource(R.string.low_stock_items, state.lowStockCount)
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    SearchField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = stringResource(R.string.search_products)
                    )
                }

                item {
                    val options = remember(state.categories) {
                        listOf<Long?>(null) + state.categories.map { it.id }
                    }
                    ChipFilterRow(
                        options = options,
                        selected = state.selectedCategoryId,
                        onSelect = viewModel::setCategory,
                        label = { id ->
                            if (id == null) stringResource(R.string.all_categories)
                            else state.categories.firstOrNull { it.id == id }?.name.orEmpty()
                        },
                        contentPadding = PaddingValues(0.dp)
                    )
                }

                item {
                    FilterChip(
                        selected = state.showArchived,
                        onClick = viewModel::toggleArchived,
                        label = { Text(stringResource(R.string.show_archived)) }
                    )
                }

                if (state.products.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Inventory2,
                            title = stringResource(R.string.no_products),
                            message = stringResource(R.string.no_products_hint),
                            actionLabel = stringResource(R.string.add_product),
                            onAction = onAddProduct
                        )
                    }
                } else {
                    itemsIndexed(state.products, key = { _, item -> item.product.id }) { index, item ->
                        StaggeredEntrance(index) {
                            ProductRow(
                                item = item,
                                currencySymbol = symbol,
                                onClick = { onEditProduct(item.product.id) },
                                onStockAction = { stockSheetProduct = item }
                            )
                        }
                    }
                }
            }
        }
    }

    stockSheetProduct?.let { item ->
        StockActionSheet(
            item = item,
            currencySymbol = symbol,
            onDismiss = { stockSheetProduct = null },
            onStockIn = { qty, cost, note, updateCost, payCash ->
                viewModel.stockIn(item.product.id, qty, cost, note, updateCost, payCash)
                stockSheetProduct = null
            },
            onAdjust = { counted, reason ->
                viewModel.adjustStock(item.product.id, counted, reason)
                stockSheetProduct = null
            },
            onWriteOff = { qty, reason ->
                viewModel.writeOff(item.product.id, qty, reason)
                stockSheetProduct = null
            }
        )
    }
}

@Composable
private fun ProductRow(
    item: ProductWithCategory,
    currencySymbol: String,
    onClick: () -> Unit,
    onStockAction: () -> Unit
) {
    val product = item.product
    val accent = item.categoryColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    KasirCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(accent.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    product.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!product.isActive) {
                        Spacer(Modifier.width(6.dp))
                        StatusPill(
                            text = stringResource(R.string.archived),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    listOfNotNull(
                        item.categoryName ?: stringResource(R.string.uncategorised),
                        product.sku.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Formatters.money(product.sellPrice, currencySymbol),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${stringResource(R.string.margin)} ${Formatters.percent(item.marginPercent, 0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StockBadge(item)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onStockAction)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        stringResource(R.string.stock_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StockBadge(item: ProductWithCategory) {
    val colors = MaterialTheme.kasirColors
    when (item.stockStatus) {
        StockStatus.OUT_OF_STOCK -> StatusPill(
            text = stringResource(R.string.out_of_stock),
            containerColor = colors.cashOutContainer,
            contentColor = colors.onCashOutContainer
        )
        StockStatus.LOW -> StatusPill(
            text = "${item.product.stock} ${item.product.unit}",
            containerColor = colors.warningContainer,
            contentColor = colors.onWarningContainer
        )
        StockStatus.HEALTHY -> StatusPill(
            text = "${item.product.stock} ${item.product.unit}",
            containerColor = colors.cashInContainer,
            contentColor = colors.onCashInContainer
        )
        StockStatus.UNTRACKED -> StatusPill(
            text = stringResource(R.string.untracked_stock),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
