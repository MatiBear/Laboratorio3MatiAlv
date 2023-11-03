package com.example.laboratorio3matialv

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastTime: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private val shakeThreshold = 10
    private var shakeMessage: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Find the TextView by its ID
        shakeMessage = findViewById(R.id.shakeMessage)

        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val currentTime = System.currentTimeMillis()
            val diffTime = currentTime - lastTime

            if (diffTime > 100) { // Minimum time interval between shakes
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val acceleration = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH)
                val actualAcceleration = Math.sqrt(acceleration.toDouble()) * SensorManager.GRAVITY_EARTH

                if (actualAcceleration > shakeThreshold) {
                    showMessage("PASO EL UMBRAL")
                } else {
                    hideMessage()
                }

                lastTime = currentTime
                lastX = x
                lastY = y
                lastZ = z
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // You can handle changes in sensor accuracy if needed.
        }
    }

    private fun showMessage(message: String) {
        shakeMessage?.text = message
        shakeMessage?.visibility = View.VISIBLE

        // Hide the message after a delay (e.g., 2 seconds)
        Handler().postDelayed({ hideMessage() }, 2000)
    }

    private fun hideMessage() {
        shakeMessage?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accelerometerListener)
    }

    fun openGame(view: View) {
        val intent = Intent(this, GameActivity::class.java) // Replace with the name of your new interface activity
        startActivity(intent)
    }
}