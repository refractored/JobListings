package net.refractored.joblistings.commands.autocompletion

import net.refractored.joblistings.JobListings
import net.refractored.joblistings.config.Presets
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.command.CommandActor
import revxrsal.commands.command.ExecutableCommand
import revxrsal.commands.process.ValueResolver

class MaterialResolver :
    ValueResolver<OrderStack>,
    SuggestionProvider {
    override fun resolve(context: ValueResolver.ValueResolverContext): OrderStack {
        val name = context.arguments().pop()
        if (name.equals("AIR", true)) {
            throw CommandErrorException(
                MessageUtil.getMessage("CreateOrder.MaterialSetToAir"),
            )
        }
        return OrderStack(name)
    }

    override fun getSuggestions(
        args: MutableList<String>,
        sender: CommandActor,
        command: ExecutableCommand,
    ): MutableCollection<String> {
        val stringArgs = args.joinToString(" ").lowercase()

        val materialSuggestions =
            Material.entries
                .asSequence()
                .map { it.name.lowercase() }
                .filterNot { name -> name in getBlacklistedMaterials().map { it.lowercase() } }
                .toMutableSet()

        val presetSuggestions = Presets.getPresets().keys

        return (materialSuggestions + presetSuggestions)
            .filter { it.startsWith(stringArgs, ignoreCase = true) }
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
