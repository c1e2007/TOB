package org.rsmod.content.raids.tob

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.content.raids.tob.raid.TobRoom
import org.rsmod.content.raids.tob.raid.TobRoomCleared
import org.rsmod.content.raids.tob.room.bloat.BloatEncounter
import org.rsmod.content.raids.tob.room.nylocas.NylocasEncounter
import org.rsmod.content.raids.tob.room.reward.RewardEncounter
import org.rsmod.content.raids.tob.room.sotetseg.SotetsegEncounter
import org.rsmod.content.raids.tob.room.verzik.VerzikEncounter
import org.rsmod.content.raids.tob.room.xarpus.XarpusEncounter
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
    private val launcher: ProtectedAccessLauncher,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onEvent<TobRoomCleared> {
            for (raid in raids.all()) {
                if (raid.room == room) {
                    raid.markCleared(room)
                }
            }
        }

        for (id in listOf(33113, 32751, 32738)) {
            onProtectedEvent<LocEvents.Op1>(id) {
                advance()
            }
        }
    }

    private fun ProtectedAccess.advance() {
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
            TobRoom.Reward, TobRoom.ShadowRealm -> null
        }

        if (nextRoom == null) {
            mes("Leaving the Theatre...")
            lobby.endRaid(raid.party)
            telejump(resolve(LOBBY_EXIT), TeleportType.Exempt)
            return
        }

        raid.room = nextRoom
        val hardMode = raid.party.settings.hardMode
        when (nextRoom) {
            TobRoom.Bloat -> bloat.open(player, hardMode)
            TobRoom.Nylocas -> nylocas.open(player, hardMode)
            TobRoom.Sotetseg -> sotetseg.open(player, hardMode)
            TobRoom.Xarpus -> xarpus.open(player, hardMode)
            TobRoom.Verzik -> verzik.open(player, hardMode)
            TobRoom.Reward -> reward.open(player, hardMode)
            else -> {}
        }

        for (member in raid.party.members) {
            if (member === player) {
                telejump(resolve(nextRoom.arrival), TeleportType.Exempt)
                mes("You advance to ${nextRoom.roomName}...")
            } else {
                launcher.launchLenient(member) {
                    telejump(resolve(nextRoom.arrival), TeleportType.Exempt)
                    mes("Your party advances to ${nextRoom.roomName}...")
                }
            }
        }
    }

    companion object {
        val LOBBY_EXIT = CoordGrid(3668, 3219, 0)
    }
}
