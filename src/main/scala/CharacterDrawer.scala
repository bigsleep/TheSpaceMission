package org.bigsleep.spacemission

import android.util.Log
import javax.microedition.khronos.opengles.{GL10, GL11}
import org.bigsleep.android.view._

class BasicCharacterDrawer(
    private val c : Character,
    private val d : ResourceImageDrawer) extends Drawer
{
    d.atCenter()
    var hp = c.getHP
    
    def getWidth = d.getWidth
    def getHeight = d.getHeight
    
    val damageEffect = new ResourceImageDrawer(d.id)
    damageEffect.atCenter()
    damageEffect.setTexEnv(GL10.GL_BLEND)
    damageEffect.setColorx(0x10000, 0x10000, 0x10000)
    val dieEffect = new Fader(d, 6)
    
    def setScale(s : Double) =
    {
        d.setScale(s)
        damageEffect.setScale(s)
    }
    
    override def update() : Unit =
    {
        if(hp <= 0) dieEffect.update
        hp = c.getHP
    }
    
    def apply(gl : GL10) : Unit =
    {
        val unit = 0x10000.toDouble
        val x = (c.motion.position(0) * unit).toInt
        val y = (c.motion.position(1) * unit).toInt
        val ang = (c.motion.angle * 180d / scala.math.Pi * unit).toInt
        gl.glLoadIdentity
        gl.glTranslatex(x, y, 0)
        gl.glRotatex(ang, 0, 0, 0x10000)
        if(c.active){
            if(c.getHP < hp)
                damageEffect.apply(gl)
            else
                d.apply(gl)
        }else{
            dieEffect.apply(gl)
        }
    }
    
    override def active : Boolean = if(c.getHP <= 0) dieEffect.active else c.active
}

class EnemyMeteorDrawer(private val e : EnemyMeteor) extends Drawer
{
    val drawer = ResourceImageDrawer(R.drawable.meteor)
    drawer.atCenter()
    val scale = (e.radius * 2d * 1.1d / drawer.getWidth.toDouble * 0x10000.toDouble).toInt
    
    val blowup = new CutOffAnimation(GameMain.getCutOffAnimationIds("blowup"))
    blowup.atCenter()
    val blowupScale = (e.radius * 2d * 1.0d / blowup.getWidth.toDouble * 0x10000.toDouble).toInt
    
    var hp = e.getHP
    val damageEffect = new ResourceImageDrawer(R.drawable.meteor)
    damageEffect.atCenter()
    damageEffect.setTexEnv(GL10.GL_BLEND)
    damageEffect.setColorx(0x10000, 0x10000, 0x10000)
    val fader = new Fader(drawer, blowup.size)
    
    override def update() : Unit =
    {
        if(e.getHP <= 0){
            fader.update
            blowup.update 
        }
        hp = e.getHP
    }
    
    def apply(gl : GL10) : Unit =
    {
        val unit = 0x10000.toDouble
        val x = (e.motion.position(0) * unit).toInt
        val y = (e.motion.position(1) * unit).toInt
        val ang = (e.motion.angle * 180d / scala.math.Pi * unit).toInt
        gl.glLoadIdentity
        gl.glTranslatex(x, y, 0)
        gl.glScalex(scale, scale, 0x10000)
        gl.glRotatex(ang, 0, 0, 0x10000)
        
        if(e.getHP > 0){
            if(e.getHP < hp) damageEffect.apply(gl)
            else drawer.apply(gl)
        }
        else if(fader.active){
            fader.apply(gl)
            gl.glLoadIdentity
            gl.glTranslatex(x, y, 0)
            gl.glScalex(blowupScale, blowupScale, 0x10000)
            blowup.apply(gl)
        }
    }
    
    override def active : Boolean = if(e.getHP <= 0) fader.active else e.active
}


class BossDrawer(private val boss : Boss) extends Drawer
{
    val partDrawer = new ResourceImageDrawer(R.drawable.boss01)
    partDrawer.atCenter()
    
    override def update() : Unit =
    {
    }
    
    def apply(gl : GL10) : Unit =
    {
        boss.arms.foreach{arm =>
            arm.nodes.foreach{n =>
                val unit = 0x10000.toDouble
                val x = (n.position(0) * unit).toInt
                val y = (n.position(1) * unit).toInt
                val scale = (0.5d * unit).toInt
                gl.glLoadIdentity
                gl.glTranslatex(x, y, 0)
                gl.glScalex(scale, scale, unit.toInt)
                partDrawer.apply(gl)
            }
        }
        
        val unit = 0x10000.toDouble
        val x = (boss.motion.position(0) * unit).toInt
        val y = (boss.motion.position(1) * unit).toInt
        gl.glLoadIdentity
        gl.glTranslatex(x, y, 0)
        
        partDrawer.apply(gl)
    }
    
    override def active : Boolean = true
}

