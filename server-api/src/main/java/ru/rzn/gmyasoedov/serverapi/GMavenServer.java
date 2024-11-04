package ru.rzn.gmyasoedov.serverapi;

import ru.rzn.gmyasoedov.maven.plugin.reader.model.MavenMapResult;
import ru.rzn.gmyasoedov.serverapi.model.request.GetModelRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GMavenServer extends Remote {
    String SERVER_ERROR_MESSAGE = "Error on getting project model. See maven log";
    String SERVER_DEBUG_PROPERTY = "gmaven.server.debug";
    String MAVEN_EXT_CLASS_PATH_PROPERTY = "maven.ext.class.path";
    String GMAVEN_HOME = "gmaven.maven.home";
    String GMAVEN_PLUGINS = "gmaven.plugins";
    String GMAVEN_PLUGINS_RESOLVE = "gmaven.resolvedArtifactIds";
    String GMAVEN_PLUGIN_ANNOTATION_PROCESSOR = "gmaven.plugin.annotation.paths.%s";
    String RESOLVE_TASK_VERSION = "1.2.3";
    String MAVEN_MODEL_READER_PLUGIN_VERSION = "0.2";
    String RESOLVE_TASK = "ru.rzn.gmyasoedov:model-reader:" + RESOLVE_TASK_VERSION + ":resolve";
    String READ_TASK = "ru.rzn.gmyasoedov:model-reader:" + RESOLVE_TASK_VERSION + ":read";
    String GMAVEN_DEPENDENCY_TREE = "gmaven.event.dependency.tree";
    String GMAVEN_RESPONSE_POM_FILE = ".gmaven.pom.json";
    String GMAVEN_RESPONSE_TREE_FILE = ".gmaven.tree.json";

    MavenMapResult getProjectModel(GetModelRequest request) throws RemoteException;
}
