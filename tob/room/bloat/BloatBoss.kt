package org.rsmod.content.raids.tob.room.bloat

import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import kotlin.random.Random

internal class BloatBoss(
    val origin: CoordGrid,
    val deps: BossDeps,
    val hardMode: Boolean = false,
) {
    lateinit var npc: Npc
        private set

    var isDead = false
        private set

    var isSleeping = false
        private set

    private var ticks = 0
    private var sleepTicks = 0
    private var walkCycleTicks = 0
    private var walkCycleLimit = 32
    private var stompCount = 0
    private var clockwise = true
    private var currentTargetCorner = 0 // 0: NE, 1: NW, 2: SW, 3: SE

    val meatShadows = HashMap<CoordGrid, Int>() // tile -> remaining ticks

    fun spawn(): Npc {
        val spawnTile = origin.translate(
            BloatArena.STATIC_BOSS_SPAWN.x - BloatArena.BASE_X,
            BloatArena.STATIC_BOSS_SPAWN.z - BloatArena.BASE_Z,
        )
        val bloat = Npc(NPC_BLOAT, spawnTile)
        bloat.respawnDir = Direction.North
        bloat.defaultMoveSpeed = MoveSpeed.Walk
        deps.npcRepo.add(bloat, Int.MAX_VALUE)
        npc = bloat
        return bloat
    }

    fun tick(players: List<Player>) {
        if (isDead || !::npc.isInitialized || npc.hitpoints <= 0) return

        ticks++

        handleFallingMeat(players)

        if (isSleeping) {
            handleSleep(players)
        } else {
            handlePatrol()
            handleFlies(players)

            walkCycleTicks++
            if (walkCycleTicks >= walkCycleLimit) {
                startSleep()
            } else if (walkCycleTicks % 15 == 0 && Random.nextInt(3) == 0) {
                // Reverse patrol direction
                clockwise = !clockwise
            }

            // Drop meat periodically while walking
            if (ticks % 4 == 0) {
                spawnMeatWave()
            }
        }
    }

    private fun handlePatrol() {
        val corners = getPatrolCorners()
        val target = corners[currentTargetCorner]
        val dist = npc.coords.chebyshevDistance(target)

        if (dist <= 1) {
            // Reached corner, advance to next
            currentTargetCorner = if (clockwise) {
                (currentTargetCorner + 1) % corners.size
            } else {
                (currentTargetCorner - 1 + corners.size) % corners.size
            }
        }

        val nextTarget = corners[currentTargetCorner]
        val dx = (nextTarget.x - npc.coords.x).coerceIn(-1, 1)
        val dz = (nextTarget.z - npc.coords.z).coerceIn(-1, 1)
        if (dx != 0 || dz != 0) {
            npc.coords = npc.coords.translate(dx, dz)
        }
    }

    private fun getPatrolCorners(): List<CoordGrid> {
        return listOf(
            origin.translate(11, 11), // NE
            origin.translate(0, 11),  // NW
            origin.translate(0, 0),   // SW
            origin.translate(11, 0),  // SE
        )
    }

    private fun handleFlies(players: List<Player>) {
        if (ticks % 3 != 0) return

        for (player in players) {
            if (hasLineOfSight(player.coords)) {
                // Bloat releases flies to this player
                val damage = Random.nextInt(10, 21)
                player.queueHit(
                    source = npc,
                    delay = 1,
                    type = HitType.Ranged,
                    damage = damage,
                    modifier = deps.playerHitModifier,
                )
                player.spotanim(SPOT_FLIES_IMPACT)

                // Bounce flies to adjacent players
                for (other in players) {
                    if (other !== player && other.coords.chebyshevDistance(player.coords) <= 1) {
                        val bounceDmg = Random.nextInt(10, 21)
                        other.queueHit(
                            source = npc,
                            delay = 2,
                            type = HitType.Ranged,
                            damage = bounceDmg,
                            modifier = deps.playerHitModifier,
                        )
                        other.spotanim(SPOT_FLIES_IMPACT)
                    }
                }
            }
        }
    }

    private fun hasLineOfSight(tile: CoordGrid): Boolean {
        // Line of sight is blocked if blocked by central pillar
        val bloatCenter = npc.coords.translate(2, 2)
        val bloatSideX = when {
            bloatCenter.x < origin.x + 5 -> -1
            bloatCenter.x > origin.x + 11 -> 1
            else -> 0
        }
        val targetSideX = when {
            tile.x < origin.x + 5 -> -1
            tile.x > origin.x + 11 -> 1
            else -> 0
        }

        val bloatSideZ = when {
            bloatCenter.z < origin.z + 5 -> -1
            bloatCenter.z > origin.z + 11 -> 1
            else -> 0
        }
        val targetSideZ = when {
            tile.z < origin.z + 5 -> -1
            tile.z > origin.z + 11 -> 1
            else -> 0
        }

        // Visible if sharing same quadrant corridor
        if (bloatSideX != 0 && bloatSideX == targetSideX) return true
        if (bloatSideZ != 0 && bloatSideZ == targetSideZ) return true
        return false
    }

    private fun startSleep() {
        isSleeping = true
        sleepTicks = 0
        npc.anim(ANIM_STOMP)
    }

    private fun handleSleep(players: List<Player>) {
        sleepTicks++

        if (sleepTicks == 30) {
            // Camera shake / warning
        } else if (sleepTicks == 31) {
            // Stomp attack to anyone in line of sight
            for (player in players) {
                if (hasLineOfSight(player.coords)) {
                    val damage = Random.nextInt(40, 81)
                    player.queueHit(
                        source = npc,
                        delay = 0,
                        type = HitType.Typeless,
                        damage = damage,
                        modifier = NoopPlayerHitModifier,
                    )
                }
            }
        } else if (sleepTicks >= 33) {
            // Wake up
            isSleeping = false
            walkCycleTicks = 0
            stompCount++
            if (stompCount >= 2) {
                npc.defaultMoveSpeed = MoveSpeed.Run
            }
            walkCycleLimit = Random.nextInt(25, 38)
        }
    }

    private fun spawnMeatWave() {
        val count = 16
        for (i in 0 until count) {
            val tile = BloatArena.randomMeatTile(origin)
            meatShadows[tile] = 3
        }
    }

    private fun handleFallingMeat(players: List<Player>) {
        val iter = meatShadows.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val remaining = entry.value - 1
            if (remaining <= 0) {
                val tile = entry.key
                iter.remove()
                // Impact! Check players standing on tile
                for (player in players) {
                    if (player.coords == tile) {
                        val damage = Random.nextInt(30, 51)
                        player.queueHit(
                            source = npc,
                            delay = 0,
                            type = HitType.Typeless,
                            damage = damage,
                            modifier = NoopPlayerHitModifier,
                        )
                        player.spotanim(SPOT_STUN)
                    }
                }
            } else {
                entry.setValue(remaining)
            }
        }
    }

    fun onBossHit(incomingDamage: Int): Int {
        // Bloat takes half damage while walking, full damage while sleeping
        return if (isSleeping) incomingDamage else incomingDamage / 2
    }

    fun cleanup() {
        meatShadows.clear()
        if (::npc.isInitialized) {
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    companion object {
        const val NPC_BLOAT = "npc.pestilent_bloat"

        const val ANIM_STOMP = "seq.bloat_stomp"
        const val SPOT_FLIES_IMPACT = "spotanim.pestilent_bloat_flies"
        const val SPOT_STUN = "spotanim.stun"
    }
}
