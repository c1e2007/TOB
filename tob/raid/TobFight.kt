package org.rsmod.content.raids.tob.raid

import org.rsmod.events.UnboundEvent

internal interface TobFight {
    val elapsedTicks: Int?
    val finished: Boolean
}

data class TobRoomCleared(val room: TobRoom) : UnboundEvent
