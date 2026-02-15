package org.qosp.notes.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.qosp.notes.App
import org.qosp.notes.BuildConfig
import org.qosp.notes.components.workers.BinCleaningWorker
import org.qosp.notes.ui.utils.ConnectionManager
import org.qosp.notes.ui.utils.Toaster


object UtilModule {

    val utilModule = module {
        includes(RepositoryModule.repoModule)

        workerOf(::BinCleaningWorker)

        singleOf(::ConnectionManager)
        singleOf(::Toaster)
    }
}
