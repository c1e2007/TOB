package org.rsmod.content.raids.tob.room.xarpus

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

internal object XarpusArena {
    const val BASE_X = 3152
    const val BASE_Z = 4368
    const val LEVEL = 1

    val STATIC_ENTRANCE = CoordGrid(3170, 4375, LEVEL)
    val STATIC_BOSS_SPAWN = CoordGrid(3169, 4386, LEVEL)
    val STATIC_BARRIER = CoordGrid(3170, 4380, LEVEL)

    const val BARRIER_DZ = 12

    fun inCombatArea(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in 8..26 && dz in 12..35
    }

    fun inRoom(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in 0..35 && dz in 0..40
    }

    fun players(origin: CoordGrid, playerList: PlayerList): List<Player> =
        playerList.filter { it.coords.level == origin.level && inCombatArea(origin, it.coords) }
}
