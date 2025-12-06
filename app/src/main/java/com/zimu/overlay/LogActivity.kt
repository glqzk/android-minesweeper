package com.zimu.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.appcompat.app.AppCompatActivity
import com.zimu.overlay.databinding.ActivityLogBinding
import java.io.File

class LogActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLogBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadLogs()
        
        binding.btnClear.setOnClickListener {
            LogManager.clearLogs()
            loadLogs()
            Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnExport.setOnClickListener {
            exportLogs()
        }
    }
    
    private fun loadLogs() {
        try {
            val logs = LogManager.getAllLogs()
            val logText = if (logs.isEmpty()) {
                "暂无日志"
            } else {
                logs.joinToString("\n")
            }
            
            binding.tvLogs.text = logText
            
            // 自动滚动到底部
            binding.scrollView.post {
                binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
            
            binding.tvStatus.text = "共 ${logs.size} 条日志"
        } catch (e: Exception) {
            binding.tvLogs.text = "加载日志失败: ${e.message}"
            android.util.Log.e("LogActivity", "Error loading logs", e)
        }
    }
    
    private fun exportLogs() {
        try {
            val exportFile = LogManager.exportLogs(this)
            if (exportFile != null) {
                // 获取文件的URI（支持Android 7.0+的FileProvider）
                val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        exportFile
                    )
                } else {
                    Uri.fromFile(exportFile)
                }
                
                // 使用文件分享Intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "字幕遮挡应用日志")
                    putExtra(Intent.EXTRA_TEXT, "日志文件已导出")
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "分享日志文件"))
                binding.tvStatus.text = "日志已导出: ${exportFile.name}"
                Toast.makeText(this, "日志已导出到: ${exportFile.absolutePath}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("LogActivity", "Error exporting logs", e)
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 每次返回时刷新日志
        loadLogs()
    }
}

