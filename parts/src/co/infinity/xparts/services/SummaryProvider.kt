/*
 * Copyright (C) 2019 The Android Open Source Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.services

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import co.infinity.xparts.R
import co.infinity.xparts.data.FpGestureUtils

/** Provide preference summary for injected items. */
class SummaryProvider : ContentProvider() {

    override fun call(method: String, uri: String?, extras: Bundle?): Bundle? {
        val bundle = Bundle()
        val summary = when (method) {
            KEY_FP_DOUBLE_TAP -> getFpDoubleTapSummary()
            else -> null
        }
        
        if (summary != null) {
            bundle.putString(META_DATA_PREFERENCE_SUMMARY, summary)
        }
        return bundle
    }

    private fun getFpDoubleTapSummary(): String {
        val context = context ?: return ""
        
        if (!FpGestureUtils.isEnabled(context)) {
            return context.getString(R.string.fp_double_tap_summary_off)
        }

        val action = FpGestureUtils.getAction(context)
        val actionName = getActionName(action)
        
        return context.getString(R.string.fp_double_tap_summary_on, actionName)
    }

    private fun getActionName(action: Int): String {
        val context = context ?: return ""
        
        // Map the action ID to the string resource directly
        val resId = when (action) {
            1 -> R.string.action_screenshot
            2 -> R.string.action_assistant
            3 -> R.string.action_play_pause
            4 -> R.string.action_notifications
            5 -> R.string.action_camera
            6 -> R.string.action_flashlight
            7 -> R.string.action_mute
            8 -> R.string.action_vibrate
            9 -> R.string.action_volume
            10 -> R.string.action_sleep
            else -> return ""
        }
        return context.getString(resId)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    companion object {
        private const val KEY_FP_DOUBLE_TAP = "fp_double_tap"
        private const val META_DATA_PREFERENCE_SUMMARY = "com.android.settings.summary"
    }
}
