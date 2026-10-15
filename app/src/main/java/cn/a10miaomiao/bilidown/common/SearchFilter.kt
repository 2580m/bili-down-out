package cn.a10miaomiao.bilidown.common

import cn.a10miaomiao.bilidown.entity.DownloadInfo

/**
 * 根据搜索关键词过滤下载列表，支持标题和子项索引标题匹配（不区分大小写）。
 *
 * - 空白查询返回原始完整列表
 * - [DownloadInfo.title] 匹配 → 保留该条目的所有子项
 * - [DownloadItemInfo.title] 或 [DownloadItemInfo.index_title] 匹配 → 保留该条目，但仅保留匹配的子项
 * - 无任何匹配 → 过滤掉该条目
 */
fun List<DownloadInfo>.filterBySearchQuery(query: String): List<DownloadInfo> {
    if (query.isBlank()) return this
    return mapNotNull { info ->
        val titleMatches = info.title.contains(query, ignoreCase = true)
        val matchedItems = info.items.filter { item ->
            item.title.contains(query, ignoreCase = true)
                || item.index_title.contains(query, ignoreCase = true)
        }
        when {
            titleMatches -> info          // 标题命中，保留全部子项
            matchedItems.isNotEmpty() -> info.copy(items = matchedItems.toMutableList())
            else -> null                   // 无匹配，过滤掉
        }
    }
}
