// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.project;

import com.intellij.externalSystem.JavaModuleData;
import com.intellij.externalSystem.JavaProjectData;
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ui.configuration.SdkLookupDecision;
import com.intellij.openapi.roots.ui.configuration.SdkLookupUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.server.GServerRemoteProcessSupport;
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings;
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.*;
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.intellij.openapi.util.text.StringUtil.compareVersionNumbers;
import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID;

public class MavenProjectResolver implements ExternalSystemProjectResolver<MavenExecutionSettings> {

    @Override
    public @Nullable DataNode<ProjectData> resolveProjectInfo(
            @NotNull ExternalSystemTaskId id,
            @NotNull String projectPath,
            boolean isPreviewMode,
            @Nullable MavenExecutionSettings settings,
            @Nullable ProjectResolverPolicy resolverPolicy,
            @NotNull ExternalSystemTaskNotificationListener listener)
            throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
        GServerRemoteProcessSupport processSupport = new GServerRemoteProcessSupport(
                JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk(), null, Path.of(settings.getMavenHome()));
        MavenProjectContainer container;
        try {
            var server = processSupport.acquire(this, "", new EmptyProgressIndicator());
            GetModelRequest request = new GetModelRequest(settings.getServiceDirectory());
            container = server.getModelWithDependency(request);
        } catch (Exception e) {
            MavenLog.LOG.error(e);
            throw new RuntimeException(e);
        }
        processSupport.stopAll();
        if (true || isPreviewMode) {
            MavenProject project = container.getProject();
            String projectName = project.getDisplayName();
            String absolutePath = project.getFile().getParent();
            ProjectData projectData = new ProjectData(SYSTEM_ID, projectName, absolutePath, absolutePath);
            DataNode<ProjectData> projectDataNode = new DataNode<>(ProjectKeys.PROJECT, projectData, null);

            String sdkName = resolveJdkName("11");
            ProjectSdkData projectSdkData = new ProjectSdkData(sdkName);
            projectDataNode.createChild(ProjectSdkData.KEY, projectSdkData);
            LanguageLevel languageLevel = LanguageLevel.parse(sdkName);
            JavaProjectData javaProjectData =
                    new JavaProjectData(SYSTEM_ID, project.getOutputDirectory(), languageLevel,
                            languageLevel.toJavaVersion().toFeatureString());
            projectDataNode.createChild(JavaProjectData.KEY, javaProjectData);

            String ideProjectPath = settings == null ? null : settings.getIdeProjectPath();
            ideProjectPath = ideProjectPath == null ? projectPath : ideProjectPath;
            ProjectResolverContext context = new ProjectResolverContext(absolutePath, ideProjectPath);

            Map<String, DataNode<ModuleData>> moduleDataByArtifactId = new HashMap<>();
            var moduleNode = createModuleData(container, projectDataNode, context, moduleDataByArtifactId);

            for (MavenProjectContainer childContainer : container.getModules()) {
                createModuleData(childContainer, moduleNode, context, moduleDataByArtifactId);
            }
            addDependencies(container, moduleDataByArtifactId);
            return projectDataNode;
        }

