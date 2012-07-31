package org.bigsleep.spacemission

import org.bigsleep.geometry._

class Boss(val player : Player, val addShot : (Character => Unit)) extends Character
{
    val p0 = Vec2(GameMain.getWidth.toDouble * 0.5d, GameMain.getHeight.toDouble * 2d)
    val r0 = 80d
    val x0 = 5.65685425e+01
    
    val motion = new MotionState
    motion.position = p0
    val shape = Circle(Vec2(0d, 0d), 64d)
    def radius = shape.radius
    
    val ns1 = (0 to 5).map{i =>
            val x = p0.x + i.toDouble * x0
            val y = p0.y + i.toDouble * x0
            val m = new Motion
            m.position = Vec2(x, y)
            m
        }.toArray
    
    val ns2 = (0 to 5).map{i =>
            val x = p0.x - i.toDouble * x0
            val y = p0.y + i.toDouble * x0
            val m = new Motion
            m.position = Vec2(x, y)
            m
        }.toArray
    
    val ns3 = (0 to 5).map{i =>
            val x = p0.x - i.toDouble * x0
            val y = p0.y - i.toDouble * x0
            val m = new Motion
            m.position = Vec2(x, y)
            m
        }.toArray
    
    val ns4 = (0 to 5).map{i =>
            val x = p0.x + i.toDouble * x0
            val y = p0.y - i.toDouble * x0
            val m = new Motion
            m.position = Vec2(x, y)
            m
        }.toArray
    
    val arm1 = new BossArm(r0, player, ns1, addShot)
    val arm2 = new BossArm(r0, player, ns2, addShot)
    val arm3 = new BossArm(r0, player, ns3, addShot)
    val arm4 = new BossArm(r0, player, ns4, addShot)
    arm1.pushBossArm(arm2)
    arm1.pushBossArm(arm3)
    arm1.pushBossArm(arm4)
    arm2.pushBossArm(arm1)
    arm2.pushBossArm(arm3)
    arm2.pushBossArm(arm4)
    arm3.pushBossArm(arm1)
    arm3.pushBossArm(arm2)
    arm3.pushBossArm(arm4)
    arm4.pushBossArm(arm1)
    arm4.pushBossArm(arm2)
    arm4.pushBossArm(arm3)
    val arms = Array(arm1, arm2, arm3, arm4)
    
    val g = new org.bigsleep.util.Generator[Unit]{
        override def body =
        {
            val fps = GameMain.FPS
            
            val count1 = fps * 4
            val dd = GameMain.getHeight.toDouble * 1.2d / count1.toDouble
            repeat(count1){
                Boss.this.move(Vec2(0d, -dd))
                arms.foreach{x => x.update}
                generate()
            }
            
            val count2 = fps * 4
            val count3 = fps * 2
            val a = math.Pi * 4d
            val da = a / count2.toDouble
            val d2 = GameMain.getHeight.toDouble * 0.6d / count1.toDouble
            doWhile(true){
                repeat(count2){
                    Boss.this.move(Vec2(0d, -d2))
                    Boss.this.rotate(Boss.this.motion.position, da)
                    generate()
                }
                repeat(count3){
                    arms.foreach{x => x.update}
                    generate()
                }
                repeat(count2){
                    Boss.this.move(Vec2(0d, d2))
                    Boss.this.rotate(Boss.this.motion.position, -da)
                    generate()
                }
                repeat(count3){
                    arms.foreach{x => x.update}
                    generate()
                }
            }
        }
    }
    
    def move(v : Vec2[Double]) : Unit =
    {
        arms.foreach{x => x.move(v)}
        motion.position += v
    }
    
    def rotate(p : Vec2[Double], ang : Double) : Unit = arms.foreach{x => x.rotate(p, ang)}
    
    override def active = getHP > 0
    
    val shotTiming = new OnceInN(GameMain.FPS * 8)
    override def update() : Unit =
    {
        if(g.hasNext) g.next
        
        if(shotTiming.apply) addShots()
    }
    
    def maxHP = 50
    
    override def damage(d : Int) : Unit =
    {
        if(this.getHP > 0){
            super.damage(d)
            if(this.getHP == 0){
                arms.foreach{x => x.damage(x.getHP)}
            }
        }
    }
    
