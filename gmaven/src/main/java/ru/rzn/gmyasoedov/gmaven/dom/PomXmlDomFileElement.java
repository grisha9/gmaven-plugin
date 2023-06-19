package ru.rzn.gmyasoedov.gmaven.dom;

import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import com.intellij.util.xml.EvaluatedXmlNameImpl;
import com.intellij.util.xml.impl.DomFileElementImpl;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomProjectModel;

public class PomXmlDomFileElement extends DomFileElementImpl<MavenDomProjectModel> {
    public PomXmlDomFileElement(XmlFile file, EvaluatedXmlNameImpl rootTagName,
                                DomFileDescription<MavenDomProjectModel> fileDescription) {
        super(file, rootTagName, fileDescription, null);
    }

    public final @NotNull MavenDomProjectModel getRootElementMy() {
        return (MavenDomProjectModel) getRootHandler().getProxy();
    }

}
