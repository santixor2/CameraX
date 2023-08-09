package com.example.camarax.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.TimeUnit

object Util {

    fun haptic(context: Context) {
        val vibrator = getVibrator(context)
        val vibratorEffect = VibrationEffect.createOneShot(50, 100)
        vibrator.vibrate(vibratorEffect)
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    }

    fun getFormattedStopWatchTime(ms: Long, includeMillis : Boolean = false):String{
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        if(!includeMillis){
            return  "${ if (hours < 0) "0" else ""}$hours:" +
                    "${ if (minutes < 0) "0" else ""}$minutes:" +
                    "${ if (seconds < 0) "0" else ""}$seconds"
        }
        return  "${ if (hours < 0) "0" else ""}$hours:" +
                "${ if (minutes < 0) "0" else ""}$minutes:" +
                "${ if (seconds < 0) "0" else ""}$seconds"

    }
}