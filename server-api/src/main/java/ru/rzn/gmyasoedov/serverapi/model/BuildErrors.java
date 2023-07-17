package ru.rzn.gmyasoedov.serverapi.model;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BuildErrors {
    public final boolean pluginNotResolved;
    public final List<MavenException> exceptions;
}
