package net.refractored.joblistings.commands.autocompletion

import org.bukkit.Material
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.command.CommandActor
import revxrsal.commands.command.ExecutableCommand

class MaterialResolver : SuggestionProvider {
    override fun getSuggestions(
        args: MutableList<String>,
        sender: CommandActor,
        command: ExecutableCommand,
    ): MutableCollection<String> =
        Material.entries
            .map { it.name }
            .map { it.lowercase() }
            .filter { it.startsWith((args.joinToString(" ")), true) }
            .toMutableSet()
}
