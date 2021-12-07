package com.example.example

import android.graphics.Rect
import android.hardware.Camera
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import java.io.File
import kotlin.math.abs

class RecordActivity : AppCompatActivity() {
    private val TAG = "RecordActivity"
    private val FOCUS_METERING_AREA_WEIGHT_DEFAULT = 1000
    private val FOCUS_AREA_SIZE_DEFAULT = 300

    private var textureView: TextureView? = null

    private var camera: Camera? = null
    var mediaRecorder: MediaRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        val btnOpt = findViewById<Button>(R.id.btn_opt)
        textureView = findViewById(R.id.textureView)
        camera = Camera.open().apply {
            setDisplayOrientation(90)
            unlock()
        }
        btnOpt.setOnClickListener {
            if (btnOpt.text == getString(R.string.opt)) {
                btnOpt.text = getString(R.string.stopOpt)

                mediaRecorder = MediaRecorder().apply {
                    setCamera(camera)
                    setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                    setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setOrientationHint(90)
                    setVideoEncodingBitRate(5 * 1024 * 1024)
                }.apply {
                    setOutputFile(File(getExternalFilesDir(""), "a.mp4").absolutePath)
                    setVideoSize(1280, 720)
                    setPreviewDisplay(Surface(textureView?.surfaceTexture))

                }.apply {
                    try {
                        prepare()
                        start()
                    } catch (e: Exception) {
                    }
                }
            } else {
                btnOpt.text = getString(R.string.opt)
                mediaRecorder?.stop()
                mediaRecorder?.release()
                camera?.stopPreview()
                camera?.release()
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> manualFocuse(event)
        }
        return super.onTouchEvent(event)
    }

    fun manualFocuse(event: MotionEvent): Boolean {
        if (camera != null) {
            val parameters: Camera.Parameters? = camera?.getParameters()
            val focusMode: String? = parameters?.getFocusMode()
            val rect: Rect? = calculateFocusArea(event.getX(), event.getY())
            val meteringAreas = mutableListOf<Camera.Area>()
            meteringAreas.add(Camera.Area(rect, FOCUS_METERING_AREA_WEIGHT_DEFAULT))

            if (parameters?.getMaxNumFocusAreas() != 0 && focusMode != null &&
                (focusMode == Camera.Parameters.FOCUS_MODE_AUTO ||
                        focusMode == (Camera.Parameters.FOCUS_MODE_MACRO) ||
                        focusMode == (Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ||
                        focusMode == (Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            ) {
                if (!parameters.getSupportedFocusModes()
                        .contains(Camera.Parameters.FOCUS_MODE_AUTO)
                ) {
                    return false
                }
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO)
                parameters.setFocusAreas(meteringAreas)
                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setMeteringAreas(meteringAreas)
                }
                camera?.setParameters(parameters)
            } else if (parameters?.getMaxNumMeteringAreas()!! > 0) {
                if (!parameters.getSupportedFocusModes()
                        ?.contains(Camera.Parameters.FOCUS_MODE_AUTO)!!
                ) {
                    return false
                }
                camera?.setParameters(parameters)

            } else {
            }
        }
        return false
    }

    private fun calculateFocusArea(x: Float, y: Float): Rect? {
        val buffer = FOCUS_AREA_SIZE_DEFAULT / 2
        val centerX = textureView?.width?.let { calculateCenter(x, it, buffer) }
        val centerY = textureView?.height?.let { calculateCenter(y, it, buffer) }
        if (centerX != null) {
            if (centerY != null) {
                return Rect(
                    centerX - buffer,
                    centerY - buffer,
                    centerX + buffer,
                    centerY + buffer
                )
            }
        }
        return null
    }

    private fun calculateCenter(coord: Float, dimen: Int, buffer: Int): Int {
        val normalized = ((coord / dimen) * 2000 - 1000).toInt()
        return if (abs(normalized) + buffer > 1000) {
            if (normalized > 0) {
                1000 - buffer
            } else {
                -1000 + buffer
            }
        } else {
            normalized
        }
    }

}