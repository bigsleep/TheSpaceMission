package org.bigsleep.spacemission

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.opengl.GLSurfaceView
import android.view.View
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams
import android.content.Context
import android.content.res.Resources
import android.util.Log

import javax.microedition.khronos.opengles.GL10

import org.bigsleep.android.view.{ResourceImageDrawer, BackgroundDrawer, CutOffAnimation}
import org.bigsleep.android.view.GLRenderer
import org.bigsleep.geometry.{Vec2, Circle}

class MainActivity extends Activity with TypedActivity
{
    var view : android.widget.FrameLayout = null
    
    override def onCreate(bundle: Bundle) {
        super.onCreate(bundle)
        view = new android.widget.FrameLayout(this)
        GameMain.onActivityCreate(this)
        setContentView(view)
        Log.d("bigsleep", "MainActivity.onCreate")
    }
    
    override def onStart() : Unit =
    {
        Log.d("bigsleep", "MainActivity.onStart");
        super.onStart()
        GameMain.onStart()
    }
    
    override def onRestart() : Unit =
    {
        Log.d("bigsleep", "MainActivity.onRestart");
        super.onRestart()
    }
    
    override def onDestroy() : Unit =
    {
        Log.d("bigsleep", "MainActivity.onDestroy");
        super.onDestroy()
    }
    
    override def onBackPressed : Unit =
    {
        Log.d("bigsleep", "MainActivity.onBackPressed")
        GameMain.post{() => GameMain.processEvent(BackPressed())}
        Log.d("bigsleep", "MainActivity.onBackPressed end")
    }
    
    override def onResume : Unit =
    {
        Log.d("bigsleep", "MainActivity.onResume")
        super.onResume()
        GameMain.onResume()
    }
    
    override def onPause : Unit =
    {
        Log.d("bigsleep", "MainActivity.onPause")
        super.onPause()
        GameMain.onPause()
    }

    override def onTouchEvent(ev : MotionEvent) : Boolean =
    {
        GameMain.onTouchEvent(ev)
        super.onTouchEvent(ev)
    }
    
    def addView(v : View) : Unit =
    {
        Log.d("bigsleep", "MainActivity.addView")
        view.addView(v)
    }
    
    def removeView(v : View) : Unit =
    {
        Log.d("bigsleep", "MainActivity.removeView")
        view.removeView(v)
    }
    
    def invalidateView() : Unit =
    {
        view.invalidate
    }
    
    private var windowWidth : Int = -1
    private var windowHeight : Int = -1
    def getWindowWidth : Int =
    {
        if(windowWidth < 0){
            windowWidth = getWindowManager.getDefaultDisplay.getWidth
        }
        windowWidth
    }
    def getWindowHeight : Int =
    {
        if(windowHeight < 0){
            windowHeight = getWindowManager.getDefaultDisplay.getHeight
        }
        windowHeight
    }
}

object GameMain
{
    Log.d("bigsleep", "GameMain initialize")
    val width = 720
    val height = 1280
    val margin = 100
    val marginY = 200
    val FPS = 30
    
    private var activity : MainActivity = null
    private var packageName : String = null
    private var res : Resources = null
    private var gameStateMachine : GameStateMachine = null
    private var random : scala.util.Random = null
    private var cutOffIds = collection.immutable.Map.empty[String, Array[Int]]
    
    def onActivityCreate(a : MainActivity) : Unit =
    synchronized{
        activity = a
        packageName = a.getPackageName
        res = a.getResources
        
        val d = a.getWindowManager.getDefaultDisplay
        random = new scala.util.Random(0)
        
        Log.d("bigsleep", "GameMain.onActivityCreate")
        ResourceImageDrawer.loadImages(a, res.getStringArray(R.array.drawables))
        initializeCutOffAnimationIds()
        
        gameStateMachine = new GameStateMachine(a)
    }
    
    private def initializeCutOffAnimationIds() : Unit =
    {
        val cutOffNames = res.getStringArray(R.array.cutoffanimations)
        cutOffNames.foreach{s =>
            val imageNames = res.getStringArray(res.getIdentifier(s, "array", packageName))
            val imageIds = imageNames.map{x => res.getIdentifier(x, "drawable", packageName)}
            cutOffIds = cutOffIds.updated(s, imageIds)
        }
    }
    
    def getCutOffAnimationIds(s : String) : Array[Int] = synchronized{cutOffIds(s)}
    
    def getBackgroundIds : Array[Int] =
    synchronized{
        val name = "background"
        val imageNames = res.getStringArray(res.getIdentifier(name, "array", packageName))
        val imageIds = imageNames.map{x => res.getIdentifier(x, "drawable", packageName)}
        imageIds
    }
    
    def onStart() : Unit =
    synchronized{
    }
    
    def onTouchEvent(ev : MotionEvent) : Unit =
    synchronized{
        gameStateMachine.onTouchEvent(ev)
    }

    def getWidth : Int = width
    def getHeight : Int = height
    
    def getWindowWidth = synchronized{ activity.getWindowWidth }
    def getWindowHeight = synchronized{ activity.getWindowHeight }
    
    def addView(v : View) : Unit = activity.addView(v)
    def removeView(v : View) : Unit = activity.removeView(v)
    
    def processEvent(ev : Event) : Unit =
    synchronized{
        gameStateMachine.processEvent(ev)
    }
    
    def onResume() : Unit =
    synchronized{
        gameStateMachine.onResume
    }
    
    def onPause() : Unit =
    synchronized{
        gameStateMachine.onPause
    }
    
    def inField(p : Vec2[Double]) : Boolean =
    {
        p.x >= - margin && p.x <= width + margin &&
        p.y >= - marginY && p.y <= height + marginY
    }
    
    override def finalize() : Unit =
    {
        Log.d("bigsleep", "GameMain.finalize")
        super.finalize
    }
    
    def getNextRDouble = synchronized{random.nextDouble}
    def getNextRInt(n : Int) = synchronized{random.nextInt(n)}
    
    def post(r : () => Unit) : Unit =
    synchronized{
        val h = new android.os.Handler
        h.post(new Runnable{
            def run : Unit =
            {
                GameMain.synchronized{ r.apply }
            }
        })
    }
    
    def runOnUiThread(r : () => Unit) : Unit =
    synchronized{
        activity.runOnUiThread(new Runnable{
            def run : Unit =
            {
                GameMain.synchronized{ r.apply }
            }
        })
    }
    
    def invalidateView() : Unit =
    synchronized{ activity.invalidateView }
}





