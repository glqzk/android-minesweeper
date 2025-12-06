package com.zimu.overlay

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FoldableHandler(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onLayoutChange: (WindowLayoutInfo) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var windowInfoTracker: WindowInfoTracker? = null
    
    fun startTracking() {
        try {
            windowInfoTracker = WindowInfoTracker.getOrCreate(context)
            windowInfoTracker?.windowLayoutInfo(context)
                ?.onEach { layoutInfo ->
                    onLayoutChange(layoutInfo)
                }
                ?.launchIn(scope)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun stopTracking() {
        // WindowInfoTracker会自动管理生命周期
    }
    
    fun getCurrentWindowMetrics(): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            metrics.bounds
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }
    
    fun getDisplayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }
    
    fun isFolded(layoutInfo: WindowLayoutInfo): Boolean {
        return layoutInfo.displayFeatures.any { feature ->
            feature is androidx.window.layout.FoldingFeature &&
            feature.state == androidx.window.layout.FoldingFeature.State.HALF_OPENED
        }
    }
    
    fun getFoldingFeature(layoutInfo: WindowLayoutInfo): androidx.window.layout.FoldingFeature? {
        return layoutInfo.displayFeatures.firstOrNull { feature ->
            feature is androidx.window.layout.FoldingFeature
        } as? androidx.window.layout.FoldingFeature
    }
}

