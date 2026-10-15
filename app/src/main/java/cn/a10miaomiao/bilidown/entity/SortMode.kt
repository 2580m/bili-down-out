package cn.a10miaomiao.bilidown.entity

/**
 * 列表排序模式。
 * @param label 弹窗中的显示文字（含方向符号）
 */
enum class SortMode(val label: String) {
    NONE("没有排序"),
    NAME_ASC("名称 ▲"),
    NAME_DESC("名称 ▼"),
    TIME_ASC("时间 ▲"),
    TIME_DESC("时间 ▼"),
    AV_ASC("AV号 ▲"),
    AV_DESC("AV号 ▼"),
}

/**
 * 对下载列表应用指定排序，返回新列表（不修改原列表）。
 * [SortMode.NONE] 时返回原列表。
 */
fun List<DownloadInfo>.applySort(mode: SortMode): List<DownloadInfo> {
    return when (mode) {
        SortMode.NONE -> this
        SortMode.NAME_ASC -> sortedBy { it.title }
        SortMode.NAME_DESC -> sortedByDescending { it.title }
        SortMode.TIME_ASC -> sortedBy { it.time_create_stamp }
        SortMode.TIME_DESC -> sortedByDescending { it.time_create_stamp }
        SortMode.AV_ASC -> sortedWith(
            compareBy<DownloadInfo> { it.avNumber() }.thenBy { it.avDirName() }
        )
        SortMode.AV_DESC -> sortedWith(
            compareByDescending<DownloadInfo> { it.avNumber() }.thenByDescending { it.avDirName() }
        )
    }
}

/**
 * AV号即缓存目录 download/ 下的子目录名（如 699141751、s_10211）。
 * 数字目录按数值排序，非数字目录（如番剧 s_xxx）排在数字之后。
 */
private fun DownloadInfo.avDirName(): String = dir_path.substringAfterLast('/')

private fun DownloadInfo.avNumber(): Long = avDirName().toLongOrNull() ?: Long.MAX_VALUE
