package com.example.monitor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.monitor.viewmodel.AuthState
import com.example.monitor.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etCode = findViewById<EditText>(R.id.etCode)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSendCode = findViewById<Button>(R.id.btnSendCode)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnSendCode.setOnClickListener {
            val email = etEmail.text.toString()
            if (email.isNotBlank()) {
                viewModel.sendCode(email)
            } else {
                Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val code = etCode.text.toString()
            val pass = etPassword.text.toString()
            if (name.isNotBlank() && email.isNotBlank() && code.isNotBlank() && pass.isNotBlank()) {
                viewModel.register(email, pass, name, code)
            } else {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_SHORT).show()
                        if (state.message == "注册成功") {
                            finish() // 返回登录页面
                        }
                        viewModel.resetState()
                    }
                    is AuthState.Error -> {
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
}