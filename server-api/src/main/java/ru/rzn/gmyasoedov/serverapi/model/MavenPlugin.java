// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package ru.rzn.gmyasoedov.serverapi.model;

public final class MavenPlugin extends MavenId {
    static final long serialVersionUID = -6113607480882347420L;

    public MavenPlugin(String groupId,
                       String artifactId,
                       String version) {
        super(groupId, artifactId, version);
    }

}
