package com.rzk.kasirpro.ui.screens.promo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.PromoScope
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.ChipFilterRow
import com.rzk.kasirpro.ui.components.IntField
import com.rzk.kasirpro.ui.components.KasirTextField
import com.rzk.kasirpro.ui.components.MoneyField
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.SettingSwitchRow

private val DayLabels = listOf(
    R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
    R.string.day_fri, R.string.day_sat, R.string.day_sun
)

@Composable
fun PromoEditScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: PromoEditViewModel = viewModel(factory = AppViewModelProvider)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val support by viewModel.support.collectAsStateWithLifecycle()
    val symbol = support.settings.currencySymbol

    var datePickerFor by remember { mutableStateOf<String?>(null) }
    var timePickerFor by remember { mutableStateOf<String?>(null) }

    val savedMessage = stringResource(R.string.promo_saved)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                PromoEditEvent.Saved -> {
                    snackbarHostState.showSnackbar(savedMessage)
                    onBack()
                }
                is PromoEditEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (form.id == 0L) R.string.add_promo else R.string.edit_promo
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
                label = stringResource(R.string.promo_name)
            )

            SectionHeader(title = stringResource(R.string.discount_type))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                DiscountType.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = form.discountType == option,
                        onClick = { viewModel.update { it.copy(discountType = option) } },
                        shape = SegmentedButtonDefaults.itemShape(index, DiscountType.entries.size)
                    ) {
                        Text(
                            if (option == DiscountType.PERCENT) stringResource(R.string.discount_percent)
                            else stringResource(R.string.discount_fixed)
                        )
                    }
                }
            }

            MoneyField(
                value = form.value,
                onValueChange = { value -> viewModel.update { it.copy(value = value) } },
                label = stringResource(R.string.discount_value),
                currencySymbol = if (form.discountType == DiscountType.PERCENT) "%" else symbol
            )

            SectionHeader(title = stringResource(R.string.applies_to))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PromoScope.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = form.scope == option,
                        onClick = { viewModel.update { it.copy(scope = option) } },
                        shape = SegmentedButtonDefaults.itemShape(index, PromoScope.entries.size)
                    ) {
                        Text(
                            when (option) {
                                PromoScope.ALL_PRODUCTS -> stringResource(R.string.scope_all)
                                PromoScope.CATEGORY -> stringResource(R.string.scope_category)
                                PromoScope.PRODUCT -> stringResource(R.string.scope_product)
                            },
                            maxLines = 1
                        )
                    }
                }
            }

            when (form.scope) {
                PromoScope.CATEGORY -> ChipFilterRow(
                    options = support.categories.map { it.id },
                    selected = form.categoryId,
                    onSelect = { id -> viewModel.update { it.copy(categoryId = id) } },
                    label = { id -> support.categories.firstOrNull { it.id == id }?.name.orEmpty() },
                    contentPadding = PaddingValues(0.dp)
                )
                PromoScope.PRODUCT -> ChipFilterRow(
                    options = support.products.map { it.product.id },
                    selected = form.productId,
                    onSelect = { id -> viewModel.update { it.copy(productId = id) } },
                    label = { id ->
                        support.products.firstOrNull { it.product.id == id }?.product?.name.orEmpty()
                    },
                    contentPadding = PaddingValues(0.dp)
                )
                PromoScope.ALL_PRODUCTS -> Unit
            }

            SectionHeader(title = stringResource(R.string.start_date))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { datePickerFor = "start" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(Formatters.date(form.startAt)) }
                OutlinedButton(
                    onClick = { datePickerFor = "end" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(Formatters.date(form.endAt)) }
            }

            SectionHeader(
                title = stringResource(R.string.active_days),
                subtitle = stringResource(R.string.active_days_hint)
            )
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayLabels.forEachIndexed { index, labelRes ->
                    val isoDay = index + 1
                    FilterChip(
                        selected = isoDay in form.days,
                        onClick = { viewModel.toggleDay(isoDay) },
                        label = { Text(stringResource(labelRes)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            SectionHeader(
                title = stringResource(R.string.happy_hour),
                subtitle = stringResource(R.string.happy_hour_hint)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { timePickerFor = "start" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("${stringResource(R.string.start_time)} ${Formatters.minuteOfDay(form.startMinute)}")
                }
                OutlinedButton(
                    onClick = { timePickerFor = "end" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("${stringResource(R.string.end_time)} ${Formatters.minuteOfDay(form.endMinute)}")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntField(
                    value = form.minQty,
                    onValueChange = { value -> viewModel.update { it.copy(minQty = value) } },
                    label = stringResource(R.string.min_qty),
                    modifier = Modifier.weight(1f)
                )
                MoneyField(
                    value = form.maxDiscount,
                    onValueChange = { value -> viewModel.update { it.copy(maxDiscount = value) } },
                    label = stringResource(R.string.max_discount),
                    currencySymbol = symbol,
                    supportingText = stringResource(R.string.max_discount_hint),
                    modifier = Modifier.weight(1f)
                )
            }

            SettingSwitchRow(
                title = stringResource(R.string.promo_enabled),
                checked = form.isActive,
                onCheckedChange = { value -> viewModel.update { it.copy(isActive = value) } }
            )

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = viewModel::save,
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        }
    }

    datePickerFor?.let { target ->
        val initial = if (target == "start") form.startAt else form.endAt
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.update {
                            if (target == "start") it.copy(startAt = millis)
                            else it.copy(endAt = millis)
                        }
                    }
                    datePickerFor = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerFor = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) { DatePicker(state = pickerState) }
    }

    timePickerFor?.let { target ->
        val initialMinutes = if (target == "start") form.startMinute else form.endMinute
        val timeState = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { timePickerFor = null },
            title = { Text(stringResource(R.string.happy_hour)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = timeState.hour * 60 + timeState.minute
                    viewModel.update {
                        if (target == "start") it.copy(startMinute = minutes)
                        else it.copy(endMinute = minutes)
                    }
                    timePickerFor = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { timePickerFor = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
