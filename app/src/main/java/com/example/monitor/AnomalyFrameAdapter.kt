package com.example.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.monitor.network.AnomalyFrame
import com.example.monitor.utils.LabelUtils

class AnomalyFrameAdapter(
    private val anomalyFrames: List<AnomalyFrame>,
    private val onItemClick: (AnomalyFrame) -> Unit
) : RecyclerView.Adapter<AnomalyFrameAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvDetails: TextView = view.findViewById(R.id.tvDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anomaly_frame, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val frame = anomalyFrames[position]
        holder.tvTime.text = "时间: ${frame.timeFormatted} (第 ${frame.frameNumber} 帧)"
        
        val details = StringBuilder()
        details.append("检测到 ${frame.detections.size} 处异常:\n")
        frame.detections.forEach { det ->
            val mappedLabel = LabelUtils.getMappedLabel(det.label)
            details.append("- $mappedLabel (置信度: ${String.format("%.2f", det.score)})\n")
        }
        holder.tvDetails.text = details.toString().trim()
        
        holder.itemView.setOnClickListener {
            onItemClick(frame)
        }
    }

    override fun getItemCount() = anomalyFrames.size
}