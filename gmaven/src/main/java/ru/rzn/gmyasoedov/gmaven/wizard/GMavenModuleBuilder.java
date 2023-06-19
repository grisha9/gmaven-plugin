package ru.rzn.gmyasoedov.gmaven.wizard;

import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.*;
import com.intellij.openapi.module.impl.ModuleEx;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle;
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings;
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static icons.OpenapiIcons.RepositoryLibraryLogo;
import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.GMAVEN;
import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID;

public class GMavenModuleBuilder extends ModuleBuilder implements SourcePathsBuilder {
    private MavenProject myAggregatorProject;
    private MavenProject myParentProject;

    private boolean myInheritGroupId;
    private boolean myInheritVersion;
    private MavenId myProjectId;

    private VirtualFile pomFile;

    @Override
    protected void setupModule(Module module) throws ConfigurationException {
        super.setupModule(module);
        ExternalSystemModulePropertyManager modulePropertyManager = ExternalSystemModulePropertyManager.getInstance(module);
        modulePropertyManager.setExternalId(GMavenConstants.SYSTEM_ID);
       // modulePropertyManager.setMavenized(true);
        Project project = module.getProject();
        String parentModuleName = (String) Optional.ofNullable(myParentProject)
                .map(MavenProject::getProperties)
                .map(p -> p.get("moduleInternalName"))
                .filter(p -> p instanceof String)
                .orElse(null);
        try {
            if (parentModuleName != null && module instanceof ModuleEx) {
              //  String newName = parentModuleName + "." + getName();
               // ((ModuleEx) module).rename(newName, true);
            }
        } catch (Exception e) {
            System.out.println();
        }
        MavenUtils.runWhenInitialized(project, (DumbAwareRunnable)
                () -> setupBuildScriptAndExternalProject(project, pomFile)
        );
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel rootModel) throws ConfigurationException {
        final VirtualFile root = createAndGetContentEntry();
        pomFile = createExternalProjectConfigFile(root.toNioPath());
        rootModel.addContentEntry(root);

        if (myJdk != null) {
            rootModel.setSdk(myJdk);
        } else {
            rootModel.inheritSdk();
        }
    }

    private void setupBuildScriptAndExternalProject(Project project, VirtualFile pomFile) {
        MavenProjectSettings projectSettings = getMavenProjectSettings(pomFile, project);
        new GMavenModuleBuilderHelper(
                myProjectId, myParentProject,
                myInheritGroupId, myInheritVersion, pomFile
        ).setupBuildScript(project, false);
        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized(() -> {
            String projectPath = projectSettings.getExternalProjectPath();
            ExternalSystemUtil.refreshProject(projectPath, new ImportSpecBuilder(project, SYSTEM_ID)
                    .callback(new ExternalProjectRefreshCallback() {
                        @Override
                        public void onSuccess(@NotNull ExternalSystemTaskId externalTaskId,
                                              @Nullable DataNode<ProjectData> externalProject) {
                            if (externalProject != null) {
                                ProjectDataManager.getInstance().importData(externalProject, project, false);
                                if (myAggregatorProject == null) {
                                    WizardUtilsKt.updateMavenSettings(project, projectPath);
                                }
                            }
                        }
                    }));
        });
    }

    @NotNull
    private MavenProjectSettings getMavenProjectSettings(VirtualFile pomFile,  Project project) {
        MavenSettings settings = MavenSettings.getInstance(project);
        String canonicalPath = pomFile.getParent().getPath();
        var projectSettings = settings.getLinkedProjectSettings(canonicalPath);
        if (projectSettings == null) {
            projectSettings = WizardUtilsKt.createMavenProjectSettings(pomFile, project);
            if (myJdk != null) {
                projectSettings.setJdkName(myJdk.getName());
            }
            settings.linkProject(projectSettings);
        }
        return projectSettings;
    }

