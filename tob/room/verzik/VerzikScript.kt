package org.rsmod.content.raids.tob.room.verzik

import jakarta.inject.Inject
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
    }
}
