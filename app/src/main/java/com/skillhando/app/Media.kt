package com.skillhando.app

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class PracticeMedia(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var output: File? = null

    fun start(file: File) {
        check(recorder == null)
        stopPlayback()
        @Suppress("DEPRECATION")
        val engine = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        try {
            engine.setAudioSource(MediaRecorder.AudioSource.MIC)
            engine.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            engine.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            engine.setMaxDuration(10 * 60_000)
            engine.setOutputFile(file.path)
            engine.prepare()
            engine.start()
            recorder = engine
            output = file
        } catch (failure: Exception) {
            engine.release()
            file.delete()
            throw failure
        }
    }

    fun pause() { recorder?.pause() }
    fun resume() { recorder?.resume() }

    fun stop(): File? {
        val engine = recorder ?: return null
        val file = output
        recorder = null
        output = null
        return try { engine.stop(); file?.takeIf { it.length() > 0 } }
        catch (_: RuntimeException) { file?.delete(); null }
        finally { engine.release() }
    }

    fun play(file: File, ended: () -> Unit) {
        check(recorder == null)
        stopPlayback()
        val engine = MediaPlayer()
        try {
            engine.setDataSource(file.absolutePath)
            engine.setOnCompletionListener { stopPlayback(); ended() }
            engine.prepare()
            player = engine
            engine.start()
        } catch (failure: Exception) {
            engine.release()
            player = null
            throw failure
        }
    }

    fun stopPlayback() { player?.release(); player = null }
    fun close() { stop()?.delete(); stopPlayback() }
}
