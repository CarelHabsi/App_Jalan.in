package com.example.app_jalanin.data.local

import com.example.app_jalanin.data.local.dao.UserDao
import com.example.app_jalanin.data.local.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {

    /**
     * Register user baru
     */
    suspend fun registerUser(
        username: String,
        password: String,
        role: String,
        fullName: String? = null,
        phoneNumber: String? = null,
        email: String? = null
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                // Cek apakah username sudah ada
                val existing = userDao.getUserByUsername(username)
                if (existing != null) {
                    return@withContext Result.failure(Exception("Username sudah terdaftar"))
                }

                val user = User(
                    username = username,
                    password = password,  // TODO: Hash password untuk production
                    role = role,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    email = email
                )
                val userId = userDao.insertUser(user)
                Result.success(userId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Login user dengan validasi role
     */
    suspend fun login(username: String, password: String, role: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.login(username, password, role)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Username, password, atau role salah"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get user by username
     */
    suspend fun getUserByUsername(username: String): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserByUsername(username)
        }
    }

    /**
     * Get all users
     */
    suspend fun getAllUsers(): List<User> {
        return withContext(Dispatchers.IO) {
            userDao.getAllUsers()
        }
    }

    /**
     * Get users by role
     */
    suspend fun getUsersByRole(role: String): List<User> {
        return withContext(Dispatchers.IO) {
            userDao.getUsersByRole(role)
        }
    }

    /**
     * Delete user
     */
    suspend fun deleteUser(userId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                userDao.deleteUser(userId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update password
     */
    suspend fun updatePassword(username: String, newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                userDao.updatePassword(username, newPassword)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

