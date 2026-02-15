package org.qosp.notes.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.qosp.notes.data.AppDatabase

object DatabaseModule {

    val dbModule = module {
        single<AppDatabase> {
            Room.databaseBuilder(
                context = androidContext(),
                klass = AppDatabase::class.java,
                name = AppDatabase.DB_NAME
            )
                .fallbackToDestructiveMigration() // Since version bump to 6
                .build()
        }

        single { get<AppDatabase>().noteDao }
        single { get<AppDatabase>().idMappingDao }
    }

}
