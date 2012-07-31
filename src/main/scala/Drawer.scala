package org.bigsleep.android.view

import android.app.Activity
import android.os.Bundle
import android.content.res.AssetManager
import android.content.Context

import android.util.Log
import android.opengl.GLU
import android.opengl.GLUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint.Style

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.FloatBuffer

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.{GL10, GL11}

import org.bigsleep.geometry.Vec2

abstract class Drawer
{
    def apply(gl : GL10) : Unit
    
    def active : Boolean = true
    
    def update() : Unit = {}
    
    def getIntBuffer(table : Seq[Int]) : IntBuffer =
    {
        val bb = ByteBuffer.allocateDirect(table.size * 4)
        bb.order(ByteOrder.nativeOrder)
        val ib : IntBuffer = bb.asIntBuffer()
        val a = new Array[Int](8)
        for(i <- 0 to 7) a(i) = table(i)
        ib.put(a)
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

class ResourceImageDrawer(val id : Int) extends Drawer
{
    private val Self = ResourceImageDrawer
    private var scale : Double = 1.0
    private var opacity : Double = 1.0
    private var texEnv = GL10.GL_MODULATE
    private var width = -1
    private var height = -1
    private val colorx = Array.fill[Int](3)(0x10000)
    
    def getScale = scale
    def getOpacity = opacity
    
    def setScale(a : Double) : Unit = scale = a
    def setOpacity(a : Double) : Unit = opacity = a
    def setTexEnv(a : Int) : Unit = texEnv = a
    def setColorx(r : Int, g : Int, b : Int) : Unit =
    {
        colorx(0) = r
        colorx(1) = g
        colorx(2) = b
    }
    
    def getWidth : Int =
    {
        if(width > 0) width
        else{
            if(!Self.size.contains(id))
                Log.d("bigsleep", "ResourceImageDrawer.getWidth size not contain")
            val sz = Self.size(id)
            width = sz(0)
            height = sz(1)
            width
        }
    }
    def getHeight : Int =
    {
        if(height > 0) height
        else{
            if(!Self.size.contains(id))
                Log.d("bigsleep", "ResourceImageDrawer.getWidth size not contain")
            val sz = Self.size(id)
            width = sz(0)
            height = sz(1)
            height
        }
    }
    
    var atCenter_ = false
    def atCenter(a : Boolean = true) : Unit = atCenter_ = a
    
    var blendFunc = alphaBlendFunc _
    def setBlendAlpha() : Unit = blendFunc = alphaBlendFunc _
    def setBlendAdd() : Unit = blendFunc = addBlendFunc _
    def setBlendAddAlpha() : Unit = blendFunc = addAlphaBlendFunc _
    def alphaBlendFunc(gl : GL10) : Unit = gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
    def addBlendFunc(gl : GL10) : Unit = gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE)
    def addAlphaBlendFunc(gl : GL10) : Unit = gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
    
    def apply(gl : GL10) : Unit =
    {
        if(!Self.binded(gl)){
            Self.bindTextures(gl)
        }
        val tid = Self.texId(id)
        val img = Self.images(id)
        gl.glEnable(GL10.GL_TEXTURE_2D)
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, texEnv)
        
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        
        gl.glEnable(GL10.GL_BLEND)
        blendFunc.apply(gl)
        
        val x : Int = 0x10000
        val w = (getWidth * x.toDouble).toInt
        val h = (getHeight * x.toDouble).toInt
        val vertex = Seq(0, 0, w, 0, w, h, 0, h)
        val tw : Int = (getWidth.toDouble / img.getWidth.toDouble * x.toDouble).toInt
        val th : Int = (getHeight.toDouble / img.getHeight.toDouble * x.toDouble).toInt
        val tvertex = Seq(0, th, tw, th, tw, 0, 0, 0)
        
        val opacityx = (opacity * 0x10000.toDouble).toInt
        gl.glColor4x(colorx(0), colorx(1), colorx(2), opacityx)
        val scalex = (scale * 0x10000.toDouble).toInt
        gl.glScalex(scalex, scalex, 0x10000)
        
        if(atCenter_){
            gl.glTranslatex(-w/2, -h/2, 0)
        }
        gl.glBindTexture(GL10.GL_TEXTURE_2D, tid)
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, texEnv)
        
