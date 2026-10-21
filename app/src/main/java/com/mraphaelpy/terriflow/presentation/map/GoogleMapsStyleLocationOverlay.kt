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
import android.os.Build
import android.os.SystemClock
import android.view.Choreographer
import android.view.Surface
import android.view.WindowManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs
import kotlin.math.exp
import android.graphics.Color as AndroidColor

class GoogleMapsStyleLocationOverlay(
    private val context: Context,
    mapView: MapView
) : MyLocationNewOverlay(GpsMyLocationProvider(context), mapView), SensorEventListener, Choreographer.FrameCallback {

    private val density = context.resources.displayMetrics.density
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var targetHeading = Float.NaN
    private var displayedHeading = Float.NaN
    private var targetSweep = DEFAULT_SWEEP
    private var displayedSweep = DEFAULT_SWEEP

    private var fromLat = 0.0
    private var fromLng = 0.0
    private var toLat = 0.0
    private var toLng = 0.0
    private var moveStartedAt = 0L
    private var hasPosition = false

    private var frameScheduled = false
    private var lastFrameNanos = 0L

    private val screenPoint = Point()
    private val geoPoint = GeoPoint(0.0, 0.0)

    private val coneRadius = 48f * density
    private val conePath = Path()
    private var conePathSweep = Float.NaN
    private val coneBounds = RectF(-coneRadius, -coneRadius, coneRadius, coneRadius)
    private val conePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            0f, 0f, coneRadius,
            intArrayOf(AndroidColor.argb(150, 66, 133, 244), AndroidColor.argb(60, 66, 133, 244), AndroidColor.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    private val accuracyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(32, 66, 133, 244)
        style = Paint.Style.FILL
    }
    private val accuracyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(80, 66, 133, 244)
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
        setShadowLayer(4f * density, 0f, 1f * density, AndroidColor.argb(80, 0, 0, 0))
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun enableMyLocation(): Boolean {
        rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        return super.enableMyLocation()
    }

    override fun disableMyLocation() {
        sensorManager?.unregisterListener(this)
        stopFrames()
        super.disableMyLocation()
    }

    override fun onDetach(mapView: MapView?) {
        sensorManager?.unregisterListener(this)
        stopFrames()
        super.onDetach(mapView)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val (axisX, axisY) = when (displayRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
        SensorManager.getOrientation(remappedMatrix, orientation)
        val heading = normalize(Math.toDegrees(orientation[0].toDouble()).toFloat())

        if (event.values.size > 4 && event.values[4] > 0f) {
            val accuracyDegrees = Math.toDegrees(event.values[4].toDouble()).toFloat()
            targetSweep = (accuracyDegrees * 2f).coerceIn(MIN_SWEEP, MAX_SWEEP)
        }

        if (targetHeading.isNaN() || abs(shortestDelta(targetHeading, heading)) >= HEADING_DEADBAND) {
            targetHeading = heading
            if (displayedHeading.isNaN()) displayedHeading = heading
            requestFrames()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (rotationSensor == null || sensor != rotationSensor) return
        targetSweep = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> MIN_SWEEP
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> DEFAULT_SWEEP
            else -> MAX_SWEEP
        }
        requestFrames()
    }

    override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
        if (location != null) {
            mMapView.post { startMoveTo(location.latitude, location.longitude) }
        }
        super.onLocationChanged(location, source)
    }

    private fun startMoveTo(lat: Double, lng: Double) {
        if (!hasPosition) {
            fromLat = lat; fromLng = lng
            toLat = lat; toLng = lng
            hasPosition = true
            moveStartedAt = 0L
            mMapView.invalidate()
            return
        }
        val (currentLat, currentLng) = currentPosition(SystemClock.uptimeMillis())
        fromLat = currentLat; fromLng = currentLng
        toLat = lat; toLng = lng
        moveStartedAt = SystemClock.uptimeMillis()
        requestFrames()
    }

    private fun currentPosition(now: Long): Pair<Double, Double> {
        val t = if (moveStartedAt == 0L) 1f else ((now - moveStartedAt).toFloat() / MOVE_DURATION_MS).coerceIn(0f, 1f)
        val eased = 1f - (1f - t) * (1f - t) * (1f - t)
        return (fromLat + (toLat - fromLat) * eased) to (fromLng + (toLng - fromLng) * eased)
    }

    override fun doFrame(frameTimeNanos: Long) {
        frameScheduled = false
        val dtMs = if (lastFrameNanos == 0L) 16f else ((frameTimeNanos - lastFrameNanos) / 1_000_000f).coerceIn(1f, 100f)
        lastFrameNanos = frameTimeNanos

        var animating = false
        if (!targetHeading.isNaN()) {
            val delta = shortestDelta(displayedHeading, targetHeading)
            if (abs(delta) > SETTLE_THRESHOLD) {
                displayedHeading = normalize(displayedHeading + delta * smoothing(dtMs, HEADING_TAU_MS))
                animating = true
            } else {
                displayedHeading = targetHeading
            }
        }
        val sweepDelta = targetSweep - displayedSweep
        if (abs(sweepDelta) > SETTLE_THRESHOLD) {
            displayedSweep += sweepDelta * smoothing(dtMs, SWEEP_TAU_MS)
            animating = true
        } else {
            displayedSweep = targetSweep
        }
        if (moveStartedAt != 0L && SystemClock.uptimeMillis() - moveStartedAt < MOVE_DURATION_MS) animating = true

        mMapView.invalidate()
        if (animating) requestFrames() else lastFrameNanos = 0L
    }

    private fun requestFrames() {
        if (frameScheduled) return
        frameScheduled = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun stopFrames() {
        if (!frameScheduled) return
        Choreographer.getInstance().removeFrameCallback(this)
        frameScheduled = false
        lastFrameNanos = 0L
    }

    override fun drawMyLocation(canvas: Canvas, pj: Projection, lastFix: Location) {
        if (!hasPosition) {
            fromLat = lastFix.latitude; fromLng = lastFix.longitude
            toLat = lastFix.latitude; toLng = lastFix.longitude
            hasPosition = true
        }
        val (lat, lng) = currentPosition(SystemClock.uptimeMillis())
        geoPoint.setCoords(lat, lng)
        pj.toPixels(geoPoint, screenPoint)
        val cx = screenPoint.x.toFloat()
        val cy = screenPoint.y.toFloat()
        val stale = System.currentTimeMillis() - lastFix.time > STALE_FIX_MS

        if (lastFix.hasAccuracy() && lastFix.accuracy > 0) {
            val radiusPx = pj.metersToEquatorPixels(lastFix.accuracy)
            if (radiusPx > 14f * density && radiusPx < 600f * density) {
                canvas.drawCircle(cx, cy, radiusPx, accuracyPaint)
                canvas.drawCircle(cx, cy, radiusPx, accuracyBorderPaint)
            }
        }

        val heading = when {
            !displayedHeading.isNaN() -> displayedHeading
            lastFix.hasBearing() -> lastFix.bearing
            else -> Float.NaN
        }
        if (!heading.isNaN() && !stale) {
            if (abs(conePathSweep - displayedSweep) > 0.5f || conePathSweep.isNaN()) {
                conePath.reset()
                conePath.moveTo(0f, 0f)
                conePath.arcTo(coneBounds, -90f - displayedSweep / 2f, displayedSweep)
                conePath.close()
                conePathSweep = displayedSweep
            }
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(heading - mMapView.mapOrientation)
            canvas.drawPath(conePath, conePaint)
            canvas.restore()
        }

        dotPaint.color = if (stale) STALE_COLOR else DOT_COLOR
        canvas.drawCircle(cx, cy, 10f * density, haloPaint)
        canvas.drawCircle(cx, cy, 7f * density, dotPaint)
    }

    private fun displayRotation(): Int {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { context.display }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
        return display?.rotation ?: Surface.ROTATION_0
    }

    private companion object {
        const val MIN_SWEEP = 40f
        const val DEFAULT_SWEEP = 55f
        const val MAX_SWEEP = 90f
        const val HEADING_DEADBAND = 0.8f
        const val SETTLE_THRESHOLD = 0.3f
        const val HEADING_TAU_MS = 90f
        const val SWEEP_TAU_MS = 250f
        const val MOVE_DURATION_MS = 800L
        const val STALE_FIX_MS = 60_000L
        val DOT_COLOR = AndroidColor.rgb(26, 115, 232)
        val STALE_COLOR = AndroidColor.rgb(140, 140, 140)

        fun normalize(angle: Float): Float = ((angle % 360f) + 360f) % 360f

        fun shortestDelta(from: Float, to: Float): Float {
            val diff = normalize(to - from)
            return if (diff > 180f) diff - 360f else diff
        }

        fun smoothing(dtMs: Float, tauMs: Float): Float = 1f - exp(-dtMs / tauMs)
    }
}
