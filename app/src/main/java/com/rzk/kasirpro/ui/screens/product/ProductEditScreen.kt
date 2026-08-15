package com.rzk.kasirpro.ui.screens.product

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
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.ChipFilterRow
import com.rzk.kasirpro.ui.components.DetailRow
import com.rzk.kasirpro.ui.components.IntField
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.KasirTextField
import com.rzk.kasirpro.ui.components.MoneyField
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.SettingSwitchRow
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun ProductEditScreen(
    scannedBarcode: String?,
    onScanConsumed: () -> Unit,
    onOpenScanner: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ProductEditViewModel = viewModel(factory = AppViewModelProvider)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val support by viewModel.support.collectAsStateWithLifecycle()
    val symbol = support.settings.currencySymbol

    LaunchedEffect(scannedBarcode) {
        val code = scannedBarcode
        if (!code.isNullOrBlank()) {
            viewModel.onBarcodeScanned(code)
            onScanConsumed()
        }
    }

    val savedMessage = stringResource(R.string.product_saved)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                ProductEditEvent.Saved -> {
                    snackbarHostState.showSnackbar(savedMessage)
                    onBack()
                }
                is ProductEditEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (form.isNew) R.string.add_product else R.string.edit_product
                        )
                    )
                },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KasirTextField(
                value = form.name,
                onValueChange = { value -> viewModel.update { it.copy(name = value) } },
                label = stringResource(R.string.product_name),
                isError = form.nameError,
                supportingText = if (form.nameError) stringResource(R.string.name_required) else null
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KasirTextField(
                    value = form.sku,
                    onValueChange = { value -> viewModel.update { it.copy(sku = value) } },
                    label = stringResource(R.string.sku),
                    modifier = Modifier.weight(1f)
                )
                KasirTextField(
                    value = form.unit,
                    onValueChange = { value -> viewModel.update { it.copy(unit = value) } },
                    label = stringResource(R.string.unit),
                    modifier = Modifier.weight(1f)
                )
            }

            KasirTextField(
                value = form.barcode,
                onValueChange = { value -> viewModel.update { it.copy(barcode = value) } },
                label = stringResource(R.string.barcode),
                trailing = {
                    IconButton(onClick = onOpenScanner) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_barcode)
                        )
                    }
                }
            )

            SectionHeader(title = stringResource(R.string.category))
            ChipFilterRow(
                options = listOf<Long?>(null) + support.categories.map { it.id },
                selected = form.categoryId,
                onSelect = { id -> viewModel.update { it.copy(categoryId = id) } },
                label = { id ->
                    if (id == null) stringResource(R.string.uncategorised)
                    else support.categories.firstOrNull { it.id == id }?.name.orEmpty()
                },
                contentPadding = PaddingValues(0.dp)
            )

            SectionHeader(title = stringResource(R.string.price))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyField(
                    value = form.costPrice,
                    onValueChange = { value -> viewModel.update { it.copy(costPrice = value) } },
                    label = stringResource(R.string.cost_price),
                    currencySymbol = symbol,
                    modifier = Modifier.weight(1f)
                )
                MoneyField(
                    value = form.sellPrice,
                    onValueChange = { value -> viewModel.update { it.copy(sellPrice = value) } },
                    label = stringResource(R.string.sell_price),
                    currencySymbol = symbol,
                    isError = form.priceError,
                    modifier = Modifier.weight(1f)
                )
            }

            KasirCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                DetailRow(
                    stringResource(R.string.margin),
                    "${Formatters.money(form.margin, symbol)} • ${Formatters.percent(form.marginPercent, 1)}",
                    emphasise = true,
                    valueColor = if (form.margin >= 0) MaterialTheme.kasirColors.cashIn
                    else MaterialTheme.kasirColors.cashOut
                )
            }

            SectionHeader(title = stringResource(R.string.stock_title))
            SettingSwitchRow(
                title = stringResource(R.string.track_stock),
                subtitle = stringResource(R.string.track_stock_hint),
                checked = form.trackStock,
                onCheckedChange = { value -> viewModel.update { it.copy(trackStock = value) } }
            )

            if (form.trackStock) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntField(
                        value = form.stock,
                        onValueChange = { value -> viewModel.update { it.copy(stock = value) } },
                        label = stringResource(R.string.initial_stock),
                        // Existing stock only moves through the stock ledger, never a form edit.
                        enabled = form.isNew,
                        modifier = Modifier.weight(1f)
                    )
                    IntField(
                        value = form.minStock,
                        onValueChange = { value -> viewModel.update { it.copy(minStock = value) } },
                        label = stringResource(R.string.min_stock),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    stringResource(R.string.min_stock_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            KasirTextField(
                value = form.note,
                onValueChange = { value -> viewModel.update { it.copy(note = value) } },
                label = stringResource(R.string.note),
                singleLine = false
            )

            SettingSwitchRow(
                title = stringResource(R.string.product_active),
                checked = form.isActive,
                onCheckedChange = { value -> viewModel.update { it.copy(isActive = value) } }
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }

            if (!form.isNew) {
                TextButton(
                    onClick = viewModel::archive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.archive),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
