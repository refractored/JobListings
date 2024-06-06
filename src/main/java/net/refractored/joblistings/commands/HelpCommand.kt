package net.refractored.joblistings.commands

import net.kyori.adventure.text.Component
import net.refractored.joblistings.gui.AllOrders
import net.refractored.joblistings.util.MessageUtil
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.DefaultFor
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class HelpCommand {
    @CommandPermission("joblistings.help")
    @Description("Displays command info")
    @Command("joblistings help")
    @DefaultFor("joblistings")
    fun help(actor: BukkitCommandActor) {
        actor.reply( Component.text()
            .append(MessageUtil.toComponent("<bold><gradient:#7ddb6d:#4CB13B>JobListings</gradient></bold><white>"))
            .appendNewline()
            .append(MessageUtil.toComponent("<gray>/joblistings orders<white>: View all pending orders"))
            .appendNewline()
            .append(MessageUtil.toComponent("<gray>/joblistings owned<white>: View all orders you own"))
            .appendNewline()
            .append(MessageUtil.toComponent("<gray>/joblistings claimed<white>: View the orders you claimed"))
            .appendNewline()
            .append(MessageUtil.toComponent("<gray>/joblistings complete<white>: Automatically complete orders"))
            .appendNewline()
            .append(MessageUtil.toComponent("<gray>/joblistings create hand<white>: Create an order from the item in your hand"))
            .appendNewline()
            .append(MessageUtil.toComponent("<gray>/joblistings create material<white>: Create an order from the specified material"))
        )
    }
}