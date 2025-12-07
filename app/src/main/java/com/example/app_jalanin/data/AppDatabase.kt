package com.example.app_jalanin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.app_jalanin.data.local.dao.UserDao
import com.example.app_jalanin.data.local.dao.RentalDao
import com.example.app_jalanin.data.local.dao.VehicleDao
import com.example.app_jalanin.data.local.entity.User
import com.example.app_jalanin.data.local.entity.Rental
import com.example.app_jalanin.data.model.Vehicle

@Database(
    entities = [User::class, Rental::class, Vehicle::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun rentalDao(): RentalDao
    abstract fun vehicleDao(): VehicleDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create rentals table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS rentals (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId INTEGER NOT NULL,
                        userEmail TEXT NOT NULL,
                        vehicleId TEXT NOT NULL,
                        vehicleName TEXT NOT NULL,
                        vehicleType TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        durationDays INTEGER NOT NULL,
                        durationHours INTEGER NOT NULL,
                        durationMinutes INTEGER NOT NULL,
                        durationMillis INTEGER NOT NULL,
                        totalPrice INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        overtimeFee INTEGER NOT NULL DEFAULT 0,
                        isWithDriver INTEGER NOT NULL DEFAULT 0,
                        deliveryAddress TEXT NOT NULL DEFAULT '',
                        deliveryLat REAL NOT NULL DEFAULT 0.0,
                        deliveryLon REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        synced INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_rentals_userId ON rentals(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_rentals_status ON rentals(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_rentals_createdAt ON rentals(createdAt)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add userEmail index if not exists
                database.execSQL("CREATE INDEX IF NOT EXISTS index_rentals_userEmail ON rentals(userEmail)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Schema validation fix - no actual changes needed
                // This just updates the version number to match current schema
                android.util.Log.d("AppDatabase", "✅ Migration 5 -> 6: Schema validation update")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Schema validation fix - no actual changes needed
                // This just updates the version number to match current schema
                android.util.Log.d("AppDatabase", "✅ Migration 6 -> 7: Schema validation update")
            }
        }

         private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create vehicles table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS vehicles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ownerId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        brand TEXT NOT NULL,
                        model TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        licensePlate TEXT NOT NULL,
                        transmission TEXT NOT NULL,
                        seats INTEGER,
                        engineCapacity TEXT,
                        pricePerHour REAL NOT NULL,
                        pricePerDay REAL NOT NULL,
                        pricePerWeek REAL NOT NULL,
                        features TEXT NOT NULL,
                        status TEXT NOT NULL,
                        statusReason TEXT,
                        locationLat REAL NOT NULL,
                        locationLon REAL NOT NULL,
                        locationAddress TEXT NOT NULL,
                        imageUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create indexes for vehicles
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vehicles_ownerId ON vehicles(ownerId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vehicles_status ON vehicles(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vehicles_type ON vehicles(type)")

                android.util.Log.d("AppDatabase", "✅ Migration 7 -> 8: Created vehicles table")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jalanin_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigration() // ✅ Allow database recreation for development
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * ✅ Clear database instance (for testing or after logout)
         */
        fun clearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                android.util.Log.d("AppDatabase", "Database instance cleared")
            }
        }
    }
}
