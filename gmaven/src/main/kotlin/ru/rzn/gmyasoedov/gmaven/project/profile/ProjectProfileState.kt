package ru.rzn.gmyasoedov.gmaven.project.profile

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Transient
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.ActivationProfile
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.ActivationProfile.ACTIVE
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.ActivationProfile.INDETERMINATE
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.SimpleProfile
import ru.rzn.gmyasoedov.gmaven.settings.ProfileExecution
import java.util.concurrent.ConcurrentHashMap


@State(name = "ProjectProfilesState", storages = [Storage("gmaven.xml")])
class ProjectProfilesStateService : PersistentStateComponent<ProjectProfilesStateService.State> {
    private var myState = State()

    class State {
        @JvmField
        val mapping = ConcurrentHashMap<String, ProfileState>()
    }

    override fun getState() = myState

    override fun loadState(state: State) {
        myState.mapping.putAll(state.mapping)
    }

    fun getProfileSate(profileData: ProfileData): ProfileState {
        val state = state.mapping[profileData.stateKey] ?: defaultState(profileData)
        return if (state.hasActivation() != profileData.isHasActivation) defaultState(profileData) else state
    }

    fun getProfileExecution(profileData: ProfileData): ProfileExecution? {
        val profileState = state.mapping[profileData.stateKey] ?: return null

        val execution = ProfileExecution(profileData.name)
        if (profileState.simpleProfile != null && profileState.simpleProfile == SimpleProfile.ACTIVE) {
            execution.isEnabled = true;
            return execution
        }
        if (profileState.activationProfile != null && profileState.activationProfile != INDETERMINATE) {
            execution.isEnabled = profileState.activationProfile == ACTIVE;
            return execution
        }
        return null
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ProjectProfilesStateService {
            return project.getService(ProjectProfilesStateService::class.java)
        }

        @JvmStatic
        fun getState(project: Project?, profileData: ProfileData): ProfileState {
            project ?: return defaultState(profileData)
            return getInstance(project).getProfileSate(profileData)
        }

        @JvmStatic
        private fun defaultState(data: ProfileData): ProfileState {
            val state = ProfileState()
            if (data.isHasActivation) {
                state.activationProfile = INDETERMINATE
            } else {
                state.simpleProfile = SimpleProfile.INACTIVE
            }
            return state
        }
    }
}

class ProfileState : Cloneable {
    var simpleProfile: SimpleProfile? = null
    var activationProfile: ActivationProfile? = null

    @Transient
    fun hasActivation() = activationProfile != null
}