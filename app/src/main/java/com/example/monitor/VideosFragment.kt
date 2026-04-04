package com.example.monitor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.monitor.network.RetrofitClient
import com.example.monitor.network.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: VideoAdapter
    private val videos = mutableListOf<VideoItem>()

    private var currentPage = 1
    private val pageSize = 10
    private var totalPages = 1
    private var currentKeyword = ""

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var tvPageInfo: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_videos, container, false)
        
        etSearch = view.findViewById(R.id.etSearch)
        btnSearch = view.findViewById(R.id.btnSearch)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        btnPrevPage = view.findViewById(R.id.btnPrevPage)
        btnNextPage = view.findViewById(R.id.btnNextPage)
        tvPageInfo = view.findViewById(R.id.tvPageInfo)
        
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)

        adapter = VideoAdapter(
            videos,
            onDeleteClick = { item -> confirmDeleteVideo(item) },
            onVideoClick = { item -> handleVideoClick(item) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        setupListeners()
        fetchVideos()

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchVideos()
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            currentKeyword = etSearch.text.toString().trim()
            currentPage = 1
            fetchVideos()
        }

        btnRefresh.setOnClickListener {
            fetchVideos()
        }

        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                fetchVideos()
            }
        }

        btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                fetchVideos()
            }
        }
    }

    private fun fetchVideos() {
        progressBar.visibility = View.VISIBLE
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(context, "未登录", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Backend API only specifies fileType filter, but we might pass search keyword if supported. 
                // Let's pass it anyway or just fetch all and filter locally if not supported.
                // Assuming it's similar to listImages, we will try to pass keyword if API ignores it we can't do much.
                // Actually the doc doesn't show keyword for list-videos. We will do local filter if it's not supported by API.
                val response = RetrofitClient.apiService.listVideos("Bearer $token", currentPage, pageSize, null)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.ok == true) {
                            var list = body.data?.list ?: emptyList()
                            
                            // Local search filter if API doesn't support it
                            if (currentKeyword.isNotEmpty()) {
                                list = list.filter { it.fileName.contains(currentKeyword, ignoreCase = true) }
                            }
                            
                            videos.clear()
                            videos.addAll(list)
                            adapter.notifyDataSetChanged()

                            val total = body.data?.total ?: 0
                            totalPages = if (total == 0) 1 else Math.ceil(total.toDouble() / pageSize).toInt()
                            tvPageInfo.text = "$currentPage / $totalPages"
                        } else {
                            Toast.makeText(context, "获取失败", Toast.LENGTH_SHORT).show()
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

    private fun confirmDeleteVideo(item: VideoItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除视频 ${item.fileName} 吗？")
            .setPositiveButton("删除") { _, _ -> deleteVideo(item.videoId) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteVideo(videoId: Long) {
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.deleteVideo("Bearer $token", videoId)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.ok == true) {
                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                        fetchVideos()
                    } else {
                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleVideoClick(item: VideoItem) {
        val intent = Intent(requireContext(), VideoDetailActivity::class.java).apply {
            putExtra("videoId", item.videoId)
            putExtra("videoPath", item.filePath)
            putExtra("isDetected", item.isDetected)
            if (item.isDetected == 1 && item.anomalyFrames != null) {
                val gson = com.google.gson.Gson()
                putExtra("anomalyFramesJson", gson.toJson(item.anomalyFrames))
            }
        }
        startActivity(intent)
    }

    private fun startVideoDetection(videoId: Long) {
        // Moved to VideoDetailActivity
    }

    private fun pollVideoProgress(videoId: Long) {
        // Moved to VideoDetailActivity
    }

    private var currentVideoUri: android.net.Uri? = null

    fun triggerVideoUpload() {
        val options = arrayOf("录像", "从相册选择")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("视频检测")
            .setItems(options) { _, which ->
                if (which == 0) {
                    recordVideo()
                } else {
                    pickVideoFromGallery()
                }
            }
            .show()
    }

    private fun recordVideo() {
        val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
        val videoFile = java.io.File(requireContext().externalCacheDir, "temp_vid_${System.currentTimeMillis()}.mp4")
        currentVideoUri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            videoFile
        )
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, currentVideoUri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try {
            startActivityForResult(intent, RECORD_VIDEO_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开相机", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, PICK_VIDEO_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == PICK_VIDEO_REQUEST) {
                data?.data?.let { uri ->
                    uploadVideo(uri)
                }
            } else if (requestCode == RECORD_VIDEO_REQUEST) {
                currentVideoUri?.let { uri ->
                    uploadVideo(uri)
                }
            }
        }
    }

    private fun uploadVideo(uri: android.net.Uri) {
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""
        if (token.isEmpty()) {
            Toast.makeText(context, "未登录", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        Toast.makeText(context, "开始上传视频...", Toast.LENGTH_LONG).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("video/*"), bytes)
                    val multipartBody = okhttp3.MultipartBody.Part.createFormData("file", "upload.mp4", requestBody)

                    val response = RetrofitClient.apiService.uploadVideo("Bearer $token", multipartBody)

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        if (response.isSuccessful && response.body()?.ok == true) {
                            Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                            fetchVideos()
                        } else {
                            Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show()
                        }
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
        private const val PICK_VIDEO_REQUEST = 2001
        private const val RECORD_VIDEO_REQUEST = 2002
    }
}
