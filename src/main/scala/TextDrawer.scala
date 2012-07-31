package org.bigsleep.android.view

import android.util.Log
import android.opengl.GLU
import android.opengl.GLUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint.Style

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class TextDrawer(var text : String, var paint : Paint)
{
    private var gl_ : GL10 = null
    private var width : Float = paint.measureText(text)
    private var height : Float = paint.getTextSize.toInt
    private var texture_id = 1
    private var img : Bitmap = Bitmap.createBitmap(toPow2(width.toInt), toPow2(height.toInt), Bitmap.Config.ARGB_8888)
    
    img.eraseColor(0x00000000)
    private val canvas = new Canvas(img)
    canvas.drawText(text, 0f, paint.getTextSize - paint.descent, paint)
    
    def getWidth = width.toInt
    def getHeight = height.toInt
    
    def apply(gl : GL10) =
    {
        if(gl_ == null || gl_ != gl){
            gl_ = gl
            bindTexture(gl)
        }
        
        gl.glEnable(GL10.GL_TEXTURE_2D)
        
        gl.glEnable(GL10.GL_BLEND)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        
        val x : Int = 0x10000
        val w = (width * x.toFloat).toInt
        val h = (height * x.toFloat).toInt
        val vertex : Array[Int] = Array(0, 0, w, 0, w, h, 0, h)
        
        val tw = (width / img.getWidth.toFloat * x.toFloat).toInt
        val th = (height / img.getHeight.toFloat * x.toFloat).toInt
        val tvertex : Array[Int] = Array(0, th, tw, th, tw, 0, 0, 0)
        
        gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000)
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id)
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE)
        gl.glVertexPointer(2, GL10.GL_FIXED, 0, getIntBuffer(vertex))
        gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, getIntBuffer(tvertex))
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glDisable(GL10.GL_BLEND)
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }
    
    def updateText(s : String, p : Paint) : Unit =
    {
        //Log.d("bigsleep", "TextDrawer.updateText")
        text = s
        paint = p
        
        width = paint.measureText(s)
        height = paint.getTextSize
        val imgw = toPow2(width.toInt)
        val imgh = toPow2(height.toInt)
        val larger = (imgw > img.getWidth || imgh > img.getHeight)
        if(larger){
            img.recycle
            img = Bitmap.createBitmap(imgw, imgh, Bitmap.Config.ARGB_8888)
        }
        
        img.eraseColor(0x00000000)
        val canvas = new Canvas(img)
        canvas.drawText(text, 0f, paint.getTextSize - paint.descent, paint)
        
        if(gl_ != null){
            if(larger){
                val id : Array[Int] = Array(texture_id)
                gl_.glDeleteTextures(1, id, 0)
                bindTexture(gl_)
            }else{
                updateTexture(gl_)
            }
        }
    }
    
    private def bindTexture(gl : GL10) =
    {
        Log.d("bigsleep", "TextDrawer.bindTexture")
        gl.glEnable(GL10.GL_TEXTURE_2D)
        val id : Array[Int] = Array(texture_id)
        gl.glGenTextures(1, id, 0)
        texture_id = id(0)
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id)
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, img, 0)
        
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR)
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE)
        
        gl.glDisable(GL10.GL_TEXTURE_2D)
    }
    
    private def updateTexture(gl : GL10) =
    {
        //Log.d("bigsleep", "TextDrawer.updateTexture")
        gl.glEnable(GL10.GL_TEXTURE_2D)
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id)
        GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, img)
        
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR)
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE)
        
        gl.glDisable(GL10.GL_TEXTURE_2D)
    }
    
    def getIntBuffer(table : Array[Int]) : java.nio.IntBuffer =
    {
        val bb = java.nio.ByteBuffer.allocateDirect(table.size * 4)
        bb.order(java.nio.ByteOrder.nativeOrder)
        val ib : java.nio.IntBuffer = bb.asIntBuffer()
        ib.put(table)
        ib.position(0)
        return ib
    }
    
    def toPow2(a : Int) : Int =
    a match{
        case x if x <= 0 => 0
        case _ =>
        {
            var b = 1
            while(b < a) b *= 2
            b
        }
    }
}
