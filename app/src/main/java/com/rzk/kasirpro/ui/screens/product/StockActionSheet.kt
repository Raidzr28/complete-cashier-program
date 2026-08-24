package com.rzk.kasirpro.ui.screens.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.model.ProductWithCategory
import com.rzk.kasirpro.ui.components.DetailRow
import com.rzk.kasirpro.ui.components.IntField
import com.rzk.kasirpro.ui.components.KasirTextField
import com.rzk.kasirpro.ui.components.MoneyField
import com.rzk.kasirpro.ui.components.SettingSwitchRow
import com.rzk.kasirpro.ui.theme.kasirColors

private enum class StockAction { IN, COUNT, WASTE }

/**
 * The three ways stock legitimately changes outside of a sale. Splitting them keeps the
 * *reason* attached to every movement, which is what makes the ledger worth reading later.
 */
@Composable
fun StockActionSheet(
    item: ProductWithCategory,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onStockIn: (qty: Int, unitCost: Long, note: String, updateCost: Boolean, payFromCash: Boolean) -> Unit,
    onAdjust: (countedStock: Int, reason: String) -> Unit,
    onWriteOff: (qty: Int, reason: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var action by remember { mutableStateOf(StockAction.IN) }

    var receivedQty by remember { mutableIntStateOf(0) }
    var unitCost by remember { mutableLongStateOf(item.product.costPrice) }
    var updateCost by remember { mutableStateOf(true) }
    var payFromCash by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    var countedStock by remember { mutableIntStateOf(item.product.stock) }
    var wasteQty by remember { mutableIntStateOf(0) }
    var reason by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                item.product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${stringResource(R.string.current_stock)}: ${item.product.stock} ${item.product.unit}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                StockAction.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = action == option,
                        onClick = { action = option },
                        shape = SegmentedButtonDefaults.itemShape(index, StockAction.entries.size)
                    ) {
                        Text(
                            when (option) {
                                StockAction.IN -> stringResource(R.string.stock_in)
                                StockAction.COUNT -> stringResource(R.string.stock_adjust)
                                StockAction.WASTE -> stringResource(R.string.write_off)
                            },
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (action) {
                StockAction.IN -> {
                    Text(
                        stringResource(R.string.stock_in_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    IntField(
                        value = receivedQty,
                        onValueChange = { receivedQty = it },
                        label = stringResource(R.string.received_qty),
                        suffix = item.product.unit
                    )
                    Spacer(Modifier.height(10.dp))
                    MoneyField(
                        value = unitCost,
                        onValueChange = { unitCost = it },
                        label = stringResource(R.string.unit_cost),
                        currencySymbol = currencySymbol
                    )
                    Spacer(Modifier.height(10.dp))
                    DetailRow(
                        stringResource(R.string.purchase_total),
                        Formatters.money(unitCost * receivedQty, currencySymbol),
                        emphasise = true
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.update_cost_price),
                        checked = updateCost,
                        onCheckedChange = { updateCost = it }
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.pay_from_cash),
                        subtitle = stringResource(R.string.pay_from_cash_hint),
                        checked = payFromCash,
                        onCheckedChange = { payFromCash = it }
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = stringResource(R.string.note)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onStockIn(receivedQty, unitCost, note, updateCost, payFromCash)
                        },
                        enabled = receivedQty > 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(stringResource(R.string.stock_in), fontWeight = FontWeight.Bold) }
                }

                StockAction.COUNT -> {
                    Text(
                        stringResource(R.string.stock_adjust_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    IntField(
                        value = countedStock,
                        onValueChange = { countedStock = it },
                        label = stringResource(R.string.counted_stock),
                        suffix = item.product.unit
                    )
                    Spacer(Modifier.height(10.dp))
                    val delta = countedStock - item.product.stock
                    DetailRow(
                        stringResource(R.string.difference),
                        (if (delta > 0) "+$delta" else delta.toString()),
                        emphasise = true,
                        valueColor = when {
                            delta > 0 -> MaterialTheme.kasirColors.cashIn
                            delta < 0 -> MaterialTheme.kasirColors.cashOut
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = stringResource(R.string.reason)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onAdjust(countedStock, reason) },
                        enabled = delta != 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
                }

                StockAction.WASTE -> {
                    Text(
                        stringResource(R.string.write_off_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    IntField(
                        value = wasteQty,
                        onValueChange = { wasteQty = it },
                        label = stringResource(R.string.quantity),
                        suffix = item.product.unit
                    )
                    Spacer(Modifier.height(10.dp))
                    DetailRow(
                        stringResource(R.string.cost),
                        Formatters.money(item.product.costPrice * wasteQty, currencySymbol),
                        valueColor = MaterialTheme.kasirColors.cashOut
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = stringResource(R.string.reason)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onWriteOff(wasteQty, reason) },
                        enabled = wasteQty in 1..item.product.stock,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(stringResource(R.string.write_off), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
