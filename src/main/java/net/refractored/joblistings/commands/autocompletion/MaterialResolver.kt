package net.refractored.joblistings.commands.autocompletion

import com.willfp.eco.util.containsIgnoreCase
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
    ): MutableCollection<String> =
        Material.entries
            .asSequence()
            .map { it.name }
            .map { it.lowercase() }
            .filter { !blacklistedMaterial(it) }
            .filter {
                it.startsWith((args.joinToString(" ")), true)
            }.toMutableSet()

    private fun blacklistedMaterial(arg: String): Boolean {
        val blacklistedMaterials = JobListings.instance.config.getStringList("BlacklistedMaterials")
        blacklistedMaterials.addAll(
            JobListings.instance.config.getStringList("BlacklistedCreateMaterials"),
        )
        return blacklistedMaterials.containsIgnoreCase(arg)
    }
}
