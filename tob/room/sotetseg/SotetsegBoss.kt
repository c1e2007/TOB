package org.rsmod.content.raids.tob.room.sotetseg

import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import kotlin.random.Random

internal class SotetsegBoss(
    val origin: CoordGrid,
    val deps: BossDeps,
    val hardMode: Boolean = false,
) {
    lateinit var npc: Npc
        private set

    var isDead = false
        private set

    private var ticks = 0
    private var attackCounter = 0
    private var isMazeActive = false
    private var mazeStep = 0
    private var mazePhase66Done = false
    private var mazePhase33Done = false

    val safeMazeTiles = HashSet<CoordGrid>()

    fun spawn(): Npc {
        val spawnTile = origin.translate(
            SotetsegArena.STATIC_BOSS_SPAWN.x - SotetsegArena.BASE_X,
            SotetsegArena.STATIC_BOSS_SPAWN.z - SotetsegArena.BASE_Z,
        )
        val sotetseg = Npc(NPC_SOTETSEG, spawnTile)
        sotetseg.respawnDir = Direction.South
        deps.npcRepo.add(sotetseg, Int.MAX_VALUE)
        npc = sotetseg
        return sotetseg
    }

    fun tick(players: List<Player>) {
        if (isDead || !::npc.isInitialized || npc.hitpoints <= 0) return

        ticks++

        if (isMazeActive) {
            handleMaze(players)
            return
        }

        checkMazeTriggers()

        if (ticks % 4 == 0 && players.isNotEmpty()) {
            attackCounter++
            if (attackCounter % 10 == 0) {
                sharedBallAttack(players)
            } else {
                standardAttack(players)
            }
        }
    }

    private fun checkMazeTriggers() {
        val maxHp = npc.baseHitpointsLvl
        val pct = (npc.hitpoints * 100) / maxHp

        if (!mazePhase66Done && pct <= 66) {
            startMaze(phase = 1)
        } else if (!mazePhase33Done && pct <= 33) {
            startMaze(phase = 2)
        }
    }

    private fun startMaze(phase: Int) {
        if (phase == 1) mazePhase66Done = true else mazePhase33Done = true
        isMazeActive = true
        mazeStep = 0
        safeMazeTiles.clear()

        // Generate safe path from north (near boss) to south (near entrance)
        var currX = origin.x + 13
        for (z in 36 downTo 22) {
            safeMazeTiles.add(CoordGrid(currX, origin.z + z, origin.level))
            currX += Random.nextInt(-1, 2).coerceIn(-3, 3)
        }
    }

    private fun handleMaze(players: List<Player>) {
        mazeStep++

        for (player in players) {
            val dz = player.coords.z - origin.z
            if (dz in 22..36) {
                // In maze zone
                if (player.coords !in safeMazeTiles) {
                    val damage = Random.nextInt(15, 26)
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

        // End maze phase after ~20 ticks or when a player reaches the top
        val reachedTop = players.any { it.coords.z >= origin.z + 36 }
        if (reachedTop || mazeStep >= 30) {
            isMazeActive = false
            safeMazeTiles.clear()
        }
    }

    private fun standardAttack(players: List<Player>) {
        val target = players.randomOrNull() ?: return
        val isMagic = Random.nextBoolean()
        npc.anim(if (isMagic) ANIM_MAGIC else ANIM_RANGED)

        val damage = Random.nextInt(5, if (hardMode) 55 else 40)
        target.queueHit(
            source = npc,
            delay = 2,
            type = if (isMagic) HitType.Magic else HitType.Ranged,
            damage = damage,
            modifier = deps.playerHitModifier,
        )
    }

    private fun sharedBallAttack(players: List<Player>) {
        val target = players.randomOrNull() ?: return
        npc.anim(ANIM_BALL)

        // Count all players standing on or within 1 tile of target
        val stacked = players.filter { it.coords.chebyshevDistance(target.coords) <= 1 }
        val totalDamage = if (hardMode) 130 else 100
        val splitDamage = totalDamage / stacked.size.coerceAtLeast(1)

        for (p in stacked) {
            p.queueHit(
                source = npc,
                delay = 3,
                type = HitType.Magic,
                damage = splitDamage,
                modifier = deps.playerHitModifier,
            )
            p.spotanim(SPOT_BALL_IMPACT)
        }
    }

    fun cleanup() {
        safeMazeTiles.clear()
        if (::npc.isInitialized) {
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    companion object {
        const val NPC_SOTETSEG = "npc.sotetseg"

        const val ANIM_MELEE = "seq.sotetseg_melee"
        const val ANIM_MAGIC = "seq.sotetseg_magic"
        const val ANIM_RANGED = "seq.sotetseg_ranged"
        const val ANIM_BALL = "seq.sotetseg_ball"

        const val SPOT_BALL_IMPACT = "spotanim.sotetseg_ball_impact"
    }
}
