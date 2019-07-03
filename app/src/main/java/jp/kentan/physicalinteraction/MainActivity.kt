package jp.kentan.physicalinteraction

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.min

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private companion object {
        const val SAMPLING_RATE = 44100
        const val NOISE_OFFSET = 500
    }

    private lateinit var audioRecord: AudioRecord

    private var sampleSize = 0
    private var isRecording = false

    private lateinit var inputData: ShortArray
    private lateinit var outputData: ShortArray

    private lateinit var inputBuffer: FloatArray
    private lateinit var outputBuffer: FloatArray

    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startRecord()

        button.setOnClickListener {
            stopRecording()
            finish()
        }
    }

    private fun initAudioRecord() {
        // AudioRecordで必要なバッファサイズを取得
        val bufSize = AudioRecord.getMinBufferSize(
            SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        sampleSize = bufSize / 2

        inputData = ShortArray(sampleSize)
        outputData = ShortArray(sampleSize)

        initNativeData()
    }

    private fun initNativeData() {
        inputBuffer = FloatArray(sampleSize)
        outputBuffer = FloatArray(sampleSize)
    }

    private fun startAudioRecording() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            sampleSize * 2
        )

        audioRecord.startRecording()
    }


    private fun startRecord() {
        initAudioRecord()
        startAudioRecording()
        isRecording = true

        initVibration()

        launch(Dispatchers.Default) {
            while (isRecording) {
                // 読み込み
                audioRecord.read(inputData, 0, sampleSize)

                val volume = min(inputData.calcVolume(), 2000).toFloat()

                val amplitude = ((volume / 2000.0) * 255.0).toInt()

                Log.d("MainActivity", volume.toString())
                if (amplitude > 0) {
                    val effect = VibrationEffect.createOneShot(200, amplitude)
                    vibrator.vibrate(effect)
                } else {
                    vibrator.cancel()
                }

                delay(200)
            }
        }
    }

    private fun initVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun stopRecording() {
        isRecording = false

        audioRecord.stop()
        audioRecord.release()
    }

    private fun ShortArray.calcVolume() =
        kotlin.math.max(0, (map { abs(it.toInt()) }.sum() / sampleSize) - NOISE_OFFSET)
}
