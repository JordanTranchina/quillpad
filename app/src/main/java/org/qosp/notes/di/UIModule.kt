package org.qosp.notes.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.qosp.notes.ui.ActivityViewModel
import org.qosp.notes.ui.archive.ArchiveViewModel
import org.qosp.notes.ui.deleted.DeletedViewModel
import org.qosp.notes.ui.editor.EditorViewModel
import org.qosp.notes.ui.launcher.LauncherViewModel
import org.qosp.notes.ui.main.MainViewModel
import org.qosp.notes.ui.search.SearchViewModel
import org.qosp.notes.ui.settings.SettingsViewModel

object UIModule {
    val uiModule = module {
        viewModelOf(::EditorViewModel)
        viewModelOf(::ActivityViewModel)
        viewModelOf(::ArchiveViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::SearchViewModel)
        viewModelOf(::MainViewModel)
        viewModel { LauncherViewModel(androidApplication(), get()) }
        viewModelOf(::DeletedViewModel)
    }
}
