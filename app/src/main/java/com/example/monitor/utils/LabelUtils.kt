package com.example.monitor.utils

import android.graphics.Color

object LabelUtils {

    fun getMappedLabelAndColor(label: String): Pair<String, Int> {
        return when (label.lowercase()) {
            "p0", "纵向裂缝" -> "纵向裂缝" to Color.RED
            "p1", "横向裂缝" -> "横向裂缝" to Color.BLUE
            "p2", "龟裂" -> "龟裂" to Color.GREEN
            "p3", "坑洞" -> "坑洞" to Color.parseColor("#FFA500") // 橙色
            "p4", "坑洞" -> "坑洞" to Color.parseColor("#FFA500") // 兼容P4
            else -> label to Color.RED // 默认颜色
        }
    }

    fun getMappedLabel(label: String): String {
        return getMappedLabelAndColor(label).first
    }

    fun getMappedColor(label: String): Int {
        return getMappedLabelAndColor(label).second
    }
}