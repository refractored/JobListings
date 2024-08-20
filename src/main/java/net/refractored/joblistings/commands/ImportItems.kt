package net.refractored.joblistings.commands

import com.willfp.eco.core.items.Items
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.config.Presets
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import revxrsal.commands.annotation.AutoComplete
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ImportItems {
    @CommandPermission("joblistings.admin.import")
    @Description("Reloads plugin configuration")
    @Command("joblistings preset import")
    @AutoComplete("eco")
    fun importItems(
        actor: BukkitCommandActor,
        import: String,
    ) {
        when (import) {
            "eco" -> {
                if (!JobListings.instance.ecoPlugin) {
                    throw CommandErrorException(
                        MessageUtil.getMessage(
                            "ImportItems.PluginNotLoaded",
                            listOf(
                                MessageReplacement(import),
                            ),
                        ),
                    )
                }
                actor.reply(
                    MessageUtil.getMessage("ImportItems.ImportingItems"),
                )
                importEcoItems()
            }
            else -> throw CommandErrorException(
                MessageUtil.getMessage("ImportItems.UnsupportedPlugin"),
            )
        }

        actor.reply(
            MessageUtil.getMessage("ImportItems.DoneImportingItems"),
        )
    }

    private fun importEcoItems() {
        val customItems = Items.getCustomItems()

        for (items in customItems) {
            var name = items.key.key

            name = name.removePrefix("set_")
            JobListings.instance.logger.info("Creating preset for $name")

            try {
                Presets.createPreset(name, items.item)
            } catch (e: IllegalArgumentException) {
                JobListings.instance.logger.warning("Preset already exists for $name")
                continue
            }
        }
    }
}
