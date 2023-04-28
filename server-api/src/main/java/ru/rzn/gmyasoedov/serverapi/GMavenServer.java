package ru.rzn.gmyasoedov.serverapi;

import ru.rzn.gmyasoedov.serverapi.model.MavenResult;
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GMavenServer extends Remote {
    String SERVER_DEBUG_PROPERTY = "gmaven.server.debug";
    String MAVEN_EXT_CLASS_PATH_PROPERTY = "maven.ext.class.path";
    String GMAVEN_HOME = "gmaven.maven.home";
    String GMAVEN_PLUGINS = "gmaveng.plugins";

    MavenResult getProjectModel(GetModelRequest request) throws RemoteException;
}
