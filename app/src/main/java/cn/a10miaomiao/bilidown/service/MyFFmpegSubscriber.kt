package cn.a10miaomiao.bilidown.service

import cn.a10miaomiao.bilidown.common.ExportLog
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.state.AppState
import cn.a10miaomiao.bilidown.state.TaskStatus
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.Level
import com.arthenica.ffmpegkit.LogCallback

open class MyFFmpegSubscriber(
    private val appState: AppState,
) {

    fun execute(args: List<String>) {
        FFmpegKit.executeWithArgumentsAsync(
            args.toTypedArray(),
            FFmpegSessionCompleteCallback { session ->
                when {
                    com.arthenica.ffmpegkit.ReturnCode.isSuccess(session.returnCode) -> onFinish()
                    com.arthenica.ffmpegkit.ReturnCode.isCancel(session.returnCode) -> onCancel()
                    else -> onError(
                        session.failStackTrace
                            ?: "ffmpeg 返回码: ${session.returnCode.value}"
                    )
                }
            },
            LogCallback { log ->
                if (log.level == Level.AV_LOG_ERROR || log.level == Level.AV_LOG_FATAL) {
                    ExportLog.error("ffmpeg: ${log.message.trim()}")
                }
            },
            null
        )
    }

    open fun onFinish() {
        ExportLog.info("ffmpeg 完成")
        appState.putTaskStatus(TaskStatus.InIdle)
    }

    open fun onCancel() {
        ExportLog.warn("ffmpeg 被取消")
        appState.putTaskStatus(TaskStatus.InIdle)
    }

    open fun onError(message: String) {
        ExportLog.error("ffmpeg 错误: $message")
        MiaoLog.info { message }
        appState.putTaskStatus(
            TaskStatus.Error(
                appState.taskStatus.value,
                message
            )
        )
    }
}
