package com.rzk.kasirpro.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.PaymentMethod
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.domain.CartCalculator
import com.rzk.kasirpro.domain.CartLine
import com.rzk.kasirpro.domain.CartTotals
import com.rzk.kasirpro.domain.TenderLine
import com.rzk.kasirpro.ui.components.AmountKeypad
import com.rzk.kasirpro.ui.components.DetailRow
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.KasirTextField
import com.rzk.kasirpro.ui.components.MoneyField
import com.rzk.kasirpro.ui.components.QuantityStepper
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.StatusPill
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> stringResource(R.string.method_cash)
    PaymentMethod.QRIS -> stringResource(R.string.method_qris)
    PaymentMethod.DEBIT -> stringResource(R.string.method_debit)
    PaymentMethod.CREDIT -> stringResource(R.string.method_credit)
    PaymentMethod.TRANSFER -> stringResource(R.string.method_transfer)
    PaymentMethod.EWALLET -> stringResource(R.string.method_ewallet)
}

// ---------------------------------------------------------------------------- cart

@Composable
fun CartSheet(
    state: PosUiState,
    onDismiss: () -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onEditLine: (Long) -> Unit,
    onOrderDiscountChange: (Long, DiscountType) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onClearCart: () -> Unit,
    onHold: (String) -> Unit,
    onCharge: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val symbol = state.settings.currencySymbol
    var showHoldDialog by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.cart),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearCart) { Text(stringResource(R.string.clear_cart)) }
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(state.lines, key = { it.product.id }) { line ->
                    CartLineRow(
                        line = line,
                        currencySymbol = symbol,
                        onQuantityChange = { onQuantityChange(line.product.id, it) },
                        onRemove = { onRemove(line.product.id) },
                        onEdit = { onEditLine(line.product.id) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = { showDetails = !showDetails }) {
                Text(if (showDetails) stringResource(R.string.close) else stringResource(R.string.more))
            }

            if (showDetails) {
                OrderDiscountEditor(
                    value = state.orderDiscountInput,
                    type = state.orderDiscountType,
                    currencySymbol = symbol,
                    onChange = onOrderDiscountChange
                )
                Spacer(Modifier.height(10.dp))
                KasirTextField(
                    value = state.customerName,
                    onValueChange = onCustomerNameChange,
                    label = stringResource(R.string.customer_name)
                )
                Spacer(Modifier.height(10.dp))
                KasirTextField(
                    value = state.orderNote,
                    onValueChange = onNoteChange,
                    label = stringResource(R.string.order_note),
                    singleLine = false
                )
                Spacer(Modifier.height(14.dp))
            }

            TotalsBlock(state.totals, symbol, state.settings)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showHoldDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.hold_order)) }
                Button(
                    onClick = onCharge,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(
                            R.string.charge_amount,
                            Formatters.money(state.totals.total, symbol)
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showHoldDialog) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showHoldDialog = false },
            title = { Text(stringResource(R.string.hold_order)) },
            text = {
                KasirTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = stringResource(R.string.hold_label)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onHold(label)
                    showHoldDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showHoldDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    currencySymbol: String,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                line.product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${Formatters.money(line.product.sellPrice, currencySymbol)} × ${line.qty}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (line.promo != null) {
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    text = stringResource(R.string.promo_applied, line.promo.name),
                    containerColor = MaterialTheme.kasirColors.promoContainer,
                    contentColor = MaterialTheme.kasirColors.onPromoContainer
                )
            }
            if (line.manualDiscount > 0) {
                Text(
                    "− ${Formatters.money(line.manualDiscount, currencySymbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.kasirColors.cashOut
                )
            }
        }
        QuantityStepper(
            quantity = line.qty,
            onQuantityChange = onQuantityChange,
            compact = true,
            minQuantity = 0
        )
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Formatters.money(line.lineTotal, currencySymbol),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, Modifier.size(16.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.remove_item),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDiscountEditor(
    value: Long,
    type: DiscountType,
    currencySymbol: String,
    onChange: (Long, DiscountType) -> Unit
) {
    Column {
        Text(
            stringResource(R.string.order_discount),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            DiscountType.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = type == option,
                    onClick = { onChange(value, option) },
                    shape = SegmentedButtonDefaults.itemShape(index, DiscountType.entries.size)
                ) {
                    Text(
                        if (option == DiscountType.PERCENT) stringResource(R.string.discount_percent)
                        else stringResource(R.string.discount_fixed)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        MoneyField(
            value = value,
            onValueChange = { onChange(it, type) },
            label = stringResource(R.string.discount),
            currencySymbol = if (type == DiscountType.PERCENT) "%" else currencySymbol
        )
    }
}

@Composable
private fun TotalsBlock(totals: CartTotals, symbol: String, settings: SettingsEntity) {
    Column {
        DetailRow(stringResource(R.string.subtotal), Formatters.money(totals.subtotal, symbol))
        if (totals.promoDiscount > 0) {
            DetailRow(
                stringResource(R.string.promo_badge),
                "− ${Formatters.money(totals.promoDiscount, symbol)}",
                valueColor = MaterialTheme.kasirColors.cashOut
            )
        }
        if (totals.lineDiscount > 0) {
            DetailRow(
                stringResource(R.string.line_discount),
                "− ${Formatters.money(totals.lineDiscount, symbol)}",
                valueColor = MaterialTheme.kasirColors.cashOut
            )
        }
        if (totals.orderDiscount > 0) {
            DetailRow(
                stringResource(R.string.order_discount),
                "− ${Formatters.money(totals.orderDiscount, symbol)}",
                valueColor = MaterialTheme.kasirColors.cashOut
            )
        }
        if (totals.serviceCharge > 0) {
            DetailRow(
                stringResource(R.string.service_charge),
                Formatters.money(totals.serviceCharge, symbol)
            )
        }
        if (totals.tax > 0) {
            DetailRow(
                "${stringResource(R.string.tax)} ${settings.taxPercent}%",
                Formatters.money(totals.tax, symbol)
            )
        }
        if (totals.rounding != 0L) {
            DetailRow(
                stringResource(R.string.rounding),
                Formatters.signedMoney(totals.rounding, symbol)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        DetailRow(
            stringResource(R.string.total),
            Formatters.money(totals.total, symbol),
            emphasise = true
        )
    }
}

// ---------------------------------------------------------------------------- payment

/**
 * Tender entry. Defaults to a single cash payment (the overwhelmingly common case) but
 * lets the cashier stack tenders for a split payment, tracking what's still owed.
 */
@Composable
fun PaymentSheet(
    totals: CartTotals,
    settings: SettingsEntity,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<TenderLine>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val symbol = settings.currencySymbol

    var tenders by remember { mutableStateOf(listOf<TenderLine>()) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var amount by remember { mutableLongStateOf(totals.total) }
    var reference by remember { mutableStateOf("") }

    val settled = tenders.sumOf { it.amount }
    val outstanding = (totals.total - settled).coerceAtLeast(0)
    val paid = settled + amount
    val change = (paid - totals.total).coerceAtLeast(0)
    val canConfirm = paid >= totals.total && !isProcessing

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(stringResource(R.string.payment), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        stringResource(R.string.amount_due),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        Formatters.money(totals.total, symbol),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text(
                            "${stringResource(R.string.amount_paid)}: ${Formatters.money(paid, symbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (change > 0)
                                "${stringResource(R.string.change_due)}: ${Formatters.money(change, symbol)}"
                            else
                                "${stringResource(R.string.still_due)}: ${Formatters.money((totals.total - paid).coerceAtLeast(0), symbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (tenders.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionHeader(title = stringResource(R.string.split_payment))
                tenders.forEachIndexed { index, tender ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(paymentMethodLabel(tender.method), Modifier.weight(1f))
                        Text(
                            Formatters.money(tender.amount, symbol),
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { tenders = tenders.filterIndexed { i, _ -> i != index } },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.payment_method),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.take(3).forEach { option ->
                    MethodChip(option, method == option, Modifier.weight(1f)) { method = option }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.drop(3).forEach { option ->
                    MethodChip(option, method == option, Modifier.weight(1f)) { method = option }
                }
            }

            Spacer(Modifier.height(14.dp))
            MoneyField(
                value = amount,
                onValueChange = { amount = it },
                label = if (method == PaymentMethod.CASH) stringResource(R.string.cash_received)
                else stringResource(R.string.amount),
                currencySymbol = symbol
            )

            if (method == PaymentMethod.CASH) {
                Spacer(Modifier.height(10.dp))
                val quickOptions = remember(outstanding) {
                    CartCalculator.quickCashOptions(if (outstanding > 0) outstanding else totals.total)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { amount = if (outstanding > 0) outstanding else totals.total },
                        label = { Text(stringResource(R.string.exact_amount)) }
                    )
                    quickOptions.take(3).forEach { option ->
                        AssistChip(
                            onClick = { amount = option },
                            label = {
                                Text(
                                    Formatters.compactMoney(option, ""),
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                KasirTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = stringResource(R.string.reference_optional),
                    keyboardType = KeyboardType.Text
                )
            }

            Spacer(Modifier.height(12.dp))
            AmountKeypad(
                onDigit = { digit -> amount = (amount * 10 + digit).coerceAtMost(999_999_999_999L) },
                onDoubleZero = { amount = (amount * 100).coerceAtMost(999_999_999_999L) },
                onBackspace = { amount /= 10 },
                onClear = { amount = 0 }
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        if (amount > 0) {
                            tenders = tenders + TenderLine(method, amount, reference)
                            reference = ""
                            amount = (totals.total - tenders.sumOf { it.amount }).coerceAtLeast(0)
                        }
                    },
                    enabled = amount > 0 && paid < totals.total,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.add_tender)) }

                Button(
                    onClick = {
                        val finalTenders =
                            if (amount > 0) tenders + TenderLine(method, amount, reference) else tenders
                        onConfirm(finalTenders)
                    },
                    enabled = canConfirm,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.complete_payment), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodChip(
    method: PaymentMethod,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            paymentMethodLabel(method),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------- held orders

@Composable
fun HeldOrdersSheet(
    heldOrders: List<SaleWithDetails>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onResume: (SaleWithDetails) -> Unit,
    onDiscard: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.held_orders), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            if (heldOrders.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = stringResource(R.string.no_held_orders)
                )
            } else {
                LazyColumn(
                    Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(heldOrders, key = { it.sale.id }) { held ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    held.sale.customerName.ifBlank {
                                        Formatters.time(held.sale.createdAt)
                                    },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "${stringResource(R.string.items_count, held.itemCount)} • " +
                                        Formatters.money(held.sale.total, currencySymbol),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onDiscard(held.sale.id) }) {
                                Text(
                                    stringResource(R.string.discard_order),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Button(
                                onClick = { onResume(held) },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(stringResource(R.string.resume_order)) }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- line edit

@Composable
fun LineEditDialog(
    line: CartLine,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onApply: (discount: Long, note: String) -> Unit
) {
    var discount by remember { mutableLongStateOf(line.manualDiscount) }
    var note by remember { mutableStateOf(line.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(line.product.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                MoneyField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = stringResource(R.string.line_discount),
                    currencySymbol = currencySymbol,
                    supportingText = Formatters.money(line.gross, currencySymbol)
                )
                Spacer(Modifier.height(10.dp))
                KasirTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.item_note)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(discount, note) }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
