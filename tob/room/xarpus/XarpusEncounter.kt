package org.rsmod.content.raids.tob.room.xarpus

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.content.raids.tob.resolve
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class XarpusEncounter
@Inject
constructor(
    private val deps: BossDeps,
    private val hpBar: BossHpBarScript,
    private val events: EventBus,
) {
    internal var fight: XarpusFight? = null
        private set
    private val spawnedNpcs = mutableListOf<Npc>()

    sealed interface Opened {
        class Room internal constructor(internal val fight: XarpusFight, val enter: CoordGrid) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun of(npc: Npc): XarpusFight? = fight?.takeIf { it.boss.npc === npc }

    internal fun at(tile: CoordGrid): XarpusFight? = fight?.takeIf { it.contains(tile) }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened {
        close()
        val fight = XarpusFight(resolve(BASE), deps, hpBar, events, hardMode)
        val npcs = fight.open()
        spawnedNpcs.addAll(npcs)
        this.fight = fight
        return Opened.Room(fight, resolve(ENTRANCE))
    }

    internal fun close() {
        fight?.dispose()
        fight = null
        for (npc in spawnedNpcs) {
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
        spawnedNpcs.clear()
    }

    companion object {
        const val KEY = "tob_xarpus"
        private val BASE = CoordGrid(XarpusArena.BASE_X, XarpusArena.BASE_Z, XarpusArena.LEVEL)
        private val ENTRANCE = CoordGrid(XarpusArena.STATIC_ENTRANCE.x, XarpusArena.STATIC_ENTRANCE.z, XarpusArena.LEVEL)
    }
}
