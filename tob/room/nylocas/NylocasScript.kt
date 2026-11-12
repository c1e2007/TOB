package org.rsmod.content.raids.tob.room.nylocas

import jakarta.inject.Inject
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
    }
}
