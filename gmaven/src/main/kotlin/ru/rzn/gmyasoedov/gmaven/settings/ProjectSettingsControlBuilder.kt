package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil
import com.intellij.openapi.externalSystem.util.PaintAwarePanel
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.SdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkComboBoxModel
import com.intellij.openapi.roots.ui.configuration.SdkListItem
import com.intellij.openapi.roots.ui.configuration.SdkListItem.*
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.util.CompositeAppearance
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties.getDistributionUrl
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.nio.file.Path
import java.util.*
import javax.swing.*

class ProjectSettingsControlBuilder(private val projectSettings: MavenProjectSettings) :
    AbstractExternalProjectSettingsControl<MavenProjectSettings>(projectSettings) {

    private var nonRecursiveCheckBox: JBCheckBox? = null
    private var useWholeProjectContextCheckBox: JBCheckBox? = null
    private var resolveModulePerSourceSetCheckBox: JBCheckBox? = null
    private var showPluginNodesCheckBox: JBCheckBox? = null
    private var useMvndForTasksCheckBox: JBCheckBox? = null
    private var threadCountField: JTextField? = null
    private var vmOptionsField: JTextField? = null
    private var argumentsField: JTextField? = null
    private var argumentsImportField: JTextField? = null
    private var snapshotUpdateComboBox: ComboBox<SnapshotUpdateComboBoxItem>? = null
    private var outPutLevelCombobox: ComboBox<OutputLevelComboBoxItem>? = null
    private var mavenHomeCombobox: ComboBox<DistributionSettingsComboBoxItem>? = null
    private var mavenCustomPathField: TextFieldWithBrowseButton? = null
    private var wrapperHintLabel: JBLabel? = null
    private var jdkComboBox: SdkComboBox? = null
    private var jdkComboBoxWrapper: JPanel? = null

    override fun validate(settings: MavenProjectSettings) = true

    override fun fillExtraControls(content: PaintAwarePanel, indentLevel: Int) {
        nonRecursiveCheckBox = JBCheckBox(GBundle.message("gmaven.settings.project.recursive"))
        content.add(nonRecursiveCheckBox!!, ExternalSystemUiUtil.getFillLineConstraints(indentLevel))

        useWholeProjectContextCheckBox = JBCheckBox(GBundle.message("gmaven.settings.project.task.context"))
        useWholeProjectContextCheckBox?.also {
            it.setToolTipText(GBundle.message("gmaven.settings.project.task.context.tooltip"))
            content.add(it, ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        }

        resolveModulePerSourceSetCheckBox = JBCheckBox(GBundle.message("gmaven.settings.project.module.per.source.set"))
        content.add(resolveModulePerSourceSetCheckBox!!, ExternalSystemUiUtil.getFillLineConstraints(indentLevel))

        useMvndForTasksCheckBox = JBCheckBox(GBundle.message("gmaven.settings.project.mvnd"))
        useMvndForTasksCheckBox!!.setToolTipText(GBundle.message("gmaven.settings.project.mvnd.tooltip"))
        content.add(useMvndForTasksCheckBox!!, ExternalSystemUiUtil.getFillLineConstraints(indentLevel))

        showPluginNodesCheckBox = JBCheckBox(GBundle.message("gmaven.settings.project.plugins"))
        showPluginNodesCheckBox!!.setToolTipText(GBundle.message("gmaven.settings.project.plugins.tooltip"))
        content.add(showPluginNodesCheckBox!!, ExternalSystemUiUtil.getFillLineConstraints(indentLevel))

        snapshotUpdateComboBox = setupSnapshotUpdateComboBox()
        val snapshotUpdateLabel = JBLabel(GBundle.message("gmaven.settings.project.snapshot.update"))
        content.add(snapshotUpdateLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(snapshotUpdateComboBox!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        snapshotUpdateLabel.setLabelFor(snapshotUpdateComboBox)

        outPutLevelCombobox = setupOutputLevelComboBox()
        val outputLevelLabel = JBLabel(GBundle.message("gmaven.settings.project.output.level"))
        content.add(outputLevelLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(outPutLevelCombobox!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        outputLevelLabel.setLabelFor(outPutLevelCombobox)


        val threadCountLabel = JBLabel(GBundle.message("gmaven.settings.project.thread.count"))
        threadCountField = JTextField()
        content.add(threadCountLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(threadCountField!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        threadCountLabel.setLabelFor(threadCountField)


        val vmOptionsLabel = JBLabel(GBundle.message("gmaven.settings.project.vm.options"))
        vmOptionsField = JTextField()
        content.add(vmOptionsLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(vmOptionsField!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        vmOptionsLabel.setLabelFor(vmOptionsField)

        val argumentsLabel = JBLabel(GBundle.message("gmaven.settings.project.arguments"))
        argumentsLabel.setToolTipText(GBundle.message("gmaven.settings.project.arguments.tooltip"))
        argumentsField = JTextField()
        argumentsField!!.setToolTipText(GBundle.message("gmaven.settings.project.arguments.tooltip"))
        content.add(argumentsLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(argumentsField!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        argumentsLabel.setLabelFor(argumentsField)

        val argumentsImportLabel = JBLabel(GBundle.message("gmaven.settings.project.arguments.import"))
        argumentsImportLabel.setToolTipText(GBundle.message("gmaven.settings.project.arguments.import.tooltip"))
        argumentsImportField = JTextField()
        argumentsImportField!!.setToolTipText(GBundle.message("gmaven.settings.project.arguments.import.tooltip"))
        content.add(argumentsImportLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(argumentsImportField!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        argumentsImportLabel.setLabelFor(argumentsImportField)


        val jdkLabel = JBLabel(GBundle.message("gmaven.settings.project.jvm"))
        jdkComboBoxWrapper = JPanel(BorderLayout())
        jdkLabel.setLabelFor(jdkComboBoxWrapper)
        content.add(jdkLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(jdkComboBoxWrapper!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))


        val mavenHomeLabel = JBLabel(GBundle.message("gmaven.settings.project.maven.home"))
        mavenHomeCombobox = setupMavenHomeComboBox()
        content.add(mavenHomeLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(mavenHomeCombobox!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        mavenHomeLabel.setLabelFor(mavenHomeCombobox)

        val hintLabel = JBLabel("", UIUtil.ComponentStyle.MINI)
        wrapperHintLabel = JBLabel("", UIUtil.ComponentStyle.MINI)
        var constraints = ExternalSystemUiUtil.getLabelConstraints(indentLevel)
        constraints.insets.top = 0
        content.add(hintLabel, constraints)
        constraints = ExternalSystemUiUtil.getLabelConstraints(0)
        constraints.insets.top = 0
        content.add(wrapperHintLabel!!, constraints)
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        hintLabel.setLabelFor(wrapperHintLabel)

        val mavenCustomPathLabel = JBLabel()
        mavenCustomPathField = TextFieldWithBrowseButton()
        content.add(mavenCustomPathLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(mavenCustomPathField!!, ExternalSystemUiUtil.getLabelConstraints(0))
        content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        mavenCustomPathLabel.setLabelFor(mavenCustomPathField)
    }

    override fun isExtraSettingModified(): Boolean {
        if (nonRecursiveCheckBox != null
            && nonRecursiveCheckBox!!.isSelected != projectSettings.nonRecursive
        ) {
            return true
        }
        if (useWholeProjectContextCheckBox != null
            && useWholeProjectContextCheckBox!!.isSelected != projectSettings.useWholeProjectContext
        ) {
            return true
        }
        if (resolveModulePerSourceSetCheckBox != null
            && resolveModulePerSourceSetCheckBox!!.isSelected != projectSettings.resolveModulePerSourceSet
        ) {
            return true
        }
        if (showPluginNodesCheckBox != null
            && showPluginNodesCheckBox!!.isSelected != projectSettings.showPluginNodes
        ) {
            return true
        }
        if (useMvndForTasksCheckBox != null
            && useMvndForTasksCheckBox!!.isSelected != projectSettings.useMvndForTasks
        ) {
            return true
        }
        if (threadCountField != null && threadCountField!!.getText() != (projectSettings.threadCount ?: "")) {
            return true
        }
        if (vmOptionsField != null && vmOptionsField!!.getText() != (projectSettings.vmOptions ?: "")) {
            return true
        }
        if (argumentsField != null && argumentsField!!.getText() != (projectSettings.arguments ?: "")) {
            return true
        }
        if (argumentsImportField != null
            && argumentsImportField!!.getText() != (projectSettings.argumentsImport ?: "")
        ) {
            return true
        }
        if (snapshotUpdateComboBox != null
            && snapshotUpdateComboBox!!.item.value != projectSettings.snapshotUpdateType
        ) {
            return true
        }
        if (outPutLevelCombobox != null
            && outPutLevelCombobox!!.item.value != projectSettings.outputLevel
        ) {
            return true
        }
        if (jdkComboBox != null
            && getJdkName(jdkComboBox!!.getSelectedItem()) != projectSettings.jdkName
        ) {
            return true
        }
        if (mavenHomeCombobox != null && mavenCustomPathField != null && mavenHomeCombobox!!.item != null) {
            val distributionSettings = mavenHomeCombobox!!.item.value
            val current = projectSettings.distributionSettings
            if (distributionSettings.type == DistributionType.CUSTOM && current.type == DistributionType.CUSTOM && Path.of(
                    mavenCustomPathField!!.getText()
                ) != current.path
            ) {
                return true
            } else if (distributionSettings.type != current.type) {
                return true
            }
        }
        return false
    }

    override fun resetExtraSettings(isDefaultModuleCreation: Boolean) {
        nonRecursiveCheckBox?.also { it.setSelected(projectSettings.nonRecursive) }
        useWholeProjectContextCheckBox?.also { it.setSelected(projectSettings.useWholeProjectContext) }
        resolveModulePerSourceSetCheckBox?.also { it.setSelected(projectSettings.resolveModulePerSourceSet) }
        showPluginNodesCheckBox?.also { it.setSelected(projectSettings.showPluginNodes) }
        useMvndForTasksCheckBox?.also { it.setSelected(projectSettings.useMvndForTasks) }
        threadCountField?.also { it.text = projectSettings.threadCount }
        vmOptionsField?.also { it.text = projectSettings.vmOptions }
        argumentsField?.also { it.text = projectSettings.arguments }
        argumentsImportField?.also { it.text = projectSettings.argumentsImport }
        snapshotUpdateComboBox?.also { it.setItem(SnapshotUpdateComboBoxItem(projectSettings.snapshotUpdateType)) }
        outPutLevelCombobox?.also { it.setItem(OutputLevelComboBoxItem(projectSettings.outputLevel)) }

        project ?: return
        mavenCustomPathField?.also {
            it.addBrowseFolderListener(
                GBundle.message("gmaven.settings.project.maven.custom.path.title"), null, project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
            )
        }
        mavenHomeCombobox?.also {
            it.removeAllItems()
            it.addItem(DistributionSettingsComboBoxItem(DistributionSettings.getBundled()))
            val distributionUrl = getDistributionUrl(project!!, projectSettings.externalProjectPath)
            if (distributionUrl.isNotEmpty()) {
                it.addItem(DistributionSettingsComboBoxItem(DistributionSettings.getWrapper(distributionUrl)))
            }
            val current: DistributionSettings = projectSettings.distributionSettings
            val mavenHome = MavenUtils.resolveMavenHome()
            if (mavenHome != null) {
                it.addItem(DistributionSettingsComboBoxItem(DistributionSettings.getLocal(mavenHome.toPath())))
            }
            val customDistribution = if (current.type == DistributionType.CUSTOM) current
            else DistributionSettings(DistributionType.CUSTOM, null, null)
            it.addItem(DistributionSettingsComboBoxItem(customDistribution))
            it.setItem(DistributionSettingsComboBoxItem(current))
            setupMavenHomeHintAndCustom(current)
        }
        jdkComboBoxWrapper?.also {
            val projectSdk: Sdk? = null
            val structureConfigurable = ProjectStructureConfigurable.getInstance(project!!)
            val sdksModel = structureConfigurable.projectJdksModel
            setupProjectSdksModel(sdksModel, project!!, projectSdk)
            recreateJdkComboBox(project!!, sdksModel, it)
            setSelectedJdk(jdkComboBox, projectSettings.jdkName)
        }
        setPreferredSize()
    }

    override fun applyExtraSettings(settings: MavenProjectSettings) {
        nonRecursiveCheckBox?.run { settings.nonRecursive = this.isSelected }
        useWholeProjectContextCheckBox?.run { settings.useWholeProjectContext = this.isSelected }
        resolveModulePerSourceSetCheckBox?.run { settings.resolveModulePerSourceSet = this.isSelected }
        showPluginNodesCheckBox?.run { settings.showPluginNodes = this.isSelected }
        useMvndForTasksCheckBox?.run { settings.useMvndForTasks = this.isSelected }
        threadCountField?.run { settings.threadCount = this.getText() }
        vmOptionsField?.run { settings.vmOptions = this.getText() }
        argumentsField?.run { settings.arguments = this.getText() }
        argumentsImportField?.run { settings.argumentsImport = this.getText() }
        snapshotUpdateComboBox?.item?.run { settings.snapshotUpdateType = this.value }
        outPutLevelCombobox?.item?.run { settings.outputLevel = this.value }
        jdkComboBox?.selectedItem?.run { settings.jdkName = getJdkName(this) }

        if (mavenHomeCombobox != null && mavenCustomPathField != null && mavenHomeCombobox!!.item != null) {
            var distributionSettings = mavenHomeCombobox!!.item.value
            if (distributionSettings.type == DistributionType.CUSTOM) {
                distributionSettings =
                    DistributionSettings(DistributionType.CUSTOM, Path.of(mavenCustomPathField!!.getText()), null)
            }
            settings.distributionSettings = distributionSettings
        }
    }

    private fun setPreferredSize() {
        val components: List<JComponent?> = listOf(
            vmOptionsField, argumentsField, argumentsImportField, threadCountField, mavenCustomPathField,
            outPutLevelCombobox, mavenHomeCombobox, jdkComboBox, snapshotUpdateComboBox
        )
        val maxWidthComponent = components.asSequence()
            .filterNotNull()
            .sortedByDescending { it.getPreferredSize().width }
            .firstOrNull() ?: return
        components
            .filterNotNull()
            .forEach { it.preferredSize = maxWidthComponent.getPreferredSize() }
    }

    private fun recreateJdkComboBox(project: Project, sdksModel: ProjectSdksModel, jdkComboBoxWrapper: JPanel) {
        jdkComboBox?.let { jdkComboBoxWrapper.remove(it) }
        jdkComboBox = SdkComboBox(SdkComboBoxModel.createJdkComboBoxModel(project, sdksModel))
        jdkComboBoxWrapper.add(jdkComboBox!!, BorderLayout.CENTER)
    }

    private fun setupOutputLevelComboBox(): ComboBox<OutputLevelComboBoxItem> {
        val values = OutputLevelType.values()
            .map { OutputLevelComboBoxItem(it) }
            .toTypedArray()
        val combobox = ComboBox(values)
        combobox.setRenderer(MyItemCellRenderer())
        return combobox
    }

    private fun setupSnapshotUpdateComboBox(): ComboBox<SnapshotUpdateComboBoxItem> {
        val values = SnapshotUpdateType.values()
            .map { SnapshotUpdateComboBoxItem(it) }
            .toTypedArray()
        val combobox = ComboBox(values)
        combobox.setRenderer(MyItemCellRenderer())
        return combobox
    }

    private fun setupMavenHomeComboBox(): ComboBox<DistributionSettingsComboBoxItem> {
        val values = listOf(DistributionSettings.getBundled())
            .map { DistributionSettingsComboBoxItem(it) }
            .toTypedArray()
        val combobox = ComboBox(values)
        combobox.setRenderer(MyItemCellRenderer())
        combobox.addItemListener { e: ItemEvent ->
            val item = e.item as DistributionSettingsComboBoxItem
            setupMavenHomeHintAndCustom(item.value)
        }
        combobox.setSelectedItem(null)
        return combobox
    }

    private fun setupMavenHomeHintAndCustom(value: DistributionSettings?) {
        value ?: return
        mavenCustomPathField?.also {
            it.isVisible = value.type == DistributionType.CUSTOM
            if (value.type == DistributionType.CUSTOM && value.path != null) {
                it.text = value.path.toString()
            } else {
                it.setText(null)
            }
        }
        wrapperHintLabel?.also {
            it.isVisible = value.type != DistributionType.CUSTOM
            if (value.type == DistributionType.MVN) {
                it.setText(value.path.toString())
            } else {
                it.setText(value.url)
            }
        }
    }

    private fun setSelectedJdk(jdkComboBox: SdkComboBox?, jdkName: String?) {
        if (jdkComboBox == null) return
        if (jdkName == ExternalSystemJdkUtil.USE_PROJECT_JDK) {
            jdkComboBox.setSelectedItem(jdkComboBox.showProjectSdkItem())
        } else if (jdkName == ExternalSystemJdkUtil.USE_JAVA_HOME) {
            jdkComboBox.setSelectedItem(jdkComboBox.showProjectSdkItem())
        } else if (jdkName == null) {
            jdkComboBox.setSelectedItem(jdkComboBox.showNoneSdkItem())
        } else {
            jdkComboBox.setSelectedSdk(jdkName)
        }
    }

    private fun getJdkName(item: SdkListItem?): String? {
        return when (item) {
            is ProjectSdkItem -> ExternalSystemJdkUtil.USE_PROJECT_JDK
            is SdkItem -> item.sdk.name
            is InvalidSdkItem -> item.sdkName
            is SdkReferenceItem -> getJdkReferenceName(item)
            else -> null
        }
    }

    private fun getJdkReferenceName(item: SdkReferenceItem): String {
        return if (ExternalSystemJdkUtil.JAVA_HOME == item.name) {
            ExternalSystemJdkUtil.JAVA_HOME
        } else {
            item.name
        }
    }

    private fun setupProjectSdksModel(sdksModel: ProjectSdksModel, project: Project, sdk: Sdk?) {
        var projectSdk = sdk
        sdksModel.reset(project)
        deduplicateSdkNames(sdksModel)
        if (projectSdk == null) {
            projectSdk = sdksModel.getProjectSdk()
            projectSdk = sdksModel.findSdk(projectSdk)
        }
        if (projectSdk != null) {
            projectSdk = ExternalSystemJdkUtil.resolveDependentJdk(projectSdk)
            projectSdk = sdksModel.findSdk(projectSdk.name)
        }
        sdksModel.setProjectSdk(projectSdk)
    }

    private fun deduplicateSdkNames(projectSdksModel: ProjectSdksModel) {
        val processedNames: MutableSet<String> = HashSet()
        val editableSdks: Collection<Sdk> = projectSdksModel.projectSdks.values
        for (sdk in editableSdks) {
            if (processedNames.contains(sdk.name)) {
                val sdkModificator = sdk.sdkModificator
                val name = SdkConfigurationUtil.createUniqueSdkName(sdk.name, editableSdks)
                sdkModificator.name = name
                sdkModificator.commitChanges()
            }
            processedNames.add(sdk.name)
        }
    }
}

private class OutputLevelComboBoxItem(value: OutputLevelType) : MyItem<OutputLevelType>(value) {
    override val text: String get() = StringUtil.capitalize(value.name.lowercase(Locale.getDefault()))
    override val comment: String? get() = null
}

private class SnapshotUpdateComboBoxItem(value: SnapshotUpdateType) : MyItem<SnapshotUpdateType>(value) {
    override val text: String get() = StringUtil.capitalize(value.name.lowercase(Locale.getDefault()))
    override val comment: String? get() = null
}

private class DistributionSettingsComboBoxItem(value: DistributionSettings) : MyItem<DistributionSettings>(value) {

    override val text: String get() = getText(value)
    override val comment: String? get() = null

    private fun getText(settings: DistributionSettings): String {
        var text = StringUtil.capitalize(settings.type.name.lowercase(Locale.getDefault()))
        if (settings.type == DistributionType.BUNDLED) {
            text += "(maven version: " + GMavenConstants.BUNDLED_MAVEN_VERSION + ")"
        }
        if (settings.type == DistributionType.MVN) {
            text = "Maven home(mvn)"
            try {
                val mavenVersion = MavenUtils.getMavenVersion(settings.path)
                if (mavenVersion != null) {
                    text += ": $mavenVersion"
                }
            } catch (e: Exception) {
                MavenLog.LOG.error(e)
            }
        }
        if (settings.type == DistributionType.WRAPPER) {
            text = "Use Maven wrapper"
        }
        return text
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DistributionSettingsComboBoxItem) return false
        return value.type == other.value.type
    }

    override fun hashCode(): Int {
        return Objects.hash(value.type)
    }
}

private abstract class MyItem<T>(val value: T) {

    abstract val text: @NlsContexts.ListItem String?
    abstract val comment: @NlsContexts.ListItem String?

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MyItem<*>) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return Objects.hash(value)
    }
}

private class MyItemCellRenderer<T> : ColoredListCellRenderer<MyItem<T>?>() {
    override fun customizeCellRenderer(
        list: JList<out MyItem<T>?>,
        value: MyItem<T>?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        if (value == null) return
        val ending = CompositeAppearance().ending
        ending.addText(value.text, getTextAttributes(selected))
        if (value.comment != null) {
            val commentAttributes = getCommentAttributes(selected)
            ending.addComment(value.comment, commentAttributes)
        }
        ending.appearance.customize(this)
    }

    companion object {
        private fun getTextAttributes(selected: Boolean): SimpleTextAttributes {
            return if (selected && !(SystemInfoRt.isWindows && UIManager.getLookAndFeel().name.contains("Windows"))) SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES else SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES
        }

        private fun getCommentAttributes(selected: Boolean): SimpleTextAttributes {
            return if (SystemInfo.isMac && selected) SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN,
                JBColor.WHITE
            ) else SimpleTextAttributes.GRAY_ATTRIBUTES
        }
    }
}

enum class OutputLevelType { DEFAULT, QUITE, DEBUG }

enum class SnapshotUpdateType { DEFAULT, NEVER, FORCE }

