package com.example.monitor

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.monitor.network.ImageItem

class ImageAdapter(private val images: List<ImageItem>) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = images[position]
        holder.tvFileName.text = item.fileName
        holder.tvDate.text = item.createdAt

        // Since we need full URL, we should prepend base URL if it's relative
        // Assuming RetrofitClient has BASE_URL
        val baseUrl = "http://10.0.2.2:7022" // Update this as per backend config
        var imageUrl = if (item.filePath.startsWith("http")) item.filePath else baseUrl + item.filePath
        imageUrl = imageUrl.replace("127.0.0.1", "10.0.2.2")

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.ivImage)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ImageDetailActivity::class.java)
            intent.putExtra("IMAGE_ID", item.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = images.size
}
