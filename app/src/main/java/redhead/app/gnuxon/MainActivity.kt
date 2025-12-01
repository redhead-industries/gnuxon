package redhead.app.gnuxon

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.util.LruCache
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity(), ComponentCallbacks2 {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var fabCamera: FloatingActionButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var videoAdapter: VideoAdapter

    private var videos = mutableListOf<VideoMetadata>()
    private lateinit var thumbnailCache: LruCache<String, Bitmap>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        fabCamera = findViewById(R.id.fabCamera)

        setSupportActionBar(toolbar)


        // Initialize thumbnail cache
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        thumbnailCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }


        // Setup RecyclerView
        videoAdapter = VideoAdapter(
            videos = videos,
            onVideoClick = { video -> playVideo(video) },
            onVideoLongClick = { video -> showVideoOptions(video) },
            getThumbnail = { video, imageView -> loadThumbnail(video, imageView) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = videoAdapter

        // Setup FAB
        fabCamera.setOnClickListener {
            startActivity(Intent(this, Camera::class.java))
        }

        // Load videos
        loadVideos()
    }

    override fun onResume() {
        super.onResume()
        loadVideos()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadVideos() {
        // Check if we have permission to read videos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, show empty state
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        }

        try {
            val moviesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "GNUXON"
            )

            if (!moviesDir.exists()) {
                moviesDir.mkdirs()
            }

            val videoFiles = moviesDir.listFiles { file ->
                file.isFile && file.extension.equals("mp4", ignoreCase = true)
            }?.toList() ?: emptyList()

            // Create VideoMetadata objects with hash lookups
            val videoMetadataList = videoFiles.map { file ->
                VideoMetadata(
                    file = file,
                    md5Hash = VideoHashManager.getHash(this, file.name)
                )
            }

            videos.clear()
            // Sort by newest first (descending by last modified date)
            videos.addAll(videoMetadataList.sortedWith(
                compareByDescending<VideoMetadata> { it.file.lastModified() }
                    .thenBy { it.file.name.lowercase() }
            ))
            videoAdapter.updateVideos(videos)

            // Show/hide empty state
            if (videos.isEmpty()) {
                emptyStateLayout.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            // Handle any errors gracefully
            Toast.makeText(
                this,
                "Error loading videos: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            // Show empty state
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }


    private fun loadThumbnail(video: File, imageView: ImageView) {
        val cacheKey = video.absolutePath

        // Check cache first
        thumbnailCache.get(cacheKey)?.let { bitmap ->
            imageView.setImageBitmap(bitmap)
            return
        }

        // Load asynchronously
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(video.absolutePath)
                    val thumbnail = retriever.frameAtTime
                    retriever.release()
                    thumbnail
                } catch (_: Exception) {
                    null
                }
            }

            bitmap?.let {
                thumbnailCache.put(cacheKey, it)
                imageView.setImageBitmap(it)
            }
        }
    }

    private fun playVideo(video: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "redhead.app.gnuxon.fileprovider",
                video
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Play video"))
            } else {
                Toast.makeText(this, R.string.no_video_player, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showVideoOptions(video: File) {
        val options = arrayOf(
            getString(R.string.share_video),
            getString(R.string.rename_video),
            getString(R.string.delete_video),
            getString(R.string.view_hash),
            getString(R.string.copy_hash)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.video_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareVideo(video)
                    1 -> showRenameDialog(video)
                    2 -> showDeleteConfirmation(video)
                    3 -> showHashDialog(video)
                    4 -> copyHashToClipboard(video)
                }
            }
            .show()
    }

    private fun shareVideo(video: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "redhead.app.gnuxon.fileprovider",
                video
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, getString(R.string.share_video)))
            Toast.makeText(this, R.string.video_shared, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRenameDialog(video: File) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename_video, null)
        val tilFilename = dialogView.findViewById<TextInputLayout>(R.id.tilFilename)
        val etFilename = dialogView.findViewById<TextInputEditText>(R.id.etFilename)

        // Pre-fill with current name (without extension)
        val currentName = video.nameWithoutExtension
        etFilename.setText(currentName)
        etFilename.selectAll()

        AlertDialog.Builder(this)
            .setTitle(R.string.rename_video_title)
            .setView(dialogView)
            .setPositiveButton(R.string.rename) { _, _ ->
                val newName = etFilename.text.toString().trim()
                if (validateAndRenameVideo(video, newName, tilFilename)) {
                    loadVideos()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun validateAndRenameVideo(
        video: File,
        newName: String,
        tilFilename: TextInputLayout
    ): Boolean {
        // Check if empty
        if (newName.isEmpty()) {
            tilFilename.error = getString(R.string.filename_empty)
            return false
        }

        // Sanitize filename - remove invalid characters
        val sanitized = newName.replace(Regex("[/\\\\:*?\"<>|]"), "")
        if (sanitized.isEmpty()) {
            tilFilename.error = getString(R.string.invalid_filename)
            return false
        }

        // Ensure .mp4 extension
        val finalName = if (sanitized.endsWith(".mp4", ignoreCase = true)) {
            sanitized
        } else {
            "$sanitized.mp4"
        }

        // Check if file already exists
        val newFile = File(video.parent, finalName)
        if (newFile.exists() && newFile != video) {
            tilFilename.error = getString(R.string.filename_exists)
            return false
        }

        // Rename the file
        return try {
            if (video.renameTo(newFile)) {
                // Clear cached thumbnail
                thumbnailCache.remove(video.absolutePath)
                // Migrate hash to new filename
                VideoHashManager.migrateHash(this, video.name, newFile.name)
                Toast.makeText(this, R.string.video_renamed, Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(this, R.string.video_rename_failed, Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "${getString(R.string.video_rename_failed)}: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun showDeleteConfirmation(video: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_video_title)
            .setMessage(getString(R.string.delete_video_message, video.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteVideo(video)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteVideo(video: File) {
        try {
            if (video.delete()) {
                thumbnailCache.remove(video.absolutePath)
                // Delete hash from storage
                VideoHashManager.deleteHash(this, video.name)
                Toast.makeText(this, R.string.video_deleted, Toast.LENGTH_SHORT).show()
                loadVideos()
            } else {
                Toast.makeText(this, R.string.video_delete_failed, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "${getString(R.string.video_delete_failed)}: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showHashDialog(video: File) {
        val hash = VideoHashManager.getHash(this, video.name)
        val fullHash = VideoHashManager.formatFullHash(hash)

        AlertDialog.Builder(this)
            .setTitle(R.string.hash_dialog_title)
            .setMessage("${getString(R.string.full_hash_label)}\n\n$fullHash")
            .setPositiveButton(R.string.copy_hash) { _, _ ->
                copyHashToClipboard(video)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyHashToClipboard(video: File) {
        val hash = VideoHashManager.getHash(this, video.name)
        if (hash != null) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("MD5 Hash", hash)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.hash_copied, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.hash_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= 60) { // TRIM_MEMORY_MODERATE
            thumbnailCache.evictAll()
        }
    }
}
