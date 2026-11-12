package org.rsmod.content.raids.tob.party

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.content.raids.party.RaidPartyRegistry

class TobPartySettings(
    var hardMode: Boolean = false,
    var preferredSize: Int = DEFAULT_SIZE,
) {
    companion object {
        const val DEFAULT_SIZE = 5
        const val MAX_SIZE = 5
    }
}

@Singleton
class TobParties @Inject constructor() :
    RaidPartyRegistry<TobPartySettings>(MAX_LISTED, MAX_MEMBERS) {
    companion object {
        const val MAX_LISTED = 45
        const val MAX_MEMBERS = 5
    }
}
