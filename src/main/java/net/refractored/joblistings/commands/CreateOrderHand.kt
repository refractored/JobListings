package net.refractored.joblistings.commands

import net.refractored.joblistings.JobListings.Companion.eco
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.serializers.ItemstackSerializers
import org.bukkit.Material
import org.bukkit.inventory.meta.Damageable
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import revxrsal.commands.exception.CommandErrorException

class CreateOrderHand {

    @CommandPermission("joblistings.order.create.hand")
    @Description("Create a new order.")
    @Command("joblistings create hand")
    fun CreateOrder(actor: BukkitCommandActor, cost: Double, amount: Int) {
        if (actor.isConsole) {
            throw CommandErrorException("You must be a player to use this command.")
        }

        if (amount < 1) {
            throw CommandErrorException("Amount must be at least 1.")
        }

        if (cost < 1) {
            throw CommandErrorException("Cost must be at least 1.")
        }

        if (eco.getBalance(actor.player) < cost) {
            throw CommandErrorException("You do not have enough money to cover your payment.")
        }

        val order = Database.orderDao.queryForFieldValues(mapOf("user" to actor.uniqueId))

        if (order.isNotEmpty()) {
            throw CommandErrorException("You already have an order.")
        }

        val item = actor.player.inventory.itemInMainHand.clone()

        if (item.type == Material.AIR) {
            throw CommandErrorException("You must be holding an item to create an order.")
        }

        if (amount > item.maxStackSize) {
            throw CommandErrorException("Amount must be less than or equal to the max stack size of the item.")
        }

        if (item.itemMeta is Damageable) {
            val damageableMeta = item.itemMeta as Damageable
            damageableMeta.damage = 0
            item.itemMeta = damageableMeta
        }

        eco.withdrawPlayer(actor.player, cost)

        Database.orderDao.create(
            Order(
                id = java.util.UUID.randomUUID(),
                cost = cost,
                user = actor.uniqueId,
                assignee = null,
                timeCreated = java.util.Date(),
                status = OrderStatus.PENDING,
                item = ItemstackSerializers.serialize(item),
                false,
            )
        )

        actor.player.sendMessage("Order created!")
    }

}