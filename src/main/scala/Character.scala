package org.bigsleep.spacemission

import android.util.Log
import org.bigsleep.geometry._

abstract class Character
{
    val motion : MotionState
    val shape : Shape
    private var hp = maxHP
        
    def update() : Unit = {}
    
    def active : Boolean = { (hp > 0) && GameMain.inField(motion.position) }
    
    def hit(b : Character) : Boolean =
    {
        if(!b.active) false
        val col = new Collision(motion, b.motion)
        val h = col.apply(shape, b.shape)
        if(h){
            damage(b.attackAbility)
            b.damage(attackAbility)
        }
        h
    }
    
    def maxHP : Int
    def attackAbility : Int = 1
    
    def getHP = hp
    
    def damage(d : Int) : Unit = hp = math.max(hp - d, 0)
    
    def getPoint : Int = 0
    
    def large = false
}

class Player extends Character
{
    val motion = new MotionState(Vec2(GameMain.getWidth / 2, GameMain.getHeight / 2), 0d)
    val shape = Circle(Vec2(0d, 0d), 30d)
    
    override def active : Boolean  = true
    
    def getPosition = motion.position
    
    //override def damage(d : Int) : Unit = {}
    
    override def maxHP = 40
    
    private var score = 0
    
    def addScore(s : Int) = score += s
    def getScore = score
}


abstract class Shot(
    val p0 : Vec2[Double],
    val v : Vec2[Double],
    val radius : Double) extends Character
{
    private var active_ = true
    
    def maxHP = 1
    
    override def active = (active_ && super.active)
    
    def setInactive() = active_ = false
    
    val motion = new MotionState(p0, 0d)
    
    val shape = Circle(Vec2(0d, 0d), radius)
    
    override def update() : Unit =
    {
        motion.position = motion.position + v
    }
}

class ShotYellow(p0_ : Vec2[Double], v_ : Vec2[Double], r_ : Double) extends Shot(p0_, v_, r_)
class ShotRed(p0_ : Vec2[Double], v_ : Vec2[Double], r_ : Double) extends Shot(p0_, v_, r_)
class ShotGreen(p0_ : Vec2[Double], v_ : Vec2[Double], r_ : Double) extends Shot(p0_, v_, r_)
class ShotBlue(p0_ : Vec2[Double], v_ : Vec2[Double], r_ : Double) extends Shot(p0_, v_, r_)

class EnemyCpp(
    val p0 : Vec2[Double],
    val v : Vec2[Double],
    val addShot : (Character => Unit),
    val player : Player) extends Character
{
    Log.d("bigsleep", "EnemyCpp create")
    val motion = new MotionState(p0, 0d)
    
    val shape = Circle(Vec2(0d, 0d), 62d)
    
    def maxHP = 3
    override def attackAbility = 2
    
    val fps = GameMain.FPS
    
    val g = new org.bigsleep.util.Generator[Unit]{
        Log.d("bigsleep", "Generator create")
        
        def body =
        {
            repeat(fps / 2){
                generate()
            }
            doWhile(true){
                val p = player.getPosition
                val vec = p - motion.position
                val theta = scala.math.atan2(vec(0), -vec(1))
                val n2 = fps / 5
                val dtheta = (theta - motion.angle) / n2.toDouble
                repeat(n2){
                    motion.angle += dtheta
                    generate()
                }
                
                repeat(fps / 2){
                    generate()
                }
                
                val vx = 30d * math.sin(motion.angle)
                val vy = - 30d * math.cos(motion.angle)
                val plus1 = new EnemyPlus(motion.position, motion.angle, Vec2(vx, vy))
                addShot(plus1)
                generate()
                
                repeat(fps / 5){
                    generate()
                }
                
                val plus2 = new EnemyPlus(motion.position, motion.angle, Vec2(vx, vy))
                addShot(plus2)
                generate()
                
                repeat(fps * 2 / 3){
                    generate()
                }
            }
        }
    }
    
    override def update() : Unit =
    {
        motion.position += v
        if(g.hasNext) g.next
    }
    
    override def getPoint = 200
}

