package org.rsmod.content.raids.tob.room.nylocas

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import kotlin.random.Random

internal class NylocasBoss(
    val origin: CoordGrid,
    val deps: BossDeps,
    val hardMode: Boolean = false,
) {
    lateinit var npc: Npc
        private set

    var isDead = false
        private set

    enum class Form(
        val npcName: String,
        val attackAnim: String,
        val hitType: HitType,
        val deathAnim: String,
    ) {
        Melee(NPC_MELEE, ANIM_MELEE, HitType.Melee, ANIM_DEATH_MELEE),
        Magic(NPC_MAGIC, ANIM_MAGIC, HitType.Magic, ANIM_DEATH_MAGIC),
        Ranged(NPC_RANGED, ANIM_RANGED, HitType.Ranged, ANIM_DEATH_RANGED),
    }

    var currentForm = Form.Melee
        private set

    private var ticks = 0
    private var formTicks = 0
    private val formDuration = 11

    fun spawn(): Npc {
        val spawnTile = origin.translate(
            NylocasArena.STATIC_BOSS_SPAWN.x - NylocasArena.BASE_X,
            NylocasArena.STATIC_BOSS_SPAWN.z - NylocasArena.BASE_Z,
        )
        val vasilias = Npc(NPC_MELEE, spawnTile)
        vasilias.respawnDir = Direction.South
        deps.npcRepo.add(vasilias, Int.MAX_VALUE)
        npc = vasilias
        return vasilias
    }

    fun tick(players: List<Player>) {
        if (isDead || !::npc.isInitialized || npc.hitpoints <= 0) return

        ticks++
        formTicks++

        if (formTicks >= formDuration) {
            transform()
            formTicks = 0
        }

        if (ticks % 4 == 0 && players.isNotEmpty()) {
            attack(players)
        }
    }

    private fun transform() {
        val others = Form.entries.filter { it != currentForm }
        currentForm = others[Random.nextInt(others.size)]
        val type = ServerCacheManager.getNpc(currentForm.npcName.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
    }

    private fun attack(players: List<Player>) {
        val target = players.randomOrNull() ?: return
        npc.anim(currentForm.attackAnim)

        val maxHit = if (hardMode) 45 else 32
        val damage = Random.nextInt(1, maxHit + 1)
        val delay = if (currentForm == Form.Melee) 0 else 2

        target.queueHit(
            source = npc,
            delay = delay,
            type = currentForm.hitType,
            damage = damage,
            modifier = deps.playerHitModifier,
        )
    }

    fun onIncomingHit(source: Player, hitType: HitType, damage: Int): Int {
        if (hitType != currentForm.hitType) {
            // Wrong combat style! Heal Vasilias and reflect damage back to attacker
            val maxHp = npc.baseHitpointsLvl
            npc.hitpoints = (npc.hitpoints + damage).coerceAtMost(maxHp)

            source.queueHit(
                source = npc,
                delay = 0,
                type = HitType.Typeless,
                damage = damage,
                modifier = NoopPlayerHitModifier,
            )
            return 0
        }
        return damage
    }

    fun cleanup() {
        if (::npc.isInitialized) {
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    companion object {
        const val NPC_BASE = "npc.nylocas_vasilias"
        const val NPC_MELEE = "npc.nylocas_vasilias_melee"
        const val NPC_MAGIC = "npc.nylocas_vasilias_magic"
        const val NPC_RANGED = "npc.nylocas_vasilias_ranged"

        const val ANIM_SPAWN = "seq.nylocas_spawn"
        const val ANIM_MELEE = "seq.nylocas_vasilias_melee"
        const val ANIM_MAGIC = "seq.nylocas_vasilias_magic"
        const val ANIM_RANGED = "seq.nylocas_vasilias_ranged"

        const val ANIM_DEATH_MELEE = "seq.nylocas_vasilias_death_melee"
        const val ANIM_DEATH_MAGIC = "seq.nylocas_vasilias_death_magic"
        const val ANIM_DEATH_RANGED = "seq.nylocas_vasilias_death_ranged"
    }
}
