package net.refractored.joblistings.commands

import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.gui.ClaimedOrders
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ClaimedOrders {
    @CommandPermission("joblistings.view.claimed")
    @Description("View the orders you claimed")
    @Command("joblistings claimed")
    fun getOrders(
        actor: BukkitCommandActor,
        @Optional player: Player? = null,
    ) {
        if (player == null) {
            actor.requirePlayer().openInventory(ClaimedOrders.getGUI(actor.requirePlayer()).inventory)
            return
        }
        if (!actor.isConsole || !actor.requirePlayer().hasPermission("joblistings.view.claimed.other")) {
            throw CommandErrorException(MessageUtil.getMessage("General.NoPermission"))
        }
        player.openInventory(ClaimedOrders.getGUI(player).inventory)
    }
}
