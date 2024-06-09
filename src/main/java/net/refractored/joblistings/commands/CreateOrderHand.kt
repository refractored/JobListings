package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.JobListings.Companion.eco
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import org.bukkit.inventory.meta.Damageable
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import revxrsal.commands.exception.CommandErrorException
import java.time.LocalDateTime
import java.util.*

class CreateOrderHand {
    @CommandPermission("joblistings.create.hand")
    @Description("Create an order from the item in your hand")
    @Command("joblistings create hand")
    fun CreateOrder(
        actor: BukkitCommandActor,
        cost: Double,
        @Optional amount: Int = 1,
        @Optional hours: Long = JobListings.instance.config.getLong("Orders.MaxOrdersTime"),
    ){
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
        queryBuilder.where().eq("status", OrderStatus.PENDING).and().eq("user", actor.uniqueId)
        val orders = orderDao.query(queryBuilder.prepare())

        if (orders.count() >= JobListings.instance.config.getInt("Orders.MaxOrders")) {
            throw CommandErrorException("You cannot have more than ${JobListings.instance.config.getInt("Orders.MaxOrders")} orders at once.")
        }

        val item = actor.player.inventory.itemInMainHand.clone()

        if (item.type == Material.AIR) {
            throw CommandErrorException("You must be holding an item to create an order.")
        }

        if (item.itemMeta is Damageable) {
            val damageableMeta = item.itemMeta as Damageable
            damageableMeta.damage = 0
            item.itemMeta = damageableMeta
        }

        val maxItems = JobListings.instance.config.getInt("Orders.MaximumItems")

        when {
            maxItems == -1 && amount > item.maxStackSize -> {
                throw CommandErrorException("Amount must be less than or equal to ${item.maxStackSize}.")
            }
            maxItems != 0 && amount >= maxItems -> {
                throw CommandErrorException("You cannot have more than $maxItems items for an order.")
            }
        }


        item.amount = 1

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
                itemAmount = amount,
                itemCompleted = 0,
                itemsReturned = 0,
                itemsObtained = 0,
                moneyReturned = false
            )
        )

        actor.player.sendMessage(
            Component.text()
                .append(MessageUtil.toComponent("<green>Order created for "))
                .append(item.displayName())
                .append(MessageUtil.toComponent(" x${amount}<reset>"))
                .appendNewline()
                .append(MessageUtil.toComponent("<green>Reward: <white>$cost<reset>"))
                .appendNewline()
                .append(MessageUtil.toComponent("<green>Expires in <white>$hours<reset><white> hours"))
                .build()
        )
    }

}