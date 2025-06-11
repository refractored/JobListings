package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.gui.ClaimedOrders
import net.refractored.joblistings.messages.Messages
import net.refractored.joblistings.messages.Messages.replace
import org.bukkit.entity.Player
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ClaimedOrders {
    @CommandPermission("joblistings.view.claimed")
    @ConfigCommand("messages.claimed")
    fun openClaimed(actor: BukkitCommandActor) {
        actor.requirePlayer().openInventory(ClaimedOrders.getGUI(actor.requirePlayer()).inventory)
        actor.reply(Messages.getMessagePrefixed("messages.claimed.execution.success"))
    }

    @CommandPermission("joblistings.view.claimed.other")
    @ConfigCommand("messages.claimed-other")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player
    ) {
        player.openInventory(ClaimedOrders.getGUI(player).inventory)
        actor.reply(
            Messages
                .getMessagePrefixed("messages.claimed-other.execution.success")
                .replace("%player%", player.name),
        )
    }
}
