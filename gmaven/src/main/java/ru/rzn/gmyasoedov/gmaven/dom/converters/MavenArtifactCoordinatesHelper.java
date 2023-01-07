package ru.rzn.gmyasoedov.gmaven.dom.converters;

import com.intellij.util.xml.ConvertContext;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;

public class MavenArtifactCoordinatesHelper {

    public static MavenId getId(ConvertContext context) {
        return new MavenId("", "", "");
    }
}
