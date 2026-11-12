package org.rsmod.content.raids.tob.room.verzik

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
class VerzikEncounter
@Inject
constructor(
    private val deps: BossDeps,
    private val hpBar: BossHpBarScript,
    private val events: EventBus,
) {
    internal var fight: VerzikFight? = null
        private set
    private val spawnedNpcs = mutableListOf<Npc>()

    sealed interface Opened {
        class Room internal constructor(internal val fight: VerzikFight, val enter: CoordGrid) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun of(npc: Npc): VerzikFight? = fight?.takeIf { it.boss.npc === npc }

    internal fun at(tile: CoordGrid): VerzikFight? = fight?.takeIf { it.contains(tile) }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened {
        close()
        val fight = VerzikFight(resolve(BASE), deps, hpBar, events, hardMode)
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
        const val KEY = "tob_verzik"
        private val BASE = CoordGrid(VerzikArena.BASE_X, VerzikArena.BASE_Z, 0)
        private val ENTRANCE = CoordGrid(VerzikArena.STATIC_ENTRANCE.x, VerzikArena.STATIC_ENTRANCE.z, 0)
    }
}
