package ru.rzn.gmyasoedov.gmaven.settings

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil
import com.intellij.openapi.externalSystem.util.PaintAwarePanel
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.SdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkComboBoxModel
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.emptyText
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.concurrency.AppExecutorUtil
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle.message
import ru.rzn.gmyasoedov.gmaven.project.wrapper.MvnDotProperties.getDistributionUrl
import ru.rzn.gmyasoedov.gmaven.settings.ProjectSettingsControlBuilder.*
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.util.*
import java.util.concurrent.Callable

class ProjectSettingsControl(private val project: Project, private val currentSettings: MavenProjectSettings) :
    AbstractExternalProjectSettingsControl<MavenProjectSettings>(project, currentSettings) {

    private lateinit var mainPanel: DialogPanel
    private var jdkComboBox: SdkComboBox? = null

    private val propertyGraph = PropertyGraph()
    private val nonRecursiveBind = propertyGraph.property(false)
    private val useWholeProjectContextBind = propertyGraph.property(false)
    private val resolveModulePerSourceSetBind = propertyGraph.property(false)
    private val showPluginNodesBind = propertyGraph.property(false)
    private val updateSnapshotsModel = CollectionComboBoxModel(SnapshotUpdateType.values().toList())
    private val outputLevelModel = CollectionComboBoxModel(OutputLevelType.values().toList())
    private val distributionTypeModel = CollectionComboBoxModel(mutableListOf<String>())
    private val threadCountBind = propertyGraph.property("")
    private val vmOptionsBind = propertyGraph.property("")
    private val additionalArgsBind = propertyGraph.property("")
    private val importArgsBind = propertyGraph.property("")
    private val mavenPathBind = propertyGraph.property("")
    private val mavenCustomPathBind = propertyGraph.property("")

    private val distributionTypeMap = mutableMapOf<String, DistributionSettings>()
    private val mavenPathVisible = AtomicBooleanProperty(false)


    override fun validate(settings: MavenProjectSettings) = true

    override fun fillExtraControls(content: PaintAwarePanel, indentLevel: Int) {
        mainPanel = panel {
            group {
                row {
                    checkBox(message("gmaven.settings.project.recursive"))
                        .align(AlignX.FILL)
                        .bindSelected(nonRecursiveBind)
                        .resizableColumn()
                }

                row {
                    checkBox(message("gmaven.settings.project.task.context"))
                        .align(AlignX.FILL)
                        .applyToComponent { toolTipText = message("gmaven.settings.project.task.context.tooltip") }
                        .bindSelected(useWholeProjectContextBind)
                        .resizableColumn()
                }

                row {
                    checkBox(message("gmaven.settings.project.module.per.source.set"))
                        .align(AlignX.FILL)
                        .bindSelected(resolveModulePerSourceSetBind)
                        .resizableColumn()
                }

                row {
                    checkBox(message("gmaven.settings.project.plugins"))
                        .align(AlignX.FILL)
                        .applyToComponent { toolTipText = message("gmaven.settings.project.plugins.tooltip") }
                        .bindSelected(showPluginNodesBind)
                        .resizableColumn()
                }

                row(message("gmaven.settings.project.snapshot.update")) {
                    comboBox(updateSnapshotsModel)
                        .align(AlignX.FILL)
                        //.validationOnApply { validateModel() }
                        .resizableColumn()
                }

                row(message("gmaven.settings.project.output.level")) {
                    comboBox(outputLevelModel)
                        .align(AlignX.FILL)
                        .resizableColumn()
                }

                row(message("gmaven.settings.project.thread.count")) {
                    textField()
                        .align(AlignX.FILL)
                        .bindText(threadCountBind)
                        .resizableColumn()
                }

                row(message("gmaven.settings.project.vm.options")) {
                    textField()
                        .align(AlignX.FILL)
                        .bindText(vmOptionsBind)
                        .resizableColumn()
                }

                row(message("gmaven.settings.project.arguments")) {
                    textField()
                        .align(AlignX.FILL)
                        .applyToComponent {
                            toolTipText = message("gmaven.settings.project.arguments.tooltip")
                            emptyText.text = "Example: -X -s /home/settings.xml -gs \"/home/global_settings.xml\""
                        }
                        .bindText(additionalArgsBind)
                        .resizableColumn()
                }

                row(message("gmaven.settings.project.arguments.import")) {
                    textField()
                        .align(AlignX.FILL)
                        .applyToComponent {
                            toolTipText = message("gmaven.settings.project.arguments.import.tooltip")
                            emptyText.text = "Example: -q verify"
                        }
                        .bindText(importArgsBind)
                        .resizableColumn()
                }

                row(message("gmaven.settings.project.jvm")) {
                    gSdkComboBox(project)
                        .align(AlignX.FILL)
                        .applyToComponent { jdkComboBox = this }
                }

                row(message("gmaven.settings.project.maven.home")) {
                    comboBox(distributionTypeModel)
                        .align(AlignX.FILL)
                        .applyToComponent { addActionListener { changeMavenTypeListener() } }
                        .resizableColumn()
                }

                row {
                    textField()
                        .align(AlignX.FILL)
                        .bindText(mavenPathBind)
                        .resizableColumn()
                        .enabled(false)
                }.visibleIf(mavenPathVisible)

                row {
                    textFieldWithBrowseButton(
                        message("gmaven.settings.project.maven.dialog.title"), project, createSingleFolderDescriptor()
                    )
                        .bindText(mavenCustomPathBind)
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .applyToComponent {
                            toolTipText = message("gmaven.settings.project.maven.dialog.title")
                            emptyText.text = toolTipText
                        }
                }.visibleIf(mavenPathVisible.not())
            }
        }
        content.add(mainPanel, ExternalSystemUiUtil.getFillLineConstraints(0).insets(0, 0, 0, 0))
    }

    private fun changeMavenTypeListener() {
        val distributionSettings = distributionTypeMap[distributionTypeModel.selected] ?: return
        val distributionType = distributionSettings.type
        mavenPathVisible.set(
            distributionType == DistributionType.MVN
                    || distributionType == DistributionType.BUNDLED || distributionType == DistributionType.WRAPPER
        )
        if (mavenPathVisible.get()) {
            if (distributionType == DistributionType.MVN) {
                mavenPathBind.set(distributionSettings.path!!.toString())
            } else {
                mavenPathBind.set(distributionSettings.url)
            }
        } else {
            mavenPathBind.set("")
        }
    }

    override fun resetExtraSettings(isDefaultModuleCreation: Boolean) {
        setSelectedJdk(jdkComboBox, currentSettings.jdkName)
        if (distributionTypeMap.isNotEmpty()) {
            setSelectedMavenType(currentSettings)
        } else {
            ReadAction.nonBlocking(Callable { getMavenDistributionsInfo(project, currentSettings) })
                .finishOnUiThread(ModalityState.current(), {
                    setMavenTypeModelData(it).let { setSelectedMavenType(currentSettings) }
                })
                .submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    override fun isExtraSettingModified(): Boolean {
        return false
    }

    override fun applyExtraSettings(settings: MavenProjectSettings) {

    }

    private fun setSelectedJdk(jdkComboBox: SdkComboBox?, jdkName: String?) {
        jdkComboBox ?: return
        if (jdkName == ExternalSystemJdkUtil.USE_PROJECT_JDK) {
            jdkComboBox.selectedItem = jdkComboBox.showProjectSdkItem()
        } else if (jdkName == ExternalSystemJdkUtil.USE_JAVA_HOME) {
            jdkComboBox.selectedItem = jdkComboBox.showProjectSdkItem()
        } else if (jdkName == null) {
            jdkComboBox.selectedItem = jdkComboBox.showNoneSdkItem()
        } else {
            jdkComboBox.setSelectedSdk(jdkName)
        }
    }

    private fun setMavenTypeModelData(distributionInfo: List<DistributionInfo>) {
        distributionTypeModel.removeAll()
        distributionTypeMap.clear()
        for (each in distributionInfo) {
            distributionTypeModel.add(each.description)
            distributionTypeMap[each.description] = each.distributionSettings
        }
        distributionTypeModel.update()
    }

    private fun setSelectedMavenType(currentSettings: MavenProjectSettings) {
        for (entry in distributionTypeMap) {
            if (entry.value.type == currentSettings.distributionSettings.type) {
                distributionTypeModel.selectedItem = entry.key
                return
            }
        }
    }
}

private fun Row.gSdkComboBox(
    project: Project
): Cell<SdkComboBox> {
    val structureConfigurable = ProjectStructureConfigurable.getInstance(project)
    val sdksModel = structureConfigurable.projectJdksModel

    setupProjectSdksModel(sdksModel, project)
    val sdkComboBox = SdkComboBox(SdkComboBoxModel.createJdkComboBoxModel(project, sdksModel))

    return cell(sdkComboBox)
}

private fun setupProjectSdksModel(sdksModel: ProjectSdksModel, project: Project) {
    sdksModel.reset(project)
    deduplicateSdkNames(sdksModel)

    var projectSdk = sdksModel.projectSdk
    projectSdk = sdksModel.findSdk(projectSdk)

    if (projectSdk != null) {
        projectSdk = ExternalSystemJdkUtil.resolveDependentJdk(projectSdk)
        projectSdk = sdksModel.findSdk(projectSdk.name)
    }
    sdksModel.projectSdk = projectSdk
}

private fun getMavenDistributionsInfo(project: Project, projectSettings: MavenProjectSettings): List<DistributionInfo> {
    val result = mutableListOf(DistributionSettings.getBundled())
    val distributionUrl = getDistributionUrl(project, projectSettings.externalProjectPath)
    if (distributionUrl.isNotEmpty()) {
        result.add(DistributionSettings.getWrapper(distributionUrl))
    }
    val mavenHome = MavenUtils.resolveMavenHome()
    if (mavenHome != null) {
        result.add(DistributionSettings.getLocal(mavenHome.toPath()))
    }
    result.add(DistributionSettings(DistributionType.CUSTOM, null, null))
    result.add(DistributionSettings(DistributionType.MVND, null, null))
    return result.map { DistributionInfo(getDistributionUIText(it), it) }
}

private fun getDistributionUIText(each: DistributionSettings): String {
    return if (each.type == DistributionType.BUNDLED) {
        StringUtil.capitalize(each.type.name.lowercase(Locale.getDefault())) +
                "(maven version: " + GMavenConstants.BUNDLED_MAVEN_VERSION + ")"
    } else if (each.type == DistributionType.WRAPPER) {
        "Use Maven Wrapper"
    } else if (each.type == DistributionType.MVN) {
        getMvnText(each)
    } else if (each.type == DistributionType.MVND) {
        "Maven Daemon"
    } else {
        "Custom Maven"
    }
}

private fun getMvnText(each: DistributionSettings): String {
    var text = "Maven home(mvn)"
    try {
        val mavenVersion = MavenUtils.getMavenVersion(each.path.toFile())
        if (mavenVersion != null) {
            text += ": $mavenVersion"
        }
    } catch (e: Exception) {
        MavenLog.LOG.warn(e)
    }
    return text
}

private data class DistributionInfo(val description: String, val distributionSettings: DistributionSettings)
