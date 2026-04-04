package com.example.monitor

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.location.Location
import android.location.LocationManager
import android.content.Context
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

    private var currentPhotoUri: android.net.Uri? = null

    fun triggerImageUpload() {
        val options = arrayOf("拍照", "从相册选择")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("图像检测")
            .setItems(options) { _, which ->
                if (which == 0) {
                    checkLocationAndTakePhoto()
                } else {
                    pickImageFromGallery()
                }
            }
            .show()
    }

    private var currentLocation: Location? = null

    private fun checkLocationAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), TAKE_PHOTO_REQUEST_LOCATION)
        } else {
            fetchLocationAndStartCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == TAKE_PHOTO_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndStartCamera()
            } else {
                Toast.makeText(context, "未授权位置信息，将不记录位置", Toast.LENGTH_SHORT).show()
                currentLocation = null
                takePhoto()
            }
        }
    }

    private fun fetchLocationAndStartCamera() {
        try {
            val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGpsEnabled || isNetworkEnabled) {
                val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                currentLocation = locationManager.getLastKnownLocation(provider)
                if (currentLocation == null) {
                    val fallbackProvider = if (isGpsEnabled) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
                    currentLocation = locationManager.getLastKnownLocation(fallbackProvider)
                }
                
                // Request a fresh update asynchronously. By the time user takes a photo, we should have a location.
                @Suppress("DEPRECATION", "MissingPermission")
                locationManager.requestSingleUpdate(provider, object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        currentLocation = location
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }, android.os.Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            currentLocation = null
        } catch (e: Exception) {
            // Ignore
        }
        takePhoto()
    }

    private fun takePhoto() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = java.io.File(requireContext().externalCacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, currentPhotoUri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开相机", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                data?.data?.let { uri ->
                    uploadImage(uri)
                }
            } else if (requestCode == TAKE_PHOTO_REQUEST) {
                currentPhotoUri?.let { uri ->
                    uploadImage(uri)
                }
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
                            val data = response.body()?.data
                            if (data != null && currentLocation != null) {
                                saveImageLocation(data.imageId, currentLocation!!.latitude, currentLocation!!.longitude)
                            }
                            currentLocation = null // reset location
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

    private fun saveImageLocation(imageId: Long, lat: Double, lon: Double) {
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = com.example.monitor.network.LocationRequest(imageId, lat, lon, "已获取坐标位置")
                RetrofitClient.apiService.saveLocation("Bearer $token", req)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val TAKE_PHOTO_REQUEST = 1002
        private const val TAKE_PHOTO_REQUEST_LOCATION = 1003
    }
}
