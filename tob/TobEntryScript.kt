package org.rsmod.content.raids.tob

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.content.raids.party.RaidParty
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.content.raids.tob.party.TobParties
import org.rsmod.content.raids.tob.party.TobPartySettings
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.content.raids.tob.raid.TobRoom
import org.rsmod.content.raids.tob.room.maiden.MaidenEncounter
import org.rsmod.game.MapClock
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(InternalApi::class)
class TobEntryScript
@Inject
constructor(
    private val parties: TobParties,
    private val lobby: TobLobby,
    private val raids: TobRaids,
    private val maiden: MaidenEncounter,
    private val manager: InstanceManager,
    private val clock: MapClock,
    private val launcher: ProtectedAccessLauncher,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onProtectedEvent<LocEvents.Op1>(32653) { enter() }
        onProtectedEvent<LocEvents.Op1>(32655) { enter() }
    }

    private suspend fun ProtectedAccess.enter() {
        var party = parties.partyOf(player)
        if (party == null) {
            val confirmed = choice2(
                "Yes, enter the Theatre.",
                true,
                "Cancel.",
                false,
                title = "Would you like to enter the Theatre of Blood?",
            )
            if (!confirmed) return
            party = lobby.create(player, clock.cycle)
            if (party == null) {
                mes("Failed to create raid party.")
                return
            }
        }

        if (party.isLeader(player)) {
            startRaid(party)
        } else {
            val raid = raids.of(party)
            if (raid != null && raid.roomId != null) {
                val session = manager.sessionForId(raid.roomId!!)
                if (session != null) {
                    val joined = manager.join(player, session, clock.cycle, forceAccess = true)
                    if (joined is InstanceManager.Result.Joined) {
                        telejump(raid.room.arrival, TeleportType.Exempt)
                        manager.finalizeEntry(player, session, clock.cycle)
                        mes("You rejoin your party in the Theatre.")
                    }
                }
            } else {
                mes("Your party leader must enter first.")
            }
        }
    }

    private suspend fun ProtectedAccess.startRaid(party: RaidParty<TobPartySettings>) {
        val existing = manager.sessionForPlayer(player)
            ?: player.uuid?.let { manager.sessionForOwner(it) }
            ?: manager.sessionForMemberName(player.displayName)
        if (existing != null) {
            manager.leave(player, existing, clock.cycle)
        }
        manager.cancelPendingEntry(player, clock.cycle)
        player.uuid?.let { manager.sessionForOwner(it) }

        val room = when (val opened = maiden.open(player, party.settings.hardMode)) {
            is MaidenEncounter.Opened.Failed -> {
                mes(opened.reason)
                return
            }
            is MaidenEncounter.Opened.Room -> opened
        }
        val session = room.fight.session
        lobby.bindRaid(session.id, party)
        val raid = raids.start(party)
        raid.roomId = session.id
        raid.room = TobRoom.Maiden

        mes("You step into the Theatre of Blood...")
        withInstanceEnterTransition(InstanceEnterTransition()) {
            telejump(room.enter, TeleportType.Exempt)
            manager.finalizeEntry(player, session, clock.cycle)
        }
        for (member in party.members) {
            if (member === player) continue
            member.mes("Your party has entered the Theatre of Blood. Step inside now!")
        }
    }
}
