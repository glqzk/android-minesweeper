package com.zimu.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import com.zimu.overlay.databinding.ControlWindowBinding

class ControlWindow(
    private val context: Context,
    private val windowManager: WindowManager,
    private val callback: Callback
) {
    
    private var controlView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var binding: ControlWindowBinding? = null
    
    interface Callback {
        fun onMove(deltaX: Int, deltaY: Int)
        fun onSizeChange(widthPercent: Int, heightPercent: Int)
        fun onAlphaChange(alpha: Float)
        fun onToggleVisibility()
    }
    
    fun show() {
        if (controlView != null) return
        
        val b = ControlWindowBinding.inflate(LayoutInflater.from(context))
        binding = b
        controlView = b.root
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 200
        }
        
        windowManager.addView(controlView, params)
        
        setupControls(b)
    }
    
    private fun setupControls(binding: ControlWindowBinding) {
        // 方向控制按钮
        binding.btnUp.setOnClickListener {
            callback.onMove(0, -20)
        }
        
        binding.btnDown.setOnClickListener {
            callback.onMove(0, 20)
        }
        
        binding.btnLeft.setOnClickListener {
            callback.onMove(-20, 0)
        }
        
        binding.btnRight.setOnClickListener {
            callback.onMove(20, 0)
        }
        
        // 大小控制
        binding.seekBarWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val heightProgress = binding.seekBarHeight.progress
                    callback.onSizeChange(progress, heightProgress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        binding.seekBarHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val widthProgress = binding.seekBarWidth.progress
                    callback.onSizeChange(widthProgress, progress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 透明度控制
        binding.seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val alpha = progress / 100f
                    callback.onAlphaChange(alpha)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 显示/隐藏切换
        binding.btnToggle.setOnClickListener {
            callback.onToggleVisibility()
            val isVisible = binding.btnToggle.text.toString().contains("隐藏")
            binding.btnToggle.text = if (isVisible) "显示遮挡" else "隐藏遮挡"
        }
        
        // 初始化滑块位置（这些值会在OverlayService加载设置后更新）
        binding.seekBarWidth.progress = 100
        binding.seekBarHeight.progress = 20
        binding.seekBarAlpha.progress = 50
    }
    
    fun syncSliders(widthPercent: Int, heightPercent: Int, alphaPercent: Int) {
        binding?.let { b ->
            b.seekBarWidth.progress = widthPercent
            b.seekBarHeight.progress = heightPercent
            b.seekBarAlpha.progress = alphaPercent
        }
    }
    
    fun dismiss() {
        controlView?.let {
            windowManager.removeView(it)
            controlView = null
            params = null
            binding = null
        }
    }
}

