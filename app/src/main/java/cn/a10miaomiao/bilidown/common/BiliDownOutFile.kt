package cn.a10miaomiao.bilidown.common

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File

class BiliDownOutFile(
    name: String,
) {

    companion object {
        const val DIR_NAME = "BiliDownOut"
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        /**
         * Android 共享存储（FUSE）会拒绝创建含 FAT/Windows 非法字符的文件名，
         * 例如 | : ? * " < > \，这里统一替换为安全字符，避免 ffmpeg 写入时报“找不到文件”。
         */
        private val INVALID_FILE_NAME_CHARS = Regex("[\\\\/:*?\"<>|]")

        fun sanitizeFileName(name: String): String {
            return name.replace(INVALID_FILE_NAME_CHARS, "-")
        }

        fun getOutFolderUri(): Uri {
            return Uri.parse("content://com.android.externalstorage.documents/document/primary:Download%2f${DIR_NAME}")
        }

        fun getOutFolderPath(): String {
            return downloadDir.path + File.separator + DIR_NAME
        }
    }

    private val outDir = File(downloadDir, DIR_NAME)

    init {
        if (!outDir.exists()){
            outDir.mkdirs()
        }
    }

    val file = File(outDir, sanitizeFileName(name))

    val path get() = file.path
    val name get() = file.name

    fun exists() = file.exists()
}
