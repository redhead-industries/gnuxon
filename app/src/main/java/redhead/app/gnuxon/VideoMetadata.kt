package redhead.app.gnuxon

import java.io.File

/**
 * Data class to hold video file and its MD5 hash
 */
data class VideoMetadata(
    val file: File,
    val md5Hash: String?
)

