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
    @Volatile
    private var isInitialized = false
    @Volatile
    private var initContext: Context? = null
    
    fun init(context: Context) {
        try {
            initContext = context.applicationContext
            val logDir = File(context.getExternalFilesDir(null), "logs")
            if (!logDir.exists()) {
                val created = logDir.mkdirs()
                if (!created && !logDir.exists()) {
                    android.util.Log.e(TAG, "Failed to create log directory: ${logDir.absolutePath}")
                    // 尝试使用内部存储
                    val internalLogDir = File(context.filesDir, "logs")
                    if (!internalLogDir.exists()) {
                        internalLogDir.mkdirs()
                    }
                    logFile = File(internalLogDir, "app_log_${System.currentTimeMillis()}.txt")
                } else {
                    logFile = File(logDir, "app_log_${System.currentTimeMillis()}.txt")
                }
            } else {
                logFile = File(logDir, "app_log_${System.currentTimeMillis()}.txt")
            }
            isInitialized = true
            android.util.Log.d(TAG, "LogManager initialized, log file: ${logFile?.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error initializing LogManager", e)
            e.printStackTrace()
            // 即使失败也标记为已初始化，避免重复尝试
            isInitialized = true
        }
    }
    
    private fun ensureInitialized() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized && initContext != null) {
                    try {
                        init(initContext!!)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Auto-init failed", e)
                    }
                }
            }
        }
    }
    
    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        // 确保已初始化
        ensureInitialized()
        
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
            
            // 添加到内存队列（即使文件写入失败也要保存到内存）
            try {
                synchronized(logQueue) {
                    logQueue.offer(logEntry)
                    // 限制队列大小
                    while (logQueue.size > MAX_LOG_SIZE) {
                        logQueue.poll()
                    }
                }
            } catch (e: Exception) {
                // 内存队列失败也不应该崩溃，至少记录到系统日志
                android.util.Log.e(TAG, "Error adding to log queue", e)
            }
            
            // 写入文件（失败不影响继续运行）
            try {
                writeToFile(logEntry)
            } catch (e: Exception) {
                // 文件写入失败不影响，至少已经保存到内存和系统日志
                android.util.Log.w(TAG, "Failed to write log to file, but saved to memory", e)
            }
            
            // 同时写入系统logcat（这是最重要的，即使其他都失败）
            try {
                when (level) {
                    "D" -> Log.d(tag, message, throwable)
                    "I" -> Log.i(tag, message, throwable)
                    "W" -> Log.w(tag, message, throwable)
                    "E" -> Log.e(tag, message, throwable)
                    else -> Log.v(tag, message, throwable)
                }
            } catch (e: Exception) {
                // 即使系统日志也失败，至少尝试打印到标准输出
                System.err.println("LogManager: [$level] $tag: $message")
                if (throwable != null) {
                    throwable.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // 最后的保护：即使所有日志记录都失败，也不应该崩溃
            try {
                Log.e(TAG, "Critical error in log() method", e)
                System.err.println("LogManager critical error: ${e.message}")
            } catch (ignored: Exception) {
                // 如果连这个都失败，只能放弃了
            }
        }
    }
    
    private fun writeToFile(logEntry: String) {
        if (logFile == null) {
            return
        }
        
        try {
            val file = logFile!!
            // 确保文件所在目录存在
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            
            // 检查文件是否可写
            if (!file.exists()) {
                file.createNewFile()
            }
            
            FileWriter(file, true).use { writer ->
                writer.append(logEntry).append("\n")
                writer.flush()
            }
        } catch (e: IOException) {
            // 文件写入失败不影响应用运行
            android.util.Log.w(TAG, "Error writing to log file: ${logFile?.absolutePath}", e)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Unexpected error writing to log file", e)
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

