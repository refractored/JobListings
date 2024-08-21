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

class PresetInfo {
    @CommandPermission("joblistings.admin.view.preset")
    @Description("Gets the info for a preset in chat.")
    @Command("joblistings preset info")
    @AutoComplete("@presets")
    fun presetInfo(
        actor: BukkitCommandActor,
        presetName: String,
    ) {
        if (actor.isConsole) {
            throw CommandErrorException(
                MessageUtil.getMessage("General.IsNotPlayer"),
            )
        }
        val item = Presets.getPresets()[presetName]
        if (item == null) {
            throw CommandErrorException(
                MessageUtil.getMessage("PresetInfo.PresetDoesNotExist"),
            )
        }
        actor.reply(
            MessageUtil.getMessage(
                "PresetInfo.PresetInfo",
                listOf(
                    MessageReplacement(presetName),
                    MessageReplacement(item.displayName()),
                ),
            ),
        )
    }
}
