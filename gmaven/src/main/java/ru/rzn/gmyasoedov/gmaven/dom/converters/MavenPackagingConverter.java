/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.rzn.gmyasoedov.gmaven.dom.converters;

import com.intellij.util.xml.ConvertContext;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import java.util.Collection;
import java.util.Set;

public class MavenPackagingConverter extends MavenProjectConstantListConverter {
    public MavenPackagingConverter() {
        super(false);
    }

    @Override
    protected Collection<String> getValues(@NotNull ConvertContext context, @NotNull MavenProject project) {
        return Set.of("jar", "pom", "maven-plugin", "war", "ear");
    }
}