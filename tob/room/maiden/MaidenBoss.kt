package org.rsmod.content.raids.tob.room.maiden

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

internal class MaidenBoss(
    val origin: CoordGrid,
    val deps: BossDeps,
    val hardMode: Boolean = false,
) {
    lateinit var npc: Npc
        private set

    val activeSplats = HashSet<CoordGrid>()
    val bloodTrails = HashMap<CoordGrid, Int>() // coord -> remaining ticks
    val bloodSpawns = ArrayList<Npc>()
    val nylos = ArrayList<Npc>()

    private var ticks = 0
    private var phase = 1 // 1: 100-70%, 2: 70-50%, 3: 50-30%, 4: 30-0%, 5: Dying, 6: Dead
    private var maxHit = 35.0
    private var maxBloodSplatHit = 10.0
    private var attackInterval = 10
    private var canRollBloodAttack = true
    private var hasUsedStormAttack = false

    var isDead = false
        private set

    fun spawn(): Npc {
        val spawnTile = origin.translate(MaidenArena.BOSS_SPAWN_DX, MaidenArena.BOSS_SPAWN_DZ)
        val maiden = Npc(NPC_MAIDEN_P1, spawnTile)
        maiden.respawnDir = Direction.East
        maiden.moveRestrict = MoveRestrict.NoMove
        deps.npcRepo.add(maiden, Int.MAX_VALUE)
        npc = maiden
        return maiden
    }

    fun tick(players: List<Player>) {
        if (isDead || !::npc.isInitialized || npc.hitpoints <= 0) return

        ticks++

        handleBloodSplats(players)
        handleNylos()
        handleBloodSpawns()

        if (ticks % attackInterval == 0 && players.isNotEmpty()) {
            if (canRollBloodAttack && Random.nextInt(4) == 0) {
                bloodAttack(players)
                canRollBloodAttack = false
                hasUsedStormAttack = false
            } else {
                stormAttack(players)
            }
        }

        checkPhaseTransitions()
    }

    private fun handleBloodSplats(players: List<Player>) {
        if (activeSplats.isEmpty() && bloodTrails.isEmpty()) return

        // Process blood trails expiration
        val trailIter = bloodTrails.iterator()
        while (trailIter.hasNext()) {
            val entry = trailIter.next()
            val remaining = entry.value - 1
            if (remaining <= 0) {
                activeSplats.remove(entry.key)
                trailIter.remove()
            } else {
                entry.setValue(remaining)
            }
        }

        // Damage players standing on splats
        for (player in players) {
            if (player.coords in activeSplats) {
                val dmg = Random.nextInt(5, maxBloodSplatHit.toInt().coerceAtLeast(6))
                player.queueHit(
                    source = npc,
                    delay = 0,
                    type = HitType.Typeless,
                    damage = dmg,
                    modifier = NoopPlayerHitModifier,
                )
                val prayerDrain = dmg / 2
                if (prayerDrain > 0) {
                    player.statSub("stat.prayer", prayerDrain, percent = 0)
                }
                heal(dmg)
            }
        }
    }

    private fun handleNylos() {
        val maidenCenter = npc.coords.translate(2, 2)
        val nyloIter = nylos.iterator()
        while (nyloIter.hasNext()) {
            val nylo = nyloIter.next()
            if (nylo.hitpoints <= 0) {
                nyloIter.remove()
                continue
            }
            val dist = nylo.coords.chebyshevDistance(maidenCenter)
            if (dist <= 6) {
                absorbNylocas(nylo)
                nyloIter.remove()
            } else {
                // Step towards Maiden
                val dx = (maidenCenter.x - nylo.coords.x).coerceIn(-1, 1)
                val dz = (maidenCenter.z - nylo.coords.z).coerceIn(-1, 1)
                if (dx != 0 || dz != 0) {
                    nylo.coords = nylo.coords.translate(dx, dz)
                }
            }
        }
    }

    private fun handleBloodSpawns() {
        val spawnIter = bloodSpawns.iterator()
        while (spawnIter.hasNext()) {
            val spawn = spawnIter.next()
            if (spawn.hitpoints <= 0) {
                spawnIter.remove()
                continue
            }
            // Drop a blood trail at current tile
            dropBloodTrail(spawn.coords)
            // Wander randomly
            val dx = Random.nextInt(-1, 2)
            val dz = Random.nextInt(-1, 2)
            if (dx != 0 || dz != 0) {
                val next = spawn.coords.translate(dx, dz)
                if (MaidenArena.inCombatArea(origin, next)) {
                    spawn.coords = next
                }
            }
        }
    }

    private fun stormAttack(players: List<Player>) {
        if (hasUsedStormAttack && !canRollBloodAttack) {
            canRollBloodAttack = true
            hasUsedStormAttack = false
        } else {
            hasUsedStormAttack = true
        }

        val target = getNearestPlayer(players) ?: return
        npc.anim(ANIM_STORM)

        val damage = Random.nextInt(1, maxHit.toInt().coerceAtLeast(2))
        target.queueHit(
            source = npc,
            delay = 2,
            type = HitType.Magic,
            damage = damage,
            modifier = deps.playerHitModifier,
        )
    }

    private fun bloodAttack(players: List<Player>) {
        npc.anim(ANIM_BLOOD)
        for (player in players) {
            dropBloodSplat(player.coords, duration = 10)
        }
        val randomPlayer = players.randomOrNull() ?: return
        val extraSplat = randomPlayer.coords.translate(Random.nextInt(-2, 3), Random.nextInt(-2, 3))
        if (MaidenArena.inCombatArea(origin, extraSplat)) {
            dropBloodSplat(extraSplat, duration = 10)
            if (bloodSpawns.size < MAX_BLOOD_SPAWNS && Random.nextInt(5) == 0) {
                spawnBloodSpawn(extraSplat)
            }
        }
    }

    private fun dropBloodSplat(tile: CoordGrid, duration: Int) {
        activeSplats.add(tile)
        bloodTrails[tile] = duration
    }

    private fun dropBloodTrail(tile: CoordGrid) {
        val duration = if (hardMode) 45 else 30
        activeSplats.add(tile)
        bloodTrails[tile] = duration
    }

    private fun spawnBloodSpawn(tile: CoordGrid) {
        val spawn = Npc(NPC_BLOOD_SPAWN, tile)
        deps.npcRepo.add(spawn, Int.MAX_VALUE)
        bloodSpawns.add(spawn)
    }

    private fun absorbNylocas(nylo: Npc) {
        maxHit += 3.5
        maxBloodSplatHit += 1.8
        heal(min(200, (nylo.hitpoints * 2)))
        if (hardMode) {
            attackInterval = max(4, attackInterval - 1)
        }
        deps.npcRepo.del(nylo, Int.MAX_VALUE)
    }

    private fun heal(amount: Int) {
        val maxHp = npc.baseHitpointsLvl
        npc.hitpoints = min(maxHp, npc.hitpoints + amount)
    }

    private fun checkPhaseTransitions() {
        val maxHp = npc.baseHitpointsLvl
        val currentHp = npc.hitpoints
        val pct = (currentHp * 100) / maxHp

        if (phase == 1 && pct <= 70) {
            phase = 2
            transmogTo(NPC_MAIDEN_P2)
            spawnNyloWave()
        } else if (phase == 2 && pct <= 50) {
            phase = 3
            transmogTo(NPC_MAIDEN_P3)
            spawnNyloWave()
        } else if (phase == 3 && pct <= 30) {
            phase = 4
            transmogTo(NPC_MAIDEN_P4)
            spawnNyloWave()
        } else if (currentHp <= 0 && !isDead) {
            sendDeath()
        }
    }

    private fun transmogTo(name: String) {
        val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
    }

    private fun spawnNyloWave() {
        val spawns = MaidenArena.NYLO_SPAWNS_NORTH + MaidenArena.NYLO_SPAWNS_SOUTH
        for (staticTile in spawns) {
            val relativeTile = origin.translate(staticTile.x - MaidenArena.BASE_X, staticTile.z - MaidenArena.BASE_Z)
            val nylo = Npc(NPC_NYLOCAS_MATOMENOS, relativeTile)
            deps.npcRepo.add(nylo, Int.MAX_VALUE)
            nylos.add(nylo)
        }
    }

    private fun getNearestPlayer(players: List<Player>): Player? {
        var shortest = Int.MAX_VALUE
        var nearest: Player? = null
        val directions = MaidenArena.STORM_DIRECTIONS.map {
            origin.translate(it.x - MaidenArena.BASE_X, it.z - MaidenArena.BASE_Z)
        }
        for (dir in directions) {
            for (p in players) {
                val dist = dir.chebyshevDistance(p.coords)
                if (dist < shortest) {
                    shortest = dist
                    nearest = p
                }
            }
        }
        return nearest ?: players.firstOrNull()
    }

    private fun sendDeath() {
        isDead = true
        transmogTo(NPC_MAIDEN_DYING)
        npc.anim(ANIM_DEATH_P1)

        activeSplats.clear()
        bloodTrails.clear()
        for (spawn in bloodSpawns) deps.npcRepo.del(spawn, Int.MAX_VALUE)
        bloodSpawns.clear()
        for (nylo in nylos) deps.npcRepo.del(nylo, Int.MAX_VALUE)
        nylos.clear()
    }

    fun cleanup() {
        activeSplats.clear()
        bloodTrails.clear()
        for (spawn in bloodSpawns) deps.npcRepo.del(spawn, Int.MAX_VALUE)
        bloodSpawns.clear()
        for (nylo in nylos) deps.npcRepo.del(nylo, Int.MAX_VALUE)
        nylos.clear()
        if (::npc.isInitialized) {
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    companion object {
        const val NPC_MAIDEN_P1 = "npc.tob_maiden_100"
        const val NPC_MAIDEN_P2 = "npc.tob_maiden_70"
        const val NPC_MAIDEN_P3 = "npc.tob_maiden_50"
        const val NPC_MAIDEN_P4 = "npc.tob_maiden_30"
        const val NPC_MAIDEN_DYING = "npc.tob_maiden_dying_a"
        const val NPC_MAIDEN_DEAD = "npc.tob_maiden_dying_b"
        const val NPC_NYLOCAS_MATOMENOS = "npc.maiden_elemental"
        const val NPC_BLOOD_SPAWN = "npc.maiden_blood_slug"

        const val ANIM_STORM = "seq.maiden_attack_special"
        const val ANIM_BLOOD = "seq.maiden_attack_blood"
        const val ANIM_DEATH_P1 = "seq.maiden_death_a"
        const val ANIM_DEATH_P2 = "seq.maiden_death_b"

        const val MAX_BLOOD_SPAWNS = 8
    }
}
