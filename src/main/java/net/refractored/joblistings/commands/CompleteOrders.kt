package net.refractored.joblistings.commands

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.j256.ormlite.stmt.QueryBuilder
import kotlinx.coroutines.withContext
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.database.Database.orderDao
import net.refractored.joblistings.messages.Messages
import net.refractored.joblistings.messages.Messages.replace
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*

class CompleteOrders {
    @CommandPermission("joblistings.completeorders")
    @ConfigCommand("messages.complete")
    fun completeOrders(actor: BukkitCommandActor) {
        JobListings.instance.launch {
            withContext(JobListings.instance.asyncDispatcher) {
                val player = actor.requirePlayer()

                val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
                queryBuilder
                    .where()
                    .eq("assignee", actor.uniqueId())
                    .and()
                    .eq("status", OrderStatus.CLAIMED)
                val orders = orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }

                if (orders.isEmpty()) {
                    actor.reply(
                        Messages.getMessagePrefixed("messages.complete.execution.no-orders"),
                    )
                    return@withContext
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
                    actor.reply(
                        Messages.getMessagePrefixed("messages.complete.execution.no-valid-items"),
                    )
                    return@withContext
                }

                if (ordersCompleted == orderCount) {
                    actor.reply(
                        Messages.getString(
                            "messages.complete.execution.success.all-completed",
                        ),
                    )
                    return@withContext
                }

                actor.reply(
                    Messages.getMessage("messages.complete.execution.success.progress")
                        .replace("%1", ordersCompleted.toString())
                        .replace("%2", ordersUpdated.toString())
                        .replace("%3", orderCount.toString()),
                )
            }
        }
    }

    private fun messageProgress(
        actor: BukkitCommandActor,
        order: Order
    ) {
        val assigneeMessage = Messages.getMessage("OrderComplete.ProgressMessageAssignee")
            .replace("%1", order.getItemInfo())
            .replace("%2", order.itemCompleted.toString())
            .replace("%3", order.itemAmount.toString())
        actor.reply(assigneeMessage)
        val ownerMessage = Messages.getMessage("OrderComplete.ProgressMessageOwner")
            .replace("%1", order.getItemInfo())
            .replace("%2", order.getAssignee()!!.name!!)
            .replace("%3", order.itemCompleted.toString())
            .replace("%4", order.itemAmount.toString())
        order.messageOwner(ownerMessage)
    }
}