        return null;
    }

    private void addDependencies(MavenProjectContainer container,
                                 Map<String, DataNode<ModuleData>> moduleDataByArtifactId) {
        addDependencies(container.getProject(), moduleDataByArtifactId);
        for (MavenProjectContainer childContainer : container.getModules()) {
            addDependencies(childContainer, moduleDataByArtifactId);
        }
    }

    private void addDependencies(MavenProject project, Map<String, DataNode<ModuleData>> moduleDataByArtifactId) {
        DataNode<ModuleData> moduleByMavenProject = moduleDataByArtifactId.get(project.getId());
        for (MavenArtifact artifact : project.getResolvedArtifacts()) {
            DataNode<ModuleData> moduleDataNodeByMavenArtifact = moduleDataByArtifactId.get(artifact.getId());
            if (moduleDataNodeByMavenArtifact == null) {
                addLibrary(moduleByMavenProject, artifact);
            } else {
                addModuleDependency(moduleByMavenProject, moduleDataNodeByMavenArtifact.getData());
            }
        }
    }

    private DataNode<ModuleData> createModuleData(@NotNull MavenProjectContainer container,
                                                  @NotNull DataNode<?> parentDataNode,
                                                  @NotNull ProjectResolverContext context,
                                                  @NotNull Map<String, DataNode<ModuleData>> moduleDataByArtifactId) {
        MavenProject project = container.getProject();
        String parentExternalName = getModuleName(parentDataNode.getData(), true);
        String parentInternalName = getModuleName(parentDataNode.getData(), false);
        String id = parentExternalName == null ? project.getArtifactId() : ":" + project.getArtifactId();
        String projectPath = project.getBasedir();

        final String mainModuleFileDirectoryPath = getIdeaModulePath(context, projectPath);

        ModuleData moduleData = new ModuleData(id, SYSTEM_ID, getDefaultModuleTypeId(), project.getArtifactId(),
                mainModuleFileDirectoryPath, //todo check rework
                projectPath);

        moduleData.setInternalName(getModuleName(parentInternalName, project.getArtifactId(), '.'));
        moduleData.setGroup(project.getGroupId());
        moduleData.setVersion(project.getVersion());
        moduleData.setModuleName(project.getArtifactId());

        moduleData.useExternalCompilerOutput(true);
        moduleData.setInheritProjectCompileOutputPath(false);
        moduleData.setCompileOutputPath(ExternalSystemSourceType.SOURCE, project.getOutputDirectory());
        moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.SOURCE, project.getOutputDirectory());

        DataNode<ModuleData> moduleDataDataNode = parentDataNode.createChild(ProjectKeys.MODULE, moduleData);
        moduleDataByArtifactId.put(project.getId(), moduleDataDataNode);

        ContentRootData contentRootData = new ContentRootData(SYSTEM_ID, projectPath);
        moduleDataDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData);
        storePath(project.getSourceRoots(), contentRootData, ExternalSystemSourceType.SOURCE);
        storePath(project.getResourceRoots(), contentRootData, ExternalSystemSourceType.RESOURCE);
        storePath(project.getTestSourceRoots(), contentRootData, ExternalSystemSourceType.TEST);
        storePath(project.getTestResourceRoots(), contentRootData, ExternalSystemSourceType.TEST_RESOURCE);

        String compilerPluginVersion = getCompilerPluginVersion(project);
        LanguageLevel sourceLanguageLevel = getMavenLanguageLevel(
                project, compilerPluginVersion, "maven.compiler.source");
        LanguageLevel targetBytecodeLevel = getMavenLanguageLevel(
                project, compilerPluginVersion, "maven.compiler.target");

        moduleDataDataNode.createChild(ModuleSdkData.KEY, new ModuleSdkData(null));

        moduleDataDataNode.createChild(JavaModuleData.KEY, new JavaModuleData(SYSTEM_ID, sourceLanguageLevel,
                targetBytecodeLevel.toJavaVersion().toFeatureString()));

        if (parentDataNode.getData() instanceof ModuleData) {
            for (MavenProjectContainer childContainer : container.getModules()) {
                createModuleData(childContainer, moduleDataDataNode, context, moduleDataByArtifactId);
            }
        }
        return moduleDataDataNode;
    }

    @NotNull
    private static String getIdeaModulePath(@NotNull ProjectResolverContext context, String projectPath) {
        final String relativePath;
        boolean isUnderProjectRoot = FileUtil.isAncestor(context.rootProjectPath, projectPath, false);
        if (isUnderProjectRoot) {
            relativePath = FileUtil.getRelativePath(context.rootProjectPath, projectPath, File.separatorChar);
        } else {
            relativePath = String.valueOf(FileUtil.pathHashCode(projectPath));
        }
        String subPath = relativePath == null || relativePath.equals(".") ? "" : relativePath;
        return context.ideaPrijectPath + File.separatorChar + subPath;
    }

    private String getModuleName(Object data, boolean external) {
        if (data instanceof ModuleData) {
            return external ? ((ModuleData) data).getExternalName() : ((ModuleData) data).getInternalName();
        }
        return null;
    }

    private static void storePath(List<String> paths, ContentRootData contentRootData, ExternalSystemSourceType type) {
        for (String path : paths) {
            contentRootData.storePath(type, path);
        }
    }

    @Override
    public boolean cancelTask(@NotNull ExternalSystemTaskId taskId,
                              @NotNull ExternalSystemTaskNotificationListener listener) {
        return false;
    }

    public static String getDefaultModuleTypeId() {
        return ModuleTypeManager.getInstance().getDefaultModuleType().getId();
    }

    private static @Nullable String resolveJdkName(@Nullable String jdkNameOrVersion) {
        if (jdkNameOrVersion == null) return null;
        Sdk sdk = SdkLookupUtil.lookupSdk(builder -> builder
                .withSdkName(jdkNameOrVersion)
                .withSdkType(ExternalSystemJdkUtil.getJavaSdkType())
                .onDownloadableSdkSuggested(__ -> SdkLookupDecision.STOP)
        );
        return sdk == null ? null : sdk.getName();
    }

    @Nullable
    private static LanguageLevel getMavenLanguageLevel(@NotNull MavenProject mavenProject,
                                                       @NotNull String compilerPluginVersion,
                                                       @NotNull String property) {

        boolean isReleaseEnabled = compareVersionNumbers(compilerPluginVersion, "3.6") >= 0;
        ;
        String mavenProjectReleaseLevel = isReleaseEnabled
                ? (String) mavenProject.getProperties().get("maven.compiler.release")
                : null;
        LanguageLevel level = LanguageLevel.parse(mavenProjectReleaseLevel);
        if (level == null) {
            String mavenProjectLanguageLevel = (String) mavenProject.getProperties().get(property);
            level = LanguageLevel.parse(mavenProjectLanguageLevel);
            if (level == null && (StringUtil.isNotEmpty(mavenProjectLanguageLevel)
                    || StringUtil.isNotEmpty(mavenProjectReleaseLevel))) {
                level = LanguageLevel.HIGHEST;
            }
        }
        return level;
    }

    @NotNull
    private static String getCompilerPluginVersion(@NotNull MavenProject mavenProject) {
        MavenPlugin plugin = MavenUtils.findPlugin(mavenProject, "org.apache.maven.plugins", "maven-compiler-plugin");
        return Optional.ofNullable(plugin).map(MavenId::getVersion).orElse(StringUtils.EMPTY);
    }

    private void addLibrary(DataNode<ModuleData> parentNode, MavenArtifact artifact) {
        LibraryData library = new LibraryData(SYSTEM_ID, artifact.getId());
        library.setArtifactId(artifact.getArtifactId());
        library.setGroup(artifact.getGroupId());
        library.setVersion(artifact.getVersion());

        library.addPath(LibraryPathType.BINARY, artifact.getFile().getAbsolutePath());

        /*LibraryLevel level = StringUtil.isNotEmpty(libraryName) ? LibraryLevel.PROJECT : LibraryLevel.MODULE;
        if (StringUtil.isEmpty(libraryName) || !linkProjectLibrary(resolverCtx, ideProject, library)) {
            level = LibraryLevel.MODULE;
        }
*/
        ModuleData data = parentNode.getData();
        LibraryDependencyData libraryDependencyData = new LibraryDependencyData(data, library, LibraryLevel.MODULE);
        libraryDependencyData.setScope(DependencyScope.COMPILE);
        //libraryDependencyData.setOrder(mergedDependency.getClasspathOrder() + classpathOrderShift);
        libraryDependencyData.setOrder(2);
        parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData);
    }

    private void addModuleDependency(DataNode<ModuleData> parentNode, ModuleData targetModule) {
        ModuleData ownerModule = parentNode.getData();
        ModuleDependencyData data = new ModuleDependencyData(ownerModule, targetModule);
        data.setOrder(1);
        DataNode<ModuleDependencyData> dataNode = parentNode
                .createChild(ProjectKeys.MODULE_DEPENDENCY, data);
        // dataNode.setIgnored(true);
    }

    private static String getModuleName(@Nullable String parentName, @NotNull String moduleName, char delimiter) {
        if (parentName == null) return moduleName;
        return parentName + delimiter + moduleName;
    }

    public static class ProjectResolverContext {
        final String rootProjectPath;
        final String ideaPrijectPath;

        public ProjectResolverContext(String rootProjectPath, String ideaPrijectPath) {
            this.rootProjectPath = rootProjectPath;
            this.ideaPrijectPath = ideaPrijectPath;
        }
    }
}
