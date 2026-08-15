package com.rzk.kasirpro.ui.screens.cashflow

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.model.CashFlowSource
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.model.PeriodPreset
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.ConfirmDialog
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.GroupedBarDatum
import com.rzk.kasirpro.ui.components.GroupedBarChart
import com.rzk.kasirpro.ui.components.HeroBalanceCard
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.PeriodChipRow
import com.rzk.kasirpro.ui.components.RankBarRow
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.StatCard
import com.rzk.kasirpro.ui.components.StatusPill
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun CashFlowScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: CashFlowViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbol = state.settings.currencySymbol

    var sheetType by remember { mutableStateOf<CashFlowType?>(null) }
    var editing by remember { mutableStateOf<CashFlowEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<CashFlowEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val savedMessage = stringResource(R.string.entry_saved)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                CashFlowEvent.Saved -> snackbarHostState.showSnackbar(savedMessage)
                is CashFlowEvent.Error -> errorMessage = event.message
            }
        }
    }
    val readonlyMessage = stringResource(R.string.auto_entry_readonly)
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it.ifBlank { readonlyMessage })
            errorMessage = null
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { sheetType = CashFlowType.OUT },
                    containerColor = MaterialTheme.kasirColors.cashOutContainer,
                    contentColor = MaterialTheme.kasirColors.onCashOutContainer,
                    icon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                    text = { Text(stringResource(R.string.cash_out)) }
                )
                ExtendedFloatingActionButton(
                    onClick = { sheetType = CashFlowType.IN },
                    containerColor = MaterialTheme.kasirColors.cashInContainer,
                    contentColor = MaterialTheme.kasirColors.onCashInContainer,
                    icon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                    text = { Text(stringResource(R.string.cash_in)) }
                )
            }
        }
    ) { padding ->
        // Resolved out here because a LazyColumn body is LazyListScope, not a composable
        // context — stringResource() cannot be called inside it.
        val todayLabel = stringResource(R.string.today)
        val yesterdayLabel = stringResource(R.string.yesterday)
        val uncategorisedLabel = stringResource(R.string.uncategorised)
        val entriesByDay = remember(state.entries, todayLabel, yesterdayLabel) {
            state.entries.groupBy {
                Formatters.relativeDay(it.createdAt, todayLabel, yesterdayLabel)
            }
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.cashflow_title),
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
                HeroBalanceCard(
                    label = stringResource(R.string.cash_on_hand),
                    value = Formatters.money(state.cashOnHand, symbol),
                    caption = stringResource(R.string.cash_on_hand_hint),
                    leading = Icons.Outlined.AccountBalanceWallet
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = stringResource(R.string.total_in),
                        value = Formatters.compactMoney(state.summary.totalIn, symbol),
                        icon = Icons.Filled.ArrowDownward,
                        accent = MaterialTheme.kasirColors.cashIn,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.total_out),
                        value = Formatters.compactMoney(state.summary.totalOut, symbol),
                        icon = Icons.Filled.ArrowUpward,
                        accent = MaterialTheme.kasirColors.cashOut,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                KasirCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.net_cashflow),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            Formatters.signedMoney(state.summary.net, symbol),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (state.summary.net >= 0) MaterialTheme.kasirColors.cashIn
                            else MaterialTheme.kasirColors.cashOut
                        )
                    }
                }
            }

            val dayKeys = viewModel.dayKeys()
            if (dayKeys.size > 1) {
                item {
                    KasirCard {
                        SectionHeader(title = stringResource(R.string.cashflow_title))
                        Spacer(Modifier.height(10.dp))
                        GroupedBarChart(
                            data = dayKeys.takeLast(14).map { key ->
                                val row = state.dailyInOut.firstOrNull { it.dayKey == key }
                                GroupedBarDatum(
                                    label = Formatters.dayKeyToDayName(key),
                                    // DailyTotal reuses `total` for money in and `profit`
                                    // for money out — see CashFlowDao.observeDailyInOut.
                                    primary = row?.total ?: 0L,
                                    secondary = row?.profit ?: 0L
                                )
                            },
                            primaryLabel = stringResource(R.string.cash_in),
                            secondaryLabel = stringResource(R.string.cash_out)
                        )
                    }
                }
            }

            if (state.expenseByCategory.isNotEmpty()) {
                item {
                    KasirCard {
                        SectionHeader(title = stringResource(R.string.expense_breakdown))
                        Spacer(Modifier.height(6.dp))
                        val peak = state.expenseByCategory.maxOf { it.total }.coerceAtLeast(1)
                        state.expenseByCategory.take(6).forEachIndexed { index, row ->
                            RankBarRow(
                                rank = index + 1,
                                label = row.category.ifBlank { uncategorisedLabel },
                                value = Formatters.compactMoney(row.total, symbol),
                                supporting = "${row.entries}×",
                                fraction = row.total.toFloat() / peak.toFloat(),
                                barColor = MaterialTheme.kasirColors.cashOut
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.typeFilter == null,
                        onClick = { viewModel.setTypeFilter(null) },
                        label = { Text(stringResource(R.string.all)) }
                    )
                    FilterChip(
                        selected = state.typeFilter == CashFlowType.IN,
                        onClick = { viewModel.setTypeFilter(CashFlowType.IN) },
                        label = { Text(stringResource(R.string.cash_in)) }
                    )
                    FilterChip(
                        selected = state.typeFilter == CashFlowType.OUT,
                        onClick = { viewModel.setTypeFilter(CashFlowType.OUT) },
                        label = { Text(stringResource(R.string.cash_out)) }
                    )
                    FilterChip(
                        selected = state.manualOnly,
                        onClick = { viewModel.setManualOnly(!state.manualOnly) },
                        label = { Text(stringResource(R.string.manual_only)) }
                    )
                }
            }

            if (state.entries.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = stringResource(R.string.cashflow_empty),
                        message = stringResource(R.string.cashflow_empty_hint)
                    )
                }
            } else {
                entriesByDay.forEach { (day, entries) ->
                    item(key = "header-$day") {
                        Text(
                            day,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(entries.size, key = { entries[it].id }) { index ->
                        val entry = entries[index]
                        CashEntryRow(
                            entry = entry,
                            currencySymbol = symbol,
                            onClick = {
                                if (entry.source == CashFlowSource.MANUAL) editing = entry
                                else errorMessage = ""
                            },
                            onLongClick = {
                                if (entry.source == CashFlowSource.MANUAL) pendingDelete = entry
                                else errorMessage = ""
                            }
                        )
                    }
                }
            }
        }
    }

    sheetType?.let { type ->
        CashEntrySheet(
            type = type,
            currencySymbol = symbol,
            existing = null,
            onDismiss = { sheetType = null },
            onSave = { amount, category, note, affectsDrawer ->
                viewModel.addEntry(type, amount, category, note, affectsDrawer)
                sheetType = null
            }
        )
    }

    editing?.let { entry ->
        CashEntrySheet(
            type = entry.type,
            currencySymbol = symbol,
            existing = entry,
            onDismiss = { editing = null },
            onSave = { amount, category, note, affectsDrawer ->
                viewModel.updateEntry(
                    entry.copy(
                        amount = amount,
                        category = category,
                        note = note,
                        affectsCashDrawer = affectsDrawer
                    )
                )
                editing = null
            },
            onDelete = {
                pendingDelete = entry
                editing = null
            }
        )
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = stringResource(R.string.delete_entry_confirm),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = { viewModel.deleteEntry(entry) },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun CashEntryRow(
    entry: CashFlowEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MaterialTheme.kasirColors
    val isIn = entry.type == CashFlowType.IN
    val accent = if (isIn) colors.cashIn else colors.cashOut
    val container = if (isIn) colors.cashInContainer else colors.cashOutContainer
    val onContainer = if (isIn) colors.onCashInContainer else colors.onCashOutContainer
    val uncategorisedLabel = stringResource(R.string.uncategorised)

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(18.dp))
            // Tap edits, long-press deletes — both refused for auto-posted rows.
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(container, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isIn) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.category.ifBlank { uncategorisedLabel },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    Formatters.time(entry.createdAt),
                    entry.note.takeIf { it.isNotBlank() }
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
                Formatters.signedMoney(if (isIn) entry.amount else -entry.amount, currencySymbol),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            if (entry.source != CashFlowSource.MANUAL) {
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    text = stringResource(R.string.auto_generated),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = Icons.Filled.Lock
                )
            } else if (!entry.affectsCashDrawer) {
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    text = stringResource(R.string.affects_drawer),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
