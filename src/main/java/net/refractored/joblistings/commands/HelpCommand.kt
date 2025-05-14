package net.refractored.joblistings.commands

import net.refractored.joblistings.JobListings
import net.refractored.joblistings.util.Messages.miniToComponent
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Range
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.help.Help

private const val ENTRIES_PER_PAGE = 5

class HelpCommand {
    @CommandPermission("joblistings.help")
    @Description("Displays command info")
    @Command("joblistings help")
    fun help(
        actor: BukkitCommandActor,
        @Range(min = 1.0) @Optional page: Int = 1,
        commands: Help.RelatedCommands<BukkitCommandActor>,
    ) {
        val list =
            commands
                .paginate(page, ENTRIES_PER_PAGE)
                .filter { it.permission().isExecutableBy(actor) }
        // TODO: Messageify
        actor.reply(
            (
                "<bold><gradient:#7ddb6d:#4CB13B>JobListings</gradient></bold><gray>" +
                    " by ${JobListings.instance.pluginMeta.authors.joinToString(
                        ", ",
                    )}"
            ).miniToComponent(),
        )
        for (command in list) {
            actor.reply("<gray>/${command.usage()}<white>: ${command.description()}".miniToComponent())
        }
        actor.reply("Page $page/${Help.numberOfPages(commands.count(), ENTRIES_PER_PAGE)} ")
    }
}
