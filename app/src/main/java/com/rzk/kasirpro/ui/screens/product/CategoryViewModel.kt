package com.rzk.kasirpro.ui.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(container: AppContainer) : ViewModel() {

    private val catalog = container.catalogRepository

    val categories: StateFlow<List<CategoryEntity>> = catalog.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, colorArgb: Int) = viewModelScope.launch {
        catalog.createCategory(CategoryEntity(name = name.trim(), colorArgb = colorArgb))
    }

    fun update(category: CategoryEntity) = viewModelScope.launch {
        catalog.updateCategory(category)
    }

    fun delete(category: CategoryEntity) = viewModelScope.launch {
        catalog.deleteCategory(category)
    }
}
