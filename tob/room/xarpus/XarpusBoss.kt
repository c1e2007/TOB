package org.rsmod.content.raids.tob.room.xarpus

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

internal class XarpusBoss(
    val origin: CoordGrid,
    val deps: BossDeps,
    val hardMode: Boolean = false,
) {
    lateinit var npc: Npc
        private set

    var isDead = false
        private set

    val acidSplats = HashMap<CoordGrid, Int>() // tile -> remaining ticks

    private var ticks = 0
    private var phase = 1 // 1: Exhumed / Healing, 2: Poison Spitting, 3: Screech / Gaze
    private var facingDir = Direction.South

    fun spawn(): Npc {
        val spawnTile = origin.translate(
            XarpusArena.STATIC_BOSS_SPAWN.x - XarpusArena.BASE_X,
            XarpusArena.STATIC_BOSS_SPAWN.z - XarpusArena.BASE_Z,
        )
        val xarpus = Npc(NPC_EXHUMED, spawnTile)
        xarpus.respawnDir = Direction.South
        deps.npcRepo.add(xarpus, Int.MAX_VALUE)
        npc = xarpus
        return xarpus
    }

    fun tick(players: List<Player>) {
        if (isDead || !::npc.isInitialized || npc.hitpoints <= 0) return

        ticks++

        handleAcidSplats(players)
        handleUnderBoss(players)
        checkPhaseTransitions()

        if (phase == 2) {
            // Poison spitting phase
            if (ticks % 4 == 0 && players.isNotEmpty()) {
                spitPoison(players)
            }
        } else if (phase == 3) {
            // Screech / Gaze phase
            if (ticks % 8 == 0) {
                rotateGaze()
            }
            if (ticks % 4 == 0 && players.isNotEmpty()) {
                spitPoison(players)
            }
        } else {
            // Exhumed phase lasts ~20 ticks then transitions to poison
            if (ticks >= 20) {
                transitionToPoison()
            }
        }
    }

    private fun handleAcidSplats(players: List<Player>) {
        val iter = acidSplats.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val remaining = entry.value - 1
            if (remaining <= 0) {
                iter.remove()
            } else {
                entry.setValue(remaining)
            }
        }

        for (player in players) {
            if (player.coords in acidSplats) {
                val damage = Random.nextInt(6, 13)
                player.queueHit(
                    source = npc,
                    delay = 0,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = NoopPlayerHitModifier,
                )
            }
        }
    }

    private fun handleUnderBoss(players: List<Player>) {
        val center = npc.coords.translate(2, 2)
        for (player in players) {
            if (player.coords.chebyshevDistance(center) <= 2) {
                // Standing under or right at boss
                val damage = Random.nextInt(4, 9)
                player.queueHit(
                    source = npc,
                    delay = 0,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = NoopPlayerHitModifier,
                )
            }
        }
    }

    private fun spitPoison(players: List<Player>) {
        npc.anim(ANIM_SPIT)
        val target = players.randomOrNull() ?: return
        acidSplats[target.coords] = 25
        val extraTile = target.coords.translate(Random.nextInt(-1, 2), Random.nextInt(-1, 2))
        if (XarpusArena.inCombatArea(origin, extraTile)) {
            acidSplats[extraTile] = 25
        }
    }

    private fun rotateGaze() {
        val dirs = listOf(
            Direction.North to (0 to 5),
            Direction.East to (5 to 0),
            Direction.South to (0 to -5),
            Direction.West to (-5 to 0),
        )
        val selected = dirs.random()
        facingDir = selected.first
        npc.faceSquare(npc.coords.translate(selected.second.first, selected.second.second))
    }

    private fun checkPhaseTransitions() {
        val maxHp = npc.baseHitpointsLvl
        val pct = (npc.hitpoints * 100) / maxHp

        if (phase < 3 && pct <= 25) {
            phase = 3
            transmogTo(NPC_SCREECH)
            npc.anim(ANIM_SCREECH)
        }
    }

    private fun transitionToPoison() {
        phase = 2
        transmogTo(NPC_POISON)
    }

    private fun transmogTo(name: String) {
        val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
    }

    fun onIncomingHit(source: Player, damage: Int): Int {
        if (phase == 3) {
            // Check if player is attacking from the direction Xarpus is staring
            val dx = source.coords.x - npc.coords.x
            val dz = source.coords.z - npc.coords.z
            val playerDir = when {
                dz > 4 -> Direction.North
                dz < -1 -> Direction.South
                dx > 4 -> Direction.East
                else -> Direction.West
            }
            if (playerDir == facingDir) {
                // Attacked while staring! Heal Xarpus and reflect damage
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
        }
        return damage
    }

    fun cleanup() {
        acidSplats.clear()
        if (::npc.isInitialized) {
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    companion object {
        const val NPC_EXHUMED = "npc.xarpus"
        const val NPC_POISON = "npc.xarpus_poison"
        const val NPC_SCREECH = "npc.xarpus_screech"

        const val ANIM_SPIT = "seq.xarpus_spit"
        const val ANIM_SCREECH = "seq.xarpus_screech"
    }
}
