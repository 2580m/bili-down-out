package cn.a10miaomiao.bilidown.shizuku.service

import android.content.Context
import android.os.RemoteException
import android.system.Os
import android.util.Log
import androidx.annotation.Keep
import cn.a10miaomiao.bilidown.callback.ProgressCallback
import cn.a10miaomiao.bilidown.common.BiliEntryJsonParser
import cn.a10miaomiao.bilidown.common.CommandUtil
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryAndPathInfo
import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryInfo
import cn.a10miaomiao.bilidown.entity.VideoOutInfo
import cn.a10miaomiao.bilidown.shizuku.IUserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext


class UserService: IUserService.Stub, CoroutineScope {

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /**
     * Constructor is required.
     */
    constructor() {
        Log.i("UserService", "constructor")
    }

    /**
     * Constructor with Context. This is only available from Shizuku API v13.
     *
     *
     * This method need to be annotated with [Keep] to prevent ProGuard from removing it.
     *
     * @param context Context created with createPackageContextAsUser
     * @see [code used to create the instance of this class](https://github.com/RikkaApps/Shizuku-API/blob/672f5efd4b33c2441dbf609772627e63417587ac/server-shared/src/main/java/rikka/shizuku/server/UserService.java.L66)
     */
    @Keep
    constructor(context: Context) {
        Log.i("UserService", "constructor with Context: context=$context")
    }

    /**
     * Reserved destroy method
     */
    override fun destroy() {
        Log.i("UserService", "destroy")
        job.cancel()
        System.exit(0)
    }

    override fun exit() {
        destroy()
    }

    @Throws(RemoteException::class)
    override fun doSomething(): String {
        Log.i("UserService", "doSomething," + "pid=" + Os.getpid() + ", uid=" + Os.getuid())
        return "pid=" + Os.getpid() + ", uid=" + Os.getuid()
    }

