package org.rsmod.content.raids.tob.room.verzik

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

internal object VerzikArena {
    const val BASE_X = 3152
    const val BASE_Z = 4300

    val STATIC_ENTRANCE = CoordGrid(3168, 4302, 0)
    val STATIC_BOSS_SPAWN = CoordGrid(3166, 4323, 0)
    val STATIC_BARRIER = CoordGrid(3168, 4310, 0)

    const val BARRIER_DZ = 10

    fun inCombatArea(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in 6..26 && dz in 10..35
    }

    fun inRoom(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in 0..32 && dz in 0..40
    }

    fun players(origin: CoordGrid, playerList: PlayerList): List<Player> =
        playerList.filter { it.coords.level == origin.level && inCombatArea(origin, it.coords) }
}
