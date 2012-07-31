package org.bigsleep.spacemission

class CollisionHandler(val width : Double, val height : Double, val maxRadius : Double)
{
    class Cell
    {
        var playerShots = List.empty[Character]
        var enemys = List.empty[Character]
        var enemyShots = List.empty[Character]
        var near = List.empty[Cell]
    }
    
    private val xdiv : Int = (width / (maxRadius * 2d)).toInt
    private val ydiv : Int = (height / (maxRadius * 2d)).toInt
    private val dx : Double = (width / xdiv.toDouble)
    private val dy : Double = (height / ydiv.toDouble)
    private var cells = Array.fill(xdiv, ydiv)(new Cell)
    private var playerShots = List.empty[Character]
    private var largeEnemys = List.empty[Character]
    
    initializeCell
    
    def initializeCell()
    {
        (0 to xdiv - 1).foreach{x =>
            (0 to ydiv - 1).foreach{y =>
                val cell = cells(x)(y)
                (-1 to 1).foreach{i =>
                    (-1 to 1).foreach{j =>
                        val a = x + i
                        val b = y + j
                        if(i != j && a >= 0 && a < xdiv && b >=0 && b < ydiv){
                            cell.near = cells(a)(b) :: cell.near
                        }
                    }
                }
            }
        }
    }
    
    def clear()
    {
        cells.foreach{x =>
            x.foreach{y =>
                y.playerShots = List.empty[Character]
                y.enemys = List.empty[Character]
                y.enemyShots = List.empty[Character]
            }
        }
        playerShots = List.empty[Character]
        largeEnemys = List.empty[Character]
    }
    
    def registerEnemy(c : Character)
    {
        if(c.large){
            largeEnemys = c:: largeEnemys
        }else{
            val pos = c.motion.position
            val x = pos(0)
            val y = pos(1)
            val px = (x / dx).toInt
            val py = (y / dy).toInt
            if(px >= 0 && px < xdiv && py >= 0 && py < ydiv){
                val ce = cells(px)(py)
                ce.enemys = c :: ce.enemys
            }
        }
    }
    
    def registerEnemyShot(c : Character)
    {
        val pos = c.motion.position
        val x = pos(0)
        val y = pos(1)
        val px = (x / dx).toInt
        val py = (y / dy).toInt
        if(px >= 0 && px < xdiv && py >= 0 && py < ydiv){
            val ce = cells(px)(py)
            ce.enemyShots = c :: ce.enemyShots
        }else{
        }
    }
    
    def registerPlayerShot(c : Character)
    {
        val pos = c.motion.position
        val x = pos(0)
        val y = pos(1)
        val px = (x / dx).toInt
        val py = (y / dy).toInt
        if(px >= 0 && px < xdiv && py >= 0 && py < ydiv){
            val ce = cells(px)(py)
            ce.playerShots = c :: ce.playerShots
        }else{
        }
        playerShots = c :: playerShots
    }
    
    def apply(player : Character)
    {
        val pos = player.motion.position
        val x = pos(0)
        val y = pos(1)
        // enemys vs playerShots
        cells.foreach{x =>
            x.foreach{c =>
                val es = c.enemys
                val ps = c.playerShots
                es.foreach{e => ps.foreach{p => e.hit(p)}}
                
                c.near.foreach{nc =>
                    val nps = nc.playerShots
                    es.foreach{e => nps.foreach{p => e.hit(p)}}
                }
            }
        }
        largeEnemys.foreach{e => playerShots.foreach{p => e.hit(p)}}
        
        // player vs enemys
        // player vs enemyShots
        {
            val px = math.max(math.min((x / dx).toInt, xdiv - 1), 0)
            val py = math.max(math.min((y / dy).toInt, ydiv - 1), 0)
            val cell = cells(px)(py)
            val enemys = cell.enemys
            val enemyShots = cell.enemyShots
            
            enemys.foreach{e => player.hit(e)}
            enemyShots.foreach{e => player.hit(e)}
            largeEnemys.foreach{e => player.hit(e)}
        }
    }
}

