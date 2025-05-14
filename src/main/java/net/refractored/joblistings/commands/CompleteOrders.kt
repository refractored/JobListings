package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import net.refractored.joblistings.database.Database.orderDao
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*

class CompleteOrders {
    @CommandPermission("joblistings.completeorders")
    @Description("Scans your inventory for items to complete an order")
    @Command("joblistings complete")
    fun completeOrders(actor: BukkitCommandActor) {
        val player = actor.requirePlayer()

        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder
            .where()
            .eq("assignee", actor.uniqueId())
            .and()
            .eq("status", OrderStatus.CLAIMED)
        val orders = orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }

        if (orders.isEmpty()) {
            throw CommandErrorException(MessageUtil.getMessage("OrderComplete.NoOrdersToComplete"))
        }

        val orderCount = orders.count()
        var ordersUpdated = 0
        var ordersCompleted = 0

        for (item in player.inventory.contents) {
            if (item == null) continue
            val order = orders.find { it.itemMatches(item) } ?: continue
            val itemAmount = (order.itemCompleted + item.amount)
            if (itemAmount < order.itemAmount) {
                order.itemCompleted += item.amount
                orderDao.update(order)
                item.amount = 0
                ordersUpdated++
                messageProgress(actor, order)
                continue
            }
            order.completeOrder(true)
            item.amount = itemAmount - order.itemAmount
            ordersCompleted++
            continue
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
                    // TODO: Migrate to use new order system
                    MessageReplacement(order.getAssignee()!!.name!!),
                    MessageReplacement(order.itemCompleted.toString()),
                    MessageReplacement(order.itemAmount.toString()),
                ),
            )
        order.messageOwner(ownerMessage)
    }
}
