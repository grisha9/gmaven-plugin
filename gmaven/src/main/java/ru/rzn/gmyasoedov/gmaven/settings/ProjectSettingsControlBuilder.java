package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.ui.configuration.SdkListItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.JAVA_HOME;
import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK;
import static com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil.createUniqueSdkName;

public class ProjectSettingsControlBuilder {

    public static void deduplicateSdkNames(@NotNull ProjectSdksModel projectSdksModel) {
        Set<String> processedNames = new HashSet<>();
        Collection<Sdk> editableSdks = projectSdksModel.getProjectSdks().values();
        for (Sdk sdk : editableSdks) {
            if (processedNames.contains(sdk.getName())) {
                SdkModificator sdkModificator = sdk.getSdkModificator();
                String name = createUniqueSdkName(sdk.getName(), editableSdks);
                sdkModificator.setName(name);
                sdkModificator.commitChanges();
            }
            processedNames.add(sdk.getName());
        }
    }

    public static String getJdkName(@Nullable SdkListItem item) {
        if (item instanceof SdkListItem.ProjectSdkItem) {
            return USE_PROJECT_JDK;
        } else if (item instanceof SdkListItem.SdkItem) {
            return ((SdkListItem.SdkItem) item).sdk.getName();
        } else if (item instanceof SdkListItem.InvalidSdkItem) {
            return ((SdkListItem.InvalidSdkItem) item).sdkName;
        } else if (item instanceof SdkListItem.SdkReferenceItem) {
            return getJdkReferenceName((SdkListItem.SdkReferenceItem) item);
        } else {
            return null;
        }
    }

    private static String getJdkReferenceName(SdkListItem.SdkReferenceItem item) {
        if (JAVA_HOME.equals(item.name)) {
            return JAVA_HOME;
        } else {
            return item.name;
        }
    }

    public enum OutputLevelType {
        DEFAULT, QUITE, DEBUG, ERROR
    }

    public enum SnapshotUpdateType {
        DEFAULT, NEVER, FORCE
    }
}
