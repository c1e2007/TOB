package org.rsmod.content.raids.tob.room.shadowrealm

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.content.raids.tob.raid.TobRoom
import org.rsmod.content.raids.tob.resolve
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class ShadowRealmEncounter
@Inject
constructor(
    private val deps: BossDeps,
) {
    sealed interface Opened {
        class Room internal constructor(val enter: CoordGrid) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened {
        return Opened.Room(resolve(TobRoom.ShadowRealm.arrival))
    }

    internal fun close() {}
}
