package org.rsmod.content.raids.tob.room.reward

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

internal object RewardArena {
    const val BASE_X = 3200
    const val BASE_Z = 4300

    val STATIC_ENTRANCE = CoordGrid(3237, 4319, 0)

    val CHEST_COORDS = listOf(
        CoordGrid(3233, 4342, 0),
        CoordGrid(3240, 4339, 0),
        CoordGrid(3226, 4339, 0),
        CoordGrid(3226, 4335, 0),
        CoordGrid(3240, 4335, 0),
    )

    fun inRoom(origin: CoordGrid, tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        return dx in 15..50 && dz in 10..50
    }

    fun players(origin: CoordGrid, playerList: PlayerList): List<Player> =
        playerList.filter { it.coords.level == origin.level && inRoom(origin, it.coords) }
}
