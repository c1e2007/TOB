package org.rsmod.content.raids.tob.party

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.content.raids.party.RaidParty
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.game.entity.Player

@Singleton
class TobLobby @Inject constructor(private val parties: TobParties, private val raids: TobRaids) {
    private val moving = HashSet<RaidParty<TobPartySettings>>()

    fun create(leader: Player, now: Int): RaidParty<TobPartySettings>? {
        return parties.create(leader, TobPartySettings(), now)
    }

    fun <T> moving(party: RaidParty<TobPartySettings>, move: () -> T): T {
        moving += party
        try {
            return move()
        } finally {
            moving -= party
        }
    }

    fun endRaid(party: RaidParty<TobPartySettings>) {
        raids.end(party)
        parties.disband(party)
    }
}
