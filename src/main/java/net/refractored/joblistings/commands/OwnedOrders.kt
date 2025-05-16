package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.gui.MyOrders
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.miniToComponent
import org.bukkit.entity.Player
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class OwnedOrders {
    @CommandPermission("joblistings.view.owned")
    @ConfigCommand("messages.owned")
    fun viewOrder(actor: BukkitCommandActor) {
        actor.requirePlayer().openInventory(MyOrders.getGUI(actor.requirePlayer()).inventory)
        actor.reply(Messages.getStringPrefixed("messages.owned.execution.success").miniToComponent())
    }

    @CommandPermission("joblistings.view.owned.other")
    @ConfigCommand("messages.owned-other")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player,
    ) {
        player.openInventory(MyOrders.getGUI(actor.requirePlayer()).inventory)
        actor.reply(
            Messages
                .getStringPrefixed("messages.owned-other.execution.success")
                .replace("%player%", player.name)
                .miniToComponent(),
        )
    }
}