        gl.glVertexPointer(2, GL10.GL_FIXED, 0, Self.getIntBuffer(vertex))
        gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, Self.getIntBuffer(tvertex))
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glDisable(GL10.GL_BLEND)
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }
}

object ResourceImageDrawer
{
    private var images = collection.immutable.Map.empty[Int, Bitmap]
    private var texId = collection.immutable.Map.empty[Int, Int]
    private var size = collection.immutable.Map.empty[Int, Vec2[Int]]
    private var gl : GL10 = null
    
    def binded(g : GL10) : Boolean = g == gl
    
    def loadImages(ctxt :  android.content.Context, names : Array[String]) : Unit =
    {
        images = collection.immutable.Map.empty[Int, Bitmap]
        size = collection.immutable.Map.empty[Int, Vec2[Int]]
        names.foreach(
            x => {
                val id = ctxt.getResources.getIdentifier(x, "drawable", ctxt.getPackageName)
                val is = ctxt.getResources.openRawResource(id)
                var bmp = BitmapFactory.decodeStream(is)
                
                val w = bmp.getWidth
                val h = bmp.getHeight
                val pow2w = toPow2(w)
                val pow2h = toPow2(h)
                if(pow2w != w || pow2h != h){
                    val n = Bitmap.createBitmap(pow2w, pow2h, bmp.getConfig)
                    n.eraseColor(0)
                    val c = new Canvas(n)
                    c.drawBitmap(bmp, 0f, 0f, null)
                    val g = bmp
                    bmp = n
                    g.recycle
                }
                if(bmp != null){
                    images = images.updated(id, bmp)
                    size = size.updated(id, Vec2(w, h))
                }
                
            })
    }
    
    def bindTextures(gl : GL10) : Unit =
    {
        if(images.size != 0){
            gl.glEnable(GL10.GL_TEXTURE_2D)
            val ids : Array[Int] = images.map(x => x._1).toArray
            val texids = ids.clone
            gl.glGenTextures(images.size, texids, 0);
            texId = collection.immutable.Map.empty[Int, Int]
            texId = texId ++ ids.zip(texids)
            images.foreach(x => {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, texId(x._1))
                GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, x._2, 0)
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR)
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR)
                gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE)
            })
            gl.glDisable(GL10.GL_TEXTURE_2D)
            Log.d("bigsleep", "bindTextures end")
        }
        this.gl = gl
    }
    
    def deleteTextures() : Unit =
    {
        if(gl != null){
            val i = texId.map(_._2).toArray
            gl.glDeleteTextures(i.size, i, 0)
            texId = collection.immutable.Map.empty[Int, Int]
            gl = null
        }
    }
    
    def apply(id : Int) : ResourceImageDrawer = new ResourceImageDrawer(id)
    
    def getIntBuffer(table : Seq[Int]) : IntBuffer =
    {
        val bb = ByteBuffer.allocateDirect(table.size * 4)
        bb.order(ByteOrder.nativeOrder)
        val ib : IntBuffer = bb.asIntBuffer()
        val a = new Array[Int](8)
        for(i <- 0 to 7) a(i) = table(i)
        ib.put(a)
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


class ImageDrawer(val img : Bitmap) extends Drawer
{
    private var textureId = 1
    private var gl_ : GL10 = null
    
    private var scale : Double = 1.0
    private var opacity : Double = 1.0
    private var texEnv = GL10.GL_REPLACE
    
    def setScale(a : Double) : Unit = scale = a
    def setOpacity(a : Double) : Unit = opacity = a
    def setTexEnv(a : Int) : Unit = texEnv = a
    def width : Int = img.getWidth
    def height : Int = img.getHeight
    
    def apply(gl : GL10) : Unit =
    {
        if(gl_ == null){
            bindTexture(gl)
        }
        gl.glEnable(GL10.GL_TEXTURE_2D)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glEnable(GL10.GL_BLEND)
        //gl.glActiveTexture(GL10.GL_TEXTURE0)
        
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        
        val x : Int = 0x10000
        val w = (width * x.toDouble).toInt
        val h = (height * x.toDouble).toInt
        val vertex = Seq(0, 0, w, 0, w, h, 0, h)
        val tvertex = Seq(0, 0, x, 0, x, x, 0, x)
        
        gl.glColor4f(0.0f, 0.0f, 0.0f, opacity.toFloat)
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId)
        gl.glVertexPointer(2, GL10.GL_FIXED, 0, getIntBuffer(vertex))
        gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, getIntBuffer(tvertex))
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glDisable(GL10.GL_BLEND)
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }
    
    private def bindTexture(gl : GL10) : Unit =
    {
        gl.glEnable(GL10.GL_TEXTURE_2D)
        val ids : Array[Int] = Array(textureId)
        gl.glGenTextures(1, ids, 0)
        textureId = ids(0)
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, img, 0)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR)
        gl.asInstanceOf[GL11].glTexEnvi(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, texEnv)
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl_ = gl
    }
}


