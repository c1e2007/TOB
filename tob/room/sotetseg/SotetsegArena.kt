package org.rsmod.content.raids.tob.room.sotetseg

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

internal object SotetsegArena {
    const val BASE_X = 3264
    const val BASE_Z = 4288

    val STATIC_ENTRANCE = CoordGrid(3279, 4293, 0)
    val STATIC_BOSS_SPAWN = CoordGrid(3277, 4326, 0)
    val STATIC_BARRIER = CoordGrid(3279, 4307, 0)

    const val BARRIER_DZ = 19

    fun inCombatArea(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in 5..25 && dz in 18..42
    }

    fun inRoom(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in 0..30 && dz in 0..45
    }

    fun players(origin: CoordGrid, playerList: PlayerList): List<Player> =
        playerList.filter { it.coords.level == origin.level && inCombatArea(origin, it.coords) }
}
