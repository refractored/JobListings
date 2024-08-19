package net.refractored.joblistings.commands

import net.refractored.joblistings.JobListings
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player

class CreatePreset {
    @CommandPermission("joblistings.admin.create.preset")
    @Description("Reloads plugin configuration")
    @Command("joblistings create preset")
    fun createPreset(
        actor: BukkitCommandActor,
        presetName: String,
    ) {
        if (actor.isConsole) {
            throw CommandErrorException(
                MessageUtil.getMessage("General.IsNotPlayer"),
            )
        }
        if (Material.entries.any { it.name.equals(presetName, true) }) {
            throw CommandErrorException(
                MessageUtil.getMessage("CreatePreset.MaterialAlreadyExists"),
            )
        }
        val item =
            actor.player.inventory.itemInMainHand
                .clone()

        if (item.type == Material.AIR) {
            throw CommandErrorException(
                MessageUtil.getMessage(
                    "CreatePreset.NotHoldingItem",
                ),
            )
        }

        JobListings.instance.presets.set(presetName, item)
    }
}
