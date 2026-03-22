package com.example.monitor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.monitor.network.BatchDeleteRequest
import com.example.monitor.network.DetectBatchRequest
import com.example.monitor.network.ImageItem
import com.example.monitor.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImagesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ImageAdapter
    private val images = mutableListOf<ImageItem>()

    private var currentPage = 1
    private val pageSize = 10
    private var totalPages = 1
    private var currentKeyword = ""

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnToggleSelect: Button
    private lateinit var btnBatchDetect: Button
    private lateinit var btnBatchDelete: Button
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var tvPageInfo: TextView
    private lateinit var btnRefresh: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_images, container, false)
        
        etSearch = view.findViewById(R.id.etSearch)
        btnSearch = view.findViewById(R.id.btnSearch)
        btnToggleSelect = view.findViewById(R.id.btnToggleSelect)
        btnBatchDetect = view.findViewById(R.id.btnBatchDetect)
        btnBatchDelete = view.findViewById(R.id.btnBatchDelete)
        btnPrevPage = view.findViewById(R.id.btnPrevPage)
        btnNextPage = view.findViewById(R.id.btnNextPage)
        tvPageInfo = view.findViewById(R.id.tvPageInfo)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)

        adapter = ImageAdapter(images)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = adapter

        setupListeners()
        fetchImages()

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchImages()
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            currentKeyword = etSearch.text.toString().trim()
            currentPage = 1
            fetchImages()
        }

        btnRefresh.setOnClickListener {
            fetchImages()
        }

        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                fetchImages()
            }
        }

        btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                fetchImages()
            }
        }

        btnToggleSelect.setOnClickListener {
            adapter.toggleSelectionMode()
            if (adapter.isSelectionMode) {
                btnToggleSelect.text = "取消选择"
                btnBatchDetect.visibility = View.VISIBLE
                btnBatchDelete.visibility = View.VISIBLE
            } else {
                btnToggleSelect.text = "批量操作"
                btnBatchDetect.visibility = View.GONE
                btnBatchDelete.visibility = View.GONE
            }
        }

        btnBatchDelete.setOnClickListener {
            val selected = adapter.selectedItems.toList()
            if (selected.isEmpty()) {
                Toast.makeText(context, "请先选择图像", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            AlertDialog.Builder(requireContext())
                .setTitle("确认批量删除")
                .setMessage("您确定要删除选中的 ${selected.size} 张图片吗？")
                .setPositiveButton("删除") { _, _ ->
                    batchDeleteImages(selected)
                }
                .setNegativeButton("取消", null)
                .show()
        }

        btnBatchDetect.setOnClickListener {
            val selected = adapter.selectedItems.toList()
            if (selected.isEmpty()) {
                Toast.makeText(context, "请先选择图像", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            batchDetectImages(selected)
        }
    }

    private fun fetchImages() {
        progressBar.visibility = View.VISIBLE
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(context, "未登录", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val keywordParam = if (currentKeyword.isEmpty()) null else currentKeyword
                val response = RetrofitClient.apiService.listImages("Bearer $token", currentPage, pageSize, null, keywordParam, null)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.ok == true) {
                            images.clear()
                            body.data?.list?.let { images.addAll(it) }
                            adapter.notifyDataSetChanged()

                            val total = body.data?.total ?: 0
                            totalPages = if (total == 0) 1 else Math.ceil(total.toDouble() / pageSize).toInt()
                            tvPageInfo.text = "$currentPage / $totalPages"
                        } else {
                            Toast.makeText(context, "获取失败: ${body?.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "请求失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun batchDeleteImages(imageIds: List<Long>) {
        progressBar.visibility = View.VISIBLE
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.batchDeleteImages("Bearer $token", BatchDeleteRequest(imageIds))
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.ok == true) {
                        Toast.makeText(context, "批量删除成功", Toast.LENGTH_SHORT).show()
                        adapter.toggleSelectionMode() // Exit selection mode
                        btnToggleSelect.text = "批量操作"
                        btnBatchDetect.visibility = View.GONE
                        btnBatchDelete.visibility = View.GONE
                        fetchImages()
                    } else {
                        Toast.makeText(context, "批量删除失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun batchDetectImages(imageIds: List<Long>) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.detectBatch(DetectBatchRequest(imageIds))
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(context, "批量检测成功", Toast.LENGTH_SHORT).show()
                        adapter.toggleSelectionMode() // Exit selection mode
                        btnToggleSelect.text = "批量操作"
                        btnBatchDetect.visibility = View.GONE
                        btnBatchDelete.visibility = View.GONE
                        fetchImages()
                    } else {
                        Toast.makeText(context, "批量检测失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun triggerImageUpload() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let { uri ->
                uploadImage(uri)
            }
        }
    }

    private fun uploadImage(uri: android.net.Uri) {
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""
        if (token.isEmpty()) {
            Toast.makeText(context, "未登录，无法上传", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        Toast.makeText(context, "开始上传...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), bytes)
                    val multipartBody = okhttp3.MultipartBody.Part.createFormData("file", "upload.jpg", requestBody)

                    val response = RetrofitClient.apiService.uploadImage("Bearer $token", multipartBody)

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        if (response.isSuccessful && response.body()?.ok == true) {
                            Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                            fetchImages() // Refresh the list
                        } else {
                            Toast.makeText(context, "上传失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "无法读取文件", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "上传异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }
}
