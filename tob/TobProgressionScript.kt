package org.rsmod.content.raids.tob

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.content.raids.tob.raid.TobRaid
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.content.raids.tob.raid.TobRoom
import org.rsmod.content.raids.tob.raid.TobRoomCleared
import org.rsmod.content.raids.tob.room.bloat.BloatEncounter
import org.rsmod.content.raids.tob.room.nylocas.NylocasEncounter
import org.rsmod.content.raids.tob.room.reward.RewardEncounter
import org.rsmod.content.raids.tob.room.sotetseg.SotetsegEncounter
import org.rsmod.content.raids.tob.room.verzik.VerzikEncounter
import org.rsmod.content.raids.tob.room.xarpus.XarpusEncounter
import org.rsmod.game.MapClock
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(InternalApi::class)
class TobProgressionScript
@Inject
constructor(
    private val lobby: TobLobby,
    private val raids: TobRaids,
    private val bloat: BloatEncounter,
    private val nylocas: NylocasEncounter,
    private val sotetseg: SotetsegEncounter,
    private val xarpus: XarpusEncounter,
    private val verzik: VerzikEncounter,
    private val reward: RewardEncounter,
    private val manager: InstanceManager,
    private val launcher: ProtectedAccessLauncher,
    private val clock: MapClock,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onEvent<TobRoomCleared> {
            val raid = lobby.raidParty(instanceId)?.let { raids.of(it) }
            raid?.markCleared(room)
        }

        for (id in listOf(33113, 32751, 32738)) {
            onProtectedEvent<LocEvents.Op1>(id) {
                advance()
            }
        }
    }

    private suspend fun ProtectedAccess.advance() {
        val raid = raids.of(player)
        if (raid == null) {
            mes("You are not currently in a Theatre of Blood raid.")
            return
        }
        if (!raid.isCleared(raid.room)) {
            mes("You must complete this challenge before proceeding.")
            return
        }

        val nextRoom = when (raid.room) {
            TobRoom.Maiden -> TobRoom.Bloat
            TobRoom.Bloat -> TobRoom.Nylocas
            TobRoom.Nylocas -> TobRoom.Sotetseg
            TobRoom.Sotetseg -> TobRoom.Xarpus
            TobRoom.Xarpus -> TobRoom.Verzik
            TobRoom.Verzik -> TobRoom.Reward
            TobRoom.Reward -> null
        }

        if (nextRoom == null) {
            mes("Leaving the Theatre...")
            val oldSession = raid.roomId?.let { manager.sessionForId(it) }
            if (oldSession != null) {
                for (member in raid.party.members) {
                    manager.leave(member, oldSession, clock.cycle)
                }
            }
            lobby.endRaid(raid.party)
            telejump(LOBBY_EXIT, TeleportType.Exempt)
            return
        }

        // Leave old session so the instance manager destroys the old room before creating the new one
        val oldSession = raid.roomId?.let { manager.sessionForId(it) }
        if (oldSession != null) {
            for (member in raid.party.members) {
                manager.leave(member, oldSession, clock.cycle)
            }
        }
        manager.cancelPendingEntry(player, clock.cycle)
        player.uuid?.let { manager.sessionForOwner(it) }

        val hardMode = raid.party.settings.hardMode
        when (nextRoom) {
            TobRoom.Bloat -> when (val opened = bloat.open(player, hardMode)) {
                is BloatEncounter.Opened.Failed -> mes(opened.reason)
                is BloatEncounter.Opened.Room -> moveTo(raid, nextRoom, opened.enter, opened.fight.session)
            }
            TobRoom.Nylocas -> when (val opened = nylocas.open(player, hardMode)) {
                is NylocasEncounter.Opened.Failed -> mes(opened.reason)
                is NylocasEncounter.Opened.Room -> moveTo(raid, nextRoom, opened.enter, opened.fight.session)
            }
            TobRoom.Sotetseg -> when (val opened = sotetseg.open(player, hardMode)) {
                is SotetsegEncounter.Opened.Failed -> mes(opened.reason)
                is SotetsegEncounter.Opened.Room -> moveTo(raid, nextRoom, opened.enter, opened.fight.session)
            }
            TobRoom.Xarpus -> when (val opened = xarpus.open(player, hardMode)) {
                is XarpusEncounter.Opened.Failed -> mes(opened.reason)
                is XarpusEncounter.Opened.Room -> moveTo(raid, nextRoom, opened.enter, opened.fight.session)
            }
            TobRoom.Verzik -> when (val opened = verzik.open(player, hardMode)) {
                is VerzikEncounter.Opened.Failed -> mes(opened.reason)
                is VerzikEncounter.Opened.Room -> moveTo(raid, nextRoom, opened.enter, opened.fight.session)
            }
            TobRoom.Reward -> when (val opened = reward.open(player, hardMode)) {
                is RewardEncounter.Opened.Failed -> mes(opened.reason)
                is RewardEncounter.Opened.Room -> moveTo(raid, nextRoom, opened.enter, opened.session)
            }
            else -> {}
        }
    }

    private fun ProtectedAccess.moveTo(
        raid: TobRaid,
        room: TobRoom,
        enter: CoordGrid,
        session: InstanceSession,
    ) {
        raid.room = room
        raid.roomId = session.id
        lobby.bindRaid(session.id, raid.party)

        for (member in raid.party.members) {
            if (member === player) {
                telejump(enter, TeleportType.Exempt)
                manager.finalizeEntry(player, session, clock.cycle)
                mes("You advance to ${room.roomName}...")
            } else {
                launcher.launchLenient(member) {
                    val joined = manager.join(member, session, clock.cycle, forceAccess = true)
                    if (joined is InstanceManager.Result.Joined) {
                        telejump(enter, TeleportType.Exempt)
                        manager.finalizeEntry(member, session, clock.cycle)
                        mes("Your party advances to ${room.roomName}...")
                    }
                }
            }
        }
    }

    companion object {
        val LOBBY_EXIT = CoordGrid(3668, 3219, 0)
    }
}
