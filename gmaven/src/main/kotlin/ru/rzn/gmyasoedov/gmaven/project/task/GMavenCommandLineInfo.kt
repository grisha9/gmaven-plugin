package ru.rzn.gmyasoedov.gmaven.project.task

import com.intellij.icons.AllIcons
import com.intellij.openapi.externalSystem.service.ui.command.line.CommandLineInfo
import com.intellij.openapi.externalSystem.service.ui.command.line.CompletionTableInfo
import com.intellij.openapi.externalSystem.service.ui.completion.TextCompletionInfo
import com.intellij.openapi.externalSystem.service.ui.project.path.WorkingDirectoryField
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import org.apache.commons.cli.Option
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import javax.swing.Icon

class GMavenCommandLineInfo(project: Project, workingDirectoryField: WorkingDirectoryField) : CommandLineInfo {
  override val settingsName: String = GBundle.message("gmaven.run.configuration.command.line.name")
  override val settingsHint: String = GBundle.message("gmaven.run.configuration.command.line.hint")

  override val dialogTitle: String = GBundle.message("gmaven.run.configuration.command.line.title")
  override val dialogTooltip: String = GBundle.message("gmaven.run.configuration.command.line.tooltip")

  override val fieldEmptyState: String = GBundle.message("gmaven.run.configuration.command.line.empty.state")

  override val tablesInfo: List<CompletionTableInfo> = listOf(
    TasksCompletionTableInfo(project, workingDirectoryField),
    ArgumentsCompletionTableInfo()
  )

  private class TasksCompletionTableInfo(
    private val project: Project,
    private val workingDirectoryField: WorkingDirectoryField
  ) : CompletionTableInfo {
    override val emptyState: String = GBundle.message("gmaven.run.configuration.command.line.tasks.empty.text")

    override val dataColumnIcon: Icon = AllIcons.General.Gear
    override val dataColumnName: String = GBundle.message("gmaven.run.configuration.command.line.task.column")

    override val descriptionColumnIcon: Icon? = null
    override val descriptionColumnName: String =
      GBundle.message("gmaven.run.configuration.command.line.description.column")

    private val completionInfoProperty = AtomicLazyProperty { calculateCompletionInfo() }

    override val completionInfo by completionInfoProperty
    override val tableCompletionInfo by completionInfoProperty

    private fun calculateCompletionInfo(): List<TextCompletionInfo> {
      return emptyList()
    }

    init {
      workingDirectoryField.whenTextChanged {
        completionInfoProperty.set(emptyList())
      }
    }
  }

  private class ArgumentsCompletionTableInfo : CompletionTableInfo {
    override val emptyState: String = GBundle.message("gmaven.run.configuration.command.line.arguments.empty.text")

    override val dataColumnIcon: Icon? = null
    override val dataColumnName: String = GBundle.message("gmaven.run.configuration.command.line.argument.column")

    override val descriptionColumnIcon: Icon? = null
    override val descriptionColumnName: String =
      GBundle.message("gmaven.run.configuration.command.line.description.column")

    private val options: List<Option> = emptyList()
    /*CommandLineOptionsProvider.getSupportedOptions().options
      .filterIsInstance<Option>()*/

    override val completionInfo: List<TextCompletionInfo> by lazy {
      options
        .filter { it.opt != null }
        .map { TextCompletionInfo("-" + it.opt, it.description) }
        .sortedBy { it.text } +
              options
                .filter { it.longOpt != null }
                .map { TextCompletionInfo("--" + it.longOpt, it.description) }
                .sortedBy { it.text }
    }

    override val tableCompletionInfo: List<TextCompletionInfo> by lazy {
      completionInfo.filter { it.text.startsWith("--") }
    }
  }
}