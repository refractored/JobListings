package net.refractored.joblistings.commands

import net.refractored.joblistings.gui.MyOrders
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class OwnedOrders {
    @CommandPermission("joblistings.view.owned")
    @Description("Opens the owned orders GUI for the player")
    @Command("joblistings owned")
    fun viewOrder(actor: BukkitCommandActor) {
        actor.requirePlayer().openInventory(MyOrders.getGUI(actor.requirePlayer()).inventory)
    }

    @CommandPermission("joblistings.view.owned.other")
    @Description("Opens the owned orders GUI for another player")
    @Command("joblistings owned other")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player,
    ) {
        actor.requirePlayer().openInventory(MyOrders.getGUI(actor.requirePlayer()).inventory)
    }
}
