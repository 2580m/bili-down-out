package cn.a10miaomiao.bilidown.common

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * 导出过程日志收集器。
 * 在导出的每个关键节点调用 [add] 记录日志，通过 [entries] 读取。
 */
object ExportLog {

    data class Entry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: Level,
        val message: String,
    ) {
        enum class Level { INFO, WARN, ERROR }
    }

    val entries: SnapshotStateList<Entry> = mutableStateListOf()

    fun info(msg: String) {
        entries.add(Entry(level = Entry.Level.INFO, message = msg))
    }

    fun warn(msg: String) {
        entries.add(Entry(level = Entry.Level.WARN, message = msg))
    }

    fun error(msg: String) {
        entries.add(Entry(level = Entry.Level.ERROR, message = msg))
    }

    fun clear() {
        entries.clear()
    }

    /**
     * 格式化所有日志为纯文本，供复制用。
     */
    fun formatForCopy(): String = entries.joinToString("\n") { e ->
        val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date(e.timestamp))
        "[$time] [${e.level.name}] ${e.message}"
    }
}
