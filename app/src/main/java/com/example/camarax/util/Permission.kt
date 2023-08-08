package com.example.camarax.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Permission {

    private const val WRITE_EXTERNAL_PERM = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private const val CAMERA_PERM = Manifest.permission.CAMERA
    private const val RECORD_AUDIO_PERM = Manifest.permission.RECORD_AUDIO

    fun hasPermissions(context: Context): Boolean {
        return (ContextCompat.checkSelfPermission(
            context,
            WRITE_EXTERNAL_PERM
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERM
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            context,
            RECORD_AUDIO_PERM
        ) == PackageManager.PERMISSION_GRANTED)
    }

    fun requestPermission(context: Context, activity: Activity) {
        if (hasPermissions(context)) return
        else {
            ActivityCompat.requestPermissions(
                activity, arrayOf(WRITE_EXTERNAL_PERM, CAMERA_PERM, RECORD_AUDIO_PERM),
                1
            )
        }

    }
}