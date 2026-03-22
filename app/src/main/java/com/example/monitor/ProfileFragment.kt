package com.example.monitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.monitor.network.RetrofitClient
import com.example.monitor.network.UpdatePasswordRequest
import com.example.monitor.network.UpdateProfileRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var ivAvatar: ImageView
    private lateinit var btnEditAvatar: ImageButton
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button

    private var token: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        ivAvatar = view.findViewById(R.id.ivAvatar)
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar)
        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        btnSave = view.findViewById(R.id.btnSave)
        btnLogout = view.findViewById(R.id.btnLogout)

        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""

        setupListeners()
        loadUserInfo()

        return view
    }

    private fun setupListeners() {
        btnLogout.setOnClickListener {
            performLogout()
        }

        btnEditAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_AVATAR_REQUEST)
        }

        btnSave.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun loadUserInfo() {
        if (token.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getUserInfo("Bearer $token")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.ok == true) {
                        val data = response.body()?.data
                        etName.setText(data?.name)
                        etEmail.setText(data?.email)

                        val avatarUrl = data?.avatarUrl
                        if (!avatarUrl.isNullOrEmpty()) {
                            val baseUrl = "http://10.0.2.2:7022"
                            var finalUrl = if (avatarUrl.startsWith("http")) avatarUrl else baseUrl + avatarUrl
                            finalUrl = finalUrl.replace("127.0.0.1", "10.0.2.2")

                            Glide.with(this@ProfileFragment)
                                .load(finalUrl)
                                .transform(CircleCrop())
                                .placeholder(R.drawable.avatar_placeholder)
                                .into(ivAvatar)
                        }
                    } else {
                        Toast.makeText(context, "加载用户信息失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveProfileChanges() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()

        if (name.isEmpty() && email.isEmpty() && newPassword.isEmpty()) {
            Toast.makeText(context, "没有需要保存的修改", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update profile (name, email)
                if (name.isNotEmpty() || email.isNotEmpty()) {
                    val request = UpdateProfileRequest(
                        name = if (name.isNotEmpty()) name else null,
                        email = if (email.isNotEmpty()) email else null,
                        password = if (email.isNotEmpty()) currentPassword else null
                    )
                    
                    val response = RetrofitClient.apiService.updateProfile("Bearer $token", request)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.ok == true) {
                            Toast.makeText(context, "基本信息更新成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "信息更新失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Update password
                if (newPassword.isNotEmpty()) {
                    if (currentPassword.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "修改密码需要提供当前密码", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val pwdRequest = UpdatePasswordRequest(currentPassword, newPassword)
                    val pwdResponse = RetrofitClient.apiService.updatePassword("Bearer $token", pwdRequest)
                    
                    withContext(Dispatchers.Main) {
                        if (pwdResponse.isSuccessful && pwdResponse.body()?.ok == true) {
                            Toast.makeText(context, "密码修改成功，请重新登录", Toast.LENGTH_LONG).show()
                            logoutLocal()
                        } else {
                            Toast.makeText(context, "密码修改失败: ${pwdResponse.body()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AVATAR_REQUEST && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let { uri ->
                uploadAvatar(uri)
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), bytes)
                    val multipartBody = okhttp3.MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)

                    val response = RetrofitClient.apiService.updateAvatar("Bearer $token", multipartBody)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.ok == true) {
                            Toast.makeText(context, "头像上传成功", Toast.LENGTH_SHORT).show()
                            loadUserInfo()
                        } else {
                            Toast.makeText(context, "头像上传失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "上传异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performLogout() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.apiService.logout("Bearer $token")
            } catch (e: Exception) {
                // Ignore errors on logout
            }
            withContext(Dispatchers.Main) {
                logoutLocal()
            }
        }
    }

    private fun logoutLocal() {
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("token").apply()

        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        private const val PICK_AVATAR_REQUEST = 1002
    }
}
