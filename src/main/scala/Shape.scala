package org.bigsleep.geometry

class MotionState(p : Vec2[Double] = Vec2(0d, 0d), a : Double = 0d)
{
    var position = p
    var angle = a
}

abstract class Shape

case class Circle(val center : Vec2[Double], val radius : Double) extends Shape()

case class Line(val end1 : Vec2[Double], val end2 : Vec2[Double]) extends Shape()

case class ShapeGroup(shapes : Array[Shape]) extends Shape()


class Collision(ma : MotionState = new MotionState(), mb : MotionState = new MotionState())
{
    import GeoUtil._
    val pos1 = ma.position
    val pos2 = mb.position
    val ang1 = ma.angle
    val ang2 = mb.angle
    private val eps = 1.0E-8
    
    def applyImpl(a : Circle, b : Circle) : Boolean =
    {
        val r = (a.radius + b.radius + eps)
        val rr = r * r
        val pa = pos1 + (if(ang1.abs > eps) a.center.rotate(ang1) else a.center)
        val pb = pos2 + (if(ang2.abs > eps) b.center.rotate(ang2) else b.center)
        val dd = (pa - pb).normSq
        (dd <= rr)
    }
    
    def applyImpl(a : Circle, b : Line) : Boolean =
    {
        val r = a.radius
        val rr = (r + eps) * (r + eps)
        val pa = pos1 + (if(ang1.abs > eps) a.center.rotate(ang1) else a.center)
        val pb1 = pos2 + (if(ang2.abs > eps) b.end1.rotate(ang2) else b.end1)
        val pb2 = pos2 + (if(ang2.abs > eps) b.end2.rotate(ang2) else b.end2)
        val v = pb1 - pb2
        val v1 = pa - pb1
        val v2 = pa - pb2
        val vn = v.norm
        
        if(v1.normSq <= rr || v2.normSq <= rr) return true
        if(vn <= eps) return false
        
        val i1 = v1 inner_prod (-v)
        val i2 = v2 inner_prod v
        
        if(i1 < 0.0 || i2 < 0.0) return false
        val d = ((v2 cross_prod v) / vn).abs
        (d < r + eps)
    }
    
    def applyImpl(a : Line, b : Circle) : Boolean = applyImpl(b, a)
    
    def applyImpl(a : ShapeGroup, b : ShapeGroup) : Boolean =
    {
        for(i <- a.shapes; j <- b.shapes){
            val c = apply(i, j)
            if(c) return true
        }
        return false
    }
    
    def applyImpl(a : ShapeGroup, b : Shape) : Boolean =
    {
        for(i <- a.shapes){
            val c = apply(i, b)
            if(c) return true
        }
        return false
    }
    
    def applyImpl(a : Shape, b : ShapeGroup) : Boolean =
    {
        for(i <- b.shapes){
            val c = apply(a, i)
            if(c) return true
        }
        return false
    }
    
    def apply(a : Shape, b : Shape) : Boolean =
    (a, b) match{
        case (x @ Circle(_,_),   y @ Circle(_,_))   => applyImpl(x, y)
        case (x @ ShapeGroup(_), y @ ShapeGroup(_)) => applyImpl(x, y)
        case (x @ ShapeGroup(_), y)                 => applyImpl(x, y)
        case (x,                 y @ ShapeGroup(_)) => applyImpl(x, y)
    }
}

object GeoUtil
{
    val eps = 1.0E-6
    val epsSq = eps * eps
    
    def near(a : Vec2[Double], b : Vec2[Double]) : Boolean = (a - b).normSq <= eps
    
    def on(a : Vec2[Double], b : Line) : Boolean =
    {
        if(near(a, b.end1) || near(a, b.end2))
            return true
        
        val ymin = math.min(b.end1(1), b.end2(1)) - eps
        val ymax = math.max(b.end1(1), b.end2(1)) + eps
        val xmin = math.min(b.end1(0), b.end2(0)) - eps
        val xmax = math.max(b.end1(0), b.end2(0)) + eps
        
        val v = b.end2 - b.end1
        val va = a - b.end1
        val cp = v cross_prod va
        val cp2 = cp * cp
        val lv2 = v.normSq
        if(lv2 < eps) return false
        val d2 = cp2 / lv2
        
        val inrange = a(0) >= xmin && a(0) <= xmax && a(1) >= ymin && a(1) <= ymax
        val online = d2 < 0.1
        inrange && online
    }
    
    def intersect(a : Line, b : Line) : Boolean =
    {
        val A = a.end1
        val B = a.end2
        val C = b.end1
        val D = b.end2
        val AB = B - A
        val AD = D - A
        val AC = C - A
        val CD = D - C
        val CA = - AC
        val CB = B - C
        val cp1 = AB cross_prod AD
        val cp2 = AB cross_prod AC
        val cp3 = CD cross_prod CA
        val cp4 = CD cross_prod CB
        if(cp1 * cp2 < 0.0 && cp3 * cp4 < 0.0){
            return true
        }else if(on(A, b) || on(B, b) || on(C, a) || on(D, a)){
            return true
        }else{
            return false
        }
    }
}




