package com.rzk.kasirpro.ui.screens.history

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.core.PrintUtils
import com.rzk.kasirpro.core.ReceiptFormatter
import com.rzk.kasirpro.data.model.SaleStatus
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.DetailRow
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.KasirTextField
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.StatusPill
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun SaleDetailScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SaleDetailViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val details = state.sale
    var showVoidDialog by remember { mutableStateOf(false) }

    val voidedMessage = stringResource(R.string.sale_voided)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SaleDetailEvent.Voided -> {
                    snackbarHostState.showSnackbar(voidedMessage)
                    onBack()
                }
                is SaleDetailEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transaction_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (details != null) {
                        IconButton(onClick = {
                            PrintUtils.shareText(
                                context,
                                ReceiptFormatter.buildText(details, state.settings),
                                details.sale.invoiceNo
                            )
                        }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = stringResource(R.string.share_receipt)
                            )
                        }
                        IconButton(onClick = {
                            PrintUtils.printHtml(
                                context,
                                ReceiptFormatter.buildHtml(details, state.settings),
                                details.sale.invoiceNo
                            )
                        }) {
                            Icon(
                                Icons.Filled.Print,
                                contentDescription = stringResource(R.string.print_receipt)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (details == null) return@Scaffold

        val symbol = state.settings.currencySymbol
        val sale = details.sale

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KasirCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            sale.invoiceNo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            Formatters.dateTime(sale.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    StatusPill(
                        text = stringResource(
                            when (sale.status) {
                                SaleStatus.COMPLETED -> R.string.status_completed
                                SaleStatus.VOID -> R.string.status_void
                                SaleStatus.HELD -> R.string.status_held
                                SaleStatus.REFUNDED -> R.string.status_refunded
                            }
                        ),
                        containerColor = if (sale.status == SaleStatus.COMPLETED)
                            MaterialTheme.kasirColors.cashInContainer
                        else MaterialTheme.kasirColors.cashOutContainer,
                        contentColor = if (sale.status == SaleStatus.COMPLETED)
                            MaterialTheme.kasirColors.onCashInContainer
                        else MaterialTheme.kasirColors.onCashOutContainer
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    Formatters.money(sale.total, symbol),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            KasirCard {
                SectionHeader(title = stringResource(R.string.cart))
                Spacer(Modifier.height(8.dp))
                details.items.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${item.qty} ${item.unit} × ${Formatters.money(item.unitPrice, symbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.promoDiscount > 0) {
                                Text(
                                    "${item.promoName} −${Formatters.money(item.promoDiscount, symbol)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.kasirColors.promo
                                )
                            }
                        }
                        Text(
                            Formatters.money(item.lineTotal, symbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                DetailRow(stringResource(R.string.subtotal), Formatters.money(sale.subtotal, symbol))
                if (sale.promoDiscount > 0) {
                    DetailRow(
                        stringResource(R.string.discount),
                        "− ${Formatters.money(sale.promoDiscount, symbol)}"
                    )
                }
                if (sale.orderDiscount > 0) {
                    DetailRow(
                        stringResource(R.string.order_discount),
                        "− ${Formatters.money(sale.orderDiscount, symbol)}"
                    )
                }
                if (sale.serviceCharge > 0) {
                    DetailRow(
                        stringResource(R.string.service_charge),
                        Formatters.money(sale.serviceCharge, symbol)
                    )
                }
                if (sale.taxAmount > 0) {
                    DetailRow(stringResource(R.string.tax), Formatters.money(sale.taxAmount, symbol))
                }
                DetailRow(
                    stringResource(R.string.total),
                    Formatters.money(sale.total, symbol),
                    emphasise = true
                )
            }

            KasirCard {
                SectionHeader(title = stringResource(R.string.payment))
                Spacer(Modifier.height(8.dp))
                details.payments.forEach { payment ->
                    DetailRow(payment.method.name, Formatters.money(payment.amount, symbol))
                }
                if (sale.changeAmount > 0) {
                    DetailRow(
                        stringResource(R.string.change_due),
                        Formatters.money(sale.changeAmount, symbol)
                    )
                }
                if (sale.customerName.isNotBlank()) {
                    DetailRow(stringResource(R.string.customer_name), sale.customerName)
                }
                DetailRow(stringResource(R.string.cashier_label), sale.cashierName)
                DetailRow(
                    stringResource(R.string.profit),
                    Formatters.money(details.profit, symbol),
                    valueColor = MaterialTheme.kasirColors.cashIn
                )
                if (sale.note.isNotBlank()) {
                    DetailRow(stringResource(R.string.note), sale.note)
                }
                if (sale.status == SaleStatus.VOID && sale.voidReason.isNotBlank()) {
                    DetailRow(stringResource(R.string.void_reason), sale.voidReason)
                }
            }

            if (sale.status == SaleStatus.COMPLETED) {
                OutlinedButton(
                    onClick = { showVoidDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        stringResource(R.string.void_sale),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showVoidDialog && details != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showVoidDialog = false },
            title = { Text(stringResource(R.string.void_sale)) },
            text = {
                Column {
                    Text(stringResource(R.string.void_confirm, details.sale.invoiceNo))
                    Spacer(Modifier.height(12.dp))
                    KasirTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = stringResource(R.string.void_reason)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.voidSale(reason)
                    showVoidDialog = false
                }) {
                    Text(
                        stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
