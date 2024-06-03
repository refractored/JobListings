package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
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
    fun getOrders(actor: BukkitCommandActor) {
        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder.orderBy("timeCreated", false)
        queryBuilder.where().eq("asignee", actor.uniqueId)
        queryBuilder.where().eq("status", OrderStatus.CLAIMED)
        val orders = orderDao.query(queryBuilder.prepare())
        if (orders.isEmpty()) {
            throw CommandErrorException("You have no orders to complete.")
        }
        for (order in orders) {
            val itemStack = actor.player.inventory.firstOrNull { it.isSimilar(order.item) } ?: continue
            if (itemStack.amount < order.item.amount) continue
            itemStack.amount -= order.item.amount
            order.status = OrderStatus.COMPLETED
            order.timeCompleted = LocalDateTime.now()
            JobListings.eco.depositPlayer(actor.player, order.cost)
            actor.reply("You have completed an order and received ${order.cost}")
        }
    }
}