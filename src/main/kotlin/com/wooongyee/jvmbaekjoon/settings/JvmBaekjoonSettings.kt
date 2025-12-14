package com.wooongyee.jvmbaekjoon.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "JvmBaekjoonSettings",
    storages = [Storage("JvmBaekjoonSettings.xml")]
)
class JvmBaekjoonSettings : PersistentStateComponent<JvmBaekjoonSettings.State> {

    data class State(
        var jdkPath: String = "",
        var kotlinCompilerPath: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): JvmBaekjoonSettings {
            return ApplicationManager.getApplication().getService(JvmBaekjoonSettings::class.java)
        }
    }
}
