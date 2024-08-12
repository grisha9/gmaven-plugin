package ru.rzn.gmyasoedov.gmaven.project.process

import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.util.Key
import ru.rzn.gmyasoedov.gmaven.project.MavenTaskProcessSupport
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest

class GOSProcessHandler(
    private val request: GServerRequest,
    private val processConsumer: ((process: GOSProcessHandler) -> Unit)? = null
) : OSProcessHandler(MavenTaskProcessSupport(request).getCommandLine()) {

    init {
        addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                super.onTextAvailable(event, outputType)
                if (request.listener != null) {
                    request.listener.onTaskOutput(request.taskId, event.text, true)
                }
            }
        })
    }

    fun startAndWait() {
        processConsumer?.let { it(this) }
        startNotify()
        waitFor()
    }
}