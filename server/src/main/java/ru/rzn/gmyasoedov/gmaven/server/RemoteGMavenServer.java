package ru.rzn.gmyasoedov.gmaven.server;


import com.intellij.execution.rmi.RemoteServer;
import ru.rzn.gmyasoedov.gmaven.server.impl.GMavenServerImpl;

import static ru.rzn.gmyasoedov.serverapi.GMavenServer.SERVER_DEBUG_PROPERTY;


public class RemoteGMavenServer extends RemoteServer {

    public static void main(String[] args) throws Exception {
        boolean debugEnabled = Boolean.parseBoolean(System.getProperty(SERVER_DEBUG_PROPERTY));
        boolean wslEnabled = Boolean.parseBoolean(System.getProperty("idea.maven.wsl"));
        start(new GMavenServerImpl(), !wslEnabled, !debugEnabled);
    }
}

