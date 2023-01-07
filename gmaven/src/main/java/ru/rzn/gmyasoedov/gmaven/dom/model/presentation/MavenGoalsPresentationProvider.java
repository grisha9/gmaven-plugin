/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package ru.rzn.gmyasoedov.gmaven.dom.model.presentation;

import com.intellij.ide.presentation.PresentationProvider;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomGoal;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomGoals;

/**
 *
 */
public class MavenGoalsPresentationProvider extends PresentationProvider<MavenDomGoals> {

  @Nullable
  @Override
  public String getName(MavenDomGoals mavenDomGoals) {
    StringBuilder res = new StringBuilder("Goals");

    boolean hasGoals = false;

    for (MavenDomGoal mavenDomGoal : mavenDomGoals.getGoals()) {
      String goal = mavenDomGoal.getStringValue();

      if (!StringUtil.isEmptyOrSpaces(goal)) {
        if (hasGoals) {
          res.append(", ");
        }
        else {
          res.append(" (");
          hasGoals = true;
        }

        res.append(goal);
      }
    }

    if (hasGoals) {
      res.append(")");
    }

    return res.toString();
  }
}
