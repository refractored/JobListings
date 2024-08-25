package net.refractored.joblistings.exceptions

import net.kyori.adventure.text.Component
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.command.CommandActor
import revxrsal.commands.exception.SendableException

class CommandErrorException(
    val component: Component,
) : SendableException("") {
    override fun sendTo(actor: CommandActor) {
        if (actor is BukkitCommandActor) {
            actor.reply(this.component)
        } else {
            throw IllegalArgumentException("actor is not type BukkitCommandActor")
        }
    }
}
