package ru.rzn.gmyasoedov.serverapi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class MavenProfile implements Serializable {
    private final String name;
    private final boolean activation;
}
