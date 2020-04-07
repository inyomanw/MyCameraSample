package com.example.mycamerasample.utils

import android.Manifest

class TextUtils {
    companion object {
        const val RATIO_4_3_VALUE = 4.0 / 3.0
        const val RATIO_16_9_VALUE = 16.0 / 9.0
        val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)
        const val PERMISSIONS_REQUEST_CODE = 342
    }
}