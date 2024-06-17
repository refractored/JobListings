package net.refractored.joblistings.commands

import net.refractored.joblistings.util.MessageUtil
import revxrsal.commands.annotation.DefaultFor
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class HelpCommand {
    @CommandPermission("joblistings.help")
    @Description("Displays command info")
    @DefaultFor("joblistings")
    fun help(actor: BukkitCommandActor) {
        actor.reply(MessageUtil.getMessage("Help.Page1"))
    }
}