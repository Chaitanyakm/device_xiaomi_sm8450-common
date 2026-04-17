/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.utils

import android.util.Log

private const val MAIN_TAG = "XParts"

object Logging {
    fun d(tag: String, msg: String) {
        if (Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
            Log.d("$MAIN_TAG-$tag", msg)
        }
    }

    fun i(tag: String, msg: String) {
        Log.i("$MAIN_TAG-$tag", msg)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.e("$MAIN_TAG-$tag", msg, tr)
        } else {
            Log.e("$MAIN_TAG-$tag", msg)
        }
    }
}
