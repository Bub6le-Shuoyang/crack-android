package com.example.monitor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.monitor.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageDetailActivity : AppCompatActivity() {

    private var imageId: Long = -1L
    private lateinit var token: String
    private lateinit var boundingBoxView: BoundingBoxView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)

        imageId = intent.getLongExtra("IMAGE_ID", -1)
        if (imageId == -1L) {
            finish()
            return
        }

        val ivDetail = findViewById<ImageView>(R.id.ivDetail)
        val tvDetectionResult = findViewById<TextView>(R.id.tvDetectionResult)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnBack = findViewById<View>(R.id.btnBack)
        val btnDetect = findViewById<Button>(R.id.btnDetect)
        val btnDelete = findViewById<Button>(R.id.btnDelete)
        boundingBoxView = findViewById(R.id.boundingBoxView)

        val sharedPref = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""

        btnBack.setOnClickListener {
            finish()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }

        btnDetect.setOnClickListener {
            // Placeholder for detection API call if needed later
            Toast.makeText(this, "检测功能正在开发中", Toast.LENGTH_SHORT).show()
        }

        loadData(progressBar, ivDetail, tvDetectionResult)
    }

    private fun loadData(progressBar: ProgressBar, ivDetail: ImageView, tvDetectionResult: TextView) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getImageDetail("Bearer $token", imageId)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.ok == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            val baseUrl = "http://10.0.2.2:7022" // Adjust base URL
                            var url = if (data.accessUrl?.startsWith("http") == true) data.accessUrl else baseUrl + (data.accessUrl ?: data.filePath)
                            url = url.replace("127.0.0.1", "10.0.2.2")
                            
                            Glide.with(this@ImageDetailActivity)
                                .load(url)
                                .into(ivDetail)

                            val sb = StringBuilder()
                            sb.append("文件名: ${data.fileName}\n")
                            sb.append("是否检测: ${if (data.isDetected) "是" else "否"}\n")
                            
                            if (data.isDetected) {
                                sb.append("检测模型: ${data.modelName ?: "未知"}\n")
                                if (data.results.isNullOrEmpty()) {
                                    sb.append("检测结果: 正常 (无缺陷)\n")
                                    boundingBoxView.setResults(emptyList(), data.width.toFloat(), data.height.toFloat())
                                } else {
                                    sb.append("检测结果:\n")
                                    data.results.forEach { res ->
                                        sb.append("- ${res.label} (置信度: ${res.score})\n")
                                    }
                                    // Pass results to draw bounding boxes
                                    boundingBoxView.setResults(data.results, data.width.toFloat(), data.height.toFloat())
                                }
                            } else {
                                boundingBoxView.setResults(emptyList(), data.width.toFloat(), data.height.toFloat())
                            }
                            tvDetectionResult.text = sb.toString()
                        }
                    } else {
                        Toast.makeText(this@ImageDetailActivity, "获取详情失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ImageDetailActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("您确定要删除这张图片吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteImage()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteImage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.deleteImage("Bearer $token", imageId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.ok == true) {
                        Toast.makeText(this@ImageDetailActivity, "删除成功", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ImageDetailActivity, "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImageDetailActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
