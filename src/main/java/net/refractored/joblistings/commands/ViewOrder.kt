package net.refractored.joblistings.commands

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import revxrsal.commands.exception.CommandErrorException


class ViewOrder {
    @CommandPermission("joblistings.order.create")
    @Description("Views order ingame")
    @Command("joblistings order create")
    fun ViewOrder(actor: BukkitCommandActor, cost: Double) {
        val order = Database.orderDao.queryForFieldValues(mapOf("user" to actor.uniqueId)).firstOrNull() ?:
            throw CommandErrorException("You do not have an order to view.")

        // Create a GUI with 3 rows (27 slots)
        val myAwesomeMenu: SGMenu = JobListings.spiGUI.create(":3", 3)
        ItemStack.deserialize(order.item!!)

        // Create a button
        val myAwesomeButton = SGButton( // Includes an ItemBuilder class with chainable methods to easily
            order.item
        ).withListener { event: InventoryClickEvent ->
            // Events are cancelled automatically, unless you turn it off
            // for your plugin or for this inventory.
            event.whoClicked.sendMessage("Hello, world!")
        }


        // Add the button to your GUI
        myAwesomeMenu.addButton(myAwesomeButton)


        // Show the GUI
        actor.player.openInventory(myAwesomeMenu.inventory)

    }
}