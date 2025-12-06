package com.zimu.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zimu.overlay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 注意：LogManager和CrashHandler应该在Application类中初始化
        // 这里再次初始化是为了确保（如果Application未初始化）
        try {
            CrashHandler.init()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing CrashHandler", e)
        }
        
        try {
            LogManager.init(applicationContext)
            LogManager.log("I", "MainActivity", "onCreate called")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing LogManager", e)
        }
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            LogManager.log("I", "MainActivity", "View binding successful")
            
            checkPermissionAndUpdateUI()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            LogManager.log("E", "MainActivity", "Error in onCreate", e)
            e.printStackTrace()
            throw e
        }
        
        binding.btnRequestPermission.setOnClickListener {
            requestOverlayPermission()
        }
        
        binding.btnStartOverlay.setOnClickListener {
            startOverlayService()
        }
        
        binding.btnStopOverlay.setOnClickListener {
            stopOverlayService()
        }
        
        binding.btnViewLogs.setOnClickListener {
            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkPermissionAndUpdateUI()
        checkServiceStatus()
    }
    
    private fun checkPermissionAndUpdateUI() {
        val hasPermission = hasOverlayPermission()
        binding.btnStartOverlay.isEnabled = hasPermission
        binding.btnStopOverlay.isEnabled = hasPermission && isServiceRunning
        
        if (hasPermission) {
            binding.tvStatus.text = getString(R.string.permission_granted)
            binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.tvStatus.text = getString(R.string.permission_denied)
            binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }
    
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startOverlayService() {
        LogManager.log("I", "MainActivity", "startOverlayService() called")
        
        // 检查权限
        val hasPermission = hasOverlayPermission()
        LogManager.log("I", "MainActivity", "Overlay permission check: $hasPermission")
        
        if (!hasPermission) {
            LogManager.log("W", "MainActivity", "Overlay permission not granted, cannot start service")
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            LogManager.log("I", "MainActivity", "Creating Intent for OverlayService")
            val intent = Intent(this, OverlayService::class.java)
            
            LogManager.log("I", "MainActivity", "Starting service, SDK: ${Build.VERSION.SDK_INT}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LogManager.log("I", "MainActivity", "Calling startForegroundService()")
                try {
                    startForegroundService(intent)
                    LogManager.log("I", "MainActivity", "startForegroundService() called successfully")
                } catch (e: IllegalStateException) {
                    LogManager.log("E", "MainActivity", "IllegalStateException in startForegroundService", e)
                    throw e
                } catch (e: SecurityException) {
                    LogManager.log("E", "MainActivity", "SecurityException in startForegroundService", e)
                    throw e
                } catch (e: Exception) {
                    LogManager.log("E", "MainActivity", "Exception in startForegroundService", e)
                    throw e
                }
            } else {
                LogManager.log("I", "MainActivity", "Calling startService() (SDK < O)")
                try {
                    startService(intent)
                    LogManager.log("I", "MainActivity", "startService() called successfully")
                } catch (e: Exception) {
                    LogManager.log("E", "MainActivity", "Exception in startService", e)
                    throw e
                }
            }
            
            LogManager.log("I", "MainActivity", "Service start command sent, setting isServiceRunning = true")
            isServiceRunning = true
            checkPermissionAndUpdateUI()
            
            Toast.makeText(this, "遮挡层已启动，可以配置遮挡层", Toast.LENGTH_LONG).show()
            LogManager.log("I", "MainActivity", "Toast shown, scheduling status check")
            
            // 延迟检查服务状态，确保服务已启动
            binding.root.postDelayed({
                try {
                    LogManager.log("I", "MainActivity", "Checking service status after delay")
                    checkServiceStatus()
                    if (OverlayService.isRunning) {
                        LogManager.log("I", "MainActivity", "Service is running, keeping app in background")
                        // 服务已启动，保持在后台运行，不退出APP
                        // 用户可以通过控制窗口配置遮挡层
                        android.util.Log.d("MainActivity", "Service started, keeping app in background")
                    } else {
                        LogManager.log("W", "MainActivity", "Service status check: service not running")
                    }
                } catch (e: Exception) {
                    LogManager.log("E", "MainActivity", "Error in delayed status check", e)
                }
            }, 1000)
            
            LogManager.log("I", "MainActivity", "startOverlayService() completed successfully")
        } catch (e: IllegalStateException) {
            LogManager.log("E", "MainActivity", "IllegalStateException starting overlay service", e)
            android.util.Log.e("MainActivity", "IllegalStateException starting overlay service", e)
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            LogManager.log("E", "MainActivity", "SecurityException starting overlay service", e)
            android.util.Log.e("MainActivity", "SecurityException starting overlay service", e)
            Toast.makeText(this, "启动失败: 权限不足 - ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            LogManager.log("E", "MainActivity", "Exception starting overlay service", e)
            android.util.Log.e("MainActivity", "Error starting overlay service", e)
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        
        isServiceRunning = false
        checkPermissionAndUpdateUI()
        Toast.makeText(this, "遮挡层已停止", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkServiceStatus() {
        // 检查服务状态
        isServiceRunning = OverlayService.isRunning
        checkPermissionAndUpdateUI()
        
        if (isServiceRunning) {
            binding.tvStatus.text = "遮挡层运行中 - 可通过控制窗口配置"
            binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }
    
    override fun onBackPressed() {
        // 如果服务正在运行，按返回键时最小化到后台，不退出
        if (isServiceRunning) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }
}

