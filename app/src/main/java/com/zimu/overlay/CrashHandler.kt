package com.zimu.overlay

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val TAG = "CrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    
    fun init() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.d(TAG, "Crash handler initialized")
    }
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            val stackTrace = StringWriter()
            val printWriter = PrintWriter(stackTrace)
            exception.printStackTrace(printWriter)
            val stackTraceString = stackTrace.toString()
            
            Log.e(TAG, "Uncaught exception in thread: ${thread.name}")
            Log.e(TAG, "Exception: ${exception.javaClass.name}")
            Log.e(TAG, "Message: ${exception.message}")
            Log.e(TAG, "Stack trace:\n$stackTraceString")
            
            // 记录到LogManager
            try {
                LogManager.log("E", TAG, "Uncaught exception in thread: ${thread.name}", exception)
            } catch (e: Exception) {
                // 如果LogManager也失败，至少记录到系统日志
                Log.e(TAG, "Failed to log to LogManager", e)
            }
            
            // 打印到logcat，方便调试
            exception.printStackTrace()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in crash handler", e)
        } finally {
            // 调用默认处理器
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}

