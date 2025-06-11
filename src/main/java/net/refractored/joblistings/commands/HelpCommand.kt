package net.refractored.joblistings.commands

import net.refractored.joblistings.JobListings
import net.refractored.joblistings.commands.annotations.ConfigCommand
import net.refractored.joblistings.messages.Messages
import net.refractored.joblistings.messages.Messages.replace
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Range
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.help.Help

private const val ENTRIES_PER_PAGE = 5

class HelpCommand {
    @CommandPermission("joblistings.help")
    @ConfigCommand("messages.help")
    fun help(
        actor: BukkitCommandActor,
        @Range(min = 1.0) @Optional page: Int = 1,
        commands: Help.RelatedCommands<BukkitCommandActor>
    ) {
        val list = commands.paginate(page, ENTRIES_PER_PAGE)

        actor.reply(
            Messages
                .getMessage("messages.help.execution.success.header")
                .replace(
                    "%authors%",
                    JobListings.instance.pluginMeta.authors.joinToString(
                        ", ",
                    ),
                ),
        )

        for (command in list) {
            if (!command.permission().isExecutableBy(actor)) continue
            actor.reply(
                Messages
                    .getMessagePrefixed("messages.help.execution.success.command")
                    .replace("%command%", command.usage())
                    .replace("%description%", command.description() ?: ""),
            )
        }

        actor.reply(
            Messages
                .getMessagePrefixed("messages.help.execution.success.footer")
                .replace("%page%", page.toString())
                .replace("%total%", Help.numberOfPages(commands.count(), ENTRIES_PER_PAGE).toString()),
        )
    }
}
