package org.bigsleep.spacemission

import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams

class GameStateMachine(ctx : Context)
{
    private val menu = new Menu(ctx)
    private val play = new Play(ctx)
    private val pause = new Pause(ctx)
    private val highscore = new Highscore(ctx)
    private val gameOver = new GameOvered(ctx)
    private val gameClear = new GameCleared(ctx)
    
    private var preState : GameState = menu
    private var state : GameState = menu
    GameMain.addView(play.getView)
    GameMain.addView(state.getView)
    
    def processEvent(ev : Event) : Unit =
    {
        Log.d("bigsleep", "GameStateMachine.processEvent start")
        (state, ev) match
        {
            case (x, NoEvent()) => state
            case (x, StartGame()) =>
            {
                play.initializeGame
                preState = state
                state = play
                
                GameMain.invalidateView
                GameMain.post{() => removeOtherView(state); play.startGame }
            }
            case (x, StartMenu()) =>
            {
                removeOtherView(menu)
                GameMain.addView(menu.getView)
                GameMain.invalidateView
                preState = state
                state = menu
                /*
                val handler = new android.os.Handler
                handler.postDelayed(new java.lang.Runnable{
                    def run(){ GameMain.synchronized{play.initializeGame} }
                }, 200)
                */
            }
            case (x, ResumeGame()) if x == pause =>
            {
                preState = state
                state = play
                Log.d("bigsleep", "GameStateMachine.processEvent on ResumeGame")
                play.playview.resumeGame
                removeOtherView(state)
            }
            case (x, BackPressed()) if x == play =>
            {
                preState = state
                state = pause
                Log.d("bigsleep", "GameStateMachine.processEvent on BackPressed")
                play.playview.pauseGame
                GameMain.addView(pause.getView)
                removeOtherView(state)
            }
            case (x, BackPressed()) if x == highscore =>
            {
                preState match{
                    case x if x == pause =>
                    {
                        state = pause
                        preState = highscore
                        GameMain.addView(pause.getView)
                        removeOtherView(state)
                    }
                    case x if x == menu || x == gameOver || x == gameClear =>
                    {
                        processEvent(StartMenu())
                    }
                    case _ => {}
                }
            }
            case (x, ShowHighscore()) =>
            {
                preState = state
                state = highscore
                GameMain.addView(highscore.getView)
                removeOtherView(state)
            }
            case (x, GameOver()) =>
            {
                play.stopGame()
                preState = state
                state = gameOver
                GameMain.addView(gameOver.getView)
                removeOtherView(state)
            }
            case (x, GameClear()) =>
            {
                play.stopGame()
                preState = state
                state = gameClear
                GameMain.addView(gameClear.getView)
                removeOtherView(state)
                highscore.newScore(play.getScore)
            }
            case _ =>
            {
                Log.d("bigsleep", "GameStateMachine.processEvent dummy event")
            }
        }
        Log.d("bigsleep", "GameStateMachine.processEvent end")
    }
    
    private def removeOtherView(s : GameState) : Unit =
    {
        if(s != menu) GameMain.removeView(menu.getView)
        if(s != pause) GameMain.removeView(pause.getView)
        if(s != highscore) GameMain.removeView(highscore.getView)
        if(s != gameOver) GameMain.removeView(gameOver.getView)
        if(s != gameClear) GameMain.removeView(gameClear.getView)
    }
    
    def onResume() : Unit = state.onResume
    def onPause() : Unit = state.onPause
    def onTouchEvent(ev : MotionEvent) : Unit = state.onTouchEvent(ev)
}

abstract class Event
case class NoEvent() extends Event
case class StateEnd() extends Event
case class StartGame() extends Event
case class StartMenu() extends Event
case class PauseGame() extends Event
case class ResumeGame() extends Event
case class BackPressed() extends Event
case class ShowHighscore() extends Event
case class GameOver() extends Event
case class GameClear() extends Event

