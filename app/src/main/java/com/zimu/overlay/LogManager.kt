package com.zimu.overlay

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object LogManager {
    private const val TAG = "LogManager"
    private const val MAX_LOG_SIZE = 10000 // 最多保存10000条日志
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var logFile: File? = null
    
    fun init(context: Context) {
        try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            logFile = File(logDir, "app_log_${System.currentTimeMillis()}.txt")
            android.util.Log.d(TAG, "LogManager initialized, log file: ${logFile?.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error initializing LogManager", e)
        }
    }
    
    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val timestamp = dateFormat.format(Date())
            val logEntry = if (throwable != null) {
                val stackTrace = java.io.StringWriter()
                val printWriter = java.io.PrintWriter(stackTrace)
                throwable.printStackTrace(printWriter)
                "$timestamp [$level] $tag: $message\n${stackTrace.toString()}"
            } else {
                "$timestamp [$level] $tag: $message"
            }
            
            // 添加到内存队列
            synchronized(logQueue) {
                logQueue.offer(logEntry)
                // 限制队列大小
                while (logQueue.size > MAX_LOG_SIZE) {
                    logQueue.poll()
                }
            }
            
            // 写入文件
            writeToFile(logEntry)
            
            // 同时写入系统logcat
            when (level) {
                "D" -> Log.d(tag, message, throwable)
                "I" -> Log.i(tag, message, throwable)
                "W" -> Log.w(tag, message, throwable)
                "E" -> Log.e(tag, message, throwable)
                else -> Log.v(tag, message, throwable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging", e)
        }
    }
    
    private fun writeToFile(logEntry: String) {
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(logEntry).append("\n")
                    writer.flush()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error writing to log file", e)
        }
    }
    
    fun getAllLogs(): List<String> {
        return synchronized(logQueue) {
            logQueue.toList()
        }
    }
    
    fun clearLogs() {
        synchronized(logQueue) {
            logQueue.clear()
        }
    }
    
    fun getLogFile(): File? = logFile
    
    fun exportLogs(context: Context): File? {
        return try {
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(exportDir, "logs_export_$timestamp.txt")
            
            FileWriter(exportFile).use { writer ->
                writer.append("=== 字幕遮挡应用日志导出 ===\n")
                writer.append("导出时间: ${dateFormat.format(Date())}\n")
                writer.append("版本: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}\n")
                writer.append("================================\n\n")
                
                getAllLogs().forEach { log ->
                    writer.append(log).append("\n")
                }
                
                writer.flush()
            }
            
            android.util.Log.d(TAG, "Logs exported to: ${exportFile.absolutePath}")
            exportFile
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error exporting logs", e)
            null
        }
    }
}

