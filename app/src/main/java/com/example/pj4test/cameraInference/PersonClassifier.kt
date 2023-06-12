/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pj4test.cameraInference

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.os.SystemClock
import android.util.Log
import com.example.pj4test.audioInference.SnapClassifier
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import com.example.pj4test.controller.ModelController
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class PersonClassifier {
    // Libraries for object detection
    lateinit var objectDetector: ObjectDetector
    lateinit var imageClassifier: ImageClassifier////////////////
    // Listener that will be handle the result of this classifier
    private var objectDetectorListener: DetectorListener? = null
    lateinit var controller : ModelController
    fun initialize(context: Context) {
        setupObjectDetector(context)
        controller = ModelController.getInstance()
        controller.setReloadTime(1000L)
    }

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU and NNAPI delegates can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    private fun setupObjectDetector(context: Context) {
        // Create the base options for the detector using specifies max results and score threshold
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(THRESHOLD)
                .setMaxResults(MAX_RESULTS)

        val optionsForClassifierBuilder =
            ImageClassifier.ImageClassifierOptions.builder()
                .setScoreThreshold(THRESHOLD)
                .setMaxResults(3)//////

        // Set general detection options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(NUM_THREADS)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, MODEL_NAME, optionsBuilder.build())

            imageClassifier =
                ImageClassifier.createFromFileAndOptions(context, CLASS_MODEL_NAME, optionsForClassifierBuilder.build())///////////

        } catch (e: IllegalStateException) {
            objectDetectorListener?.onObjectDetectionError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        // Inference time is the difference between the system time at the start and finish of the
        // process
        if(!controller.allowRun()){
            return
        }
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = objectDetector.detect(tensorImage)
        val results_class = imageClassifier.classify(tensorImage)
        Log.d("Classification","Class res is : $results_class")
        controller.setResult(results.toString())
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        if(results_class[0].categories.isNotEmpty()){
        objectDetectorListener?.onObjectDetectionResults(
            results_class[0].categories[0].label,
            results_class[0].categories[0].score,
            results,
            inferenceTime,
            tensorImage.height,
            tensorImage.width)}
    }

    interface DetectorListener {
        fun onObjectDetectionError(error: String)
        fun onObjectDetectionResults(
            classificationResult: String,
            classificationScore: Float,
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    fun setDetectorListener(listener: DetectorListener) {
        objectDetectorListener = listener
    }

    companion object {
        const val THRESHOLD: Float = 0.5f
        const val NUM_THREADS: Int = 2
        const val MAX_RESULTS: Int = 3
        const val MODEL_NAME = "mobilenet_v1.tflite"
        const val CLASS_MODEL_NAME = "model.tflite"
    }
}
