package com.rzk.kasirpro.data.local

import androidx.room.withTransaction
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.local.entity.StockMovementEntity
import com.rzk.kasirpro.data.model.StockMovementType

/**
 * First-run content. A brand-new POS with an empty product grid is impossible to evaluate,
 * so we ship a small realistic catalogue that the owner can delete in one tap from Settings.
 */
object DatabaseSeeder {

    private val categories = listOf(
        CategoryEntity(name = "Food", colorArgb = 0xFFEF6C00.toInt(), iconKey = "food", sortOrder = 1),
        CategoryEntity(name = "Drinks", colorArgb = 0xFF0288D1.toInt(), iconKey = "drink", sortOrder = 2),
        CategoryEntity(name = "Snacks", colorArgb = 0xFF7B1FA2.toInt(), iconKey = "snack", sortOrder = 3),
        CategoryEntity(name = "Household", colorArgb = 0xFF2E7D32.toInt(), iconKey = "home", sortOrder = 4),
        CategoryEntity(name = "Toiletries", colorArgb = 0xFFC2185B.toInt(), iconKey = "care", sortOrder = 5)
    )

    /** name, sku, barcode, categoryIndex, cost, price, stock, minStock, unit */
    private val products = listOf(
        Seed("Fried Rice", "FD-001", "8991002101010", 0, 9_000, 18_000, 40, 10, "plate"),
        Seed("Fried Noodles", "FD-002", "8991002101027", 0, 8_500, 17_000, 35, 10, "plate"),
        Seed("Chicken Satay", "FD-003", "8991002101034", 0, 14_000, 25_000, 24, 8, "portion"),
        Seed("Meatball Soup", "FD-004", "8991002101041", 0, 10_000, 20_000, 30, 10, "bowl"),
        Seed("Iced Tea", "DR-001", "8991002102017", 1, 2_000, 6_000, 120, 24, "glass"),
        Seed("Iced Coffee", "DR-002", "8991002102024", 1, 5_000, 15_000, 80, 20, "glass"),
        Seed("Mineral Water 600ml", "DR-003", "8886008101053", 1, 2_500, 5_000, 96, 24, "bottle"),
        Seed("Orange Juice", "DR-004", "8991002102048", 1, 6_000, 16_000, 45, 12, "glass"),
        Seed("Potato Chips 68g", "SN-001", "8992753101019", 2, 7_500, 11_000, 60, 15, "pack"),
        Seed("Chocolate Wafer", "SN-002", "8992753101026", 2, 4_000, 7_000, 75, 20, "pack"),
        Seed("Peanut Crackers", "SN-003", "8992753101033", 2, 3_500, 6_500, 8, 15, "pack"),
        Seed("Instant Noodles", "HH-001", "8998866200011", 3, 2_800, 4_000, 150, 30, "pack"),
        Seed("Cooking Oil 1L", "HH-002", "8998866200028", 3, 16_000, 21_000, 24, 8, "bottle"),
        Seed("Dish Soap 400ml", "HH-003", "8998866200035", 3, 9_000, 14_000, 4, 10, "bottle"),
        Seed("Shampoo Sachet", "TL-001", "8999999500016", 4, 700, 1_500, 200, 50, "sachet"),
        Seed("Bath Soap Bar", "TL-002", "8999999500023", 4, 3_200, 5_500, 40, 12, "pcs"),
        Seed("Toothpaste 190g", "TL-003", "8999999500030", 4, 12_000, 18_500, 0, 6, "tube")
    )

    suspend fun seed(db: KasirDatabase) {
        val settingsDao = db.settingsDao()
        if (settingsDao.get() == null) settingsDao.upsert(SettingsEntity())

        val categoryDao = db.categoryDao()
        val categoryIds = categoryDao.insertAll(categories)

        val now = System.currentTimeMillis()
        val productDao = db.productDao()
        val stockDao = db.stockDao()

        products.forEach { seed ->
            val entity = ProductEntity(
                name = seed.name,
                sku = seed.sku,
                barcode = seed.barcode,
                categoryId = categoryIds.getOrNull(seed.categoryIndex)?.takeIf { it > 0 },
                costPrice = seed.cost,
                sellPrice = seed.price,
                stock = seed.stock,
                minStock = seed.minStock,
                unit = seed.unit,
                createdAt = now,
                updatedAt = now
            )
            val id = productDao.insert(entity)
            if (id > 0 && seed.stock > 0) {
                stockDao.insert(
                    StockMovementEntity(
                        productId = id,
                        productName = seed.name,
                        type = StockMovementType.INITIAL,
                        qty = seed.stock,
                        stockBefore = 0,
                        stockAfter = seed.stock,
                        unitCost = seed.cost,
                        note = "Opening balance",
                        createdAt = now
                    )
                )
            }
        }
    }

    /**
     * Wipes catalogue + history but keeps store settings. Child tables go first so the
     * foreign keys stay satisfied at every step of the transaction.
     */
    suspend fun clearAll(db: KasirDatabase) {
        db.withTransaction {
            val writable = db.openHelper.writableDatabase
            listOf(
                "DELETE FROM sale_payments",
                "DELETE FROM sale_items",
                "DELETE FROM sales",
                "DELETE FROM stock_movements",
                "DELETE FROM cash_flows",
                "DELETE FROM shifts",
                "DELETE FROM promos",
                "DELETE FROM products",
                "DELETE FROM categories",
                "UPDATE settings SET invoiceSequence = 0, invoiceDateKey = '' WHERE id = 1"
            ).forEach(writable::execSQL)
        }
    }

    private data class Seed(
        val name: String,
        val sku: String,
        val barcode: String,
        val categoryIndex: Int,
        val cost: Long,
        val price: Long,
        val stock: Int,
        val minStock: Int,
        val unit: String
    )
}
