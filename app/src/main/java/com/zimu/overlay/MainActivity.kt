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
        
        // 初始化全局异常处理器
        CrashHandler.init()
        
        // 初始化日志管理器
        LogManager.init(applicationContext)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
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
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val intent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            isServiceRunning = true
            checkPermissionAndUpdateUI()
            Toast.makeText(this, "遮挡层已启动，可以配置遮挡层", Toast.LENGTH_LONG).show()
            
            // 延迟检查服务状态，确保服务已启动
            binding.root.postDelayed({
                checkServiceStatus()
                if (OverlayService.isRunning) {
                    // 服务已启动，保持在后台运行，不退出APP
                    // 用户可以通过控制窗口配置遮挡层
                    android.util.Log.d("MainActivity", "Service started, keeping app in background")
                }
            }, 1000)
        } catch (e: Exception) {
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

