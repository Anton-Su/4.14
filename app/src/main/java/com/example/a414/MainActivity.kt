package com.example.a414

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a414.ui.theme._414Theme
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var azimuthState = mutableFloatStateOf(0f)
    private var sensorAvailable = mutableStateOf(true)
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val alpha = 0.97f
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                    geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                    geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
                }
            }
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val azimuthRad = orientation[0]
                val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                azimuthState.floatValue = (azimuthDeg + 360f) % 360f
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (accelerometerSensor == null || magnetometerSensor == null) {
            sensorAvailable.value = false
        }
        setContent {
            _414Theme {
                Greeting(
                    azimuth = azimuthState.floatValue,
                    sensorAvailable = sensorAvailable.value
                )
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (sensorAvailable.value) {
            accelerometerSensor?.let {
                sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            }
            magnetometerSensor?.let {
                sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }
}

@Composable
fun Greeting(azimuth: Float, sensorAvailable: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        if (!sensorAvailable) {
            Text(
                text = "Устройство не поддерживает датчик ориентации",
                color = Color.Red,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
            return@Box
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Компас",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
            ) {
                val sizeDp = maxWidth

                // Анимированный угол поворота стрелки
                val animatedRotation by animateFloatAsState(
                    targetValue = -azimuth,
                    animationSpec = tween(durationMillis = 300),
                    label = "compass_rotation"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCompassDisk(this)
                    drawCompassArrow(this, animatedRotation)
                    drawNorthLabel(this)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Азимут: ${azimuth.toInt()}°",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun drawCompassDisk(scope: DrawScope) {
    with(scope) {
        // Внешний круг
        drawCircle(
            color = Color(0xFF2A2A2A),
            radius = size.minDimension / 2f
        )
        // Граница круга
        drawCircle(
            color = Color(0xFF555555),
            radius = size.minDimension / 2f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )
        // Метки сторон света
        val tickRadius = size.minDimension / 2f - 16.dp.toPx()
        val directions = listOf(0f to "N", 90f to "E", 180f to "S", 270f to "W")
        directions.forEach { (angleDeg, _) ->
            val angleRad = Math.toRadians(angleDeg.toDouble() - 90.0)
            val x = center.x + tickRadius * cos(angleRad).toFloat()
            val y = center.y + tickRadius * sin(angleRad).toFloat()
            val tickColor = if (angleDeg == 0f) Color(0xFFE53935) else Color(0xFF888888)
            drawCircle(
                color = tickColor,
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
        }
        for (i in 0 until 12) {
            if (i % 3 == 0)
                continue
            val angleDeg = i * 30f
            val angleRad = Math.toRadians(angleDeg.toDouble() - 90.0)
            val outerR = size.minDimension / 2f - 6.dp.toPx()
            val innerR = size.minDimension / 2f - 18.dp.toPx()
            val startX = center.x + innerR * cos(angleRad).toFloat()
            val startY = center.y + innerR * sin(angleRad).toFloat()
            val endX = center.x + outerR * cos(angleRad).toFloat()
            val endY = center.y + outerR * sin(angleRad).toFloat()
            drawLine(
                color = Color(0xFF555555),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun drawCompassArrow(scope: DrawScope, rotationDeg: Float) {
    with(scope) {
        rotate(degrees = rotationDeg, pivot = center) {
            val arrowLength = size.minDimension / 2f * 0.75f
            val arrowWidth = 14.dp.toPx()
            // Красная часть (север — вверх)
            val northPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(center.x, center.y - arrowLength)
                lineTo(center.x - arrowWidth / 2, center.y)
                lineTo(center.x + arrowWidth / 2, center.y)
                close()
            }
            drawPath(path = northPath, color = Color(0xFFE53935))
            // Серая часть (юг — вниз)
            val southPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(center.x, center.y + arrowLength)
                lineTo(center.x - arrowWidth / 2, center.y)
                lineTo(center.x + arrowWidth / 2, center.y)
                close()
            }
            drawPath(path = southPath, color = Color(0xFF757575))

            // Центральный круг
            drawCircle(
                color = Color(0xFF333333),
                radius = 10.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF888888),
                radius = 10.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

private fun drawNorthLabel(scope: DrawScope) {
    with(scope) {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        val y = center.y - size.minDimension / 2f * 0.75f + 16.dp.toPx()
        drawContext.canvas.nativeCanvas.drawText("N", center.x, y, paint)
    }
}