package com.rzk.kasirpro.ui.screens.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.local.entity.PromoEntity
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.ProductWithCategory
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.data.repository.CheckoutRequest
import com.rzk.kasirpro.data.repository.CheckoutResult
import com.rzk.kasirpro.di.AppContainer
import com.rzk.kasirpro.domain.CartCalculator
import com.rzk.kasirpro.domain.CartLine
import com.rzk.kasirpro.domain.CartTotals
import com.rzk.kasirpro.domain.PromoEngine
import com.rzk.kasirpro.domain.TenderLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Cart contents before promos are applied — the part the cashier actually edits. */
private data class CartEntry(
    val product: ProductEntity,
    val qty: Int,
    val manualDiscount: Long = 0,
    val note: String = ""
)

data class PosUiState(
    val query: String = "",
    val selectedCategoryId: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductWithCategory> = emptyList(),
    val lines: List<CartLine> = emptyList(),
    val totals: CartTotals = CartTotals(),
    val settings: SettingsEntity = SettingsEntity(),
    val orderDiscountInput: Long = 0,
    val orderDiscountType: DiscountType = DiscountType.AMOUNT,
    val customerName: String = "",
    val orderNote: String = "",
    val heldOrders: List<SaleWithDetails> = emptyList(),
    val livePromos: List<PromoEntity> = emptyList(),
    val resumedHeldSaleId: Long? = null,
    val isProcessing: Boolean = false
) {
    val isCartEmpty: Boolean get() = lines.isEmpty()
}

sealed interface PosEvent {
    data class Completed(val saleId: Long) : PosEvent
    data class InsufficientStock(val productName: String, val available: Int) : PosEvent
    data class ProductNotFound(val barcode: String) : PosEvent
    data class Error(val message: String) : PosEvent
    data object Underpaid : PosEvent
    data object OrderHeld : PosEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModel(private val container: AppContainer) : ViewModel() {

    private val catalog = container.catalogRepository
    private val sales = container.salesRepository
    private val promos = container.promoRepository
    private val settingsRepo = container.settingsRepository

    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<Long?>(null)
    private val entries = MutableStateFlow<List<CartEntry>>(emptyList())
    private val orderDiscount = MutableStateFlow(0L to DiscountType.AMOUNT)
    private val customer = MutableStateFlow("" to "")
    private val resumedHeldId = MutableStateFlow<Long?>(null)
    private val processing = MutableStateFlow(false)

    private val events = Channel<PosEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    /**
     * Ticks once a minute so a happy-hour promo starts and stops applying on its own,
     * without the cashier having to leave and re-enter the screen.
     */
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    private val livePromos = clock.flatMapLatest { now -> promos.observeLive(now) }

    private val productFeed = combine(query, categoryFilter) { q, cat -> q to cat }
        .flatMapLatest { (q, cat) -> catalog.search(q, cat) }

    private val cartFeed = combine(entries, livePromos, clock) { list, promoList, now ->
        // Promos are recomputed from scratch on every cart change: quantity thresholds mean
        // adding one more unit can newly qualify a line for a discount.
        list.map { entry ->
            CartLine(
                product = entry.product,
                qty = entry.qty,
                manualDiscount = entry.manualDiscount,
                note = entry.note,
                promo = PromoEngine.bestFor(entry.product, entry.qty, promoList, now)
            )
        }
    }

    private val catalogFeed = combine(
        productFeed,
        catalog.observeCategories(),
        sales.observeHeld(),
        livePromos
    ) { products, categories, held, promoList ->
        CatalogSignals(products, categories, held, promoList)
    }

    private val cartSignals = combine(
        cartFeed,
        settingsRepo.settings,
        orderDiscount,
        customer,
        resumedHeldId
    ) { lines, settings, discount, cust, heldId ->
        CartSignals(
            lines = lines,
            settings = settings,
            totals = CartCalculator.totals(lines, settings, discount.first, discount.second),
            discountInput = discount.first,
            discountType = discount.second,
            customerName = cust.first,
            note = cust.second,
            resumedHeldId = heldId
        )
    }

    val uiState: StateFlow<PosUiState> = combine(
        catalogFeed,
        cartSignals,
        query,
        categoryFilter,
        processing
    ) { cat, cart, q, categoryId, busy ->
        PosUiState(
            query = q,
            selectedCategoryId = categoryId,
            categories = cat.categories,
            products = cat.products,
            lines = cart.lines,
            totals = cart.totals,
            settings = cart.settings,
            orderDiscountInput = cart.discountInput,
            orderDiscountType = cart.discountType,
            customerName = cart.customerName,
            orderNote = cart.note,
            heldOrders = cat.held,
            livePromos = cat.promos,
            resumedHeldSaleId = cart.resumedHeldId,
            isProcessing = busy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PosUiState())

    // ------------------------------------------------------------------ catalogue

    fun setQuery(value: String) { query.value = value }

    fun setCategory(id: Long?) { categoryFilter.value = id }

    /** Scanner result: exact barcode match goes straight into the cart. */
    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = catalog.findByBarcode(barcode)
            if (product == null) {
                events.send(PosEvent.ProductNotFound(barcode))
            } else {
                addProduct(product)
            }
        }
    }

