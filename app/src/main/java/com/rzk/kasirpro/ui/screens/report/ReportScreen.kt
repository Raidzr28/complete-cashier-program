package com.rzk.kasirpro.ui.screens.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.BarChart
import com.rzk.kasirpro.ui.components.BarDatum
import com.rzk.kasirpro.ui.components.DetailRow
import com.rzk.kasirpro.ui.components.DonutChart
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.PeriodChipRow
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.SliceDatum
import com.rzk.kasirpro.ui.components.StatCard
import com.rzk.kasirpro.ui.screens.pos.paymentMethodLabel
import com.rzk.kasirpro.ui.theme.CategoricalChartPalette
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun ReportScreen(
    onOpenStatistics: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenShift: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbol = state.settings.currencySymbol

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                stringResource(R.string.reports_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            PeriodChipRow(
                selected = state.period.preset,
                onSelect = viewModel::setPeriod,
                contentPadding = PaddingValues(0.dp)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = stringResource(R.string.net_sales),
                    value = Formatters.compactMoney(state.summary.net, symbol),
                    icon = Icons.Filled.ShoppingBag,
                    deltaPercent = state.revenueDelta,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.gross_profit),
                    value = Formatters.compactMoney(state.summary.profit, symbol),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accent = MaterialTheme.kasirColors.cashIn,
                    deltaPercent = state.profitDelta,
                    supporting = Formatters.percent(state.summary.marginPercent, 1),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = stringResource(R.string.orders_count),
                    value = state.summary.orders.toString(),
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    accent = MaterialTheme.colorScheme.tertiary,
                    deltaPercent = state.orderDelta,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.average_order),
                    value = Formatters.compactMoney(state.summary.averageOrderValue, symbol),
                    icon = Icons.Filled.Payments,
                    accent = MaterialTheme.kasirColors.warning,
                    supporting = stringResource(R.string.items_count, state.summary.itemsSold),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            KasirCard {
                SectionHeader(title = stringResource(R.string.net_sales))
                Spacer(Modifier.height(10.dp))
                BarChart(
                    data = viewModel.dayKeys().takeLast(14).map { key ->
                        val row = state.dailyTotals.firstOrNull { it.dayKey == key }
                        BarDatum(
                            label = Formatters.dayKeyToShort(key),
                            value = row?.total ?: 0L,
                            display = Formatters.compactMoney(row?.total ?: 0L, symbol)
                        )
                    },
                    emptyLabel = stringResource(R.string.no_data)
                )
            }
        }

        item {
            KasirCard {
                SectionHeader(title = stringResource(R.string.gross_sales))
                Spacer(Modifier.height(8.dp))
                DetailRow(
                    stringResource(R.string.gross_sales),
                    Formatters.money(state.summary.gross, symbol)
                )
                DetailRow(
                    stringResource(R.string.discount),
                    "− ${Formatters.money(state.summary.discount, symbol)}",
                    valueColor = MaterialTheme.kasirColors.cashOut
                )
                DetailRow(stringResource(R.string.tax), Formatters.money(state.summary.tax, symbol))
                DetailRow(
                    stringResource(R.string.cost),
                    Formatters.money(state.summary.cost, symbol)
                )
                DetailRow(
                    stringResource(R.string.net_sales),
                    Formatters.money(state.summary.net, symbol),
                    emphasise = true
                )
            }
        }

        if (state.payments.isNotEmpty()) {
            item {
                KasirCard {
                    SectionHeader(title = stringResource(R.string.payment_methods))
                    Spacer(Modifier.height(12.dp))
                    DonutChart(
                        slices = state.payments.mapIndexed { index, row ->
                            SliceDatum(
                                label = paymentMethodLabel(row.method),
                                value = row.total,
                                display = Formatters.compactMoney(row.total, symbol),
                                // Colour by the method's own position in the enum, so a
                                // filter that hides one method never repaints the others.
                                color = CategoricalChartPalette[
                                    row.method.ordinal % CategoricalChartPalette.size
                                ]
                            )
                        },
                        centerLabel = stringResource(R.string.orders_count),
                        centerValue = state.summary.orders.toString()
                    )
                }
            }
        }

        item {
            KasirCard {
                SectionHeader(title = stringResource(R.string.cashflow_title))
                Spacer(Modifier.height(8.dp))
                DetailRow(
                    stringResource(R.string.total_in),
                    Formatters.money(state.cash.totalIn, symbol),
                    valueColor = MaterialTheme.kasirColors.cashIn
                )
                DetailRow(
                    stringResource(R.string.total_out),
                    Formatters.money(state.cash.totalOut, symbol),
                    valueColor = MaterialTheme.kasirColors.cashOut
                )
                DetailRow(
                    stringResource(R.string.net_cashflow),
                    Formatters.signedMoney(state.cash.net, symbol),
                    emphasise = true,
                    valueColor = if (state.cash.net >= 0) MaterialTheme.kasirColors.cashIn
                    else MaterialTheme.kasirColors.cashOut
                )
            }
        }

        item {
            NavigationRowCard(
                icon = Icons.Filled.QueryStats,
                title = stringResource(R.string.statistics_title),
                subtitle = stringResource(R.string.best_sellers),
                onClick = onOpenStatistics
            )
        }
        item {
            NavigationRowCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = stringResource(R.string.history_title),
                subtitle = stringResource(R.string.recent_transactions),
                onClick = onOpenHistory
            )
        }
        item {
            NavigationRowCard(
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.shift_title),
                subtitle = stringResource(R.string.shift_history),
                onClick = onOpenShift
            )
        }
        item {
            NavigationRowCard(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.store_profile),
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun NavigationRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    KasirCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
