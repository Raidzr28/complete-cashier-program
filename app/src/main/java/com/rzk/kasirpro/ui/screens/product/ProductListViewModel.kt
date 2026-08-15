package com.rzk.kasirpro.ui.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.ProductWithCategory
import com.rzk.kasirpro.data.model.StockValuation
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductListUiState(
    val query: String = "",
    val selectedCategoryId: Long? = null,
    val showArchived: Boolean = false,
    val products: List<ProductWithCategory> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val valuation: StockValuation = StockValuation(),
    val lowStockCount: Int = 0,
    val settings: SettingsEntity = SettingsEntity()
)

sealed interface StockEvent {
    data object StockUpdated : StockEvent
    data class Error(val message: String) : StockEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModel(container: AppContainer) : ViewModel() {

    private val catalog = container.catalogRepository
    private val stock = container.stockRepository
    private val settingsRepo = container.settingsRepository

    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<Long?>(null)
    private val showArchived = MutableStateFlow(false)

    private val events = Channel<StockEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val filters = combine(query, categoryFilter, showArchived) { q, c, a -> Triple(q, c, a) }

    /**
     * Archived products are filtered in memory rather than with another query: the catalogue
     * is small enough that a second DAO path isn't worth the duplication.
     */
    private val filteredProducts = combine(
        catalog.observeAllProducts(),
        filters
    ) { all, (q, categoryId, archived) ->
        all.filter { item ->
            val product = item.product
            val matchesArchive = if (archived) true else product.isActive
            val matchesCategory = categoryId == null || product.categoryId == categoryId
            val needle = q.trim().lowercase()
            val matchesQuery = needle.isEmpty() ||
                product.name.lowercase().contains(needle) ||
                product.sku.lowercase().contains(needle) ||
                product.barcode.contains(needle)
            matchesArchive && matchesCategory && matchesQuery
        }
    }

    private val inventorySignals = combine(
        catalog.observeCategories(),
        catalog.observeStockValuation(),
        catalog.observeLowStockCount(),
        settingsRepo.settings
    ) { categories, valuation, lowStock, settings ->
        InventorySignals(categories, valuation, lowStock, settings)
    }

    val uiState: StateFlow<ProductListUiState> = combine(
        filteredProducts,
        inventorySignals,
        filters
    ) { products, inventory, (q, categoryId, archived) ->
        ProductListUiState(
            query = q,
            selectedCategoryId = categoryId,
            showArchived = archived,
            products = products,
            categories = inventory.categories,
            valuation = inventory.valuation,
            lowStockCount = inventory.lowStockCount,
            settings = inventory.settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    private data class InventorySignals(
        val categories: List<CategoryEntity>,
        val valuation: StockValuation,
        val lowStockCount: Int,
        val settings: SettingsEntity
    )

    fun setQuery(value: String) { query.value = value }
    fun setCategory(id: Long?) { categoryFilter.value = id }
    fun toggleArchived() { showArchived.value = !showArchived.value }

    fun archive(productId: Long) = viewModelScope.launch { catalog.archiveProduct(productId) }
    fun restore(productId: Long) = viewModelScope.launch { catalog.restoreProduct(productId) }

    fun stockIn(
        productId: Long,
        qty: Int,
        unitCost: Long,
        note: String,
        updateCostPrice: Boolean,
        payFromCash: Boolean
    ) = viewModelScope.launch {
        stock.stockIn(productId, qty, unitCost, note, updateCostPrice, payFromCash)
            .onSuccess { events.send(StockEvent.StockUpdated) }
            .onFailure { events.send(StockEvent.Error(it.message.orEmpty())) }
    }

    fun adjustStock(productId: Long, countedStock: Int, reason: String) = viewModelScope.launch {
        stock.adjustTo(productId, countedStock, reason)
            .onSuccess { events.send(StockEvent.StockUpdated) }
            .onFailure { events.send(StockEvent.Error(it.message.orEmpty())) }
    }

    fun writeOff(productId: Long, qty: Int, reason: String) = viewModelScope.launch {
        stock.writeOff(productId, qty, reason)
            .onSuccess { events.send(StockEvent.StockUpdated) }
            .onFailure { events.send(StockEvent.Error(it.message.orEmpty())) }
    }
}
