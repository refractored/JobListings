package net.refractored.joblistings.exceptions

import net.kyori.adventure.text.ComponentLike
import net.refractored.joblistings.util.Messages.toPlaintext
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.command.CommandActor
import revxrsal.commands.exception.SendableException

class CommandErrorException(
    val component: ComponentLike,
) : SendableException() {
    override fun sendTo(actor: CommandActor) {
        if (actor is BukkitCommandActor) {
            actor.reply(this.component)
        } else {
            actor.reply(this.component.asComponent().toPlaintext())
        }
    }
}
