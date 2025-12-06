package com.zimu.overlay

import android.app.Application
import android.util.Log

class App : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 在应用启动的最早阶段初始化
        try {
            // 先初始化全局异常处理器
            CrashHandler.init()
            Log.d("App", "CrashHandler initialized")
            
            // 初始化日志管理器（使用Application Context）
            LogManager.init(this)
            Log.d("App", "LogManager initialized")
            
            // 记录应用启动
            LogManager.log("I", "App", "Application onCreate - App started")
            LogManager.log("I", "App", "SDK Version: ${android.os.Build.VERSION.SDK_INT}")
            LogManager.log("I", "App", "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            
        } catch (e: Exception) {
            // 即使初始化失败也要记录到系统日志
            Log.e("App", "Error initializing app", e)
            e.printStackTrace()
        }
    }
}

