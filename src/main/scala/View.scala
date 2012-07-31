package org.bigsleep.spacemission

import android.util.Log
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.content.Context
import javax.microedition.khronos.opengles.GL10

import org.bigsleep.android.view.{GLRenderer, Drawer, ResourceImageDrawer, BackgroundDrawer}

class PlayView(ctx : Context)
{
    Log.d("bigsleep", "PlayView created")
    
    private var drawables = collection.mutable.ListBuffer.empty[Drawer]
    private var model = new GameModel(this.addCharacter _)
    private val glSurfaceView = new GLSurfaceView(ctx)
    
    private var time : Long = 0L
    private var oldtime : Long = -1L
    private val scrollSpeed = 5d
    private val background = new BackgroundDrawer(
        GameMain.getBackgroundIds, GameMain.getWidth.toDouble, GameMain.getHeight.toDouble, scrollSpeed)
    private var playStatus = new PlayStatusDrawer(model)
    
    private val renderer = new GLRenderer(GameMain.width, GameMain.height.toDouble)
    renderer.setOnDraw(this.onDraw _)
    glSurfaceView.setRenderer(renderer)
    glSurfaceView.setRenderMode(RENDERMODE_CONTINUOUSLY)
    
    private var running = false
    def pauseGame() : Unit =
    {
        model.onTouchEvent(new TouchEvent(TouchEventType.NON, 0d, 0d))
        running = false
        oldtime = -1L
    }
    def resumeGame() : Unit = running = true
    def initializeGame() : Unit =
    {
        drawables = collection.mutable.ListBuffer.empty[Drawer]
        time = 0L
        oldtime = -1L
        background.setPosition(0d)
        model = new GameModel(this.addCharacter _)
        playStatus = new PlayStatusDrawer(model)
    }
    def startGame() : Unit =
    {
        running = true
    }
    def stopGame() : Unit =
    {
        running = false
        oldtime = -1L
    }
    
    def getModel = model
    
    private val delayTime = (1000d / GameMain.FPS.toDouble).toInt
    val logTiming = OnceInN(30)
    val logTiming2 = OnceInN(30)
    def onDraw(gl : GL10) : Unit =
    {
        //if(logTiming.apply) Log.d("bigsleep", "PlayView.onDraw")
        val cur = android.os.SystemClock.uptimeMillis
        gl.glClearColor(1f, 1f, 1f, 1f)
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT)
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        
        gl.glLoadIdentity()
        background.setWidth(renderer.getWidth)
        background.setHeight(renderer.getHeight)
        
        GameMain.synchronized{
            if(running){
                if(oldtime < 0) oldtime = cur
                time += cur - oldtime
                oldtime = cur
                playStatus.setTime(time)
                playStatus.update
                background.update
                drawables.foreach(_.update)
                model.update
                
                if(model.getPlayer.getHP <= 0){
                    GameMain.runOnUiThread{() => GameMain.processEvent(GameOver())}
                    stopGame()
                }
            }
            
            background.apply(gl)
            drawables.foreach(_.apply(gl))
            drawables = drawables.filter({(x : Drawer) => x.active})
            playStatus.apply(gl)
        }
        
        val end = android.os.SystemClock.uptimeMillis
        val duration = math.max(delayTime - (end - cur), 0)
        java.lang.Thread.sleep(duration)
        //if(logTiming2.apply) Log.d("bigsleep", "PlayView.onDraw end")
    }
    
    def update() : Unit = background.update()
    
    def render() : Unit = glSurfaceView.requestRender()
    
    def addDrawable(a : Drawer) : Unit = drawables += a

    def addCharacter(a : Character) : Unit =
    {
        a match{
            case x : Player => drawables += createDrawer(x, R.drawable.rocket)
            case x : Shot =>
            {
                val id = x match{
                    case y : ShotYellow => R.drawable.shotyellow
                    case y : ShotRed => R.drawable.shotred
                    case y : ShotGreen => R.drawable.shotgreen
                    case y : ShotBlue => R.drawable.shotblue
                }
                val rid = new ResourceImageDrawer(id)
                val scale = (x.radius * 2.0) / rid.getWidth.toDouble
                rid.setScale(scale)
                val drawer = new BasicCharacterDrawer(x, rid)
                drawables += drawer
            }
            case x : Enemy01G => drawables += createDrawer(x, R.drawable.e01g)
            case x : Enemy01R => drawables += createDrawer(x, R.drawable.e01r)
            case x : Enemy02 => drawables += createDrawer(x, R.drawable.e02)
            case x : EnemyCpp => drawables += createDrawer(x, R.drawable.c)
            case x : EnemyPlus =>
            {
                val d = createDrawer(x, R.drawable.plus)
                d.setScale(2d)
                drawables += d
            }
            case x : EnemyUFO => drawables += createDrawer(x, R.drawable.e04ufo)
            case x : EnemyMeteor => drawables += new EnemyMeteorDrawer(x)
            case x : Boss =>
            {
                x.arms.foreach{arm =>
                    arm.parts.foreach{p =>
                        val pdrawer = createDrawer(p, R.drawable.boss01)
                        pdrawer.setScale(p.radius * 2d / pdrawer.getWidth.toDouble)
                        drawables += pdrawer
                    }
                }
                
                val d = createDrawer(x, R.drawable.boss01)
                d.setScale(x.radius * 2d / d.getWidth.toDouble)
                drawables += d
            }
            case _ => Log.d("bigsleep", "PlayView addCharacter unknown Character")
        }
        Log.d("bigsleep", "PlayView.addCharacter " + drawables.size)
    }
    
    private def createDrawer(c : Character, id : Int) : BasicCharacterDrawer = new BasicCharacterDrawer(c, new ResourceImageDrawer(id))
    
    def getAndroidView : GLSurfaceView = glSurfaceView
    
    def onPause() : Unit =
    {
        running = false
        oldtime = -1
        model.onTouchEvent(new TouchEvent(TouchEventType.NON, 0d, 0d))
    }
    
    def onResume() : Unit =
    {
        running = true
    }
    
    def onTouchEvent(ev : TouchEvent) : Unit = model.onTouchEvent(ev)
    
    def getTime : Long = time
    
    def getScore : Int = model.getScore
}



