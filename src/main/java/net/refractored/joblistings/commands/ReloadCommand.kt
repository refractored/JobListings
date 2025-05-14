package net.refractored.joblistings.commands

import net.refractored.joblistings.JobListings
import net.refractored.joblistings.util.Messages
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ReloadCommand {
    @CommandPermission("joblistings.admin.reload")
    @Description("Reloads plugin configuration")
    @Command("joblistings reload")
    fun reload(actor: BukkitCommandActor) {
        JobListings.instance.reload()
        actor.reply(Messages.getString("Reload.Reloaded"))
    }
}
