package com.rzk.kasirpro.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rzk.kasirpro.R
import com.rzk.kasirpro.data.model.PeriodPreset

/** The period filter every reporting surface shares, so "7 days" means one thing app-wide. */
@Composable
fun PeriodChipRow(
    selected: PeriodPreset,
    onSelect: (PeriodPreset) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    presets: List<PeriodPreset> = DefaultPeriodPresets
) {
    ChipFilterRow(
        options = presets,
        selected = selected,
        onSelect = onSelect,
        label = { periodLabel(it) },
        modifier = modifier,
        contentPadding = contentPadding
    )
}

val DefaultPeriodPresets = listOf(
    PeriodPreset.TODAY,
    PeriodPreset.YESTERDAY,
    PeriodPreset.WEEK,
    PeriodPreset.MONTH,
    PeriodPreset.YEAR,
    PeriodPreset.ALL
)

@Composable
fun periodLabel(preset: PeriodPreset): String = when (preset) {
    PeriodPreset.TODAY -> stringResource(R.string.today)
    PeriodPreset.YESTERDAY -> stringResource(R.string.yesterday)
    PeriodPreset.WEEK -> stringResource(R.string.this_week)
    PeriodPreset.MONTH -> stringResource(R.string.this_month)
    PeriodPreset.YEAR -> stringResource(R.string.this_year)
    PeriodPreset.ALL -> stringResource(R.string.all_time)
    PeriodPreset.CUSTOM -> stringResource(R.string.custom_range)
}
