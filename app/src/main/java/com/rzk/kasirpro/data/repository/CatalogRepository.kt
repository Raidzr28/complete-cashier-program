package com.rzk.kasirpro.data.repository

import androidx.room.withTransaction
import com.rzk.kasirpro.data.local.KasirDatabase
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.local.entity.StockMovementEntity
import com.rzk.kasirpro.data.model.ProductWithCategory
import com.rzk.kasirpro.data.model.StockMovementType
import com.rzk.kasirpro.data.model.StockValuation
import kotlinx.coroutines.flow.Flow

/** Products and categories: the catalogue the POS grid is built from. */
class CatalogRepository(private val db: KasirDatabase) {

    private val productDao = db.productDao()
    private val categoryDao = db.categoryDao()
    private val stockDao = db.stockDao()

    fun observeProducts(): Flow<List<ProductWithCategory>> = productDao.observeActive()

    fun observeAllProducts(): Flow<List<ProductWithCategory>> = productDao.observeAll()

    fun search(query: String, categoryId: Long?): Flow<List<ProductWithCategory>> =
        productDao.search(query.trim(), categoryId)

    fun observeProduct(id: Long): Flow<ProductWithCategory?> = productDao.observeById(id)

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeActive()

    fun observeLowStock(): Flow<List<ProductWithCategory>> = productDao.observeLowStock()

    fun observeLowStockCount(): Flow<Int> = productDao.observeLowStockCount()

    fun observeProductCount(): Flow<Int> = productDao.observeProductCount()

    fun observeStockValuation(): Flow<StockValuation> = productDao.observeStockValuation()

    suspend fun getProduct(id: Long): ProductEntity? = productDao.getById(id)

    /** Barcode lookup used by the scanner — an exact match, active products only. */
    suspend fun findByBarcode(barcode: String): ProductEntity? =
        productDao.getByBarcode(barcode.trim())

    /**
     * Creates a product and, when it starts with stock on hand, writes the matching
     * opening-balance movement so the audit trail is complete from day one.
     */
    suspend fun createProduct(product: ProductEntity): Result<Long> = runCatching {
        require(product.name.isNotBlank()) { "Product name is required" }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val id = productDao.insert(product.copy(createdAt = now, updatedAt = now))
            if (product.trackStock && product.stock > 0) {
                stockDao.insert(
                    StockMovementEntity(
                        productId = id,
                        productName = product.name,
                        type = StockMovementType.INITIAL,
                        qty = product.stock,
                        stockBefore = 0,
                        stockAfter = product.stock,
                        unitCost = product.costPrice,
                        note = "Opening balance",
                        createdAt = now
                    )
                )
            }
            id
        }
    }

    /**
     * Edits everything except stock. Stock only ever moves through
     * [StockRepository], so the movement ledger stays the single explanation of a balance.
     */
    suspend fun updateProduct(product: ProductEntity): Result<Unit> = runCatching {
        require(product.name.isNotBlank()) { "Product name is required" }
        val existing = productDao.getById(product.id) ?: error("Product not found")
        productDao.update(
            product.copy(
                stock = existing.stock,
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Soft delete — history keeps pointing at a real product name. */
    suspend fun archiveProduct(id: Long) =
        productDao.setActive(id, false, System.currentTimeMillis())

    suspend fun restoreProduct(id: Long) =
        productDao.setActive(id, true, System.currentTimeMillis())

    suspend fun deleteProductPermanently(product: ProductEntity): Result<Unit> = runCatching {
        db.withTransaction {
            stockDao.deleteForProduct(product.id)
            productDao.delete(product)
        }
    }

    // ------------------------------------------------------------- categories

    suspend fun createCategory(category: CategoryEntity): Result<Long> = runCatching {
        require(category.name.isNotBlank()) { "Category name is required" }
        val id = categoryDao.insert(category)
        if (id <= 0) error("A category with that name already exists")
        id
    }

    suspend fun updateCategory(category: CategoryEntity): Result<Unit> = runCatching {
        require(category.name.isNotBlank()) { "Category name is required" }
        categoryDao.update(category)
    }

    /** Products keep existing and fall back to "Uncategorised" via the FK's SET NULL. */
    suspend fun deleteCategory(category: CategoryEntity): Result<Int> = runCatching {
        val affected = categoryDao.productCount(category.id)
        categoryDao.delete(category)
        affected
    }
}
