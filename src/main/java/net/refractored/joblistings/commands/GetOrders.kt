package net.refractored.joblistings.commands

import net.refractored.joblistings.gui.AllOrders
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player

class GetOrders {
    @CommandPermission("joblistings.view.orders")
    @Description("View all pending orders")
    @Command("joblistings orders")
    fun getOrders(actor: BukkitCommandActor) {
        actor.player.openInventory(AllOrders.getGUI().inventory)
    }
}
