package ru.rzn.gmyasoedov.gmaven.server.impl;

import com.intellij.openapi.util.text.StringUtilRt;
import org.codehaus.plexus.classworlds.launcher.Launcher;
import ru.rzn.gmyasoedov.gmaven.server.result.ResultHolder;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;
import ru.rzn.gmyasoedov.serverapi.model.MavenResult;
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest;

import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GMavenServerImpl implements GMavenServer {

    @Override
    public MavenResult getProjectModel(GetModelRequest request) throws RemoteException {
        fillSystemProperties(request);
        try {
            Launcher.mainWithExitCode(getMvnArgs(request));
            return ResultHolder.result;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static String[] getMvnArgs(GetModelRequest request) {
        List<String> mvnArgs = new ArrayList<>();
        if (!StringUtilRt.isEmpty(request.artifactId)) {
            mvnArgs.addAll(Arrays.asList("-pl", request.artifactId, "-amd", "-am"));
        }
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
        if (!StringUtilRt.isEmpty(request.gMavenPluginPath)) {
            mvnArgs.add("install:install-file");
            mvnArgs.add("-Dfile=" + request.gMavenPluginPath);
            mvnArgs.add("-DgroupId=ru.rzn.gmyasoedov");
            mvnArgs.add("-DartifactId=model-reader");
            mvnArgs.add("-Dversion=1.0-SNAPSHOT");
            mvnArgs.add("-Dpackaging=jar");
        } else if (request.tasks != null && !request.tasks.isEmpty()) {
            mvnArgs.addAll(request.tasks);
        } else {
            mvnArgs.add("ru.rzn.gmyasoedov:model-reader:1.0-SNAPSHOT:resolve");
        }
        if (!StringUtilRt.isEmpty(request.profiles)) {
            mvnArgs.add("-P");
            mvnArgs.add(request.profiles);
        }
        if (!StringUtilRt.isEmpty(request.analyzerGA)) {
            mvnArgs.add("-pl");
            mvnArgs.add(request.analyzerGA);
            mvnArgs.add("-Daether.conflictResolver.verbose=true");
            mvnArgs.add("-Daether.dependencyManager.verbose=true");
            //mvnArgs.add("-amd"); mvnArgs.add( "-am");
        }
        //[ru.rzn.gmyasoedov:model-reader:1.0-SNAPSHOT:resolve, -pl, org.example:untitled4, -Daether.conflictResolver.verbose=true, -Daether.dependencyManager.verbose=true, -amd, -am]
        return mvnArgs.toArray(new String[0]);
    }

    private static void fillSystemProperties(GetModelRequest request) {
        String mavenHome = System.getProperty(GMavenServer.GMAVEN_HOME);
        String extClasspath = System.getProperty(GMavenServer.MAVEN_EXT_CLASS_PATH_PROPERTY);
        if (mavenHome == null) throw new RuntimeException("no maven home path");
        if (extClasspath == null) throw new RuntimeException("no maven ext class path");
        String projectPath = request.projectPath;

        System.setProperty("classworlds.conf", Paths.get(mavenHome, "bin", "m2.conf").toString());
        System.setProperty("maven.home", mavenHome);
        System.setProperty("library.jansi.path", Paths.get(mavenHome, "lib", "jansi-native").toString());
        System.setProperty("maven.multiModuleProjectDirectory", projectPath);
        System.setProperty("user.dir", projectPath);

    }
}
