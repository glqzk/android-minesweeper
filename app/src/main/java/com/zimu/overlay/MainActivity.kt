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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkPermissionAndUpdateUI()
        
        binding.btnRequestPermission.setOnClickListener {
            requestOverlayPermission()
        }
        
        binding.btnStartOverlay.setOnClickListener {
            startOverlayService()
        }
        
        binding.btnStopOverlay.setOnClickListener {
            stopOverlayService()
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
        
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        isServiceRunning = true
        checkPermissionAndUpdateUI()
        Toast.makeText(this, "遮挡层已启动", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        
        isServiceRunning = false
        checkPermissionAndUpdateUI()
        Toast.makeText(this, "遮挡层已停止", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkServiceStatus() {
        // 简单检查服务状态，实际应该通过Service连接来检查
        isServiceRunning = OverlayService.isRunning
        checkPermissionAndUpdateUI()
    }
}

