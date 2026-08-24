package com.rzk.kasirpro.ui.screens.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.core.PrintUtils
import com.rzk.kasirpro.core.ReceiptFormatter
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.DetailRow
import com.rzk.kasirpro.ui.components.OrganicBlob
import com.rzk.kasirpro.ui.components.grainOverlay
import com.rzk.kasirpro.ui.theme.ReceiptTextStyle
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun ReceiptScreen(
    onDone: () -> Unit,
    viewModel: ReceiptViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val details = state.sale

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            OrganicBlob(
                color = MaterialTheme.kasirColors.cashIn,
                modifier = Modifier.size(120.dp)
            )
            Box(
                Modifier
                    .size(72.dp)
                    .background(MaterialTheme.kasirColors.cashInContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.kasirColors.onCashInContainer,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.sale_complete),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (details != null) {
            val symbol = state.settings.currencySymbol
            Spacer(Modifier.height(4.dp))
            Text(
                details.sale.invoiceNo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            Text(
                Formatters.money(details.sale.total, symbol),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (details.sale.changeAmount > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${stringResource(R.string.change_due)}: " +
                        Formatters.money(details.sale.changeAmount, symbol),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.kasirColors.cashIn,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))

            // Paper-like preview so what's on screen matches what comes out of the printer.
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .grainOverlay()
                    .padding(18.dp)
            ) {
                Text(
                    state.settings.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.settings.storeAddress.isNotBlank()) {
                    Text(
                        state.settings.storeAddress,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                details.items.forEach { item ->
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(item.productName, style = ReceiptTextStyle)
                            Text(
                                "${item.qty} × ${Formatters.number(item.unitPrice)}",
                                style = ReceiptTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.promoDiscount > 0) {
                                Text(
                                    "${item.promoName} −${Formatters.number(item.promoDiscount)}",
                                    style = ReceiptTextStyle,
                                    color = MaterialTheme.kasirColors.promo
                                )
                            }
                        }
                        Text(Formatters.number(item.lineTotal), style = ReceiptTextStyle)
                    }
                    Spacer(Modifier.height(6.dp))
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DetailRow(
                    stringResource(R.string.subtotal),
                    Formatters.money(details.sale.subtotal, symbol)
                )
                if (details.sale.orderDiscount > 0) {
                    DetailRow(
                        stringResource(R.string.discount),
                        "− ${Formatters.money(details.sale.orderDiscount, symbol)}"
                    )
                }
                if (details.sale.taxAmount > 0) {
                    DetailRow(
                        stringResource(R.string.tax),
                        Formatters.money(details.sale.taxAmount, symbol)
                    )
                }
                DetailRow(
                    stringResource(R.string.total),
                    Formatters.money(details.sale.total, symbol),
                    emphasise = true
                )
                details.payments.forEach { payment ->
                    DetailRow(payment.method.name, Formatters.money(payment.amount, symbol))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    state.settings.receiptFooter,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        PrintUtils.shareText(
                            context,
                            ReceiptFormatter.buildText(details, state.settings),
                            details.sale.invoiceNo
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.share_receipt))
                }
                OutlinedButton(
                    onClick = {
                        PrintUtils.printHtml(
                            context,
                            ReceiptFormatter.buildHtml(details, state.settings),
                            details.sale.invoiceNo
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.print_receipt))
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.new_sale), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}
