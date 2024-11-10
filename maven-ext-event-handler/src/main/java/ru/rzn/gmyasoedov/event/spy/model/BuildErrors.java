package ru.rzn.gmyasoedov.event.spy.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class BuildErrors implements Serializable {
    public boolean pluginNotResolved;
    public List<String> exceptions = Collections.emptyList();
}
