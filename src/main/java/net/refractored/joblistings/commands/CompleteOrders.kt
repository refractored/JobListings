package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageUtil
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import revxrsal.commands.exception.CommandErrorException
import java.time.LocalDateTime
import java.util.*

class CompleteOrders {
    @CommandPermission("joblistings.view.completeorders")
    @Description("Finish your orders.")
    @Command("joblistings complete")
    fun completeOrders(actor: BukkitCommandActor) {
        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder.orderBy("timeCreated", false)
        queryBuilder.where().eq("assignee", actor.uniqueId)
        queryBuilder.where().eq("status", OrderStatus.CLAIMED)
        val orders = orderDao.query(queryBuilder.prepare())
        if (orders.isEmpty()) {
            throw CommandErrorException("You have no orders to complete.")
        }
        val orderCount = orders.size
        var completionCount = 0
        for (order in orders) {
            // If item is split into multiple stacks, it will not detect it.`
            val itemStack = actor.player.inventory
                .firstOrNull{ it?.isSimilar(order.item) ?: false && it.amount >= order.item.amount } ?: continue
            itemStack.amount -= order.item.amount
            order.status = OrderStatus.COMPLETED
            order.timeCompleted = LocalDateTime.now()
            orderDao.update(order)
            JobListings.eco.depositPlayer(actor.player, order.cost)
            completionCount++
            val assigneeMessage =  Component.text()
                .append(MessageUtil.toComponent(
                    "<green>You have completed the order <gray>"
                ))
                .append(order.itemInfo)
                .append(MessageUtil.toComponent(
                    "<gray> and received <gold>${order.cost}</gold>."
                ))
                .build()
            actor.reply(assigneeMessage)
            val ownerMessage = Component.text()
                .append(MessageUtil.toComponent(
                "<green>One of your orders, <gray>"
            ))
                .append(order.itemInfo)
                .append(MessageUtil.toComponent(
                    "<gray>, was completed!"
                ))
                .build()
            order.messageOwner(ownerMessage)
        }
        if (completionCount == 0) {
            throw CommandErrorException("None your claimed order's requirements were met.")
        }
        if (completionCount == orderCount) {
            actor.reply(MessageUtil.toComponent("<green>All your orders have been completed."))
            return
        }
        actor.reply(
            MessageUtil.toComponent("<green><gold>$completionCount</gold> orders have been completed out of <gold>$orderCount</gold>." +
                    "\nYou now have <gold>${(orderCount - completionCount)}/<gold> orders left")
        )
    }
}