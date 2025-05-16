package net.refractored.joblistings.commands

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.j256.ormlite.stmt.QueryBuilder
import kotlinx.coroutines.withContext
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.commands.autocomplete.MaterialSuggesstion
import net.refractored.joblistings.database.Database.orderDao
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.order.tables.PendingOrder
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.miniToComponent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Range
import revxrsal.commands.annotation.SuggestWith
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*

class CreateOrder {
    @CommandPermission("joblistings.create.hand")
    @Description("messages.create.hand.description")
    @ConfigCommand("messages.create.hand.command")
    fun createOrderHand(
        actor: BukkitCommandActor,
        @Range(min = 1.0) cost: Double,
        @Optional @Range(min = 1.0) amount: Int = 1,
        @Optional @Range(min = 1.0) hours: Long = JobListings.instance.config.getLong("orders.max-order-time"),
    ) {
        val player = actor.requirePlayer()

        val minHours = JobListings.instance.config.getLong("orders.min-order-time")
        val maxHours = JobListings.instance.config.getLong("orders.max-order-time")

        if (hours < minHours || hours > maxHours) {
            throw CommandErrorException(
                Messages
                    .getStringPrefixed("messages.create.common.not-in-range")
                    .replace("%min%", "$minHours")
                    .replace("%max%", "$minHours")
                    .miniToComponent(),
            )
        }

        if (JobListings.instance.eco.getBalance(player) < cost) {
            throw CommandErrorException(
                Messages.getStringPrefixed("messages.create.common.not-enough-money").miniToComponent(),
            )
        }

        val item =
            player.inventory.itemInMainHand
                .clone()

        if (item.type == Material.AIR) {
            throw CommandErrorException(
                Messages
                    .getString(
                        "messages.create.hand.execution.not-holding-item",
                    ).miniToComponent(),
            )
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
                cost,
                amount,
                hours,
            )
        }
    }

    @CommandPermission("joblistings.create.material")
    @Description("messages.create.material.description")
    @ConfigCommand("messages.create.material.command")
    fun createOrderMaterial(
        actor: BukkitCommandActor,
        @SuggestWith(MaterialSuggesstion::class) @Named("type") stackName: String,
        @Range(min = 1.0) cost: Double,
        @Optional @Range(min = 1.0) amount: Int = 1,
        @Optional @Range(min = 1.0) hours: Long = JobListings.instance.config.getLong("orders.max-order-time"),
    ) {
        val player = actor.requirePlayer()

        val minHours = JobListings.instance.config.getLong("orders.min-order-time")
        val maxHours = JobListings.instance.config.getLong("orders.max-order-time")

        if (hours < minHours || hours > maxHours) {
            throw CommandErrorException(
                Messages
                    .getStringPrefixed("messages.create.common.not-in-range")
                    .replace("%min%", "$minHours")
                    .replace("%max%", "$minHours")
                    .miniToComponent(),
            )
        }

        if (JobListings.instance.eco.getBalance(player) < cost) {
            throw CommandErrorException(
                Messages.getStringPrefixed("messages.create.common.not-enough-money").miniToComponent(),
            )
        }

        val item: ItemStack =
            /* Presets.getPreset(stackName)
                ?:*/
            Material.getMaterial(stackName.uppercase())?.let { ItemStack(it) }
                ?: throw CommandErrorException(
                    Messages.getStringPrefixed("messages.create-material.execution.invalid-material").miniToComponent(),
                )

        item.amount = 1

        if (!item.type.isItem) {
            throw CommandErrorException(
                Messages.getStringPrefixed("messages.create-material.execution.not-item").miniToComponent(),
            )
        }

        if (blacklistedMaterial(item.type.name)) {
            throw CommandErrorException(
                Messages.getStringPrefixed("messages.create-material.execution.blacklisted").miniToComponent(),
            )
        }

        checkAmount(amount, item)

        item.amount = 1

        JobListings.instance.launch {
            createOrder(
                actor,
                item,
                cost,
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
        hours: Long,
    ) {
        withContext(JobListings.instance.asyncDispatcher) {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeCreated", false)
            queryBuilder
                .where()
                .eq("status", OrderStatus.PENDING)
                .and()
                .eq("user", actor.uniqueId())
            val orders = orderDao.countOf(queryBuilder.prepare())
            val maxOrders = PendingOrder.getMaxOrders(actor.requirePlayer())

            if (orders >= maxOrders) {
                throw CommandErrorException(
                    MessageUtil.getMessage(
                        "CreateOrder.MaxOrdersReached",
                        listOf(MessageReplacement("$maxOrders")),
                    ),
                )
            }

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
                MessageUtil.getMessage(
                    "CreateOrder.OrderCreated",
                    listOf(
                        MessageReplacement(orderInfo),
                        MessageReplacement(cost.toString()),
                        MessageReplacement(hours.toString()),
                    ),
                ),
            )
        }
    }

    private fun blacklistedMaterial(arg: String): Boolean {
        val blacklistedMaterials = JobListings.instance.config.getStringList("orders.BlacklistedMaterials")
        blacklistedMaterials.addAll(
            JobListings.instance.config.getStringList("orders.BlacklistedCreateMaterials"),
        )
        return blacklistedMaterials.any { it.equals(arg, true) }
    }

    private fun checkAmount(
        amount: Int,
        item: ItemStack,
    ) {
        val maxItems = JobListings.instance.config.getInt("orders.max-items")

        when {
            maxItems == -1 && amount > item.maxStackSize -> {
                throw CommandErrorException(
                    Messages
                        .getString("messages.create-material.execution.invalid-amount")
                        .replace("%max%", item.maxStackSize.toString())
                        .miniToComponent(),
                )
            }
            maxItems != 0 && amount >= maxItems -> {
                throw CommandErrorException(
                    Messages
                        .getString("messages.create-material.execution.invalid-amount")
                        .replace("%max%", maxItems.toString())
                        .miniToComponent(),
                )
            }
        }
    }
}
