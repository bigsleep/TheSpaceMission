package org.bigsleep.util

abstract class Generator[T]
{
    import scala.util.continuations._
    
    private var current : Result = reset{body; Done()}
    
    def body : Unit @cpsParam[Result, Result]
    
    def foreach[U](f : T => U) : Unit = {while(hasNext) next.foreach(f)}
    
    def generate(a : T) : Unit @cpsParam[Result, Result] =
    {
        shift{k : (Unit => Result) => Next(a, k)}
    }
    
    def next : Option[T] =
    {
        current match{
            case Next(x, k) =>
            {
                current = k()
                Some(x)
            }
            case Done() => None
        }
    }
    
    def hasNext : Boolean = current != Done()
    
    def doWhile(cond : => Boolean)(f : => (Unit @cpsParam[Result, Result])) : Unit @cpsParam[Result, Result] =
    {
        if(cond){
            f
            doWhile(cond)(f)
        }else ()
    }
    
    def repeat(n : Int)(f : => (Unit @cpsParam[Result, Result])) : Unit @cpsParam[Result, Result] =
    {
        if(n > 0){
            f
            repeat(n - 1)(f)
        }else ()
    }
    
    abstract class Result
    case class Next(v : T, k : (Unit => Result)) extends Result
    case class Done() extends Result
}

