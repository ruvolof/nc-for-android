package com.werebug.androidnetcat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetcatSessionViewModel : ViewModel() {

    data class SessionState(val output: String = "", val running: Boolean = true)

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var netcatProcess: NetcatProcess? = null

    private val processListener = object : NetcatProcess.Listener {
        override fun onOutput(message: String) {
            _sessionState.update { it.copy(output = it.output + message) }
        }

        override fun onExit(exitValue: Int) {
            _sessionState.update { it.copy(running = false) }
        }
    }

    // Called by every activity instance, including the ones a configuration
    // change recreates: only the first one gets to start a process, the rest
    // re-attach to the session already running here.
    fun startIfNotRunning(ncArgv: List<String>) {
        if (netcatProcess != null) {
            return
        }
        val process = NetcatProcess(ncArgv, processListener)
        netcatProcess = process
        viewModelScope.launch(Dispatchers.IO) { process.run() }
    }

    fun send(message: String) {
        netcatProcess?.addToSendQueue(message)
    }

    override fun onCleared() {
        netcatProcess?.halt()
        super.onCleared()
    }
}
