package org.bigsleep.spacemission

import android.os.SystemClock

class FPSChecker(s : Int = 30)
{
    val sample = s
    var samples : collection.immutable.Seq[Double] = collection.immutable.Seq.empty[Double]
    private var curtime : Long = SystemClock.uptimeMillis
    private var oldtime : Long = -1
    
    def apply : Double =
    {
        curtime = SystemClock.uptimeMillis
        if(oldtime < 0) oldtime = curtime
        val dif = curtime - oldtime
        oldtime = curtime
        val a = (1000d / math.max(1d, dif.toDouble))
        samples = samples :+ a
        while(samples.size > sample){
            samples = samples.tail
        }
        if(samples.isEmpty){
            return 0.0
        }else{
            val a = samples.sum
            val average = a / samples.size.toDouble
            return average
        }
    }
}

