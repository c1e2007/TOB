package org.rsmod.content.raids.tob

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.tob.room.bloat.BloatArena
import org.rsmod.content.raids.tob.room.bloat.BloatEncounter
import org.rsmod.content.raids.tob.room.maiden.MaidenArena
import org.rsmod.content.raids.tob.room.maiden.MaidenEncounter
import org.rsmod.content.raids.tob.room.nylocas.NylocasArena
import org.rsmod.content.raids.tob.room.nylocas.NylocasEncounter
import org.rsmod.content.raids.tob.room.sotetseg.SotetsegArena
import org.rsmod.content.raids.tob.room.sotetseg.SotetsegEncounter
import org.rsmod.content.raids.tob.room.verzik.VerzikArena
import org.rsmod.content.raids.tob.room.verzik.VerzikEncounter
import org.rsmod.content.raids.tob.room.xarpus.XarpusArena
import org.rsmod.content.raids.tob.room.xarpus.XarpusEncounter
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TobBarrierScript
@Inject
constructor(
    private val maiden: MaidenEncounter,
    private val bloat: BloatEncounter,
    private val nylocas: NylocasEncounter,
    private val sotetseg: SotetsegEncounter,
    private val xarpus: XarpusEncounter,
    private val verzik: VerzikEncounter,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.tob_arena_barrier") {
            passBarrier()
        }
    }

    private suspend fun ProtectedAccess.passBarrier() {
        val coords = player.coords

        val maidenFight = maiden.at(coords)
        if (maidenFight != null && maidenFight.contains(coords)) {
            if (coords.x > maidenFight.origin.x + MaidenArena.BARRIER_DX) {
                player.coords = coords.translate(-2, 0)
                maidenFight.start()
            } else {
                player.coords = coords.translate(2, 0)
            }
            return
        }

        val bloatFight = bloat.at(coords)
        if (bloatFight != null && bloatFight.contains(coords)) {
            if (coords.x > bloatFight.origin.x + BloatArena.BARRIER_DX) {
                player.coords = coords.translate(-2, 0)
                bloatFight.start()
            } else {
                player.coords = coords.translate(2, 0)
            }
            return
        }

        val nyloFight = nylocas.at(coords)
        if (nyloFight != null && nyloFight.contains(coords)) {
            if (coords.z > nyloFight.origin.z + NylocasArena.BARRIER_DZ) {
                player.coords = coords.translate(0, -2)
                nyloFight.start()
            } else {
                player.coords = coords.translate(0, 2)
            }
            return
        }

        val sotetsegFight = sotetseg.at(coords)
        if (sotetsegFight != null && sotetsegFight.contains(coords)) {
            if (coords.z < sotetsegFight.origin.z + SotetsegArena.BARRIER_DZ) {
                player.coords = coords.translate(0, 2)
                sotetsegFight.start()
            } else {
                player.coords = coords.translate(0, -2)
            }
            return
        }

        val xarpusFight = xarpus.at(coords)
        if (xarpusFight != null && xarpusFight.contains(coords)) {
            if (coords.z > xarpusFight.origin.z + XarpusArena.BARRIER_DZ) {
                player.coords = coords.translate(0, -2)
                xarpusFight.start()
            } else {
                player.coords = coords.translate(0, 2)
            }
            return
        }

        val verzikFight = verzik.at(coords)
        if (verzikFight != null && verzikFight.contains(coords)) {
            if (coords.z < verzikFight.origin.z + VerzikArena.BARRIER_DZ) {
                player.coords = coords.translate(0, 2)
                verzikFight.start()
            } else {
                player.coords = coords.translate(0, -2)
            }
            return
        }
    }
}
