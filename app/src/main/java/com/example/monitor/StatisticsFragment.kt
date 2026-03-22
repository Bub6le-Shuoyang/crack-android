package com.example.monitor

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.monitor.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsFragment : Fragment() {

    private lateinit var tvTotalImages: TextView
    private lateinit var tvTotalAnomaly: TextView
    private lateinit var tvAnomalyRate: TextView
    private lateinit var llAnomalyTypes: LinearLayout
    private lateinit var llDailyTrend: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)
        
        tvTotalImages = view.findViewById(R.id.tvTotalImages)
        tvTotalAnomaly = view.findViewById(R.id.tvTotalAnomaly)
        tvAnomalyRate = view.findViewById(R.id.tvAnomalyRate)
        llAnomalyTypes = view.findViewById(R.id.llAnomalyTypes)
        llDailyTrend = view.findViewById(R.id.llDailyTrend)
        progressBar = view.findViewById(R.id.progressBar)

        fetchStatistics()

        return view
    }

    private fun fetchStatistics() {
        progressBar.visibility = View.VISIBLE
        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""

        if (token.isEmpty()) {
            progressBar.visibility = View.GONE
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getImageOverview("Bearer $token")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.ok == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            tvTotalImages.text = data.totalImages.toString()
                            tvTotalAnomaly.text = data.totalAnomalyImages.toString()
                            tvAnomalyRate.text = String.format("%.1f%%", data.anomalyRate * 100)

                            // Populate Anomaly Types
                            llAnomalyTypes.removeAllViews()
                            if (data.topAnomalyType.isEmpty()) {
                                val emptyView = TextView(context).apply {
                                    text = "暂无数据"
                                    setTextColor(Color.GRAY)
                                }
                                llAnomalyTypes.addView(emptyView)
                            } else {
                                data.topAnomalyType.forEach { type ->
                                    val row = LinearLayout(context).apply {
                                        orientation = LinearLayout.HORIZONTAL
                                        setPadding(0, 8, 0, 8)
                                    }
                                    
                                    val labelView = TextView(context).apply {
                                        text = type.label
                                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                                        setTextColor(Color.DKGRAY)
                                    }
                                    
                                    val countView = TextView(context).apply {
                                        text = "${type.count} 次"
                                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                        setTextColor(Color.BLACK)
                                        setTypeface(null, android.graphics.Typeface.BOLD)
                                    }
                                    
                                    row.addView(labelView)
                                    row.addView(countView)
                                    llAnomalyTypes.addView(row)
                                }
                            }

                            // Populate Daily Trend
                            llDailyTrend.removeAllViews()
                            if (data.dailyTrend.isEmpty()) {
                                val emptyView = TextView(context).apply {
                                    text = "暂无数据"
                                    setTextColor(Color.GRAY)
                                }
                                llDailyTrend.addView(emptyView)
                            } else {
                                data.dailyTrend.forEach { trend ->
                                    val row = LinearLayout(context).apply {
                                        orientation = LinearLayout.HORIZONTAL
                                        setPadding(0, 8, 0, 8)
                                    }
                                    
                                    val dateView = TextView(context).apply {
                                        text = trend.date
                                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                                        setTextColor(Color.DKGRAY)
                                        textSize = 14f
                                    }
                                    
                                    val statsView = TextView(context).apply {
                                        text = "检测: ${trend.total} | 异常: ${trend.anomaly}"
                                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                        setTextColor(if (trend.anomaly > 0) Color.RED else Color.parseColor("#4CAF50"))
                                        textSize = 14f
                                    }
                                    
                                    row.addView(dateView)
                                    row.addView(statsView)
                                    llDailyTrend.addView(row)
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "获取统计数据失败", Toast.LENGTH_SHORT).show()
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
}
