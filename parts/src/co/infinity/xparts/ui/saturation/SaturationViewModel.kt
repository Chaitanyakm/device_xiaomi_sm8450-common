/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.saturation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.infinity.xparts.data.SaturationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SaturationViewModel(private val context: Context) : ViewModel() {

    private val utils = SaturationUtils.getInstance(context)
    
    // Stores integer: 100 = 1.0, 200 = 2.0
    private val _saturationValue = MutableStateFlow(100)
    val saturationValue: StateFlow<Int> = _saturationValue.asStateFlow()

    init {
        _saturationValue.value = utils.getSavedSaturation()
    }

    fun setSaturation(value: Int) {
        _saturationValue.value = value
        utils.setSaturation(value)
    }
}

class SaturationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaturationViewModel::class.java)) {
            return SaturationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
