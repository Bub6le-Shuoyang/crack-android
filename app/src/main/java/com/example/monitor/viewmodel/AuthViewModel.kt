package com.example.monitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monitor.network.*
import com.example.monitor.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String, val data: Any? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.login(LoginRequest(email, pass))
                if (response.isSuccessful && response.body()?.token != null) {
                    _authState.value = AuthState.Success("登录成功", response.body())
                } else {
                    _authState.value = AuthState.Error(response.body()?.message ?: "登录失败")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun sendCode(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.sendCode(SendCodeRequest(email))
                if (response.isSuccessful && response.body()?.ok == true) {
                    _authState.value = AuthState.Success("验证码已发送")
                } else {
                    _authState.value = AuthState.Error(response.body()?.message ?: "发送失败")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun register(email: String, pass: String, name: String, code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.register(RegisterRequest(email, pass, name, code))
                if (response.isSuccessful && response.body()?.ok == true) {
                    _authState.value = AuthState.Success("注册成功")
                } else {
                    _authState.value = AuthState.Error(response.body()?.message ?: "注册失败")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun forgotPassword(email: String, code: String, newPass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.forgotPassword(ForgotPasswordRequest(email, code, newPass))
                if (response.isSuccessful && response.body()?.ok == true) {
                    _authState.value = AuthState.Success("密码重置成功")
                } else {
                    _authState.value = AuthState.Error(response.body()?.message ?: "重置失败")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "网络错误")
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}