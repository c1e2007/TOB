package org.rsmod.content.raids.tob.room.reward

import jakarta.inject.Inject
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RewardScript
@Inject
constructor(
    private val encounter: RewardEncounter,
    private val lobby: TobLobby,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onEvent<InstanceEndedEvent>(instanceEventId(RewardEncounter.KEY)) {
            encounter.close(instanceId)
            lobby.raidEnded(instanceId)
        }

        for (id in 33086..33090) {
            onProtectedEvent<LocEvents.Op1>(id) {
                openChest()
            }
        }
    }

    private fun ProtectedAccess.openChest() {
        encounter.claim(player)
    }
}
