package ru.rzn.gmyasoedov.gmaven.server.impl;

import com.intellij.openapi.util.text.StringUtilRt;
import org.codehaus.plexus.classworlds.launcher.Launcher;
import ru.rzn.gmyasoedov.gmaven.server.result.ResultHolder;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;
import ru.rzn.gmyasoedov.serverapi.GServerUtils;
import ru.rzn.gmyasoedov.serverapi.model.MavenResult;
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest;

import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class GMavenServerImpl implements GMavenServer {

    @Override
    public MavenResult getProjectModel(GetModelRequest request) throws RemoteException {
        fillSystemProperties(request);
        try {
            Launcher.mainWithExitCode(getMvnArgs(request));
            return GServerUtils.toResult(ResultHolder.result);
        } catch (Exception e) {
            return GServerUtils.toResult(e);
        }
    }

    private static String[] getMvnArgs(GetModelRequest request) {
        List<String> mvnArgs = new ArrayList<>();
        if (!StringUtilRt.isEmpty(request.alternativePom)) {
            mvnArgs.add("-f");
            mvnArgs.add(request.alternativePom);
        }
        if (request.nonRecursion) {
            mvnArgs.add("-N");
        }
        if (request.offline) {
            mvnArgs.add("-o");
        }
        if (!StringUtilRt.isEmpty(request.profiles)) {
            mvnArgs.add("-P");
            mvnArgs.add(request.profiles);
        }
        if (!StringUtilRt.isEmpty(request.threadCount)) {
            mvnArgs.add("-T");
            mvnArgs.add(request.threadCount);
        }
        if (request.quiteLogs) {
            mvnArgs.add("-q");
        }
        if (request.debugLog) {
            mvnArgs.add("-X");
        }
        if (request.updateSnapshots) {
            mvnArgs.add("-U");
        }
        if (request.notUpdateSnapshots) {
            mvnArgs.add("-nsu");
        }
        if (!StringUtilRt.isEmpty(request.dependencyAnalyzerGA)) {
            mvnArgs.add("-D" + GMAVEN_DEPENDENCY_TREE + "=true");
            if (!request.dependencyAnalyzerGA.equals(RESOLVE_TASK)) {
                mvnArgs.add("-pl");
                mvnArgs.add(request.dependencyAnalyzerGA);
                mvnArgs.add("-Daether.conflictResolver.verbose=true");
                mvnArgs.add("-Daether.dependencyManager.verbose=true");
                mvnArgs.add("-am");
            }
        } else if (!StringUtilRt.isEmpty(request.projectList)) {
            mvnArgs.add("-pl");
            mvnArgs.add(request.projectList);
            if (request.subTaskArguments != null) {
                mvnArgs.addAll(request.subTaskArguments);
            }
        }
        if (request.additionalArguments != null && !request.additionalArguments.isEmpty()) {
            mvnArgs.addAll(request.additionalArguments);
        }
        if (request.importArguments != null && !request.importArguments.isEmpty()) {
            mvnArgs.addAll(request.importArguments);
        }
        if (!StringUtilRt.isEmpty(request.gMavenPluginPath)) {
            mvnArgs.add("install:install-file");
            mvnArgs.add("-Dfile=" + request.gMavenPluginPath);
            mvnArgs.add("-DgroupId=ru.rzn.gmyasoedov");
            mvnArgs.add("-DartifactId=model-reader");
            mvnArgs.add("-Dversion=" + RESOLVE_TASK_VERSION);
            mvnArgs.add("-Dpackaging=jar");
        } else if (request.tasks != null && !request.tasks.isEmpty()) {
            mvnArgs.addAll(request.tasks);
        } else if (request.readOnly) {
            mvnArgs.add(READ_TASK);
        } else {
            mvnArgs.add(RESOLVE_TASK);
        }
        System.out.println("mvn" + getMvnArgs(mvnArgs));
        return mvnArgs.toArray(new String[0]);
    }

    private static String getMvnArgs(List<String> mvnArgs) {
        try {
            StringBuilder result = new StringBuilder();
            for (String arg : mvnArgs) {
                result.append(" ").append(arg);
            }
            return result.toString();
        } catch (Exception ignore) {
            return "";
        }
    }

    private static void fillSystemProperties(GetModelRequest request) {
        String mavenHome = System.getProperty(GMavenServer.GMAVEN_HOME);
        String extClasspath = System.getProperty(GMavenServer.MAVEN_EXT_CLASS_PATH_PROPERTY);
        if (mavenHome == null) throw new RuntimeException("no maven home path");
        if (extClasspath == null) throw new RuntimeException("no maven ext class path");
        String projectPath = request.multiModuleProjectDirectory != null
                ? request.multiModuleProjectDirectory : request.projectPath;
        System.setProperty("classworlds.conf", Paths.get(mavenHome, "bin", "m2.conf").toString());
        System.setProperty("maven.home", mavenHome);
        System.setProperty("library.jansi.path", Paths.get(mavenHome, "lib", "jansi-native").toString());
        System.setProperty("maven.multiModuleProjectDirectory", projectPath);
        System.setProperty("user.dir", request.projectPath);
        if (request.multiModuleProjectDirectory != null) {
            System.out.printf("userDir: %s multiModuleProjectDir: %s%n", request.projectPath, projectPath);
        }
    }
}
