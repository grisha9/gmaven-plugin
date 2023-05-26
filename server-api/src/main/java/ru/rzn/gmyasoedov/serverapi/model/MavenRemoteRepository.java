package ru.rzn.gmyasoedov.serverapi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class MavenRemoteRepository implements Serializable {
    private final String id;
    private final String url;
}
