package org.rsmod.content.raids.tob.room.maiden

import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.content.raids.tob.raid.TobFight
import org.rsmod.content.raids.tob.raid.TobRoom
import org.rsmod.content.raids.tob.raid.TobRoomCleared
import org.rsmod.content.raids.tob.resolve
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class MaidenFight(
    val origin: CoordGrid,
    private val deps: BossDeps,
    private val hpBar: BossHpBarScript,
    private val events: EventBus,
    val hardMode: Boolean = false,
) : TobFight {
    val boss = MaidenBoss(resolve(origin), deps, hardMode)

    private var started = false
    private var startedAt = 0
    private var endedAt: Int? = null

    override val elapsedTicks: Int?
        get() = if (started) (endedAt ?: deps.mapClock.cycle) - startedAt else null

    override val finished: Boolean
        get() = endedAt != null

    fun contains(tile: CoordGrid): Boolean = MaidenArena.inRoom(resolve(origin), tile)

    fun fighters(): List<Player> = MaidenArena.players(resolve(origin), deps.playerList)

    fun open(): List<Npc> {
        val maidenNpc = boss.spawn()
        return listOf(maidenNpc)
    }

    fun start() {
        if (started) return
        started = true
        startedAt = deps.mapClock.cycle
    }

    fun tick() {
        if (!started || finished) return

        val fighters = fighters()
        boss.tick(fighters)

        if (boss.isDead) {
            complete()
        }
    }

    private fun complete() {
        if (finished) return
        endedAt = deps.mapClock.cycle
        events.publish(TobRoomCleared(TobRoom.Maiden))
    }

    fun dispose() {
        boss.cleanup()
    }
}