abstract class GameState
{
    def getView : View
    def onResume : Unit = {}
    def onPause : Unit = {}
    def onTouchEvent(ev : MotionEvent) : Unit = {}
}

class Opening(ctx : Context) extends GameState
{
    //val view = new OpeningView(ctx)
    private val view = new TextView(ctx){
        override def onTouchEvent(ev : MotionEvent) : Boolean =
        {
            if(ev.getAction == MotionEvent.ACTION_DOWN){
                GameMain.processEvent(StateEnd())
            }
            super.onTouchEvent(ev)
        }
    }
    def getView = view
    view.setText("Opening")
}

class Menu(ctx : Context) extends GameState
{
    private val view = initView(ctx)
    
    private def initView(c : Context) : View =
    {
        val frame = new android.widget.FrameLayout(c)
        
        val title = new android.widget.ImageView(c)
        title.setImageResource(R.drawable.menu)
        title.setScaleType(android.widget.ImageView.ScaleType.FIT_XY)
        frame.addView(title)
        
        val padding = (GameMain.getWindowWidth.toDouble * 0.2).toInt
        val buttonLayout = new LinearLayout(c)
        buttonLayout.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.CENTER_VERTICAL)
        buttonLayout.setPadding(padding, padding, padding, padding)
        val startButton = new android.widget.ImageButton(c)
        startButton.setImageResource(R.drawable.start_button)
        startButton.setAdjustViewBounds(true)
        startButton.setBackgroundColor(0x00000000)
        startButton.setOnClickListener(new android.view.View.OnClickListener() {
            override def onClick(v : View)
            {
                Log.d("bigsleep", "Menu on startButton Clicked")
                GameMain.processEvent(StartGame())
            }
        })
        buttonLayout.addView(startButton)
        
        val layout = new android.widget.LinearLayout(c)
        layout.setBackgroundColor(0x00000000)
        layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.CENTER_VERTICAL)
        layout.addView(buttonLayout, LayoutParams.WRAP_CONTENT)
        
        frame.addView(layout)
        
        frame
    }
    
    def getView = view
}

class Play(ctx : Context) extends GameState
{
    Log.d("bigsleep", "Play create")
    
    def getView = playview.getAndroidView
    
    val playview : PlayView = new PlayView(ctx)
    val view = playview.getAndroidView
    Log.d("bigsleep", "Play.view create")
    
    private def addCharacter(c : Character) : Unit = playview.addCharacter(c)
    
    def initializeGame() : Unit = playview.initializeGame
    
    def startGame() : Unit =
    {
        playview.startGame
    }
    
    def stopGame() : Unit = playview.stopGame
    
    override def onPause() : Unit = playview.onPause
    override def onResume() : Unit = playview.onResume
    
    override def onTouchEvent(ev : MotionEvent) : Unit =
    {
        val x = ev.getX.toDouble
        val y = GameMain.getHeight.toDouble - ev.getY.toDouble
        
        ev.getAction match{
            case MotionEvent.ACTION_DOWN => playview.onTouchEvent(new TouchEvent(TouchEventType.DOWN, x, y))
            case MotionEvent.ACTION_MOVE => playview.onTouchEvent(new TouchEvent(TouchEventType.MOVE, x, y))
            case MotionEvent.ACTION_OUTSIDE => playview.onTouchEvent(new TouchEvent(TouchEventType.OUTSIDE, x, y))
            case MotionEvent.ACTION_UP => playview.onTouchEvent(new TouchEvent(TouchEventType.UP, x, y))
            case MotionEvent.ACTION_CANCEL => playview.onTouchEvent(new TouchEvent(TouchEventType.NON, x, y))
            case _ => {}
        }
    }
    
    def getTime : Long = playview.getTime
    
    def getScore : Int = playview.getScore
}

class Pause(ctx : Context) extends GameState
{
    val view = new android.widget.FrameLayout(ctx)
    
