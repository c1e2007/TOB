package org.rsmod.content.raids.tob.room.nylocas

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

internal object NylocasArena {
    const val BASE_X = 3280
    const val BASE_Z = 4240

    val STATIC_ENTRANCE = CoordGrid(3295, 4283, 0)
    val STATIC_BOSS_SPAWN = CoordGrid(3294, 4247, 0)
    val STATIC_BARRIER = CoordGrid(3295, 4256, 0)

    val PILLAR_NW = CoordGrid(3289, 4253, 0)
    val PILLAR_SW = CoordGrid(3289, 4242, 0)
    val PILLAR_NE = CoordGrid(3300, 4253, 0)
    val PILLAR_SE = CoordGrid(3300, 4242, 0)

    const val BARRIER_DZ = 16

    fun inCombatArea(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        // Boss room is within dx 5..25 and dz 0..16
        return dx in 5..25 && dz in 0..16
    }

    fun inRoom(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in 0..30 && dz in -5..45
    }

    fun players(origin: CoordGrid, playerList: PlayerList): List<Player> =
        playerList.filter { it.coords.level == origin.level && inCombatArea(origin, it.coords) }
}
