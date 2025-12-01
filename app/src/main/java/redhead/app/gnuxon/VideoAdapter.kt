package redhead.app.gnuxon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoAdapter(
    private var videos: List<VideoMetadata>,
    private val onVideoClick: (File) -> Unit,
    private val onVideoLongClick: (File) -> Unit,
    private val getThumbnail: (File, ImageView) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvFilename: TextView = view.findViewById(R.id.tvFilename)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvFileSize: TextView = view.findViewById(R.id.tvFileSize)
        val tvMd5Hash: TextView = view.findViewById(R.id.tvMd5Hash)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoMetadata = videos[position]
        val video = videoMetadata.file

        holder.tvFilename.text = video.name
        holder.tvDateTime.text = formatDate(video.lastModified())
        holder.tvFileSize.text = formatFileSize(video.length())
        holder.tvDuration.text = "..." // Will be updated asynchronously
        holder.tvMd5Hash.text = VideoHashManager.truncateHash(videoMetadata.md5Hash)

        // Reset thumbnail to placeholder
        holder.ivThumbnail.setImageResource(R.drawable.ic_video_placeholder)

        // Load thumbnail asynchronously
        getThumbnail(video, holder.ivThumbnail)

        holder.itemView.setOnClickListener { onVideoClick(video) }
        holder.itemView.setOnLongClickListener {
            onVideoLongClick(video)
            true
        }
    }

    override fun getItemCount(): Int = videos.size

    fun updateVideos(newVideos: List<VideoMetadata>) {
        val diffCallback = VideoDiffCallback(videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback, true) // enable move detection
        videos = newVideos
        diffResult.dispatchUpdatesTo(this)
    }

    private class VideoDiffCallback(
        private val oldList: List<VideoMetadata>,
        private val newList: List<VideoMetadata>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].file.absolutePath == newList[newItemPosition].file.absolutePath
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldMetadata = oldList[oldItemPosition]
            val newMetadata = newList[newItemPosition]
            return oldMetadata.file.name == newMetadata.file.name &&
                   oldMetadata.file.lastModified() == newMetadata.file.lastModified() &&
                   oldMetadata.file.length() == newMetadata.file.length() &&
                   oldMetadata.md5Hash == newMetadata.md5Hash
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
