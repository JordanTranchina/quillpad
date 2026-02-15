package org.qosp.notes.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.qosp.notes.data.repo.IdMappingRepository
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.data.repo.NoteRepositoryImpl

object RepositoryModule {

    val repoModule = module {
        includes(DatabaseModule.dbModule)

        singleOf(::NoteRepositoryImpl) bind NoteRepository::class
        singleOf(::IdMappingRepository)
    }
}
