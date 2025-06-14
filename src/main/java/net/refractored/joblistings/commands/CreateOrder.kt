package net.refractored.joblistings.commands

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.j256.ormlite.stmt.QueryBuilder
import kotlinx.coroutines.withContext
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.commands.annotations.ConfigRange
import net.refractored.joblistings.commands.autocomplete.MaterialSuggestion
import net.refractored.joblistings.database.Database.orderDao
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.messages.Messages
import net.refractored.joblistings.messages.Messages.replace
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.order.tables.PendingOrder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.SuggestWith
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*

class CreateOrder {
    @CommandPermission("joblistings.create.hand")
    @ConfigCommand("messages.create.hand")
    fun createOrderHand(
        actor: BukkitCommandActor,
        @ConfigRange(maxPath = "pending-orders.reward.maximum", minPath = "pending-orders.reward.minimum") reward: Double,
        @Optional @ConfigRange(maxPath = "pending-orders.max-items", minPath = "") amount: Int = 1,
        @Optional @ConfigRange(
            maxPath = "pending-orders.expiration.maximum",
            minPath = "pending-orders.expiration.minimum",
        )
        hours: Long = JobListings.instance.config.getLong("orders.max-order-time")
    ) {
        val player = actor.requirePlayer()

        if (JobListings.instance.eco.getBalance(player) < reward) {
            actor.reply(
                Messages.getMessagePrefixed("messages.create.common.not-enough-money"),
            )
            return
        }

        val item =
            player.inventory.itemInMainHand
                .clone()

        if (item.type == Material.AIR) {
            actor.reply(
                Messages
                    .getMessagePrefixed(
                        "messages.create.hand.execution.not-holding-item",
                    ),
            )
            return
        }

        blacklistedMaterial(item.type.name)

        if (item.itemMeta is Damageable) {
            val damageableMeta = item.itemMeta as Damageable
            damageableMeta.resetDamage()
            item.itemMeta = damageableMeta
        }

        checkAmount(amount, item)

        item.amount = 1

        JobListings.instance.launch {
            createOrder(
                actor,
                item,
                reward,
                amount,
                hours,
            )
        }
    }

    @CommandPermission("joblistings.create.material")
    @ConfigCommand("messages.create.material")
    fun createOrderMaterial(
        actor: BukkitCommandActor,
        @SuggestWith(MaterialSuggestion::class) @Named("type") stackName: String,
        @ConfigRange(maxPath = "pending-orders.reward.maximum", minPath = "pending-orders.reward.minimum") reward: Double,
        @Optional @ConfigRange(maxPath = "pending-orders.max-items", minPath = "") amount: Int = 1,
        @Optional @ConfigRange(
            maxPath = "pending-orders.expiration.maximum",
            minPath = "pending-orders.expiration.minimum",
        ) hours: Long = JobListings.instance.config.getLong("pending-orders.expiration.maximum")
    ) {
        val player = actor.requirePlayer()

        if (JobListings.instance.eco.getBalance(player) < reward) {
            actor.reply(
                Messages.getMessagePrefixed("messages.create.common.not-enough-money"),
            )
            return
        }

        val item: ItemStack? =
            /* Presets.getPreset(stackName)
                ?:*/
            Material.getMaterial(stackName.uppercase())?.let { ItemStack(it) }

        if (item == null) {
            actor.reply(
                Messages.getMessagePrefixed("messages.create-material.execution.invalid-material"),
            )
            return
        }

        item.amount = 1

        if (!item.type.isItem) {
            actor.reply(
                Messages.getMessagePrefixed("messages.create-material.execution.not-item"),
            )
            return
        }

        if (blacklistedMaterial(item.type.name)) {
            actor.reply(
                Messages.getMessagePrefixed("messages.create-material.execution.blacklisted"),
            )
            return
        }

        checkAmount(amount, item)

        item.amount = 1

        JobListings.instance.launch {
            createOrder(
                actor,
                item,
                reward,
                amount,
                hours,
            )
        }
    }

    /**
     * Checks if the player has more than the max amount of orders, then creates the order if not.
     *
     */
    private suspend fun createOrder(
        actor: BukkitCommandActor,
        itemStack: ItemStack,
        cost: Double,
        amount: Int,
        hours: Long
    ) {
        withContext(JobListings.instance.asyncDispatcher) {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeCreated", false)

            val orders =
                queryBuilder
                    .where()
                    .eq("status", OrderStatus.PENDING)
                    .and()
                    .eq("user", actor.uniqueId())
                    .countOf()

            val maxOrders = PendingOrder.getMaxOrders(actor.requirePlayer())

            if (orders >= maxOrders) {
                actor.reply(
                    Messages.getMessagePrefixed("CreateOrder.MaxOrdersReached")
                        .replace("%1", maxOrders.toString()),
                )
                return@withContext
            }

            // vault api must be sync
            withContext(JobListings.instance.minecraftDispatcher) {
                JobListings.instance.eco.withdrawPlayer(actor.requirePlayer(), cost)
            }

            val order =
                PendingOrder.create(
                    actor.uniqueId(),
                    cost,
                    itemStack,
                    amount,
                    hours,
                )

            val orderInfo = order.getItemInfo()

            actor.requirePlayer().sendMessage(
                Messages.getMessage("CreateOrder.OrderCreated")
                    .replace("%1", orderInfo)
                    .replace("%2", cost.toString())
                    .replace("%3", hours.toString()),
            )
        }
    }

    private fun blacklistedMaterial(arg: String): Boolean {
        val blacklistedMaterials = JobListings.instance.config.getStringList("orders.BlacklistedMaterials")
        blacklistedMaterials.addAll(JobListings.instance.config.getStringList("orders.BlacklistedCreateMaterials"))
        return blacklistedMaterials.any { it.equals(arg, true) }
    }

    private fun checkAmount(
        amount: Int,
        item: ItemStack
    ) {
        val maxItems = JobListings.instance.config.getInt("orders.max-items")

        when {
            maxItems == -1 && amount > item.maxStackSize -> {
                throw CommandErrorException(
                    Messages
                        .getMessagePrefixed("messages.create-material.execution.invalid-amount")
                        .replace("%max%", item.maxStackSize.toString()),
                )
            }
            maxItems != 0 && amount >= maxItems -> {
                throw CommandErrorException(
                    Messages
                        .getMessagePrefixed("messages.create-material.execution.invalid-amount")
                        .replace("%max%", maxItems.toString()),
                )
            }
        }
    }
}
