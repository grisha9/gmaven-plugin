package ru.rzn.gmyasoedov.gmaven.server;


import com.intellij.execution.rmi.GRemoteServer;
import ru.rzn.gmyasoedov.gmaven.server.impl.GMavenServerImpl;

public class RemoteGMavenServer extends GRemoteServer {

    public static void main(String[] args) throws Exception {
        start(new GMavenServerImpl(), !Boolean.parseBoolean(System.getProperty("idea.maven.wsl")));
    }
}

