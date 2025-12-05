package com.example.app_jalanin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.app_jalanin.data.local.dao.UserDao
import com.example.app_jalanin.data.local.entity.User

@Database(
    entities = [User::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Rename username column to email
                // SQLite doesn't support ALTER TABLE RENAME COLUMN directly in older versions
                // So we create a new table and copy data

                // 1. Create new table with email column
                database.execSQL("""
                    CREATE TABLE users_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        email TEXT NOT NULL,
                        password TEXT NOT NULL,
                        role TEXT NOT NULL,
                        fullName TEXT,
                        phoneNumber TEXT,
                        createdAt INTEGER NOT NULL,
                        synced INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 2. Copy data from old table (username -> email)
                database.execSQL("""
                    INSERT INTO users_new (id, email, password, role, fullName, phoneNumber, createdAt, synced)
                    SELECT id, username, password, role, fullName, phoneNumber, createdAt, synced
                    FROM users
                """.trimIndent())

                // 3. Drop old table
                database.execSQL("DROP TABLE users")

                // 4. Rename new table to users
                database.execSQL("ALTER TABLE users_new RENAME TO users")

                // 5. Create unique index on email
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jalanin_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
