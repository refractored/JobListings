package net.refractored.joblistings.exceptions

import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import revxrsal.commands.bukkit.sender
import revxrsal.commands.command.CommandActor
import revxrsal.commands.exception.*


// This is for only the commands :3
class CommandErrorHandler : DefaultExceptionHandler() {
    override fun invalidNumber(actor: CommandActor, exception: InvalidNumberException) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.InvalidNumber",
                listOf(
                    MessageReplacement(exception.input)
                )
            )
        )
    }

    override fun commandInvocation(actor: CommandActor, exception: CommandInvocationException) {
        MessageUtil.getMessage("General.UnexpectedError")
        exception.cause.printStackTrace()
    }

    override fun tooManyArguments(actor: CommandActor, exception: TooManyArgumentsException) {
        val command = exception.command
        val usage = (command.path.toRealString() + " " + command.usage).trim { it <= ' ' }
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.TooManyArguments",
                listOf(
                    MessageReplacement(usage)
                )
            )
        )
    }

    override fun invalidCommand(actor: CommandActor, exception: InvalidCommandException) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.InvalidCommand",
                listOf(
                    MessageReplacement(exception.input)
                )
            )
        )    }

    override fun noSubcommandSpecified(actor: CommandActor, exception: NoSubcommandSpecifiedException) {
        actor.sender.sendMessage(
            MessageUtil.getMessage("General.NoSubcommandSpecified")
        )
    }

    override fun missingArgument(actor: CommandActor, exception: MissingArgumentException) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.MissingArguments",
                listOf(
                    MessageReplacement(exception.parameter.name)
                )
            )
        )
    }

    override fun noPermission(actor: CommandActor, exception: NoPermissionException) {
        actor.sender.sendMessage(
            MessageUtil.getMessage("General.NoPermission")
        )
    }
}
