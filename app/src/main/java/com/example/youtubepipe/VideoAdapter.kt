package com.example.youtubepipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VideoAdapter(
    private var videos: List<VideoItem>,
    private val onItemClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtDetails: TextView = view.findViewById(R.id.txtDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.txtTitle.text = video.title
        holder.txtDetails.text = "${video.uploaderName} • ${video.views} views"

        Glide.with(holder.itemView.context)
            .load(video.thumbnail)
            .centerCrop()
            .into(holder.imgThumbnail)

        holder.itemView.setOnClickListener { onItemClick(video) }
    }

    override fun getItemCount(): Int = videos.size

    fun updateData(newVideos: List<VideoItem>) {
        videos = newVideos
        notifyDataSetChanged()
    }
}