    @Throws(RemoteException::class)
    override fun readDownloadList(path: String?): MutableList<BiliDownloadEntryAndPathInfo> {
        if (path.isNullOrBlank()) {
            return mutableListOf()
        }
        MiaoLog.debug { "readDownloadList(${path})" }
        val dir = File(path)
        val list = mutableListOf<BiliDownloadEntryAndPathInfo>()
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()
                ?.filter { it.isDirectory }
                ?.forEach {
                    MiaoLog.debug { it.path }
                    list.addAll(readDownloadDirectory(it.path))
                }
        }
        return list
    }

    @Throws(RemoteException::class)
    override fun readDownloadDirectory(path: String?): MutableList<BiliDownloadEntryAndPathInfo> {
        if (path.isNullOrBlank()) {
            return mutableListOf()
        }
        val dir = File(path)
        val list = mutableListOf<BiliDownloadEntryAndPathInfo>()
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()
                ?.filter { pageDir -> pageDir.isDirectory }
                ?.map {
                    Pair(it, File(it.path + "/entry.json"))
                }
                ?.filter { it.second.exists() }
                ?.forEach {
                    val (entryDir, entryFile) = it
                    val entryJson = entryFile.readText()
                    val entry = BiliEntryJsonParser.parseOrNull(entryJson)
                        ?: return@forEach
                    list.add(BiliDownloadEntryAndPathInfo(
                        entry = entry,
                        entryDirPath = entryDir.path,
                        pageDirPath = dir.path
                    ))
                }
        }
        return list
    }

    override fun exportBiliVideo(
        entryDirPath: String?,
        outFilePath: String?,
        callback: ProgressCallback?
    ): String? {
        val entryDirFile = File(entryDirPath)
        val entryJsonFile = File(entryDirPath, "entry.json")
        val outFile = File(outFilePath)
        val json = Json { ignoreUnknownKeys = true }
        val entry = json.decodeFromString<BiliDownloadEntryInfo>(entryJsonFile.readText())
        val videoDirPath = entryDirPath + "/" + entry.videoDirName
        val videoDir = File(videoDirPath)
        val videoOutInfo = VideoOutInfo(
            entryDirPath = entryDirPath!!,
            outFilePath = outFilePath!!,
            name = outFile.name,
            cover = entry.cover,
        )
        val ffmpegSubscriber = ShizukuFFmpegSubscriber(
            videoOutInfo, callback
        )
        if (!videoDir.exists() || !videoDir.isDirectory) {
            val videoFile = entryDirFile.listFiles()?.find {
                it.isFile && it.name.startsWith(entry.videoDirName)
                        && it.name.endsWith(".mp4")
            }
            if (videoFile == null) {
                return "找不到视频文件夹：${entry.videoDirName}"
            }
            // 直接复制mp4文件
            callback?.onStart(videoOutInfo)
            launch {
                copyFile(videoFile, outFile)
                callback?.onFinish(videoOutInfo)
            }
            return null
        }
        if (entry.media_type == 1) {
            // 多段blv(flv)视频
            val blvFiles = mutableListOf<File>()
            var blvIndex = 0
            while (true) {
                val file = File(videoDir, "/${blvIndex}.blv")
                if (file.exists()) {
                    blvFiles.add(file)
                    blvIndex++
                } else {
                    break
                }
            }
            if (blvFiles.isEmpty()) {
                return "找不到视频文件：0.blv"
            }
            callback?.onStart(videoOutInfo)
            try {
                mergerVideos(blvFiles, outFile, ffmpegSubscriber)
            } catch (e: Exception) {
                callback?.onError(videoOutInfo, "ffmpeg blv 合并启动失败: ${e.message}")
                return null
            }
            return null
        } else {
            val videoFile = File(videoDir, "video.m4s")
            val audioFile = File(videoDir, "audio.m4s")
            if (!videoFile.exists()) {
                return "找不到视频文件"
            }
            if (!audioFile.exists()) {
                callback?.onStart(videoOutInfo)
                launch {
                    copyFile(videoFile, outFile)
                    callback?.onFinish(videoOutInfo)
                }
                return null
            }
            callback?.onStart(videoOutInfo)
            try {
                mergerVideoAndAudio(videoFile, audioFile, outFile, ffmpegSubscriber)
            } catch (e: Exception) {
                callback?.onError(videoOutInfo, "ffmpeg 启动失败: ${e.message}")
                return null
            }
            return null
        }
    }

    private suspend fun copyFile(
        inputFile: File,
        outFile: File
    ) {
        val fileInputStream = FileInputStream(inputFile)
        val fileOutputStream = FileOutputStream(outFile)
        val buffer = ByteArray(1024)
        var byteRead: Int
        while (-1 != fileInputStream.read(buffer).also { byteRead = it }) {
            fileOutputStream.write(buffer, 0, byteRead)
        }
        fileInputStream.close()
        fileOutputStream.flush()
        fileOutputStream.close()
    }

    private fun mergerVideoAndAudio(
        videoFile: File,
        audioFile: File,
        outFile: File,
        subscriber: ShizukuFFmpegSubscriber,
    ) {
        if (!outFile.parentFile!!.exists()){
            outFile.parentFile!!.mkdirs()
        }
        val commands = listOf(
            "-i", videoFile.absolutePath,
            "-i", audioFile.absolutePath,
            "-c:v", "copy",
            "-strict", "experimental",
            outFile.absolutePath
        )
        //开始执行FFmpeg命令
        subscriber.execute(commands)
    }

    private fun mergerVideos(
        videoFiles: List<File>,
        outFile: File,
        subscriber: ShizukuFFmpegSubscriber,
    ) {
        if (!outFile.parentFile!!.exists()){
            outFile.parentFile!!.mkdirs()
        }
        val ffTxtFile = File(outFile.parent, ".ff.txt")
        if (ffTxtFile.exists() && ffTxtFile.isDirectory) {
            ffTxtFile.deleteRecursively()
            ffTxtFile.delete()
        }
        val ffTxtContent = videoFiles.map {
            file -> "file ${CommandUtil.filePath(file)}"
        }.joinToString("\n")
        ffTxtFile.writeText(ffTxtContent)

        val commands = listOf(
            "-f", "concat",
            "-safe", "0",
            "-i", ffTxtFile.absolutePath,
            "-c", "copy",
            outFile.absolutePath
        )
        //开始执行FFmpeg命令
        subscriber.execute(commands)
    }

}
