package cn.a10miaomiao.bilidown.common

import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 安全解析 Bilibili 缓存的 entry.json 文件。
 *
 * 处理空文件、损坏的 JSON 以及格式不完整的输入，避免整个列表读取崩溃。
 */
object BiliEntryJsonParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val jsonStrict = Json {
        ignoreUnknownKeys = true
    }

    /**
     * 尝试解析 entry.json 内容（兼容新版 Bilibili 缓存，自动转换字段类型）。
     */
    fun parseOrNull(jsonString: String): BiliDownloadEntryInfo? {
        if (jsonString.isBlank()) return null
        return try {
            json.decodeFromString(BiliDownloadEntryInfo.serializer(), jsonString)
        } catch (e: SerializationException) { null }
        catch (e: IllegalArgumentException) { null }
    }

    /**
     * 用原始严格模式解析（与原版行为完全一致），仅检查空文件。
     */
    fun parseOrNullStrict(jsonString: String): BiliDownloadEntryInfo? {
        if (jsonString.isBlank()) return null
        return try {
            jsonStrict.decodeFromString(BiliDownloadEntryInfo.serializer(), jsonString)
        } catch (e: SerializationException) { null }
        catch (e: IllegalArgumentException) { null }
    }
}
