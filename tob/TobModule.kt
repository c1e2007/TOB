package org.rsmod.content.raids.tob

import org.rsmod.content.raids.tob.party.TobLobby
import org.rsmod.content.raids.tob.party.TobParties
import org.rsmod.content.raids.tob.raid.TobRaids
import org.rsmod.content.raids.tob.room.bloat.BloatEncounter
import org.rsmod.content.raids.tob.room.maiden.MaidenEncounter
import org.rsmod.content.raids.tob.room.nylocas.NylocasEncounter
import org.rsmod.content.raids.tob.room.reward.RewardEncounter
import org.rsmod.content.raids.tob.room.shadowrealm.ShadowRealmEncounter
import org.rsmod.content.raids.tob.room.sotetseg.SotetsegEncounter
import org.rsmod.content.raids.tob.room.verzik.VerzikEncounter
import org.rsmod.content.raids.tob.room.xarpus.XarpusEncounter
import org.rsmod.plugin.module.PluginModule

class TobModule : PluginModule() {
    override fun bind() {
        bind(TobParties::class.java)
        bind(TobLobby::class.java)
        bind(TobRaids::class.java)
        bind(MaidenEncounter::class.java)
        bind(BloatEncounter::class.java)
        bind(NylocasEncounter::class.java)
        bind(SotetsegEncounter::class.java)
        bind(ShadowRealmEncounter::class.java)
        bind(XarpusEncounter::class.java)
        bind(VerzikEncounter::class.java)
        bind(RewardEncounter::class.java)
    }
}
