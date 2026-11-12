package org.rsmod.content.raids.tob

import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
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

class TobEntryScript
@Inject
constructor(
    private val parties: TobParties,
    private val lobby: TobLobby,
    private val raids: TobRaids,
    private val maiden: MaidenEncounter,
    private val clock: MapClock,
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
            if (raid != null) {
                telejump(resolve(raid.room.arrival), TeleportType.Exempt)
                mes("You rejoin your party in the Theatre.")
            } else {
                mes("Your party leader must enter first.")
            }
        }
    }

    private fun ProtectedAccess.startRaid(party: RaidParty<TobPartySettings>) {
        when (val opened = maiden.open(player, party.settings.hardMode)) {
            is MaidenEncounter.Opened.Failed -> {
                mes(opened.reason)
                return
            }
            is MaidenEncounter.Opened.Room -> {
                val raid = raids.start(party)
                raid.room = TobRoom.Maiden

                mes("You step into the Theatre of Blood...")
                telejump(resolve(TobRoom.Maiden.arrival), TeleportType.Exempt)
                for (member in party.members) {
                    if (member === player) continue
                    member.mes("Your party has entered the Theatre of Blood. Step inside now!")
                }
            }
        }
    }
}
