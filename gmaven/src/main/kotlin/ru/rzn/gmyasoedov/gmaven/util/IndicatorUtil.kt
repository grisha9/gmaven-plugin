package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

object IndicatorUtil {

    fun getTaskInfo(title: String, canBeCancelled: Boolean = true): Task.Backgroundable {
        return object : Task.Backgroundable(null, title, canBeCancelled) {
            override fun run(indicator: ProgressIndicator) {}
        }
    }
}