@file:JvmName("MavenSdkUtils")

package ru.rzn.gmyasoedov.gmaven.utils

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.util.lang.JavaVersion
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings

private data class MavenJvmProviderId(val projectSettings: MavenProjectSettings) : SdkLookupProvider.Id

fun setupMavenJvm(project: Project, projectSettings: MavenProjectSettings) {

    SdkLookupProvider.getInstance(project, MavenJvmProviderId(projectSettings))
        .newLookupBuilder()
        .withVersionFilter { JavaVersion.tryParse(it) != null }
        .withSdkType(ExternalSystemJdkUtil.getJavaSdkType())
        .withSdkHomeFilter { ExternalSystemJdkUtil.isValidJdk(it) }
        .onSdkNameResolved { sdk ->
            /* We have two types of sdk resolving:
             *  1. Download sdk manually
             *    a. by download action from SdkComboBox
             *    b. by sdk downloader
             *    c. by action that detects incorrect project sdk
             *  2. Lookup sdk (search in fs, download and etc)
             *    a. search in fs, search in sdk table and etc
             *    b. download
             *
             * All download actions generates fake (invalid) sdk and puts it to jdk table.
             * This code allows to avoid some irregular conflicts
             * For example: strange duplications in SdkComboBox or unexpected modifications of gradleJvm
             */
            val fakeSdk = sdk?.let(::findRegisteredSdk)
            /*if (fakeSdk != null && projectSettings.jdkPath == null) {
                projectSettings.jdkPath = fakeSdk.name
            }*/
        }
        .onSdkResolved { sdk ->
           /* if (projectSettings.jdkPath == null) {
                projectSettings.jdkPath = sdk?.name
            }*/
        }
        .executeLookup()
}

private fun findRegisteredSdk(sdk: Sdk): Sdk? = runReadAction {
    val projectJdkTable = ProjectJdkTable.getInstance()
    projectJdkTable.findJdk(sdk.name, sdk.sdkType.name)
}
