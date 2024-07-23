package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.inventory.meta.Damageable
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import java.util.*

class CompleteOrders {
    @CommandPermission("joblistings.completeorders")
    @Description("Scans your inventory for items to complete an order")
    @Command("joblistings complete")
    fun completeOrders(actor: BukkitCommandActor) {
        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder
            .where()
            .eq("assignee", actor.uniqueId)
            .and()
            .eq("status", OrderStatus.CLAIMED)
        val orders = orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }
        if (orders.isEmpty()) {
            throw CommandErrorException(MessageUtil.getMessage("OrderComplete.NoOrdersToComplete"))
        }
        val orderCount = orders.count()
        var ordersUpdated = 0
        var ordersCompleted = 0
        // TODO: REWRITE THIS ATROCITY
        forEachOrder@ for (order in orders) {
            forEachItem@ for (item in actor.player.inventory.storageContents) {
                if (item == null) continue@forEachItem
                if (!order.itemMatches(item)) continue@forEachItem
                if (order.item is Damageable && item.itemMeta is Damageable) {
                    if ((order.item as Damageable).damage != (item.itemMeta as Damageable).damage) {
                        actor.reply(
                            MessageUtil.getMessage(
                                "OrderComplete.DamagedItem",
                                listOf(
                                    MessageReplacement(order.getItemInfo()),
                                ),
                            ),
                        )
                        continue@forEachItem
                    }
                }
                if (order.itemCompleted + item.amount >= order.itemAmount) {
                    // Order completed YIPPEE
                    val itemsLeft = (order.itemCompleted + item.amount) - order.itemAmount
                    order.completeOrder(true)
                    item.amount = itemsLeft
                    ordersCompleted++
                    continue@forEachOrder
                }
                // Order not completed :(
                order.itemCompleted += item.amount
                orderDao.update(order)
                item.amount = 0
                ordersUpdated++
                messageProgress(actor, order)
                continue@forEachItem
            }
        }
        if (ordersUpdated == 0 && ordersCompleted == 0) {
            throw CommandErrorException(
                MessageUtil.getMessage(
                    "OrderComplete.NoItemsFound",
                ),
            )
        }
        if (ordersCompleted == orderCount) {
            actor.reply(
                MessageUtil.getMessage(
                    "OrderComplete.AllOrdersCompleted",
                ),
            )
            return
        }
        actor.reply(
            MessageUtil.getMessage(
                "OrderComplete.OrderProgress",
                listOf(
                    MessageReplacement(ordersCompleted.toString()),
                    MessageReplacement(ordersUpdated.toString()),
                    MessageReplacement(orderCount.toString()),
                ),
            ),
        )
    }

    private fun messageProgress(
        actor: BukkitCommandActor,
        order: Order,
    ) {
        val assigneeMessage =
            MessageUtil.getMessage(
                "OrderComplete.ProgressMessageAssignee",
                listOf(
                    MessageReplacement(order.getItemInfo()),
                    MessageReplacement(order.itemCompleted.toString()),
                    MessageReplacement(order.itemAmount.toString()),
                ),
            )
        actor.reply(assigneeMessage)
        val ownerMessage =
            MessageUtil.getMessage(
                "OrderComplete.ProgressMessageOwner",
                listOf(
                    MessageReplacement(order.getItemInfo()),
                    MessageReplacement(actor.player.displayName()),
                    MessageReplacement(order.itemCompleted.toString()),
                    MessageReplacement(order.itemAmount.toString()),
                ),
            )
        order.messageOwner(ownerMessage)
    }
}
