package org.rsmod.content.raids.tob

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.party.RaidParty
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.content.raids.tob.party.TobParties
import org.rsmod.content.raids.tob.party.TobPartySettings
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.content.raids.tob.raid.TobRoom
import org.rsmod.content.raids.tob.room.bloat.BloatEncounter
import org.rsmod.content.raids.tob.room.maiden.MaidenEncounter
import org.rsmod.content.raids.tob.room.nylocas.NylocasEncounter
import org.rsmod.content.raids.tob.room.reward.RewardEncounter
import org.rsmod.content.raids.tob.room.shadowrealm.ShadowRealmEncounter
import org.rsmod.content.raids.tob.room.sotetseg.SotetsegEncounter
import org.rsmod.content.raids.tob.room.verzik.VerzikEncounter
import org.rsmod.content.raids.tob.room.xarpus.XarpusEncounter
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.Cheat
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(InternalApi::class)
class TobCheatScript
@Inject
constructor(
    private val launcher: ProtectedAccessLauncher,
    private val lobby: TobLobby,
    private val parties: TobParties,
    private val raids: TobRaids,
    private val maiden: MaidenEncounter,
    private val bloat: BloatEncounter,
    private val nylocas: NylocasEncounter,
    private val sotetseg: SotetsegEncounter,
    private val shadowRealm: ShadowRealmEncounter,
    private val xarpus: XarpusEncounter,
    private val verzik: VerzikEncounter,
    private val reward: RewardEncounter,
    private val clock: MapClock,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onCommand("tob") {
            desc = "Teleport to Theatre of Blood lobby"
            cheat { teleToLobby() }
        }
        onCommand("tobstart") {
            desc = "Start Theatre of Blood raid (Maiden)"
            cheat { startMaiden() }
        }
        onCommand("tobmaiden") {
            desc = "Start Theatre of Blood raid at Maiden"
            cheat { startMaiden() }
        }
        onCommand("tobbloat") {
            desc = "Enter Bloat room"
            cheat { enterBloat() }
        }
        onCommand("tobnylo") {
            desc = "Enter Nylocas room"
            cheat { enterNylo() }
        }
        onCommand("tobsotetseg") {
            desc = "Enter Sotetseg room"
            cheat { enterSotetseg() }
        }
        onCommand("tobshadow") {
            desc = "Enter Shadow Realm"
            cheat { enterShadow() }
        }
        onCommand("tobxarpus") {
            desc = "Enter Xarpus room"
            cheat { enterXarpus() }
        }
        onCommand("tobverzik") {
            desc = "Enter Verzik room"
            cheat { enterVerzik() }
        }
        onCommand("tobreward") {
            desc = "Enter ToB Reward room"
            cheat { enterReward() }
        }
        onCommand("tobleave") {
            desc = "Leave ToB"
            cheat { leave() }
        }
        onCommand("tobreset") {
            desc = "Reset the active ToB room (respawn boss, clear hazards)"
            cheat { resetRoom() }
        }
    }

    private fun Cheat.getOrCreateParty(): RaidParty<TobPartySettings>? {
        var party = parties.partyOf(player)
        if (party == null) {
            party = lobby.create(player, clock.cycle)
        }
        return party
    }

    private fun Cheat.teleToLobby() {
        launcher.launchLenient(player) {
            telejump(resolve(LOBBY_SPAWN), TeleportType.Exempt)
            mes("Teleported to Theatre of Blood lobby.")
        }
    }

    private fun Cheat.leave() {
        val party = parties.partyOf(player)
        if (party != null) {
            lobby.endRaid(party)
        }
        launcher.launchLenient(player) {
            telejump(resolve(LOBBY_SPAWN), TeleportType.Exempt)
            mes("You leave the Theatre of Blood.")
        }
    }

    private fun Cheat.startMaiden() {
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = maiden.open(player, party.settings.hardMode)) {
                is MaidenEncounter.Opened.Failed -> mes(opened.reason)
                is MaidenEncounter.Opened.Room -> {
                    val raid = raids.start(party)
                    raid.room = TobRoom.Maiden
                    telejump(resolve(TobRoom.Maiden.arrival), TeleportType.Exempt)
                    mes("Entered Maiden encounter.")
                }
            }
        }
    }

    private fun Cheat.enterBloat() {
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = bloat.open(player, party.settings.hardMode)) {
                is BloatEncounter.Opened.Failed -> mes(opened.reason)
                is BloatEncounter.Opened.Room -> {
                    val raid = raids.of(party) ?: raids.start(party)
                    raid.room = TobRoom.Bloat
                    telejump(resolve(TobRoom.Bloat.arrival), TeleportType.Exempt)
                    mes("Entered Bloat encounter.")
                }
            }
        }
    }

    private fun Cheat.enterNylo() {
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = nylocas.open(player, party.settings.hardMode)) {
                is NylocasEncounter.Opened.Failed -> mes(opened.reason)
                is NylocasEncounter.Opened.Room -> {
                    val raid = raids.of(party) ?: raids.start(party)
                    raid.room = TobRoom.Nylocas
                    telejump(resolve(TobRoom.Nylocas.arrival), TeleportType.Exempt)
                    mes("Entered Nylocas encounter.")
                }
            }
        }
    }

    private fun Cheat.enterSotetseg() {
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = sotetseg.open(player, party.settings.hardMode)) {
                is SotetsegEncounter.Opened.Failed -> mes(opened.reason)
                is SotetsegEncounter.Opened.Room -> {
                    val raid = raids.of(party) ?: raids.start(party)
                    raid.room = TobRoom.Sotetseg
                    telejump(resolve(TobRoom.Sotetseg.arrival), TeleportType.Exempt)
                    mes("Entered Sotetseg encounter.")
                }
            }
        }
    }

    private fun Cheat.enterShadow() {
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = shadowRealm.open(player, party.settings.hardMode)) {
                is ShadowRealmEncounter.Opened.Failed -> mes(opened.reason)
                is ShadowRealmEncounter.Opened.Room -> {
                    val raid = raids.of(party) ?: raids.start(party)
                    raid.room = TobRoom.ShadowRealm
                    telejump(resolve(TobRoom.ShadowRealm.arrival), TeleportType.Exempt)
                    mes("Entered Shadow Realm.")
                }
            }
        }
    }

    private fun Cheat.enterXarpus() {
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = xarpus.open(player, party.settings.hardMode)) {
                is XarpusEncounter.Opened.Failed -> mes(opened.reason)
                is XarpusEncounter.Opened.Room -> {
                    val raid = raids.of(party) ?: raids.start(party)
                    raid.room = TobRoom.Xarpus
                    telejump(resolve(TobRoom.Xarpus.arrival), TeleportType.Exempt)
                    mes("Entered Xarpus encounter.")
                }
            }
        }
    }

    private fun Cheat.enterVerzik() {
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = verzik.open(player, party.settings.hardMode)) {
                is VerzikEncounter.Opened.Failed -> mes(opened.reason)
                is VerzikEncounter.Opened.Room -> {
                    val raid = raids.of(party) ?: raids.start(party)
                    raid.room = TobRoom.Verzik
                    telejump(resolve(TobRoom.Verzik.arrival), TeleportType.Exempt)
                    mes("Entered Verzik encounter.")
                }
            }
        }
    }

    private fun Cheat.enterReward() {
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = reward.open(player, party.settings.hardMode)) {
                is RewardEncounter.Opened.Failed -> mes(opened.reason)
                is RewardEncounter.Opened.Room -> {
                    val raid = raids.of(party) ?: raids.start(party)
                    raid.room = TobRoom.Reward
                    telejump(resolve(TobRoom.Reward.arrival), TeleportType.Exempt)
                    mes("Entered Reward Vault.")
                }
            }
        }
    }

    private fun Cheat.resetRoom() {
        val raid = raids.of(player)
        val room = raid?.room ?: TobRoom.entries.firstOrNull { r ->
            player.coords.level == r.arrival.level &&
                player.coords.x in (r.arrival.x - 64)..(r.arrival.x + 64) &&
                player.coords.z in (r.arrival.z - 64)..(r.arrival.z + 64)
        } ?: TobRoom.Maiden

        raid?.cleared?.remove(room)
        val hardMode = raid?.party?.settings?.hardMode ?: false

        when (room) {
            TobRoom.Maiden -> {
                maiden.close()
                maiden.open(player, hardMode)
            }
            TobRoom.Bloat -> {
                bloat.close()
                bloat.open(player, hardMode)
            }
            TobRoom.Nylocas -> {
                nylocas.close()
                nylocas.open(player, hardMode)
            }
            TobRoom.Sotetseg -> {
                sotetseg.close()
                sotetseg.open(player, hardMode)
            }
            TobRoom.ShadowRealm -> {
                shadowRealm.close()
                shadowRealm.open(player, hardMode)
            }
            TobRoom.Xarpus -> {
                xarpus.close()
                xarpus.open(player, hardMode)
            }
            TobRoom.Verzik -> {
                verzik.close()
                verzik.open(player, hardMode)
            }
            TobRoom.Reward -> {
                reward.close()
                reward.open(player, hardMode)
            }
        }

        launcher.launchLenient(player) {
            mes("Reset ${room.roomName}: NPCs respawned and hazards cleared.")
        }
    }

    companion object {
        val LOBBY_SPAWN = CoordGrid(3668, 3219, 0)
    }
}
