package org.rsmod.content.raids.tob.room.verzik

import jakarta.inject.Inject
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.script.onEvent
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VerzikScript
@Inject
constructor(
    private val encounter: VerzikEncounter,
    private val lobby: TobLobby,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onEvent<InstanceEndedEvent>(instanceEventId(VerzikEncounter.KEY)) {
            encounter.close(instanceId)
            lobby.raidEnded(instanceId)
        }
    }
}
