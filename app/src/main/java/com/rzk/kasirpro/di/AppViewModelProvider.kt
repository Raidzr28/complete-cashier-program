package com.rzk.kasirpro.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rzk.kasirpro.KasirApplication
import com.rzk.kasirpro.ui.screens.cashflow.CashFlowViewModel
import com.rzk.kasirpro.ui.screens.dashboard.DashboardViewModel
import com.rzk.kasirpro.ui.screens.history.HistoryViewModel
import com.rzk.kasirpro.ui.screens.history.SaleDetailViewModel
import com.rzk.kasirpro.ui.screens.pos.PosViewModel
import com.rzk.kasirpro.ui.screens.product.CategoryViewModel
import com.rzk.kasirpro.ui.screens.product.ProductEditViewModel
import com.rzk.kasirpro.ui.screens.product.ProductListViewModel
import com.rzk.kasirpro.ui.screens.promo.PromoEditViewModel
import com.rzk.kasirpro.ui.screens.promo.PromoListViewModel
import com.rzk.kasirpro.ui.screens.receipt.ReceiptViewModel
import com.rzk.kasirpro.ui.screens.report.ReportViewModel
import com.rzk.kasirpro.ui.screens.report.StatisticsViewModel
import com.rzk.kasirpro.ui.screens.settings.SettingsViewModel
import com.rzk.kasirpro.ui.screens.shift.ShiftViewModel
import com.rzk.kasirpro.ui.screens.stock.StockMovementViewModel

/**
 * Single factory for every ViewModel in the app.
 *
 * Each screen calls `viewModel(factory = AppViewModelProvider)`. Screens that take a route
 * argument pull it from [createSavedStateHandle], which reads the nav arguments of the
 * back-stack entry that owns the ViewModel.
 */
val AppViewModelProvider = viewModelFactory {
    initializer { DashboardViewModel(container()) }
    initializer { PosViewModel(container()) }
    initializer { ReceiptViewModel(container(), createSavedStateHandle()) }
    initializer { ProductListViewModel(container()) }
    initializer { ProductEditViewModel(container(), createSavedStateHandle()) }
    initializer { CategoryViewModel(container()) }
    initializer { StockMovementViewModel(container()) }
    initializer { CashFlowViewModel(container()) }
    initializer { ReportViewModel(container()) }
    initializer { StatisticsViewModel(container()) }
    initializer { HistoryViewModel(container()) }
    initializer { SaleDetailViewModel(container(), createSavedStateHandle()) }
    initializer { ShiftViewModel(container()) }
    initializer { PromoListViewModel(container()) }
    initializer { PromoEditViewModel(container(), createSavedStateHandle()) }
    initializer { SettingsViewModel(container()) }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KasirApplication).container
