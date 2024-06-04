package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.JobListings.Companion.eco
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import org.bukkit.Material
import org.bukkit.inventory.meta.Damageable
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import revxrsal.commands.exception.CommandErrorException
import java.time.LocalDateTime
import java.util.*

class CreateOrderHand {

    @CommandPermission("joblistings.order.create.hand")
    @Description("Create a new order.")
    @Command("joblistings create hand")
    fun CreateOrder(actor: BukkitCommandActor, cost: Double, amount: Int, hours: Long) {
        if (actor.isConsole) {
            throw CommandErrorException("You must be a player to use this command.")
        }

        if (amount < 1) {
            throw CommandErrorException("Amount must be at least 1.")
        }

        if (hours < 1) {
            throw CommandErrorException("Hours must be at least 1.")
        }

        if (hours > JobListings.instance.config.getLong("Orders.MaxOrdersTime")) {
            throw CommandErrorException("Hours must be less than or equal to ${JobListings.instance.config.getLong("Orders.MaxOrdersTime")}.")
        }

        if (hours < JobListings.instance.config.getLong("Orders.MinOrdersTime")) {
            throw CommandErrorException("Hours must be more than or equal to ${JobListings.instance.config.getLong("Orders.MinOrdersTime")}.")
        }

        if (cost < 1) {
            throw CommandErrorException("Cost must be at least 1.")
        }

        if (eco.getBalance(actor.player) < cost) {
            throw CommandErrorException("You do not have enough money to cover your payment.")
        }


        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder.orderBy("timeCreated", false)
        queryBuilder.where().eq("status", OrderStatus.PENDING)
        val orders = orderDao.query(queryBuilder.prepare())

        if (orders.count() > JobListings.instance.config.getInt("Orders.MaxOrders")) {
            throw CommandErrorException("You cannot have more than ${JobListings.instance.config.getInt("Orders.MaxOrders")} orders at once.")
        }

        val item = actor.player.inventory.itemInMainHand.clone()

        if (item.type == Material.AIR) {
            throw CommandErrorException("You must be holding an item to create an order.")
        }

        if (amount > item.maxStackSize) {
            throw CommandErrorException("Amount must be less than or equal to the max stack size of the item. (${item.maxStackSize} max)")
        }

        if (item.itemMeta is Damageable) {
            val damageableMeta = item.itemMeta as Damageable
            damageableMeta.damage = 0
            item.itemMeta = damageableMeta
        }

        item.amount = amount

        eco.withdrawPlayer(actor.player, cost)

        orderDao.create(
            Order(
                id = UUID.randomUUID(),
                cost = cost,
                user = actor.uniqueId,
                assignee = null,
                timeCreated = LocalDateTime.now(),
                timeExpires = LocalDateTime.now().plusHours(hours),
                timeDeadline = null,
                timeCompleted = null,
                timeClaimed = null,
                status = OrderStatus.PENDING,
                item = item,
            )
        )

        actor.player.sendMessage("Order created!")
    }

}