package org.bigsleep.android.view

import android.util.Log
import android.opengl.GLU
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.Renderer
import android.content.Context
import android.graphics.Bitmap

import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint.Style

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(val width : Double, val height : Double) extends Renderer
{
    Log.d("bigsleep", "Renderer created")
    def getWidth = width
    def getHeight = height
    
    override def onSurfaceCreated(gl : GL10, config : EGLConfig) =
    {
        Log.d("bigsleep", "GLRenderer.onSurfaceCreated")
        	gl.glShadeModel(GL10.GL_FLAT)
    }
    
    override def onSurfaceChanged(gl : GL10, w : Int, h : Int) =
    {
    	    	gl.glViewport(0, 0, w, h)
        	gl.glMatrixMode(GL10.GL_PROJECTION)
        	gl.glLoadIdentity()
        	GLU.gluOrtho2D(gl, 0.0f, width.toFloat, 0.0f, height.toFloat)
    		gl.glMatrixMode(GL10.GL_MODELVIEW)
    }
    
    private var onDraw = (gl : GL10) => {}
    def setOnDraw(f : (GL10) => Unit) : Unit = onDraw = f
    
    override def onDrawFrame(gl : GL10) =
    {
        onDraw(gl)
        gl.glFlush()
    }
}
