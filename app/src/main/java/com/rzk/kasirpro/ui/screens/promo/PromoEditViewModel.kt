package com.rzk.kasirpro.ui.screens.promo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.data.local.entity.PromoEntity
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.ProductWithCategory
import com.rzk.kasirpro.data.model.PromoScope
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
import java.time.LocalDate
import java.time.ZoneId

/** Local midnight today, the anchor both date fields are expressed against. */
private fun todayStart(): Long = LocalDate.now(ZoneId.systemDefault())
    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** One day minus a millisecond — turns an end *date* into an inclusive end *instant*. */
private const val END_OF_DAY_OFFSET = 86_399_999L

/**
 * [startAt] and [endAt] are both midnight-anchored here; the inclusive end instant is
 * only applied when saving, so re-editing a promo can't creep its end date forward.
 */
data class PromoForm(
    val id: Long = 0,
    val name: String = "",
    val discountType: DiscountType = DiscountType.PERCENT,
    val value: Long = 10,
    val scope: PromoScope = PromoScope.ALL_PRODUCTS,
    val categoryId: Long? = null,
    val productId: Long? = null,
    val startAt: Long = todayStart(),
    val endAt: Long = todayStart() + 7 * 86_400_000L,
    val days: Set<Int> = emptySet(),
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val minQty: Int = 1,
    val maxDiscount: Long = 0,
    val isActive: Boolean = true
) {
    val canSave: Boolean
        get() = name.isNotBlank() && value > 0 && endAt >= startAt &&
            (scope != PromoScope.CATEGORY || categoryId != null) &&
            (scope != PromoScope.PRODUCT || productId != null)
}

sealed interface PromoEditEvent {
    data object Saved : PromoEditEvent
    data class Error(val message: String) : PromoEditEvent
}

class PromoEditViewModel(
    container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val promos = container.promoRepository
    private val promoId: Long = savedStateHandle.get<Long>(Routes.ARG_PROMO_ID) ?: 0L

    private val _form = MutableStateFlow(PromoForm())
    val form: StateFlow<PromoForm> = _form.asStateFlow()

    private val events = Channel<PromoEditEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    data class Support(
        val categories: List<CategoryEntity> = emptyList(),
        val products: List<ProductWithCategory> = emptyList(),
        val settings: SettingsEntity = SettingsEntity()
    )

    val support: StateFlow<Support> = combine(
        container.catalogRepository.observeCategories(),
        container.catalogRepository.observeProducts(),
        container.settingsRepository.settings
    ) { categories, products, settings -> Support(categories, products, settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Support())

    init {
        if (promoId != 0L) {
            viewModelScope.launch {
                promos.getById(promoId)?.let { promo ->
                    _form.value = PromoForm(
                        id = promo.id,
                        name = promo.name,
                        discountType = promo.discountType,
                        value = promo.value,
                        scope = promo.scope,
                        categoryId = promo.categoryId,
                        productId = promo.productId,
                        startAt = promo.startAt,
                        endAt = promo.endAt - END_OF_DAY_OFFSET,
                        days = promo.daysOfWeek.split(',')
                            .mapNotNull { it.trim().toIntOrNull() }
                            .toSet(),
                        startMinute = promo.startMinuteOfDay,
                        endMinute = promo.endMinuteOfDay,
                        minQty = promo.minQty,
                        maxDiscount = promo.maxDiscountAmount,
                        isActive = promo.isActive
                    )
                }
            }
        }
    }

    fun update(transform: (PromoForm) -> PromoForm) { _form.value = transform(_form.value) }

    fun toggleDay(isoDay: Int) = update { current ->
        current.copy(
            days = if (isoDay in current.days) current.days - isoDay else current.days + isoDay
        )
    }

    fun save() {
        val form = _form.value
        if (!form.canSave) return
        viewModelScope.launch {
            val entity = PromoEntity(
                id = form.id,
                name = form.name.trim(),
                discountType = form.discountType,
                value = form.value,
                scope = form.scope,
                categoryId = form.categoryId.takeIf { form.scope == PromoScope.CATEGORY },
                productId = form.productId.takeIf { form.scope == PromoScope.PRODUCT },
                startAt = form.startAt,
                // Push the end to the last millisecond of the chosen day, otherwise a promo
                // "ending today" would stop applying at midnight this morning.
                endAt = form.endAt + END_OF_DAY_OFFSET,
                daysOfWeek = form.days.sorted().joinToString(","),
                startMinuteOfDay = form.startMinute,
                endMinuteOfDay = form.endMinute,
                minQty = form.minQty.coerceAtLeast(1),
                maxDiscountAmount = form.maxDiscount,
                isActive = form.isActive
            )
            promos.save(entity)
                .onSuccess { events.send(PromoEditEvent.Saved) }
                .onFailure { events.send(PromoEditEvent.Error(it.message.orEmpty())) }
        }
    }
}
