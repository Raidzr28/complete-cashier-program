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
    data class Error(val message: String) : ProductEditEvent
}

class ProductEditViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val catalog = container.catalogRepository
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

    fun onBarcodeScanned(code: String) = update { it.copy(barcode = code) }

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
}