class EnemyPlus(
    val p0 : Vec2[Double],
    val a : Double,
    val v : Vec2[Double]) extends Character
{
    val motion = new MotionState(p0, a)
    
    /*
    object PlusShape extends ShapeGroup(Array(
        Line(Vec2(-64d, 0d), Vec2(64d, 0d)),
        Line(Vec2(0d, -64d), Vec2(0d, 64d))))
    */
    
    val shape = Circle(Vec2(0d, 0d), 12d)
    
    def maxHP = 2
    
    override def update() : Unit =
    {
        motion.position = motion.position + v
    }
}

class EnemyUFO(val p0 : Vec2[Double], val theta0 : Double, val addShot : (Character) => Unit)
    extends Character
{
    val motion = new MotionState(p0, 0d)
    
    val shape = Circle(Vec2(0d, 0d), 52.5d)
    
    def maxHP = 4
    override def attackAbility = 3
    
    val fps = GameMain.FPS
    val pi2 = math.Pi * 2d
    val vy = - 10d
    val A = 160d
    val omega = (pi2 / GameMain.FPS.toDouble) * 0.5d
    var theta = theta0
    while(theta >= pi2) theta -= pi2
    
    val R = Mat2.rotationM(math.Pi / 6d)
    
    val t = OnceInN(fps * 3 / 4)
    var shotv = Vec2(0d, -15d)
    override def update() : Unit =
    {
        val x = p0(0) + A * math.cos(theta)
        val y = motion.position(1) + vy
        motion.position = Vec2(x, y)
        theta += omega
        if(theta >= pi2) theta = 0d
        
        if(t.apply){
            val p = motion.position + Vec2(0d, -22d)
            val r = 10d
            addShot(new ShotRed(p, shotv, r))
            shotv = shotv * R
        }
    }
    
    override def getPoint = 300
}

class EnemyMeteor(
    val p0 : Vec2[Double],
    val a : Double,
    val v : Vec2[Double],
    val radius : Double) extends Character
{
    val motion = new MotionState(p0, 0d)
    
    val shape = Circle(Vec2(0d, 0d), radius)
    
    override def update() : Unit =
    {
        motion.position += v
        motion.angle += a
    }
    
    override def maxHP = 5
    override def attackAbility = 2
    
    override def active : Boolean =
    {
        val p = motion.position
        (getHP > 0) &&
        (p(0) >= - radius) &&
        (p(0) <= GameMain.width + radius) &&
        (p(1) <= GameMain.height + GameMain.marginY) &&
        (p(1) >= - radius)
    }
    
    override def large = (radius >= 64d)
    
    override def getPoint = 150
}


abstract class Enemy01(val p0 : Vec2[Double]) extends Character
{
    val motion = new MotionState(p0, 0d)
    
    val shape = Circle(Vec2(0d, 12d), 40d)
    
    def radius = 40d
}

class Enemy01G(p0 : Vec2[Double], v : Vec2[Double], addShot : (Character) => Unit, player : Player)
    extends Enemy01(p0)
{
    override def maxHP = 1
    override def attackAbility = 2
    
    val R = Mat2.rotationM(math.Pi / 6d)
    
    val t = OnceInN(GameMain.FPS * 4 / 3)
    override def update() : Unit =
    {
        motion.position += v
        if(t.apply){
            val p = motion.position + Vec2(0d, -20d)
            val v1 = Vec2(0d, -10d)
            val v2 = R * v1
            val v3 = v1 * R
            val r = 8d
            addShot(new ShotRed(p, v1, r))
            addShot(new ShotRed(p, v2, r))
            addShot(new ShotRed(p, v3, r))
        }
    }
    
    override def getPoint = 100
}

