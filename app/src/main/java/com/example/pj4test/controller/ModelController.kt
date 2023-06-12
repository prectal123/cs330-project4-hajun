package com.example.pj4test.controller

import android.util.Log
import android.os.SystemClock
import com.example.pj4test.fragment.CameraFragment
import java.util.Vector

class ModelController private constructor() {
    private val HIGH_AUDIO_INTERVAL = 200L
    private val LOW_AUDIO_INTERVAL = 50L
    private val HIGH_IMAGE_INTERVAL = 200L
    private val LOW_IMAGE_INTERVAL = 50L

    private val AUDIO_THRESHOLD = 0.3F
    private val IMAGE_THRESHOLD = 0.5F

    private var result = ""
    private var reloadInterval = 0L
    private var allowTime = 0L
    private var personScore = 0F
    private var audioInterval = 0L
    private var audioAllowTime = 0L
    private var imageInterval = 0L
    private var imageAllowTime = 0L
    private var footStepTimeWindow = Vector<Float>()
    private var gamingTimeWindow = Vector<Float>()
    private var imageTimeWindow = Vector<Float>()
    private var isGaming = false
    private var isFootstep = false

    fun onCreate(){
        result = "Not set yet"
    }
    private fun calculateAverage(vec: Vector<Float>): Float{
        var sum: Float = 0F
        for(i in vec){
            sum += i
        }
        return sum/vec.size
    }

    fun getIntervalInfo():String{
        return "Audio Interval: $audioInterval, Image Interval: $imageInterval"
    }
    fun setGamingScore(score: Float){
        if(gamingTimeWindow.size == 10){ gamingTimeWindow.removeElementAt(0) }
        gamingTimeWindow.addElement(score)
        if(calculateAverage(gamingTimeWindow) > AUDIO_THRESHOLD ){
            isGaming = true
        }
    }
    fun needBindCamera(): Boolean{
        Log.d("Controller", "WHY ${calculateAverage(footStepTimeWindow)}")
        return isFootstep
    }

    fun setPersonScore(score: Float){
        personScore = score
        if(imageTimeWindow.size == 10){ imageTimeWindow.removeElementAt(0) }
        imageTimeWindow.addElement(score)
        if(calculateAverage(imageTimeWindow) > IMAGE_THRESHOLD) { imageInterval = LOW_IMAGE_INTERVAL }
        else { imageInterval = HIGH_IMAGE_INTERVAL }
    }

    fun setFootStepScore(score: Float){
        Log.d("Controller", "In set score")
        if(footStepTimeWindow.size == 1){ footStepTimeWindow.removeElementAt(0) }
        footStepTimeWindow.addElement(score)
        if(calculateAverage(footStepTimeWindow) > AUDIO_THRESHOLD) {
            audioInterval = LOW_AUDIO_INTERVAL
            isFootstep = true
        }
        else {
            audioInterval = HIGH_AUDIO_INTERVAL
            isFootstep = false
        }
    }

    fun allowCameraProceed(): Boolean{
        if(imageAllowTime <= SystemClock.uptimeMillis()){
            imageAllowTime = SystemClock.uptimeMillis() + imageInterval
            return true
        }
        else {
            return false
        }
    }
    fun allowAudioProceed(): Boolean{
        if(isGaming && audioAllowTime <= SystemClock.uptimeMillis()){
            audioAllowTime = SystemClock.uptimeMillis() + audioInterval
            return true
        }
        else {
            return false
        }
    }
    fun setResult(text: String){
        Log.d("Control", result)
        result = text
        Log.d("Control",result)
        //Collects the state of model reading ins.
    }

    fun allowRun(): Boolean {
        if(allowTime <= SystemClock.uptimeMillis()){
            allowTime = SystemClock.uptimeMillis() + reloadInterval
            return true
        }
        else {
            return false
        }
    }

    fun setReloadTime(time: Long){
        reloadInterval = time
    }
    companion object {
        // Singleton instance
        @Volatile
        private var instance: ModelController? = null

        // Get the instance of the ModelManager class
        fun getInstance(): ModelController {
            return instance ?: synchronized(this) {
                instance ?: ModelController().also { instance = it }
            }
        }
    }
}