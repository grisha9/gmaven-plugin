package ru.rzn.gmyasoedov.gmaven;

import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
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
import icons.GMavenIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.chooser.MavenPomFileChooserDescriptor;
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver;
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData;
import ru.rzn.gmyasoedov.gmaven.project.profile.ProjectProfilesStateService;
import ru.rzn.gmyasoedov.gmaven.project.task.MavenTaskManager;
import ru.rzn.gmyasoedov.gmaven.settings.*;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findAll;

public final class MavenManager //manager
        implements ExternalSystemConfigurableAware,
        ExternalSystemUiAware,
        // ExternalSystemAutoImportAware,
        StartupActivity,
        ExternalSystemManager<MavenProjectSettings, MavenSettingsListener,
                MavenSettings, MavenLocalSettings, MavenExecutionSettings> {

    /*  @NotNull
      private final ExternalSystemAutoImportAware myAutoImportDelegate = new CachingExternalSystemAutoImportAware(new GradleAutoImportAware());
  */
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
            String rootProjectPath = projectSettings != null ? projectSettings.getExternalProjectPath() : projectPath;
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
                ideProjectPath = rootProjectPath;
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
            }
            result.setProjectBuildFile(projectSettings == null ? null : projectSettings.getProjectBuildFile());
            result.setSkipTests(settings.isSkipTests());
            addCurrentProfiles(project, projectPath, result);

            return result;
        };
    }

    private static void addCurrentProfiles(Project project, String rootProjectPath, MavenExecutionSettings result) {
        ExternalProjectInfo projectData = ProjectDataManager.getInstance()
                .getExternalProjectData(project, GMavenConstants.SYSTEM_ID, rootProjectPath);

        if (projectData == null || projectData.getExternalProjectStructure() == null) return;
        ProjectProfilesStateService profilesStateService = ProjectProfilesStateService.getInstance(project);
        Collection<DataNode<ModuleData>> modules = findAll(projectData.getExternalProjectStructure(), ProjectKeys.MODULE);
        for (DataNode<ModuleData> moduleNode : modules) {
            for (DataNode<ProfileData> profileDataNode : findAll(moduleNode, ProfileData.KEY)) {
                ProfileExecution profileExecution = profilesStateService.getProfileExecution(profileDataNode.getData());
                result.getExecutionWorkspace().addProfile(profileExecution);
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
        // We want to automatically refresh linked projects on gradle service directory change.
       /* MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(MavenSettings.getInstance(project).getChangesTopic(), new GradleSettingsListenerAdapter() {

            @Override
            public void onServiceDirectoryPathChange(@Nullable String oldPath, @Nullable String newPath) {
                for (MavenProjectSettings projectSettings : MavenSettings.getInstance(project).getLinkedProjectsSettings()) {
                    ExternalProjectsManager.getInstance(project).getExternalProjectsWatcher().markDirty(projectSettings.getExternalProjectPath());
                }
            }

            @Override
            public void onGradleHomeChange(@Nullable String oldPath, @Nullable String newPath, @NotNull String linkedProjectPath) {
                ExternalProjectsManager.getInstance(project).getExternalProjectsWatcher().markDirty(linkedProjectPath);
            }

            *//*@Override
            public void onGradleDistributionTypeChange(DistributionType currentValue, @NotNull String linkedProjectPath) {
                ExternalProjectsManager.getInstance(project).getExternalProjectsWatcher().markDirty(linkedProjectPath);
            }*//*

            @Override
            public void onBuildDelegationChange(boolean delegatedBuild, @NotNull String linkedProjectPath) {
                if (!updateOutputRoots(delegatedBuild, linkedProjectPath)) {
                    ExternalProjectsManager.getInstance(project).getExternalProjectsWatcher().markDirty(linkedProjectPath);
                }
            }

            private boolean updateOutputRoots(boolean delegatedBuild, @NotNull String linkedProjectPath) {
                ExternalProjectInfo projectInfo =
                        ProjectDataManager.getInstance().getExternalProjectData(project, GradleConstants.SYSTEM_ID, linkedProjectPath);
                if (projectInfo == null) return false;

                String buildNumber = projectInfo.getBuildNumber();
                if (buildNumber == null) return false;

                final DataNode<ProjectData> projectStructure = projectInfo.getExternalProjectStructure();
                if (projectStructure == null) return false;

                String title = ExternalSystemBundle.message("progress.refresh.text", projectStructure.getData().getExternalName(),
                        projectInfo.getProjectSystemId().getReadableName());
                ProgressManager.getInstance().run(new Task.Backgroundable(project, title, false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        DumbService.getInstance(project).suspendIndexingAndRun(title, () -> {
                            for (DataNode<ModuleData> moduleDataNode : findAll(projectStructure, ProjectKeys.MODULE)) {
                                moduleDataNode.getData().useExternalCompilerOutput(delegatedBuild);
                                for (DataNode<GradleSourceSetData> sourceSetDataNode : findAll(moduleDataNode, GradleSourceSetData.KEY)) {
                                    sourceSetDataNode.getData().useExternalCompilerOutput(delegatedBuild);
                                }
                            }
                            ApplicationManager.getApplication().getService(ProjectDataManager.class).importData(projectStructure, project, true);
                        });
                    }
                });
                return true;
            }
        });

        // We used to assume that gradle scripts are always named 'build.gradle' and kept path to that build.gradle file at ide settings.
        // However, it was found out that that is incorrect assumption (IDEA-109064). Now we keep paths to gradle script's directories
        // instead. However, we don't want to force old users to re-import gradle projects because of that. That's why we check gradle
        // config and re-point it from build.gradle to the parent dir if necessary.
        Map<String, String> adjustedPaths = patchLinkedProjects(project);
        if (adjustedPaths == null) {
            return;
        }

        GradleLocalSettings localSettings = GradleLocalSettings.getInstance(project);
        patchRecentTasks(adjustedPaths, localSettings);
        patchAvailableProjects(adjustedPaths, localSettings);*/
    }

   /* @Nullable
    private static Map<String, String> patchLinkedProjects(@NotNull Project project) {
        GradleSettings settings = GradleSettings.getInstance(project);
        Collection<GradleProjectSettings> correctedSettings = new ArrayList<>();
        Map<String*//* old path *//*, String*//* new path *//*> adjustedPaths = new HashMap<>();
        for (GradleProjectSettings projectSettings : settings.getLinkedProjectsSettings()) {
            String oldPath = projectSettings.getExternalProjectPath();
            if (oldPath != null && new File(oldPath).isFile() && FileUtilRt.extensionEquals(oldPath, GradleConstants.EXTENSION)) {
                try {
                    String newPath = new File(oldPath).getParentFile().getCanonicalPath();
                    projectSettings.setExternalProjectPath(newPath);
                    adjustedPaths.put(oldPath, newPath);
                } catch (IOException e) {
                    LOG.warn(String.format(
                            "Unexpected exception occurred on attempt to re-point linked gradle project path from build.gradle to its parent dir. Path: %s",
                            oldPath
                    ), e);
                }
            }
            correctedSettings.add(projectSettings);
        }
        if (adjustedPaths.isEmpty()) {
            return null;
        }

        settings.setLinkedProjectsSettings(correctedSettings);
        return adjustedPaths;
    }

    private static void patchAvailableProjects(@NotNull Map<String, String> adjustedPaths, @NotNull GradleLocalSettings localSettings) {
        Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> adjustedAvailableProjects =
                new HashMap<>();
        for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : localSettings.getAvailableProjects().entrySet()) {
            String newPath = adjustedPaths.get(entry.getKey().getPath());
            if (newPath == null) {
                adjustedAvailableProjects.put(entry.getKey(), entry.getValue());
            } else {
                adjustedAvailableProjects.put(new ExternalProjectPojo(entry.getKey().getName(), newPath), entry.getValue());
            }
        }
        localSettings.setAvailableProjects(adjustedAvailableProjects);
    }

    private static void patchRecentTasks(@NotNull Map<String, String> adjustedPaths, @NotNull GradleLocalSettings localSettings) {
        for (ExternalTaskExecutionInfo taskInfo : localSettings.getRecentTasks()) {
            ExternalSystemTaskExecutionSettings s = taskInfo.getSettings();
            String newPath = adjustedPaths.get(s.getExternalProjectPath());
            if (newPath != null) {
                s.setExternalProjectPath(newPath);
            }
        }
    }*/
}
