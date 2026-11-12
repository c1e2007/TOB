package org.rsmod.content.raids.tob

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
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
import org.rsmod.content.raids.tob.room.sotetseg.SotetsegEncounter
import org.rsmod.content.raids.tob.room.verzik.VerzikEncounter
import org.rsmod.content.raids.tob.room.xarpus.XarpusEncounter
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
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
    private val xarpus: XarpusEncounter,
    private val verzik: VerzikEncounter,
    private val reward: RewardEncounter,
    private val manager: InstanceManager,
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
            desc = "Leave ToB instance"
            cheat { leave() }
        }
    }

    private fun cleanPrevious(player: Player) {
        val existing = manager.sessionForPlayer(player)
            ?: player.uuid?.let { manager.sessionForOwner(it) }
            ?: manager.sessionForMemberName(player.displayName)
        if (existing != null) {
            manager.leave(player, existing, clock.cycle)
        }
        manager.cancelPendingEntry(player, clock.cycle)
        player.uuid?.let { manager.sessionForOwner(it) }
    }

    private fun Cheat.getOrCreateParty(): RaidParty<TobPartySettings>? {
        var party = parties.partyOf(player)
        if (party == null) {
            party = lobby.create(player, clock.cycle)
        }
        return party
    }

    private fun Cheat.teleToLobby() {
        cleanPrevious(player)
        launcher.launchLenient(player) {
            telejump(LOBBY_SPAWN, TeleportType.Exempt)
            mes("Teleported to Theatre of Blood lobby.")
        }
    }

    private fun Cheat.leave() {
        cleanPrevious(player)
        val party = parties.partyOf(player)
        if (party != null) {
            lobby.endRaid(party)
        }
        launcher.launchLenient(player) {
            telejump(LOBBY_SPAWN, TeleportType.Exempt)
            mes("You leave the Theatre of Blood.")
        }
    }

    private fun Cheat.startMaiden() {
        cleanPrevious(player)
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = maiden.open(player, party.settings.hardMode)) {
                is MaidenEncounter.Opened.Failed -> mes(opened.reason)
                is MaidenEncounter.Opened.Room -> {
                    val session = opened.fight.session
                    lobby.bindRaid(session.id, party)
                    val raid = raids.start(party)
                    raid.roomId = session.id
                    raid.room = TobRoom.Maiden
                    telejump(opened.enter, TeleportType.Exempt)
                    manager.finalizeEntry(player, session, clock.cycle)
                    mes("Entered Maiden encounter.")
                }
            }
        }
    }

    private fun Cheat.enterBloat() {
        cleanPrevious(player)
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = bloat.open(player, party.settings.hardMode)) {
                is BloatEncounter.Opened.Failed -> mes(opened.reason)
                is BloatEncounter.Opened.Room -> {
                    val session = opened.fight.session
                    lobby.bindRaid(session.id, party)
                    val raid = raids.start(party)
                    raid.roomId = session.id
                    raid.room = TobRoom.Bloat
                    telejump(opened.enter, TeleportType.Exempt)
                    manager.finalizeEntry(player, session, clock.cycle)
                    mes("Entered Bloat encounter.")
                }
            }
        }
    }

    private fun Cheat.enterNylo() {
        cleanPrevious(player)
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = nylocas.open(player, party.settings.hardMode)) {
                is NylocasEncounter.Opened.Failed -> mes(opened.reason)
                is NylocasEncounter.Opened.Room -> {
                    val session = opened.fight.session
                    lobby.bindRaid(session.id, party)
                    val raid = raids.start(party)
                    raid.roomId = session.id
                    raid.room = TobRoom.Nylocas
                    telejump(opened.enter, TeleportType.Exempt)
                    manager.finalizeEntry(player, session, clock.cycle)
                    mes("Entered Nylocas encounter.")
                }
            }
        }
    }

    private fun Cheat.enterSotetseg() {
        cleanPrevious(player)
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = sotetseg.open(player, party.settings.hardMode)) {
                is SotetsegEncounter.Opened.Failed -> mes(opened.reason)
                is SotetsegEncounter.Opened.Room -> {
                    val session = opened.fight.session
                    lobby.bindRaid(session.id, party)
                    val raid = raids.start(party)
                    raid.roomId = session.id
                    raid.room = TobRoom.Sotetseg
                    telejump(opened.enter, TeleportType.Exempt)
                    manager.finalizeEntry(player, session, clock.cycle)
                    mes("Entered Sotetseg encounter.")
                }
            }
        }
    }

    private fun Cheat.enterXarpus() {
        cleanPrevious(player)
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = xarpus.open(player, party.settings.hardMode)) {
                is XarpusEncounter.Opened.Failed -> mes(opened.reason)
                is XarpusEncounter.Opened.Room -> {
                    val session = opened.fight.session
                    lobby.bindRaid(session.id, party)
                    val raid = raids.start(party)
                    raid.roomId = session.id
                    raid.room = TobRoom.Xarpus
                    telejump(opened.enter, TeleportType.Exempt)
                    manager.finalizeEntry(player, session, clock.cycle)
                    mes("Entered Xarpus encounter.")
                }
            }
        }
    }

    private fun Cheat.enterVerzik() {
        cleanPrevious(player)
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = verzik.open(player, party.settings.hardMode)) {
                is VerzikEncounter.Opened.Failed -> mes(opened.reason)
                is VerzikEncounter.Opened.Room -> {
                    val session = opened.fight.session
                    lobby.bindRaid(session.id, party)
                    val raid = raids.start(party)
                    raid.roomId = session.id
                    raid.room = TobRoom.Verzik
                    telejump(opened.enter, TeleportType.Exempt)
                    manager.finalizeEntry(player, session, clock.cycle)
                    mes("Entered Verzik encounter.")
                }
            }
        }
    }

    private fun Cheat.enterReward() {
        cleanPrevious(player)
        val party = getOrCreateParty() ?: return
        launcher.launch(player, busyText = "Please close your dialogue first.") {
            when (val opened = reward.open(player, party.settings.hardMode)) {
                is RewardEncounter.Opened.Failed -> mes(opened.reason)
                is RewardEncounter.Opened.Room -> {
                    lobby.bindRaid(opened.session.id, party)
                    val raid = raids.start(party)
                    raid.roomId = opened.session.id
                    raid.room = TobRoom.Reward
                    telejump(opened.enter, TeleportType.Exempt)
                    manager.finalizeEntry(player, opened.session, clock.cycle)
                    mes("Entered Reward Vault.")
                }
            }
        }
    }

    companion object {
        val LOBBY_SPAWN = CoordGrid(3668, 3219, 0)
    }
}
