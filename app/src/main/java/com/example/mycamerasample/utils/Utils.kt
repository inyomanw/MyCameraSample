package com.example.mycamerasample.utils

import androidx.camera.core.AspectRatio
import com.example.mycamerasample.utils.TextUtils.Companion.RATIO_16_9_VALUE
import com.example.mycamerasample.utils.TextUtils.Companion.RATIO_4_3_VALUE
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Utils {

    fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }
}