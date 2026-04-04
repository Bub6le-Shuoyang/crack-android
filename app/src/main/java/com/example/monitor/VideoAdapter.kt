package com.example.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.monitor.network.VideoItem

class VideoAdapter(
    private val videos: List<VideoItem>,
    private val onDeleteClick: (VideoItem) -> Unit,
    private val onVideoClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivCover)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnDeleteVideo: ImageButton = view.findViewById(R.id.btnDeleteVideo)
        val videoCoverContainer: View = view.findViewById(R.id.videoCoverContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = videos[position]
        holder.tvFileName.text = item.fileName
        holder.tvDate.text = item.createdAt

        // Format duration
        val safeDuration = item.duration ?: 0.0
        val minutes = (safeDuration / 60).toInt()
        val seconds = (safeDuration % 60).toInt()
        holder.tvDuration.text = String.format("%02d:%02d", minutes, seconds)

        // Status text
        when (item.isDetected) {
            0 -> {
                holder.tvStatus.text = "未检测"
                holder.tvStatus.setTextColor(android.graphics.Color.GRAY)
            }
            1 -> {
                val anomalyText = if (item.anomalyCount ?: 0 > 0) {
                    "检测完成 - 发现 ${item.anomalyCount} 处异常"
                } else {
                    "检测完成 - 正常"
                }
                holder.tvStatus.text = anomalyText
                holder.tvStatus.setTextColor(if (item.anomalyCount ?: 0 > 0) android.graphics.Color.RED else android.graphics.Color.parseColor("#4CAF50"))
            }
            else -> {
                holder.tvStatus.text = "检测中/未知状态"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            }
        }

        val baseUrl = "http://127.0.0.1:7022"
        val safeCoverPath = item.coverPath ?: ""
        var coverUrl = if (safeCoverPath.startsWith("http")) safeCoverPath else baseUrl + safeCoverPath
        coverUrl = coverUrl.replace("10.60.22.66", "127.0.0.1").replace("10.0.2.2", "127.0.0.1")

        Glide.with(holder.itemView.context)
            .load(coverUrl)
            .into(holder.ivCover)

        holder.btnDeleteVideo.setOnClickListener {
            onDeleteClick(item)
        }

        holder.videoCoverContainer.setOnClickListener {
            onVideoClick(item)
        }
    }

    override fun getItemCount(): Int = videos.size
}
