package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.gui.OwnedOrders
import net.refractored.joblistings.messages.Messages
import net.refractored.joblistings.messages.Messages.replace
import org.bukkit.entity.Player
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class OwnedOrders {
    @CommandPermission("joblistings.view.owned")
    @ConfigCommand("messages.owned")
    fun viewOrder(actor: BukkitCommandActor) {
        actor.requirePlayer().openInventory(OwnedOrders.getGUI(actor.requirePlayer()).inventory)
        actor.reply(Messages.getMessagePrefixed("messages.owned.execution.success"))
    }

    @CommandPermission("joblistings.view.owned.other")
    @ConfigCommand("messages.owned-other")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player
    ) {
        player.openInventory(OwnedOrders.getGUI(player).inventory)
        actor.reply(
            Messages
                .getMessagePrefixed("messages.owned-other.execution.success")
                .replace("%player%", player.name),
        )
    }
}
