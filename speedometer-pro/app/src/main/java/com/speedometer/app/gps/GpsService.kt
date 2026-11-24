package com.speedometer.app.gps

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.speedometer.app.MainActivity
import com.speedometer.app.R
import java.util.Locale

/**
 * Foreground service that owns the GPS stream.
 *
 * Two things matter for accuracy:
 *  - [Priority.PRIORITY_HIGH_ACCURACY] asks the OS for GPS (not just network).
 *  - We read [Location.getSpeed] (GPS Doppler speed) instead of computing speed
 *    from consecutive positions, which is how cheap implementations fail.
 */
class GpsService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var wakeLock: PowerManager.WakeLock? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val best = result.locations
                .filter { it != null }
                .minByOrNull { it.accuracy }
                ?: return
            onFix(best)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when {
            action == ACTION_START -> startTracking()
            action == ACTION_STOP -> {
                stopTracking()
                stopSelf()
            }
            action == ACTION_RESET -> GpsStateHolder.resetSession()
        }
        return START_STICKY
    }

    private fun startTracking() {
        startForeground(NOTIFICATION_ID, buildNotification(0f))
        acquireWakeLock()
        requestUpdates()
        GpsStateHolder.publish(GpsStateHolder.current().copy(running = true, message = "Tracking aktif"))
    }

    private fun stopTracking() {
        releaseWakeLock()
        fusedClient.removeLocationUpdates(callback)
        GpsStateHolder.publish(GpsStateHolder.current().copy(running = false, message = "Tracking berhenti"))
    }

    private fun requestUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            GpsStateHolder.publish(GpsStateHolder.current().copy(message = "Izin lokasi belum diberikan"))
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(250L)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, callback, null)
        } catch (e: SecurityException) {
            GpsStateHolder.publish(GpsStateHolder.current().copy(message = "Izin lokasi ditolak"))
        }
    }

    private fun onFix(location: Location) {
        val st = GpsStateHolder.current()
        val result = GpsStateHolder.tracker.onSample(
            TripTracker.Sample(
                timeMs = SystemClock.elapsedRealtime(),
                lat = location.latitude,
                lon = location.longitude,
                speedMps = location.speed,
                accuracyM = location.accuracy
            ),
            satelliteCount = satelliteCountOf(location)
        )

        val smoothed = GpsStateHolder.smoother.push(result.speedKmh)

        GpsStateHolder.publish(
            st.copy(
                running = true,
                gpsFix = result.valid,
                speedKmh = result.speedKmh,
                smoothedKmh = smoothed,
                maxSpeedKmh = result.maxSpeedKmh,
                distanceM = result.totalDistanceM,
                elapsedMs = result.elapsedMs,
                accuracyM = location.accuracy,
                satellites = satelliteCountOf(location),
                message = when {
                    result.valid -> "OK"
                    result.rejectedReason?.startsWith("accuracy") == true ->
                        "Signal lemah (${location.accuracy.toInt()}m)"
                    else -> result.rejectedReason ?: "OK"
                }
            )
        )

        updateNotification(smoothed)
    }

    private fun satelliteCountOf(location: Location): Int {
        val extras = location.extras ?: return 0
        return try {
            extras.getInt("satellites", extras.getInt("satellitesvalue", 0))
        } catch (e: Exception) {
            0
        }
    }

    private fun updateNotification(speedKmh: Float) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(speedKmh))
    }

    private fun buildNotification(speedKmh: Float): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(String.format(Locale.US, "%.0f km/h", speedKmh))
            .setSmallIcon(R.drawable.ic_speedometer)
            .setContentIntent(pi)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SpeedometerPro::gps"
            ).apply { acquire(6 * 60 * 60 * 1000L) }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.speedometer.app.action.START"
        const val ACTION_STOP = "com.speedometer.app.action.STOP"
        const val ACTION_RESET = "com.speedometer.app.action.RESET"
        private const val CHANNEL_ID = "gps_tracking"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, GpsService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, GpsService::class.java).setAction(ACTION_STOP))
        }

        fun reset(context: Context) {
            context.startService(Intent(context, GpsService::class.java).setAction(ACTION_RESET))
        }
    }
}
