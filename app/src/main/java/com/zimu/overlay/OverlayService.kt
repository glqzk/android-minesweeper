package com.zimu.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.window.layout.WindowLayoutInfo
import com.zimu.overlay.databinding.OverlayViewBinding
import kotlin.math.max
import kotlin.math.min

class OverlayService : Service() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var controlWindow: ControlWindow? = null
    private var foldableHandler: FoldableHandler? = null
    private var sharedPreferences: SharedPreferences? = null
    
    private var overlayParams: WindowManager.LayoutParams? = null
    private var overlayX = 0
    private var overlayY = 0
    private var overlayWidth = 0
    private var overlayHeight = 0
    private var overlayAlpha = 0.5f
    private var isOverlayVisible = true
    
    companion object {
        @Volatile
        var isRunning = false
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val PREFS_NAME = "overlay_prefs"
        private const val KEY_X = "overlay_x"
        private const val KEY_Y = "overlay_y"
        private const val KEY_WIDTH = "overlay_width"
        private const val KEY_HEIGHT = "overlay_height"
        private const val KEY_ALPHA = "overlay_alpha"
    }
    
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        loadSettings()
        createOverlayView()
        createControlWindow()
        setupFoldableHandler()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeOverlayView()
        controlWindow?.dismiss()
        foldableHandler?.stopTracking()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "字幕遮挡服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "字幕遮挡服务正在运行"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("字幕遮挡服务")
                .setContentText("遮挡层正在运行")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("字幕遮挡服务")
                .setContentText("遮挡层正在运行")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build()
        }
    }
    
    private fun loadSettings() {
        val metrics = windowManager?.let { 
            val displayMetrics = android.util.DisplayMetrics()
            it.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics
        } ?: return
        
        overlayX = sharedPreferences?.getInt(KEY_X, metrics.widthPixels / 2) ?: metrics.widthPixels / 2
        overlayY = sharedPreferences?.getInt(KEY_Y, (metrics.heightPixels * 0.8).toInt()) ?: (metrics.heightPixels * 0.8).toInt()
        overlayWidth = sharedPreferences?.getInt(KEY_WIDTH, metrics.widthPixels) ?: metrics.widthPixels
        overlayHeight = sharedPreferences?.getInt(KEY_HEIGHT, (metrics.heightPixels * 0.2).toInt()) ?: (metrics.heightPixels * 0.2).toInt()
        overlayAlpha = sharedPreferences?.getFloat(KEY_ALPHA, 0.5f) ?: 0.5f
    }
    
    private fun saveSettings() {
        sharedPreferences?.edit()?.apply {
            putInt(KEY_X, overlayX)
            putInt(KEY_Y, overlayY)
            putInt(KEY_WIDTH, overlayWidth)
            putInt(KEY_HEIGHT, overlayHeight)
            putFloat(KEY_ALPHA, overlayAlpha)
            apply()
        }
    }
    
    private fun createOverlayView() {
        val binding = OverlayViewBinding.inflate(LayoutInflater.from(this))
        overlayView = binding.root
        
        val overlayRect = binding.overlayRect
        overlayRect.alpha = overlayAlpha
        overlayRect.visibility = if (isOverlayVisible) View.VISIBLE else View.GONE
        
        overlayParams = WindowManager.LayoutParams(
            overlayWidth,
            overlayHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = overlayX
            y = overlayY
        }
        
        windowManager?.addView(overlayView, overlayParams)
        
        // 添加拖拽功能
        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = overlayParams?.x ?: 0
                        initialY = overlayParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        overlayX = initialX + deltaX
                        overlayY = initialY + deltaY
                        updateOverlayPosition()
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        saveSettings()
                        return true
                    }
                }
                return false
            }
        })
    }
    
    private fun setupFoldableHandler() {
        windowManager?.let { wm ->
            foldableHandler = FoldableHandler(this, wm) { layoutInfo ->
                handleLayoutChange(layoutInfo)
            }
            foldableHandler?.startTracking()
        }
    }
    
    private fun handleLayoutChange(layoutInfo: WindowLayoutInfo) {
        // 当折叠状态改变时，调整遮挡层位置
        val metrics = foldableHandler?.getCurrentWindowMetrics()
        metrics?.let {
            // 确保遮挡层在可见区域内
            if (overlayX + overlayWidth > it.width()) {
                overlayX = it.width() - overlayWidth
            }
            if (overlayY + overlayHeight > it.height()) {
                overlayY = it.height() - overlayHeight
            }
            updateOverlayPosition()
        }
    }
    
    private fun createControlWindow() {
        val metrics = windowManager?.let { 
            val displayMetrics = android.util.DisplayMetrics()
            it.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics
        }
        
        controlWindow = ControlWindow(this, windowManager!!, object : ControlWindow.Callback {
            override fun onMove(deltaX: Int, deltaY: Int) {
                overlayX += deltaX
                overlayY += deltaY
                updateOverlayPosition()
            }
            
            override fun onSizeChange(widthPercent: Int, heightPercent: Int) {
                val metrics = windowManager?.let { 
                    val displayMetrics = android.util.DisplayMetrics()
                    it.defaultDisplay.getMetrics(displayMetrics)
                    displayMetrics
                } ?: return
                
                overlayWidth = (metrics.widthPixels * widthPercent / 100).coerceAtLeast(100)
                overlayHeight = (metrics.heightPixels * heightPercent / 100).coerceAtLeast(50)
                updateOverlaySize()
            }
            
            override fun onAlphaChange(alpha: Float) {
                overlayAlpha = alpha
                updateOverlayAlpha()
            }
            
            override fun onToggleVisibility() {
                isOverlayVisible = !isOverlayVisible
                updateOverlayVisibility()
            }
        })
        controlWindow?.show()
        
        // 同步控制窗口的滑块值
        metrics?.let { m ->
            val widthPercent = (overlayWidth * 100 / m.widthPixels).coerceIn(0, 100)
            val heightPercent = (overlayHeight * 100 / m.heightPixels).coerceIn(0, 100)
            val alphaPercent = (overlayAlpha * 100).toInt().coerceIn(0, 100)
            controlWindow?.syncSliders(widthPercent, heightPercent, alphaPercent)
        }
    }
    
    private fun updateOverlayPosition() {
        overlayParams?.let { params ->
            params.x = overlayX.coerceAtLeast(0)
            params.y = overlayY.coerceAtLeast(0)
            windowManager?.updateViewLayout(overlayView, params)
        }
    }
    
    private fun updateOverlaySize() {
        overlayParams?.let { params ->
            params.width = overlayWidth
            params.height = overlayHeight
            windowManager?.updateViewLayout(overlayView, params)
            saveSettings()
        }
    }
    
    private fun updateOverlayAlpha() {
        overlayView?.findViewById<View>(R.id.overlayRect)?.alpha = overlayAlpha
        saveSettings()
    }
    
    private fun updateOverlayVisibility() {
        overlayView?.findViewById<View>(R.id.overlayRect)?.visibility = 
            if (isOverlayVisible) View.VISIBLE else View.GONE
    }
    
    private fun removeOverlayView() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }
}

