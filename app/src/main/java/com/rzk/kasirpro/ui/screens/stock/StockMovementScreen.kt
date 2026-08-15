package com.rzk.kasirpro.ui.screens.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.History
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
import com.rzk.kasirpro.data.local.entity.StockMovementEntity
import com.rzk.kasirpro.data.model.StockMovementType
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.PeriodChipRow
import com.rzk.kasirpro.ui.theme.kasirColors

/** Read-only audit trail: what moved, when, why, and what the balance was either side. */
@Composable
fun StockMovementScreen(
    onBack: () -> Unit,
    viewModel: StockMovementViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.movement_history)) },
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
            PeriodChipRow(
                selected = state.period.preset,
                onSelect = viewModel::setPeriod
            )
            Spacer(Modifier.size(10.dp))

            if (state.movements.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.no_movements)
                )
            } else {
                val todayLabel = stringResource(R.string.today)
                val yesterdayLabel = stringResource(R.string.yesterday)
                val grouped = state.movements.groupBy {
                    Formatters.relativeDay(it.createdAt, todayLabel, yesterdayLabel)
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (day, movements) ->
                        item(key = "h-$day") {
                            Text(
                                day,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(movements.size, key = { movements[it].id }) { index ->
                            MovementRow(movements[index], state.currencySymbol)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementRow(movement: StockMovementEntity, currencySymbol: String) {
    val inbound = movement.qty > 0
    val accent = if (inbound) MaterialTheme.kasirColors.cashIn else MaterialTheme.kasirColors.cashOut

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                movementIcon(movement.type),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                movement.productName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    movementLabel(movement.type),
                    Formatters.time(movement.createdAt),
                    movement.note.takeIf { it.isNotBlank() }
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
                if (inbound) "+${movement.qty}" else movement.qty.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                "${movement.stockBefore} → ${movement.stockAfter}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (movement.unitCost > 0 && inbound) {
                Text(
                    Formatters.compactMoney(movement.unitCost * movement.qty, currencySymbol),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun movementLabel(type: StockMovementType): String = stringResource(
    when (type) {
        StockMovementType.PURCHASE_IN -> R.string.movement_purchase
        StockMovementType.SALE_OUT -> R.string.movement_sale
        StockMovementType.ADJUSTMENT -> R.string.movement_adjustment
        StockMovementType.RETURN_IN -> R.string.movement_return
        StockMovementType.VOID_RETURN -> R.string.movement_void
        StockMovementType.WASTE_OUT -> R.string.movement_waste
        StockMovementType.INITIAL -> R.string.movement_initial
    }
)

private fun movementIcon(type: StockMovementType) = when (type) {
    StockMovementType.PURCHASE_IN, StockMovementType.RETURN_IN, StockMovementType.VOID_RETURN ->
        Icons.Filled.MoveDown
    StockMovementType.SALE_OUT -> Icons.Filled.MoveUp
    StockMovementType.WASTE_OUT -> Icons.Filled.Delete
    StockMovementType.ADJUSTMENT -> Icons.Filled.Tune
    StockMovementType.INITIAL -> Icons.Filled.Inventory
}
