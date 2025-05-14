package net.refractored.joblistings.commands

import net.refractored.joblistings.gui.AllOrders
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPriority
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class GetOrders {
    @CommandPermission("joblistings.view.orders")
    @Description("Opens the orders GUI for the player")
    @CommandPriority.Low
    @Command("joblistings")
    fun defaultCommand(actor: BukkitCommandActor) {
        AllOrders.openGUI(actor.requirePlayer())
    }

    @CommandPermission("joblistings.view.orders")
    @Description("Opens the orders GUI for the player")
    @Command("joblistings orders")
    fun openOrders(actor: BukkitCommandActor) {
        AllOrders.openGUI(actor.requirePlayer())
    }

    @CommandPermission("joblistings.view.orders.other")
    @Description("Opens the orders GUI for another player")
    @Command("joblistings orders other")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player,
    ) {
        AllOrders.openGUI(player)
    }
}
