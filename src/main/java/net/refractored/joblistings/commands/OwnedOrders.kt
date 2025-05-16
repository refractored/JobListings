package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.commands.annotations.ConfigDescription
import net.refractored.joblistings.gui.MyOrders
import org.bukkit.entity.Player
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class OwnedOrders {
    @CommandPermission("joblistings.view.owned")
    @ConfigDescription("messages.owned.description")
    @ConfigCommand("messages.owned.command")
    fun viewOrder(actor: BukkitCommandActor) {
        actor.requirePlayer().openInventory(MyOrders.getGUI(actor.requirePlayer()).inventory)
    }

    @CommandPermission("joblistings.view.owned.other")
    @ConfigDescription("messages.owned-other.description")
    @ConfigCommand("messages.owned-other.command")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player,
    ) {
        actor.requirePlayer().openInventory(MyOrders.getGUI(actor.requirePlayer()).inventory)
    }
}
