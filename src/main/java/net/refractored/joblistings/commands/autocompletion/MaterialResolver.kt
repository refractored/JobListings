package net.refractored.joblistings.commands.autocompletion

import net.refractored.joblistings.JobListings
import org.bukkit.Material
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.command.CommandActor
import revxrsal.commands.command.ExecutableCommand

class MaterialResolver : SuggestionProvider {
    override fun getSuggestions(
        args: MutableList<String>,
        sender: CommandActor,
        command: ExecutableCommand,
    ): MutableCollection<String> {
        val stringArgs = args.joinToString(" ")
        return Material.entries
            .map { it.name.lowercase() }
            .filter { !getBlacklistedMaterials().contains(it.lowercase()) }
            .filter { it.startsWith(stringArgs, true) }
            .toMutableSet()
    }

    private fun getBlacklistedMaterials(): Set<String> {
        val config = JobListings.instance.config
        val blacklistedMaterials = config.getStringList("Orders.BlacklistedMaterials")
        val additionalBlacklistedMaterials = config.getStringList("Orders.BlacklistedCreateMaterials")
        return (blacklistedMaterials + additionalBlacklistedMaterials)
            .map { it.lowercase() }
            .toSet()
    }
}
