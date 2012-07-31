package org.bigsleep.spacemission

import android.util.Log
import android.content.Context
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.view.ViewGroup
import android.view.Gravity
//import android.text.format.Time

class Time(
    val year : Int,
    val month : Int,
    val day : Int,
    val hour : Int,
    val minute : Int,
    val second : Int) extends Ordered[Time]
{
    def compare(that : Time) : Int =
    {
        if(this.year != that.year)
            if(this.year < that.year) return -1
            else return 1
        
        if(this.month != that.month)
            if(this.month < that.month) return -1
            else return 1
        
        if(this.day != that.day)
            if(this.day < that.day) return -1
            else return 1
        
        if(this.minute != that.minute)
            if(this.minute < that.minute) return -1
            else return 1
        
        if(this.second != that.second)
            if(this.second < that.second) return -1
            else return 1
        
        return 0
    }
}

class Highscore(ctx : Context) extends GameState
{
    private val context = ctx
    private val size = 10
    private val monthName = Array(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val preference = ctx.getSharedPreferences("highscore", Context.MODE_PRIVATE)
    private var scores : List[(Int, Time)] = (1 to 10).map{x =>
        val s = preference.getInt(x.toString + "s", 0)
        val t = new Time(0, 12, 0, 0, 0, 0)
        val date = preference.getString(x.toString + "d", formatDate(t))
        (s, parseDate(date))
    }.toList
    
    private val view : LinearLayout = initView
    
    def getView = view
    
    private def initView : LinearLayout =
    {
        val layout = new LinearLayout(context)
        updateView(layout)
        layout
    }

    private def updateView(layout : LinearLayout) : Unit =
    {
        layout.removeAllViews
        layout.setBackgroundColor(0xAA000000)
        layout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL)
        layout.setOrientation(LinearLayout.VERTICAL)
        
        val textSize = GameMain.getWindowHeight.toFloat / 45f
        
        val width = GameMain.getWindowWidth
        val height = GameMain.getWindowHeight / (size + 1)
        val t1 = new TextView(context)
        t1.setText("HIGHSCORE")
        t1.setTextSize(textSize)
        t1.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL)
        val tLP = new LayoutParams(width, height)
        tLP.weight = 1
        layout.addView(t1, tLP)
        
        Log.d("bigsleep", "scores size " + scores.size )
        
        var rank = 1
        scores.foreach{x =>
            val score = x._1
            val date = formatDate(x._2)
            val horizontal = new LinearLayout(context)
            horizontal.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL)
            horizontal.setOrientation(LinearLayout.HORIZONTAL)
            
            val rankView = new TextView(context)
            rankView.setText(rank.toString)
            rankView.setTextSize(textSize)
            rankView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL)
            val rankLP = new LayoutParams(width / 6, height)
            rankLP.weight = 1
            
            val scoreView = new TextView(context)
            scoreView.setText(score.toString)
            scoreView.setTextSize(textSize)
            scoreView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL)
            val scoreLP = new LayoutParams(width * 2 / 6, height)
            scoreLP.weight = 2
            
            val dateView = new TextView(context)
            dateView.setText(date)
            dateView.setTextSize(textSize)
            dateView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL)
            val dateLP = new LayoutParams(width * 3 / 6, height)
            dateLP.weight = 3
            
            horizontal.addView(rankView, rankLP)
            horizontal.addView(scoreView, scoreLP)
            horizontal.addView(dateView, dateLP)
            
            val horiLP = new LayoutParams(width, height)
            horiLP.weight = 1
            
            layout.addView(horizontal, horiLP)
            
            rank += 1
        }
    }

    def newScore(s : Int) : Unit =
    {
        val t0 = new android.text.format.Time()
        t0.setToNow()
        val t = new Time(t0.year, t0.month, t0.monthDay, t0.hour, t0.minute, t0.second)
        
        scores = scores :+ (s, t)
        
        scores = scores.sortWith{(x : (Int, Time), y : (Int, Time)) =>
            if(x._1 != y._1) x._1 > y._1
            else x._2 < y._2
        }.take(size)
        
        val e = preference.edit
        var rank = 1
        scores.foreach{x =>
            e.putInt(rank.toString + "s", x._1)
            e.putString(rank.toString + "d", formatDate(x._2))
            e.commit()
            rank += 1
        }
        updateView(view)
    }

    def formatDate(t : Time) : String =
    {
        val month = if(t.month >= 0 && t.month < 12) monthName(t.month) else "???"
        "%3s %02d %04d %02d:%02d:%02d".format(month, t.day, t.year, t.hour, t.minute, t.second)
    }
    
    def parseDate(d : String) : Time =
    {
        val datePattern = """(\S{3})\s(\d{2})\s(\d{4})\s(\d{2}):(\d{2}):(\d{2})""".r
        
        val t = d match{
            case datePattern(month, day, year, hour, minute, second) =>
            {
                Log.d("bigsleep", "highscore parseDate " + month + " " + day + " " + year + " " + hour + " " + minute + " " + second)
                val m = monthName.indexOf(month)
                new Time(year.toInt, m, day.toInt, hour.toInt, minute.toInt, second.toInt)
            }
            case _ => new Time(0, 12, 0, 0, 0, 0)
        }
        Log.d("bigsleep", "highscore parseDate " + d + ", " + formatDate(t))
        t
    }
}

