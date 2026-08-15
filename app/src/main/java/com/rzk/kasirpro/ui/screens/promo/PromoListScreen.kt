package com.rzk.kasirpro.ui.screens.promo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.rzk.kasirpro.data.local.entity.PromoEntity
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.domain.PromoEngine
import com.rzk.kasirpro.ui.components.ConfirmDialog
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.StatusPill
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun PromoListScreen(
    onBack: () -> Unit,
    onEditPromo: (Long) -> Unit,
    viewModel: PromoListViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<PromoEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.promos_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditPromo(0L) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_promo)) }
            )
        }
    ) { padding ->
        if (state.promos.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.LocalOffer,
                title = stringResource(R.string.no_promos),
                message = stringResource(R.string.no_promos_hint),
                actionLabel = stringResource(R.string.add_promo),
                onAction = { onEditPromo(0L) },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.promos, key = { it.id }) { promo ->
                    PromoCard(
                        promo = promo,
                        currencySymbol = state.currencySymbol,
                        onClick = { onEditPromo(promo.id) },
                        onToggle = { viewModel.setActive(promo.id, it) },
                        onDelete = { pendingDelete = promo }
                    )
                }
            }
        }
    }

    pendingDelete?.let { promo ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = stringResource(R.string.delete_promo_confirm, promo.name),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = { viewModel.delete(promo) },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun PromoCard(
    promo: PromoEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val colors = MaterialTheme.kasirColors

    // Three independent reasons a promo isn't discounting anything right now, and the
    // owner needs to be able to tell them apart at a glance.
    val (label, container, content) = when {
        !promo.isActive -> Triple(
            stringResource(R.string.promo_paused),
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        now >= promo.endAt -> Triple(
            stringResource(R.string.promo_expired),
            colors.cashOutContainer,
            colors.onCashOutContainer
        )
        now < promo.startAt -> Triple(
            stringResource(R.string.promo_scheduled),
            colors.infoContainer,
            colors.onInfoContainer
        )
        PromoEngine.isLiveNow(promo, now) -> Triple(
            stringResource(R.string.promo_live),
            colors.cashInContainer,
            colors.onCashInContainer
        )
        // In its date range, but outside today's weekday / happy-hour window.
        else -> Triple(
            stringResource(R.string.promo_scheduled),
            colors.warningContainer,
            colors.onWarningContainer
        )
    }

    KasirCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    promo.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    PromoEngine.describe(promo, currencySymbol),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Formatters.shortDate(promo.startAt)} → ${Formatters.shortDate(promo.endAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(text = label, containerColor = container, contentColor = content)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = promo.isActive, onCheckedChange = onToggle)
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
