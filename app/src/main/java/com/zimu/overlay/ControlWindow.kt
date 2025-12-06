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
        fun onColorChange(color: Int)
        fun onRotationChange(rotation: Float)
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
        
        // 颜色选择
        binding.btnColorBlack.setOnClickListener {
            callback.onColorChange(0xFF000000.toInt())
            updateColorButtons(binding, 0xFF000000.toInt())
        }
        
        binding.btnColorWhite.setOnClickListener {
            callback.onColorChange(0xFFFFFFFF.toInt())
            updateColorButtons(binding, 0xFFFFFFFF.toInt())
        }
        
        binding.btnColorRed.setOnClickListener {
            callback.onColorChange(0xFFFF0000.toInt())
            updateColorButtons(binding, 0xFFFF0000.toInt())
        }
        
        binding.btnColorBlue.setOnClickListener {
            callback.onColorChange(0xFF0000FF.toInt())
            updateColorButtons(binding, 0xFF0000FF.toInt())
        }
        
        // 旋转角度控制
        binding.seekBarRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    callback.onRotationChange(progress.toFloat())
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 初始化滑块位置（这些值会在OverlayService加载设置后更新）
        binding.seekBarWidth.progress = 100
        binding.seekBarHeight.progress = 20
        binding.seekBarAlpha.progress = 50
        binding.seekBarRotation.progress = 0
    }
    
    private fun updateColorButtons(binding: ControlWindowBinding, selectedColor: Int) {
        // 重置所有按钮的选中状态（可以通过添加选中边框来实现）
        // 这里简化处理，实际可以添加选中状态的视觉反馈
    }
    
    fun syncSliders(widthPercent: Int, heightPercent: Int, alphaPercent: Int, rotationPercent: Int) {
        binding?.let { b ->
            b.seekBarWidth.progress = widthPercent
            b.seekBarHeight.progress = heightPercent
            b.seekBarAlpha.progress = alphaPercent
            b.seekBarRotation.progress = rotationPercent
        }
    }
    
    fun syncColor(color: Int) {
        // 可以根据颜色更新按钮选中状态
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

