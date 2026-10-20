package com.mraphaelpy.terriflow.presentation.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.graphics.Color as AndroidColor

/**
 * Overlay de localização ao vivo estilo Google Maps:
 * - Ponto central azul vibrante com contorno branco e leve sombra
 * - Círculo de precisão do GPS (halo suave)
 * - Cone de visão direcional (feixe que aponta para a direção em que o usuário está olhando via bússola do aparelho)
 */
class GoogleMapsStyleLocationOverlay(
    context: Context,
    mapView: MapView
) : org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(GpsMyLocationProvider(context), mapView), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private var currentHeading: Float = 0f
    private var lastHeadingTime: Long = 0L
    private val rotationMatrix = FloatArray(9)
    private val orientationVals = FloatArray(3)

    private val accuracyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(35, 66, 133, 244)
        style = Paint.Style.FILL
    }
    private val accuracyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(90, 66, 133, 244)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val whiteHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 2f, AndroidColor.argb(70, 0, 0, 0))
    }
    private val blueDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(66, 133, 244)
        style = Paint.Style.FILL
    }

    override fun enableMyLocation(): Boolean {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return super.enableMyLocation()
    }

    override fun disableMyLocation() {
        sensorManager?.unregisterListener(this)
        super.disableMyLocation()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val now = System.currentTimeMillis()
        if (now - lastHeadingTime < 80) return

        val newHeading: Float
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationVals)
            val azimuth = Math.toDegrees(orientationVals[0].toDouble()).toFloat()
            newHeading = (azimuth + 360f) % 360f
        } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            newHeading = (event.values[0] + 360f) % 360f
        } else {
            return
        }

        if (kotlin.math.abs(newHeading - currentHeading) > 2.0f) {
            currentHeading = newHeading
            lastHeadingTime = now
            mMapView.postInvalidate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun drawMyLocation(canvas: Canvas, pj: Projection, lastFix: Location) {
        val screenPt = Point()
        pj.toPixels(GeoPoint(lastFix.latitude, lastFix.longitude), screenPt)
        val cx = screenPt.x.toFloat()
        val cy = screenPt.y.toFloat()

        // 1. Círculo de precisão do GPS (halo suave azul)
        if (lastFix.hasAccuracy() && lastFix.accuracy > 0) {
            val radiusPx = pj.metersToEquatorPixels(lastFix.accuracy)
            if (radiusPx > 12f && radiusPx < 1200f) {
                canvas.drawCircle(cx, cy, radiusPx, accuracyPaint)
                canvas.drawCircle(cx, cy, radiusPx, accuracyBorderPaint)
            }
        }

        // 2. Cone de visão direcional (feixe estilo Google Maps)
        val heading = if (currentHeading != 0f) currentHeading else if (lastFix.hasBearing()) lastFix.bearing else 0f
        val coneRadius = 90f
        val sweepAngle = 55f
        val startAngle = heading - (sweepAngle / 2f) - 90f

        val conePath = Path().apply {
            moveTo(cx, cy)
            arcTo(
                RectF(cx - coneRadius, cy - coneRadius, cx + coneRadius, cy + coneRadius),
                startAngle,
                sweepAngle
            )
            close()
        }

        val conePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, coneRadius,
                intArrayOf(
                    AndroidColor.argb(140, 66, 133, 244),
                    AndroidColor.argb(45, 66, 133, 244),
                    AndroidColor.TRANSPARENT
                ),
                floatArrayOf(0f, 0.60f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawPath(conePath, conePaint)

        // 3. Ponto branco externo com sombra suave
        canvas.drawCircle(cx, cy, 18f, whiteHaloPaint)

        // 4. Ponto azul central (Google Maps)
        canvas.drawCircle(cx, cy, 12f, blueDotPaint)
    }
}
