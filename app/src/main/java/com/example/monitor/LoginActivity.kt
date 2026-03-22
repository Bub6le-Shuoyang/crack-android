package com.example.monitor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.monitor.viewmodel.AuthState
import com.example.monitor.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check login status first
        val sharedPref = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""
        if (token.isNotEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()
            if (email.isNotBlank() && pass.isNotBlank()) {
                viewModel.login(email, pass)
            } else {
                Toast.makeText(this, "请输入邮箱和密码", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> { }
                    is AuthState.Success -> {
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                        
                        // Save token
                        val response = state.data as? com.example.monitor.network.LoginResponse
                        response?.token?.let { token ->
                            val sharedPref = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                            sharedPref.edit().putString("token", token).apply()
                        }

                        viewModel.resetState()
                        // 登录成功后跳转到主页面
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is AuthState.Error -> {
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
}