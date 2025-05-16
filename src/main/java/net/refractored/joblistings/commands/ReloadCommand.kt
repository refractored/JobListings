package net.refractored.joblistings.commands

import net.refractored.joblistings.JobListings
import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.util.Messages
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ReloadCommand {
    @CommandPermission("joblistings.admin.reload")
    @ConfigCommand("messages.reload")
    fun reload(actor: BukkitCommandActor) {
        JobListings.instance.reload()
        actor.reply(Messages.getStringPrefixed("messages.reload.execution.success"))
    }
}
