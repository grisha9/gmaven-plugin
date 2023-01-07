package ru.rzn.gmyasoedov.gmaven.server.impl;

import com.intellij.openapi.util.text.StringUtilRt;
import org.codehaus.plexus.classworlds.launcher.Launcher;
import ru.rzn.gmyasoedov.gmaven.server.result.ResultHolder;
import ru.rzn.gmyasoedov.serverapi.GMavenServer;
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer;
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest;
import ru.rzn.gmyasoedov.serverapi.model.GMavenExecutionResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GMavenServerImpl implements GMavenServer {
    private static final String MODEL_READER_PLUGIN_GAV = "ru.rzn.gmyasoedov:model-reader:1.0-SNAPSHOT";

    @Override
    public MavenProjectContainer getModel(GetModelRequest request) throws RemoteException {
        fillSystemProperties(request);
        String[] args = Arrays.asList(MODEL_READER_PLUGIN_GAV + ":read", "-N").toArray(new String[0]);
        try {
            Launcher.mainWithExitCode(args);
            return ResultHolder.projectContainer;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public MavenProjectContainer getModelWithDependency(GetModelRequest request) throws RemoteException {
        fillSystemProperties(request);
        List<String> mvnArgs = new ArrayList<>();
        mvnArgs.add(MODEL_READER_PLUGIN_GAV + ":resolve");
        if (!StringUtilRt.isEmpty(request.artifactId)) {
            mvnArgs.addAll(Arrays.asList("-pl", request.artifactId, "-amd", "-am"));
        }
        if (request.offline) {
            mvnArgs.add("-o");
        }
        try {
            Launcher.mainWithExitCode(mvnArgs.toArray(new String[0]));
            return ResultHolder.projectContainer;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
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
