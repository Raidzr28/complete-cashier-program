package com.rzk.kasirpro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rzk.kasirpro.data.local.dao.CashFlowDao
import com.rzk.kasirpro.data.local.dao.CategoryDao
import com.rzk.kasirpro.data.local.dao.ProductDao
import com.rzk.kasirpro.data.local.dao.PromoDao
import com.rzk.kasirpro.data.local.dao.SaleDao
import com.rzk.kasirpro.data.local.dao.SettingsDao
import com.rzk.kasirpro.data.local.dao.ShiftDao
import com.rzk.kasirpro.data.local.dao.StockDao
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.local.entity.CategoryEntity
import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.local.entity.PromoEntity
import com.rzk.kasirpro.data.local.entity.SaleEntity
import com.rzk.kasirpro.data.local.entity.SaleItemEntity
import com.rzk.kasirpro.data.local.entity.SalePaymentEntity
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.local.entity.ShiftEntity
import com.rzk.kasirpro.data.local.entity.StockMovementEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        PromoEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SalePaymentEntity::class,
        CashFlowEntity::class,
        ShiftEntity::class,
        StockMovementEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KasirDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun promoDao(): PromoDao
    abstract fun saleDao(): SaleDao
    abstract fun cashFlowDao(): CashFlowDao
    abstract fun shiftDao(): ShiftDao
    abstract fun stockDao(): StockDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DB_NAME = "kasir_pro.db"

        @Volatile
        private var instance: KasirDatabase? = null

        fun get(context: Context, scope: CoroutineScope): KasirDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext, scope).also { instance = it }
            }

        private fun build(context: Context, scope: CoroutineScope): KasirDatabase =
            Room.databaseBuilder(context, KasirDatabase::class.java, DB_NAME)
                // FK constraints are what keep sale_items from outliving their sale.
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seeding needs the DAOs, which aren't usable until the builder
                        // returns — so hop off onto a coroutine that reads `instance`.
                        scope.launch(Dispatchers.IO) {
                            instance?.let { DatabaseSeeder.seed(it) }
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    }
                })
                .build()
    }
}
