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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.serialization.PropertyMapping;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.server.GServerRemoteProcessSupport;
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings;
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.*;
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest;

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
        MavenProjectContainer container = null;
        try {
            var server = processSupport.acquire(this, "", new EmptyProgressIndicator());
            GetModelRequest request = new GetModelRequest(settings.getServiceDirectory());
            container = server.getModelWithDependency(request);
            System.out.println(container);
        } catch (Exception e) {
            MavenLog.LOG.error(e);
            e.printStackTrace();
            System.out.println(e);
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

            final String ideProjectPath = settings == null ? null : settings.getIdeProjectPath();
            final String mainModuleFileDirectoryPath = ideProjectPath == null ? projectPath : ideProjectPath;

            Map<String, DataNode<ModuleData>> moduleDataByArtifactId = new HashMap<>();
            var moduleNode = createModuleData(null, container, projectDataNode,
                    mainModuleFileDirectoryPath, moduleDataByArtifactId);

            for (MavenProjectContainer childContainer : container.getModules()) {
                createModuleData(project.getArtifactId(), childContainer, moduleNode,
                        mainModuleFileDirectoryPath, moduleDataByArtifactId);
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

    private DataNode<ModuleData> createModuleData(@Nullable String parentModuleId,
                                                  @NotNull MavenProjectContainer container,
                                                  @NotNull DataNode<?> parentDataNode,
                                                  @NotNull String mainModuleFileDirectoryPath,
                                                  @NotNull Map<String, DataNode<ModuleData>> moduleDataByArtifactId) {
        MavenProject project = container.getProject();
        String externalName = getModuleName(parentModuleId, project.getArtifactId(), ':');
        String internalName = getModuleName(parentModuleId, project.getArtifactId(), '.');
        String projectPath = project.getBasedir();
        ModuleData moduleData = new MavenModuleData(externalName,
                externalName, internalName, mainModuleFileDirectoryPath,
                projectPath);// ???projectPath todo check projectPath

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

        if (parentModuleId != null) {
            for (MavenProjectContainer childContainer : container.getModules()) {
                createModuleData(project.getArtifactId(), childContainer, moduleDataDataNode,
                        mainModuleFileDirectoryPath, moduleDataByArtifactId);
            }
        }
        return moduleDataDataNode;
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
        parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData);
    }

    private void addModuleDependency(DataNode<ModuleData> parentNode, ModuleData targetModule) {
        ModuleData ownerModule = parentNode.getData();
        parentNode.createChild(ProjectKeys.MODULE_DEPENDENCY, new ModuleDependencyData(ownerModule, targetModule));
    }

    private static String getModuleName(@Nullable String parentName, @NotNull String moduleName, char delimiter) {
        if (parentName == null) return moduleName;
        return parentName + delimiter + moduleName;
    }

    public static class MavenModuleData extends ModuleData {

        @PropertyMapping({"id", "externalName", "internalName", "moduleFileDirectoryPath", "externalConfigPath"})
        public MavenModuleData(@NotNull String id,
                               @NotNull String externalName,
                               @NotNull String internalName,
                               @NotNull String moduleFileDirectoryPath,
                               @NotNull String externalConfigPath) {
            super(id, SYSTEM_ID, getDefaultModuleTypeId(),
                    externalName, internalName,
                    moduleFileDirectoryPath, externalConfigPath);
            String moduleName = StringUtil.substringAfterLast(getExternalName(), ":");
            if (moduleName != null) {
                setModuleName(moduleName);
            }
        }
    }
}
