package com.example.pj4test.controller

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import java.util.*
import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
///import androidx.core.content.ContentProviderCompat.requireContext

class ModelController private constructor() {
    private val HIGH_AUDIO_INTERVAL = 200L
    private val LOW_AUDIO_INTERVAL = 80L
    private val HIGH_IMAGE_INTERVAL = 500L
    private val LOW_IMAGE_INTERVAL = 100L

    private val GAME_THRESHOLD = 0.3F
    private val FOOT_THRESHOLD = 0.5F
    private val IMAGE_THRESHOLD = 0.8F

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
    private var isCharging = false

    private fun calculateAverage(vec: Vector<Float>): Float{
        var sum: Float = 0F
        if(vec.size == 0) return 0F
        for(i in vec){
            sum += i
        }
        return sum/vec.size
    }

    fun getIntervalInfo():String{
        return "Audio Interval: $audioInterval, Image Interval: $imageInterval"
    }
    fun setGamingScore(score: Float){
        if(gamingTimeWindow.size >= 3){
        gamingTimeWindow = Vector<Float>(gamingTimeWindow.slice(gamingTimeWindow.size-3..gamingTimeWindow.size-1))}
        gamingTimeWindow.addElement(score)
        isGaming = calculateAverage(gamingTimeWindow) > GAME_THRESHOLD
    }
    fun needBindCamera(): Boolean{
        //Log.d("Controller", "WHY ${isGaming||isFootstep}")
        return (calculateAverage(gamingTimeWindow) > GAME_THRESHOLD || calculateAverage(footStepTimeWindow) > FOOT_THRESHOLD)
    }

    fun setPersonScore(score: Float){
        personScore = score
        if(imageTimeWindow.size >= 10){
            imageTimeWindow = Vector<Float>(
                imageTimeWindow.slice(imageTimeWindow.size-10..imageTimeWindow.size-1)) }
        imageTimeWindow.addElement(score)
        if(calculateAverage(imageTimeWindow) > IMAGE_THRESHOLD) { imageInterval = LOW_IMAGE_INTERVAL }
        else { imageInterval = HIGH_IMAGE_INTERVAL }
    }

    fun setCharge(charge : Boolean) {
        isCharging = charge
    }

    fun getStatus():String {
        //Log.d("Controller", "WHY ${calculateAverage(gamingTimeWindow)} isgame: ${isGaming}")
        val game_avg = calculateAverage(gamingTimeWindow)
        val foot_avg = calculateAverage(footStepTimeWindow)
        if(game_avg > GAME_THRESHOLD || foot_avg > FOOT_THRESHOLD) { return "ACTIVE" }
        else {return "inactive Game:"+game_avg.toString()+" Foot: "+ foot_avg.toString()}
    }

    fun setFootStepScore(score: Float){
        //Log.d("Controller", "In set score")
        if(footStepTimeWindow.size >= 3){ footStepTimeWindow = Vector<Float>(footStepTimeWindow.slice(footStepTimeWindow.size-3..footStepTimeWindow.size-1)) }
        footStepTimeWindow.addElement(score)
        if(calculateAverage(footStepTimeWindow) > FOOT_THRESHOLD) {
            isFootstep = true
            imageInterval = LOW_IMAGE_INTERVAL
        }
        else {
            isFootstep = false
            imageInterval = HIGH_IMAGE_INTERVAL
        }
        if(isFootstep||isGaming) audioInterval = LOW_AUDIO_INTERVAL
        else audioInterval = HIGH_AUDIO_INTERVAL
    }

    fun allowCameraProceed(): Boolean{
        if(imageAllowTime <= SystemClock.uptimeMillis() || isCharging){
            imageAllowTime = SystemClock.uptimeMillis() + imageInterval
            return true
        }
        else {
            return false
        }
    }
    fun allowAudioProceed(): Boolean{
        if(audioAllowTime <= SystemClock.uptimeMillis() || isCharging){
            audioAllowTime = SystemClock.uptimeMillis() + audioInterval
            return true
        }
        else {
            return false
        }
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