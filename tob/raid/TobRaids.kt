package org.rsmod.content.raids.tob.raid

import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceId
import org.rsmod.content.raids.party.RaidParty
import org.rsmod.content.raids.tob.party.TobPartySettings
import org.rsmod.game.entity.Player

class TobRaid(val party: RaidParty<TobPartySettings>) {
    var room: TobRoom = TobRoom.Maiden
    var roomId: InstanceId? = null
    var deaths = 0
    val dead = HashSet<Player>()
    val cleared = HashSet<TobRoom>()
    var wiping = false

    fun isCleared(r: TobRoom): Boolean = r in cleared
    fun markCleared(r: TobRoom) { cleared += r }
}

@Singleton
class TobRaids {
    private val raids = HashMap<RaidParty<TobPartySettings>, TobRaid>()

    fun start(party: RaidParty<TobPartySettings>): TobRaid {
        raids.remove(party)?.let(::release)
        return TobRaid(party).also { raids[party] = it }
    }

    fun of(party: RaidParty<TobPartySettings>): TobRaid? = raids[party]

    fun of(player: Player): TobRaid? =
        raids.values.firstOrNull { player in it.party.members }

    fun isDead(player: Player): Boolean = of(player)?.dead?.contains(player) == true

    fun end(party: RaidParty<TobPartySettings>) {
        raids.remove(party)?.let(::release)
    }

    private fun release(raid: TobRaid) {
        raid.dead.clear()
        raid.cleared.clear()
    }
}
