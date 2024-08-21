package net.refractored.joblistings.commands

import net.refractored.joblistings.config.Presets
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import revxrsal.commands.annotation.AutoComplete
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player

class PresetGet {
    @CommandPermission("joblistings.admin.get.preset")
    @Description("Gets the item for the preset.")
    @Command("joblistings preset get")
    @AutoComplete("@presets")
    fun presetGet(
        actor: BukkitCommandActor,
        presetName: String,
    ) {
        if (actor.isConsole) {
            throw CommandErrorException(
                MessageUtil.getMessage("General.IsNotPlayer"),
            )
        }

        val item = Presets.getPresets()[presetName]
            ?: throw CommandErrorException(
                MessageUtil.getMessage("PresetInfo.PresetDoesNotExist"),
            )

        val itemNumber = (actor.player.inventory.addItem(item)).values.sumOf { it.amount }

        if (itemNumber != 0) {
            throw CommandErrorException(
                MessageUtil.getMessage("PresetGet.InventoryFull"),
            )
        }

        actor.reply(
            MessageUtil.getMessage(
                "PresetGet.GotPreset",
                listOf(
                    MessageReplacement(presetName),
                ),
            ),
        )
    }
}
