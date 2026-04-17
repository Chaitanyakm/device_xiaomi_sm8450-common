/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.gestures

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.infinity.xparts.data.FpGestureUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FpGestureViewModel(private val context: Context) : ViewModel() {

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _selectedAction = MutableStateFlow(1)
    val selectedAction: StateFlow<Int> = _selectedAction.asStateFlow()

    init {
        _isEnabled.value = FpGestureUtils.isEnabled(context)
        _selectedAction.value = FpGestureUtils.getAction(context)
    }

    fun toggleEnabled(enable: Boolean) {
        FpGestureUtils.setEnabled(context, enable)
        _isEnabled.value = enable
    }

    fun setAction(action: Int) {
        FpGestureUtils.setAction(context, action)
        _selectedAction.value = action
    }
}

class FpGestureViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FpGestureViewModel::class.java)) {
            return FpGestureViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
