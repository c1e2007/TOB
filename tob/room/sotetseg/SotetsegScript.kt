package org.rsmod.content.raids.tob.room.sotetseg

import jakarta.inject.Inject
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SotetsegScript
@Inject
constructor(
    private val encounter: SotetsegEncounter,
    private val lobby: TobLobby,
) : PluginScript() {

    override fun ScriptContext.startup() {
    }
}
