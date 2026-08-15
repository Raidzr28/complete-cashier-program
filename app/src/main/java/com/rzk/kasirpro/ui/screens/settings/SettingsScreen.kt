package com.rzk.kasirpro.ui.screens.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.BuildConfig
import com.rzk.kasirpro.R
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.ConfirmDialog
import com.rzk.kasirpro.ui.components.IntField
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.KasirTextField
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.SettingSwitchRow
import com.rzk.kasirpro.ui.navigation.Routes

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }

    val savedMessage = stringResource(R.string.settings_saved)
    val clearedMessage = stringResource(R.string.data_cleared)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SettingsEvent.Saved -> snackbarHostState.showSnackbar(savedMessage)
                SettingsEvent.DataCleared -> snackbarHostState.showSnackbar(clearedMessage)
                is SettingsEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                KasirCard {
                    SectionHeader(title = stringResource(R.string.store_profile))
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = settings.storeName,
                        onValueChange = { value -> viewModel.update { it.copy(storeName = value) } },
                        label = stringResource(R.string.store_name)
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = settings.storeAddress,
                        onValueChange = { value -> viewModel.update { it.copy(storeAddress = value) } },
                        label = stringResource(R.string.store_address),
                        singleLine = false
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = settings.storePhone,
                        onValueChange = { value -> viewModel.update { it.copy(storePhone = value) } },
                        label = stringResource(R.string.store_phone)
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = settings.currencySymbol,
                        onValueChange = { value -> viewModel.update { it.copy(currencySymbol = value) } },
                        label = stringResource(R.string.currency_symbol)
                    )
                }
            }

            item {
                KasirCard {
                    SectionHeader(title = stringResource(R.string.tax_settings))
                    SettingSwitchRow(
                        title = stringResource(R.string.enable_tax),
                        checked = settings.taxEnabled,
                        onCheckedChange = { value -> viewModel.update { it.copy(taxEnabled = value) } }
                    )
                    if (settings.taxEnabled) {
                        IntField(
                            value = settings.taxPercent,
                            onValueChange = { value -> viewModel.update { it.copy(taxPercent = value) } },
                            label = stringResource(R.string.tax_percent),
                            suffix = "%"
                        )
                        SettingSwitchRow(
                            title = stringResource(R.string.tax_inclusive),
                            checked = settings.taxInclusive,
                            onCheckedChange = { value ->
                                viewModel.update { it.copy(taxInclusive = value) }
                            }
                        )
                    }
                    SettingSwitchRow(
                        title = stringResource(R.string.enable_service_charge),
                        checked = settings.serviceChargeEnabled,
                        onCheckedChange = { value ->
                            viewModel.update { it.copy(serviceChargeEnabled = value) }
                        }
                    )
                    if (settings.serviceChargeEnabled) {
                        IntField(
                            value = settings.serviceChargePercent,
                            onValueChange = { value ->
                                viewModel.update { it.copy(serviceChargePercent = value) }
                            },
                            label = stringResource(R.string.service_charge_percent),
                            suffix = "%"
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    IntField(
                        value = settings.roundingNearest,
                        onValueChange = { value ->
                            viewModel.update { it.copy(roundingNearest = value) }
                        },
                        label = stringResource(R.string.rounding_nearest),
                        suffix = settings.currencySymbol
                    )
                }
            }

            item {
                KasirCard {
                    SectionHeader(title = stringResource(R.string.receipt_settings))
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = settings.receiptHeader,
                        onValueChange = { value ->
                            viewModel.update { it.copy(receiptHeader = value) }
                        },
                        label = stringResource(R.string.receipt_header)
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = settings.receiptFooter,
                        onValueChange = { value ->
                            viewModel.update { it.copy(receiptFooter = value) }
                        },
                        label = stringResource(R.string.receipt_footer),
                        singleLine = false
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.print_barcode),
                        checked = settings.printBarcodeOnReceipt,
                        onCheckedChange = { value ->
                            viewModel.update { it.copy(printBarcodeOnReceipt = value) }
                        }
                    )
                }
            }

            item {
                KasirCard {
                    SectionHeader(title = stringResource(R.string.inventory_settings))
                    SettingSwitchRow(
                        title = stringResource(R.string.low_stock_alerts),
                        checked = settings.lowStockAlertEnabled,
                        onCheckedChange = { value ->
                            viewModel.update { it.copy(lowStockAlertEnabled = value) }
                        }
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.block_out_of_stock),
                        checked = settings.blockSaleWhenOutOfStock,
                        onCheckedChange = { value ->
                            viewModel.update { it.copy(blockSaleWhenOutOfStock = value) }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = settings.defaultCashierName,
                        onValueChange = { value ->
                            viewModel.update { it.copy(defaultCashierName = value) }
                        },
                        label = stringResource(R.string.default_cashier)
                    )
                    Spacer(Modifier.height(10.dp))
                    KasirTextField(
                        value = settings.invoicePrefix,
                        onValueChange = { value ->
                            viewModel.update { it.copy(invoicePrefix = value) }
                        },
                        label = stringResource(R.string.invoice_no)
                    )
                }
            }

            item {
                LinkRow(
                    icon = Icons.Filled.Category,
                    label = stringResource(R.string.manage_categories),
                    onClick = { onNavigate(Routes.CATEGORIES) }
                )
            }
            item {
                LinkRow(
                    icon = Icons.Filled.LocalOffer,
                    label = stringResource(R.string.manage_promos),
                    onClick = { onNavigate(Routes.PROMOS) }
                )
            }

            item {
                KasirCard {
                    SectionHeader(title = stringResource(R.string.data_section))
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = viewModel::restoreSampleData,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(stringResource(R.string.restore_sample)) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            stringResource(R.string.clear_data),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.clear_data),
            message = stringResource(R.string.clear_data_confirm),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = viewModel::clearBusinessData,
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    KasirCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(14.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
