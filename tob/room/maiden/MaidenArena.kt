package org.rsmod.content.raids.tob.room.maiden

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

internal object MaidenArena {
    const val BASE_X = 3136
    const val BASE_Z = 4416

    const val ARENA_ENTRY_DX = 83
    const val ARENA_ENTRY_DZ = 43

    const val BARRIER_DX = 49
    const val BARRIER_DZ = 31

    const val BOSS_SPAWN_DX = 26
    const val BOSS_SPAWN_DZ = 28

    const val SPECTATE_DX = 54
    const val SPECTATE_DZ = 37

    const val ROOM_WIDTH = 96
    const val ROOM_LENGTH = 56

    // Static coordinates
    val STATIC_ENTRANCE = CoordGrid(3219, 4459, 0)
    val STATIC_BARRIER = CoordGrid(3185, 4447, 0)
    val STATIC_BOSS_SPAWN = CoordGrid(3162, 4444, 0)
    val STATIC_SPECTATE = CoordGrid(3190, 4453, 0)

    val NYLO_SPAWNS_NORTH = listOf(
        CoordGrid(3168, 4455, 0),
        CoordGrid(3174, 4455, 0),
        CoordGrid(3180, 4455, 0),
        CoordGrid(3186, 4455, 0),
    )

    val NYLO_SPAWNS_SOUTH = listOf(
        CoordGrid(3168, 4435, 0),
        CoordGrid(3174, 4435, 0),
        CoordGrid(3180, 4435, 0),
        CoordGrid(3186, 4435, 0),
    )

    val STORM_DIRECTIONS = listOf(
        CoordGrid(3167, 4446, 0), // East
        CoordGrid(3165, 4449, 0), // North
        CoordGrid(3165, 4444, 0), // South
        CoordGrid(3162, 4447, 0), // West
    )

    fun inRoom(origin: CoordGrid, tile: CoordGrid): Boolean =
        tile.x in origin.x until (origin.x + ROOM_WIDTH) &&
            tile.z in origin.z until (origin.z + ROOM_LENGTH) &&
            tile.level == origin.level

    fun inCombatArea(origin: CoordGrid, tile: CoordGrid): Boolean =
        tile.x in origin.x until (origin.x + BARRIER_DX) &&
            tile.z in origin.z until (origin.z + ROOM_LENGTH) &&
            tile.level == origin.level

    fun players(origin: CoordGrid, playerList: PlayerList): List<Player> =
        playerList.filterNotNull().filter { inCombatArea(origin, it.coords) }
}
