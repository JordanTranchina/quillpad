package org.qosp.notes.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.qosp.notes.data.dao.IdMappingDao
import org.qosp.notes.data.dao.NoteDao
import org.qosp.notes.data.model.IdMapping
import org.qosp.notes.data.model.NoteEntity

@Database(
    entities = [
        NoteEntity::class,
        IdMapping::class,
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract val noteDao: NoteDao
    abstract val idMappingDao: IdMappingDao

    companion object {
        const val DB_NAME = "notes_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.apply {
                    execSQL("ALTER TABLE notes ADD COLUMN isCompactPreview INTEGER NOT NULL DEFAULT (0)")
                }
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.apply {
                    execSQL("ALTER TABLE notes ADD COLUMN screenAlwaysOn INTEGER NOT NULL DEFAULT (0)")
                }
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.apply {
                    execSQL("ALTER TABLE cloud_ids ADD COLUMN storageUri TEXT")
                }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.apply {
                    execSQL("CREATE INDEX IF NOT EXISTS cloud_ids_id_index ON cloud_ids (localNoteId)")
                    execSQL("CREATE INDEX IF NOT EXISTS cloud_ids_id_provider_index ON cloud_ids (localNoteId, provider)")
                }
            }
        }
    }
}
