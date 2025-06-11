package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.commands.annotations.ConfigDescription
import net.refractored.joblistings.gui.AllOrders
import net.refractored.joblistings.messages.Messages
import net.refractored.joblistings.messages.Messages.replace
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPriority
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class GetOrders {
    @CommandPermission("joblistings.view.orders")
    @ConfigCommand("messages.orders")
    fun openOrders(actor: BukkitCommandActor) {
        AllOrders.openGUI(actor.requirePlayer())
        actor.reply(Messages.getMessagePrefixed("messages.orders.execution.success"))
    }

    @CommandPermission("joblistings.view.orders.other")
    @ConfigCommand("messages.orders-other")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player
    ) {
        AllOrders.openGUI(player)
        actor.reply(
            Messages
                .getMessagePrefixed("messages.orders-other.execution.success")
                .replace("%player%", player.name),
        )
    }
}

class GetOrdersBlank {
    @CommandPermission("joblistings.view.orders")
    @ConfigCommand("")
    @ConfigDescription("messages.orders.description")
    @CommandPriority.Low
    fun defaultCommand(actor: BukkitCommandActor) {
        AllOrders.openGUI(actor.requirePlayer())
        actor.reply(Messages.getMessagePrefixed("messages.orders.execution.success"))
    }
}
