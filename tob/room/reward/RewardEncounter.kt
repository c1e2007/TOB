package org.rsmod.content.raids.tob.room.reward

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.content.raids.tob.resolve
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class RewardEncounter
@Inject
constructor(
    private val deps: BossDeps,
) {
    private val playerRewards = HashMap<String, List<RewardChest.RewardItem>>()

    sealed interface Opened {
        class Room internal constructor(val origin: CoordGrid, val enter: CoordGrid) : Opened
        data class Failed(val reason: String) : Opened
    }

    internal fun open(owner: Player, hardMode: Boolean = false): Opened {
        val rewards = RewardChest.rollReward(hardMode)
        playerRewards[owner.username] = rewards
        return Opened.Room(resolve(BASE), resolve(ENTRANCE))
    }

    internal fun claim(player: Player) {
        val rewards = playerRewards.remove(player.username) ?: return
        RewardChest.awardRewards(player, rewards)
    }

    internal fun close() {
        playerRewards.clear()
    }

    companion object {
        const val KEY = "tob_reward"
        private val BASE = CoordGrid(RewardArena.BASE_X, RewardArena.BASE_Z, 0)
        private val ENTRANCE = CoordGrid(RewardArena.STATIC_ENTRANCE.x, RewardArena.STATIC_ENTRANCE.z, 0)
    }
}
