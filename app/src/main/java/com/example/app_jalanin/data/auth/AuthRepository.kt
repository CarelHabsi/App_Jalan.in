package com.example.app_jalanin.data.auth

import android.content.Context

class AuthRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("auth_store", Context.MODE_PRIVATE)

    suspend fun ensureDummyPassenger() {
        if (!prefs.contains(KEY_USERNAME)) {
            val hash = PasswordHasher.sha256("jalanin_aja_dulu")
            prefs.edit()
                .putString(KEY_USERNAME, "user123")
                .putString(KEY_PASSWORD_HASH, hash)
                .putString(KEY_ROLE, UserRole.PENUMPANG.name)
                .apply()
        }
    }

    suspend fun login(username: String, password: String, selectedRole: UserRole): Boolean {
        val u = prefs.getString(KEY_USERNAME, null) ?: return false
        val ph = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val roleStr = prefs.getString(KEY_ROLE, null) ?: return false
        val role = runCatching { UserRole.valueOf(roleStr) }.getOrNull() ?: return false
        val passOk = ph == PasswordHasher.sha256(password)
        val userOk = u.equals(username, ignoreCase = false)
        val roleOk = role == selectedRole
        return userOk && passOk && roleOk
    }

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_ROLE = "role"
    }
}
