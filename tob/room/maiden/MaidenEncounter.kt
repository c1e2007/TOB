package org.rsmod.content.raids.tob.room.maiden

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
class MaidenEncounter
@Inject
constructor(
    private val deps: BossDeps,
    private val hpBar: BossHpBarScript,
    private val events: EventBus,
) {
    internal var fight: MaidenFight? = null
        private set
    private val spawnedNpcs = mutableListOf<Npc>()

    sealed interface Opened {
        class Room internal constructor(internal val fight: MaidenFight, val enter: CoordGrid) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun of(npc: Npc): MaidenFight? = fight?.takeIf { it.boss.npc === npc }

    internal fun at(tile: CoordGrid): MaidenFight? = fight?.takeIf { it.contains(tile) }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened {
        close()
        val fight = MaidenFight(resolve(BASE), deps, hpBar, events, hardMode)
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
        const val KEY = "tob_maiden"
        private val BASE = CoordGrid(MaidenArena.BASE_X, MaidenArena.BASE_Z, 0)
        private val ENTRANCE = CoordGrid(MaidenArena.STATIC_ENTRANCE.x, MaidenArena.STATIC_ENTRANCE.z, 0)
    }
}
