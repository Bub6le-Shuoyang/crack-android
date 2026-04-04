package com.example.monitor

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.monitor.network.ImageItem

class ImageAdapter(private val images: List<ImageItem>) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    var isSelectionMode = false
    val selectedItems = mutableSetOf<Long>()

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
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

        val baseUrl = "http://127.0.0.1:7022"
        var imageUrl = if (item.filePath.startsWith("http")) item.filePath else baseUrl + item.filePath
        imageUrl = imageUrl.replace("10.60.22.66", "127.0.0.1").replace("10.0.2.2", "127.0.0.1")

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.ivImage)

        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = selectedItems.contains(item.id)

        holder.cbSelect.setOnClickListener {
            if (holder.cbSelect.isChecked) {
                selectedItems.add(item.id)
            } else {
                selectedItems.remove(item.id)
            }
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                if (selectedItems.contains(item.id)) {
                    selectedItems.remove(item.id)
                    holder.cbSelect.isChecked = false
                } else {
                    selectedItems.add(item.id)
                    holder.cbSelect.isChecked = true
                }
            } else {
                val intent = Intent(holder.itemView.context, ImageDetailActivity::class.java)
                intent.putExtra("IMAGE_ID", item.id)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = images.size

    fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        if (!isSelectionMode) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
    }
}
