package org.rsmod.content.raids.tob.room.nylocas

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
class NylocasEncounter
@Inject
constructor(
    private val deps: BossDeps,
    private val hpBar: BossHpBarScript,
    private val events: EventBus,
) {
    internal var fight: NylocasFight? = null
        private set
    private val spawnedNpcs = mutableListOf<Npc>()

    sealed interface Opened {
        class Room internal constructor(internal val fight: NylocasFight, val enter: CoordGrid) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun of(npc: Npc): NylocasFight? = fight?.takeIf { it.boss.npc === npc }

    internal fun at(tile: CoordGrid): NylocasFight? = fight?.takeIf { it.contains(tile) }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened {
        close()
        val fight = NylocasFight(resolve(BASE), deps, hpBar, events, hardMode)
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
        const val KEY = "tob_nylocas"
        private val BASE = CoordGrid(NylocasArena.BASE_X, NylocasArena.BASE_Z, 0)
        private val ENTRANCE = CoordGrid(NylocasArena.STATIC_ENTRANCE.x, NylocasArena.STATIC_ENTRANCE.z, 0)
    }
}
