package org.rsmod.content.raids.tob.room.maiden

import jakarta.inject.Inject
import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MaidenScript
@Inject
constructor(
    private val encounter: MaidenEncounter,
    private val lobby: TobLobby,
) : PluginScript() {

    override fun ScriptContext.startup() {
    }
}
