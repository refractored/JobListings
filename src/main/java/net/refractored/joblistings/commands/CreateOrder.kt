package net.refractored.joblistings.commands

import net.refractored.joblistings.database.Database
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import org.bukkit.Material
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import revxrsal.commands.exception.CommandErrorException
import javax.xml.crypto.Data

class CreateOrder {
    @CommandPermission("joblistings.order.create")
    @Description("Create a new order.")
    @Command("joblistings order create")
    fun CreateOrder(actor: BukkitCommandActor, cost: Double) {
        val order = Database.orderDao.queryForFieldValues(mapOf("user" to actor.uniqueId)).firstOrNull()
        if (order != null) {
            throw CommandErrorException("You already have an order.")
        }

        if (actor.player.inventory.itemInMainHand.type != Material.AIR)
            throw CommandErrorException("You must be holding an item to create an order.")

        Database.orderDao.create(
            Order(
                id = java.util.UUID.randomUUID(),
                cost = cost,
                user = actor.uniqueId,
                timeCreated = java.util.Date(),
                assignee = null,
                item = actor.player.inventory.itemInMainHand.serializeAsBytes(),
                status = OrderStatus.PENDING,
            )
        )
    }
}