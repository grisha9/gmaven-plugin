package ru.rzn.gmyasoedov.serverapi;

import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer;
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest;
import ru.rzn.gmyasoedov.serverapi.model.GMavenExecutionResult;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GMavenServer extends Remote {
    String SERVER_DEBUG_PROPERTY = "gmaven.server.debug";
    String MAVEN_EXT_CLASS_PATH_PROPERTY = "maven.ext.class.path";
    String GMAVEN_HOME = "gmaven.maven.home";

    MavenProjectContainer getModel(GetModelRequest request) throws RemoteException;

    MavenProjectContainer getModelWithDependency(GetModelRequest request) throws RemoteException;

}
