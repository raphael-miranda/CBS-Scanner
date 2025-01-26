package com.cbs.scanner

import android.content.Context
import android.util.Log
import java.io.File

fun paddedString(original: String, totalLength: Int): String {
    return original.padEnd(totalLength, ' ')
}

fun getCBSScannerDir(context: Context): File? {
    // Get external storage directory
    val appSpecificDir = context.getExternalFilesDir(null)
    val cbsScannerDir = File(appSpecificDir, "CBS_Scanner")
    if (!cbsScannerDir.exists()) {
        val created = cbsScannerDir.mkdirs()
        if (!created) {
            Log.e("=========", "Failed to create directory")
            return null
        }
    }
    return cbsScannerDir
}