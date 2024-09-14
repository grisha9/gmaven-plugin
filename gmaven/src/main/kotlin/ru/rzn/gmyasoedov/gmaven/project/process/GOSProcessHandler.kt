package ru.rzn.gmyasoedov.gmaven.project.process

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest

class GOSProcessHandler(
    private val request: GServerRequest,
    private val commandLine: GeneralCommandLine,
    private val processConsumer: ((process: GOSProcessHandler) -> Unit)? = null
) : OSProcessHandler(commandLine) {

    init {
        addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                super.onTextAvailable(event, outputType)
                val text = StringUtil.notNullize(event.text)
                if (Registry.`is`("gmaven.server.debug")) {
                    println(text)
                }
                if (request.listener != null) {
                    request.listener.onTaskOutput(request.taskId, text, true)
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