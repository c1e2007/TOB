package org.rsmod.content.raids.tob.room.reward

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class RewardEncounter
@Inject
constructor(
    private val deps: BossDeps,
    private val manager: InstanceManager,
) {
    private val rooms = HashMap<InstanceId, CoordGrid>()
    private val playerRewards = HashMap<String, List<RewardChest.RewardItem>>()

    sealed interface Opened {
        class Room internal constructor(val origin: CoordGrid, val enter: CoordGrid, val session: InstanceSession) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened =
        when (val result = manager.create(owner, KEY, SPEC, InstanceAccess.Private, deps.mapClock.cycle)) {
            is InstanceManager.Result.Failed -> Opened.Failed(result.reason)
            is InstanceManager.Result.Joined -> Opened.Failed("Reward rooms cannot be joined directly.")
            is InstanceManager.Result.Created -> {
                val origin = checkNotNull(manager.resolveCoord(result.session, BASE))
                rooms[result.session.id] = origin
                val rewards = RewardChest.rollReward(hardMode)
                playerRewards[owner.username] = rewards
                Opened.Room(origin, result.enter, result.session)
            }
        }

    internal fun claim(player: Player) {
        val rewards = playerRewards.remove(player.username) ?: return
        RewardChest.awardRewards(player, rewards)
    }

    internal fun close(id: InstanceId) {
        rooms.remove(id)
    }

    companion object {
        const val KEY = "tob_reward"
        private val BASE = CoordGrid(RewardArena.BASE_X, RewardArena.BASE_Z, 0)
        private val ENTRANCE = CoordGrid(RewardArena.STATIC_ENTRANCE.x, RewardArena.STATIC_ENTRANCE.z, 0)
        private const val MAX_PARTY = 5

        private fun local(tile: CoordGrid) =
            RegionLocal(tile.level, tile.mx, tile.mz, tile.lx, tile.lz)

        private val TEMPLATE = RegionTemplate.create {
            copy(400, 537, 0) {
                zoneWidth = 8
                zoneLength = 8
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
