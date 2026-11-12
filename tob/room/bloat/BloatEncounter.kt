package org.rsmod.content.raids.tob.room.bloat

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
class BloatEncounter
@Inject
constructor(
    private val deps: BossDeps,
    private val hpBar: BossHpBarScript,
    private val events: EventBus,
) {
    internal var fight: BloatFight? = null
        private set
    private val spawnedNpcs = mutableListOf<Npc>()

    sealed interface Opened {
        class Room internal constructor(internal val fight: BloatFight, val enter: CoordGrid) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun of(npc: Npc): BloatFight? = fight?.takeIf { it.boss.npc === npc }

    internal fun at(tile: CoordGrid): BloatFight? = fight?.takeIf { it.contains(tile) }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened {
        close()
        val fight = BloatFight(resolve(BASE), deps, hpBar, events, hardMode)
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
        const val KEY = "tob_bloat"
        private val BASE = CoordGrid(BloatArena.BASE_X, BloatArena.BASE_Z, 0)
        private val ENTRANCE = CoordGrid(BloatArena.STATIC_ENTRANCE.x, BloatArena.STATIC_ENTRANCE.z, 0)
    }
}
