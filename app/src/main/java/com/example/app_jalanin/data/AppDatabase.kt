package com.example.app_jalanin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.app_jalanin.data.local.dao.UserDao
import com.example.app_jalanin.data.local.entity.User

@Database(
    entities = [User::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jalanin_database"
                )
                    .fallbackToDestructiveMigration()  // Reset DB jika ada perubahan schema (hanya untuk development)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


