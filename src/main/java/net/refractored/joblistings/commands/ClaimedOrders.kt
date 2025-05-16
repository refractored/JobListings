package net.refractored.joblistings.commands

import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.gui.ClaimedOrders
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ClaimedOrders {
    @CommandPermission("joblistings.view.claimed")
    @Description("Opens the claimed orders GUI for the player")
    @ConfigCommand("claimed")
    fun openClaimed(actor: BukkitCommandActor) {
        actor.requirePlayer().openInventory(ClaimedOrders.getGUI(actor.requirePlayer()).inventory)
    }

    @CommandPermission("joblistings.view.claimed.other")
    @Description("Opens the claimed orders GUI for another player")
    @ConfigCommand("claimed other")
    fun openOrdersOther(
        actor: BukkitCommandActor,
        player: Player,
    ) {
        actor.requirePlayer().openInventory(ClaimedOrders.getGUI(actor.requirePlayer()).inventory)
    }
}