    val widgets = new LinearLayout(ctx)
    widgets.setBackgroundColor(0xAA000000)
    widgets.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.CENTER_VERTICAL)
    widgets.setOrientation(android.widget.LinearLayout.VERTICAL)
    
    val scale = GameMain.getWindowHeight.toFloat / GameMain.getHeight.toFloat
    val buttonWidth = (GameMain.getWindowWidth.toDouble * 0.8).toInt
    val textSize = 40f * scale
    val buttonHeight = (textSize * 1.2).toInt
    val sidePadding = (GameMain.getWindowWidth.toDouble * 0.2).toInt
    val padding = sidePadding / 2
    
    val resumeButtonLayout = new LinearLayout(ctx)
    resumeButtonLayout.setPadding(sidePadding, padding, sidePadding, padding)
    val resumeButton = new android.widget.ImageButton(ctx)
    resumeButton.setBackgroundColor(0)
    resumeButton.setImageResource(R.drawable.resume_button)
    resumeButton.setAdjustViewBounds(true)
    resumeButton.setOnClickListener(new android.view.View.OnClickListener() {
        override def onClick(v : View)
        {
            Log.d("bigsleep", "Pause on resume Button Clicked")
            GameMain.processEvent(ResumeGame())
        }
    })
    resumeButtonLayout.addView(resumeButton)
    
    val quitButtonLayout = new LinearLayout(ctx)
    quitButtonLayout.setPadding(sidePadding, padding, sidePadding, padding)
    val quitButton = new android.widget.ImageButton(ctx)
    quitButton.setBackgroundColor(0)
    quitButton.setImageResource(R.drawable.quit_button)
    quitButton.setAdjustViewBounds(true)
    quitButton.setOnClickListener(new android.view.View.OnClickListener() {
        override def onClick(v : View)
        {
            Log.d("bigsleep", "Pause on quit Button Clicked")
            GameMain.processEvent(StartMenu())
        }
    })
    quitButtonLayout.addView(quitButton)
    
    
    val hsButtonLayout = new LinearLayout(ctx)
    hsButtonLayout.setPadding(sidePadding, padding, sidePadding, padding)
    val hsButton = new android.widget.ImageButton(ctx)
    hsButton.setBackgroundColor(0)
    hsButton.setImageResource(R.drawable.highscore_button)
    hsButton.setAdjustViewBounds(true)
    hsButton.setOnClickListener(new android.view.View.OnClickListener() {
        override def onClick(v : View)
        {
            Log.d("bigsleep", "Pause on quit Button Clicked")
            GameMain.processEvent(ShowHighscore())
        }
    })
    hsButtonLayout.addView(hsButton)
    
    widgets.addView(resumeButtonLayout, LayoutParams.WRAP_CONTENT)
    widgets.addView(quitButtonLayout, LayoutParams.WRAP_CONTENT)
    widgets.addView(hsButtonLayout, LayoutParams.WRAP_CONTENT)
    
    view.addView(widgets)
    
    def getView : View = view
}


class GameOvered(context : Context) extends GameState
{
    private val view = initView(context)
    
    private def initView(ctx : Context) : LinearLayout =
    {
        val widgets = new LinearLayout(ctx)
        widgets.setBackgroundColor(0x00000000)
        widgets.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.CENTER_VERTICAL)
        widgets.setOrientation(android.widget.LinearLayout.VERTICAL)
        
        val textSize = GameMain.getWindowHeight.toFloat / 10f
        val sidePadding = (GameMain.getWindowWidth.toDouble * 0.2).toInt
        val padding = sidePadding / 2
        
