package net.refractored.joblistings.commands

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.j256.ormlite.stmt.QueryBuilder
import kotlinx.coroutines.withContext
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.database.Database.orderDao
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.miniToComponent
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
                        Messages.getStringPrefixed("messages.complete.execution.no-orders").miniToComponent(),
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
                    throw CommandErrorException(
                        Messages
                            .getStringPrefixed(
                                "messages.complete.execution.no-valid-items",
                            ).miniToComponent(),
                    )
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
                    MessageUtil.getMessage(
                        "messages.complete.execution.success.progress",
                        listOf(
                            MessageReplacement(ordersCompleted.toString()),
                            MessageReplacement(ordersUpdated.toString()),
                            MessageReplacement(orderCount.toString()),
                        ),
                    ),
                )
            }
        }
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
