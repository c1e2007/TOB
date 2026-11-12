package org.rsmod.content.raids.tob.room.reward

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player
import kotlin.random.Random

internal object RewardChest {
    data class RewardItem(val name: String, val amount: Int, val isUnique: Boolean = false)

    private val UNIQUES = listOf(
        RewardItem("obj.scythe_of_vitur_uncharged", 1, true),
        RewardItem("obj.ghrazi_rapier", 1, true),
        RewardItem("obj.sanguinesti_staff_uncharged", 1, true),
        RewardItem("obj.justiciar_faceguard", 1, true),
        RewardItem("obj.justiciar_chestguard", 1, true),
        RewardItem("obj.justiciar_legguards", 1, true),
        RewardItem("obj.avernic_defender_hilt", 1, true),
    )

    private val REGULAR_LOOT = listOf(
        RewardItem("obj.death_rune", 500),
        RewardItem("obj.blood_rune", 500),
        RewardItem("obj.soul_rune", 500),
        RewardItem("obj.dragon_dart", 100),
        RewardItem("obj.battlestaff_noted", 15),
        RewardItem("obj.grimy_ranarr_weed_noted", 30),
        RewardItem("obj.grimy_snapdragon_noted", 25),
        RewardItem("obj.grimy_torstol_noted", 20),
        RewardItem("obj.coins", 100_000),
    )

    fun rollReward(hardMode: Boolean = false): List<RewardItem> {
        val uniqueChance = if (hardMode) 9 else 11 // ~1/9 or ~1/11 chance
        val rolledUnique = Random.nextInt(uniqueChance) == 0

        val rewards = ArrayList<RewardItem>()
        if (rolledUnique) {
            val unique = rollUnique()
            rewards.add(unique)
        }

        // Add 2-3 regular loot items
        val regularCount = Random.nextInt(2, 4)
        val shuffled = REGULAR_LOOT.shuffled()
        for (i in 0 until regularCount) {
            rewards.add(shuffled[i])
        }

        return rewards
    }

    private fun rollUnique(): RewardItem {
        val roll = Random.nextInt(19)
        return when (roll) {
            0 -> UNIQUES[0] // Scythe (1/19)
            1, 2 -> UNIQUES[1] // Rapier (2/19)
            3, 4 -> UNIQUES[2] // Sang staff (2/19)
            5, 6 -> UNIQUES[3] // Faceguard (2/19)
            7, 8 -> UNIQUES[4] // Chestguard (2/19)
            9, 10 -> UNIQUES[5] // Legguards (2/19)
            else -> UNIQUES[6] // Avernic hilt (8/19)
        }
    }

    fun awardRewards(player: Player, rewards: List<RewardItem>) {
        for (item in rewards) {
            val res = player.invAdd(player.inv, item.name, item.amount)
            if (item.isUnique) {
                player.mes("<col=ef1020>Special loot: ${item.amount}x ${item.name}!</col>")
            }
        }
    }
}
