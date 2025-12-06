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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.window.layout.WindowLayoutInfo
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class OverlayService : Service() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var controlWindow: ControlWindow? = null
    private var foldableHandler: FoldableHandler? = null
    private var sharedPreferences: SharedPreferences? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var overlayParams: WindowManager.LayoutParams? = null
    private var overlayX = 0
    private var overlayY = 0
    private var overlayWidth = 0
    private var overlayHeight = 0
    private var overlayAlpha = 0.5f
    private var overlayColor = 0xFF000000.toInt() // 默认黑色
    private var overlayRotation = 0f // 旋转角度
    private var isOverlayVisible = true
    private var isInitialized = false
    
    // 手势识别相关变量
    private var initialDistance = 0f
    private var initialRotation = 0f
    private var initialScale = 1f
    private var initialCenterX = 0f
    private var initialCenterY = 0f
    private var isMultiTouch = false
    
    companion object {
        @Volatile
        var isRunning = false
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val PREFS_NAME = "overlay_prefs"
        private const val KEY_X = "overlay_x"
        private const val KEY_Y = "overlay_y"
        private const val KEY_WIDTH = "overlay_width"
        private const val KEY_HEIGHT = "overlay_height"
        private const val KEY_ALPHA = "overlay_alpha"
        private const val KEY_COLOR = "overlay_color"
        private const val KEY_ROTATION = "overlay_rotation"
        private const val DELAY_INIT_MS = 500L // 延迟500ms初始化窗口
    }
    
    override fun onCreate() {
        // 在最开始就记录日志，确保即使后续崩溃也能看到
        try {
            android.util.Log.d(TAG, "=== OverlayService.onCreate() START ===")
            LogManager.log("I", TAG, "=== OverlayService.onCreate() START ===")
            LogManager.log("I", TAG, "SDK Version: ${Build.VERSION.SDK_INT}")
            LogManager.log("I", TAG, "Service class: ${this.javaClass.name}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error logging onCreate start", e)
        }
        
        try {
            super.onCreate()
            LogManager.log("I", TAG, "super.onCreate() completed")
        } catch (e: Exception) {
            LogManager.log("E", TAG, "Error in super.onCreate()", e)
            android.util.Log.e(TAG, "Error in super.onCreate()", e)
            throw e
        }
        
        try {
            isRunning = true
            isInitialized = false
            LogManager.log("I", TAG, "Service state: isRunning=true, isInitialized=false")
            
            // 使用Application Context确保服务独立运行
            LogManager.log("I", TAG, "Getting WindowManager from applicationContext")
            windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            LogManager.log("I", TAG, "WindowManager obtained: ${windowManager != null}")
            
            LogManager.log("I", TAG, "Getting SharedPreferences")
            sharedPreferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            LogManager.log("I", TAG, "SharedPreferences obtained: ${sharedPreferences != null}")
            
            if (windowManager == null) {
                android.util.Log.e(TAG, "WindowManager is null, cannot initialize service")
                LogManager.log("E", TAG, "WindowManager is null, cannot initialize service")
                stopSelf()
                return
            }
            
            // 检查悬浮窗权限
            LogManager.log("I", TAG, "Checking overlay permission")
            if (!checkOverlayPermission()) {
                android.util.Log.e(TAG, "Overlay permission not granted, stopping service")
                LogManager.log("E", TAG, "Overlay permission not granted, stopping service")
                stopSelf()
                return
            }
            LogManager.log("I", TAG, "Overlay permission granted")
            
            LogManager.log("I", TAG, "Creating notification channel")
            createNotificationChannel()
            LogManager.log("I", TAG, "Notification channel created")
            
            LogManager.log("I", TAG, "Creating notification")
            val notification = createNotification()
            LogManager.log("I", TAG, "Notification created, starting foreground service")
            startForeground(NOTIFICATION_ID, notification)
            LogManager.log("I", TAG, "Foreground service started with notification")
            
            LogManager.log("I", TAG, "Loading settings")
            loadSettings()
            LogManager.log("I", TAG, "Settings loaded")
            
            // 延迟创建窗口，确保系统完全初始化
            LogManager.log("I", TAG, "Scheduling delayed initialization in ${DELAY_INIT_MS}ms")
            mainHandler.postDelayed({
                try {
                    LogManager.log("I", TAG, "Delayed initialization starting, isInitialized=$isInitialized")
                    if (!isInitialized) {
                        android.util.Log.d(TAG, "Delayed initialization starting")
                        LogManager.log("I", TAG, "Step 1: Creating overlay view")
                        // 分步创建：先创建遮挡层
                        if (createOverlayView()) {
                            android.util.Log.d(TAG, "Overlay view created successfully")
                            LogManager.log("I", TAG, "Overlay view created successfully")
                            // 成功后再创建控制窗口
                            LogManager.log("I", TAG, "Step 2: Creating control window")
                            if (createControlWindow()) {
                                android.util.Log.d(TAG, "Control window created successfully")
                                LogManager.log("I", TAG, "Control window created successfully")
                            } else {
                                LogManager.log("W", TAG, "Control window creation failed")
                            }
                            LogManager.log("I", TAG, "Step 3: Setting up foldable handler")
                            setupFoldableHandler()
                            LogManager.log("I", TAG, "Foldable handler setup completed")
                            isInitialized = true
                            android.util.Log.d(TAG, "Service initialized successfully")
                            LogManager.log("I", TAG, "=== Service initialized successfully ===")
                        } else {
                            android.util.Log.e(TAG, "Failed to create overlay view")
                            LogManager.log("E", TAG, "Failed to create overlay view")
                        }
                    } else {
                        LogManager.log("W", TAG, "Service already initialized, skipping delayed init")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error during delayed initialization", e)
                    LogManager.log("E", TAG, "Error during delayed initialization", e)
                    e.printStackTrace()
                    // 记录完整的堆栈跟踪
                    val sw = java.io.StringWriter()
                    val pw = java.io.PrintWriter(sw)
                    e.printStackTrace(pw)
                    android.util.Log.e(TAG, "Full stack trace: ${sw.toString()}")
                    LogManager.log("E", TAG, "Full stack trace: ${sw.toString()}")
                }
            }, DELAY_INIT_MS)
            LogManager.log("I", TAG, "Delayed initialization scheduled")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in onCreate", e)
            LogManager.log("E", TAG, "=== CRITICAL ERROR in onCreate ===", e)
            e.printStackTrace()
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            e.printStackTrace(pw)
            val stackTrace = sw.toString()
            android.util.Log.e(TAG, "Full stack trace: $stackTrace")
            LogManager.log("E", TAG, "Full stack trace: $stackTrace")
            try {
                stopSelf()
            } catch (stopException: Exception) {
                LogManager.log("E", TAG, "Error stopping service", stopException)
            }
        } finally {
            LogManager.log("I", TAG, "=== OverlayService.onCreate() END ===")
        }
    }
    
    private fun checkOverlayPermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val result = android.provider.Settings.canDrawOverlays(applicationContext)
                android.util.Log.d(TAG, "Overlay permission check: $result")
                result
            } else {
                android.util.Log.d(TAG, "SDK < 23, permission granted by default")
                true
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error checking overlay permission", e)
            false
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d(TAG, "onStartCommand called, flags: $flags, startId: $startId")
        
        // 如果服务已经初始化，检查窗口是否还在
        if (isInitialized) {
            mainHandler.post {
                try {
                    LogManager.log("I", TAG, "Checking windows in onStartCommand")
                    // 检查并重新创建窗口（如果不存在）
                    if (overlayView == null || overlayView?.parent == null) {
                        android.util.Log.w(TAG, "Overlay view missing, recreating...")
                        LogManager.log("W", TAG, "Overlay view missing, recreating...")
                        createOverlayView()
                    }
                    if (controlWindow == null) {
                        android.util.Log.w(TAG, "Control window missing, recreating...")
                        LogManager.log("W", TAG, "Control window missing, recreating...")
                        createControlWindow()
                    }
                    LogManager.log("I", TAG, "Window check completed in onStartCommand")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error recreating windows", e)
                    LogManager.log("E", TAG, "Error recreating windows", e)
                }
            }
        }
        
        LogManager.log("I", TAG, "onStartCommand returning START_STICKY")
        return START_STICKY // 确保服务被系统杀死后自动重启
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        android.util.Log.d(TAG, "onDestroy called")
        try {
            isRunning = false
            isInitialized = false
            
            // 清理所有资源
            mainHandler.removeCallbacksAndMessages(null)
            removeOverlayView()
            controlWindow?.dismiss()
            foldableHandler?.stopTracking()
            
            android.util.Log.d(TAG, "Service destroyed")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in onDestroy", e)
        }
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "字幕遮挡服务",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "字幕遮挡服务正在运行"
                    setShowBadge(false)
                }
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
                android.util.Log.d(TAG, "Notification channel created")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating notification channel", e)
        }
    }
    
    private fun createNotification(): Notification {
        return try {
            val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }
            
            notificationBuilder
                .setContentTitle("字幕遮挡服务")
                .setContentText("遮挡层正在运行")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
            
            // Android 14+ 需要设置通知可见性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
            }
            
            notificationBuilder.build()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating notification", e)
            e.printStackTrace()
            // 返回一个基本的通知
            try {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setContentTitle("字幕遮挡服务")
                    .setContentText("服务运行中")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setOngoing(true)
                    .build()
            } catch (e2: Exception) {
                android.util.Log.e(TAG, "Error creating fallback notification", e2)
                // 最后的降级方案 - 创建一个空通知
                @Suppress("DEPRECATION")
                Notification()
            }
        }
    }
    
    private fun loadSettings() {
        try {
            val metrics = windowManager?.let { 
                val displayMetrics = android.util.DisplayMetrics()
                it.defaultDisplay.getMetrics(displayMetrics)
                displayMetrics
            } ?: run {
                android.util.Log.w(TAG, "WindowManager is null, using default settings")
                return
            }
            
            overlayX = sharedPreferences?.getInt(KEY_X, metrics.widthPixels / 2) ?: metrics.widthPixels / 2
            overlayY = sharedPreferences?.getInt(KEY_Y, (metrics.heightPixels * 0.8).toInt()) ?: (metrics.heightPixels * 0.8).toInt()
            overlayWidth = sharedPreferences?.getInt(KEY_WIDTH, metrics.widthPixels) ?: metrics.widthPixels
            overlayHeight = sharedPreferences?.getInt(KEY_HEIGHT, (metrics.heightPixels * 0.2).toInt()) ?: (metrics.heightPixels * 0.2).toInt()
            overlayAlpha = sharedPreferences?.getFloat(KEY_ALPHA, 0.5f) ?: 0.5f
            overlayColor = sharedPreferences?.getInt(KEY_COLOR, 0xFF000000.toInt()) ?: 0xFF000000.toInt()
            overlayRotation = sharedPreferences?.getFloat(KEY_ROTATION, 0f) ?: 0f
            
            android.util.Log.d(TAG, "Settings loaded: x=$overlayX, y=$overlayY, w=$overlayWidth, h=$overlayHeight")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading settings", e)
        }
    }
    
    private fun saveSettings() {
        sharedPreferences?.edit()?.apply {
            putInt(KEY_X, overlayX)
            putInt(KEY_Y, overlayY)
            putInt(KEY_WIDTH, overlayWidth)
            putInt(KEY_HEIGHT, overlayHeight)
            putFloat(KEY_ALPHA, overlayAlpha)
            putInt(KEY_COLOR, overlayColor)
            putFloat(KEY_ROTATION, overlayRotation)
            apply()
        }
    }
    
    private fun createOverlayView(): Boolean {
        return try {
            android.util.Log.d(TAG, "Creating overlay view...")
            
            // 再次检查权限
            if (!checkOverlayPermission()) {
                android.util.Log.e(TAG, "Overlay permission not granted, cannot create view")
                return false
            }
            
            // 如果窗口已存在，先移除
            if (overlayView != null && overlayView?.parent != null) {
                android.util.Log.d(TAG, "Overlay view already exists, removing first")
                removeOverlayView()
            }
            
            val wm = windowManager
            if (wm == null) {
                android.util.Log.e(TAG, "WindowManager is null, cannot create overlay view")
                return false
            }
            
            // 直接创建View，不依赖布局文件
            overlayView = View(applicationContext).apply {
                setBackgroundColor(overlayColor)
                alpha = overlayAlpha
                rotation = overlayRotation
                visibility = if (isOverlayVisible) View.VISIBLE else View.GONE
            }
            
            if (overlayView == null) {
                android.util.Log.e(TAG, "Failed to create overlay view")
                return false
            }
            
            overlayParams = WindowManager.LayoutParams(
                overlayWidth,
                overlayHeight,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = overlayX
                y = overlayY
                // 锁定方向，不受屏幕旋转影响
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    @Suppress("DEPRECATION")
                    preferredDisplayModeId = 0
                }
            }
            
            // 在添加视图前再次检查权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(applicationContext)) {
                    android.util.Log.e(TAG, "Permission check failed before addView")
                    return false
                }
            }
            
            try {
                wm.addView(overlayView, overlayParams)
                android.util.Log.d(TAG, "Overlay view created and added successfully")
                LogManager.log("I", TAG, "Overlay view created and added successfully")
            } catch (e: android.view.WindowManager.BadTokenException) {
                android.util.Log.e(TAG, "BadTokenException when adding overlay view", e)
                LogManager.log("E", TAG, "BadTokenException when adding overlay view", e)
                return false
            } catch (e: SecurityException) {
                android.util.Log.e(TAG, "SecurityException when adding overlay view - permission issue", e)
                LogManager.log("E", TAG, "SecurityException when adding overlay view - permission issue", e)
                return false
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception when adding overlay view", e)
                LogManager.log("E", TAG, "Exception when adding overlay view", e)
                e.printStackTrace()
                return false
            }
            
            // 添加完整的手势识别：单指拖拽、双指缩放、双指旋转
            overlayView?.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var lastPointerCount = 0
                
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if (event == null || overlayParams == null) return false
                    
                    try {
                        val pointerCount = event.pointerCount
                        
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                // 单指按下，准备拖拽
                                initialX = overlayParams.x
                                initialY = overlayParams.y
                                initialTouchX = event.rawX
                                initialTouchY = event.rawY
                                isMultiTouch = false
                                lastPointerCount = 1
                                return true
                            }
                            
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                // 多点触控开始，准备缩放或旋转
                                if (pointerCount == 2) {
                                    isMultiTouch = true
                                    val x0 = event.getX(0)
                                    val y0 = event.getY(0)
                                    val x1 = event.getX(1)
                                    val y1 = event.getY(1)
                                    
                                    // 计算初始距离和角度
                                    initialDistance = sqrt((x1 - x0).pow(2) + (y1 - y0).pow(2))
                                    initialRotation = atan2(y1 - y0, x1 - x0)
                                    initialScale = 1f
                                    
                                    // 计算中心点
                                    initialCenterX = (x0 + x1) / 2
                                    initialCenterY = (y0 + y1) / 2
                                    
                                    // 保存初始大小和旋转
                                    initialScale = 1f
                                    lastPointerCount = 2
                                }
                                return true
                            }
                            
                            MotionEvent.ACTION_MOVE -> {
                                if (isMultiTouch && pointerCount == 2) {
                                    // 双指操作：缩放和旋转
                                    val x0 = event.getX(0)
                                    val y0 = event.getY(0)
                                    val x1 = event.getX(1)
                                    val y1 = event.getY(1)
                                    
                                    // 计算当前距离和角度
                                    val currentDistance = sqrt((x1 - x0).pow(2) + (y1 - y0).pow(2))
                                    val currentRotation = atan2(y1 - y0, x1 - x0)
                                    
                                    // 计算缩放比例
                                    if (initialDistance > 0) {
                                        val scale = currentDistance / initialDistance
                                        val newWidth = (overlayWidth * scale).toInt().coerceAtLeast(50)
                                        val newHeight = (overlayHeight * scale).toInt().coerceAtLeast(50)
                                        
                                        // 限制最大尺寸（屏幕尺寸）
                                        val metrics = wm.let {
                                            val displayMetrics = android.util.DisplayMetrics()
                                            it.defaultDisplay.getMetrics(displayMetrics)
                                            displayMetrics
                                        }
                                        overlayWidth = newWidth.coerceAtMost(metrics.widthPixels)
                                        overlayHeight = newHeight.coerceAtMost(metrics.heightPixels)
                                    }
                                    
                                    // 计算旋转角度（度）
                                    val rotationDelta = Math.toDegrees((currentRotation - initialRotation).toDouble()).toFloat()
                                    overlayRotation = (overlayRotation + rotationDelta).let {
                                        // 限制在0-360度
                                        if (it < 0) it + 360f else if (it >= 360f) it - 360f else it
                                    }
                                    
                                    // 更新视图
                                    updateOverlaySize()
                                    updateOverlayRotation()
                                    
                                    // 更新初始值，用于连续操作
                                    initialDistance = currentDistance
                                    initialRotation = currentRotation
                                    
                                } else if (!isMultiTouch && pointerCount == 1) {
                                    // 单指拖拽
                                    val deltaX = (event.rawX - initialTouchX).toInt()
                                    val deltaY = (event.rawY - initialTouchY).toInt()
                                    overlayX = (initialX + deltaX).coerceAtLeast(0)
                                    overlayY = (initialY + deltaY).coerceAtLeast(0)
                                    updateOverlayPosition()
                                }
                                return true
                            }
                            
                            MotionEvent.ACTION_POINTER_UP -> {
                                // 一个手指抬起
                                if (pointerCount == 2) {
                                    // 还剩一个手指，切换回单指模式
                                    val remainingIndex = if (event.actionIndex == 0) 1 else 0
                                    initialTouchX = event.getX(remainingIndex)
                                    initialTouchY = event.getY(remainingIndex)
                                    initialX = overlayParams.x
                                    initialY = overlayParams.y
                                    isMultiTouch = false
                                    lastPointerCount = 1
                                }
                                return true
                            }
                            
                            MotionEvent.ACTION_UP -> {
                                // 所有手指抬起，保存设置
                                isMultiTouch = false
                                lastPointerCount = 0
                                // 延迟保存，避免频繁IO
                                mainHandler.postDelayed({
                                    saveSettings()
                                }, 500)
                                return true
                            }
                            
                            MotionEvent.ACTION_CANCEL -> {
                                isMultiTouch = false
                                lastPointerCount = 0
                                return true
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error in touch listener", e)
                        LogManager.log("E", TAG, "Error in touch listener", e)
                    }
                    return false
                }
            })
            
            true // 成功创建
        } catch (e: android.view.WindowManager.BadTokenException) {
            android.util.Log.e(TAG, "BadTokenException creating overlay view", e)
            LogManager.log("E", TAG, "BadTokenException creating overlay view", e)
            e.printStackTrace()
            false
        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "SecurityException creating overlay view - permission denied", e)
            LogManager.log("E", TAG, "SecurityException creating overlay view - permission denied", e)
            e.printStackTrace()
            false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating overlay view", e)
            LogManager.log("E", TAG, "Error creating overlay view", e)
            e.printStackTrace()
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            e.printStackTrace(pw)
            android.util.Log.e(TAG, "Full stack trace: ${sw.toString()}")
            false
        }
    }
    
    private fun setupFoldableHandler() {
        try {
            val wm = windowManager
            if (wm != null) {
                foldableHandler = FoldableHandler(applicationContext, wm) { layoutInfo ->
                    handleLayoutChange(layoutInfo)
                }
                foldableHandler?.startTracking()
                android.util.Log.d(TAG, "Foldable handler setup completed")
            } else {
                android.util.Log.w(TAG, "WindowManager is null, skipping foldable handler setup")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error setting up foldable handler", e)
            // 折叠屏功能不是必须的，失败不影响主功能
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
    
    private fun createControlWindow(): Boolean {
        return try {
            android.util.Log.d(TAG, "Creating control window...")
            
            // 再次检查权限
            if (!checkOverlayPermission()) {
                android.util.Log.e(TAG, "Overlay permission not granted, cannot create control window")
                return false
            }
            
            // 如果控制窗口已存在，先移除
            controlWindow?.dismiss()
            controlWindow = null
            
            val wm = windowManager
            if (wm == null) {
                android.util.Log.e(TAG, "WindowManager is null, cannot create control window")
                return false
            }
            
            val metrics = wm.let { 
                val displayMetrics = android.util.DisplayMetrics()
                it.defaultDisplay.getMetrics(displayMetrics)
                displayMetrics
            }
            
            // 使用Application Context确保控制窗口独立运行
            controlWindow = ControlWindow(applicationContext, wm, object : ControlWindow.Callback {
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
            
            override fun onColorChange(color: Int) {
                overlayColor = color
                updateOverlayColor()
            }
            
            override fun onRotationChange(rotation: Float) {
                overlayRotation = rotation
                updateOverlayRotation()
            }
        })
        controlWindow?.show()
        
        // 同步控制窗口的滑块值
        metrics?.let { m ->
            val widthPercent = (overlayWidth * 100 / m.widthPixels).coerceIn(0, 100)
            val heightPercent = (overlayHeight * 100 / m.heightPixels).coerceIn(0, 100)
            val alphaPercent = (overlayAlpha * 100).toInt().coerceIn(0, 100)
            val rotationPercent = overlayRotation.toInt().coerceIn(0, 360)
            controlWindow?.syncSliders(widthPercent, heightPercent, alphaPercent, rotationPercent)
            controlWindow?.syncColor(overlayColor)
            }
            
            android.util.Log.d(TAG, "Control window created successfully")
            LogManager.log("I", TAG, "Control window created successfully")
            true // 成功创建
        } catch (e: android.view.WindowManager.BadTokenException) {
            android.util.Log.e(TAG, "BadTokenException creating control window", e)
            LogManager.log("E", TAG, "BadTokenException creating control window", e)
            e.printStackTrace()
            false
        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "SecurityException creating control window - permission denied", e)
            LogManager.log("E", TAG, "SecurityException creating control window - permission denied", e)
            e.printStackTrace()
            false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating control window", e)
            LogManager.log("E", TAG, "Error creating control window", e)
            e.printStackTrace()
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            e.printStackTrace(pw)
            android.util.Log.e(TAG, "Full stack trace: ${sw.toString()}")
            false
        }
    }
    
    private fun updateOverlayPosition() {
        try {
            overlayParams?.let { params ->
                params.x = overlayX.coerceAtLeast(0)
                params.y = overlayY.coerceAtLeast(0)
                windowManager?.updateViewLayout(overlayView, params)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating overlay position", e)
        }
    }
    
    private fun updateOverlaySize() {
        try {
            overlayParams?.let { params ->
                params.width = overlayWidth
                params.height = overlayHeight
                windowManager?.updateViewLayout(overlayView, params)
                saveSettings()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating overlay size", e)
        }
    }
    
    private fun updateOverlayAlpha() {
        try {
            overlayView?.alpha = overlayAlpha
            saveSettings()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating overlay alpha", e)
        }
    }
    
    private fun updateOverlayVisibility() {
        try {
            overlayView?.visibility = if (isOverlayVisible) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating overlay visibility", e)
        }
    }
    
    private fun updateOverlayColor() {
        try {
            overlayView?.setBackgroundColor(overlayColor)
            saveSettings()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating overlay color", e)
        }
    }
    
    private fun updateOverlayRotation() {
        try {
            overlayView?.rotation = overlayRotation
            // 不立即保存，避免频繁IO，在触摸结束时统一保存
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating overlay rotation", e)
        }
    }
    
    private fun removeOverlayView() {
        overlayView?.let { view ->
            try {
                if (view.parent != null) {
                    windowManager?.removeView(view)
                    android.util.Log.d(TAG, "Overlay view removed")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error removing overlay view", e)
            }
            overlayView = null
            overlayParams = null
        }
    }
}

