package org.rsmod.content.raids.tob.room.nylocas

import jakarta.inject.Inject
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.script.onEvent
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NylocasScript
@Inject
constructor(
    private val encounter: NylocasEncounter,
    private val lobby: TobLobby,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onEvent<InstanceEndedEvent>(instanceEventId(NylocasEncounter.KEY)) {
            encounter.close(instanceId)
            lobby.raidEnded(instanceId)
        }
    }
}
