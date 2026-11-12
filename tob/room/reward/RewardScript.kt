package org.rsmod.content.raids.tob.room.reward

import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RewardScript
@Inject
constructor(
    private val encounter: RewardEncounter,
) : PluginScript() {

    override fun ScriptContext.startup() {
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
