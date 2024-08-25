package net.refractored.joblistings.commands

import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.gui.AllOrders
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.entity.Player
import revxrsal.commands.annotation.DefaultFor
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player

class GetOrders {
    @CommandPermission("joblistings.view.orders")
    @Description("View all pending orders")
    @DefaultFor("joblistings")
    @Subcommand("orders")
    fun getOrders(
        actor: BukkitCommandActor,
        @Optional player: Player? = null,
    ) {
        if (player == null) {
            actor.player.openInventory(AllOrders.getGUI().inventory)
            return
        }
        if (!actor.player.hasPermission("joblistings.view.orders.others")) {
            throw CommandErrorException(MessageUtil.getMessage("General.NoPermission"))
        }
        if (actor.isConsole) {
            throw CommandErrorException(MessageUtil.getMessage("General.PlayerOnly"))
        }
        player.openInventory(AllOrders.getGUI().inventory)
    }
}
