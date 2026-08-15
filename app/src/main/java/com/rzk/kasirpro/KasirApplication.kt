package com.rzk.kasirpro

import android.app.Application
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.launch

class KasirApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Guarantees the pinned settings row exists before the first checkout tries to
        // mint an invoice number, even if Room's onCreate seed hasn't finished yet.
        container.applicationScope.launch {
            container.settingsRepository.ensureInitialised()
        }
    }
}
