package com.example.monitor

import android.media.MediaPlayer
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.monitor.network.AnomalyFrame
import com.example.monitor.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoDetailActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var boundingBoxView: BoundingBoxView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartDetect: Button
    private lateinit var rvAnomalies: RecyclerView
    private lateinit var tvAnomalyTitle: TextView
    private lateinit var toolbar: Toolbar

    private var videoId: Long = -1
    private var videoPath: String = ""
    private var isDetected: Int = 0
    private var anomalyFramesJson: String? = null
    private var anomalyFrames: List<AnomalyFrame> = emptyList()
    
    private var videoWidth = 1f
    private var videoHeight = 1f
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_detail)

        videoId = intent.getLongExtra("videoId", -1)
        videoPath = intent.getStringExtra("videoPath") ?: ""
        isDetected = intent.getIntExtra("isDetected", 0)
        anomalyFramesJson = intent.getStringExtra("anomalyFramesJson")

        initViews()
        setupVideoView()
        
        if (isDetected == 1) {
            btnStartDetect.visibility = View.GONE
            loadResultsFromJson()
        } else {
            btnStartDetect.visibility = View.VISIBLE
            tvAnomalyTitle.text = "视频尚未检测"
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        videoView = findViewById(R.id.videoView)
        boundingBoxView = findViewById(R.id.boundingBoxView)
        progressBar = findViewById(R.id.videoProgressBar)
        btnStartDetect = findViewById(R.id.btnStartDetect)
        rvAnomalies = findViewById(R.id.rvAnomalies)
        tvAnomalyTitle = findViewById(R.id.tvAnomalyTitle)
        
        rvAnomalies.layoutManager = LinearLayoutManager(this)
        
        btnStartDetect.setOnClickListener {
            startVideoDetection()
        }
    }

    private fun setupVideoView() {
        if (videoPath.isEmpty()) return
        
        // Construct full URL
        val baseUrl = "http://10.0.2.2:7022/" // Or RetrofitClient.BASE_URL if it's accessible. But RetrofitClient.BASE_URL is private. Wait, we'll hardcode or reflection. Let's just use RetrofitClient's base url string pattern. Wait, I'll update RetrofitClient to expose BASE_URL.
        // Actually, we can just use 10.0.2.2 since we reverted to 127.0.0.1 in RetrofitClient. Wait, user said "之前用adb成功过，请继续使用adb可以吗", so it is 127.0.0.1:7022.
        
        val baseUrlFixed = "http://127.0.0.1:7022"
        val fullUrl = if (videoPath.startsWith("http")) videoPath else "$baseUrlFixed$videoPath"

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.setVideoPath(fullUrl)
        
        videoView.setOnPreparedListener { mp ->
            videoWidth = mp.videoWidth.toFloat()
            videoHeight = mp.videoHeight.toFloat()
            mp.start()
            startPollingBoxes()
        }
        
        videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "视频加载失败", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun loadResultsFromJson() {
        if (!anomalyFramesJson.isNullOrEmpty()) {
            try {
                val gson = Gson()
                val listType = object : TypeToken<List<AnomalyFrame>>() {}.type
                anomalyFrames = gson.fromJson(anomalyFramesJson, listType)
                tvAnomalyTitle.text = "检测到的异常片段 (${anomalyFrames.size} 处)"
                rvAnomalies.adapter = AnomalyFrameAdapter(anomalyFrames) { frame ->
                    seekToFrame(frame)
                }
            } catch (e: Exception) {
                tvAnomalyTitle.text = "解析异常数据失败"
                e.printStackTrace()
            }
        } else {
            // fallback: fetch from API if JSON was not passed
            fetchVideoDetails()
        }
    }

    private fun startVideoDetection() {
        if (videoId == -1L) return
        
        btnStartDetect.isEnabled = false
        btnStartDetect.text = "请求检测中..."
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.detectVideo(videoId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.ok == true) {
                        btnStartDetect.visibility = View.GONE
                        tvAnomalyTitle.text = "已加入检测队列，正在检测..."
                        pollVideoProgress()
                    } else {
                        btnStartDetect.isEnabled = true
                        btnStartDetect.text = "重新检测"
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@VideoDetailActivity, "开始检测失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnStartDetect.isEnabled = true
                    btnStartDetect.text = "重新检测"
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@VideoDetailActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun pollVideoProgress() {
        CoroutineScope(Dispatchers.IO).launch {
            var isFinished = false
            while (!isFinished && isActive) {
                delay(3000) // Poll every 3 seconds
                try {
                    val response = RetrofitClient.apiService.getVideoProgress(videoId)
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.ok == true) {
                            val progress = body.progress ?: 0
                            withContext(Dispatchers.Main) {
                                tvAnomalyTitle.text = "正在检测: $progress%"
                            }
                            if (progress == 100) {
                                isFinished = true
                                withContext(Dispatchers.Main) {
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this@VideoDetailActivity, "检测完成", Toast.LENGTH_SHORT).show()
                                    val data = body.data
                                    if (data != null) {
                                        anomalyFrames = data.anomalyFrames
                                        tvAnomalyTitle.text = "检测到的异常片段 (${anomalyFrames.size} 处)"
                                        rvAnomalies.adapter = AnomalyFrameAdapter(anomalyFrames) { frame ->
                                            seekToFrame(frame)
                                        }
                                        // Update local JSON so it stays cached
                                        val gson = Gson()
                                        anomalyFramesJson = gson.toJson(anomalyFrames)
                                    } else {
                                        tvAnomalyTitle.text = "获取异常数据失败"
                                    }
                                }
                            } else if (progress == -1) {
                                isFinished = true
                                withContext(Dispatchers.Main) {
                                    progressBar.visibility = View.GONE
                                    tvAnomalyTitle.text = "检测失败"
                                    btnStartDetect.visibility = View.VISIBLE
                                    btnStartDetect.isEnabled = true
                                    btnStartDetect.text = "重新检测"
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient errors
                }
            }
        }
    }

    private fun fetchVideoDetails() {
        if (videoId == -1L) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getVideoProgress(videoId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.ok == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            anomalyFrames = data.anomalyFrames
                            tvAnomalyTitle.text = "检测到的异常片段 (${anomalyFrames.size} 处)"
                            rvAnomalies.adapter = AnomalyFrameAdapter(anomalyFrames) { frame ->
                                seekToFrame(frame)
                            }
                        } else {
                            tvAnomalyTitle.text = "暂无异常数据"
                        }
                    } else {
                        tvAnomalyTitle.text = "获取异常数据失败"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvAnomalyTitle.text = "网络错误: ${e.message}"
                }
            }
        }
    }

    private fun seekToFrame(frame: AnomalyFrame) {
        val timeMs = (frame.time * 1000).toInt()
        videoView.seekTo(timeMs)
        videoView.pause() // Pause to let user see the bounding box
        
        // Draw the box for this frame explicitly
        boundingBoxView.setResults(frame.detections, videoWidth, videoHeight)
    }

    private fun startPollingBoxes() {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                if (videoView.isPlaying) {
                    val currentPosSec = videoView.currentPosition / 1000.0
                    // Find if there's an anomaly within 0.5s of current time
                    val currentFrame = anomalyFrames.find { Math.abs(it.time - currentPosSec) < 0.5 }
                    
                    if (currentFrame != null) {
                        boundingBoxView.setResults(currentFrame.detections, videoWidth, videoHeight)
                    } else {
                        boundingBoxView.setResults(emptyList(), videoWidth, videoHeight)
                    }
                }
                delay(200) // Poll every 200ms
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}