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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.model.ProductSalesStat
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.BarChart
import com.rzk.kasirpro.ui.components.BarDatum
import com.rzk.kasirpro.ui.components.DetailRow
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.PeriodChipRow
import com.rzk.kasirpro.ui.components.RankBarRow
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.StaggeredEntrance
import com.rzk.kasirpro.ui.components.StatCard
import com.rzk.kasirpro.ui.theme.kasirColors

/**
 * Product performance. Best sellers and slowest movers sit side by side on purpose: the
 * pair is what actually drives a restocking decision.
 */
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbol = state.settings.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
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
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StaggeredEntrance(0) {
                    PeriodChipRow(
                        selected = state.period.preset,
                        onSelect = viewModel::setPeriod,
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }

            item {
                StaggeredEntrance(1) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        RankMetric.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = state.metric == option,
                                onClick = { viewModel.setMetric(option) },
                                shape = SegmentedButtonDefaults.itemShape(index, RankMetric.entries.size)
                            ) {
                                Text(
                                    when (option) {
                                        RankMetric.QUANTITY -> stringResource(R.string.rank_by_qty)
                                        RankMetric.REVENUE -> stringResource(R.string.rank_by_revenue)
                                        RankMetric.PROFIT -> stringResource(R.string.rank_by_profit)
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item {
                StaggeredEntrance(2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = stringResource(R.string.busiest_hour),
                            value = state.busiestHour?.let { "${it.hourKey}:00" } ?: "—",
                            icon = Icons.Filled.Schedule,
                            accent = MaterialTheme.kasirColors.warning,
                            supporting = state.busiestHour?.let {
                                Formatters.compactMoney(it.total, symbol)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.never_sold),
                            value = state.neverSold.size.toString(),
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            accent = MaterialTheme.kasirColors.cashOut,
                            higherIsBetter = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                StaggeredEntrance(3) {
                    KasirCard {
                        SectionHeader(title = stringResource(R.string.sales_by_hour))
                        Spacer(Modifier.height(10.dp))
                        BarChart(
                            data = (7..22).map { hour ->
                                val key = "%02d".format(hour)
                                val row = state.hourly.firstOrNull { it.hourKey == key }
                                BarDatum(
                                    label = if (hour % 3 == 1) key else "",
                                    value = row?.total ?: 0L,
                                    display = Formatters.compactMoney(row?.total ?: 0L, symbol)
                                )
                            },
                            emptyLabel = stringResource(R.string.no_data),
                            barColor = MaterialTheme.kasirColors.warning
                        )
                    }
                }
            }

            item {
                StaggeredEntrance(4) {
                    RankingCard(
                        title = stringResource(R.string.best_sellers),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        stats = state.best,
                        metric = state.metric,
                        currencySymbol = symbol,
                        barColor = MaterialTheme.kasirColors.cashIn
                    )
                }
            }

            item {
                StaggeredEntrance(5) {
                    RankingCard(
                        title = stringResource(R.string.worst_sellers),
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        stats = state.worst,
                        metric = state.metric,
                        currencySymbol = symbol,
                        barColor = MaterialTheme.kasirColors.warning
                    )
                }
            }

            if (state.neverSold.isNotEmpty()) {
                item {
                    StaggeredEntrance(6) {
                        KasirCard {
                            SectionHeader(
                                title = stringResource(R.string.never_sold),
                                subtitle = stringResource(R.string.never_sold_hint)
                            )
                            Spacer(Modifier.height(8.dp))
                            state.neverSold.take(10).forEach { stat ->
                                DetailRow(stat.productName, "0")
                            }
                        }
                    }
                }
            }

            if (state.categories.isNotEmpty()) {
                item {
                    StaggeredEntrance(7) {
                        KasirCard {
                            SectionHeader(title = stringResource(R.string.sales_by_category))
                            Spacer(Modifier.height(6.dp))
                            val peak = state.categories.maxOf { it.revenue }.coerceAtLeast(1)
                            state.categories.forEachIndexed { index, row ->
                                RankBarRow(
                                    rank = index + 1,
                                    label = row.categoryName
                                        ?: stringResource(R.string.uncategorised),
                                    value = Formatters.compactMoney(row.revenue, symbol),
                                    supporting = stringResource(R.string.units_sold, row.qtySold),
                                    fraction = row.revenue.toFloat() / peak.toFloat()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    stats: List<ProductSalesStat>,
    metric: RankMetric,
    currencySymbol: String,
    barColor: Color
) {
    KasirCard {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = barColor)
            Spacer(Modifier.padding(horizontal = 5.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))

        if (stats.isEmpty()) {
            Text(
                stringResource(R.string.no_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@KasirCard
        }

        val peak = stats.maxOf { metricValue(it, metric) }.coerceAtLeast(1L)
        stats.forEachIndexed { index, stat ->
            RankBarRow(
                rank = index + 1,
                label = stat.productName,
                value = when (metric) {
                    RankMetric.QUANTITY -> stat.qtySold.toString()
                    RankMetric.REVENUE -> Formatters.compactMoney(stat.revenue, currencySymbol)
                    RankMetric.PROFIT -> Formatters.compactMoney(stat.profit, currencySymbol)
                },
                supporting = when (metric) {
                    RankMetric.QUANTITY ->
                        Formatters.compactMoney(stat.revenue, currencySymbol)
                    else -> stringResource(R.string.units_sold, stat.qtySold)
                },
                fraction = metricValue(stat, metric).toFloat() / peak.toFloat(),
                barColor = barColor
            )
        }
    }
}

private fun metricValue(stat: ProductSalesStat, metric: RankMetric): Long = when (metric) {
    RankMetric.QUANTITY -> stat.qtySold.toLong()
    RankMetric.REVENUE -> stat.revenue
    RankMetric.PROFIT -> stat.profit
}
