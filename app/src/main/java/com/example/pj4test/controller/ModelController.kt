package com.example.pj4test.controller

import android.util.Log
import android.os.SystemClock

class ModelController {
    private var result = ""
    private var reloadInterval = 0L
    private var allowTime = 0L
    fun onCreate(){
        result = "Not set yet"
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

}