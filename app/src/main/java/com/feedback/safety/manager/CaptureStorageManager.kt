package com.feedback.safety.manager

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureStorageManager(private val context: Context) {
    
    private val rootDir: File by lazy {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "SafetyCapture")
        if (!dir.exists()) dir.mkdirs()
        File(dir, ".nomedia").apply { if (!exists()) createNewFile() }
        dir
    }

    fun saveBitmap(bitmap: Bitmap): File? {
        val date = Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        val timeStr = SimpleDateFormat("HH-mm-ss", Locale.US).format(date)
        
        val dateDir = File(rootDir, dateStr)
        if (!dateDir.exists()) dateDir.mkdirs()
        
        val file = File(dateDir, "$timeStr.jpg")
        return try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            out.flush()
            out.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getStorageUsedBytes(): Long {
        return calculateDirSize(rootDir)
    }
    
    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) calculateDirSize(f) else f.length()
        }
        return size
    }
    
    fun getScreenshotCount(): Int {
        var count = 0
        val dirs = rootDir.listFiles { f -> f.isDirectory } ?: return 0
        for (d in dirs) {
            count += (d.listFiles { f -> f.extension == "jpg" }?.size ?: 0)
        }
        return count
    }
    
    fun deleteAll() {
        val dirs = rootDir.listFiles { f -> f.isDirectory } ?: return
        for (d in dirs) {
            d.deleteRecursively()
        }
    }
}
