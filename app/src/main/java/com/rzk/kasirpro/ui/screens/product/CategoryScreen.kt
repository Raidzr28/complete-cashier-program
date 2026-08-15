package com.rzk.kasirpro.ui.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.ui.components.ConfirmDialog
import com.rzk.kasirpro.ui.components.EmptyState
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.KasirTextField

/** Palette offered when creating a category — colour-coding the POS grid is the point. */
private val CategoryColorChoices = listOf(
    0xFFEF6C00, 0xFF0288D1, 0xFF7B1FA2, 0xFF2E7D32, 0xFFC2185B,
    0xFF5D4037, 0xFF00838F, 0xFF455A64, 0xFFF9A825, 0xFF6A1B9A
).map { it.toInt() }

@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    viewModel: CategoryViewModel = viewModel(factory = AppViewModelProvider)
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
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
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_category)) }
            )
        }
    ) { padding ->
        if (categories.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Category,
                title = stringResource(R.string.no_categories),
                actionLabel = stringResource(R.string.add_category),
                onAction = { creating = true },
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
                items(categories, key = { it.id }) { category ->
                    KasirCard(
                        onClick = { editing = category },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(Color(category.colorArgb), RoundedCornerShape(11.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                category.name,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { pendingDelete = category }) {
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
    }

    if (creating || editing != null) {
        CategoryDialog(
            existing = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { name, color ->
                val target = editing
                if (target == null) viewModel.create(name, color)
                else viewModel.update(target.copy(name = name, colorArgb = color))
                creating = false
                editing = null
            }
        )
    }

    pendingDelete?.let { category ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = stringResource(R.string.delete_category_confirm, category.name),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = { viewModel.delete(category) },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun CategoryDialog(
    existing: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var color by remember {
        mutableIntStateOf(existing?.colorArgb ?: CategoryColorChoices.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (existing == null) R.string.add_category else R.string.edit_category
                )
            )
        },
        text = {
            Column {
                KasirTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.category_name)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.category_color),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryColorChoices.forEach { choice ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(Color(choice), CircleShape)
                                .border(
                                    width = if (color == choice) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { color = choice }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, color) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
