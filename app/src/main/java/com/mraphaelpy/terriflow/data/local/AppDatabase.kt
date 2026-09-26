package com.mraphaelpy.terriflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mraphaelpy.terriflow.data.local.converter.Converters
import com.mraphaelpy.terriflow.data.local.dao.*
import com.mraphaelpy.terriflow.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        TerritoryEntity::class,
        TerritoryEventEntity::class,
        NotificationEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun territoryDao(): TerritoryDao
    abstract fun territoryEventDao(): TerritoryEventDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "terriflow.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE territories ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE territories ADD COLUMN longitude REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE territories ADD COLUMN boundaryPoints TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN photoUrl TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE territories ADD COLUMN blockPolygons TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE territories ADD COLUMN currentResponsiblePhotoUrl TEXT")
            }
        }
    }
}
