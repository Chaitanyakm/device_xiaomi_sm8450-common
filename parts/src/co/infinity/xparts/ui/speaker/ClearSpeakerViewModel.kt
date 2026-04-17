/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.speaker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.infinity.xparts.data.ClearSpeakerUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClearSpeakerViewModel(
    private val utils: ClearSpeakerUtils
) : ViewModel() {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var cleanupJob: Job? = null

    fun toggleCleaning(enable: Boolean) {
        if (enable) {
            startCleaning()
        } else {
            stopCleaning()
        }
    }

    private fun startCleaning() {
        if (utils.startPlaying()) {
            _isPlaying.value = true
            
            cleanupJob?.cancel()
            cleanupJob = viewModelScope.launch {
                delay(30_000)
                stopCleaning()
            }
        } else {
            _isPlaying.value = false
        }
    }

    private fun stopCleaning() {
        cleanupJob?.cancel()
        utils.stopPlaying()
        _isPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopCleaning()
    }
}

class ClearSpeakerViewModelFactory(
    private val utils: ClearSpeakerUtils
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClearSpeakerViewModel::class.java)) {
            return ClearSpeakerViewModel(utils) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
