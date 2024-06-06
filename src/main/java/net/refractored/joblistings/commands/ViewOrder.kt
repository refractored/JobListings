package net.refractored.joblistings.commands

import net.refractored.joblistings.gui.MyOrders
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission


class ViewOrder {
    @CommandPermission("joblistings.view.owned")
    @Description("View and manage orders you own")
    @Command("joblistings owned")
    fun viewOrder(actor: BukkitCommandActor) {
        MyOrders.openMyOrders(actor)
    }
}