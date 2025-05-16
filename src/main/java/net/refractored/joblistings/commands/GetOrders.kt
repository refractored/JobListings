package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.commands.annotations.ConfigDescription
import net.refractored.joblistings.gui.AllOrders
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.miniToComponent
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPriority
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class GetOrders {
    @CommandPermission("joblistings.view.orders")
    @ConfigDescription("messages.orders.description")
    @ConfigCommand("messages.orders.command")
    fun openOrders(actor: BukkitCommandActor) {
        AllOrders.openGUI(actor.requirePlayer())
        actor.reply(Messages.getStringPrefixed("messages.orders.execution.success").miniToComponent())
    }

    @CommandPermission("joblistings.view.orders.other")
    @ConfigDescription("messages.orders-other.description")
    @ConfigCommand("messages.orders-other.command")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player,
    ) {
        AllOrders.openGUI(player)
        actor.reply(Messages.getStringPrefixed("messages.orders-other.execution.success").miniToComponent())
    }
}

class GetOrdersBlank {
    @CommandPermission("joblistings.view.orders")
    @ConfigDescription("messages.orders.description")
    @CommandPriority.Low
    @ConfigCommand("")
    fun defaultCommand(actor: BukkitCommandActor) {
        AllOrders.openGUI(actor.requirePlayer())
        actor.reply(Messages.getStringPrefixed("messages.orders.execution.success").miniToComponent())
    }
}
