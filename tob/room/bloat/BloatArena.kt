package org.rsmod.content.raids.tob.room.bloat

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import kotlin.random.Random

internal object BloatArena {
    const val BASE_X = 3288
    const val BASE_Z = 4440

    val STATIC_ENTRANCE = CoordGrid(3322, 4448, 0)
    val STATIC_SPECTATE = CoordGrid(3304, 4434, 0)
    val STATIC_BOSS_SPAWN = CoordGrid(3299, 4445, 0)

    val STATIC_NW = CoordGrid(3288, 4451, 0)
    val STATIC_SW = CoordGrid(3288, 4440, 0)
    val STATIC_SE = CoordGrid(3299, 4440, 0)
    val STATIC_NE = CoordGrid(3299, 4451, 0)

    const val BARRIER_DX = 16

    fun inCombatArea(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        // Combat corridor is within 0..16 x, 0..16 z
        return dx in 0..16 && dz in 0..16
    }

    fun isPillar(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        // Central pillar occupies dx 5..11 and dz 5..11
        return dx in 5..11 && dz in 5..11
    }

    fun isWalkableCorridor(origin: CoordGrid, tile: CoordGrid): Boolean {
        return inCombatArea(origin, tile) && !isPillar(origin, tile)
    }

    fun inRoom(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in -4..35 && dz in -5..20
    }

    fun players(origin: CoordGrid, playerList: PlayerList): List<Player> =
        playerList.filter { it.coords.level == origin.level && inCombatArea(origin, it.coords) }

    fun randomMeatTile(origin: CoordGrid): CoordGrid {
        while (true) {
            val dx = Random.nextInt(0, 17)
            val dz = Random.nextInt(0, 17)
            val tile = origin.translate(dx, dz)
            if (isWalkableCorridor(origin, tile)) {
                return tile
            }
        }
    }
}
