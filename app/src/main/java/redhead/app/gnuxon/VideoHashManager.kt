package redhead.app.gnuxon

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object VideoHashManager {
    private const val PREFS_NAME = "gnuxon_prefs"
    private const val HASH_KEY_PREFIX = "video_hash_"

    /**
     * Compute MD5 hash for a video file
     * This is a blocking operation and should be called from IO dispatcher
     */
    suspend fun computeMD5Hash(file: File): String? = withContext(Dispatchers.IO) {
        try {
            val messageDigest = MessageDigest.getInstance("MD5")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    messageDigest.update(buffer, 0, bytesRead)
                }
            }
            messageDigest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compute MD5 hash and save it immediately
     */
    suspend fun computeAndSaveHash(context: Context, file: File) {
        val hash = computeMD5Hash(file)
        if (hash != null) {
            saveHash(context, file.name, hash)
        }
    }

    /**
     * Save MD5 hash to SharedPreferences using filename as key
     */
    fun saveHash(context: Context, filename: String, hash: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString("$HASH_KEY_PREFIX$filename", hash)
        }
    }

    /**
     * Retrieve MD5 hash from SharedPreferences using filename as key
     */
    fun getHash(context: Context, filename: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$HASH_KEY_PREFIX$filename", null)
    }

    /**
     * Delete MD5 hash from SharedPreferences
     */
    fun deleteHash(context: Context, filename: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove("$HASH_KEY_PREFIX$filename")
        }
    }

    /**
     * Migrate hash when file is renamed
     */
    fun migrateHash(context: Context, oldFilename: String, newFilename: String) {
        val hash = getHash(context, oldFilename)
        if (hash != null) {
            deleteHash(context, oldFilename)
            saveHash(context, newFilename, hash)
        }
    }

    /**
     * Truncate hash for display in list view
     * Format: "MD5: a1b2c3d4...w9x8y7z6"
     */
    fun truncateHash(hash: String?): String {
        return if (hash != null && hash.length == 32) {
            "MD5: ${hash.take(8)}...${hash.takeLast(8)}"
        } else {
            "Hash unavailable"
        }
    }

    /**
     * Format full hash for display in dialog
     */
    fun formatFullHash(hash: String?): String {
        return hash ?: "Hash unavailable"
    }
}

