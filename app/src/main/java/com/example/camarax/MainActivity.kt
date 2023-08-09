package com.example.camarax

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.camarax.databinding.ActivityMainBinding
import com.example.camarax.extension.ToGone
import com.example.camarax.extension.toVisible
import com.example.camarax.util.SaveToMediaStore
import com.example.camarax.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var mediaPlayer: MediaPlayer? = null
    private var systemVolume: Int = 0
    private val TAG = MainActivity::class.java.simpleName
    private var isDisplayingTimer = false
    private var isLensFacingBack: Boolean = true
    private var thumbnailImageUri: Uri? = null
    private var imageCapture: ImageCapture? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        mediaPlayer = MediaPlayer.create(this, R.raw.camera_shutter)

        initClickListener()
    }

    override fun onResume() {
        super.onResume()
        startCameraWithVideo(isLensFacingBack = true)
        systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        initRenderView()
    }

    private fun initRenderView() {
        isDisplayingTimer = false
        binding.apply {
            tvTimer.text = ""
            btnFlash.setImageResource(R.drawable.flash_off)
            seekbarZoom.progress = 0
            btnCameraCapture.toVisible()
            btnPhotoView.toVisible()
            binding.btnCameraSwitch.toVisible()
        }
    }

    private fun initClickListener() {
        binding.apply {
            btnCameraSwitch.setOnClickListener {
                Util.haptic(this@MainActivity)
                isLensFacingBack = !isLensFacingBack
                startCameraWithVideo(isLensFacingBack)

                if (!isLensFacingBack) binding.btnFlash.setImageResource(R.drawable.flash_off)
            }

            btnCameraCapture.setOnClickListener {
                if (recording != null) return@setOnClickListener
                Util.haptic(this@MainActivity)
                setShutterVolume(8)
                mediaPlayer?.start()
                takePhoto()
            }
            btnCameraCapture.setOnClickListener {
                captureVideo()
                true
            }
            btnFlash.setOnClickListener {
                if (!cameraInfo!!.hasFlashUnit()) return@setOnClickListener
                Util.haptic(this@MainActivity)

                when (cameraInfo?.torchState?.value) {
                    TorchState.ON -> {
                        cameraControl?.enableTorch(false)
                        binding.btnFlash.setImageResource(R.drawable.flash_off)
                    }

                    TorchState.OFF -> {
                        cameraControl?.enableTorch(true)
                        binding.btnFlash.setImageResource(R.drawable.flash_on)
                    }
                }
            }
            btnPhotoView.setOnClickListener {
                Util.haptic(this@MainActivity)
                Intent(Intent.ACTION_VIEW).also {
                    it.setDataAndType(thumbnailImageUri, "image/")
                    startActivity(it)
                }
            }
        }
    }

    private fun setShutterVolume(volume: Int) {
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        setShutterVolume(systemVolume)
    }

    private fun startCameraWithVideo(isLensFacingBack: Boolean) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            val recoder = Recorder.Builder().setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            ).build()
            videoCapture = VideoCapture.withOutput(recoder)
            imageCapture = ImageCapture.Builder().build()
            val lensFacingType = if (isLensFacingBack) CameraSelector.LENS_FACING_BACK
            else CameraSelector.LENS_FACING_FRONT
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacingType).build()

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
                cameraInfo!!.zoomState.observe(this) {
                    binding.tvCurrentZoon.text = DecimalFormat("#.#X").format(it.zoomRatio)
                }
                binding.seekbarZoom.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        cameraControl!!.setLinearZoom(p1 / 10.toFloat())
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "use case binding faile", e)
            }
        }, ContextCompat.getMainExecutor(this))

    }

    private fun takePhoto() {
        val localImageCapture = imageCapture ?: return
        val ouputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            SaveToMediaStore.getContentValues("image/jpeg")
        ).build()


        localImageCapture.takePicture(ouputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    outputFileResults.savedUri?.let { setGalleryThumbnail(it) }
                    setShutterVolume(systemVolume)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed ${exception.message}", exception)
                }

            })
    }

    @SuppressLint("MissingPermission")
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return
        binding.btnCameraCapture.isEnabled = false
        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            initRenderView()
            return
        }

        isDisplayingTimer = true
        displayingTimer()
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(SaveToMediaStore.getContentValues("Video/mp4"))
            .build()

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.btnCameraCapture.apply {
                            isEnabled = true
                            setImageResource(R.drawable.shutter_record)
                        }
                        binding.btnCameraSwitch.ToGone()
                        binding.btnPhotoView.ToGone()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (recordEvent.hasError()) {
                            setGalleryThumbnail(recordEvent.outputResults.outputUri)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "video capture ends with erros: ${recordEvent.error}")
                        }

                        binding.btnCameraCapture.apply {
                            setImageResource(R.drawable.shutter_photo)
                            isEnabled = true
                        }
                    }

                }

            }
    }

    private fun displayingTimer() {
        var i = 0
        CoroutineScope(Dispatchers.Main).launch {
            while (isDisplayingTimer) {
                i++
                val formattedTimer = Util.getFormattedStopWatchTime((1000 * i).toLong(), true)
                binding.tvTimer.text = formattedTimer
                delay(1000)
            }
        }
    }

    private fun setGalleryThumbnail(imageUri: Uri) {
        thumbnailImageUri = imageUri
        Glide.with(this).load(imageUri).apply(RequestOptions.circleCropTransform())
            .into(binding.btnPhotoView)
    }
}