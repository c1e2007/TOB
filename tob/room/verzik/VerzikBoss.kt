package org.rsmod.content.raids.tob.room.verzik

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

internal class VerzikBoss(
    val origin: CoordGrid,
    val deps: BossDeps,
    val hardMode: Boolean = false,
) {
    lateinit var npc: Npc
        private set

    var isDead = false
        private set

    var phase = 1 // 1: Throne, 2: Bouncing Spider, 3: Spider Queen
        private set

    private var ticks = 0
    private var attackCycle = 0
    private var greenBallTarget: Player? = null
    private var greenBallBounces = 0

    val yellowPools = HashSet<CoordGrid>()
    val tornadoes = ArrayList<Npc>()

    fun spawn(): Npc {
        val spawnTile = origin.translate(
            VerzikArena.STATIC_BOSS_SPAWN.x - VerzikArena.BASE_X,
            VerzikArena.STATIC_BOSS_SPAWN.z - VerzikArena.BASE_Z,
        )
        val verzik = Npc(NPC_P1, spawnTile)
        verzik.respawnDir = Direction.South
        deps.npcRepo.add(verzik, Int.MAX_VALUE)
        npc = verzik
        return verzik
    }

    fun tick(players: List<Player>) {
        if (isDead || !::npc.isInitialized || npc.hitpoints <= 0) return

        ticks++

        checkPhaseTransitions()

        when (phase) {
            1 -> tickPhase1(players)
            2 -> tickPhase2(players)
            3 -> tickPhase3(players)
        }
    }

    private fun tickPhase1(players: List<Player>) {
        if (ticks % 6 == 0 && players.isNotEmpty()) {
            // Electric / bomb attack
            val target = players.randomOrNull() ?: return
            npc.anim(ANIM_P1_ATTACK)
            val damage = Random.nextInt(20, if (hardMode) 75 else 50)
            target.queueHit(
                source = npc,
                delay = 2,
                type = HitType.Magic,
                damage = damage,
                modifier = deps.playerHitModifier,
            )
        }
    }

    private fun tickPhase2(players: List<Player>) {
        if (ticks % 4 == 0 && players.isNotEmpty()) {
            attackCycle++
            val target = players.randomOrNull() ?: return

            if (attackCycle % 5 == 0) {
                // Bounce attack
                val dist = target.coords.chebyshevDistance(npc.coords)
                if (dist <= 2) {
                    val damage = Random.nextInt(35, 65)
                    target.queueHit(
                        source = npc,
                        delay = 0,
                        type = HitType.Typeless,
                        damage = damage,
                        modifier = NoopPlayerHitModifier,
                    )
                }
            } else {
                // Cabbages / ranged attack
                npc.anim(ANIM_P2_ATTACK)
                val damage = Random.nextInt(15, 35)
                target.queueHit(
                    source = npc,
                    delay = 2,
                    type = HitType.Ranged,
                    damage = damage,
                    modifier = deps.playerHitModifier,
                )
            }
        }
    }

    private fun tickPhase3(players: List<Player>) {
        // Enraged if < 20%
        val maxHp = npc.baseHitpointsLvl
        val pct = (npc.hitpoints * 100) / maxHp
        val attackInterval = if (pct <= 20) 4 else 5

        if (ticks % attackInterval == 0 && players.isNotEmpty()) {
            attackCycle++
            val target = players.randomOrNull() ?: return

            if (attackCycle % 7 == 0) {
                // Green ball attack (shared / bounced)
                npc.anim(ANIM_P3_GREEN_BALL)
                target.queueHit(
                    source = npc,
                    delay = 3,
                    type = HitType.Magic,
                    damage = Random.nextInt(25, 45),
                    modifier = deps.playerHitModifier,
                )
            } else if (attackCycle % 10 == 0) {
                // Yellow pools mechanic
                spawnYellowPools(players)
            } else {
                // Standard melee / magic / ranged
                val style = Random.nextInt(3)
                when (style) {
                    0 -> {
                        npc.anim(ANIM_P3_MELEE)
                        target.queueHit(
                            source = npc,
                            delay = 0,
                            type = HitType.Melee,
                            damage = Random.nextInt(15, 40),
                            modifier = deps.playerHitModifier,
                        )
                    }
                    1 -> {
                        npc.anim(ANIM_P3_MAGIC)
                        target.queueHit(
                            source = npc,
                            delay = 2,
                            type = HitType.Magic,
                            damage = Random.nextInt(15, 38),
                            modifier = deps.playerHitModifier,
                        )
                    }
                    else -> {
                        npc.anim(ANIM_P3_RANGED)
                        target.queueHit(
                            source = npc,
                            delay = 2,
                            type = HitType.Ranged,
                            damage = Random.nextInt(15, 38),
                            modifier = deps.playerHitModifier,
                        )
                    }
                }
            }
        }
    }

    private fun spawnYellowPools(players: List<Player>) {
        yellowPools.clear()
        for (player in players) {
            val tile = player.coords.translate(Random.nextInt(-2, 3), Random.nextInt(-2, 3))
            if (VerzikArena.inCombatArea(origin, tile)) {
                yellowPools.add(tile)
            }
        }
    }

    private fun checkPhaseTransitions() {
        val maxHp = npc.baseHitpointsLvl
        val pct = (npc.hitpoints * 100) / maxHp

        if (phase == 1 && pct <= 0) {
            phase = 2
            transmogTo(NPC_P2)
            npc.hitpoints = maxHp
        } else if (phase == 2 && pct <= 0) {
            phase = 3
            transmogTo(NPC_P3)
            npc.hitpoints = maxHp
        } else if (phase == 3 && pct <= 0 && !isDead) {
            isDead = true
            npc.anim(ANIM_DEATH)
            cleanup()
        }
    }

    private fun transmogTo(name: String) {
        val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
    }

    fun cleanup() {
        yellowPools.clear()
        for (tornado in tornadoes) deps.npcRepo.del(tornado, Int.MAX_VALUE)
        tornadoes.clear()
        if (::npc.isInitialized) {
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    companion object {
        const val NPC_P1 = "npc.verzik_vitur_p1"
        const val NPC_P2 = "npc.verzik_vitur_p2"
        const val NPC_P3 = "npc.verzik_vitur_p3"

        const val ANIM_P1_ATTACK = "seq.verzik_p1_attack"
        const val ANIM_P2_ATTACK = "seq.verzik_p2_attack"
        const val ANIM_P3_MELEE = "seq.verzik_p3_melee"
        const val ANIM_P3_MAGIC = "seq.verzik_p3_magic"
        const val ANIM_P3_RANGED = "seq.verzik_p3_ranged"
        const val ANIM_P3_GREEN_BALL = "seq.verzik_p3_green_ball"
        const val ANIM_DEATH = "seq.verzik_death"
    }
}