    // ------------------------------------------------------------------ cart

    fun addProduct(product: ProductEntity) {
        val settings = uiState.value.settings
        val current = entries.value
        val existing = current.firstOrNull { it.product.id == product.id }
        val nextQty = (existing?.qty ?: 0) + 1

        if (settings.blockSaleWhenOutOfStock && product.trackStock && nextQty > product.stock) {
            viewModelScope.launch {
                events.send(PosEvent.InsufficientStock(product.name, product.stock))
            }
            return
        }

        entries.value = if (existing == null) {
            current + CartEntry(product, 1)
        } else {
            current.map { if (it.product.id == product.id) it.copy(qty = nextQty) else it }
        }
    }

    fun setQuantity(productId: Long, qty: Int) {
        entries.value = if (qty <= 0) {
            entries.value.filterNot { it.product.id == productId }
        } else {
            entries.value.map { if (it.product.id == productId) it.copy(qty = qty) else it }
        }
    }

    fun setLineDiscount(productId: Long, discount: Long) {
        entries.value = entries.value.map {
            if (it.product.id == productId) it.copy(manualDiscount = discount.coerceAtLeast(0)) else it
        }
    }

    fun setLineNote(productId: Long, note: String) {
        entries.value = entries.value.map {
            if (it.product.id == productId) it.copy(note = note) else it
        }
    }

    fun removeLine(productId: Long) = setQuantity(productId, 0)

    fun clearCart() {
        entries.value = emptyList()
        orderDiscount.value = 0L to DiscountType.AMOUNT
        customer.value = "" to ""
        resumedHeldId.value = null
    }

    fun setOrderDiscount(value: Long, type: DiscountType) {
        orderDiscount.value = value.coerceAtLeast(0) to type
    }

    fun setCustomerName(name: String) { customer.value = name to customer.value.second }

    fun setOrderNote(note: String) { customer.value = customer.value.first to note }

    // ------------------------------------------------------------------ held orders

    fun holdOrder(label: String) {
        val state = uiState.value
        if (state.isCartEmpty) return
        viewModelScope.launch {
            sales.holdOrder(state.lines, state.totals, label)
                .onSuccess {
                    // The resumed original (if any) was already consumed into this new hold.
                    state.resumedHeldSaleId?.let { sales.discardHeld(it) }
                    clearCart()
                    events.send(PosEvent.OrderHeld)
                }
                .onFailure { events.send(PosEvent.Error(it.message.orEmpty())) }
        }
    }

    /** Pulls a parked cart back onto the screen, re-resolving each product's live price. */
    fun resumeHeld(sale: SaleWithDetails) {
        viewModelScope.launch {
            val restored = sale.items.mapNotNull { item ->
                val productId = item.productId ?: return@mapNotNull null
                val product = catalog.getProduct(productId) ?: return@mapNotNull null
                CartEntry(product, item.qty, item.lineDiscount, item.note)
            }
            entries.value = restored
            customer.value = sale.sale.customerName to sale.sale.note
            resumedHeldId.value = sale.sale.id
        }
    }

    fun discardHeld(saleId: Long) {
        viewModelScope.launch {
            sales.discardHeld(saleId)
            if (resumedHeldId.value == saleId) resumedHeldId.value = null
        }
    }

    // ------------------------------------------------------------------ checkout

    fun checkout(tenders: List<TenderLine>) {
        val state = uiState.value
        if (state.isCartEmpty || processing.value) return

        processing.value = true
        viewModelScope.launch {
            val result = sales.checkout(
                CheckoutRequest(
                    lines = state.lines,
                    totals = state.totals,
                    tenders = tenders,
                    orderDiscountInput = state.orderDiscountInput,
                    orderDiscountType = state.orderDiscountType,
                    customerName = state.customerName,
                    note = state.orderNote,
                    resumedHeldSaleId = state.resumedHeldSaleId
                )
            )
            processing.value = false

            when (result) {
                is CheckoutResult.Success -> {
                    clearCart()
                    events.send(PosEvent.Completed(result.sale.sale.id))
                }
                is CheckoutResult.InsufficientStock ->
                    events.send(PosEvent.InsufficientStock(result.productName, result.available))
                CheckoutResult.Underpaid -> events.send(PosEvent.Underpaid)
                CheckoutResult.EmptyCart -> Unit
                is CheckoutResult.Failure -> events.send(PosEvent.Error(result.message))
            }
        }
    }

    private data class CatalogSignals(
        val products: List<ProductWithCategory>,
        val categories: List<CategoryEntity>,
        val held: List<SaleWithDetails>,
        val promos: List<PromoEntity>
    )

    private data class CartSignals(
        val lines: List<CartLine>,
        val settings: SettingsEntity,
        val totals: CartTotals,
        val discountInput: Long,
        val discountType: DiscountType,
        val customerName: String,
        val note: String,
        val resumedHeldId: Long?
    )
}
