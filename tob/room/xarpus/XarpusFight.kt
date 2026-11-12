package org.rsmod.content.raids.tob.room.xarpus

import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.InstanceSession
import org.rsmod.content.raids.tob.raid.TobFight
import org.rsmod.content.raids.tob.raid.TobRoom
import org.rsmod.content.raids.tob.raid.TobRoomCleared
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class XarpusFight(
    override val session: InstanceSession,
    val origin: CoordGrid,
    private val deps: BossDeps,
    private val hpBar: BossHpBarScript,
    private val events: EventBus,
    val hardMode: Boolean = false,
) : TobFight {
    val boss = XarpusBoss(origin, deps, hardMode)

    private var started = false
    private var startedAt = 0
    private var endedAt: Int? = null

    override val elapsedTicks: Int?
        get() = if (started) (endedAt ?: deps.mapClock.cycle) - startedAt else null

    override val finished: Boolean
        get() = endedAt != null

    fun contains(tile: CoordGrid): Boolean = XarpusArena.inRoom(origin, tile)

    fun fighters(): List<Player> = XarpusArena.players(origin, deps.playerList)

    fun open(): List<Npc> {
        val xarpusNpc = boss.spawn()
        return listOf(xarpusNpc)
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

        if (boss.npc.hitpoints <= 0) {
            complete()
        }
    }

    private fun complete() {
        if (finished) return
        endedAt = deps.mapClock.cycle
        events.publish(TobRoomCleared(session.id, TobRoom.Xarpus))
    }

    fun dispose() {
        boss.cleanup()
    }
}
