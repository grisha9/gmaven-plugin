// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.dom;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomParent;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomProjectModel;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomProperties;
import ru.rzn.gmyasoedov.gmaven.utils.MavenDomUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MavenPropertyResolver {
    public static final Pattern PATTERN = Pattern.compile("\\$\\{(.+?)}|@(.+?)@");

    /**
     * Resolve properties from the string (either like {@code ${propertyName}} or like {@code @propertyName@}).
     *
     * @param text       text string to resolve properties in
     * @param projectDom a project dom
     * @return string with the properties resolved
     */
    public static String resolve(String text, MavenDomProjectModel projectDom) {
        XmlElement element = projectDom.getXmlElement();
        if (element == null) return text;

        VirtualFile file = MavenDomUtil.getVirtualFile(element);
        if (file == null) return text;

        StringBuilder res = new StringBuilder();
        try {
            doFilterText(PATTERN, projectDom, text, null, false, null, res);
        } catch (IOException e) {
            throw new RuntimeException(e); // never thrown
        }

        return res.toString();
    }

    private static void doFilterText(Pattern pattern,
                                     MavenDomProjectModel projectDom,
                                     String text,
                                     @Nullable String escapeString,
                                     boolean escapeWindowsPath,
                                     @Nullable Map<String, String> resolvedPropertiesParam,
                                     Appendable out) throws IOException {
        Map<String, String> resolvedProperties = resolvedPropertiesParam;

        Matcher matcher = pattern.matcher(text);
        int groupCount = matcher.groupCount();

        int last = 0;
        while (matcher.find()) {
            if (escapeString != null) {
                int escapeStringStartIndex = matcher.start() - escapeString.length();
                if (escapeStringStartIndex >= last) {
                    if (text.startsWith(escapeString, escapeStringStartIndex)) {
                        out.append(text, last, escapeStringStartIndex);
                        out.append(matcher.group());
                        last = matcher.end();
                        continue;
                    }
                }
            }

            out.append(text, last, matcher.start());
            last = matcher.end();

            String propertyName = null;

            for (int i = 0; i < groupCount; i++) {
                propertyName = matcher.group(i + 1);
                if (propertyName != null) {
                    break;
                }
            }

            assert propertyName != null;

            if (resolvedProperties == null) {
                resolvedProperties = new HashMap<>();
            }

            String propertyValue = resolvedProperties.get(propertyName);
            if (propertyValue == null) {
                if (resolvedProperties.containsKey(propertyName)) { // if cyclic property dependencies
                    out.append(matcher.group());
                    continue;
                }


                String resolved = doResolvePropertyForMavenDomModel(propertyName, projectDom);

                if (resolved == null) {
                    out.append(matcher.group());
                    continue;
                }

                resolvedProperties.put(propertyName, null);

                StringBuilder sb = new StringBuilder();
                doFilterText(pattern, projectDom, resolved, null, escapeWindowsPath, resolvedProperties, sb);
                propertyValue = sb.toString();

                resolvedProperties.put(propertyName, propertyValue);
            }

            if (escapeWindowsPath) {
                //MavenEscapeWindowsCharacterUtils.escapeWindowsPath(out, propertyValue);
            } else {
                out.append(propertyValue);
            }
        }

        out.append(text, last, text.length());
    }

    public static Properties collectPropertiesFromDOM(MavenDomProjectModel projectDom) {
        Properties result = new Properties();
        collectPropertiesFromDOM(projectDom.getProperties(), result);
        return result;
    }

    private static void collectPropertiesFromDOM(MavenDomProperties props, Properties result) {
        XmlTag propsTag = props.getXmlTag();
        if (propsTag != null) {
            for (XmlTag each : propsTag.getSubTags()) {
                result.setProperty(each.getName(), each.getValue().getTrimmedText());
            }
        }
    }

    @Nullable
    private static String doResolvePropertyForMavenDomModel(String propName, MavenDomProjectModel projectDom) {
        if (propName.startsWith("parent.")) {
            MavenDomParent parentDomElement = projectDom.getMavenParent();
            if (!parentDomElement.exists()) {
                return null;
            }

            propName = propName.substring("parent.".length());

            if (propName.equals("groupId")) {
                return parentDomElement.getGroupId().getStringValue();
            }
            if (propName.equals("artifactId")) {
                return parentDomElement.getArtifactId().getStringValue();
            }
            if (propName.equals("version")) {
                return parentDomElement.getVersion().getStringValue();
            }
        }
        return null;
    }
}
