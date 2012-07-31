package org.bigsleep.spacemission

import android.util.Log
import org.bigsleep.geometry.Vec2

class GameModel(private val onAddCharacter : (Character) => Unit)
{
    Log.d("bigsleep", "GameModel create")
    private val player = new Player
    onAddCharacter(player)
    
    private var playerShots = collection.mutable.ListBuffer.empty[Shot]
    private var enemys = collection.mutable.ListBuffer.empty[Character]
    private var enemyShots = collection.mutable.ListBuffer.empty[Character]
    private val stageStateMachine = new StageStateMachine(player, this.addEnemy _, this.addEnemyShot _)
    
    private val touchState = new TouchState
    
    val colHandler = new CollisionHandler(GameMain.width, GameMain.height, 64d)
    
    val shotTiming = OnceInN(GameMain.FPS / 5)
    val enemyCollisionTiming = OnceInN(1)
    val playerCollisionTiming = OnceInN(1)
    def update : Unit =
    {
        player.update
        playerShots.foreach(_.update)
        enemys.foreach(_.update)
        enemyShots.foreach(_.update)
        stageStateMachine.update
        
        // add player shot
        if(shotTiming.apply){
            val ev = touchState.eventType
            if(ev == TouchEventType.DOWN || ev == TouchEventType.MOVE){
                val p = player.motion.position + Vec2(0d, 60d)
                val v = Vec2(0d, 12d)
                val r = 10d
                val s = new ShotYellow(p, v, r)
                playerShots += s
                onAddCharacter(s)
            }
        }
        
        // collision
        colHandler.clear()
        playerShots.foreach{x => colHandler.registerPlayerShot(x)}
        enemys.foreach{x => colHandler.registerEnemy(x)}
        enemyShots.foreach{x => colHandler.registerEnemyShot(x)}
        colHandler.apply(player)
        
        enemys.foreach{x =>
            if(x.getHP <= 0) player.addScore(x.getPoint)
        }
        
        playerShots = playerShots.filter(_.active)
        enemys = enemys.filter(_.active)
        enemyShots = enemyShots.filter(_.active)
    }
    
    def addEnemy(c : Character) : Unit =
    {
        enemys += c
        onAddCharacter(c)
    }
    
    def addEnemyShot(c : Character) : Unit =
    {
        enemyShots += c
        onAddCharacter(c)
        Log.d("bigsleep", "addEnemyShot " + enemyShots.size)
    }
    
    val pxMin = 80d
    val pxMax = GameMain.width - 80d
    val pyMin = 80d
    val pyMax = GameMain.height - 80d
    def onTouchEvent(ev : TouchEvent) : Unit =
    {
        ev.eventType match{
            case TouchEventType.DOWN =>
            {
                touchState.eventType = TouchEventType.DOWN
            }
            case TouchEventType.MOVE =>
            {
                val dx = ev.x - touchState.x
                val dy = ev.y - touchState.y
                val x = math.min(math.max(pxMin, player.motion.position(0) + dx), pxMax)
                val y = math.min(math.max(pyMin, player.motion.position(1) + dy), pyMax)
                player.motion.position = Vec2(x, y)
            }
            case _ =>
            {
                touchState.eventType = TouchEventType.NON
            }
        }
        touchState.x = ev.x
        touchState.y = ev.y
    }
    
    def getPlayer = player
    
    def getScore : Int = player.getScore
}

class TouchEvent(val eventType : TouchEventType, val x : Double, val y : Double)

class TouchEventType
object TouchEventType
{
    val DOWN, MOVE, UP, OUTSIDE, NON = new TouchEventType
}

class TouchState
{
    var eventType = TouchEventType.NON
    var x = 0d
    var y = 0d
    var px = 0d
    var py = 0d
    var downx = 0d
    var downy = 0d
    var tap = false
}

case class OnceInN(n : Int)
{
    val number = math.max(n, 1)
    private var count = 0
    
    def apply() : Boolean =
    {
        if(count == number - 1){
            count = 0
            true
        }else{
            count += 1
            false
        }
    }
}


