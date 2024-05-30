package net.refractored.joblistings.commands

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.serializers.ItemstackSerializers
import org.bukkit.event.inventory.InventoryClickEvent
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import revxrsal.commands.exception.CommandErrorException
import net.refractored.joblistings.JobListings.Companion.eco
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit


class ViewOrder {
    @CommandPermission("joblistings.order.create")
    @Description("Views order ingame")
    @Command("joblistings order view")
    fun ViewOrder(actor: BukkitCommandActor) {
        if (actor.isConsole) {
            throw CommandErrorException("You must be a player to use this command.")
        }

        val order = Database.orderDao.queryForFieldValues(mapOf("user" to actor.uniqueId)).firstOrNull() ?:
            throw CommandErrorException("You do not have an order to view.")

        val gui: SGMenu = JobListings.spiGUI.create("Your Order", 3)
        val item = ItemstackSerializers.deserialize(order.item)!!.clone()
        val itemMetaCopy = item.itemMeta
        val infoLore = listOf(
            MessageUtil.toComponent(""),
            MessageUtil.toComponent("<reset><red>Cost: <white>${order.cost}"),
            MessageUtil.toComponent("<reset><red>User: <white>${Bukkit.getOfflinePlayer(order.user).name}"),
            MessageUtil.toComponent("<reset><red>Created: <white>${order.timeCreated}"),
        )

        if (itemMetaCopy.hasLore()) {
            val itemLore = itemMetaCopy.lore()!!
            itemLore.addAll(infoLore)
            itemMetaCopy.lore(itemLore)
        } else {
            itemMetaCopy.lore(infoLore)
        }

        item.itemMeta = itemMetaCopy

        val button = SGButton(
            item
        ).withListener { event: InventoryClickEvent ->
            event.whoClicked.sendMessage("Order Deleted & Refunded!")
            Database.orderDao.delete(order)
            eco.depositPlayer(actor.player, order.cost)
            event.whoClicked.closeInventory()
        }

        gui.setButton(13, button)

        actor.player.openInventory(gui.inventory)

    }
}