class Enemy01R(
    p0 : Vec2[Double],
    addShot : (Character) => Unit,
    player : Player,
    v : Double,
    p1 : Vec2[Double],
    p2 : Vec2[Double])
    extends Enemy01(p0)
{
    Log.d("bigsleep", "Enemy01R created")
    
    override def maxHP = 3
    override def attackAbility = 3
    
    val g = new org.bigsleep.util.Generator[Unit]{
        Log.d("bigsleep", "Generator create")
        
        def body =
        {
            val vec = p1 - p0
            val d = vec.norm
            val count1 = (d / v).toInt
            val velo = vec / count1.toDouble
            repeat(count1){
                motion.position += velo
                generate()
            }
            
            val vec2 = p2 - motion.position
            val d2 = vec2.norm
            val count2 = (d2 * 2d / v).toInt
            val velo2 = vec2 / count2.toDouble
            repeat(count2){
                motion.position += velo2
                generate()
            }
            
            doWhile(true){
                motion.position += velo
                generate()
            }
        }
    }
    
    val t = OnceInN(GameMain.FPS * 8 / 10)
    override def update() : Unit =
    {
        if(g.hasNext) g.next
        if(t.apply){
            val p = motion.position + Vec2(0d, -20d)
            val v = (player.getPosition - p)
            val ve = v * (15d / v.norm)
            val r = 8d
            val s = new ShotRed(p, ve, r)
            addShot(s)
        }
    }
    
    override def getPoint = 200
}

class Enemy02(
    val p0 : Vec2[Double],
    val addShot : (Character => Unit),
    val player : Player) extends Character
{
    Log.d("bigsleep", "EnemyCpp create")
    val motion = new MotionState(p0, 0d)
    
    val shape = ShapeGroup(Array(
        Circle(Vec2(0d, -18d), 30d),
        Circle(Vec2(0d, 48d), 40d),
        Circle(Vec2(0d, 120d), 32d)))
    
    def maxHP = 5
    override def attackAbility = 3
    
    override def large = true
    
    val fps = GameMain.FPS
    
    val g = new org.bigsleep.util.Generator[Unit]{
        def body =
        {
            val sz = 3 + GameMain.getNextRInt(3)
            val ps = new Array[Vec2[Double]](sz)
            (0 to sz - 1).foreach{i =>
                val x = 50d + GameMain.getNextRDouble * (GameMain.width - 100d)
                val y = 50d + GameMain.getNextRDouble * (GameMain.height - 100d)
                ps(i) = Vec2(x, y)
            }
            
            val it = ps.iterator
            val daMax = math.Pi / 16d
            var ve = Vec2(0d, -24d)
            doWhile(it.hasNext){
                val p = it.next
                val v1 = (p - motion.position)
                val theta = math.atan2(v1.x, - v1.y) - motion.angle
                val n1 = fps / 2
                val dtheta = theta / n1.toDouble
                repeat(n1){
                    motion.angle += dtheta
                    generate()
                }
                
                val angle = motion.angle
                repeat(fps){
                    val da = (GameMain.getNextRDouble - 0.5d) * 2d * daMax
                    val a = angle + da
                    motion.angle = a
                    generate()
                }
                
                val velo = 24d
                val n2 = (v1.norm / velo).toInt
                val dv = v1 / n2.toDouble
                ve = dv
                repeat(n2){
                    motion.position += dv
                    generate()
                }
            }
            
            doWhile(true){
                motion.position += ve
                generate()
            }
        }
    }
    
    val R = Mat2.rotationM(math.Pi / 4d)
    val t = OnceInN(fps)
    
    override def update() : Unit =
    {
        if(g.hasNext) g.next
        if(t.apply){
            val v = Vec2(0d, -18d).rotate(motion.angle)
            val p = motion.position + Vec2(0d, -64d)
            val R = Mat2.rotationM(math.Pi / 6d)
            val v1 = v
            val v2 = R * v1
            val v3 = v1 * R
            val r = 8d
            addShot(new ShotRed(p, v1, r))
            addShot(new ShotRed(p, v2, r))
            addShot(new ShotRed(p, v3, r))
        }
    }
    
    override def getPoint = 300
}
