        val tv = new TextView(ctx)
        tv.setText("GAME OVER")
        tv.setTextSize(textSize)
        tv.setTextColor(0xFFFFFFFF)
        tv.setPadding(sidePadding, padding, sidePadding, padding)
        tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.CENTER_VERTICAL)
        
        val menuButtonLayout = new LinearLayout(ctx)
        menuButtonLayout.setPadding(sidePadding, padding, sidePadding, padding)
        val menuButton = new android.widget.ImageButton(ctx)
        menuButton.setBackgroundColor(0)
        menuButton.setImageResource(R.drawable.menu_button)
        menuButton.setAdjustViewBounds(true)
        menuButton.setOnClickListener(new android.view.View.OnClickListener() {
            override def onClick(v : View)
            {
                Log.d("bigsleep", "Pause on resume Button Clicked")
                GameMain.processEvent(StartMenu())
            }
        })
        menuButtonLayout.addView(menuButton)
        
        val hsButtonLayout = new LinearLayout(ctx)
        hsButtonLayout.setPadding(sidePadding, padding, sidePadding, padding)
        val hsButton = new android.widget.ImageButton(ctx)
        hsButton.setBackgroundColor(0)
        hsButton.setImageResource(R.drawable.highscore_button)
        hsButton.setAdjustViewBounds(true)
        hsButton.setOnClickListener(new android.view.View.OnClickListener() {
            override def onClick(v : View)
            {
                Log.d("bigsleep", "Pause on quit Button Clicked")
                GameMain.processEvent(ShowHighscore())
            }
        })
        hsButtonLayout.addView(hsButton)
        
        widgets.addView(tv, LayoutParams.WRAP_CONTENT)
        widgets.addView(menuButtonLayout, LayoutParams.WRAP_CONTENT)
        widgets.addView(hsButtonLayout, LayoutParams.WRAP_CONTENT)
        widgets
    }
    
    def getView : View = view
}

class GameCleared(context : Context) extends GameState
{
    private val view = initView(context)
    
    private def initView(ctx : Context) : LinearLayout =
    {
        val widgets = new LinearLayout(ctx)
        widgets.setBackgroundColor(0x00000000)
        widgets.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.CENTER_VERTICAL)
        widgets.setOrientation(android.widget.LinearLayout.VERTICAL)
        
        val textSize = GameMain.getWindowHeight.toFloat / 10f
        val sidePadding = (GameMain.getWindowWidth.toDouble * 0.2).toInt
        val padding = sidePadding / 2
        
        val tv = new TextView(ctx)
        tv.setText("GAME CLEAR")
        tv.setTextSize(textSize)
        tv.setTextColor(0xFFFFFFFF)
        tv.setPadding(sidePadding, padding, sidePadding, padding)
        tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.CENTER_VERTICAL)
        
        val menuButtonLayout = new LinearLayout(ctx)
        menuButtonLayout.setPadding(sidePadding, padding, sidePadding, padding)
        val menuButton = new android.widget.ImageButton(ctx)
        menuButton.setBackgroundColor(0)
        menuButton.setImageResource(R.drawable.menu_button)
        menuButton.setAdjustViewBounds(true)
        menuButton.setOnClickListener(new android.view.View.OnClickListener() {
            override def onClick(v : View)
            {
                Log.d("bigsleep", "Pause on resume Button Clicked")
                GameMain.processEvent(StartMenu())
            }
        })
        menuButtonLayout.addView(menuButton)
        
        val hsButtonLayout = new LinearLayout(ctx)
        hsButtonLayout.setPadding(sidePadding, padding, sidePadding, padding)
        val hsButton = new android.widget.ImageButton(ctx)
        hsButton.setBackgroundColor(0)
        hsButton.setImageResource(R.drawable.highscore_button)
        hsButton.setAdjustViewBounds(true)
        hsButton.setOnClickListener(new android.view.View.OnClickListener() {
            override def onClick(v : View)
            {
                Log.d("bigsleep", "Pause on quit Button Clicked")
                GameMain.processEvent(ShowHighscore())
            }
        })
        hsButtonLayout.addView(hsButton)
        
        widgets.addView(tv, LayoutParams.WRAP_CONTENT)
        widgets.addView(menuButtonLayout, LayoutParams.WRAP_CONTENT)
        widgets.addView(hsButtonLayout, LayoutParams.WRAP_CONTENT)
        widgets
    }
    
    def getView : View = view
}
