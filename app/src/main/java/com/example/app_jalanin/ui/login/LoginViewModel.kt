package com.example.app_jalanin.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_jalanin.data.auth.AuthRepository
import com.example.app_jalanin.data.auth.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app)

    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val selectedRole = MutableStateFlow(UserRole.PENUMPANG)

    private val _loginSuccess = MutableStateFlow<Boolean?>(null)
    val loginSuccess: StateFlow<Boolean?> = _loginSuccess

    private val _lastUsername = MutableStateFlow<String?>(null)
    val lastUsername = _lastUsername.asStateFlow()
    private val _lastRole = MutableStateFlow<UserRole?>(null)
    val lastRole = _lastRole.asStateFlow()

    init {
        viewModelScope.launch { repo.ensureDummyPassenger() }
    }

    fun login() {
        viewModelScope.launch {
            val ok = repo.login(
                username.value.trim(),
                password.value,
                selectedRole.value
            )
            _loginSuccess.value = ok
            if (ok) {
                _lastUsername.value = username.value.trim()
                _lastRole.value = selectedRole.value
            }
        }
    }
}
