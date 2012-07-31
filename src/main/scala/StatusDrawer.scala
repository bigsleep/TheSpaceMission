package org.bigsleep.spacemission

import android.graphics.Paint
import javax.microedition.khronos.opengles.GL10

class PlayStatusDrawer(model : GameModel) extends org.bigsleep.android.view.Drawer
{
    private val paint = new Paint()
    paint.setColor(0xFFFFFFFF)
    paint.setStyle(Paint.Style.FILL)
    paint.setTextSize(60)
    
    private val FPS = GameMain.FPS
    private val fpsChecker = new FPSChecker()
    private val timeDrawer = new org.bigsleep.android.view.TextDrawer(formatTime(0), paint)
    private val fpsDrawer = new org.bigsleep.android.view.TextDrawer(formatFPS(0d), paint)
    private val scoreDrawer = new org.bigsleep.android.view.TextDrawer(formatScore(0), paint)
    
    private val y : Int = (paint.getTextSize * 0x10000.toFloat).toInt
    private val space = y / 10
    private val gaugeWidth = timeDrawer.getWidth * 0x10000 - space * 2
    private val gaugeHeight = y - space * 2
    private var time = 0L
    
    def apply(gl : GL10) : Unit =
    {
        gl.glLoadIdentity
        scoreDrawer.apply(gl)
        
        val y : Int = (paint.getTextSize * 0x10000.toFloat).toInt
        gl.glTranslatex(0, y, 0)
        timeDrawer.apply(gl)
        /*
        gl.glTranslatex(0, y, 0)
        fpsDrawer.apply(gl)
        */
        gl.glTranslatex(0, y, 0)
        drawHPGauge(gl)
    }
    
    val updateTiming = OnceInN(FPS / 5)
    override def update() : Unit =
    {
        val fps = fpsChecker.apply
        if(updateTiming.apply){
            timeDrawer.updateText(formatTime(time), paint)
            fpsDrawer.updateText(formatFPS(fps), paint)
            scoreDrawer.updateText(formatScore(model.getPlayer.getScore), paint)
        }
    }
    
    def setTime(t : Long) : Unit = time = t
    
    def formatTime(t : Long) : String =
    {
        val h = t / 3600000
        val m = (t % 3600000) / 60000
        val s = (t % 60000) / 1000
        "%02d:%02d:%02d".format(h, m, s)
    }
    
    def formatFPS(fps : Double) : String = "%2.2f".format(fps)
    
    def formatScore(s : Int) : String = "%07d".format(s)
    
    def drawHPGauge(gl : GL10) : Unit =
    {
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        
        val hp = model.getPlayer.getHP.toDouble
        val maxHP = model.getPlayer.maxHP.toDouble
        val w0 = (gaugeWidth * hp / maxHP).toInt + space
        val h0 = gaugeHeight + space
        val vertex = Seq(space, space, w0, space, w0, h0, space, h0)
        gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000)
        gl.glLineWidthx(0x10000 * 4)
        gl.glVertexPointer(2, GL10.GL_FIXED, 0, getIntBuffer(vertex))
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        
        val w1 = gaugeWidth + space
        val vertex1 = Seq(space, space, w1, space, w1, h0, space, h0)
        gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000)
        gl.glLineWidthx(0x10000 * 4)
        gl.glVertexPointer(2, GL10.GL_FIXED, 0, getIntBuffer(vertex1))
        gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4)
        
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }
}


