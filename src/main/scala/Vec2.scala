package org.bigsleep.geometry

class Vec2[T] private(val x : T, val y : T)(implicit num: Numeric[T])
{
    import num._
    
    type Self = Vec2[T]
    
    val size = 2
    
    def apply(i : Int) : T =
    i match{
        case 0 => x
        case 1 => y
    }
    
    def + (that : Self) : Self = new Vec2[T](x + that.x, y + that.y)
    
    def - (that : Self) : Self = new Vec2[T](x - that.x, y - that.y)
    
    def * (f : T) : Self = new Vec2[T](x * f, y * f)
    
    def * (m : Mat2[T]) : Self =
    {
        val a = x * m.e00 + y * m.e10
        val b = x * m.e01 + y * m.e11
        new Vec2[T](a, b)
    }
    
    def / (f : Double) : Vec2[Double] = new Vec2[Double](x.toDouble / f, y.toDouble / f)
    
    def unary_- = new Vec2[T](-x, -y)
    
    def norm : Double = math.sqrt(normSq.toDouble)
    
    def normSq : T = (x * x + y * y)
    
    def distance(that : Self) : Double = (this - that).norm
    
    def distanceSq(that : Self) = (this - that).normSq
    
    def inner_prod(that : Self) : T = (x * that.x + y * that.y)
    
    def cross_prod(that : Self) = (x * that.y - y * that.x)
    
    def rotate(rad : Double) : Vec2[Double] = Mat2.rotationM(rad) * (new Vec2[Double](x.toDouble, y.toDouble))
    
    override def toString = "Vec2(" + x.toString + ", " + y.toString + ")"
}

object Vec2
{
    def apply[T](a : T, b : T)(implicit num: Numeric[T]) : Vec2[T] = new Vec2[T](a, b)
}

class Mat2[T] private(val e00 : T, val e01 : T, val e10 : T, val e11 : T)
    (implicit num: Numeric[T])
{
    import num._
    
    val size = 2
    
    type Self = Mat2[T]
    
    def apply(i : Int, j : Int) : T =
    (i, j) match{
        case (0, 0) => e00
        case (0, 1) => e01
        case (1, 0) => e10
        case (1, 1) => e11
    }
    
    def + (that : Self) : Self = new Self(e00 + that.e00, e01 + that.e01, e10 + that.e10, e11 + that.e11)
    
    def - (that : Self) : Self = new Self(e00 - that.e00, e01 - that.e01, e10 - that.e10, e11 - that.e11)
    
    def * (f : T) : Self = new Self(e00 * f, e01 * f, e10 * f, e11 * f)
    
    def / (f : Double) : Mat2[Double] = new Mat2[Double](e00.toDouble / f, e01.toDouble / f, e10.toDouble / f, e11.toDouble / f)
    
    def unary_- = new Self(-e00, -e01, -e10, -e11)
    
    def * (v : Vec2[T]) : Vec2[T] = Vec2(e00 * v.x + e01 * v.y, e10 * v.x + e11 * v.y)
    
    def * (that : Self) : Self =
    {
        val m00 = e00 * that.e00 + e01 * that.e10
        val m01 = e00 * that.e01 + e01 * that.e11
        val m10 = e10 * that.e00 + e11 * that.e10
        val m11 = e10 * that.e01 + e11 * that.e11
        new Self(m00, m01, m10, m11)
    }
    
    override def toString = "Mat2((" + e00 + ", " + e01 + "),(" + e10 + "," + e11 + "))"
}

object Mat2
{
    def apply[T](a : T, b : T, c : T, d : T)(implicit num: Numeric[T]) : Mat2[T] =
    {
        new Mat2[T](a, b, c, d)
    }
    
    def rotationM(ang : Double) : Mat2[Double] =
    {
        val c = math.cos(ang)
        val s = math.sin(ang)
        Mat2(c, -s, s, c)
    }
}

