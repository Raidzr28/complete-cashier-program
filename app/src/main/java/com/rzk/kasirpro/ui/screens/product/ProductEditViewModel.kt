package com.rzk.kasirpro.ui.screens.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.di.AppContainer
import com.rzk.kasirpro.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductForm(
    val id: Long = 0,
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val categoryId: Long? = null,
    val costPrice: Long = 0,
    val sellPrice: Long = 0,
    val stock: Int = 0,
    val minStock: Int = 5,
    val unit: String = "pcs",
    val trackStock: Boolean = true,
    val isActive: Boolean = true,
    val note: String = ""
) {
    val isNew: Boolean get() = id == 0L
    val margin: Long get() = sellPrice - costPrice
    val marginPercent: Double
        get() = if (sellPrice <= 0) 0.0 else margin * 100.0 / sellPrice
    val nameError: Boolean get() = name.isBlank()
    val priceError: Boolean get() = sellPrice <= 0
    val canSave: Boolean get() = !nameError && !priceError
}

sealed interface ProductEditEvent {
    data object Saved : ProductEditEvent
    data object StockUpdated : ProductEditEvent
    /** Scanned a barcode that already belongs to another active product. */
    data class ExistingProduct(val product: ProductEntity) : ProductEditEvent
    data class Error(val message: String) : ProductEditEvent
}

class ProductEditViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val catalog = container.catalogRepository
    private val stock = container.stockRepository
    private val productId: Long = savedStateHandle.get<Long>(Routes.ARG_PRODUCT_ID) ?: 0L

    private val _form = MutableStateFlow(ProductForm())
    val form: StateFlow<ProductForm> = _form.asStateFlow()

    private val events = Channel<ProductEditEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    data class Support(
        val categories: List<CategoryEntity> = emptyList(),
        val settings: SettingsEntity = SettingsEntity()
    )

    val support: StateFlow<Support> = combine(
        catalog.observeCategories(),
        container.settingsRepository.settings
    ) { categories, settings -> Support(categories, settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Support())

    init {
        if (productId != 0L) {
            viewModelScope.launch {
                catalog.getProduct(productId)?.let { product ->
                    _form.value = ProductForm(
                        id = product.id,
                        name = product.name,
                        sku = product.sku,
                        barcode = product.barcode,
                        categoryId = product.categoryId,
                        costPrice = product.costPrice,
                        sellPrice = product.sellPrice,
                        stock = product.stock,
                        minStock = product.minStock,
                        unit = product.unit,
                        trackStock = product.trackStock,
                        isActive = product.isActive,
                        note = product.note
                    )
                }
            }
        }
    }

    fun update(transform: (ProductForm) -> ProductForm) {
        _form.value = transform(_form.value)
    }

    /**
     * A scan means "this is the item in my hand" — if that barcode already belongs to
     * another active product, the cashier almost certainly meant to restock it, not spawn
     * a duplicate catalogue entry with a fragmented stock count.
     */
    fun onBarcodeScanned(code: String) {
        update { it.copy(barcode = code) }
        if (_form.value.isNew) {
            viewModelScope.launch {
                catalog.findByBarcode(code)?.let { existing ->
                    events.send(ProductEditEvent.ExistingProduct(existing))
                }
            }
        }
    }

    fun save() {
        val form = _form.value
        if (!form.canSave) return
        viewModelScope.launch {
            val entity = ProductEntity(
                id = form.id,
                name = form.name.trim(),
                sku = form.sku.trim(),
                barcode = form.barcode.trim(),
                categoryId = form.categoryId,
                costPrice = form.costPrice,
                sellPrice = form.sellPrice,
                stock = form.stock,
                minStock = form.minStock,
                unit = form.unit.trim().ifBlank { "pcs" },
                trackStock = form.trackStock,
                isActive = form.isActive,
                note = form.note.trim()
            )
            val result = if (form.isNew) {
                catalog.createProduct(entity).map { }
            } else {
                catalog.updateProduct(entity)
            }
            result
                .onSuccess { events.send(ProductEditEvent.Saved) }
                .onFailure { events.send(ProductEditEvent.Error(it.message.orEmpty())) }
        }
    }

    fun archive() {
        val id = _form.value.id
        if (id == 0L) return
        viewModelScope.launch {
            catalog.archiveProduct(id)
            events.send(ProductEditEvent.Saved)
        }
    }

    /** Restock this product. Goes through [StockRepository] so the movement ledger stays whole. */
    fun stockIn(qty: Int, unitCost: Long, note: String, updateCostPrice: Boolean, payFromCash: Boolean) {
        val id = _form.value.id
        if (id == 0L) return
        viewModelScope.launch {
            stock.stockIn(id, qty, unitCost, note, updateCostPrice, payFromCash)
                .onSuccess { refreshStock(id); events.send(ProductEditEvent.StockUpdated) }
                .onFailure { events.send(ProductEditEvent.Error(it.message.orEmpty())) }
        }
    }

    fun adjustStock(countedStock: Int, reason: String) {
        val id = _form.value.id
        if (id == 0L) return
        viewModelScope.launch {
            stock.adjustTo(id, countedStock, reason)
                .onSuccess { refreshStock(id); events.send(ProductEditEvent.StockUpdated) }
                .onFailure { events.send(ProductEditEvent.Error(it.message.orEmpty())) }
        }
    }

    fun writeOffStock(qty: Int, reason: String) {
        val id = _form.value.id
        if (id == 0L) return
        viewModelScope.launch {
            stock.writeOff(id, qty, reason)
                .onSuccess { refreshStock(id); events.send(ProductEditEvent.StockUpdated) }
                .onFailure { events.send(ProductEditEvent.Error(it.message.orEmpty())) }
        }
    }

    private suspend fun refreshStock(id: Long) {
        catalog.getProduct(id)?.let { product ->
            _form.value = _form.value.copy(stock = product.stock, costPrice = product.costPrice)
        }
    }
}