    @Override
    public @Nullable Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
        setMavenModuleFilePath(project, getName());
        return super.commitModule(project, model);
    }

    private void setMavenModuleFilePath(@NotNull Project project, @NotNull String moduleName) {
        if (myParentProject == null) return;
        String parentModuleName = (String) Optional.of(myParentProject)
                .map(MavenProject::getProperties)
                .map(p -> p.get("moduleInternalName"))
                .filter(p -> p instanceof String)
                .orElse(null);
        if (StringUtil.isNotEmpty(parentModuleName)) {
            String moduleFilePath = project.getBasePath() + File.separator + parentModuleName + "." + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION;
            setModuleFilePath(moduleFilePath);
        }
    }

    //todo IDEA-85478 Maven compiler plugin testSource and testTarget  MY CHANGES!!!
   /* @Override
    public @Nullable Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
        setMavenModuleFilePath(project, getName());
        return super.commitModule(project, model);
    }

    private void setMavenModuleFilePath(@NotNull Project project, @NotNull String moduleName) {
        if (myParentProject == null) return;

        String parentModuleName = MavenImportUtil.getModuleName(myParentProject, project);
        if (StringUtil.isNotEmpty(parentModuleName)) {
            String moduleFilePath =
                    project.getBasePath() + File.separator + parentModuleName + "." + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION;
            setModuleFilePath(moduleFilePath);
        }
    }
*/
    @Override
    public @NonNls String getBuilderId() {
        return getClass().getName();
    }

    @Override
    public String getPresentableName() {
        return GMAVEN;
    }

    @Override
    public String getParentGroup() {
        return JavaModuleType.JAVA_GROUP;
    }

    @Override
    public int getWeight() {
        return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT;
    }

    @Override
    public String getDescription() {
        return GBundle.message("maven.builder.module.builder.description");
    }

    @Override
    public Icon getNodeIcon() {
        return RepositoryLibraryLogo;
    }

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Override
    public boolean isSuitableSdkType(SdkTypeId sdk) {
        return sdk == JavaSdk.getInstance();
    }

    private VirtualFile createAndGetContentEntry() {
        String path = FileUtil.toSystemIndependentName(getContentEntryPath());
        new File(path).mkdirs();
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    }

    @Override
    public List<Pair<String, String>> getSourcePaths() {
        return Collections.emptyList();
    }

    @Override
    public void setSourcePaths(List<Pair<String, String>> sourcePaths) {
    }

    @Override
    public void addSourcePath(Pair<String, String> sourcePathInfo) {
    }

    public void setAggregatorProject(MavenProject project) {
        myAggregatorProject = project;
    }

    public MavenProject getAggregatorProject() {
        return myAggregatorProject;
    }

    public void setParentProject(MavenProject project) {
        myParentProject = project;
    }

    public MavenProject getParentProject() {
        return myParentProject;
    }

    public void setInheritedOptions(boolean groupId, boolean version) {
        myInheritGroupId = groupId;
        myInheritVersion = version;
    }

    public boolean isInheritGroupId() {
        return myInheritGroupId;
    }

    public void setInheritGroupId(boolean inheritGroupId) {
        myInheritGroupId = inheritGroupId;
    }

    public boolean isInheritVersion() {
        return myInheritVersion;
    }

    public void setInheritVersion(boolean inheritVersion) {
        myInheritVersion = inheritVersion;
    }

    public void setProjectId(MavenId id) {
        myProjectId = id;
    }

    public MavenId getProjectId() {
        return myProjectId;
    }

    @Override
    public String getGroupName() {
        return GMAVEN;
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        final ModuleNameLocationSettings nameLocationSettings = settingsStep.getModuleNameLocationSettings();
        if (nameLocationSettings != null && myProjectId != null) {
            nameLocationSettings.setModuleName(StringUtil.sanitizeJavaIdentifier(myProjectId.getArtifactId()));
            if (myAggregatorProject != null) {
                Path path = Path.of(
                        myAggregatorProject.getParentFile().getAbsolutePath(),
                        myProjectId.getArtifactId());
                nameLocationSettings.setModuleContentRoot(path.toString());
            }
        }
        return super.modifySettingsStep(settingsStep);
    }

    @Nullable
    @Override
    public Project createProject(String name, String path) {
        Project project = super.createProject(name, path);
        if (project != null) {
            ExternalProjectsManagerImpl.setupCreatedProject(project);
        }
        return project;
    }

    private static @NotNull VirtualFile createExternalProjectConfigFile(@NotNull Path parent)
            throws ConfigurationException {
        Path file = parent.resolve(GMavenConstants.POM_XML);
        try {
            Files.deleteIfExists(file);
            try {
                Files.createFile(file);
            } catch (FileAlreadyExistsException ignore) {
            }

            VirtualFile virtualFile = VfsUtil.findFile(file, true);
            if (virtualFile == null) {
                throw new ConfigurationException("Can not create configuration file " + file);
            }
            if (virtualFile.isDirectory()) {
                throw new ConfigurationException("Configuration file is directory " + file);
            }
            VfsUtil.markDirtyAndRefresh(false, false, false, virtualFile);
            return virtualFile;
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage(), e, "Error create build file");
        }
    }
}
