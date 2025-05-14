package net.refractored.joblistings.commands

import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.gui.AllOrders
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class GetOrders {
    @CommandPermission("joblistings.view.orders")
    @Description("View all pending orders")
    @Command("joblistings orders")
    fun getOrders(
        actor: BukkitCommandActor,
        @Optional player: Player = actor.requirePlayer(),
    ) {
        if (!player.hasPermission("joblistings.view.orders.others")) {
            throw CommandErrorException(MessageUtil.getMessage("General.NoPermission"))
        }
        if (actor.isConsole) {
            throw CommandErrorException(MessageUtil.getMessage("General.PlayerOnly"))
        }
        AllOrders.openGUI(player)
    }
}
