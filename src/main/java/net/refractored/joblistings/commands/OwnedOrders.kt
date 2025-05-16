package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.gui.MyOrders
import org.bukkit.entity.Player
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class OwnedOrders {
    @CommandPermission("joblistings.view.owned")
    @ConfigCommand("messages.owned")
    fun viewOrder(actor: BukkitCommandActor) {
        actor.requirePlayer().openInventory(MyOrders.getGUI(actor.requirePlayer()).inventory)
    }

    @CommandPermission("joblistings.view.owned.other")
    @ConfigCommand("messages.owned-other")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player,
    ) {
        player.openInventory(MyOrders.getGUI(actor.requirePlayer()).inventory)
    }
}
