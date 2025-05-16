package net.refractored.joblistings.commands.autocomplete

import net.refractored.joblistings.JobListings.Companion.instance
import org.bukkit.Material
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext

class MaterialSuggesstion : SuggestionProvider<BukkitCommandActor> {
    override fun getSuggestions(context: ExecutionContext<BukkitCommandActor>): List<String> {
        val config = instance.config
        val blacklistedMaterials = config.getStringList("orders.BlacklistedMaterials")
        val additionalBlacklistedMaterials = config.getStringList("orders.BlacklistedCreateMaterials")
        val blacklist =
            (blacklistedMaterials + additionalBlacklistedMaterials)
                .map { it.lowercase() }
                .toSet()
        val materialSuggestions =
            Material.entries
                .asSequence()
                .filter { it.isItem }
                .map { it.name.lowercase() }
                .filterNot { name -> name in blacklist.map { it.lowercase() } }
                .toMutableSet()
        return (materialSuggestions /*+ presetSuggestions*/).toList()
    }
}
