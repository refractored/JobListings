package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import com.willfp.eco.core.items.Items
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings.Companion.eco
import net.refractored.joblistings.JobListings.Companion.ecoPlugin
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import java.time.LocalDateTime
import java.util.*

class CompleteOrders {
    @CommandPermission("joblistings.completeorders")
    @Description("Scans your inventory for items to complete an order")
        @Command("joblistings complete")
    fun completeOrders(actor: BukkitCommandActor) {
        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder.where().eq("assignee", actor.uniqueId).and().eq("status", OrderStatus.CLAIMED)
        val orders = orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }
        if (orders.isEmpty()) {
            throw CommandErrorException(MessageUtil.getMessage("Orders.NoOrdersToComplete"))
        }
        val orderCount = orders.count()
        var ordersUpdated = 0
        var ordersCompleted = 0
        for (order in orders) {
            for (item in actor.player.inventory.storageContents){
                item ?: continue
                if (!isMatchingItem(item, order)) continue
                if (order.item is Damageable && item.itemMeta is Damageable) {
                    if ((order.item as Damageable).damage != (item.itemMeta as Damageable).damage) {
                        actor.reply(
                            MessageUtil.getMessage(
                                "OrderComplete.DamagedItem",
                                listOf(
                                    MessageReplacement(order.getItemInfo())
                                )
                            )
                        )
                        continue
                    }
                }
                if (order.itemCompleted + item.amount >= order.itemAmount) {
                    // Order completed YIPPEE
                    val itemsLeft = (order.itemCompleted + item.amount) - order.itemAmount
                    order.itemCompleted = order.itemAmount
                    order.status = OrderStatus.COMPLETED
                    order.timeCompleted = LocalDateTime.now()
                    orderDao.update(order)
                    // If order completed send the reward :D
                    eco.depositPlayer(
                        Bukkit.getOfflinePlayer(actor.uniqueId),
                        order.cost
                    )
                    item.amount = itemsLeft
                    ordersCompleted++
                    messageCompletion(actor, order)
                    continue
                }
                // Order not completed :(
                order.itemCompleted += item.amount
                orderDao.update(order)
                item.amount = 0
                ordersUpdated++
                messageProgress(actor, order)

            }
        }
        if (ordersUpdated == 0) {
            throw CommandErrorException(
                MessageUtil.getMessage(
                    "OrderComplete.NoItemsFound",
                )
            )
        }
        if (ordersCompleted == orderCount) {
            actor.reply(
                MessageUtil.getMessage(
                    "OrderComplete.AllOrdersCompleted",
                )
            )
            return
        }
        actor.reply(
            MessageUtil.getMessage(
                "OrderComplete.OrderProgress",
                listOf(
                    MessageReplacement(ordersCompleted.toString()),
                    MessageReplacement(orderCount.toString()),
                    MessageReplacement(ordersUpdated.toString()),
                    MessageReplacement(orderCount.toString()),
                )

            )
        )
    }

    /**
     * Returns whether the given item matches the order
     */
    private fun isMatchingItem(item: ItemStack,order: Order): Boolean {
        ecoPlugin.let{
            if (Items.isCustomItem(item)) {
                return !Items.getCustomItem(order.item)!!.matches(item)
            }
        }
        return order.item.isSimilar(item)
    }

    private fun messageCompletion(actor: BukkitCommandActor, order: Order) {
        val assigneeMessage = MessageUtil.getMessage(
            "OrderComplete.CompletedMessageAssignee",
            listOf(
                MessageReplacement(order.getItemInfo()),
                MessageReplacement(order.cost.toString()),
            )
        )
        actor.reply(assigneeMessage)
        val ownerMessage = MessageUtil.getMessage(
            "OrderComplete.CompletedMessageOwner",
            listOf(
                MessageReplacement(order.getItemInfo()),
                MessageReplacement(actor.player.displayName()),
            )
        )
        order.messageOwner(ownerMessage)
    }

    private fun messageProgress(actor: BukkitCommandActor, order: Order){
        val assigneeMessage = MessageUtil.getMessage(
            "OrderComplete.ProgressMessageAssignee",
            listOf(
                MessageReplacement(order.getItemInfo()),
                MessageReplacement(order.itemCompleted.toString()),
                MessageReplacement(order.itemAmount.toString()),
            )
        )
        actor.reply(assigneeMessage)
        val ownerMessage = MessageUtil.getMessage(
            "OrderComplete.ProgressMessageOwner",
            listOf(
                MessageReplacement(order.getItemInfo()),
                MessageReplacement(actor.player.displayName()),
                MessageReplacement(order.itemCompleted.toString()),
                MessageReplacement(order.itemAmount.toString()),
            )
        )
        order.messageOwner(ownerMessage)
    }
}