package ru.rzn.gmyasoedov.gmaven.utils;

import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.DisposeAwareRunnable;
import com.intellij.util.SystemProperties;
import com.intellij.util.xml.NanoXmlBuilder;
import com.intellij.util.xml.NanoXmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectsManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.util.io.JarUtil.getJarAttribute;
import static com.intellij.openapi.util.io.JarUtil.loadProperties;
import static com.intellij.openapi.util.text.StringUtil.*;
import static com.intellij.util.xml.NanoXmlBuilder.stop;
import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.M2;

public class MavenUtils {
    private MavenUtils() {
    }

    @NotNull
    public static VirtualFile getVFile(File file) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (virtualFile == null) throw new RuntimeException("Virtual file not found " + file);
        return virtualFile;
    }

    public static void runWhenInitialized(@NotNull Project project, @NotNull Runnable runnable) {
        if (project.isDisposed()) {
            return;
        }

        if (isNoBackgroundMode()) {
            runnable.run();
        } else if (project.isInitialized()) {
            runDumbAware(project, runnable);
        } else {
            StartupManager.getInstance(project).runAfterOpened(runnable);
        }
    }

    private static boolean isNoBackgroundMode() {
        return (ApplicationManager.getApplication().isUnitTestMode()
                || ApplicationManager.getApplication().isHeadlessEnvironment() &&
                !CoreProgressManager.shouldKeepTasksAsynchronousInHeadlessMode());
    }

    private static void runDumbAware(@NotNull Project project, @NotNull Runnable r) {
        if (DumbService.isDumbAware(r)) {
            r.run();
        } else {
            DumbService.getInstance(project).runWhenSmart(DisposeAwareRunnable.create(r, project));
        }
    }

    public static void setupFileTemplate(Project project,
                                         VirtualFile file,
                                         Properties properties) throws IOException {
        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate fileTemplate = manager.getJ2eeTemplate(MavenFileTemplateGroupFactory.MAVEN_PROJECT_XML_TEMPLATE);
        Properties allProperties = manager.getDefaultProperties();
        allProperties.putAll(properties);

        String text = fileTemplate.getText(allProperties);
        Pattern pattern = Pattern.compile("\\$\\{(.*)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, "\\$" + toUpperCase(matcher.group(1)) + "\\$");
        }
        matcher.appendTail(builder);
        text = builder.toString();

        TemplateImpl template = (TemplateImpl) TemplateManager.getInstance(project).createTemplate("", "", text);
        for (int i = 0; i < template.getSegmentsCount(); i++) {
            if (i == template.getEndSegmentNumber()) continue;
            String name = template.getSegmentName(i);
            String value = "\"" + properties.getProperty(name, "") + "\"";
            template.addVariable(name, value, value, true);
        }

        VfsUtil.saveText(file, template.getTemplateText());

       /* PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            new ReformatCodeProcessor(project, psiFile, null, false).run();
        }*/
    }

    public static boolean isPomFileName(String fileName) {
        return fileName.equals(GMavenConstants.POM_XML) ||
                fileName.endsWith(".pom") || fileName.startsWith("pom.") ||
                fileName.equals(GMavenConstants.SUPER_POM_XML);
    }

    public static boolean isPotentialPomFile(String nameOrPath) {
        String[] split;
        try {
            String extensions = Registry.stringValue("gmaven.support.extensions");
            split = extensions.split(",");
        } catch (Exception e) {
            split = new String[]{"pom"};
        }
        return ArrayUtil.contains(FileUtilRt.getExtension(nameOrPath), split);
    }

    public static boolean isPomFile(@Nullable Project project, @Nullable VirtualFile file) {
        if (file == null) return false;

        String name = file.getName();
        if (isPomFileName(name)) return true;
        if (!isPotentialPomFile(name)) return false;

        return isPomFileIgnoringName(project, file);
    }

    public static boolean isSimplePomFile(@Nullable VirtualFile file) {
        if (file == null) return false;

        String name = file.getName();
        if (isPomFileName(name)) return true;
        if (!isPotentialPomFile(name)) return false;
        return false;
    }

    public static boolean isPomFileIgnoringName(@Nullable Project project, @NotNull VirtualFile file) {
        if (project == null || !project.isInitialized()) {
            if (!FileUtil.extensionEquals(file.getName(), "xml")) return false;
            try {
                try (InputStream in = file.getInputStream()) {
                    Ref<Boolean> isPomFile = Ref.create(false);
                    Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    NanoXmlUtil.parse(reader, new NanoXmlBuilder() {
                        @Override
                        public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception {
                            if ("project".equals(name)) {
                                isPomFile.set(nsURI.startsWith("http://maven.apache.org/POM/"));
                            }
                            stop();
                        }
                    });
                    return isPomFile.get();
                }
            } catch (IOException ignore) {
                return false;
            }
        }

        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
        if (mavenProjectsManager.findProject(file) != null) return true;

        return ReadAction.compute(() -> {
            if (project.isDisposed()) return false;
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) return false;
            return MavenDomUtil.isProjectFile(psiFile);
        });
    }

    public static void showError(Project project, @NlsContexts.NotificationTitle String title, Throwable e) {
        MavenLog.LOG.warn(title, e);
        Notifications.Bus.notify(new Notification(
                GMavenConstants.GMAVEN, title, e.getMessage(), NotificationType.ERROR), project
        );
    }

    public static void setupProjectSdk(@NotNull Project project) {
        if (ProjectRootManager.getInstance(project).getProjectSdk() == null) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                Sdk projectSdk = suggestProjectSdk();
                if (projectSdk == null) return;
                JavaSdkUtil.applyJdkToProject(project, projectSdk);
            });
        }
    }

    @Nullable
    public static Sdk suggestProjectSdk() {
        Project defaultProject = ProjectManager.getInstance().getDefaultProject();
        ProjectRootManager defaultProjectManager = ProjectRootManager.getInstance(defaultProject);
        Sdk defaultProjectSdk = defaultProjectManager.getProjectSdk();
        if (defaultProjectSdk != null) return null;
        ProjectJdkTable projectJdkTable = ProjectJdkTable.getInstance();
        Sdk[] allJdks = projectJdkTable.getAllJdks();
        if (allJdks.length == 0) {
            projectJdkTable.preconfigure();
        }
        SdkType sdkType = ExternalSystemJdkUtil.getJavaSdkType();
        return projectJdkTable.getSdksOfType(sdkType).stream()
                .filter(it -> it.getHomePath() != null && JdkUtil.checkForJre(it.getHomePath()))
                .max(sdkType.versionComparator())
                .orElse(null);
    }

    @Nullable
    public static Module findIdeModule(@NotNull Project project, @NotNull ModuleData moduleData) {
        return new IdeModelsProviderImpl(project).findIdeModule(moduleData);
    }

    @Nullable
    public static File resolveMavenHome() {
        String m2home = System.getenv("M2_HOME");
        if (!isEmptyOrSpaces(m2home)) {
            final File homeFromEnv = new File(m2home);
            if (isValidMavenHome(homeFromEnv)) {
                return homeFromEnv;
            }
        }

        String mavenHome = System.getenv("MAVEN_HOME");
        if (!isEmptyOrSpaces(mavenHome)) {
            final File mavenHomeFile = new File(mavenHome);
            if (isValidMavenHome(mavenHomeFile)) {
                return mavenHomeFile;
            }
        }

        String userHome = SystemProperties.getUserHome();
        if (!isEmptyOrSpaces(userHome)) {
            File underUserHome = new File(userHome, "m2");
            if (isValidMavenHome(underUserHome)) {
                return underUserHome;
            }

            File sdkManMavenHome = Path.of(userHome, ".sdkman", "candidates", "maven", "current").toFile();
            if (isValidMavenHome(sdkManMavenHome)) {
                return sdkManMavenHome;
            }
        }

        if (SystemInfo.isMac) {
            File home = fromBrew();
            if (home != null) {
                return home;
            }

            if ((home = fromMacSystemJavaTools()) != null) {
                return home;
            }
        } else if (SystemInfo.isLinux) {
            File home = new File("/usr/share/maven");
            if (isValidMavenHome(home)) {
                return home;
            }

            home = new File("/usr/share/maven2");
            if (isValidMavenHome(home)) {
                return home;
            }
        }

        return null;
    }

    public static boolean isValidMavenHome(@Nullable File home) {
        return home != null && isValidMavenHome(home.toPath());
    }

    public static boolean isValidMavenHome(@Nullable Path home) {
        if (home == null) return false;
        return home.resolve("bin").resolve("m2.conf").toFile().exists();
    }

    @Nullable
    private static File fromBrew() {
        final File brewDir = new File("/usr/local/Cellar/maven");
        final String[] list = brewDir.list();
        if (list == null || list.length == 0) {
            return null;
        }

        if (list.length > 1) {
            Arrays.sort(list, (o1, o2) -> compareVersionNumbers(o2, o1));
        }

        final File file = new File(brewDir, list[0] + "/libexec");
        return isValidMavenHome(file) ? file : null;
    }

    @Nullable
    private static File fromMacSystemJavaTools() {
        final File symlinkDir = new File("/usr/share/maven");
        if (isValidMavenHome(symlinkDir)) {
            return symlinkDir;
        }

        // well, try to search
        final File dir = new File("/usr/share/java");
        final String[] list = dir.list();
        if (list == null || list.length == 0) {
            return null;
        }

        String home = null;
        final String prefix = "maven-";
        final int versionIndex = prefix.length();
        for (String path : list) {
            if (path.startsWith(prefix) &&
                    (home == null || compareVersionNumbers(path.substring(versionIndex), home.substring(versionIndex)) > 0)) {
                home = path;
            }
        }

        if (home != null) {
            File file = new File(dir, home);
            if (isValidMavenHome(file)) {
                return file;
            }
        }

        return null;
    }

    public static @NotNull @NlsSafe Path getGeneratedSourcesDirectory(
            @NotNull String buildDirectory, boolean testSources) {
        return Path.of(buildDirectory, (testSources ? "generated-test-sources" : "generated-sources"));
    }

    public static @NotNull @NlsSafe Path getGeneratedAnnotationsDirectory(
            @NotNull String buildDirectory,
            boolean testSources) {
        return getGeneratedSourcesDirectory(buildDirectory, testSources)
                .resolve((testSources ? "test-annotations" : "annotations"));
    }

    @Nullable
    public static String getMavenVersion(@Nullable File mavenHome) {
        if (mavenHome == null) return null;
        File[] libs = new File(mavenHome, "lib").listFiles();


        if (libs != null) {
            for (File mavenLibFile : libs) {
                String lib = mavenLibFile.getName();
                if (lib.equals("maven-core.jar")) {
                    MavenLog.LOG.debug("Choosing version by maven-core.jar");
                    return getMavenLibVersion(mavenLibFile);
                }
                if (lib.startsWith("maven-core-") && lib.endsWith(".jar")) {
                    MavenLog.LOG.debug("Choosing version by maven-core.xxx.jar");
                    String version = lib.substring("maven-core-".length(), lib.length() - ".jar".length());
                    return contains(version, ".x") ? getMavenLibVersion(mavenLibFile) : version;
                }
                if (lib.startsWith("maven-") && lib.endsWith("-uber.jar")) {
                    MavenLog.LOG.debug("Choosing version by maven-xxx-uber.jar");
                    return lib.substring("maven-".length(), lib.length() - "-uber.jar".length());
                }
            }
        }
        MavenLog.LOG.warn("Cannot resolve maven version for " + mavenHome);
        return null;
    }

    private static String getMavenLibVersion(final File file) {
        WSLDistribution distribution = WslPath.getDistributionByWindowsUncPath(file.getPath());
        File fileToRead = Optional.ofNullable(distribution)
                .map(it -> distribution.getWslPath(file.getPath()))
                .map(distribution::resolveSymlink)
                .map(distribution::getWindowsPath)
                .map(File::new)
                .orElse(file);

        Properties props = loadProperties(fileToRead, "META-INF/maven/org.apache.maven/maven-core/pom.properties");
        return props != null
                ? nullize(props.getProperty("version"))
                : nullize(getJarAttribute(file, java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION));
    }

    @NotNull
    public static Path resolveM2() {
        return Path.of(SystemProperties.getUserHome(), M2);
    }
}
