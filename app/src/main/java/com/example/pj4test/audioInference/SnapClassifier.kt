package com.example.pj4test.audioInference

import android.content.Context
import android.media.AudioRecord
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import com.example.pj4test.controller.ModelController
import org.tensorflow.lite.task.audio.classifier.Classifications

class SnapClassifier {
    // Libraries for audio classification
    lateinit var classifier: AudioClassifier
    //lateinit var classifier2: AudioClassifier
    lateinit var recorder: AudioRecord
    lateinit var tensor: TensorAudio
    lateinit var controller: ModelController
    // Listener that will be handle the result of this classifier
    private var detectorListener: DetectorListener? = null

    // TimerTask
    private var task: TimerTask? = null

    /**
     * initialize
     *
     * Create YAMNet classifier from tflite model file saved in YAMNET_MODEL,
     * initialize the audio recorder, and make recorder start recording.
     * Set TimerTask for periodic inferences by REFRESH_INTERVAL_MS milliseconds.
     *
     * @param   context Context of the application
     */
    fun initialize(context: Context) {
        classifier = AudioClassifier.createFromFile(context, TEACHED_MODEL)
        //classifier2 = AudioClassifier.createFromFile(context, YAMNET_MODEL)//Use same format as previous one
        controller = ModelController.getInstance()
        Log.d(TAG, "Model loaded from: $YAMNET_MODEL")
        audioInitialize()
        startRecording()

        startInferencing()
    }

    /**
     * audioInitialize
     *
     * Create the instance of TensorAudio and AudioRecord from the AudioClassifier.
     */
    private fun audioInitialize() {
        tensor = classifier.createInputTensorAudio()
        val format = classifier.requiredTensorAudioFormat
        val recorderSpecs = "Number Of Channels: ${format.channels}\n" +
                "Sample Rate: ${format.sampleRate}"
        Log.d(TAG, recorderSpecs)
        Log.d(TAG, classifier.requiredInputBufferSize.toString())

        recorder = classifier.createAudioRecord()
    }

    /**
     * startRecording
     *
     * This method make recorder start recording.
     * After this function, the microphone is ready for reading.
     */
    private fun startRecording() {
        recorder.startRecording()
        Log.d(TAG, "record started!")
    }

    /**
     * stopRecording
     *
     * This method make recorder stop recording.
     * After this function, the microphone is unavailable for reading.
     */
    private fun stopRecording() {
        recorder.stop()
        Log.d(TAG, "record stopped.")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * inference
     *
     * Make model inference of the audio gotten from audio recorder.
     * Change recorded audio clip into an input tensor of the model,
     * and classify the tensor with the audio classifier model.
     *
     * To classify honking sound, calculate the max predicted scores among 3 related classes,
     * "Vehicle horn, car horn, honking", "Beep, bleep", and "Buzzer".
     *
     * @return  A score of the maximum float value among three classes
     */
    fun inference(): MutableList<Classifications> {
        tensor.load(recorder)
        Log.d(TAG, tensor.tensorBuffer.shape.joinToString(","))
        val output = classifier.classify(tensor)
        Log.d(TAG, output.toString())
        Log.d(TAG, "Audio Classifier : " + output[0].categories[0].score)
        controller.setFootStepScore(output[0].categories.find { it.label == "1 footstep" }!!.score)
        controller.setGamingScore(output[0].categories.find { it.label == "2 game" }!!.score)
        return output//[0].categories.find { it.label == "1 footstep" }!!.score
    }

    fun startInferencing() {
        if (task == null) {
            task = Timer().scheduleAtFixedRate(0, REFRESH_INTERVAL_MS) {
                if(controller.allowAudioProceed()){
                    val score = inference()
                    if(score.isNotEmpty())
                        detectorListener?.onResults(score[0].categories.find { it.label == "1 footstep" }!!.score, score[0].categories.find { it.label == "2 game" }!!.score)
                }
            }
        }
    }

    fun stopInferencing() {
        task?.cancel()
        task = null
    }

    /**
     * interface DetectorListener
     *
     * This is an interface for listener.
     * To get result from this classifier, inherit this interface
     * and set itself to this' detector listener
     */
    interface DetectorListener {
        fun onResults(FootScore: Float, GameScore: Float)
    }

    /**
     * setDetectorListener
     *
     * Set detector listener for this classifier.
     */
    fun setDetectorListener(listener: DetectorListener) {
        detectorListener = listener
    }

    /**
     * companion object
     *
     * This includes useful constants for this classifier.
     *
     * @property    TAG                 tag for logging
     * @property    REFRESH_INTERVAL_MS refresh interval of the inference
     * @property    YAMNET_MODEL        file path of the model file
     * @property    TEACHED_MODEL       file from teachable machine
     * @property    THRESHOLD           threshold of the score to classify sound as a horn sound
     */
    companion object {
        const val TAG = "HornClassifier"

        const val REFRESH_INTERVAL_MS = 33L
        const val YAMNET_MODEL = "yamnet_classification.tflite"
        const val TEACHED_MODEL = "game_footstep_model.tflite"
        const val THRESHOLD = 0.3f
    }
}