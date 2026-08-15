package com.rzk.kasirpro.ui.screens.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.model.SaleStatus
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.PeriodChipRow
import com.rzk.kasirpro.ui.components.SearchField
import com.rzk.kasirpro.ui.components.StatusPill
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenSale: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbol = state.settings.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(Modifier.padding(horizontal = 16.dp)) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = stringResource(R.string.search_invoice)
                )
            }
            Spacer(Modifier.height(10.dp))
            PeriodChipRow(
                selected = state.period.preset,
                onSelect = viewModel::setPeriod
            )
            Spacer(Modifier.height(10.dp))

            if (state.sales.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = stringResource(R.string.no_transactions)
                )
            } else {
                val todayLabel = stringResource(R.string.today)
                val yesterdayLabel = stringResource(R.string.yesterday)
                val grouped = state.sales.groupBy {
                    Formatters.relativeDay(it.sale.createdAt, todayLabel, yesterdayLabel)
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (day, sales) ->
                        item(key = "h-$day") {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    day,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    Formatters.money(
                                        sales.filter { it.sale.status == SaleStatus.COMPLETED }
                                            .sumOf { it.sale.total },
                                        symbol
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(sales.size, key = { sales[it].sale.id }) { index ->
                            SaleRow(sales[index], symbol) { onOpenSale(sales[index].sale.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: SaleWithDetails, currencySymbol: String, onClick: () -> Unit) {
    val voided = sale.sale.status != SaleStatus.COMPLETED
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                sale.sale.invoiceNo,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    Formatters.time(sale.sale.createdAt),
                    sale.sale.customerName.takeIf { it.isNotBlank() },
                    stringResource(R.string.items_count, sale.itemCount)
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Formatters.money(sale.sale.total, currencySymbol),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (voided) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            if (voided) {
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    text = stringResource(
                        when (sale.sale.status) {
                            SaleStatus.VOID -> R.string.status_void
                            SaleStatus.HELD -> R.string.status_held
                            SaleStatus.REFUNDED -> R.string.status_refunded
                            else -> R.string.status_completed
                        }
                    ),
                    containerColor = MaterialTheme.kasirColors.cashOutContainer,
                    contentColor = MaterialTheme.kasirColors.onCashOutContainer
                )
            }
        }
    }
}
