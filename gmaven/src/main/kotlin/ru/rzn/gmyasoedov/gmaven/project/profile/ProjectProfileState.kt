package ru.rzn.gmyasoedov.gmaven.project.profile

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Transient
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.ActivationProfile
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.ProfileData.SimpleProfile
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

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ProjectProfilesStateService {
            return project.getService(ProjectProfilesStateService::class.java)
        }
    }
}

class ProfileState : Cloneable {
    var simpleProfile: SimpleProfile? = null
    var activationProfile: ActivationProfile? = null

    @Transient
    fun hasActivation() = activationProfile != null
}