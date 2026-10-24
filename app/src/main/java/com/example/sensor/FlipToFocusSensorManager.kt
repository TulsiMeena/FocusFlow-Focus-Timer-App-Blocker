package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hardware Accelerometer / Gravity sensor controller for Feature #4: Flip-to-Focus.
 * Detects when the user places the phone Face-Down on a flat surface to auto-start/maintain
 * deep work, and warns via haptics & UI banner if the phone is picked up mid-session.
 */
class FlipToFocusSensorManager(
  private val context: Context,
  private val onFaceDownConfirmed: () -> Unit,
  private val onPhonePickedUp: () -> Unit
) : SensorEventListener {

  private val sensorManager: SensorManager? =
    context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val accelerometer: Sensor? =
    sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  private val _isSensorAvailable = MutableStateFlow(accelerometer != null)
  val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

  private val _isFaceDown = MutableStateFlow(false)
  val isFaceDown: StateFlow<Boolean> = _isFaceDown.asStateFlow()

  private val _zGravity = MutableStateFlow(9.8f)
  val zGravity: StateFlow<Float> = _zGravity.asStateFlow()

  private val _orientationLabel = MutableStateFlow("Face-Up / In Hand")
  val orientationLabel: StateFlow<String> = _orientationLabel.asStateFlow()

  private val _pickupWarningMessage = MutableStateFlow<String?>(null)
  val pickupWarningMessage: StateFlow<String?> = _pickupWarningMessage.asStateFlow()

  private var isListening = false
  private var faceDownSinceMillis = 0L
  private var hasTriggeredForCurrentFlip = false
  private var wasFaceDownInSession = false

  fun startListening() {
    if (isListening || accelerometer == null || sensorManager == null) return
    try {
      sensorManager.registerListener(
        this,
        accelerometer,
        SensorManager.SENSOR_DELAY_UI
      )
      isListening = true
    } catch (_: Exception) {
    }
  }

  fun stopListening() {
    if (!isListening || sensorManager == null) return
    try {
      sensorManager.unregisterListener(this)
    } catch (_: Exception) {
    }
    isListening = false
    faceDownSinceMillis = 0L
    hasTriggeredForCurrentFlip = false
  }

  fun dismissPickupWarning() {
    _pickupWarningMessage.value = null
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
    val x = event.values[0]
    val y = event.values[1]
    val z = event.values[2]

    // Low-pass filter for smooth telemetry
    val smoothedZ = (_zGravity.value * 0.75f) + (z * 0.25f)
    _zGravity.value = ((smoothedZ * 10f).roundToInt() / 10f)

    val currentlyFaceDown = (smoothedZ < -7.5f && abs(x) < 4.2f && abs(y) < 4.2f)
    val now = System.currentTimeMillis()

    if (currentlyFaceDown) {
      if (!_isFaceDown.value) {
        _isFaceDown.value = true
        _orientationLabel.value = "Face-Down Locked (Table)"
        _pickupWarningMessage.value = null
        faceDownSinceMillis = now
      } else if (!hasTriggeredForCurrentFlip && faceDownSinceMillis > 0L && (now - faceDownSinceMillis) >= 1_400L) {
        hasTriggeredForCurrentFlip = true
        wasFaceDownInSession = true
        vibrateConfirm()
        onFaceDownConfirmed()
      }
    } else {
      val clearlyPickedUp = smoothedZ > -3.5f || abs(x) > 5.0f || abs(y) > 5.0f
      if (clearlyPickedUp && _isFaceDown.value) {
        _isFaceDown.value = false
        faceDownSinceMillis = 0L
        hasTriggeredForCurrentFlip = false
        _orientationLabel.value = if (smoothedZ > 6.5f) "Face-Up (Flat)" else "In Hand / Tilted"

        if (wasFaceDownInSession) {
          wasFaceDownInSession = false
          _pickupWarningMessage.value =
            "Phone picked up during Flip-to-Focus! Place your phone face-down to stay in the zone."
          vibrateWarning()
          onPhonePickedUp()
        }
      } else if (!_isFaceDown.value) {
        _orientationLabel.value = if (smoothedZ > 6.5f) "Face-Up (Flat)" else "In Hand / Tilted"
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

  private fun vibrateConfirm() {
    try {
      val vibrator = getVibrator() ?: return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(90L, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(90L)
      }
    } catch (_: Exception) {
    }
  }

  private fun vibrateWarning() {
    try {
      val vibrator = getVibrator() ?: return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
          VibrationEffect.createWaveform(longArrayOf(0L, 120L, 80L, 160L), -1)
        )
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(220L)
      }
    } catch (_: Exception) {
    }
  }

  private fun getVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vm?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }
}
