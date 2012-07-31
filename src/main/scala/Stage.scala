package org.bigsleep.spacemission

import android.util.Log
import org.bigsleep.geometry._

class StageStateMachine(
    private val player : Player,
    private val addCharacter : (Character) => Unit,
    private val addShot : (Character) => Unit)
{
    private var stage : Stage = new Stage1(player, addCharacter, addShot)
    
    def update() : Unit =
    {
        stage.update
        val ev = stage.checkEvent
        processEvent(ev)
    }
    
    def processEvent(ev : StageEvent) : Unit =
    {
        (stage, ev) match
        {
            case (x, Stage1BossStart()) => stage = new Stage1Boss(player, addCharacter, addShot)
            case _ => {}
        }
    }
}

abstract class StageEvent
case class NoStageEvent() extends StageEvent
case class Stage1BossStart() extends StageEvent

abstract class Stage
{
    def update() : Unit = {}
    def checkEvent : StageEvent = NoStageEvent()
}

class Stage1(
    private val player : Player,
    private val addCharacter : (Character) => Unit,
    private val addShot : (Character) => Unit) extends Stage
{
    val fps = GameMain.FPS
    val width = GameMain.width.toDouble
    val height = GameMain.height.toDouble
    val margin = GameMain.margin.toDouble
    val marginY = GameMain.marginY.toDouble
    val random = scala.util.Random
    private var currentEvent : StageEvent = NoStageEvent()
    
    val g = new org.bigsleep.util.Generator[Unit]{
        
        def body =
        {
            phase1
            phase2
            phase3
            phase2
            phase4
            phase5
            phase2
            phase4
            phase5
            phase6
            phase1
            phase6
            currentEvent = Stage1BossStart()
            generate()
        }
        
        def phase1 =
        {
            val add = addCharacter
            repeat(fps){
                generate()
            }
            
            {
                val p = Vec2(width * 0.5d, height + marginY)
                val v = Vec2(0d, -15d)
                val e = new Enemy01G(p, v, addShot, player)
                addCharacter(e)
                generate()
            }
            
            {
                val x1 = width * 0.3d
                val x2 = width * 0.5d
                val x3 = width * 0.7d
                val y = height + marginY
                val v = Vec2(0d, -15d)
                repeat(3){
                    repeat(fps){
                        generate()
                    }
                    val e1 = new Enemy01G(Vec2(x1, y), v, addShot, player)
                    val e2 = new Enemy01G(Vec2(x2, y), v, addShot, player)
                    val e3 = new Enemy01G(Vec2(x3, y), v, addShot, player)
                    addCharacter(e1)
                    addCharacter(e2)
                    addCharacter(e3)
                    generate()
                }
            }
        }
        
        def phase2 =
        {
            repeat(fps){
                generate()
            }
            repeat(3){
                var x = width * 0.3d
                val y = height + marginY
                val v = Vec2(0d, -15d)
                
                repeat(3){
                    val p0 : Vec2[Double] = Vec2(x, y)
                    repeat(4){
                        repeat(fps / 4){
                            generate()
                        }
                        val e = new Enemy01G(p0, v, addShot, player)
                        addCharacter(e)
                        generate()
                    }
                    x += width * 0.2d
                    
                    repeat(fps){
                        generate()
                    }
                }
            }
        }
        
        def phase3 =
        {
            var n = 0
            val mid = width * 0.5d
            val x1 = Seq(mid - 256d, mid - 128d, mid)
            val x2 = Seq(mid, mid + 128d, mid + 256d)
            val y = height + marginY
            val y1 = height * 0.7d
            val v = 15d
            repeat(4){
                {
                    val x = if((n % 2) == 0) x1 else x2
                    x.foreach{a => addCharacter(new Enemy01R(Vec2(a, y), addShot, player, v, Vec2(a, y1), Vec2(mid, y1)))}
                    generate()
                }
                repeat(fps * 2){
                    generate()
                }
                
                {
                    val x = if((n % 2) != 0) x1 else x2
                    x.foreach{a => addCharacter(new Enemy01R(Vec2(a, y), addShot, player, v, Vec2(a, y1), Vec2(mid, y1)))}
                    generate()
                }
                repeat(fps * 2){
                    generate()
                }
                n += 1
            }
        }
        
        def phase4 =
        {
            var n = 0
            val x1 = width * 0.25d
            val x2 = width * 0.75d
            val y = height + marginY
            val a1 = 0d
            val a2 = math.Pi
            val count = fps / 3
            repeat(4){
                repeat(fps){
                    generate()
                }
                val x = if(n % 2 == 0) x1 else x2
                var m = 0
                repeat(3){
                    repeat(count){
                        generate()
                    }
                    val p = Vec2(x, y)
                    val a = if(m % 2 == 0) a1 else a2
                    addCharacter(new EnemyUFO(p, a, addShot))
                    generate()
                    m += 1
                }
                n += 1
            }
            repeat(fps){
                generate()
            }
        }
        
        def phase5 =
        {
            var n = 0
            val x0 = width * 0.5d
            val dx = width * 0.2d
            val y = height + marginY
            val v = Vec2(0d, -12d)
            repeat(3){
                repeat(fps){
                    generate()
                }
                if(n > 1){
                    (1 to n).foreach{i =>
                        if(i == 0){
                            val p = Vec2(x0, y)
                            addCharacter(new EnemyCpp(p, v, addShot, player))
                        }else{
                            val p1 = Vec2(x0 - dx * i.toDouble, y)
                            val p2 = Vec2(x0 + dx * i.toDouble, y)
                            addCharacter(new EnemyCpp(p1, v, addShot, player))
                            addCharacter(new EnemyCpp(p2, v, addShot, player))
                        }
                    }
                }
                generate()
                n += 1
            }
        }
        
        def phase6 =
        {
            repeat(2){
                repeat(fps * 3){
                    generate()
                }
                
                val x = (width + margin * 2) * random.nextDouble - margin
                val y = height + marginY
                addCharacter(new Enemy02(Vec2(x, y), addShot, player))
            }
        }
    }
    
    var meteorTiming = OnceInN(fps * 2 + random.nextInt(fps))
    
    override def update() : Unit =
    {
        if(g.hasNext) g.next
        if(meteorTiming.apply){
            createMeteor
            meteorTiming = OnceInN(fps * 2 + random.nextInt(fps))
        }
    }
    
    override def checkEvent : StageEvent = currentEvent
    
    def createMeteor
    {
        val speed = 30d
        val x = (width + margin * 2) * random.nextDouble - margin
        val y = height + marginY
        
        val p = player.motion.position
        val vec0 = p - Vec2(x, y)
        val vec = vec0 * (speed / vec0.norm)
        val r = 100d * random.nextDouble + 60d
        val maxAnglev = math.Pi * 5d / fps.toDouble
        val a = (random.nextDouble - 0.5d) * 2d * maxAnglev
        val e : Character = new EnemyMeteor(Vec2(x, y), a, vec, r)
        addCharacter(e)
    }
}

class Stage1Boss(
    val player : Player,
    val addCharacter : (Character) => Unit,
    val addShot : (Character => Unit)) extends Stage
{
    val fps = GameMain.FPS
    val width = GameMain.width.toDouble
    val height = GameMain.height.toDouble
    val margin = GameMain.margin.toDouble
    val marginY = GameMain.marginY.toDouble
    val random = scala.util.Random
    
    val g = new org.bigsleep.util.Generator[Unit]{
        def body =
        {
            repeat(fps * 5){ generate() }
            val boss = new Boss(player, addShot)
            addCharacter(boss)
            boss.arms.foreach{arm =>
                arm.parts.foreach{x => addCharacter(x)}
            }
            doWhile(boss.active){ generate() }
            repeat(fps * 2){ generate() }

            GameMain.runOnUiThread{() => GameMain.processEvent(GameClear())}
            doWhile(true){ generate() }
        }
    }
    
    override def update() : Unit =
    {
        if(g.hasNext) g.next
    }
}



