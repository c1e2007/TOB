package org.rsmod.content.raids.tob.raid

import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceSession
import org.rsmod.events.UnboundEvent

internal interface TobFight {
    val session: InstanceSession
    val elapsedTicks: Int?
    val finished: Boolean
}

data class TobRoomCleared(val instanceId: InstanceId, val room: TobRoom) : UnboundEvent
