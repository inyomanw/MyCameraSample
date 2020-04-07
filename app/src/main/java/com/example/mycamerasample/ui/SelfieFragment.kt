package com.example.mycamerasample.ui

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.mycamerasample.R
import com.example.mycamerasample.utils.LuminosityAnalyzer
import com.example.mycamerasample.utils.aspectRatio
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_selfie.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

abstract class SelfieFragment : BottomSheetDialogFragment() {

    private lateinit var viewFinder: PreviewView

    protected var lensFacing = CameraSelector.LENS_FACING_FRONT
    protected var imageFilePath: String? = null
    private var imageCapture: ImageCapture? = null
    private val executor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private lateinit var mainExecutor: Executor


    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    private val imageSavedCallback by lazy {
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(file: File) {
                imageFilePath = file.absolutePath
                openPreview(file)
            }

            override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_selfie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogShowListener()
        camera_preview.post { bindCameraUseCases2() }
        setupListeners()
        setupLayout()
        viewFinder = view.findViewById(R.id.camera_preview)
        mainExecutor = ContextCompat.getMainExecutor(requireContext())

    }

    abstract fun setupLayout()
    private fun dialogShowListener() {
        dialog?.setOnShowListener {
            val dlg = dialog?.let { it as BottomSheetDialog } ?: return@setOnShowListener
            val bottomSheet =
                dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            with(bottomSheetBehavior) {
                state = BottomSheetBehavior.STATE_EXPANDED
                peekHeight = bottomSheet.height
            }
        }
    }

    private fun setupListeners() {
        iv_switch.setOnClickListener {
            // Bind use cases
            bindCameraUseCases2()
            lensFacing = when (lensFacing) {
                CameraSelector.LENS_FACING_FRONT -> CameraSelector.LENS_FACING_BACK
                else -> CameraSelector.LENS_FACING_FRONT
            }

//            bindCameraUseCases()
        }
        bt_capture.setOnClickListener {
            it.isEnabled = false
//            val file = context?.let {
//                val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)
//                PhotoUtils.createImageFile(context = it)
//            } ?: return@setOnClickListener
//            imageCapture?.takePicture(
//                file,
//                ImageCapture.Metadata(),
//                executor,
//                imageSavedCallback
//            )
        }
//        tv_retake.setOnClickListener {
//            retakePicture()
//        }
    }

    private fun retakePicture() {
//        bt_capture.isEnabled = true
//        iv_switch.isVisible = true
//        bt_capture.isVisible = true
//        iv_preview.isInvisible = true
//        bt_proceed.isInvisible = true
//        camera.isInvisible = false
//        tv_retake.isGone = true
    }

    private fun bindCameraUseCases() {
        val rotation = viewFinder.display.rotation

        val metrics = DisplayMetrics().also { camera_preview.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val previewConfig = Preview.Builder().apply {
            //            setLensFacing(lensFacing)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(camera_preview.display.rotation)
        }.build()


        val preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits requested capture mode
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()


//        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases2() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d("logd", "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d("logd", "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            // Default PreviewSurfaceProvider
            preview?.setPreviewSurfaceProvider(viewFinder.previewSurfaceProvider)

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits requested capture mode
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            // ImageAnalysis
            imageAnalyzer = ImageAnalysis.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(mainExecutor, LuminosityAnalyzer { luma ->
                        // Values returned from our analyzer are passed to the attached listener
                        // We log image analysis results here - you should do something useful instead!
                        Log.d("logd", "Average luminosity: $luma")
                    })
                }

            // Must unbind the use-cases before rebinding them.
            cameraProvider.unbindAll()

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("logd", "Use case binding failed", exc)
            }

        }, mainExecutor)
    }

    private fun openPreview(file: File) {
        CoroutineScope(Dispatchers.Default).launch {
            Log.d("logd", "file : $file")
            //            val bitmap = PhotoUtils.overridePhotoSize(requireContext(), file)
//            iv_preview.post {
//                iv_preview.setImageBitmap(bitmap)
//                iv_switch.isVisible = false
//                iv_preview.isVisible = true
//                bt_proceed.isVisible = true
//                tv_retake.isVisible = true
//                camera.isInvisible = true
//                bt_capture.isInvisible = true
//            }
        }
    }

    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension
        )

    companion object {
        const val STORE_KEY = "store_key"
        const val SELFIE_TYPE = "selfie_type"
        const val SELFIE_TYPE_CHECKOUT = 1
    }

//setup camera di bindCameraUseCases()
    //handle hasil foto di imageSavedCallback
}