package org.rsmod.content.raids.tob.room.bloat

import jakarta.inject.Inject
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BloatScript
@Inject
constructor(
    private val encounter: BloatEncounter,
    private val lobby: TobLobby,
) : PluginScript() {

    override fun ScriptContext.startup() {
    }
}
