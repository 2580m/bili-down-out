package cn.a10miaomiao.bilidown.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 本地内置的少量 Material 图标。
 *
 * 替代体积巨大的 material-icons-extended 依赖，仅保留 App 实际用到的
 * 几个 extended 图标（其余图标由 material-icons-core 提供）。
 */
object AppIcons {

    private fun buildIcon(name: String, pathData: String): ImageVector {
        val nodes = PathParser().parsePathString(pathData).toNodes()
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = nodes,
            fill = SolidColor(Color.Black),
        ).build()
    }

    /** Material Icons "sort" */
    val Sort: ImageVector by lazy {
        buildIcon(
            name = "Sort",
            pathData = "M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z",
        )
    }

    /** Material Icons "content_copy" */
    val ContentCopy: ImageVector by lazy {
        buildIcon(
            name = "ContentCopy",
            pathData = "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
        )
    }

    /** Material Icons "description" */
    val Description: ImageVector by lazy {
        buildIcon(
            name = "Description",
            pathData = "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
        )
    }
}
