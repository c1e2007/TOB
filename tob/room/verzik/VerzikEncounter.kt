package org.rsmod.content.raids.tob.room.verzik

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class VerzikEncounter
@Inject
constructor(
    private val deps: BossDeps,
    private val manager: InstanceManager,
    private val hpBar: BossHpBarScript,
    private val events: EventBus,
) {
    private val fights = HashMap<InstanceId, VerzikFight>()

    sealed interface Opened {
        class Room internal constructor(internal val fight: VerzikFight, val enter: CoordGrid) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun of(id: InstanceId): VerzikFight? = fights[id]

    internal fun of(npc: Npc): VerzikFight? = fights.values.firstOrNull { it.boss.npc === npc }

    internal fun at(tile: CoordGrid): VerzikFight? = fights.values.firstOrNull { it.contains(tile) }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened =
        when (val result = manager.create(owner, KEY, SPEC, InstanceAccess.Private, deps.mapClock.cycle)) {
            is InstanceManager.Result.Failed -> Opened.Failed(result.reason)
            is InstanceManager.Result.Joined -> Opened.Failed("Verzik rooms cannot be joined directly.")
            is InstanceManager.Result.Created -> {
                val origin = checkNotNull(manager.resolveCoord(result.session, BASE))
                val fight = VerzikFight(result.session, origin, deps, hpBar, events, hardMode)
                for (npc in fight.open()) manager.attachNpc(result.session.id, npc)
                fights[result.session.id] = fight
                Opened.Room(fight, result.enter)
            }
        }

    internal fun close(id: InstanceId) {
        fights.remove(id)?.dispose()
    }

    companion object {
        const val KEY = "tob_verzik"
        private val BASE = CoordGrid(VerzikArena.BASE_X, VerzikArena.BASE_Z, 0)
        private val ENTRANCE = CoordGrid(VerzikArena.STATIC_ENTRANCE.x, VerzikArena.STATIC_ENTRANCE.z, 0)
        private const val MAX_PARTY = 5

        private fun local(tile: CoordGrid) =
            RegionLocal(tile.level, tile.mx, tile.mz, tile.lx, tile.lz)

        private val TEMPLATE = RegionTemplate.create {
            copy(394, 537, 0) {
                zoneWidth = 12
                zoneLength = 7
            }
        }

        private val AREA =
            InstanceArea.template(
                template = TEMPLATE,
                enterCoord = local(ENTRANCE),
                exitCoord = CoordGrid(3667, 3219, 0),
            )

        private val SPEC =
            InstanceSpec(
                fee = 0,
                maxPlayers = MAX_PARTY,
                reclaimTicks = 0,
                graceTicks = 0,
                destroyWhenEmpty = true,
                area = AREA,
                settingsRowId = 0,
            )
    }
}
