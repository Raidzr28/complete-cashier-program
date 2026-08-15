package com.rzk.kasirpro.ui.screens.shift

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.local.entity.ShiftEntity
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.DetailRow
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.HeroBalanceCard
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.KasirTextField
import com.rzk.kasirpro.ui.components.MoneyField
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun ShiftScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ShiftViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbol = state.settings.currencySymbol
    var showClose by remember { mutableStateOf(false) }

    val closedMessage = stringResource(R.string.shift_closed)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                ShiftEvent.Closed -> snackbarHostState.showSnackbar(closedMessage)
                ShiftEvent.Opened -> Unit
                is ShiftEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shift_title)) },
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
            val shift = state.openShift
            if (shift == null) {
                item { OpenShiftCard(state.settings.defaultCashierName, symbol, viewModel::openShift) }
            } else {
                item {
                    HeroBalanceCard(
                        label = stringResource(R.string.expected_cash),
                        value = Formatters.money(state.expectedCash, symbol),
                        caption = stringResource(
                            R.string.shift_duration,
                            Formatters.duration(shift.openedAt, System.currentTimeMillis())
                        ),
                        leading = Icons.Outlined.Schedule
                    )
                }
                item {
                    KasirCard {
                        SectionHeader(title = stringResource(R.string.shift_summary))
                        Spacer(Modifier.height(8.dp))
                        DetailRow(stringResource(R.string.cashier_name), shift.cashierName)
                        DetailRow(
                            stringResource(R.string.opening_cash),
                            Formatters.money(shift.openingCash, symbol)
                        )
                        DetailRow(
                            stringResource(R.string.shift_sales),
                            Formatters.money(state.shiftSales.net, symbol)
                        )
                        DetailRow(
                            stringResource(R.string.orders_count),
                            state.shiftSales.orders.toString()
                        )
                        DetailRow(
                            stringResource(R.string.expected_cash),
                            Formatters.money(state.expectedCash, symbol),
                            emphasise = true
                        )
                    }
                }
                item {
                    Button(
                        onClick = { showClose = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.close_shift), fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { SectionHeader(title = stringResource(R.string.shift_history)) }

            if (state.history.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.no_open_shift),
                        message = stringResource(R.string.no_open_shift_hint)
                    )
                }
            } else {
                items(state.history.size, key = { state.history[it].id }) { index ->
                    PastShiftCard(state.history[index], symbol)
                }
            }
        }
    }

    if (showClose && state.openShift != null) {
        CloseShiftDialog(
            expectedCash = state.expectedCash,
            currencySymbol = symbol,
            onDismiss = { showClose = false },
            onConfirm = { actual, note ->
                viewModel.closeShift(actual, note)
                showClose = false
            }
        )
    }
}

@Composable
private fun OpenShiftCard(
    defaultCashier: String,
    currencySymbol: String,
    onOpen: (String, Long) -> Unit
) {
    var cashier by remember { mutableStateOf(defaultCashier) }
    var openingCash by remember { mutableLongStateOf(0L) }

    KasirCard {
        Text(
            stringResource(R.string.no_open_shift),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.no_open_shift_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        KasirTextField(
            value = cashier,
            onValueChange = { cashier = it },
            label = stringResource(R.string.cashier_name)
        )
        Spacer(Modifier.height(10.dp))
        MoneyField(
            value = openingCash,
            onValueChange = { openingCash = it },
            label = stringResource(R.string.opening_cash),
            currencySymbol = currencySymbol,
            supportingText = stringResource(R.string.opening_cash_hint)
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onOpen(cashier, openingCash) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.open_shift), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PastShiftCard(shift: ShiftEntity, currencySymbol: String) {
    val colors = MaterialTheme.kasirColors
    KasirCard {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    shift.cashierName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${Formatters.dateTime(shift.openedAt)} — " +
                        (shift.closedAt?.let { Formatters.time(it) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                when {
                    shift.difference > 0 ->
                        stringResource(R.string.over_by, Formatters.money(shift.difference, currencySymbol))
                    shift.difference < 0 ->
                        stringResource(R.string.short_by, Formatters.money(-shift.difference, currencySymbol))
                    else -> stringResource(R.string.balanced)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    shift.difference > 0 -> colors.cashIn
                    shift.difference < 0 -> colors.cashOut
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        DetailRow(
            stringResource(R.string.expected_cash),
            Formatters.money(shift.expectedCash, currencySymbol)
        )
        DetailRow(
            stringResource(R.string.counted_cash),
            Formatters.money(shift.actualCash, currencySymbol)
        )
    }
}

@Composable
private fun CloseShiftDialog(
    expectedCash: Long,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    var counted by remember { mutableLongStateOf(expectedCash) }
    var note by remember { mutableStateOf("") }
    val difference = counted - expectedCash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.close_shift)) },
        text = {
            Column {
                DetailRow(
                    stringResource(R.string.expected_cash),
                    Formatters.money(expectedCash, currencySymbol),
                    emphasise = true
                )
                Spacer(Modifier.height(10.dp))
                MoneyField(
                    value = counted,
                    onValueChange = { counted = it },
                    label = stringResource(R.string.counted_cash),
                    currencySymbol = currencySymbol
                )
                Spacer(Modifier.height(10.dp))
                DetailRow(
                    stringResource(R.string.variance),
                    when {
                        difference > 0 ->
                            stringResource(R.string.over_by, Formatters.money(difference, currencySymbol))
                        difference < 0 ->
                            stringResource(R.string.short_by, Formatters.money(-difference, currencySymbol))
                        else -> stringResource(R.string.balanced)
                    },
                    emphasise = true,
                    valueColor = when {
                        difference > 0 -> MaterialTheme.kasirColors.cashIn
                        difference < 0 -> MaterialTheme.kasirColors.cashOut
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(Modifier.height(10.dp))
                KasirTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.note)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(counted, note) }) {
                Text(stringResource(R.string.close_shift), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
