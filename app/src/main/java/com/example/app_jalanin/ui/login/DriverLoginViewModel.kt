package com.example.app_jalanin.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_jalanin.data.AppDatabase
import com.example.app_jalanin.data.local.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DriverLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String, role: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            val result = repository.login(username, password, role)

            _loginState.value = if (result.isSuccess) {
                val user = result.getOrNull()!!
                LoginState.Success(user.username, user.role, user.fullName)
            } else {
                LoginState.Error(result.exceptionOrNull()?.message ?: "Login gagal")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val username: String, val role: String, val fullName: String?) : LoginState()
    data class Error(val message: String) : LoginState()
}

