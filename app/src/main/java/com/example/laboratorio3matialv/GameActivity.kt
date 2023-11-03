package com.example.laboratorio3matialv

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var instructionsText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var currentScoreText: TextView
    private var highScore = 0
    private var currentScore = 0
    private var actualAcceleration = 0f
    private val shakeThreshold = 10
    private var lastTime: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var lastShakeTime: Long = 0

    private lateinit var mediaPlayer : MediaPlayer
    private var isTaskCompleted = false
    private var shakenAlready = false
    private var taskSuccess = false
    private var taskAttempted = false
    private val instructions = arrayOf("Tap", "Shake", "Slide") // List of instructions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_activity)

        instructionsText = findViewById(R.id.instructionsText)
        highScoreText = findViewById(R.id.highScoreText)
        currentScoreText = findViewById(R.id.currentScoreText)

        mediaPlayer = MediaPlayer.create(this, R.raw.gamewin)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        gestureDetector = GestureDetector(this, GestureListener())

        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        // Load the high score from SharedPreferences
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        highScore = prefs.getInt("HighScore", 0)
        updateHighScoreText()

        // Start the game loop
        startGameLoop()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {

            if (!taskAttempted) {
                if (instructionsText.text == "Fling") {
                    // Correct response to the "Slide" instruction
                    // Increase the score and update UI
                    updateCurrentScore()
                } else if (instructionsText.text != "Completed!") {
                    // Incorrect response to the "Slide" instruction
                    // End the game
                    endGame()
                }
            }
            return super.onFling(e1, e2!!, velocityX, velocityY)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {

            if (!taskAttempted) {
                if (instructionsText.text == "Tap") {
                    // Correct response to the "Tap" instruction
                    // Increase the score and update UI
                    updateCurrentScore()
                } else if (instructionsText.text != "Completed!") {
                    // Incorrect response to the "Tap" instruction
                    // End the game
                    endGame()
                }
            }
            return super.onSingleTapUp(e)
        }

    }

    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            detectShake(x, y, z)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // This is needed for it to work
        }
    }

    private fun detectShake(x: Float, y: Float, z: Float) {
        val currentTime = System.currentTimeMillis()
        val diffTime = currentTime - lastTime

        if (diffTime > 100) { // Minimum time interval between shakes
            val deltaX = x - lastX
            val deltaY = y - lastY
            val deltaZ = z - lastZ

            val acceleration = (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH)
            actualAcceleration = (Math.sqrt(acceleration.toDouble()) * SensorManager.GRAVITY_EARTH).toFloat()

            if (actualAcceleration > shakeThreshold) {

                if (!shakenAlready && !taskAttempted) { // 1000 = 1 second
                    if (instructionsText.text == "Shake") {
                        // Correct response to the "Shake" instruction
                        updateCurrentScore()
                    } else if (instructionsText.text != "Completed!") {
                        // Incorrect response to the "Shake" instruction
                        // End the game
                        endGame()
                    }
                    shakenAlready = true
                }
            }

            lastTime = currentTime
            lastX = x
            lastY = y
            lastZ = z
        }
    }

    private fun startGameLoop() {
        val handler = Handler(Looper.getMainLooper())
        val instructions = listOf("Tap", "Fling", "Shake")

        val gameRunnable = object : Runnable {
            override fun run() {
                val randomIndex = (0 until instructions.size).random()
                val currentInstruction = instructions[randomIndex]
                instructionsText.text = currentInstruction

                // Start a timer for 3 seconds
                val timerHandler = Handler(Looper.getMainLooper())
                timerHandler.postDelayed({

                    // After a brief delay, check if the task is completed
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isTaskCompleted && instructionsText.text != "Game Over") {
                            // Task was completed, continue the game
                            taskAttempted = false
                            taskSuccess = false
                            shakenAlready = false
                            startGameLoop() // Start the next task
                        } else if (instructionsText.text != "Game Over" && instructionsText.text != "Completed!") {
                            // Task was not completed, end the game
                            endGame()
                        }
                    }, 1000) // 1000 milliseconds (1 second)
                }, 3000) // 3000 milliseconds (3 seconds)
            }
        }

        handler.post(gameRunnable)
    }

    private fun getRandomInstruction(): String {
        val randomIndex = Random.nextInt(instructions.size)
        return instructions[randomIndex]
    }

    private fun updateHighScoreText() {
        highScoreText.text = "High Score: $highScore"
    }

    private fun updateCurrentScore() {
        currentScore++
        currentScoreText.text = "Score: $currentScore"
        taskAttempted = true
        taskSuccess = true
        isTaskCompleted = true
        instructionsText.text = "Completed!"
        playSound(R.raw.gamewin)

        updateHighScore()
    }

    private fun endGame() {
        instructionsText.text = "Game Over"
        updateHighScore()
        playSound(R.raw.gameover)

        taskAttempted = true
        taskSuccess = false

        Handler(Looper.getMainLooper()).postDelayed({
            finishGame()
        }, 3000) // 3000 milliseconds (3 seconds)
    }

    private fun updateHighScore() {
        if (currentScore > highScore) {
            highScore = currentScore
            val prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt("HighScore", highScore)
            editor.apply()
        }
        updateHighScoreText()
    }

    private fun finishGame() {
        val intent = Intent()
        intent.putExtra("HighScore", highScore)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun playSound(soundResource: Int) {
        mediaPlayer = MediaPlayer.create(this, soundResource)
        mediaPlayer.start()
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    override fun onStop() {
        super.onStop()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }


}