    override def attackAbility = 2
    
    override def getPoint : Int = 10000
    
    val shotV = (0 to 14).map{i =>
        val a = (i.toDouble / 15.toDouble) * 2d * math.Pi
        val v = Vec2(15d, 0d).rotate(a)
        v
    }
    def addShots() : Unit =
    {
        val p = motion.position
        val r = 8d
        shotV.foreach{v =>
            val s = new ShotBlue(p, v, r)
            addShot(s)
        }
    }
}

class Motion extends MotionState(Vec2(0d, 0d), 0d)
{
    var velocity = Vec2(0d, 0d)
    var force = Vec2(0d, 0d)
    var mass = 1d
}

class BossArm(
    val r0 : Double,
    val player : Player,
    val ns : Array[Motion],
    val addShot : (Character => Unit))
{
    val eps = 1.0E-1
    val vmax = 500d
    val nodes = ns.clone
    val p0 = ns(0).position
    var intaract = List.empty[BossArm]
    val parts = nodes.map{x => new BossArmPart(this, x)}
    
    var hp = maxHP
    def maxHP = 30
    
    def active : Boolean = hp > 0
    
    def getHP : Int = hp
    
    def damage(d : Int) : Unit = hp = math.max(hp - d, 0)
    
    def calcForce() : Unit =
    {
        nodes.foreach{x => x.force = Vec2(0d, 0d)}
        
        val n = nodes(nodes.size - 1)
        val pp = player.motion.position
        val p1 = n.position
        
        {
            val pv = pp - p1
            val dsq = pv.normSq
            val d = math.sqrt(dsq)
            if(d <= 800d && d >= 1.0E-4){
                val z =  2.0E-12 * dsq * d
                n.force += pv / z
            }
        }
        
        intaract.foreach{x =>
            val p2 = x.getEnd2
            val pv = p2 - p1
            val dsq = pv.normSq
            val d = math.sqrt(dsq)
            if(d <= 600d && d >= 1.0E-4){
                val z =  8.0E-12 * dsq * d
                n.force += - pv / z
            } 
        }
    }
    
    var theta = 0d
    val dtheta = math.Pi / 30d
    val positionOld = Array.fill(nodes.size)(Vec2(0d, 0d))
    val shotTiming = new OnceInN(GameMain.FPS * 2)
    def update() : Unit =
    {
        if(hp > 0){
            val dt = 1d / GameMain.FPS.toDouble
            positionOld(0) = nodes(0).position
            
            theta += dtheta
            if(theta >= math.Pi) theta -= math.Pi
            (1 to nodes.size - 1).foreach{i =>
                val n = nodes(i)
                val vnew = n.velocity + n.force * (0.5 * dt / n.mass)
                n.velocity = vConstraint(vnew)
                positionOld(i) = n.position
                n.position += n.velocity * dt
            }
            
            rattleCorrectionPosition()
            calcForce()
            
            (1 to nodes.size - 1).foreach{i =>
                val n = nodes(i)
                val vnew = n.velocity + n.force * (0.5 * dt / n.mass)
                n.velocity = vConstraint(vnew)
            }
            rattleCorrectionVelocity()
            
            if(shotTiming.apply){
                val p = nodes(nodes.size - 1).position
                val v = (player.getPosition - p)
                val ve = v * (15d / v.norm)
                val r = 12d
                val s = new ShotRed(p, ve, r)
                addShot(s)
            }
        }
    }
    
    def rattleCorrectionPosition() : Unit =
    {
        val dt = 1d / GameMain.FPS.toDouble
        var count = 0
        var finished = false
        while(count < 1 && !finished){
            finished = true
            (1 to nodes.size - 1).reverse.foreach{i =>
                val n0 = nodes(i-1)
                val n1 = nodes(i)
                
                val p01 = n0.position - n1.position
                val dSq = p01.normSq
                val d = math.sqrt(dSq)
                val delta = math.abs(d - r0)
                
                if(delta > eps){
                    finished = false
                    val pOld = positionOld(i-1) - positionOld(i)
                    val mass0 = n0.mass
                    val mass1 = n1.mass
                    val g = (dSq - r0 * r0) / (2d * (p01 inner_prod pOld) * (1d / mass0 + 1d / mass1))
                    val corr = pOld * g
                    val corrV = corr / dt
                    
                    if(i != 1){
                        n0.position = pConstraint(i, n0.position - corr / mass0)
                        val v0 = n0.velocity - corrV / mass0
                        n0.velocity = vConstraint(v0)
                        
                        n1.position = pConstraint(i, n1.position + corr / mass1)
                        val v1 = n1.velocity + corrV / mass1
                        n1.velocity = vConstraint(v1)
                    }else{
                        n1.position = pConstraint(i, n1.position + corr * (1d / mass0 + 1d / mass1))
                        val v1 = n1.velocity + corrV * (1d / mass0 + 1d / mass1)
                        n1.velocity = vConstraint(v1)
                    }
                }
            }
            count += 1
        }
    }
    
    def rattleCorrectionVelocity() : Unit =
    {
        val dt = 1d / GameMain.FPS.toDouble
        var count = 0
        var finished = false
        while(count < 1 && !finished){
            finished = true
            (1 to nodes.size - 1).reverse.foreach{i =>
                val n0 = nodes(i-1)
                val n1 = nodes(i)
                val mass0 = n0.mass
                val mass1 = n1.mass
                
                val p01 = n0.position - n1.position
                val v01 = n0.velocity - n1.velocity
                val pv = p01 inner_prod v01
                val delta = math.abs(pv * dt / r0)
                
                if(delta > eps){
                    finished = false
                    val k = pv / (r0 * r0 * (1d / mass0 + 1d / mass1))
                    val corrV = p01 * k
                    
                    if(i != 1){
                        val v0 = n0.velocity - corrV / mass0
                        n0.velocity = vConstraint(v0)
                        val v1 = n1.velocity + corrV / mass1
                        n1.velocity = vConstraint(v1)
                    }else{
                        val v1 = n1.velocity + corrV * (1d / mass0 + 1d / mass1)
                        n1.velocity = vConstraint(v1)
                    }
                }
            }
            count += 1
        }
    }
    
    def vConstraint(v : Vec2[Double]) : Vec2[Double] =
    {
        val vnorm = v.norm
        if(vnorm <= vmax)
            v
        else
            v * (vmax / vnorm)
    }
    
    def pConstraint(i : Int, p : Vec2[Double]) : Vec2[Double] =
    {
        val pmax = r0 * i.toDouble
        val pnorm = (p - nodes(0).position).norm
        if(pnorm <= pmax)
            p
        else
            nodes(0).position + (p - nodes(0).position) * (pmax / pnorm)
    }
    
    def pushBossArm(a : BossArm) : Unit = intaract = a :: intaract
    
    def getEnd1 : Vec2[Double] = nodes(0).position
    
    def getEnd2 : Vec2[Double] = nodes(nodes.size - 1).position
    
    def setEnd1(p : Vec2[Double]) : Unit = nodes(0).position = p
    
    def move(v : Vec2[Double]) : Unit =
    {
        nodes.foreach{x => x.position += v}
        (0 to positionOld.size - 1).foreach{i => positionOld(i) += v}
    }
    
    def rotate(center : Vec2[Double], ang : Double) : Unit =
    {
        nodes.foreach{x =>
            x.position = (x.position - center).rotate(ang) + center
            x.velocity = x.velocity.rotate(ang)
        }
        (0 to positionOld.size - 1).foreach{i => positionOld(i) = (positionOld(i) - center).rotate(ang) + center}
    }
    
    calcForce()
}

class BossArmPart(val arm : BossArm, val m : Motion) extends Character
{
    val motion = m
    val shape = Circle(Vec2(0d, 0d), radius)
    
    def radius = 20d
    
    override def active : Boolean = arm.active
    
    override def hit(b : Character) : Boolean =
    {
        if(!b.active) false
        val col = new Collision(motion, b.motion)
        val h = col.apply(shape, b.shape)
        if(h){
            arm.damage(b.attackAbility)
            b.damage(attackAbility)
        }
        h
    }
    
    def maxHP : Int = 0
    override def attackAbility : Int = 2
    
    override def getPoint : Int = 200
    
    override def getHP = arm.getHP
    
    override def damage(d : Int) : Unit = arm.damage(d)
}



























