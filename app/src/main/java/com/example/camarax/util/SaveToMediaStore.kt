package com.example.camarax.util

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat

object SaveToMediaStore {

    fun getContentValues(type : String): ContentValues {
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis())
        val folderName = "camerax"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, type)

            when{
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_PICTURES + File.separator + folderName)
                Build.VERSION.SDK_INT > Build.VERSION_CODES.P ->
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$folderName")
            }
        }
        return contentValues
    }
}