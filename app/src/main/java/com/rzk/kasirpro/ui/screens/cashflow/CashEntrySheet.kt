package com.rzk.kasirpro.ui.screens.cashflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.repository.CashCategories
import com.rzk.kasirpro.ui.components.AmountKeypad
import com.rzk.kasirpro.ui.components.KasirTextField
import com.rzk.kasirpro.ui.components.SettingSwitchRow
import com.rzk.kasirpro.ui.theme.kasirColors

/**
 * Record a cash movement. Deliberately amount-first with a full-size keypad: the cashier
 * usually has money in one hand, and the amount is the only field that is always required.
 */
@Composable
fun CashEntrySheet(
    type: CashFlowType,
    currencySymbol: String,
    existing: CashFlowEntity?,
    onDismiss: () -> Unit,
    onSave: (amount: Long, category: String, note: String, affectsDrawer: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isIn = type == CashFlowType.IN
    val colors = MaterialTheme.kasirColors
    val container = if (isIn) colors.cashInContainer else colors.cashOutContainer
    val onContainer = if (isIn) colors.onCashInContainer else colors.onCashOutContainer

    var amount by remember { mutableLongStateOf(existing?.amount ?: 0L) }
    var category by remember { mutableStateOf(existing?.category.orEmpty()) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    var affectsDrawer by remember { mutableStateOf(existing?.affectsCashDrawer ?: true) }

    val presets = remember(type) { if (isIn) CashCategories.cashIn else CashCategories.cashOut }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                stringResource(
                    if (existing != null) R.string.edit_entry
                    else if (isIn) R.string.add_cash_in else R.string.add_cash_out
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(container, RoundedCornerShape(22.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = onContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        Formatters.money(amount, currencySymbol),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = onContainer
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            AmountKeypad(
                onDigit = { digit -> amount = (amount * 10 + digit).coerceAtMost(999_999_999_999L) },
                onDoubleZero = { amount = (amount * 100).coerceAtMost(999_999_999_999L) },
                onBackspace = { amount /= 10 },
                onClear = { amount = 0 }
            )

            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.category), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = category == preset,
                        onClick = { category = preset },
                        label = { Text(preset) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            KasirTextField(
                value = category,
                onValueChange = { category = it },
                label = stringResource(R.string.category)
            )

            Spacer(Modifier.height(10.dp))
            KasirTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.note),
                singleLine = false
            )

            Spacer(Modifier.height(4.dp))
            SettingSwitchRow(
                title = stringResource(R.string.affects_drawer),
                subtitle = stringResource(R.string.affects_drawer_hint),
                checked = affectsDrawer,
                onCheckedChange = { affectsDrawer = it }
            )

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onSave(amount, category, note, affectsDrawer) },
                enabled = amount > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }

            if (amount <= 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.amount_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (onDelete != null) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
