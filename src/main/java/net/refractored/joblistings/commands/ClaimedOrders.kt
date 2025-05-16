package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.commands.annotations.ConfigDescription
import net.refractored.joblistings.gui.ClaimedOrders
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.miniToComponent
import org.bukkit.entity.Player
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ClaimedOrders {
    @CommandPermission("joblistings.view.claimed")
    @ConfigDescription("messages.claimed.description")
    @ConfigCommand("messages.claimed.command")
    fun openClaimed(actor: BukkitCommandActor) {
        actor.requirePlayer().openInventory(ClaimedOrders.getGUI(actor.requirePlayer()).inventory)
        actor.reply(Messages.getStringPrefixed("messages.claimed.execution.success").miniToComponent())
    }

    @CommandPermission("joblistings.view.claimed.other")
    @ConfigDescription("messages.claimed-other.description")
    @ConfigCommand("messages.claimed-other.command")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player,
    ) {
        player.openInventory(ClaimedOrders.getGUI(actor.requirePlayer()).inventory)
        actor.reply(Messages.getStringPrefixed("messages.claimed-other.execution.success").miniToComponent())
    }
}
