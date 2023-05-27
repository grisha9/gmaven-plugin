package ru.rzn.gmyasoedov.gmaven.project.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.ActivationProfile
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.SimpleProfile
import ru.rzn.gmyasoedov.gmaven.project.profile.ProfileState
import ru.rzn.gmyasoedov.gmaven.project.profile.ProjectProfilesStateService

class ChangeProfileStateAction : ExternalSystemNodeAction<ProfileData>(ProfileData::class.java) {

    override fun perform(
        project: Project,
        projectSystemId: ProjectSystemId,
        externalData: ProfileData,
        e: AnActionEvent
    ) {
        val profilesStateService = ProjectProfilesStateService.getInstance(project)
        val profileState = profilesStateService.state.mapping.get(externalData.stateKey)
            ?: ProfileData.defaultState(externalData)
        nextState(project, profileState, externalData)
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, projectSystemId)
    }

    private fun nextState(project: Project, profileState: ProfileState, profileData: ProfileData) {
        var simpleProfile = profileState.simpleProfile
        var activationProfile = profileState.activationProfile
        if (simpleProfile != null) {
            simpleProfile = SimpleProfile.values()[(simpleProfile.ordinal + 1) % SimpleProfile.values().size]
        }
        if (activationProfile != null) {
            activationProfile =
                ActivationProfile.values()[(activationProfile.ordinal + 1) % ActivationProfile.values().size]
        }
        val state = ProfileState()
        state.simpleProfile = simpleProfile
        state.activationProfile = activationProfile
        ProjectProfilesStateService.getInstance(project).state.mapping.put(profileData.stateKey, state)
    }
}