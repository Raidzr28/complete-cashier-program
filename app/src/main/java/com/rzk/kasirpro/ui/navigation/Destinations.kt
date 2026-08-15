package com.rzk.kasirpro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import com.rzk.kasirpro.R

/** Every route in the app. String routes keep navigation independent of serialization. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val POS = "pos"
    const val PRODUCTS = "products"
    const val CASHFLOW = "cashflow"
    const val REPORTS = "reports"

    const val STATISTICS = "statistics"
    const val HISTORY = "history"
    const val SHIFT = "shift"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val PROMOS = "promos"
    const val MOVEMENTS = "movements"
    const val SCANNER = "scanner"

    private const val PRODUCT_EDIT_BASE = "product_edit"
    private const val PROMO_EDIT_BASE = "promo_edit"
    private const val SALE_DETAIL_BASE = "sale_detail"
    private const val RECEIPT_BASE = "receipt"

    const val PRODUCT_EDIT = "$PRODUCT_EDIT_BASE/{productId}"
    const val PROMO_EDIT = "$PROMO_EDIT_BASE/{promoId}"
    const val SALE_DETAIL = "$SALE_DETAIL_BASE/{saleId}"
    const val RECEIPT = "$RECEIPT_BASE/{saleId}"

    const val ARG_PRODUCT_ID = "productId"
    const val ARG_PROMO_ID = "promoId"
    const val ARG_SALE_ID = "saleId"

    /** 0 means "create new". */
    fun productEdit(productId: Long = 0L) = "$PRODUCT_EDIT_BASE/$productId"
    fun promoEdit(promoId: Long = 0L) = "$PROMO_EDIT_BASE/$promoId"
    fun saleDetail(saleId: Long) = "$SALE_DETAIL_BASE/$saleId"
    fun receipt(saleId: Long) = "$RECEIPT_BASE/$saleId"
}

/** Key handed back from the scanner screen through the previous entry's saved state. */
const val SCAN_RESULT_KEY = "scanned_barcode"

enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector
) {
    HOME(Routes.DASHBOARD, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    POS(Routes.POS, R.string.nav_pos, Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    PRODUCTS(Routes.PRODUCTS, R.string.nav_products, Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    CASHFLOW(Routes.CASHFLOW, R.string.nav_cashflow, Icons.Filled.Savings, Icons.Outlined.Savings),
    REPORTS(Routes.REPORTS, R.string.nav_reports, Icons.Filled.Assessment, Icons.Outlined.Assessment)
}
