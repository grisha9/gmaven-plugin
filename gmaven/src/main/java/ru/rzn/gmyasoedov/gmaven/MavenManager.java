package ru.rzn.gmyasoedov.gmaven;

import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.ui.DefaultExternalSystemUiAware;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.ExecutionSearchScopes;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.execution.ParametersListUtil;
import icons.GMavenIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.chooser.MavenPomFileChooserDescriptor;
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData;
import ru.rzn.gmyasoedov.gmaven.project.profile.ProjectProfilesStateService;
import ru.rzn.gmyasoedov.gmaven.project.task.MavenTaskManager;
import ru.rzn.gmyasoedov.gmaven.settings.DistributionSettings;
import ru.rzn.gmyasoedov.gmaven.settings.GMavenConfigurable;
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings;
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionWorkspace;
import ru.rzn.gmyasoedov.gmaven.settings.MavenLocalSettings;
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings;
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings;
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettingsListener;
import ru.rzn.gmyasoedov.gmaven.settings.ProfileExecution;
import ru.rzn.gmyasoedov.gmaven.settings.ProjectExecution;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.intellij.openapi.externalSystem.model.ProjectKeys.MODULE;
import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findAll;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findAllRecursively;
import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.MODULE_PROP_BUILD_FILE;