class BackgroundDrawer(
    val ids : Array[Int],
    private var width : Double,
    private var height : Double,
    val speed : Double) extends Drawer
{
    val drawers : Array[ResourceImageDrawer] = ids.map(x => new ResourceImageDrawer(x))
    val scale = width / drawers(0).getWidth.toDouble
    val scalex = toFixed(scale)
    val totalHeight = (drawers.foldLeft(0){(x : Int, y : ResourceImageDrawer) => x + y.getHeight}).toDouble * scale
    private var pos = 0d
    
    def setWidth(w : Double) = width = w
    def setHeight(h : Double) = height = h
    
    def apply(gl : GL10) : Unit =
    {
        var p = pos
        drawers.foreach{x =>
            val h = x.getHeight.toDouble * scale
            if(p >= -h && p < height){
                gl.glLoadIdentity
                gl.glTranslatex(0, toFixed(p), 0)
                gl.glScalex(scalex, scalex, 0x10000)
                x.apply(gl)
            }else if(p + totalHeight < height){
                gl.glLoadIdentity
                gl.glTranslatex(0, toFixed(p + totalHeight), 0)
                gl.glScalex(scalex, scalex, 0x10000)
                x.apply(gl)
            }
            p += h
            if(p >= totalHeight) p -= totalHeight
        }
    }
    
    override def update() : Unit =
    {
        pos -= speed
        if(pos <= - totalHeight) pos += totalHeight
    }
    
    def setPosition(p : Double) : Unit = pos = p
    
    def toFixed(a : Float) : Int = (a * 0x10000.toFloat).toInt
    def toFixed(a : Double) : Int = (a * 0x10000.toDouble).toInt
}

class CutOffAnimation(i : Array[Int]) extends Drawer
{
    val ids = i
    val drawers : Array[ResourceImageDrawer] = ids.map(x => new ResourceImageDrawer(x))
    val size = ids.size
    
    var count = 0
    def apply(gl : GL10) : Unit =
    {
        drawers(count).apply(gl)
    }
    
    override def update() : Unit =
    {
        count = if(count + 1 < size) count + 1 else 0
    }
    
    def getWidth : Int = if(size > 0) drawers(0).getWidth else 0
    def getHeight : Int = if(size > 0) drawers(0).getHeight else 0
    
    def atCenter(a : Boolean = true) : Unit = drawers.foreach(_.atCenter(a))
    def setBlendAlpha : Unit = drawers.foreach(_.setBlendAlpha)
    def setBlendAdd : Unit = drawers.foreach(_.setBlendAdd)
    def setBlendAddAlpha : Unit = drawers.foreach(_.setBlendAddAlpha)
}

class Fader(val drawer : ResourceImageDrawer, val n : Int) extends Drawer
{
    private var opacity = drawer.getOpacity
    private var count = n
    private val dop = opacity / n.toDouble
    
    def apply(gl : GL10) : Unit =
    {
        drawer.setOpacity(opacity)
        drawer.apply(gl)
    }
    
    override def update() : Unit =
    {
        opacity -= dop
        count -= 1
    }
    
    override def active = count > 0
}

