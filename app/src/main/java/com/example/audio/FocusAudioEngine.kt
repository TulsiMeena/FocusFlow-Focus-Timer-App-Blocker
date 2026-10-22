package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

/**
 * Offline real-time procedural soundscape generator using Android AudioTrack.
 * Generates continuous soothing focus ambient audio without requiring external audio files.
 */
class FocusAudioEngine {

  private val scope = CoroutineScope(Dispatchers.Default)
  private var audioJob: Job? = null
  private var audioTrack: AudioTrack? = null

  private val _currentSoundscape = MutableStateFlow(SoundscapeType.NONE)
  val currentSoundscape: StateFlow<SoundscapeType> = _currentSoundscape.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _volume = MutableStateFlow(0.7f)
  val volume: StateFlow<Float> = _volume.asStateFlow()

  fun setVolume(vol: Float) {
    val clamped = vol.coerceIn(0.0f, 1.0f)
    _volume.value = clamped
    try {
      audioTrack?.setVolume(clamped)
    } catch (_: Exception) {}
  }

  fun togglePlayback() {
    if (_isPlaying.value) {
      stop()
    } else {
      if (_currentSoundscape.value == SoundscapeType.NONE) {
        selectSoundscape(SoundscapeType.RAIN)
      } else {
        startCurrent()
      }
    }
  }

  fun selectSoundscape(soundscape: SoundscapeType) {
    _currentSoundscape.value = soundscape
    if (soundscape == SoundscapeType.NONE) {
      stop()
    } else {
      stopInternal()
      startCurrent()
    }
  }

  private fun startCurrent() {
    val sound = _currentSoundscape.value
    if (sound == SoundscapeType.NONE) return

    _isPlaying.value = true
    audioJob = scope.launch {
      runAudioLoop(sound)
    }
  }

  fun stop() {
    _isPlaying.value = false
    stopInternal()
  }

  private fun stopInternal() {
    audioJob?.cancel()
    audioJob = null
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (e: Exception) {
      Log.w("FocusAudioEngine", "Error stopping AudioTrack: ${e.message}")
    } finally {
      audioTrack = null
    }
  }

  private fun runAudioLoop(sound: SoundscapeType) {
    val sampleRate = 22050
    val bufferSize = AudioTrack.getMinBufferSize(
      sampleRate,
      AudioFormat.CHANNEL_OUT_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    try {
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrack = track
      track.setVolume(_volume.value)
      track.play()

      val buffer = ShortArray(bufferSize / 2)
      val random = Random()

      // Synthesis state variables
      var brownNoiseVal = 0.0
      var wavePhase = 0.0
      var binauralPhase = 0.0
      var rainFilter = 0.0
      var rainEnvelope = 0.0
      var tickCounter = 0

      while (scope.isActive && _isPlaying.value) {
        val soundType = _currentSoundscape.value
        if (soundType == SoundscapeType.NONE) break

        for (i in buffer.indices) {
          val sample: Double = when (soundType) {
            SoundscapeType.WHITE_NOISE -> {
              (random.nextDouble() * 2.0 - 1.0) * 0.28
            }
            SoundscapeType.BROWN_NOISE -> {
              val white = random.nextDouble() * 2.0 - 1.0
              brownNoiseVal = (brownNoiseVal + (0.03 * white)) / 1.03
              (brownNoiseVal * 3.5).coerceIn(-1.0, 1.0) * 0.45
            }
            SoundscapeType.RAIN -> {
              val white = random.nextDouble() * 2.0 - 1.0
              rainFilter = rainFilter * 0.92 + white * 0.08
              // Occasional droplet burst
              if (random.nextInt(350) == 0) {
                rainEnvelope = 0.45
              } else {
                rainEnvelope *= 0.998
              }
              (rainFilter * 0.7 + (white * rainEnvelope * 0.35)) * 0.45
            }
            SoundscapeType.OCEAN -> {
              wavePhase += (2.0 * Math.PI * 0.12) / sampleRate
              val waveSurge = (sin(wavePhase) * 0.5 + 0.5)
              val white = random.nextDouble() * 2.0 - 1.0
              brownNoiseVal = (brownNoiseVal + (0.04 * white)) / 1.04
              (brownNoiseVal * 3.0 * (0.2 + 0.8 * waveSurge)) * 0.4
            }
            SoundscapeType.BINAURAL -> {
              // 432 Hz focus tone with subtle 8 Hz alpha brainwave modulation
              binauralPhase += (2.0 * Math.PI * 432.0) / sampleRate
              wavePhase += (2.0 * Math.PI * 8.0) / sampleRate
              val alphaMod = 0.8 + 0.2 * sin(wavePhase)
              sin(binauralPhase) * alphaMod * 0.25
            }
            SoundscapeType.FOREST -> {
              wavePhase += (2.0 * Math.PI * 0.2) / sampleRate
              val gust = (sin(wavePhase) * 0.4 + 0.6)
              val white = random.nextDouble() * 2.0 - 1.0
              rainFilter = rainFilter * 0.85 + white * 0.15
              (rainFilter * gust) * 0.35
            }
            SoundscapeType.CAFE -> {
              val white = random.nextDouble() * 2.0 - 1.0
              brownNoiseVal = (brownNoiseVal + (0.02 * white)) / 1.02
              (brownNoiseVal * 2.2) * 0.32
            }
            SoundscapeType.NONE -> 0.0
          }

          buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(
            Short.MIN_VALUE.toInt(),
            Short.MAX_VALUE.toInt()
          ).toShort()
        }

        track.write(buffer, 0, buffer.size)
      }
    } catch (e: Exception) {
      Log.e("FocusAudioEngine", "AudioTrack loop failed", e)
    } finally {
      stopInternal()
    }
  }

  companion object {
    @Volatile
    private var instance: FocusAudioEngine? = null

    fun get(): FocusAudioEngine {
      return instance ?: synchronized(this) {
        val newInst = FocusAudioEngine()
        instance = newInst
        newInst
      }
    }
  }
}
