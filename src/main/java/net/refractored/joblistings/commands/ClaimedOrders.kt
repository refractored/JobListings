package net.refractored.joblistings.commands

import net.refractored.joblistings.gui.MyClaimedOrders
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ClaimedOrders {
    @CommandPermission("joblistings.view.claimed")
    @Description("View the orders you claimed")
    @Command("joblistings claimed")
    fun getOrders(actor: BukkitCommandActor) {
        MyClaimedOrders.openMyClaimedOrders(actor)
    }
}