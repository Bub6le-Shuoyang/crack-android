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

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etCode = findViewById<EditText>(R.id.etCode)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val btnSendCode = findViewById<Button>(R.id.btnSendCode)
        val btnReset = findViewById<Button>(R.id.btnReset)

        btnSendCode.setOnClickListener {
            val email = etEmail.text.toString()
            if (email.isNotBlank()) {
                viewModel.sendCode(email)
            } else {
                Toast.makeText(this, "请输入注册邮箱", Toast.LENGTH_SHORT).show()
            }
        }

        btnReset.setOnClickListener {
            val email = etEmail.text.toString()
            val code = etCode.text.toString()
            val pass = etNewPassword.text.toString()
            if (email.isNotBlank() && code.isNotBlank() && pass.isNotBlank()) {
                viewModel.forgotPassword(email, code, pass)
            } else {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        Toast.makeText(this@ForgotPasswordActivity, state.message, Toast.LENGTH_SHORT).show()
                        if (state.message == "密码重置成功") {
                            finish() // 返回登录页面
                        }
                        viewModel.resetState()
                    }
                    is AuthState.Error -> {
                        Toast.makeText(this@ForgotPasswordActivity, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
}