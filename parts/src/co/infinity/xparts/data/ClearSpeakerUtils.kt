/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.data

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import co.infinity.xparts.R
import co.infinity.xparts.utils.Logging
import java.io.IOException

class ClearSpeakerUtils private constructor(private val context: Context) {

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null

    fun startPlaying(): Boolean {
        // Set vendor parameter to enable cleaning mode
        audioManager.setParameters("status_earpiece_clean=on")
        
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            isLooping = true
        }

        return try {
            val file = context.resources.openRawResourceFd(R.raw.clear_speaker_sound)
            try {
                mediaPlayer?.setDataSource(
                    file.fileDescriptor,
                    file.startOffset,
                    file.length
                )
            } finally {
                file.close()
            }

            mediaPlayer?.apply {
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            true
        } catch (ioe: IOException) {
            Logging.e(TAG, "Failed to play speaker clean sound!", ioe)
            stopPlaying() // Cleanup if failed
            false
        }
    }

    fun stopPlaying() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                }
                mediaPlayer!!.reset()
                mediaPlayer!!.release()
            } catch (e: Exception) {
                Logging.e(TAG, "Error stopping media player", e)
            }
            mediaPlayer = null
        }
        
        // Reset vendor parameter
        audioManager.setParameters("status_earpiece_clean=off")
    }

    companion object {
        private const val TAG = "ClearSpeakerUtils"

        @Volatile
        private var instance: ClearSpeakerUtils? = null

        fun getInstance(context: Context): ClearSpeakerUtils {
            return instance ?: synchronized(this) {
                instance ?: ClearSpeakerUtils(context.applicationContext).also { instance = it }
            }
        }
    }
}