public final class MavenManager
        implements ExternalSystemConfigurableAware,
        ExternalSystemUiAware,
        StartupActivity,
        ExternalSystemManager<MavenProjectSettings, MavenSettingsListener,
                MavenSettings, MavenLocalSettings, MavenExecutionSettings> {

    @NotNull
    @Override
    public ProjectSystemId getSystemId() {
        return GMavenConstants.SYSTEM_ID;
    }

    @NotNull
    @Override
    public Function<Project, MavenSettings> getSettingsProvider() {
        return MavenSettings::getInstance;
    }

    @NotNull
    @Override
    public Function<Project, MavenLocalSettings> getLocalSettingsProvider() {
        return MavenLocalSettings::getInstance;
    }

    @NotNull
    @Override
    public Function<Pair<Project, String>, MavenExecutionSettings> getExecutionSettingsProvider() {
        return pair -> {
            Project project = pair.first;
            String projectPath = pair.second;
            MavenSettings settings = MavenSettings.getInstance(project);
            MavenProjectSettings projectSettings = settings.getLinkedProjectSettings(projectPath);
            String rootProjectDirPath = projectSettings != null ? projectSettings.getExternalProjectPath() : projectPath;
            DistributionSettings distributionSettings = projectSettings != null
                    ? projectSettings.getDistributionSettings() : DistributionSettings.getBundled();

            MavenExecutionSettings result = new MavenExecutionSettings(distributionSettings,
                    projectSettings != null ? projectSettings.getVmOptions() : null,
                    projectSettings != null && projectSettings.getNonRecursive(),
                    settings.isOfflineMode());

            //todo - сделать как в org.jetbrains.plugins.gradle.GradleManager#configureExecutionWorkspace
            String jdkName = projectSettings != null ? projectSettings.getJdkName() : null;
            Sdk jdk = ExternalSystemJdkUtil.getJdk(project, jdkName);
            if (jdk == null) {
                jdk = ExternalSystemJdkUtil.getJdk(project, USE_PROJECT_JDK);
            }
            if (jdk != null) {
                result.setJavaHome(jdk.getHomePath());
                result.setJdkName(jdk.getName());
            }

            String ideProjectPath;
            if (project.getBasePath() == null ||
                    (project.getProjectFilePath() != null && StringUtil.endsWith(project.getProjectFilePath(), ".ipr"))) {
                ideProjectPath = rootProjectDirPath;
            } else {
                ideProjectPath = project.getBasePath() + "/.idea/modules";
            }
            result.setIdeProjectPath(ideProjectPath);
            if (projectSettings != null) {
                result.setResolveModulePerSourceSet(projectSettings.getResolveModulePerSourceSet());
                result.setUseQualifiedModuleNames(projectSettings.isUseQualifiedModuleNames());
                result.setNonRecursive(projectSettings.getNonRecursive());
                result.setUpdateSnapshots(projectSettings.getUpdateSnapshots());
                result.setThreadCount(projectSettings.getThreadCount());
                result.setOutputLevel(projectSettings.getOutputLevel());
                fillExecutionWorkSpace(project, projectSettings, projectPath, result.getExecutionWorkspace());
                if (projectSettings.getArguments() != null) {
                    result.withArguments(ParametersListUtil.parse(projectSettings.getArguments(), true, true));
                }
            }
            if (settings.isSkipTests()) {
                result.withEnvironmentVariables(Map.of("skipTests", "true"));
            }
            return result;
        };
    }

    private static void fillExecutionWorkSpace(Project project,
                                               MavenProjectSettings projectSettings,
                                               String projectPath,
                                               MavenExecutionWorkspace workspace) {
        ExternalProjectInfo projectData = ProjectDataManager.getInstance()
                .getExternalProjectData(project, GMavenConstants.SYSTEM_ID, projectSettings.getExternalProjectPath());

        if (projectData == null || projectData.getExternalProjectStructure() == null) return;
        ProjectProfilesStateService profilesStateService = ProjectProfilesStateService.getInstance(project);
        Collection<DataNode<ModuleData>> modules = findAll(projectData.getExternalProjectStructure(), MODULE);
        workspace.setProjectBuildFile(getProjectBuildFile(projectSettings, modules));
        boolean isNotRootPath = !Objects.equals(projectSettings.getExternalProjectPath(), projectPath);
        Collection<DataNode<ModuleData>> allModules = findAllRecursively(
                projectData.getExternalProjectStructure(), MODULE
        );


        if (isNotRootPath) {
            allModules.stream()
                    .filter(node -> Objects.equals(node.getData().getLinkedExternalProjectPath(), projectPath))
                    .findFirst()
                    .ifPresent(node -> {
                        ModuleData module = node.getData();
                        workspace.setSubProjectBuildFile(module.getProperty(MODULE_PROP_BUILD_FILE));
                        addedIgnoredModule(workspace, findAllRecursively(node, MODULE));
                        if (projectSettings.getUseWholeProjectContext()) {
                            workspace.addProject(new ProjectExecution(MavenUtils.toGAString(module), true));
                            workspace.setSubProjectBuildFile(null);
                        }
                    });
        } else {
            addedIgnoredModule(workspace, allModules);
        }
        for (DataNode<ModuleData> moduleNode : modules) {
            for (DataNode<ProfileData> profileDataNode : findAll(moduleNode, ProfileData.KEY)) {
                ProfileExecution profileExecution = profilesStateService.getProfileExecution(profileDataNode.getData());
                workspace.addProfile(profileExecution);
            }
        }
    }

    @Override
    public void enhanceRemoteProcessing(@NotNull SimpleJavaParameters parameters) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Class<? extends ExternalSystemProjectResolver<MavenExecutionSettings>> getProjectResolverClass() {
        return MavenProjectResolver.class;
    }

    @Override
    public Class<? extends ExternalSystemTaskManager<MavenExecutionSettings>> getTaskManagerClass() {
        return MavenTaskManager.class;
    }

    @NotNull
    @Override
    public Configurable getConfigurable(@NotNull Project project) {
        return new GMavenConfigurable(project);
    }

    @Nullable
    @Override
    public FileChooserDescriptor getExternalProjectConfigDescriptor() {
        return FileChooserDescriptorFactory.createSingleFolderDescriptor();
    }

    @Nullable
    @Override
    public Icon getProjectIcon() {
        return GMavenIcons.MavenProject;
    }

    @Nullable
    @Override
    public Icon getTaskIcon() {
        return DefaultExternalSystemUiAware.INSTANCE.getTaskIcon();
    }

    @NotNull
    @Override
    public String getProjectRepresentationName(@NotNull String targetProjectPath, @Nullable String rootProjectPath) {
        return ExternalSystemApiUtil.getProjectRepresentationName(targetProjectPath, rootProjectPath);
    }

    @NotNull
    @Override
    public String getProjectRepresentationName(@NotNull Project project,
                                               @NotNull String targetProjectPath,
                                               @Nullable String rootProjectPath) {
        return ExternalSystemApiUtil.getProjectRepresentationName(targetProjectPath, rootProjectPath);
    }

   /* @Nullable
    @Override
    public String getAffectedExternalProjectPath(@NotNull String changedFileOrDirPath, @NotNull Project project) {
        return myAutoImportDelegate.getAffectedExternalProjectPath(changedFileOrDirPath, project);
    }

    @Override
    public List<File> getAffectedExternalProjectFiles(String projectPath, @NotNull Project project) {
        return myAutoImportDelegate.getAffectedExternalProjectFiles(projectPath, project);
    }

    @Override
    public boolean isApplicable(@Nullable ProjectResolverPolicy resolverPolicy) {
        return myAutoImportDelegate.isApplicable(resolverPolicy);
    }*/

    @NotNull
    @Override
    public FileChooserDescriptor getExternalProjectDescriptor() {
        return new MavenPomFileChooserDescriptor();
    }

    @Nullable
    @Override
    public GlobalSearchScope getSearchScope(@NotNull Project project,
                                            @NotNull ExternalSystemTaskExecutionSettings taskExecutionSettings) {
        String projectPath = taskExecutionSettings.getExternalProjectPath();
        if (StringUtil.isEmpty(projectPath)) return null;

        MavenSettings settings = MavenSettings.getInstance(project);
        MavenProjectSettings projectSettings = settings.getLinkedProjectSettings(projectPath);
        if (projectSettings == null) return null;

        if (!projectSettings.getResolveModulePerSourceSet()) {
            // use default implementation which will find target module using projectPathFile
            return null;
        } else {
            List<Module> modules = JBIterable.of(ModuleManager.getInstance(project).getModules())
                    .filter(module -> StringUtil.equals(projectPath, ExternalSystemApiUtil.getExternalProjectPath(module)))
                    .toList();
            return modules.isEmpty() ? null : ExecutionSearchScopes.executionScope(modules);
        }
    }

    @Override
    public void runActivity(@NotNull final Project project) {
    }

    @Nullable
    private static String getProjectBuildFile(MavenProjectSettings projectSettings,
                                              Collection<DataNode<ModuleData>> modules) {
        return modules.stream()
                .map(DataNode::getData)
                .filter(m -> Path.of(m.getLinkedExternalProjectPath())
                        .equals(Path.of(projectSettings.getExternalProjectPath())))
                .filter(m -> m.getProperty(MODULE_PROP_BUILD_FILE) != null)
                .map(m -> m.getProperty(MODULE_PROP_BUILD_FILE))
                .findFirst()
                .orElse(null);
    }

    private static void addedIgnoredModule(MavenExecutionWorkspace workspace, Collection<DataNode<ModuleData>> allModules) {
        allModules.stream()
                .filter(DataNode::isIgnored)
                .map(node -> new ProjectExecution(MavenUtils.toGAString(node.getData()), false))
                .forEach(workspace::addProject);
    }
}
