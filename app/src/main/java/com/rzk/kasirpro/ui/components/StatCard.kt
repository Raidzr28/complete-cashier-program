package com.rzk.kasirpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.ui.theme.kasirColors
import kotlin.math.abs

/**
 * A single headline number. Optionally carries a period-over-period delta, which is the
 * part that turns a number into information — "Rp 1.2M" means little, "Rp 1.2M, +18%"
 * means the day is going well.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    deltaPercent: Double? = null,
    /** For costs and expenses, a rise is bad — flips the delta colouring. */
    higherIsBetter: Boolean = true,
    supporting: String? = null,
    onClick: (() -> Unit)? = null
) {
    KasirCard(
        modifier = modifier,
        containerColor = containerColor,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(accent.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.size(8.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (deltaPercent != null || supporting != null) {
            Spacer(Modifier.size(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (deltaPercent != null) DeltaBadge(deltaPercent, higherIsBetter)
                if (supporting != null) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DeltaBadge(percent: Double, higherIsBetter: Boolean = true, modifier: Modifier = Modifier) {
    val rising = percent >= 0
    val good = if (higherIsBetter) rising else !rising
    val color = if (good) MaterialTheme.kasirColors.cashIn else MaterialTheme.kasirColors.cashOut
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            if (rising) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            Formatters.percent(abs(percent), 0),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * The hero card on the dashboard — one dominant figure over a tinted surface, with up to
 * two supporting figures beside it.
 */
@Composable
fun HeroBalanceCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    leading: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    trailing: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    Icon(
                        leading, contentDescription = null,
                        tint = contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                if (trailing != null) trailing()
            }
            Spacer(Modifier.size(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (caption != null) {
                Spacer(Modifier.size(4.dp))
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}
